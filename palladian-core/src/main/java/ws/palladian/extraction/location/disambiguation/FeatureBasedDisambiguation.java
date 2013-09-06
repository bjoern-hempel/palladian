package ws.palladian.extraction.location.disambiguation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.extraction.location.ContextClassifier.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.disambiguation.LocationFeatureExtractor.LocationInstance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * A disambiguation approach using machine learning. The required models can be created using the
 * {@link FeatureBasedDisambiguationLearner}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class FeatureBasedDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedDisambiguation.class);

    public static final double PROBABILITY_THRESHOLD = 0.15;

    private final double probabilityThreshold;

    private final QuickDtClassifier classifier = new QuickDtClassifier();

    private final LocationFeatureExtractor featureExtractor = new LocationFeatureExtractor();

    private final QuickDtModel model;

    public FeatureBasedDisambiguation(QuickDtModel model) {
        this(model, PROBABILITY_THRESHOLD);
    }

    public FeatureBasedDisambiguation(QuickDtModel model, double probabilityThreshold) {
        Validate.notNull(model, "model must not be null");
        Validate.inclusiveBetween(0., 1., probabilityThreshold,
                "probabilityThreshold must be between inclusive 0 and 1.");
        this.model = model;
        this.probabilityThreshold = probabilityThreshold;
    }

    @Override
    public List<LocationAnnotation> disambiguate(String text, MultiMap<ClassifiedAnnotation, Location> locations) {

        Set<LocationInstance> instances = featureExtractor.makeInstances(text, locations);
        Map<Integer, Double> scoredLocations = CollectionHelper.newHashMap();

        for (LocationInstance instance : instances) {
            CategoryEntries classification = classifier.classify(instance, model);
            scoredLocations.put(instance.getId(), classification.getProbability("true"));
        }

        List<LocationAnnotation> result = CollectionHelper.newArrayList();
        for (Annotation annotation : locations.keySet()) {
            Collection<Location> candidates = locations.get(annotation);

            double highestScore = 0;
            Location selectedLocation = null;

            for (Location location : candidates) {
                double score = scoredLocations.get(location.getId());
                if (score > highestScore) {
                    highestScore = score;
                    selectedLocation = location;
                }
            }

            if (selectedLocation != null && highestScore >= probabilityThreshold) {
                result.add(new LocationAnnotation(annotation, selectedLocation));
                Object[] logArgs = new Object[] {annotation.getValue(), highestScore, selectedLocation};
                LOGGER.debug("[+] '{}' was classified as location with {}: {}", logArgs);
            } else {
                LOGGER.debug("[-] '{}' was classified as no location with {}", annotation.getValue(), highestScore);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureBasedDisambiguation [probabilityThreshold=");
        builder.append(probabilityThreshold);
        builder.append("]");
        return builder.toString();
    }

}