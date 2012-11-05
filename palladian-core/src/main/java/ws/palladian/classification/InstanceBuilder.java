package ws.palladian.classification;

import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class InstanceBuilder {
    
    // TODO auto-generate feature names if not explicitly given.
    
    private final FeatureVector featureVector;

    public InstanceBuilder() {
        this.featureVector = new FeatureVector();
    }
    
    public InstanceBuilder set(String name, String value) {
        featureVector.add(new NominalFeature(name, value));
        return this;
    }
    
    public InstanceBuilder set(String name, Double value) {
        featureVector.add(new NumericFeature(name, value));
        return this;
    }
    
    public Instance create(String target) {
        Instance instance = new Instance(target, featureVector);
        return instance;
    }
    
    public FeatureVector create() {
        return featureVector;
    }

}