package tud.iir.classification.controlledtagging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.Stopwords;
import tud.iir.classification.WordCorrelation;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetCallback;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetEntry;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetFilter;
import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;

/**
 * 
 * optimum vlaues:
 * 
 * avgPr: 0.4863928631767631
 * avgRc: 0.23635926371440288
 * avgF1: 0.3181269338103561
 * 
 * @author Philipp Katz
 * 
 */
public class KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(KeyphraseExtractor.class);

    /** The stemmer to use. Snowball offers stemmer implementations for various languages. */
    private SnowballStemmer stemmer = new englishStemmer();

    /** List of stopwords to use. */
    private Stopwords stopwords = new Stopwords(Stopwords.Predefined.EN);

    /** The TokenizerPlus is responsible for all tokenization steps. */
    private TokenizerPlus tokenizer = new TokenizerPlus();

    /** The corpus is a model for the whole document collection. */
    private Corpus corpus = new Corpus();

    /** The classifier is used for predicting relevance values for keyphrase candidates. */
    private CandidateClassifier classifier = new CandidateClassifier();

    private boolean controlledMode = false; // XXX testing

    /** Different assignment strategies. */
    public enum AssignmentMode {

        /** Assign maximum count of keyphrases (e.g. 10 keyphrases or less). */
        FIXED_COUNT,

        /** Assign keyphrases which exceed a specified threshold (e.g. all keyphrases with weights > 0.75). */
        THRESHOLD,

        /**
         * Assign maximum count of keyphrases or more if they exceed the specified threshold (e.g. all keyphrases with
         * weights > 0.75 or 10 or less).
         */
        COMBINED

    }

    public enum CorrelationReRankingMode {
        NO_CORRELATIONS, SHALLOW_CORRELATIONS, DEEP_CORRELATIONS
    }

    private AssignmentMode assignmentMode = AssignmentMode.FIXED_COUNT;
    private CorrelationReRankingMode correlationReRankingMode = CorrelationReRankingMode.DEEP_CORRELATIONS;

    private int keyphraseCount = 10;
    private float keyphraseThreshold = 0.75f;

    private float correlationWeight = 90000;

    private String modelName = "controlledTagger";

    public KeyphraseExtractor() {
        tokenizer.setUsePosTagging(false);
    }

    public void buildCorpus(String filePath) {

        LOGGER.info("building corpus ...");

        StopWatch sw = new StopWatch();
        final Counter counter = new Counter();

        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] split = line.split("#");
                if (split.length < 2) {
                    return;
                }

                String text = split[0];
                Set<String> tags = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    tags.add(split[i]);
                }
                addToCorpus(text, tags);

                counter.increment();
                if (counter.getCount() % 10 == 0) {
                    LOGGER.info("added " + counter + " lines");
                }
            }
        });

        saveCorpus();
        LOGGER.info("built and saved corpus in " + sw.getElapsedTimeString());

    }

    /**
     * TODO We can keep the training data in memory. And add an option for exporting CSV for KNIME.
     * 
     * @param filePath
     * @param limit
     */
    public void buildClassifier(String filePath, final int limit) {

        LOGGER.info("creating training data for classifier ...");

        StopWatch sw = new StopWatch();
        final Counter counter = new Counter();

        // keep the CSV training data in memory for now
        final StringBuilder trainData = new StringBuilder();

        // create the training data for the classifier
        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("#");

                if (split.length < 2) {
                    return;
                }

                // create the document model
                DocumentModel candidates = createDocumentModel(split[0]);

                // the manually assigned keyphrases
                Set<String> tags = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    tags.add(split[i].toLowerCase());
                }

                // keep stemmed and unstemmed representation
                Set<String> stemmedTags = stem(tags);
                tags.addAll(stemmedTags);

                // mark positive candidates, i.e. those which were manually assigned
                // in the training data
                for (Candidate candidate : candidates) {
                    boolean isCandidate = tags.contains(candidate.getStemmedValue());
                    isCandidate = isCandidate || tags.contains(candidate.getValue());
                    isCandidate = isCandidate || tags.contains(candidate.getValue().replace(" ", ""));
                    candidate.setPositive(isCandidate);
                }

                // if this is the first iteration, write header with feature names;
                // this is only for convenience reasons, for example if we want to
                // experiment with the classification with KNIME
                if (counter.getCount() == 0) {
                    Set<String> featureNames = candidates.iterator().next().getFeatures().keySet();
                    trainData.append("#").append(StringUtils.join(featureNames, ";")).append("\n");
                }

                trainData.append(candidates.toCSV());

                counter.increment();
                if (counter.getCount() % 10 == 0) {
                    LOGGER.info("added " + counter + " lines");
                }
                if (counter.getCount() == limit) {
                    breakLineLoop();
                }
            }
        });

        // write the train data for the classifier to CSV file
        FileHelper.writeToFile("data/temp/KeyphraseExtractorTraining.csv", trainData);
        LOGGER.info("created training data in " + sw.getElapsedTimeString());

        // train and save the classifier
        LOGGER.info("training classifier ...");
        sw.start();

        // save memory; this is necessary, as the corpus consumes great amounts of memory, but
        // fortunately we don't need the corpus for the training process
        corpus = null;

        classifier = new CandidateClassifier();
        classifier.trainClassifier("data/temp/KeyphraseExtractorTraining.csv");
        classifier.saveTrainedClassifier();

        LOGGER.info("finished training in " + sw.getElapsedTimeString());

    }

    public void addToCorpus(String text) {

        List<Token> tokens = tokenize(text);
        corpus.addPhrases(tokens);

    }

    public void addToCorpus(String text, Set<String> keyphrases) {

        // tokenize the text and add the tokens/phrases to the corpus
        List<Token> tokens = tokenize(text);
        corpus.addPhrases(tokens);

        // the corpus stores the stemmed keyphrases!
        keyphrases = stem(keyphrases);
        corpus.addKeyphrases(keyphrases);

    }

    public void saveCorpus() {
        LOGGER.info("saving corpus ...");
        StopWatch sw = new StopWatch();
        corpus.makeRelativeScores();
        FileHelper.serialize(corpus, modelName + ".ser");
        LOGGER.info("saved corpus in " + sw.getElapsedTimeString());
    }

    public void loadCorpus() {
        LOGGER.info("loading corpus ...");
        StopWatch sw = new StopWatch();
        corpus = FileHelper.deserialize(modelName + ".ser");
        LOGGER.info("loaded corpus in " + sw.getElapsedTimeString());
    }

    public void loadClassifier() {
        LOGGER.info("loading classifier ...");
        StopWatch sw = new StopWatch();
        classifier.useTrainedClassifier();
        LOGGER.info("loaded classifier in " + sw.getElapsedTimeString());
    }

    public List<Candidate> extract(String text) {

        DocumentModel candidates = createDocumentModel(text);

        // eliminate undesired candidates in advance
        ListIterator<Candidate> listIterator = candidates.listIterator();
        while (listIterator.hasNext()) {
            Candidate candidate = listIterator.next();

            boolean ignore = stopwords.contains(candidate.getValue());
            ignore = ignore || !candidate.getValue().matches("[a-zA-Z\\s]{3,}");
            ignore = ignore || (controlledMode && candidate.getPrior() == 0);

            if (ignore) {
                listIterator.remove();
            }
        }

        // perform the regression for ranking the candidates
        classifier.classify(candidates);

        // do the correlation based re-ranking
        reRankCandidates(candidates);

        // TODO refactor this to its own method ////////////////////////////////////
        // Candidate[] candidateArray = candidates.toArray(new Candidate[candidates.size()]);
        // int numReRanking = candidateArray.length * (candidateArray.length - 1) / 2;
        //
        // // TODO parameter
        // float correlationWeight = 90000;
        //
        // for (int i = 0; i < candidateArray.length; i++) {
        // Candidate cand1 = candidateArray[i];
        // for (int j = i; j < candidateArray.length; j++) {
        // Candidate cand2 = candidateArray[j];
        //
        // WordCorrelation correlation = corpus.getCorrelation(cand1, cand2);
        // if (correlation != null) {
        // float reRanking = (float) ((correlationWeight / numReRanking) * correlation
        // .getRelativeCorrelation());
        // cand1.setRegressionValue(cand1.getRegressionValue() + reRanking);
        // cand2.setRegressionValue(cand2.getRegressionValue() + reRanking);
        // }
        // }
        // }
        // ///////////////////////////////////////////////////////////////////////////

        // create the final result, take the top n candidates
        limitResult(candidates);

        return candidates;

    }

    private void limitResult(DocumentModel candidates) {

        // sort the candidates by regression value
        Collections.sort(candidates, new CandidateComparator());

        // ListIterator for manipulating the list
        ListIterator<Candidate> listIterator = candidates.listIterator();

        switch (assignmentMode) {

            // assign a maximum number of keyphrases as result
            case FIXED_COUNT:
                if (candidates.size() > keyphraseCount) {
                    candidates.subList(keyphraseCount, candidates.size()).clear();
                }
                break;

            // assign all keyphrases which have a weight about the specified threshold
            case THRESHOLD:
                while (listIterator.hasNext()) {
                    if (listIterator.next().getRegressionValue() <= keyphraseThreshold) {
                        listIterator.remove();
                    }
                }
                break;

            // first assign a maximum number of keyphrases (FIXED_COUNT), but if there are more keyphrases
            // with weights above the specified threshold, we assign more than the specified count
            case COMBINED:
                while (listIterator.hasNext()) {
                    Candidate next = listIterator.next();
                    if (listIterator.nextIndex() > 10 && next.getRegressionValue() <= keyphraseThreshold) {
                        listIterator.remove();
                    }
                }
                break;
        }

    }

    //
    // this code is copy+paste from ControlledTagger
    //
    private void reRankCandidates(DocumentModel candidates) {

        StopWatch sw = new StopWatch();
        
        if (candidates.isEmpty()) {
            return;
        }
        
        Collections.sort(candidates, new CandidateComparator());

        // experimental: to normalize the range of the re-ranked tags back to their original range,
        // by keeping the lower/upper bounds, so we keep the general properties of the TF/IDF -- elsewise
        // we will get outliers which are considerably bigger than most of the other tag weights.
        double oldMin = candidates.get(0).getRegressionValue();
        double oldMax = candidates.get(candidates.size() - 1).getRegressionValue();

        // Option 1: do a "shallow" re-ranking, only considering top-tag (n)
        if (correlationReRankingMode == CorrelationReRankingMode.SHALLOW_CORRELATIONS) {
            Iterator<Candidate> candidateIterator = candidates.iterator();
            Candidate topCandidate = candidateIterator.next();

            while (candidateIterator.hasNext()) {
                Candidate currentCandidate = candidateIterator.next();

                WordCorrelation correlation = corpus.getCorrelation(topCandidate, currentCandidate);
                if (correlation != null) {
                    currentCandidate.increaseRegressionValue(correlationWeight * correlation.getRelativeCorrelation());
                }
            }
        }

        // Option 2: do a "deep" re-ranking, considering correlations between each possible combination
        else if (correlationReRankingMode == CorrelationReRankingMode.DEEP_CORRELATIONS) {
            Candidate[] candidatesArray = candidates.toArray(new Candidate[candidates.size()]);

            // experimental:
            // normalization factor; we have (n - 1) + (n - 2) + ... + 1 = n * (n - 1) / 2 re-rankings.
            int numReRanking = candidatesArray.length * (candidatesArray.length - 1) / 2;

            for (int i = 0; i < candidatesArray.length; i++) {
                Candidate candidate1 = candidatesArray[i];
                for (int j = i; j < candidatesArray.length; j++) {
                    Candidate candidate2 = candidatesArray[j];
                    
                    WordCorrelation correlation = corpus.getCorrelation(candidate1, candidate2);
                    if (correlation != null) {
                        float reRanking = (float) ((correlationWeight / numReRanking) * correlation
                                .getRelativeCorrelation());
                        // FIXME why dont we put the numReRanking division outside the loop?

                        assert !Double.isInfinite(reRanking);
                        assert !Double.isNaN(reRanking);

                        candidate1.increaseRegressionValue(reRanking);
                        candidate2.increaseRegressionValue(reRanking);
                    }
                    
                }
            }

        }

        // re-sort the list, as ranking weights have changed
        Collections.sort(candidates, new CandidateComparator());

        // do the scaling back to the original range (see comment above)
        double newMin = candidates.get(0).getRegressionValue();
        double newMax = candidates.get(candidates.size() - 1).getRegressionValue();

        if (newMin != newMax) { // avoid division by zero
            for (Candidate candidate : candidates) {

                // http://de.wikipedia.org/wiki/Normalisierung_(Mathematik)
                double normalized = (candidate.getRegressionValue() - newMin) * ((oldMax - oldMin) / (newMax - newMin))
                        + oldMin;
                candidate.setRegressionValue(normalized);

            }
        }

        LOGGER.trace("correlation reranking for " + candidates.size() + " in " + sw.getElapsedTimeString());

    }

    public void evaluate(String filePath) {

        LOGGER.info("starting evaluation ...");
        StopWatch sw = new StopWatch();
        final Counter counter = new Counter();
        final float[] prRcValues = new float[2];

        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] split = line.split("#");

                if (split.length < 2) {
                    return;
                }

                // the manually assigned keyphrases
                Set<String> realKeyphrases = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    realKeyphrases.add(split[i].toLowerCase());
                }
                Set<String> stemmedRealKeyphrases = stem(realKeyphrases);
                int realCount = stemmedRealKeyphrases.size();
                realKeyphrases.addAll(stemmedRealKeyphrases);

                // automatically extract keyphrases
                List<Candidate> assignedKeyphrases = extract(split[0]);
                int correctCount = 0;
                int assignedCount = assignedKeyphrases.size();

                // determine Pr/Rc values by considering assigned and real keyphrases
                for (Candidate assigned : assignedKeyphrases) {
                    for (String real : realKeyphrases) {

                        boolean correct = real.equalsIgnoreCase(assigned.getValue());
                        correct = correct || real.equalsIgnoreCase(assigned.getValue().replace(" ", ""));
                        correct = correct || real.equalsIgnoreCase(assigned.getStemmedValue());

                        if (correct) {
                            correctCount++;
                            break; // inner loop
                        }

                    }
                }

                float precision = (float) correctCount / assignedCount;
                if (Float.isNaN(precision)) {
                    precision = 0;
                }
                float recall = (float) correctCount / realCount;

                LOGGER.info("real keyphrases: " + realKeyphrases);
                LOGGER.info("assigned keyphrases: " + assignedKeyphrases);
                LOGGER.info("real: " + realCount + " assigned: " + assignedCount + " correct: " + correctCount);
                LOGGER.info("pr: " + precision + " rc: " + recall);

                prRcValues[0] += precision;
                prRcValues[1] += recall;
                counter.increment();

            }
        });

        // calculate average Pr/Rc/F1 values
        float averagePrecision = (float) prRcValues[0] / counter.getCount();
        float averageRecall = (float) prRcValues[1] / counter.getCount();
        float averageF1 = 2 * averagePrecision * averageRecall / (averagePrecision + averageRecall);

        LOGGER.info("-----------------------------------------------");
        LOGGER.info("finished evaluation in " + sw.getElapsedTimeString());
        LOGGER.info("average precision: " + averagePrecision);
        LOGGER.info("average recall: " + averageRecall);
        LOGGER.info("average f1: " + averageF1);

    }

    private DocumentModel createDocumentModel(String text) {

        DocumentModel model = new DocumentModel(corpus);
        List<Token> tokens = tokenize(text);
        model.addTokens(tokens);
        model.createCandidates();

        return model;

    }

    private List<Token> tokenize(String text) {

        List<Token> tokens = new ArrayList<Token>();
        List<Token> uniGrams = tokenizer.tokenize(text);
        List<Token> collocations = tokenizer.makeCollocations(uniGrams, 5);

        tokens.addAll(uniGrams);
        tokens.addAll(collocations);

        return tokens;

    }

    private String stem(String unstemmed) {
        stemmer.setCurrent(unstemmed.toLowerCase());
        stemmer.stem();
        return stemmer.getCurrent();
    }

    private Set<String> stem(Set<String> unstemmed) {
        Set<String> result = new HashSet<String>();
        for (String unstemmedTag : unstemmed) {
            String stem = stem(unstemmedTag);
            result.add(stem);
        }
        return result;
    }

    public static void main(String[] args) {

        /*
         * float x = (float) 0 / 12983;
         * System.out.println(x);
         * System.out.println(x == 0);
         * System.exit(0);
         */

        final KeyphraseExtractor extractor = new KeyphraseExtractor();
        String filePath = "data/tag_dataset_10000.txt";

        // //////////////////////////////////////////////
        // CORPUS CREATION
        // //////////////////////////////////////////////
//        extractor.buildCorpus(filePath);

        // //////////////////////////////////////////////
        // FEATURE SET FOR TRAINING CREATION
        // //////////////////////////////////////////////
//        extractor.buildClassifier(filePath, 1000);

        // //////////////////////////////////////////////
        // EVALUATION
        // //////////////////////////////////////////////

        extractor.loadCorpus();
        extractor.loadClassifier();
        extractor.evaluate("evaluation.txt");

        System.exit(0);

        String d1 = "If it walks like a duck and quacks like a duck, it must be a duck.";
        String d2 = "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin.";
        String d3 = "Bugs' ascension to stardom also prompted the Warner animators to recast Daffy Duck as the rabbit's rival, intensely jealous and determined to steal back the spotlight while Bugs remained indifferent to the duck's jealousy, or used it to his advantage. This turned out to be the recipe for the success of the duo.";
        String d4 = "6:25 PM 1/7/2007 blog entry: I found this great recipe for Rabbit Braised in Wine on cookingforengineers.com.";
        // String d5 =
        // "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipies for Jiaozi.";
        String d5 = "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipe for Jiaozi.";

        extractor.addToCorpus(d1);
        extractor.addToCorpus(d2);
        extractor.addToCorpus(d3);
        extractor.addToCorpus(d4);
        extractor.addToCorpus(d5);

        // System.out.println(". -> " + extractor.corpus.getInverseDocumentFrequency("."));

        DocumentModel candidates = extractor.createDocumentModel(d2); // (, 1);
        System.out.println(candidates);
        System.exit(0);

        String text2 = "the quick brown fox jumps over the lazy dog. the quick brown fox. brownfox. brownfox. brownfox. brownfox. brownfox.";
        // String text = "apple apple apples apples";
        // String text = "Apple sells phones called iPhones. The iPhone is a smart phone. Smart phones are great!";
        // String text = "iPhones iPhone iPhones";

        DocumentModel makeCandidates = extractor.createDocumentModel(text2); // , 1);
        // System.out.println(makeCandidates);
        System.out.println(makeCandidates.toCSV());
        System.exit(0);

        // List<Token> tokens = extractor.tokenize(text, -1);
        // System.out.println(tokens);
        // DocumentModel model = extractor.tokenize(text, 2);
        // System.out.println(model);
        // List<Token> tokenize2 = extractor.tokenize2(text, 3);
        // CollectionHelper.print(tokenize2);
        // DocumentModel c = extractor.makeCandidates(text, 3);
        // System.out.println(c);
        //
        // System.exit(0);

        // String x = FileHelper.readFileToString("tokenizerProblem.txt");
        // List<String> t = Tokenizer.tokenize(x);
        // System.out.println(t.size());
        //
        // DocumentModel tokenize = extractor.tokenize(x, 3);
        // Collection<Candidate> candidates = tokenize.getCandidates(2);
        // for (Candidate candidate : candidates) {
        // System.out.println(candidate);
        // }
        // //System.out.println(tokenize);
        // System.exit(0);

        // StopWatch sw = new StopWatch();
        // extractor.extractFromFile("dataset_10000.txt");
        // System.out.println(sw.getElapsedTimeString());

        System.exit(1);
        //
        // String text =
        // "the quick brown fox jumps over the lazy dog. the quick brown fox. brownfox. brownfox. brownfox. brownfox. brownfox.";
        // // List<Token> tokens = extractor.tokenize(text, -1);
        // // System.out.println(tokens);
        // DocumentModel model = extractor.extract(text, 2);
        // System.out.println(model);

    }

    @SuppressWarnings("unused")
    private static void evaluate(final KeyphraseExtractor extractor) {

        final StringBuilder sb = new StringBuilder();
        DeliciousDatasetReader reader = new DeliciousDatasetReader();

        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        filter.setMaxFileSize(600000);
        reader.setFilter(filter);

        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                String content = FileHelper.readFileToString(entry.getPath());
                content = HTMLHelper.htmlToString(content, true);
                content = StringHelper.removeControlCharacters(content);
                content = content.replace("#", " ");
                sb.append(content).append("#").append(StringUtils.join(entry.getTags().uniqueSet(), "#")).append("\n");

            }
        };
        reader.read(callback, 1000);
        FileHelper.writeToFile("evaluation.txt", sb);

    }

}
