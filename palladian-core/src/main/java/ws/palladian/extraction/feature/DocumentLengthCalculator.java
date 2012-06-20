/**
 * Created on: 16.06.2012 19:16:09
 */
package ws.palladian.extraction.feature;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * Calculates the length of a {@link PipelineDocument}s content and provides this length as a {@link NumericFeature} of
 * the document.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class DocumentLengthCalculator extends StringDocumentPipelineProcessor {

    /**
     * <p>
     * Used for serializing objects of this class. This should only change if the attribute set of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -1616045495717647235L;

    /**
     * <p>
     * The identifier for the {@link Feature} extracted by this {@link PipelineProcessor}.
     * </p>
     */
    public static final FeatureDescriptor<NumericFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            "ws.palladian.documentlength", NumericFeature.class);

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        int length = document.getContent().length();
        double doubleValue = Integer.valueOf(length).doubleValue();
        NumericFeature feature = new NumericFeature(PROVIDED_FEATURE_DESCRIPTOR, doubleValue);
        document.addFeature(feature);
    }

}