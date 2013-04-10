package ws.palladian.classification;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * A {@link CategoryEntries} implementation which uses a {@link Map} internally.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class CategoryEntriesMap implements CategoryEntries {

    private final Map<String, Double> entryMap;

    /**
     * <p>
     * Create a new and empty {@link CategoryEntriesMap}. New entries can be added using {@link #set(String, double)}.
     * </p>
     */
    public CategoryEntriesMap() {
        entryMap = CollectionHelper.newHashMap();
    }

    /**
     * <p>
     * Create a new {@link CategoryEntriesMap} by merging multiple {@link CategoryEntries} instances.
     * </p>
     * 
     * @param categoryEntries The category entries, not <code>null</code>.
     * @return The merged {@link CategoryEntriesMap}.
     */
    public static CategoryEntriesMap merge(CategoryEntries... categoryEntries) {
        Validate.notNull(categoryEntries, "categoryEntries must not be null");
        Map<String, Double> valueMap = LazyMap.create(ConstantFactory.create(0.));
        for (CategoryEntries entries : categoryEntries) {
            for (String category : entries) {
                Double value = valueMap.get(category);
                valueMap.put(category, value + entries.getProbability(category));
            }
        }
        CategoryEntriesMap result = new CategoryEntriesMap();
        for (String category : valueMap.keySet()) {
            result.set(category, valueMap.get(category));
        }
        return result;
    }

    /**
     * <p>
     * Create a new {@link CategoryEntriesMap} by copying an existing one.
     * </p>
     * 
     * @param categoryEntries The {@link CategoryEntries} object to copy, not <code>null</code>.
     */
    public CategoryEntriesMap(CategoryEntries categoryEntries) {
        Validate.notNull(categoryEntries, "categoryEntries must not be null");
        entryMap = CollectionHelper.newHashMap();
        for (String categoryName : categoryEntries) {
            entryMap.put(categoryName, categoryEntries.getProbability(categoryName));
        }
    }

    /**
     * <p>
     * Create a new {@link CategoryEntriesMap} from a given {@link Map} with category and probability values. The values
     * are normalized, so that the sum of all probabilities is one.
     * </p>
     * 
     * @param map The map with categories and probabilities, not <code>null</code>.
     */
    public CategoryEntriesMap(Map<String, Double> map) {
        Validate.notNull(map, "map must not be null");
        entryMap = CollectionHelper.newHashMap();
        double sum = 0;
        for (Double probability : map.values()) {
            sum += probability;
        }
        for (String categoryName : map.keySet()) {
            entryMap.put(categoryName, map.get(categoryName) / sum);
        }
    }

    @Override
    public double getProbability(String categoryName) {
        Validate.notNull(categoryName, "categoryName must not be null");
        Double result = entryMap.get(categoryName);
        if (result == null) {
            return 0;
        }
        return result;
    }

    /**
     * <p>
     * Set the probability of a category name.
     * </p>
     * 
     * @param categoryName The name of the category, not <code>null</code>.
     * @param probability The associated probability, higher or equal zero.
     */
    public void set(String categoryName, double probability) {
        Validate.notNull(categoryName, "categoryName must not be null");
        Validate.isTrue(probability >= 0, "probability must be higher/equal zero");
        entryMap.put(categoryName, probability);
    }

    @Override
    public String getMostLikelyCategory() {
        double maxProbability = -1;
        String maxName = null;
        for (Entry<String, Double> entry : entryMap.entrySet()) {
            if (entry.getValue() > maxProbability) {
                maxProbability = entry.getValue();
                maxName = entry.getKey();
            }
        }
        return maxName;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("CategoryEntriesMap [");
        boolean first = true;
        for (String categoryName : this) {
            if (first) {
                first = false;
            } else {
                toStringBuilder.append(", ");
            }
            toStringBuilder.append(categoryName);
            toStringBuilder.append("=");
            toStringBuilder.append(MathHelper.round(getProbability(categoryName), 4));
        }
        toStringBuilder.append("]");
        return toStringBuilder.toString();
    }

    @Override
    public Iterator<String> iterator() {
        return entryMap.keySet().iterator();
    }

}
