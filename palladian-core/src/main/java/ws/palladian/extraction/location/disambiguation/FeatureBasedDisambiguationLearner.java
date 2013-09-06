package ws.palladian.extraction.location.disambiguation;

import static ws.palladian.extraction.location.PalladianLocationExtractor.LONG_ANNOTATION_SPLIT;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickdt.randomForest.RandomForestBuilder;
import ws.palladian.classification.Instance;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.extraction.location.AnnotationFilter;
import ws.palladian.extraction.location.ContextClassifier;
import ws.palladian.extraction.location.ContextClassifier.ClassificationMode;
import ws.palladian.extraction.location.ContextClassifier.ClassifiedAnnotation;
import ws.palladian.extraction.location.EntityPreprocessingTagger;
import ws.palladian.extraction.location.GeoUtils;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.extraction.location.LocationExtractorUtils.LocationDocument;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.LocationFeatureExtractor.LocationInstance;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CompositeIterator;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * This class is responsible for training models which can be used by the {@link FeatureBasedDisambiguation}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class FeatureBasedDisambiguationLearner {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedDisambiguationLearner.class);

    private final QuickDtLearner learner = new QuickDtLearner(new RandomForestBuilder().numTrees(10));

    private final LocationFeatureExtractor featureExtraction = new LocationFeatureExtractor();

    private final EntityPreprocessingTagger tagger = new EntityPreprocessingTagger(LONG_ANNOTATION_SPLIT);

    private final AnnotationFilter filter = new AnnotationFilter();

    private final ContextClassifier contextClassifier = new ContextClassifier(ClassificationMode.PROPAGATION);

    private final LocationSource locationSource;

    public FeatureBasedDisambiguationLearner(LocationSource locationSource) {
        Validate.notNull(locationSource, "locationSource must not be null");
        this.locationSource = locationSource;
    }

    public void learn(File datasetDirectory) {
        learn(LocationExtractorUtils.iterateDataset(datasetDirectory));
    }

    /**
     * <p>
     * Learn from multiple data sets.
     * </p>
     * 
     * @param datasetDirectories The directories to the training data sets, not <code>null</code>.
     */
    public void learn(File... datasetDirectories) {
        Validate.notNull(datasetDirectories, "datasetDirectories must not be null");
        List<Iterator<LocationDocument>> datasetIterators = CollectionHelper.newArrayList();
        for (File datasetDirectory : datasetDirectories) {
            datasetIterators.add(LocationExtractorUtils.iterateDataset(datasetDirectory));
        }
        learn(new CompositeIterator<LocationDocument>(datasetIterators));
    }

    public void learn(Iterator<LocationDocument> trainDocuments) {
        Set<Trainable> trainingData = createTrainingData(trainDocuments);
        String baseFileName = String.format("data/temp/location_disambiguation_%s", System.currentTimeMillis());
        ClassificationUtils.writeCsv(trainingData, new File(baseFileName + ".csv"));
        QuickDtModel model = learner.train(trainingData);
        String modelFileName = baseFileName + ".model";
        FileHelper.serialize(model, modelFileName);
    }

    private Set<Trainable> createTrainingData(Iterator<LocationDocument> trainDocuments) {
        Set<Trainable> trainingData = CollectionHelper.newHashSet();
        while (trainDocuments.hasNext()) {
            LocationDocument trainDocument = trainDocuments.next();
            String text = trainDocument.getText();
            List<LocationAnnotation> trainAnnotations = trainDocument.getAnnotations();

            List<Annotation> taggedEntities = tagger.getAnnotations(text);
            taggedEntities = filter.filter(taggedEntities);
            List<ClassifiedAnnotation> classifiedEntities = contextClassifier.classify(taggedEntities, text);
            MultiMap<ClassifiedAnnotation, Location> locations = PalladianLocationExtractor.fetchLocations(
                    locationSource, classifiedEntities);

            Set<LocationInstance> instances = featureExtraction.makeInstances(text, locations);
            Set<Trainable> trainInstances = createTrainData(instances, trainAnnotations);
            trainingData.addAll(trainInstances);
        }
        return trainingData;
    }

    private Set<Trainable> createTrainData(Set<LocationInstance> instances, List<LocationAnnotation> positiveLocations) {
        Set<Trainable> result = CollectionHelper.newHashSet();
        int numPositive = 0;
        for (LocationInstance instance : instances) {
            boolean positiveClass = false;
            for (LocationAnnotation trainAnnotation : positiveLocations) {
                // we cannot determine the correct location, if the training data did not provide coordinates
                if (instance.getLatitude() == null || instance.getLongitude() == null) {
                    continue;
                }
                Location trainLocation = trainAnnotation.getLocation();
                // XXX offsets are not considered here; necessary?
                boolean samePlace = GeoUtils.getDistance(instance, trainLocation) < 50;
                boolean sameName = instance.commonName(trainLocation);
                boolean sameType = instance.getType().equals(trainLocation.getType());
                // consider locations as positive samples, if they have same name and have max. distance of 50 kms
                if (samePlace && sameName && sameType) {
                    numPositive++;
                    positiveClass = true;
                    break;
                }
            }
            result.add(new Instance(positiveClass, instance));
        }
        double positivePercentage = MathHelper.round((float)numPositive / instances.size() * 100, 2);
        LOGGER.info("{} positive instances in {} ({}%)", numPositive, instances.size(), positivePercentage);
        return result;
    }

    public static void main(String[] args) {
        LocationSource locationSource = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        FeatureBasedDisambiguationLearner learner = new FeatureBasedDisambiguationLearner(locationSource);
        File datasetTud = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/1-training");
        File datasetLgl = new File("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/1-train");
        File datasetClust = new File("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/CLUST-converted/1-train");
        learner.learn(datasetTud);
        learner.learn(datasetLgl);
        learner.learn(datasetClust);
        learner.learn(datasetTud, datasetLgl, datasetClust);
        // dataset = new File("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/2-validation");
        // learner.learn(dataset);
    }

}