package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.ResultIterator;
import ws.palladian.persistence.ResultSetCallback;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;

/**
 * The FeedDatabase is an implementation of the FeedStore that stores feeds and items in a relational database.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public class FeedDatabase extends DatabaseManager implements FeedStore {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDatabase.class);

    // ////////////////// feed prepared statements ////////////////////
    private static final String ADD_FEED_ITEM = "INSERT IGNORE INTO feed_items SET feedId = ?, title = ?, link = ?, rawId = ?, published = ?, authors = ?, description = ?, text = ?, pageText = ?";
    private static final String ADD_FEED = "INSERT IGNORE INTO feeds SET feedUrl = ?, siteUrl = ?, title = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, activityPattern = ?";
    private static final String UPDATE_FEED = "UPDATE feeds SET feedUrl = ?, siteUrl = ?, title = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, lastEtag = ?, lastPollTime = ?, activityPattern = ? WHERE id = ?";
    private static final String UPDATE_FEED_POST_DISTRIBUTION = "REPLACE INTO feeds_post_distribution SET feedID = ?, minuteOfDay = ?, posts = ?, chances = ?";
    private static final String GET_FEED_POST_DISTRIBUTION = "SELECT minuteOfDay, posts, chances FROM feeds_post_distribution WHERE feedID = ?";
    private static final String GET_FEEDS = "SELECT * FROM feeds";
    private static final String GET_FEED_BY_URL = "SELECT * FROM feeds WHERE feedUrl = ?";
    private static final String GET_FEED_BY_ID = "SELECT * FROM feeds WHERE id = ?";
    private static final String GET_ITEMS_BY_RAW_ID = "SELECT * FROM feed_items WHERE rawID = ?";
    private static final String GET_ITEMS_BY_RAW_ID_2 = "SELECT * FROM feed_items WHERE feedId = ? AND rawID = ?";
    private static final String CHANGE_CHECK_APPROACH = "UPDATE feeds SET minCheckInterval = 5, maxCheckInterval = 1, lastHeadlines = '', checks = 0, lastFeedEntry = NULL";
    private static final String GET_ITEMS = "SELECT * FROM feed_items LIMIT ? OFFSET ?";
    private static final String GET_ALL_ITEMS = "SELECT * FROM feed_items";
    private static final String GET_ITEM_BY_ID = "SELECT * FROM feed_items WHERE id = ?";
    private static final String DELETE_ITEM_BY_ID = "DELETE FROM feed_items WHERE id = ?";

    @Override
    public boolean addFeed(Feed feed) {
        LOGGER.trace(">addFeed " + feed);
        boolean added = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getFeedUrl());
        parameters.add(feed.getSiteUrl());
        parameters.add(feed.getTitle());
        parameters.add(feed.getContentType().getIdentifier());
        parameters.add(feed.getLanguage());
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getLastHeadlines());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getLastFeedEntrySQLTimestamp());
        parameters.add(feed.getActivityPattern());
        int result = runInsertReturnId(ADD_FEED, parameters);
        if (result > 0) {
            feed.setId(result);
            added = true;
        }

        LOGGER.trace("<addFeed " + added);
        return added;
    }

    @Override
    public boolean updateFeed(Feed feed) {
        LOGGER.trace(">updateFeed " + feed);

        if (feed.getId() == -1) {
            LOGGER.debug("feed does not exist and is added therefore");
            return addFeed(feed);
        }

        boolean updated = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getFeedUrl());
        parameters.add(feed.getSiteUrl());
        parameters.add(feed.getTitle());
        parameters.add(feed.getContentType().getIdentifier());
        parameters.add(feed.getLanguage());
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getLastHeadlines());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getLastFeedEntrySQLTimestamp());
        parameters.add(feed.getLastETag());
        parameters.add(feed.getLastPollTimeSQLTimestamp());
        parameters.add(feed.getActivityPattern());
        parameters.add(feed.getId());

        int result = runUpdate(UPDATE_FEED, parameters);
        if (result == 1) {
            updated = true;
        }

        LOGGER.trace("<updateFeed " + updated);

        return updated;
    }

    public Map<Integer, int[]> getFeedPostDistribution(Feed feed) {

        final Map<Integer, int[]> postDistribution = new HashMap<Integer, int[]>();

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                int minuteOfDay = resultSet.getInt("minuteOfDay");
                int posts = resultSet.getInt("posts");
                int chances = resultSet.getInt("chances");
                int[] postsChances = { posts, chances };
                postDistribution.put(minuteOfDay, postsChances);
            }
        };

        runQuery(callback, GET_FEED_POST_DISTRIBUTION, feed.getId());
        return postDistribution;
    }

    public void updateFeedPostDistribution(Feed feed, Map<Integer, int[]> postDistribution) {
        for (java.util.Map.Entry<Integer, int[]> distributionEntry : postDistribution.entrySet()) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(feed.getId());
            parameters.add(distributionEntry.getKey());
            parameters.add(distributionEntry.getValue()[0]);
            parameters.add(distributionEntry.getValue()[1]);
            runUpdate(UPDATE_FEED_POST_DISTRIBUTION, parameters);
        }
    }

    /**
     * When the check approach is switched we need to reset learned and calculated values such as check intervals,
     * checks, lastHeadlines etc.
     */
    public void changeCheckApproach() {
        runUpdate(CHANGE_CHECK_APPROACH);
    }

    @Override
    public List<Feed> getFeeds() {
        return runQuery(new FeedRowConverter(), GET_FEEDS);
    }

    @Override
    public Feed getFeedByUrl(String feedUrl) {
        return runSingleQuery(new FeedRowConverter(), GET_FEED_BY_URL, feedUrl);
    }

    @Override
    public Feed getFeedByID(int feedID) {
        return runSingleQuery(new FeedRowConverter(), GET_FEED_BY_ID, feedID);
    }

    @Override
    public boolean addFeedItem(Feed feed, FeedItem entry) {
        LOGGER.trace(">addEntry " + entry + " to " + feed);
        boolean added = false;

        int result = runInsertReturnId(ADD_FEED_ITEM, getItemParameters(feed, entry));
        if (result > 0) {
            entry.setId(result);
            added = true;
        }

        LOGGER.trace("<addEntry " + added);
        return added;
    }

    private List<Object> getItemParameters(Feed feed, FeedItem entry) {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getId());
        parameters.add(entry.getTitle());
        parameters.add(entry.getLink());
        parameters.add(entry.getRawId());
        parameters.add(entry.getPublishedSQLTimestamp());
        parameters.add(entry.getAuthors());
        parameters.add(entry.getItemDescription());
        parameters.add(entry.getItemText());
        parameters.add(entry.getPageText());
        return parameters;
    }

    @Override
    public int addFeedItems(Feed feed, List<FeedItem> feedItems) {
        LOGGER.trace(">addFeedItems " + feedItems.size() + " to " + feed);
        int added = 0;

        List<List<Object>> batchArgs = new ArrayList<List<Object>>();
        for (FeedItem feedItem : feedItems) {
            List<Object> parameters = getItemParameters(feed, feedItem);
            batchArgs.add(parameters);
        }

        // set the generated IDs back to the FeedItems and count number of added items
        int[] result = runBatchInsertReturnIds(ADD_FEED_ITEM, batchArgs);
        for (int i = 0; i < result.length; i++) {
            int id = result[i];
            if (id > 0) {
                feedItems.get(i).setId(id);
                added++;
            }
        }

        LOGGER.trace("<addFeedItems " + added);
        return added;
    }

    @Deprecated
    public FeedItem getFeedItemByRawId(String rawId) {
        return runSingleQuery(new FeedItemRowConverter(), GET_ITEMS_BY_RAW_ID, rawId);
    }

    @Override
    public FeedItem getFeedItemByRawId(int feedId, String rawId) {
        return runSingleQuery(new FeedItemRowConverter(), GET_ITEMS_BY_RAW_ID_2, feedId, rawId);
    }

    public FeedItem getFeedItemById(int id) {
        return runSingleQuery(new FeedItemRowConverter(), GET_ITEM_BY_ID, id);
    }

    /**
     * Get the specified count of feed items, starting at offset.
     * 
     * @param limit
     * @param offset
     * @return
     */
    public List<FeedItem> getFeedItems(int limit, int offset) {
        return runQuery(new FeedItemRowConverter(), GET_ITEMS, limit, offset);
    }

    /**
     * Get FeedItems by using a custom SQL query. The SELECT part must contain all appropriate columns with their
     * names from the feed_items table.
     * 
     * @param sqlQuery
     * @return
     */
    @Override
    public List<FeedItem> getFeedItemsBySqlQuery(String sqlQuery) {
        return runQuery(new FeedItemRowConverter(), sqlQuery);
    }

    public void deleteFeedItemById(int id) {
        runUpdate(DELETE_ITEM_BY_ID, id);
    }

    public ResultIterator<FeedItem> getFeedItems() {
        return runQueryWithIterator(new FeedItemRowConverter(), GET_ALL_ITEMS);
    }

    public void clearFeedTables() {
        runUpdate("TRUNCATE TABLE feeds");
        runUpdate("TRUNCATE TABLE feed_items");
        runUpdate("TRUNCATE TABLE feeds_post_distribution");
        runUpdate("TRUNCATE TABLE feed_evaluation_polls");
    }

}