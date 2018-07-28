package ws.palladian.kaggle.redhat.dataset.transformers;

import java.util.Objects;

import ws.palladian.core.AppendedVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.Value;

/**
 * Take an arbitrary {@link Value} and transform it to a {@link NominalValue}
 * through its {@link Value#toString()} method.
 * 
 * @author pk
 *
 */
public final class ToNominalValueTransformer extends AbstractDatasetFeatureVectorTransformer {
	private final String valueName;

	public ToNominalValueTransformer(String valueName) {
		this.valueName = Objects.requireNonNull(valueName, "valueName must not be null");
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		return new FeatureInformationBuilder().add(featureInformation).set(valueName + "_nominal", NominalValue.class)
				.create();
	}

	@Override
	public FeatureVector compute(FeatureVector featureVector) {
		String nominalValue = featureVector.get(valueName).toString();
		FeatureVector appendedVector = new InstanceBuilder().set(valueName + "_nominal", nominalValue).create();
		return new AppendedVector(featureVector, appendedVector);
	}

}