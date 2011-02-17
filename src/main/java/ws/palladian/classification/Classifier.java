package ws.palladian.classification;

import java.io.Serializable;

import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.FeatureSetting;

public abstract class Classifier<T> implements Serializable {

    /** The serialize version ID. */
    private static final long serialVersionUID = -7017462894898815981L;

    /** A classifier has a name. */
    private String name = "";

    /** A classifier has training documents. */
    private transient Instances<T> trainingInstances = new Instances<T>();

    /** A classifier has test documents that can be used to calculate recall, precision, and F-score. */
    private transient Instances<T> testInstances = new Instances<T>();

    /** A classifier classifies to certain categories. */
    protected Categories categories;

    /**
     * Configurations for the classification type ({@link ClassificationTypeSetting.SINGLE},
     * {@link ClassificationTypeSetting.HIERARCHICAL}, or {@link ClassificationTypeSetting.TAG}).
     */
    protected ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();

    /** The feature settings which should be used by the text classifier. */
    private FeatureSetting featureSetting = new FeatureSetting();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Categories getCategories() {
        return categories;
    }

    public void setCategories(Categories categories) {
        this.categories = categories;
    }

    public void setClassificationTypeSetting(ClassificationTypeSetting classificationTypeSetting) {
        this.classificationTypeSetting = classificationTypeSetting;
    }

    public ClassificationTypeSetting getClassificationTypeSetting() {
        return classificationTypeSetting;
    }

    public int getClassificationType() {
        return getClassificationTypeSetting().getClassificationType();
    }

    public void setFeatureSetting(FeatureSetting featureSetting) {
        this.featureSetting = featureSetting;
    }

    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

    public Instances<T> getTrainingInstances() {
        return trainingInstances;
    }

    public void setTrainingInstances(Instances<T> trainingInstances) {
        this.trainingInstances = trainingInstances;
        getPossibleCategories(trainingInstances);
    }

    public Instances<T> getTestInstances() {
        return testInstances;
    }

    public void setTestInstances(Instances<T> testInstances) {
        this.testInstances = testInstances;
    }

    /**
     * After training instances have been assigned, we can find out which nominal categories are possible for the
     * classifier to classify.
     */
    protected void getPossibleCategories(Instances<T> instances) {
        if (getCategories() == null) {
            setCategories(new Categories());
        }
        for (T instance : instances) {
            String categoryName = ((Instance) instance).getInstanceCategory().getName();
            Category category = getCategories().getCategoryByName(categoryName);
            if (category == null) {
                category = new Category(categoryName);
                category.increaseFrequency();
                getCategories().add(category);
            } else {
                category.increaseFrequency();
            }
        }
        getCategories().calculatePriors();
    }

    public abstract void save(String classifierPath);

}