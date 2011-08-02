package ws.palladian.iirmodel;

/**
 * Created on: 13.09.2009 21:22:49
 */

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

/**
 * <p>
 * Represents a thread from a web forum or discussion board.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @since 1.0
 * @version 3.0
 */
@Entity
public final class ItemStream extends StreamSource {

    /**
     * <p>
     * Used for serializing this object to a file via java API.
     * </p>
     */
    private static final long serialVersionUID = 9194871722956364875L;

    /**
     * <p>
     * The items available within this item stream.
     * </p>
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
    @OrderBy("publicationDate ASC")
    private List<Item> items;

    /**
     * <p>
     * Creates a new {@link ItemStream} with no initial values. Use the provided setter methods to initialize the
     * instance.
     * </p>
     */
    protected ItemStream() {
        super();
        this.items = new LinkedList<Item>();
    }

    /**
     * <p>
     * Creates a new {@link ItemStream} with no items but all other values initialized.
     * </p>
     * 
     * @param streamSource The stream source is a system wide unique name identifying the source for a set of generated
     *            item streams. It might be the sources name as long as no other stream with the same name exists or the
     *            sources URL otherwise. For web forum threads this might be the forum name. For <a
     *            href="http://www.facebook.com">Facebook</a> it might be "facebook" or "http://facebook.com".
     * @param sourceAddress The address to access this stream. This usually is an URL but might be a file system path
     *            (in URL form or not) as well.
     * @param channelName Streams with similar content are often presented together under common name. This property
     *            provides the name of the stream channel the current item stream belongs to.
     */
    public ItemStream(String streamSource, String sourceAddress, String channelName) {
        super(streamSource, sourceAddress, channelName);
        this.items = new LinkedList<Item>();
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    /**
     * <p>
     * Adds a new {@link Item} to the end of this {@link ItemStream}s list of items. If same item already exists, it is
     * overwritten by the new one.
     * </p>
     * 
     * @param item The new item to add.
     */
    public void addItem(Item item) {
        if (items.contains(item)) {
            items.remove(item);
        }
        items.add(item);
        item.setParent(this);
    }

    /**
     * <p>
     * Adds a {@link Collection} of items to the end of this {@link ItemStream}'s list of items. If some item already
     * exists, it is overwritten by the new one.
     * </p>
     * 
     * @param items
     */
    public void addItems(Collection<Item> items) {
        for (Item item : items) {
            addItem(item);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ItemStream [identifier=");
        builder.append(getIdentifier());
        builder.append(", streamSource=");
        builder.append(getStreamSource());
        builder.append(", items=");
        builder.append(items);
        builder.append(", sourceAddress=");
        builder.append(getSourceAddress());
        builder.append(", channelName=");
        builder.append(getChannelName());
        builder.append("]");
        return builder.toString();
    }

    //
    // Attention: do not auto-generate the following methods,
    // they have been manually changed to consider the super#getSourceAddress()
    //

    @Override
    public boolean equals(Object itemStream) {
        if (this == itemStream) {
            return true;
        }
        if (itemStream == null) {
            return false;
        }
        if (getClass() != itemStream.getClass()) {
            return false;
        }

        StreamSource other = (StreamSource)itemStream;
        if (getSourceAddress() == null) {
            if (other.getSourceAddress() != null) {
                return false;
            }
        } else if (!getSourceAddress().equals(other.getSourceAddress())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSourceAddress() == null) ? 0 : getSourceAddress().hashCode());
        return result;
    }

}
