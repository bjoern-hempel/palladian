package ws.palladian.retrieval.feeds.evaluation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.date.DateHelper;

/**
 * @author Sandro Reichert
 * 
 */
public class ChartCreator {

    private static final Logger LOGGER = Logger.getLogger(ChartCreator.class);
    
    private static final String FEED_SIZE_HISTOGRAM_FILE_PATH = "data/temp/feedSizeHistogrammData.csv";
    private static final String FEED_AGE_FILE_PATH = "data/temp/feedAgeData.csv";
    private static final String TIMELINESS2_FILE_PATH = "data/temp/timeliness2Data.csv";
    private static final String PERCENTAGE_NEW_MAX_POLL_FILE_PATH = "data/temp/percentNewMaxPollData.csv";
    private static final String SUM_VOLUME_MAX_MIN_TIME_FILE_PATH = "data/temp/sumVolumeTimeData";
    private static final String FEEDS_NEW_ITEMS_PATH_INPUT = "data/temp/Feeds_NewItems_IN.csv";
    private static final String FEEDS_NEW_ITEMS_PATH_OUTPUT = "data/temp/Feeds_NewItems_OUT.csv";

    private final int maxNumberOfPollsScoreMax;
    private final int maxNumberOfPollsScoreMin;
    
    private final EvaluationDatabase database;
    
    private final int totalExperimentHours;

    /**
     * Our policies:<br />
     * MIN: get every item as early as possible but not before it is available and <br />
     * MAX: only poll feed if all items in the window are new, but do not miss any item inbetween
     * 
     * @author Sandro Reichert
     */
    public enum Policy {
        MIN, MAX
    }

    /**
     * @param maxNumberOfPollsScoreMin The maximum number of polls to be used by methods creating data for the
     *            MIN-policy.
     * @param maxNumberOfPollsScoreMax The maximum number of polls to be used by methods creating data for the
     *            MAX-policy.
     */
    public ChartCreator(final int maxNumberOfPollsScoreMin, final int maxNumberOfPollsScoreMax) {
        this.database = new EvaluationDatabase();
        this.maxNumberOfPollsScoreMax = maxNumberOfPollsScoreMin;
        this.maxNumberOfPollsScoreMin = maxNumberOfPollsScoreMax;
        this.totalExperimentHours = (int) ((FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND - FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND) / DateHelper.HOUR_MS);
    }


    /**
     * Generates a *.csv file to generate a feed size histogram and stores it at
     * {@link ChartCreator#FEED_SIZE_HISTOGRAM_FILE_PATH}.
     * csv file has pattern (feed size in KB; number of feeds having this size;percentage of all feeds;)
     * 
     * @param chartInterval The size of the interval in KB, e.g. 10: 10KB, 20KB
     * @param chartNumberOfIntervals The number of intervals to display in detail, one additional interval 'more' is
     *            added automatically.
     *            e.g. 20 generates 20 intervals of size {@link #chartInterval} plus one containing all feeds that are
     *            larger
     */
    @SuppressWarnings("unused")
    private void createFeedSizeHistogrammFile(final int chartInterval, final int chartNumberOfIntervals) {
        List<EvaluationFeedPoll> polls = database.getFeedSizes();
        int[] feedSizeDistribution = new int[chartNumberOfIntervals + 1];
        int totalNumberOfFeeds = 0;
        
        for (EvaluationFeedPoll poll : polls) {
//            int feedID = poll.getFeedID();
            float pollSize = poll.getSizeOfPoll();
            int i =  new Double(Math.floor(pollSize/1024/chartInterval)).intValue() ;
            i = i > chartNumberOfIntervals ? chartNumberOfIntervals : i;
            feedSizeDistribution[i]++;
            totalNumberOfFeeds++;
        }        
        
        StringBuilder feedSizeDistributionSB = new StringBuilder();
        feedSizeDistributionSB.append("Feed size in KB;number of feeds;percentage of the feeds;\n");
        int currentIntervalSize = 0;
        String intervalSizeToWrite = "";
        final int chartMax = chartInterval * chartNumberOfIntervals;

        for (int number : feedSizeDistribution) {
            currentIntervalSize += chartInterval;
            intervalSizeToWrite = currentIntervalSize > chartMax ? "more" : String.valueOf(currentIntervalSize);
            feedSizeDistributionSB.append(intervalSizeToWrite).append(";").append(number).append(";")
                    .append((float) number / (float) totalNumberOfFeeds * 100).append(";\n");
        }
        
        boolean outputWritten = FileHelper.writeToFile(FEED_SIZE_HISTOGRAM_FILE_PATH, feedSizeDistributionSB);
        if (outputWritten) {
            LOGGER.info("feedSizeHistogrammFile written to: " + FEED_SIZE_HISTOGRAM_FILE_PATH);
        } else {
            LOGGER.fatal("feedSizeHistogrammFile has not been written to: " + FEED_SIZE_HISTOGRAM_FILE_PATH);
        }
    }


    /**
     * Generates a *.csv file to generate a feed age histogram and stores it at {@link ChartCreator#FEED_AGE_FILE_PATH}.
     * csv file has pattern (feed file age;number of feeds;percentage of the feeds;)
     */
    @SuppressWarnings("unused")
    private void createFeedAgeFile(){        
        List<EvaluationItemIntervalItem> polls = database.getAverageUpdateIntervals();
        int[] feedAgeDistribution = new int[34];
        int totalNumberOfFeeds = 0;
        
        for (EvaluationItemIntervalItem intervalItem : polls) {
            int averageUpdateIntervalHours = new Double(Math.floor(intervalItem.getAverageUpdateInterval()/3600000)).intValue();
            int i = -1;
            if (averageUpdateIntervalHours <= 24) {
                i = averageUpdateIntervalHours;
            } else if (averageUpdateIntervalHours <= 24 * 2) {
                i = 24; // 2 days
            } else if (averageUpdateIntervalHours <= 24 * 3) {
                i = 25; // 3 days
            } else if (averageUpdateIntervalHours <= 24 * 4) {
                i = 26; // 4 days
            } else if (averageUpdateIntervalHours <= 24 * 5) {
                i = 27; // 5 days
            } else if (averageUpdateIntervalHours <= 24 * 6) {
                i = 28; // 6 days
            } else if (averageUpdateIntervalHours <= 24 * 7) {
                i = 29; // 7 days
            } else if (averageUpdateIntervalHours <= 24 * 7 * 2) {
                i = 30; // 2 week
            } else if (averageUpdateIntervalHours <= 24 * 7 * 3) {
                i = 31; // 3 weeks
            } else if (averageUpdateIntervalHours <= 24 * 7 * 4) {
                i = 32; // 4 weeks
            } else {
                i = 33; // more
            }
            feedAgeDistribution[i]++;
            totalNumberOfFeeds++;
        }        

        StringBuilder feedAgeSB = new StringBuilder();
        feedAgeSB.append("feed file age;number of feeds;percentage of the feeds;\n");
        int i = 0;
        String[] caption = {"1 hour","2 hours","3 hours","4 hours","5 hours","6 hours","7 hours","8 hours","9 hours","10 hours","11 hours","12 hours","13 hours","14 hours","15 hours","16 hours","17 hours","18 hours","19 hours","20 hours","21 hours","22 hours","23 hours","24 hours","2 days","3 days","4 days","5 days","6 days","7 days","2 weeks","3 weeks","4 weeks","more"};
        
        for (int number : feedAgeDistribution) {
            feedAgeSB.append(caption[i]).append(";").append(number).append(";")
                    .append((float) number / (float) totalNumberOfFeeds * 100).append(";\n");
            i++;
        }
        
        boolean outputWritten = FileHelper.writeToFile(FEED_AGE_FILE_PATH, feedAgeSB);
        if (outputWritten) {
            LOGGER.info("feedAgeFile written to: " + FEED_AGE_FILE_PATH);
        } else {
            LOGGER.fatal("feedAgeFile has not been written to: " + FEED_AGE_FILE_PATH);
        }
    }

    /**
     * Helper function to process data aggregated (e.g. averaged) by numberOfPolls (for each data base table)
     * 
     * @param resultMap the map to write the output to
     * @param polls contains the preaggregated average values from one table
     * @param columnToWrite the position in Double[] to write the data to. This is the position in the *.csv file that
     *            is written.
     * @param numberOfColumns the total number of columns that are written to the *.csv file, used to create the
     *            Double[] in {@link resultMap}
     * @return the number of lines that has been processed
     */
    private int processDataAggregatedByPoll(List<EvaluationFeedPoll> polls, Map<Integer, Double[]> resultMap,
            final int columnToWrite, final int numberOfColumns) {
        int lineCount = 0;
        for (EvaluationFeedPoll poll : polls) {
            int pollToProcess = poll.getNumberOfPoll();
            Double[] aggregatedDataAtCurrentPoll = new Double[numberOfColumns];
            if (resultMap.containsKey(pollToProcess)) {
                aggregatedDataAtCurrentPoll = resultMap.get(pollToProcess);
            }
            aggregatedDataAtCurrentPoll[columnToWrite] = poll.getAverageValue();
            resultMap.put(poll.getNumberOfPoll(), aggregatedDataAtCurrentPoll);
            lineCount++;
        }
        return lineCount;
    }


    /**
     * Generates a *.csv file to generate the timeliness2 chart and stores it at
     * {@link ChartCreator#TIMELINESS2_FILE_PATH}. The file contains the average scoreMin per numberOfPoll for each
     * polling strategy separately. The csv file has the pattern (number of poll; adaptive; probabilistic; fix learned;
     * fix1h; fix1d)
     */
    @SuppressWarnings("unused")
    private void createAverageScoreMinByPollFile() {
        LOGGER.info("starting to create timeliness2File...");
        StringBuilder timeliness2SB = new StringBuilder();        
        Map<Integer, Double[]> timeliness2Map = new TreeMap<Integer, Double[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        final int NUMBER_OF_COLUMNS = 5;
        int columnToWrite = 0;

        timeliness2SB.append("numberOfPoll;");
        for (PollingStrategy pollingStrategy : PollingStrategy.values()) {
            LOGGER.info("starting to create data for " + pollingStrategy.toString());
            timeliness2SB.append(pollingStrategy.toString().toLowerCase()).append(";");
            polls = database.getAverageScoreMinPerPollFromMinPoll(pollingStrategy, maxNumberOfPollsScoreMax);
            processDataAggregatedByPoll(polls, timeliness2Map, columnToWrite, NUMBER_OF_COLUMNS);
            LOGGER.info("finished creating data for " + pollingStrategy.toString());
            columnToWrite++;
        }
        timeliness2SB.append("\n");

        writeMapToCSV(timeliness2Map, timeliness2SB, TIMELINESS2_FILE_PATH);
        LOGGER.info("finished creating timeliness2File.");
    }

    /**
     * Generates a *.csv file containing the average percentage of percentageNewEntries by numberOfPoll for each
     * strategy separately. File is written to {@link ChartCreator#PERCENTAGE_NEW_MAX_POLL_FILE_PATH}, file has structure
     * (numberOfPoll; adaptive; probabilistic; fix learned; fix1h; fix1d)
     */
    @SuppressWarnings("unused")
    private void createPercentageNewMaxPollFile() {
        LOGGER.info("starting to create percentageNewMax...");
        StringBuilder percentageNewSB = new StringBuilder();
        Map<Integer, Double[]> percentageNewMap = new TreeMap<Integer, Double[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        final int NUMBER_OF_COLUMNS = 5;
        int columnToWrite = 0;

        percentageNewSB.append("numberOfPoll;");
        for (PollingStrategy pollingStrategy : PollingStrategy.values()) {
            LOGGER.info("starting to create data for " + pollingStrategy.toString());
            percentageNewSB.append(pollingStrategy.toString().toLowerCase()).append(";");
            polls = database
                    .getAveragePercentageNewEntriesPerPollFromMaxPoll(pollingStrategy, maxNumberOfPollsScoreMin);
            processDataAggregatedByPoll(polls, percentageNewMap, columnToWrite, NUMBER_OF_COLUMNS);
            LOGGER.info("finished creating data for " + pollingStrategy.toString());
            columnToWrite++;
        }
        percentageNewSB.append("\n");

        writeMapToCSV(percentageNewMap, percentageNewSB, PERCENTAGE_NEW_MAX_POLL_FILE_PATH);
        LOGGER.info("finished creating percentageNewFile.");
    }

    /**
     * Helper to traverse the result map {@link outputMap}, append its items to given StringBuilder {@link outputSB} and
     * write SB into the *.csv file {@link filePath}.
     * For every pair (K,V) in the map, the Intger value (key) is written into the first column (e.g. number of poll),
     * followed by the values for each strategy, e.g. adaptive, probabilistic, fix learned, fix1h, fix1d
     * 
     * @param outputMap the map to traverse with <numberOfPoll, {adaptive, probabilistic, fix learned, fix1h, fix1d}>
     * @param outputSB the StringBuilder that already contains the header information (column heads)
     * @param filePath the file to write the output to
     */
    private void writeMapToCSV(Map<Integer, Double[]> outputMap, StringBuilder outputSB, String filePath) {
        Iterator<Integer> it = outputMap.keySet().iterator();
        while (it.hasNext()) {
            int currentPoll = it.next();
            Double[] scoresAtCurrentPoll = outputMap.get(currentPoll);
            outputSB.append(currentPoll).append(";").append(scoresAtCurrentPoll[0]).append(";")
                    .append(scoresAtCurrentPoll[1]).append(";").append(scoresAtCurrentPoll[2]).append(";")
                    .append(scoresAtCurrentPoll[3]).append(";").append(scoresAtCurrentPoll[4]).append(";\n");
        }

        boolean outputWritten = FileHelper.writeToFile(filePath, outputSB);
        if (outputWritten) {
            LOGGER.info(filePath + " has been written");
        } else {
            LOGGER.fatal(filePath + " has NOT been written!");
        }
    }

    /**
     * Helper function to cumulate the sizeOfPoll values per hour.<br />
     * 
     * The function gets a list of {@link polls} which represents all polls one {@link PollingStrategy} (fix, adaptive,
     * etc.) would have done within our experiment time (specified by {@link ChartCreator#totalExperimentHours}.
     * Missing polls are simulated by adding the sizeOfPoll of the last poll that has been done by the
     * {@link PollingStrategy}.
     * 
     * @param polls contains the polls done by one {@link PollingStrategy}
     * @param totalResultMapMax the map to write the output to. The map may contain values of other
     *            {@link PollingStrategy}s.
     * @param columnToWrite the position in Long[] to write the data to.
     * @param numberOfColumns the total number of columns ({@link PollingStrategy}s)
     * @param simulateEtagUsage Use to simulate the usage of eTags and HTTP304. If true, and if the poll contains no
     *            new item, only the header is added, otherwise the transfer volume of the poll (or the last poll if
     *            polls are simulated)
     */
    private void volumeHelper(List<EvaluationFeedPoll> polls, Map<Integer, Long[]> totalResultMapMax,
            final int columnToWrite, final int numberOfColumns, final boolean simulateEtagUsage) {

        int feedIDLastStep = -1;
        int sizeOfPollLast = -1;
        int minuteLastStep = 0;
        int checkIntervalLast = -1;

        for (EvaluationFeedPoll poll : polls) {
            int feedIDCurrent = poll.getFeedID();
            
            // in Davids DB nicht vorhandene Polls simulieren
            if(feedIDLastStep != -1 && feedIDLastStep != feedIDCurrent) {
                
                int sizeToAdd = simulateEtagUsage && poll.getSupportsConditionalGet() ? poll
                        .getConditionalGetResponseSize() : sizeOfPollLast;
                
                while (minuteLastStep + checkIntervalLast < totalExperimentHours * 60) {

                    final int minuteToProcess = minuteLastStep + checkIntervalLast;
                    // x/60 + 1: add 1 hour to result to start with hour 1 instead of 0 (minute 1 means hour 1)
                    final int hourToProcess = minuteToProcess / 60 + 1;

                    addSizeOfPollToMap(totalResultMapMax, numberOfColumns, columnToWrite, hourToProcess,
                            sizeToAdd);
                    minuteLastStep = minuteToProcess;

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("simmuliere für FeedID " + feedIDLastStep + ", aktuelle Stunde: " + hourToProcess
                                + " aktuelle Minute " + minuteToProcess + " checkInterval "
                                + checkIntervalLast + " addiere " + sizeToAdd + "bytes" + " totalResultMapMax Feld: "
                                + totalResultMapMax.get(hourToProcess)[columnToWrite]);
                    }
                }
                minuteLastStep = 0;
            }
                   
            // aktuellen Poll behandeln
            final int hourToProcess = poll.getHourOfExperiment();

            int sizeOfPoll;
            if (simulateEtagUsage && poll.getNewWindowItems() == 0 && poll.getSupportsConditionalGet() == true) {
                    sizeOfPoll = poll.getConditionalGetResponseSize();
            } else {
                sizeOfPoll = poll.getSizeOfPoll();
            }

            addSizeOfPollToMap(totalResultMapMax, numberOfColumns, columnToWrite, hourToProcess, sizeOfPoll);

            if (poll.getNumberOfPoll() >= 2) {
                minuteLastStep += checkIntervalLast;
            }
            feedIDLastStep = feedIDCurrent;
            sizeOfPollLast = sizeOfPoll;
            checkIntervalLast = poll.getCheckInterval();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("bearbeite      FeedID " + feedIDCurrent + ", aktuelle Stunde: " + hourToProcess
                        + " aktuelle Minute " + minuteLastStep + " checkInterval " + poll.getCheckInterval()
                        + " addiere " + sizeOfPoll + "bytes" + " totalResultMapMax Feld: "
                        + totalResultMapMax.get(hourToProcess)[columnToWrite]);
            }
        }
    }

    /**
     * Helper's helper to add a sizeOfPoll ({@link sizeToAdd}) to the given virtual column ({@link columnToWrite})
     * in the row ({@link hourToProcess}) of a *.csv file, represented by the {@link totalResultMapMax}. If
     * {@link totalResultMapMax} already contains an entry for ({@link hourToProcess}), ({@link sizeToAdd}) is added
     * to the specified ({@link columnToWrite}), otherwise a new Long[] with {@link numberOfColumns} is generated
     * and the value is written to it. Finally, the virtual row is written back into {@link totalResultMapMax}
     * 
     * @param totalResultMapMax the map to write the result to, schema: <hourOfExperiment,
     *            cumulatedVolumePerAlgorithm{fix1440,fix60,ficLearned,adaptive,probabilistic} in Bytes>
     * @param numberOfColumns the number of columns = number of algorithms (5)
     * @param columnToWrite the column to which {@link SIZE_TO_ADD} is written
     * @param hourToProcess the row to which {@link SIZE_TO_ADD} is written
     * @param sizeToAdd the sizeOfPoll in bytes that is added to the specified field
     */
    private void addSizeOfPollToMap(Map<Integer, Long[]> totalResultMapMax, final int numberOfColumns,
            final int columnToWrite, final int hourToProcess, final int sizeToAdd) {

        Long[] transferredDataArray = new Long[numberOfColumns];
        if (totalResultMapMax.containsKey(hourToProcess)) {
            transferredDataArray = totalResultMapMax.get(hourToProcess);
        } else {
            Arrays.fill(transferredDataArray, 0L);
        }
        transferredDataArray[columnToWrite] += sizeToAdd;
        totalResultMapMax.put(hourToProcess, transferredDataArray);
    }

    /**
     * Calculates the cumulated transfer volume per poll per {@link PollingStrategy} in megabytes to a *.csv file.
     * Every row represents one poll, every column is one {@link PollingStrategy}.
     * 
     * @param policy The {@link Policy} to generate the file for.
     * @param simulateEtagUsage If true, for each poll that has no new item, the size of the conditional header is
     *            added to the transfer volume (instead of the sizeOfPoll).
     * @param feedIDMax the highest FeedID in the data set.
     */
    private void cumulatedVolumePerTimeFile(final Policy policy, final boolean simulateEtagUsage, final int feedIDMax) {
        LOGGER.info("starting to create sumVolumeFile for policy " + policy);
        StringBuilder cumulatedVolumeSB = new StringBuilder();
        // <hourOfExperiment, cumulatedVolumePerAlgorithm[fix1440,fix60,ficLearned,adaptive,probabilistic] in Bytes>
        Map<Integer, Long[]> totalResultMap = new TreeMap<Integer, Long[]>();
        List<EvaluationFeedPoll> polls = new LinkedList<EvaluationFeedPoll>();
        final int numberOfColumns = PollingStrategy.values().length;
        int feedIDStart = 1;
        int feedIDEnd = 10000;
        final int feedIDStep = 10000;
        int columnToWrite = 0;
    
        cumulatedVolumeSB.append("hour of experiment;");
        for (PollingStrategy pollingStrategy : PollingStrategy.values()) {

            LOGGER.info("starting to create data for " + pollingStrategy.toString());
            cumulatedVolumeSB.append(pollingStrategy.toString().toLowerCase()).append(";");
            feedIDStart = 1;
            feedIDEnd = 10000;

            while (feedIDEnd < feedIDMax) {
                LOGGER.info("checking feedIDs " + feedIDStart + " to " + feedIDEnd);
                polls = database.getTransferVolumeByHourFromTime(policy, pollingStrategy, feedIDStart, feedIDEnd);
                volumeHelper(polls, totalResultMap, columnToWrite, numberOfColumns, simulateEtagUsage);
                feedIDStart += feedIDStep;
                feedIDEnd += feedIDStep;
            }
            columnToWrite++;
            LOGGER.info("finished creating data for " + pollingStrategy.toString());
        }
        cumulatedVolumeSB.append("\n");

        // //////////// write totalResultMapMax to StringBuilder, cumulating the values row-wise \\\\\\\\\\\\\\\\\
        final long byteToMB = 1048576;
        Long[] volumesCumulated = new Long[numberOfColumns];
        Arrays.fill(volumesCumulated, 0l);
        Iterator<Integer> it = totalResultMap.keySet().iterator();
        while (it.hasNext()) {
            int currentHour = it.next();
            Long[] volumes = totalResultMap.get(currentHour);

            for (int i = 0; i < numberOfColumns; i++) {
                volumesCumulated[i] += volumes[i];
            }
            cumulatedVolumeSB.append(currentHour).append(";").append(volumesCumulated[0] / byteToMB).append(";")
                    .append(volumesCumulated[1] / byteToMB).append(";").append(volumesCumulated[2] / byteToMB)
                    .append(";").append(volumesCumulated[3] / byteToMB).append(";")
                    .append(volumesCumulated[4] / byteToMB).append(";\n");
        }

        // //////////// write final output to file \\\\\\\\\\\\\\\\\
        String filePathToWrite = "";
        String eTag = simulateEtagUsage ? "ETag" : "NoETag";
        switch (policy) {
            case MAX:
                filePathToWrite = SUM_VOLUME_MAX_MIN_TIME_FILE_PATH + "_Max" + eTag + ".csv";
                break;
            case MIN:
                filePathToWrite = SUM_VOLUME_MAX_MIN_TIME_FILE_PATH + "_Min" + eTag + ".csv";
                break;
            default:
                throw new IllegalStateException("unknown Policy: " + policy.toString());
        }
        boolean outputWritten = FileHelper.writeToFile(filePathToWrite, cumulatedVolumeSB);
        if (outputWritten) {
            LOGGER.info("sumVolumeFile for policy " + policy + " written to: " + filePathToWrite);
        } else {
            LOGGER.fatal("sumVolumeFile for policy " + policy + " has not been written to: " + filePathToWrite);
        }
    }

    /**
     * Rewrites file FEEDS_NEW_ITEMS_PATH_INPUT to FEEDS_NEW_ITEMS_PATH_OUTPUT. Input is a two column table with "new
     * Items"; "Number of Feeds", where (12, 2217) means that exactly 300 feeds had exactly 12 new items. Output file
     * has a third column "cumulated number of feeds" (12, 2217, 50385), where the third row states that 50385 feeds had
     * at least 12 new items, but some of these 50385 feeds have more than 12 new items.
     */
    @SuppressWarnings("unused")
    private void feedsNewItemsRewriter() {
        List<String> input = FileHelper.readFileToArray(FEEDS_NEW_ITEMS_PATH_INPUT);
        int highestNewItems = 0;
        boolean headPassed = false;

        // get highestNewItems-value
        for (String feed : input) {
            if (!headPassed) {
                headPassed = true;
                continue;
            }
            Integer feedI = Integer.parseInt(feed.split(";")[0]);
            if (feedI != null) {
                highestNewItems = feedI;
            } else {
                break;
            }
        }

        // size highestNewItems + 1 for additional line with zero new items
        Integer[][] expandedFile = new Integer[highestNewItems + 1][4];
        int lineCounter = 0;
        headPassed = false;
        String head = null;

        for (String currentLineS : input) {
            // copy head
            if (!headPassed) {
                head = currentLineS;
                headPassed = true;
                continue;
            }

            String[] currentLineA = currentLineS.split(";");

            // add missing lines
            while (Integer.parseInt(currentLineA[0]) > lineCounter) {
                expandedFile[lineCounter][0] = lineCounter;
                expandedFile[lineCounter][1] = Integer.parseInt(currentLineA[1]);
                expandedFile[lineCounter][2] = 0;
                lineCounter++;
            }

            // copy line from file
            expandedFile[lineCounter][0] = Integer.parseInt(currentLineA[0]);
            expandedFile[lineCounter][1] = Integer.parseInt(currentLineA[1]);
            expandedFile[lineCounter][2] = Integer.parseInt(currentLineA[1]);

            lineCounter++;
        }

        // cumulate feeds
        // copy value of last line
        expandedFile[lineCounter - 1][3] = expandedFile[lineCounter - 1][2];

        for (int i = lineCounter - 2; i >= 0; i--) {
            expandedFile[i][3] = expandedFile[i][2] + expandedFile[i + 1][3];
        }

        // write output
        StringBuilder outputSB = new StringBuilder();
        outputSB.append(head).append(";cumulated number of feeds;\n");

        for (Integer[] line : expandedFile) {
            if (line[0] == null) {
                break;
            }
            outputSB.append(line[0]).append(";").append(line[1]).append(";").append(line[3]).append(";\n");
        }

        FileHelper.writeToFile(FEEDS_NEW_ITEMS_PATH_OUTPUT, outputSB);
    }


    // /**
    // * Only a test
    // */
    // private void printFeedPolls() {
    // List<EvaluationFeedPoll> polls = ed.getAllFeedPollsFromAdaptiveMaxTime();
    //
    // for (EvaluationFeedPoll poll : polls) {
    // // int feedID = poll.getFeedID();
    // LOGGER.info(poll.getFeedID() + " " + poll.getSizeOfPoll());
    // }
    //
    // }

    /**
     * @param args ...
     */
	public static void main(String[] args) {

        /**
         * 200 polls for scoreMin
         * 200 polls for scoreMax
         */
        ChartCreator cc = new ChartCreator(200, 200);

        // cc.printFeedPolls();
        // cc.createFeedSizeHistogrammFile(10, 20); // letzter Test 12.11. DB Schema v2
        // cc.createFeedAgeFile(); // letzter Test 12.11. DB Schema v2
        // cc.createAverageScoreMinByPollFile(); // letzter Test 12.11. DB Schema v2
        // cc.createPercentageNewMaxPollFile(); // letzter Test 12.11. DB Schema v2
        cc.cumulatedVolumePerTimeFile(Policy.MAX, false, 210000); // letzter Test 12.11. DB Schema v2
        cc.cumulatedVolumePerTimeFile(Policy.MAX, true, 210000); // letzter Test 12.11. DB Schema v2
        cc.cumulatedVolumePerTimeFile(Policy.MIN, false, 210000); // letzter Test 12.11. DB Schema v2
        cc.cumulatedVolumePerTimeFile(Policy.MIN, true, 210000); // letzter Test 12.11. DB Schema v2
	}

}