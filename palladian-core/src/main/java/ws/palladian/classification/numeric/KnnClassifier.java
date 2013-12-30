package ws.palladian.classification.numeric;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.utils.MinMaxNormalizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.collection.EntryValueComparator;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A KNN (k-nearest neighbor) classifier. It classifies {@link FeatureVector}s based on the k nearest {@link Instance}s
 * from a {@link KnnModel} created by a {@link KnnLearner}. Since this is an instance based classifier, it is fast
 * during the learning phase but has a more complicated prediction phase.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class KnnClassifier implements Classifier<KnnModel> {

    /**
     * <p>
     * Number of nearest neighbors that are allowed to vote. If neighbors have the same distance they will all be
     * considered for voting, k might increase in these cases.
     * </p>
     */
    private final int k;

    /**
     * <p>
     * Creates a new completely initialized KNN classifier with specified k using a {@link MinMaxNormalizer}. A typical
     * value for k is 3. This constructor should be used if the created object is used for prediction.
     * </p>
     * 
     * @param k The parameter k specifying the k nearest neighbors to use for classification. Must be greater zero.
     */
    public KnnClassifier(int k) {
        Validate.isTrue(k > 0, "k must be greater zero");
        this.k = k;
    }

    /**
     * <p>
     * Creates a new completely initialized KNN classifier with a k of 3 and a {@link MinMaxNormalizer}. This
     * constructor should typically be used if the class is used for learning. In that case the value of k is not
     * important. It is only used during prediction.
     * </p>
     */
    public KnnClassifier() {
        this(3);
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, KnnModel model) {

        model.getNormalization().normalize(classifiable);
        
        // initialize with all category names and a score of zero
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder().set(model.getCategories(), 0);

        // find k nearest neighbors, compare instance to every known instance
        List<Pair<Trainable, Double>> neighbors = CollectionHelper.newArrayList();
        for (Trainable example : model.getTrainingExamples()) {
            double distance = getDistanceBetween(classifiable.getFeatureVector(), example.getFeatureVector());
            neighbors.add(Pair.of(example, distance));
        }

        // sort near neighbor map by distance
        Collections.sort(neighbors, new EntryValueComparator<Double>(Order.ASCENDING));

        // if there are several instances at the same distance we take all of them into the voting, k might get bigger
        // in those cases
        double lastDistance = -1;
        int ck = 0;
        for (Pair<Trainable, Double> neighbor : neighbors) {

            if (ck >= k && neighbor.getValue() != lastDistance) {
                break;
            }

            double distance = neighbor.getValue();
            double weight = 1.0 / (distance + 0.000000001);
            String targetClass = neighbor.getKey().getTargetClass();
            builder.add(targetClass, weight);

            lastDistance = distance;
            ck++;
        }

        return builder.create();
    }

    /**
     * <p>
     * Distance function, the shorter the distance the more important the category of the known instance. Euclidean
     * Distance = sqrt(SUM_0,n (i1-i2)²)
     * </p>
     * 
     * @param vector The instance to classify.
     * @param featureVector The instance in the vector space with known categories.
     * @return distance The Euclidean distance between the two instances in the vector space.
     */
    private double getDistanceBetween(FeatureVector vector, FeatureVector featureVector) {

        // XXX factor this distance measure out to a strategy class.

        double squaredSum = 0;

        Collection<NumericFeature> instanceFeatures = vector.getAll(NumericFeature.class);

        for (NumericFeature instanceFeature : instanceFeatures) {
            squaredSum += Math.pow(
                    instanceFeature.getValue()
                            - featureVector.get(NumericFeature.class, instanceFeature.getName()).getValue(), 2);
        }

        return Math.sqrt(squaredSum);
    }

}
