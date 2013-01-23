package ws.palladian.extraction.location;

import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.PositionAnnotation;

public class Location extends PositionAnnotation {

    private static final String LOCATION_ANNOTATION_NAME = "Location";

    private List<String> names = CollectionHelper.newArrayList();
    private String type;
    private Double latitude;
    private Double longitude;
    private Integer population;

    public Location() {
        // FIXME
        super(LOCATION_ANNOTATION_NAME, 0, 1, 0, "");
    }

    public Location(PositionAnnotation annotation) {
        super(annotation);
    }

    public String getLocationName() {
        if (names.isEmpty()) {
            return "";
        }
        return names.iterator().next();
    }

    public List<String> getLocationNames() {
        return names;
    }

    /**
     * <p>
     * Set the name(s) of this location. Of multiple names exist, the primary name is per definitionem the first in the
     * list.
     * </p>
     * 
     * @param names
     */
    public void setNames(List<String> names) {
        this.names = names;
    }

    public void addName(String name) {
        this.names.add(name);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getPopulation() {
        return population;
    }

    public void setPopulation(Integer population) {
        this.population = population;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Location [names=");
        builder.append(names);
        builder.append(", type=");
        builder.append(type);
        builder.append(", latitude=");
        builder.append(latitude);
        builder.append(", longitude=");
        builder.append(longitude);
        builder.append(", population=");
        builder.append(population);
        builder.append(", getStartPosition()=");
        builder.append(getStartPosition());
        builder.append(", getEndPosition()=");
        builder.append(getEndPosition());
        builder.append(", getName()=");
        builder.append(getName());
        builder.append(", getValue()=");
        builder.append(getValue());
        builder.append("]");
        return builder.toString();
    }

}
