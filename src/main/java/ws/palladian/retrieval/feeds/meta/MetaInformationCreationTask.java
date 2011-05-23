/**
 * Created on: 25.10.2010 18:24:49
 */
package ws.palladian.retrieval.feeds.meta;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.retrieval.ConnectionTimeoutPool;
import ws.palladian.retrieval.feeds.Feed;

import com.sun.syndication.feed.rss.Guid;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * The MetaInformationCreator gets information about last modified since and
 * ETag support as well as information about the header size.
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @version 1.0
 * @since 1.0
 * 
 */
public final class MetaInformationCreationTask implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);

    private final Feed feed;

    private final DatabaseManager dbManager;

    private static final String psSupportsLMS = "UPDATE feeds SET supportsLMS=? WHERE id=?";

    private static final String psSupportsEtag = "UPDATE feeds SET supportsETag=? WHERE id=?";

    private static final String psResponseSize = "UPDATE feeds SET conditionalGetResponseSize=? WHERE id=?";

    private static final String psSupportsPubSubHubBub = "UPDATE feeds SET supportsPubSuHubBub=? WHERE id=?";

    private static final String psIsAccessibleFeed = "UPDATE feeds SET isAccessibleFeed=? WHERE id=?";

    private static final String psFeedVersion = "UPDATE feeds SET feedFormat=? WHERE id=?";

    private static final String psHasItemIds = "UPDATE feeds SET hasItemIds=? WHERE id=?";

    private static final Pattern[] validFeedPatterns = new Pattern[] { Pattern.compile("<rss"),
            Pattern.compile("<feed"), Pattern.compile("<rdf:RDF") };


    private String currentFeedContent;

    public MetaInformationCreationTask(Feed feed, DatabaseManager dbManager) {
        this.feed = feed;
        this.dbManager = dbManager;
    }

    private String getContent(URL feedURL) throws IOException {
        InputStream feedInput = null;
        String ret = "";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) feedURL.openConnection();
            connection.setReadTimeout(10000);
            ConnectionTimeoutPool timeoutPool = ConnectionTimeoutPool.getInstance();
            timeoutPool.add(connection, 2 * DateHelper.MINUTE_MS);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.0.4) Gecko/2008102920 Firefox/3.0.4");
            feedInput = connection.getInputStream();
            ret = IOUtils.toString(feedInput);
        } finally {
            IOUtils.closeQuietly(feedInput);
            if (connection != null) {
                connection.disconnect();
            }
        }
        return ret;
    }

    private Integer getFeedResponseSize(final HttpURLConnection connection) {
        int ret = 0;
        try {
            if (HttpURLConnection.HTTP_NOT_MODIFIED == connection.getResponseCode()) {
                ret = new Integer((connection.getContentLength() == -1 ? 0 : connection.getContentLength())
                        + sumHeaderFieldSize(connection.getHeaderFields()));
            } else {
                ret = new Integer(-1);
            }
        } catch (IOException e) {
            LOGGER.error("Could not read header fields");
        }
        return ret;
    }

    /**
     * Checks if a web feeds server does support condition gets.
     * 
     * @param feed
     *            The feed to check.
     * @param responseSize
     *            Contains the size of the returned message with HTTP Status
     *            code 304 or -1 if conditional get is not supported.
     * @return {@code true} if the feed supports conditional gets, {@code false} otherwise.
     * @throws IOException
     */
    private Boolean getFeedSupports304(final HttpURLConnection connection) throws IOException {
        Boolean ret = false;

        if (HttpURLConnection.HTTP_NOT_MODIFIED == connection.getResponseCode()) {
            ret = true;
        }
        return ret;
    }

    private Boolean getFeedSupportsPubSubHubBub(final URLConnection connection) throws IOException {
        if (currentFeedContent != null && currentFeedContent.contains("rel=\"hub\"")) {
            return Boolean.valueOf(true);
        } else {
            return Boolean.valueOf(false);
        }
    }

    private String getFeedVersion(SyndFeed feed) throws IllegalArgumentException, FeedException {
        return feed.getFeedType();
    }

    /**
     * @return
     * @throws FeedException
     */
    private SyndFeed getSyndFeed() throws FeedException {
        SyndFeedInput input = new SyndFeedInput();
        input.setPreserveWireFeed(true);
        StringReader currentFeedInputReader = new StringReader(currentFeedContent);
        SyndFeed feed = input.build(currentFeedInputReader);
        currentFeedInputReader.close();
        return feed;
    }

    private Boolean getSupportsETag(final URLConnection connection) {
        boolean ret = false;
        ret = connection.getHeaderField("Etag") == null;
        return ret;
    }

    private Boolean isAccessibleFeed(HttpURLConnection connection) throws IOException {
        if (HttpURLConnection.HTTP_NOT_FOUND == connection.getResponseCode()
                || HttpURLConnection.HTTP_FORBIDDEN == connection.getResponseCode()) {
            return false;
        }
        if (currentFeedContent != null) {
            for (Pattern pattern : validFeedPatterns) {
                Matcher matcher = pattern.matcher(currentFeedContent);
                if (matcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create meta information, that is, find:
     * <ul>
     * <li>Etag support</li>
     * <li>last modified since support</li>
     * <li>If conditional get is supported also the size of the reply.</li>
     * </ul>
     */
    @Override
    public void run() {

        HttpURLConnection connection = null;

        try {
            URL feedURL = new URL(feed.getFeedUrl());
            connection = (HttpURLConnection) feedURL.openConnection();
            ConnectionTimeoutPool timeoutPool = ConnectionTimeoutPool.getInstance();
            timeoutPool.add(connection, 2 * DateHelper.MINUTE_MS);
            connection.setIfModifiedSince(System.currentTimeMillis() + 60000);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.0.4) Gecko/2008102920 Firefox/3.0.4");
            connection.setReadTimeout(10000);
            connection.connect();

            currentFeedContent = getContent(feedURL);
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL of feed with id: " + feed.getId() + " is malformed!", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not reed from feed with id: " + feed.getId(), e);
        } finally {
            connection.disconnect();
        }

        Boolean isAccessibleFeed = false;
        try {
            isAccessibleFeed = isAccessibleFeed(connection);
        } catch (IOException e) {
            LOGGER.error("Unable to check if feed at URL " + feed.getFeedUrl() + " is accessible.");
        }

        Boolean supports304 = false;

        try {

            supports304 = getFeedSupports304(connection);

        } catch (IOException e) {
            LOGGER.error("Could not get HTTP header information for feed with id: " + feed.getId() + ".");
        }

        Boolean supportsETag = getSupportsETag(connection);
        Integer responseSize = -1;
        if (supports304) {
            responseSize = getFeedResponseSize(connection);
        }
        Boolean supportsPubSubHubBub = Boolean.valueOf(false);

        try {
            supportsPubSubHubBub = getFeedSupportsPubSubHubBub(connection);
        } catch (IOException e1) {
            LOGGER.error("Could not get Content with information about PubSubHubBub information for feed with id: "
                    + feed.getId() + ".");
        }

        String feedVersion = null;
        SyndFeed syndFeed = null;
        try {
            syndFeed = getSyndFeed();
            feedVersion = getFeedVersion(syndFeed);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to determine feed version.", e);
        } catch (FeedException e) {
            LOGGER.error("Unable to determine feed version.", e);
        }

        boolean hasItemIds = false;
        if (syndFeed != null) {
            hasItemIds = checkForItemIds(syndFeed);
        }

        try {
            writeMetaInformationToDatabase(feed, supports304, supportsETag, responseSize, supportsPubSubHubBub,
                    isAccessibleFeed, feedVersion, hasItemIds);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to store results to Database.", e);
        } finally {
            connection.disconnect();
        }

        feed.freeMemory();
        // feed.setItems(null);

        MetaInformationCreator.counter++;
        LOGGER.info("Processed feed: "
                + feed.getId()
                + "; percent done: "
                + MathHelper.round(100 * MetaInformationCreator.counter
                        / (double) MetaInformationCreator.collectionSize, 2) + "(" + MetaInformationCreator.counter
                + ")");
    }

    private boolean checkForItemIds(SyndFeed syndFeed) {

        boolean result = false;

        Iterator<?> it = syndFeed.getEntries().iterator();
        if (it.hasNext()) {
            SyndEntry syndEntry = (SyndEntry) it.next();
            Object wireEntry = syndEntry.getWireEntry();

            if (wireEntry instanceof com.sun.syndication.feed.atom.Entry) {
                com.sun.syndication.feed.atom.Entry atomEntry = (com.sun.syndication.feed.atom.Entry) wireEntry;
                String rawId = atomEntry.getId();
                if (rawId != null && !rawId.isEmpty()) {
                    result = true;
                }
            } else if (wireEntry instanceof com.sun.syndication.feed.rss.Item) {
                com.sun.syndication.feed.rss.Item rssItem = (com.sun.syndication.feed.rss.Item) wireEntry;
                Guid guid = rssItem.getGuid();
                if (guid != null && !guid.getValue().isEmpty()) {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * @param headerFields
     *            The header fields from an http connection.
     * @return The summed up size of all header fields in bytes
     */
    private int sumHeaderFieldSize(Map<String, List<String>> headerFields) {
        Integer ret = 0;
        for (Entry<String, List<String>> entry : headerFields.entrySet()) {
            String key = entry.getKey();
            if (key != null) {
                byte[] bytes = key.getBytes();
                ret += bytes.length;
            }
            for (String value : entry.getValue()) {
                ret += value.getBytes().length;
            }
        }
        return ret;
    }

    private void writeMetaInformationToDatabase(Feed feed, Boolean supportsLMS, Boolean supportsETag,
            Integer responseSizeValue, Boolean supportsPubSubHubBub, Boolean isAccessibleFeed, String feedVersion,
            boolean hasItemIds)
            throws SQLException {

        Integer id = feed.getId();

        dbManager.runUpdate(psSupportsLMS, supportsLMS, id);
        dbManager.runUpdate(psSupportsEtag, supportsETag, id);
        dbManager.runUpdate(psResponseSize, responseSizeValue, id);
        dbManager.runUpdate(psSupportsPubSubHubBub, supportsPubSubHubBub, id);
        dbManager.runUpdate(psIsAccessibleFeed, isAccessibleFeed, id);
        dbManager.runUpdate(psFeedVersion, feedVersion, id);
        dbManager.runUpdate(psHasItemIds, hasItemIds, id);
    }

}
