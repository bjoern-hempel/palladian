//package ws.palladian.classification.discretization;
//
//import java.util.HashSet;
//import java.util.Set;
//
//import ws.palladian.classification.text.CountingCategoryEntriesBuilder;
//import ws.palladian.core.CategoryEntries;
//import ws.palladian.core.Instance;
//import ws.palladian.core.dataset.Dataset;
//
///** @deprecated Use {@link ws.palladian.core.dataset.statistics.DatasetStatistics} instead. */
//@Deprecated
//public class DatasetStatistics {
//
//    private CategoryEntries categoryPriors;
//    
//    private Set<String> featureNames;
//
//    public DatasetStatistics(Iterable<? extends Instance> instances) {
//        CountingCategoryEntriesBuilder categoryPriorsBuilder = new CountingCategoryEntriesBuilder();
//        this.featureNames = new HashSet<>();
//        for (Instance instance : instances) {
//            categoryPriorsBuilder.add(instance.getCategory(), 1);
//            featureNames.addAll(instance.getVector().keys());
//        }
//        categoryPriors = categoryPriorsBuilder.create();
//    }
//
//    public CategoryEntries getCategoryPriors() {
//        return categoryPriors;
//    }
//    
//    public int getNumCategories() {
//    	return getCategoryPriors().size();
//    }
//    
//    /** @deprecated Replaced by {@link Dataset#getFeatureNames()}. */
//    @Deprecated
//    public Set<String> getFeatureNames() {
//        return featureNames;
//    }
//
//}
