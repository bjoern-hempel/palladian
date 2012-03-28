package ws.palladian.extraction.feature;

import ws.palladian.extraction.PipelineProcessor;

/**
 * <p>
 * A {@link PipelineProcessor} for removing tokens based on the length from a pre-tokenized text. This means, the
 * documents to be processed by this class must be processed by a Tokenizer in advance, supplying
 * Tokenizer.PROVIDED_FEATURE annotations.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public class LengthTokenRemover extends TokenRemover {

    private static final long serialVersionUID = 1L;

    private final int minLength;
    private final int maxLength;

    /**
     * <p>
     * Creates a new {@link LengthTokenRemover} with the specified minimum and maximum lengths.
     * </p>
     * 
     * @param minLength Minimum length for a token to be accepted.
     * @param maxLength Maximum length for a token to be accepted.
     */
    public LengthTokenRemover(int minLength, int maxLength) {
        super();
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    /**
     * <p>
     * Creates a new {@link LengthTokenRemover} with the specified minimum length and no limitation for the maximum
     * length.
     * </p>
     * 
     * @param minLength Minimum length for a token to be accepted.
     */
    public LengthTokenRemover(int minLength) {
        this(minLength, Integer.MAX_VALUE);
    }

    @Override
    protected boolean remove(Annotation annotation) {
        int length = annotation.getValue().length();
        return length < minLength || length > maxLength;
    }

}