package ws.palladian.model.features;

/**
 * <p>
 * The base class for all features used by different Information Retrieval and Extraction components inside Palladian. A
 * {@code Feature} can be any information from or about a document that is helpful to guess correct information about
 * that particular document.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @see ws.palladian.classification.Classifier
 * @param <T>
 *            The data type used to represent this {@code Feature}'s value.
 */
public class Feature<T> {
    /**
     * <p>
     * The {@link FeatureVector} wide unique identifier of this {@code Feature}.
     * </p>
     */
    private String name;
    /**
     * <p>
     * The {@code Feature}'s value containing concrete extracted data from a document.
     * </p>
     */
    private T value;

    /**
     * <p>
     * Creates a new {@code Feature} with all attributes initialized.
     * </p>
     * 
     * @param name
     *            The {@link FeatureVector} wide unique identifier of this {@code Feature}.
     * @param value
     *            The {@code Feature}'s value containing concrete extracted data
     *            from a document.
     */
    public Feature(String name, T value) {
        super();
        this.name = name;
        this.value = value;
    }

    /**
     * <p>
     * Provides the {@link FeatureVector} wide unique identifier of this {@code Feature}.
     * </p>
     * 
     * @return The string representing this {@code Feature}s identifier.
     */
    public final String getName() {
        return name;
    }

    /**
     * <p>
     * Resets this {@code Feature}'s identifier overwriting the old one. Use with care!
     * </p>
     * 
     * @param name
     *            The {@link FeatureVector} wide unique identifier of this {@code Feature}.
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * <p>
     * Provides the {@code Feature}'s value containing concrete extracted data from a document.
     * </p>
     * 
     * @return The {@code Feature}'s value
     */
    public final T getValue() {
        return value;
    }

    /**
     * <p>
     * Resets and overwrites the {@code Feature}'s value.
     * </p>
     * 
     * @param value
     *            The {@code Feature}'s value containing concrete extracted data
     *            from a document.
     */
    public final void setValue(T value) {
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Feature [name=");
        builder.append(name);
        builder.append(", value=");
        builder.append(value);
        builder.append("]");
        return builder.toString();
    }

}
