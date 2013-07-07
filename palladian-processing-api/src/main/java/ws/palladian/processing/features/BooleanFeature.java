/**
 * Created on: 13.06.2012 14:03:39
 */
package ws.palladian.processing.features;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class BooleanFeature extends AbstractFeature<Boolean> {

    /**
     * @param name
     * @param value
     */
    public BooleanFeature(String name, boolean value) {
        super(name, value);
    }

}
