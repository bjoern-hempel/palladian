package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation for tweets containing a given url.<br/>
 * This uses Tweetmeme - http://help.tweetmeme.com/category/developers/api/
 * <br/><br/>
 * Currently 250 req/h, whitelisting possible<br/>
 * Information about current limits in HTTP headers<br/>
 * X-RateLimit-Limit and X-RateLimit-Remaining
 * 
 * @author Julien Schmehl
 *
 */
public class TweetmemeStats implements RankingService{

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(TweetmemeStats.class);
    
	/** No config values. */
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "tweetmeme";
    
    /** The ranking value types of this service **/
    /** 
     * The number of tweets mentioning this url.
     * Commitment value is 0.9
     */
    static RankingType TWEETS = new RankingType("twitter_tweets", "Twitter tweets", "The number of " +
    		"tweets mentioning this url, derived from tweetmeme.", 0.9f);
    /** 
     * The number of comments tweets mentioning this url.
     * Commitment value is 1.0
     */
    static RankingType COMMENTS = new RankingType("tweetmeme_comments", "Tweetmeme comments", "The number of " +
    		"comments on tweetmeme for this url.", 1.0f);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*60;
    
	public TweetmemeStats() {
        // we use a rather short timeout here, as responses are short.
        crawler.setConnectionTimeout(5000);
	}

	@Override
	public Ranking getRanking(String url) {
		Map<RankingType, Float> results = new HashMap<RankingType, Float>();
		Ranking ranking = new Ranking(this, url);
		if(isBlocked()) return ranking;

        try {

            String encUrl = StringHelper.urlEncode(url);
            JSONObject json = crawler.getJSONDocument("http://api.tweetmeme.com/url_info.json?url=" + encUrl);
            ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            if(json != null) {
            	if(json.has("story")) {
	            	int count = json.getJSONObject("story").getInt("url_count");
	            	int comments = json.getJSONObject("story").getInt("comment_count");
	            	results.put(TWEETS, (float) count);
	            	results.put(COMMENTS, (float) comments);
	                LOGGER.trace("Tweetmeme stats for " + url + " : " + results);
            	} else if(json.has("comment")) {
            		if(json.getString("comment").equals("unable to resolve URL")) {
            			results.put(TWEETS, 0f);
    	            	results.put(COMMENTS, 0f);
    	                LOGGER.trace("Tweetmeme stats for " + url + " : " + results);
            		}
            	} else {
                	results.put(TWEETS, null);
                	LOGGER.trace("Tweetmeme stats for " + url + "could not be fetched");
                    checkBlocked();
                }
            } else {
            	results.put(TWEETS, null);
            	LOGGER.trace("Tweetmeme stats for " + url + "could not be fetched");
                checkBlocked();
            }

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        }

        ranking.setValues(results);
        return ranking;
	}

	@Override
	public Map<String, Ranking> getRanking(List<String> urls) {
		Map<String, Ranking> results = new HashMap<String, Ranking>();
		if(isBlocked()) return results;
		 
		// iterate through urls and get ranking for each
		for(String u:urls) results.put(u, getRanking(u));

        return results;
        
	}
	
	/**
	 * Force a new check if this service is blocked due to excess
	 * of request limits. This updates the blocked-attribute
	 * of this service.
	 * 
	 * @return True if the service is momentarily blocked, false otherwise
	 */
	public boolean checkBlocked() {
		boolean error = false;
		try {
			JSONObject json = crawler.getJSONDocument("http://api.tweetmeme.com/url_info.json?url=http://www.google.com/");
			if(json.has("status"))
				if(json.get("status").equals("failure")) error = true;
		} catch (JSONException e) {
			LOGGER.error("HttpException " + e.getMessage());
		}
		if(!error) {
			blocked = false;
			lastCheckBlocked = new Date().getTime();
			return false;
		}
		blocked = true;
		lastCheckBlocked = new Date().getTime();
		LOGGER.error("Twitter Ranking Service is momentarily blocked. Will check again in 1h. Try resetting your IP-Address.");
		return true;
	}
	/**
	 * Returns if this service is momentarily blocked or not.
	 * 
	 * @return True if the service is momentarily blocked, false otherwise
	 */
	public boolean isBlocked() {
		if(new Date().getTime()-lastCheckBlocked < checkBlockedIntervall) return blocked;
		else return checkBlocked();
	}
	/**
	 * Sets this service blocked status to unblocked and resets the
	 * time of the last check to now.
	 * 
	 * @return True if reset was successful, false otherwise
	 */
	public boolean resetBlocked() {
		blocked = false;
		lastCheckBlocked = new Date().getTime();
		return true;
	}
	/**
	 * Get the id of this ranking service.
	 * 
	 * @return The id-string of this service
	 */
	public String getServiceId() {
		return SERVICE_ID;
	}
	/**
	 * Get all ranking types of this ranking service.
	 * 
	 * @return A list of ranking types
	 */
	public List<RankingType> getRankingTypes() {
		ArrayList<RankingType> types = new ArrayList<RankingType>();
		types.add(TWEETS);
		types.add(COMMENTS);
		return types;
	}
	/**
	 * Get the ranking type for this id.
	 * 
	 * @return The ranking type for the given id
	 */
	public RankingType getRankingType(String id) {
		if(id.equals(TWEETS.getId())) return TWEETS;
		else if(id.equals(COMMENTS.getId())) return COMMENTS;
		return null;
	}

}