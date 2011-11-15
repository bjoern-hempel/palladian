package ws.palladian.retrieval.feeds.evaluation.restoreFeedSizes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedClassifier;
import ws.palladian.retrieval.feeds.FeedTaskResult;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * TUDCS6 specific.<br />
 * Load the reconstructed feed entry streams from csv files (created when creating a dataset such as TUDCS6) to
 * database.
 * Iterate over all feed_polls. If httpStatusCode is 304, use the HTTP header size from the feed, otherwise the size of
 * the content of the current or previous gz file: Use pollTimestamp and check for *.gz file with same name. If it
 * exists, uncompress it and calculate its size. If no file exists, use size from previous gz file. (We stored only gz
 * files that contained at least one new entry)
 * If any HTTP status code besides 304 has been returned, the size of the last gz is added. This is not correct but
 * happened in less than 0.5% of the requests, so it should not matter...
 * 
 * Similar to {@link FeedTask}, this class uses a thread to process a single feed. While {@link FeedTask} retrieves the
 * feed from the web, this task iterates over persisted files generated by the {@link DatasetCreator}.
 * 
 * @author Sandro Reichert
 */
public class FeedSizeRestoreTask implements Callable<FeedTaskResult> {

    /** The logger for this class. */
    private final static Logger LOGGER = Logger.getLogger(FeedSizeRestoreTask.class);

    /**
     * The feed to process by this task.
     */
    private Feed feed = null;

    /**
     * The feed DB.
     */
    private final FeedDatabase feedDatabase;

    /**
     * Warn if processing of a feed takes longer than this.
     */
    public static final long EXECUTION_WARN_TIME = 3 * DateHelper.MINUTE_MS;


    /**
     * All gz files of this feed.
     */
    File[] allFiles = null;

    /**
     * Cache the size of the last non-HTTP304 response since we have gz files only in case there is a new item.
     */
    int lastFullResponseSize = 0;

    /**
     * Creates a new gz processing task for a provided feed.
     * 
     * @param feed The feed retrieved by this task.
     */
    public FeedSizeRestoreTask(Feed dbFeed, FeedDatabase feedDatabase) {
        this.feed = dbFeed;
        this.feedDatabase = feedDatabase;
    }

    /** A collection of all intermediate results that can happen, e.g. when updating meta information or a data base. */
    private Set<FeedTaskResult> resultSet = new HashSet<FeedTaskResult>();

    @Override
    public FeedTaskResult call() throws Exception {
        StopWatch timer = new StopWatch();
        try {
            LOGGER.debug("Start processing of feed id " + feed.getId() + " (" + feed.getFeedUrl() + ")");

            // skip feeds that have never been checked. We dont have any files, nor a folder for them.
            if (feed.getChecks() == 0) {
                LOGGER.debug("Feed id " + feed.getId() + " has never been checked. Nothing to do.");
                resultSet.add(FeedTaskResult.SUCCESS);
                doFinalLogging(timer);
                return getResult();
            }

            List<PollMetaInformation> feedPolls = feedDatabase.getFeedPollsByID(feed.getId());
            
            String gzPath = DatasetCreator.getFolderPath(feed.getId());
            allFiles = FileHelper.getFiles(gzPath, ".gz");
            Arrays.sort(allFiles); // sort chronologically


            for (PollMetaInformation feedPoll : feedPolls) {
                if (feedPoll.getHttpStatusCode() == 304) {
                    // use header size
                    feedPoll.setResponseSize(feed.getMetaInformation().getCgHeaderSize());
                } else if (feedPoll.getNumberNewItems() != null
                        && (feedPoll.getNumberNewItems() > 0 || feed.getActivityPattern() == FeedClassifier.CLASS_EMPTY)) {
                    // get size from gz file
                    int currentResponseSize = getResponseSizeFromGZ(feedPoll.getPollTimestamp());
                    feedPoll.setResponseSize(currentResponseSize);
                    // remember the size for next iteration
                    lastFullResponseSize = currentResponseSize;
                } else {
                    // use size from previous file
                    feedPoll.setResponseSize(lastFullResponseSize);
                }

                // write responseSize to database
                feedDatabase.updateFeedPoll(feedPoll);

                // free memory
                feedPoll = null;
            }
            resultSet.add(FeedTaskResult.SUCCESS);

            // store all Items in db
            doFinalStuff(timer);
            return getResult();

            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals. Errors are logged only and not written to database.
        } catch (Throwable th) {
            LOGGER.fatal("Error processing feedID " + feed.getId() + ": " + th);
            resultSet.add(FeedTaskResult.ERROR);
            doFinalLogging(timer);
            return getResult();
        }

    }

    private int getResponseSizeFromGZ(Date pollTimestamp){
        int responseSize = 0;

        for (File file : allFiles) {
            // skip files that are tagged to be removable.
            if (file.getName().endsWith("removeable.gz")) {
                continue;
            }
            long pollTime = pollTimestamp.getTime() / 1000;
            if (file.getName().startsWith(String.valueOf(pollTime))) {

                InputStream inputStream = null;
                try {
                    // Wrap this with a GZIPInputStream, if necessary.
                    // Do not use InputStreamReader, as this works encoding specific.
                    inputStream = new GZIPInputStream(new FileInputStream(file));

                    // Read the whole file.
                    ByteArrayOutputStream payload = new ByteArrayOutputStream();
                    int b;
                    while ((b = inputStream.read()) != -1) {
                        payload.write(b);
                    }
                    responseSize = payload.toByteArray().length;
                } catch (FileNotFoundException e) {
                    LOGGER.error(e);
                } catch (IOException e) {
                    LOGGER.error(e);
                } finally {
                    FileHelper.close(inputStream);
                }
                break;
            }
        }

        if (allFiles.length == 0 && feed.getActivityPattern() == FeedClassifier.CLASS_EMPTY) {
            responseSize = (int) feed.getMetaInformation().getByteSize();
        }

        // if size is still zero, we did not found a gz to the current poll so we use the one from the size last poll.
        if (responseSize == 0) {
            if (lastFullResponseSize == 0) {
                resultSet.add(FeedTaskResult.ERROR);
            }
            responseSize = lastFullResponseSize;
        }

        return responseSize;
    }

    /**
     * Sets the feed task result and processing time of this task, saves the feed to database, does the final logging
     * and frees the feed's memory.
     * 
     * @param timer The {@link StopWatch} to estimate processing time
     */
    private void doFinalStuff(StopWatch timer) {
        if (timer.getElapsedTime() > EXECUTION_WARN_TIME) {
            LOGGER.warn("Processing feed id " + feed.getId() + " took very long: " + timer.getElapsedTimeString());
            resultSet.add(FeedTaskResult.EXECUTION_TIME_WARNING);
        }
        doFinalLogging(timer);
        // since the feed is kept in memory we need to remove all items and the document stored in the feed
        feed.freeMemory(true);
    }

    /**
     * Decide the status of this FeedTask. This is done here to have a fixed ranking on the values.
     * 
     * @return The (current) result of the feed task.
     */
    private FeedTaskResult getResult() {
        FeedTaskResult result = null;
        if (resultSet.contains(FeedTaskResult.ERROR)) {
            result = FeedTaskResult.ERROR;
        } else if (resultSet.contains(FeedTaskResult.UNREACHABLE)) {
            result = FeedTaskResult.UNREACHABLE;
        } else if (resultSet.contains(FeedTaskResult.UNPARSABLE)) {
            result = FeedTaskResult.UNPARSABLE;
        } else if (resultSet.contains(FeedTaskResult.EXECUTION_TIME_WARNING)) {
            result = FeedTaskResult.EXECUTION_TIME_WARNING;
        } else if (resultSet.contains(FeedTaskResult.MISS)) {
            result = FeedTaskResult.MISS;
        } else if (resultSet.contains(FeedTaskResult.SUCCESS)) {
            result = FeedTaskResult.SUCCESS;
        } else {
            result = FeedTaskResult.OPEN;
        }

        return result;
    }

    /**
     * Do final logging of result to error or debug log, depending on the FeedTaskResult.
     * 
     * @param timer the {@link StopWatch} started when started processing the feed.
     */
    private void doFinalLogging(StopWatch timer) {
        FeedTaskResult result = getResult();
        String msg = "Finished processing of feed id " + feed.getId() + ". Result: " + result + ". Processing took "
                + timer.getElapsedTimeString();
        if (result == FeedTaskResult.ERROR) {
            LOGGER.error(msg);
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(msg);
        }
    }

}
