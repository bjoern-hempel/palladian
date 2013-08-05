package ws.palladian.extraction.location;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * @author Philipp Katz
 */
public final class LocationExtractorUtils {

    public static String normalizeName(String value) {
        if (value.matches("([A-Z]\\.)+")) {
            value = value.replace(".", "");
        }
        value = value.replaceAll("[©®™]", "");
        value = value.replaceAll("\\s+", " ");
        if (value.equals("US")) {
            value = "U.S.";
        }
        return value;
    }

    public static boolean isDescendantOf(Location child, Location parent) {
        return child.getAncestorIds().contains(parent.getId());
    }

    public static boolean isChildOf(Location child, Location parent) {
        Integer firstId = CollectionHelper.getFirst(child.getAncestorIds());
        if (firstId == null) {
            return false;
        }
        return firstId == parent.getId();
    }

    /**
     * <p>
     * Get the biggest {@link Location} from the given {@link Collection}.
     * </p>
     * 
     * @param locations The locations.
     * @return The {@link Location} with the highest population, or <code>null</code> in case the collection was empty,
     *         or none of the locations has a population specified.
     */
    public static Location getBiggest(Collection<Location> locations) {
        Validate.notNull(locations, "locations must not be null");
        Location biggest = null;
        for (Location location : locations) {
            Long population = location.getPopulation();
            if (population == null) {
                continue;
            }
            if (biggest == null || population > biggest.getPopulation()) {
                biggest = location;
            }
        }
        return biggest;
    }

    /**
     * <p>
     * Get the highest population from the given {@link Collection} of {@link Location}s.
     * </p>
     * 
     * @param locations The locations, not <code>null</code>.
     * @return The count of the highest population, or zero, in case the collection was empty or non of the locations
     *         had a population value.
     */
    public static long getHighestPopulation(Collection<Location> locations) {
        Validate.notNull(locations, "locations must not be null");
        Location biggestLocation = getBiggest(locations);
        if (biggestLocation == null || biggestLocation.getPopulation() == null) {
            return 0;
        }
        return biggestLocation.getPopulation();
    }

    /**
     * <p>
     * For each pair in the given Collection of {@link Location}s determine the distance, and return the highest
     * distance.
     * </p>
     * 
     * @param locations {@link Collection} of {@link Location}s, not <code>null</code>.
     * @return The maximum distance between any pair in the given {@link Collection}, or zero in case the collection wsa
     *         empty.
     */
    public static double getLargestDistance(Collection<Location> locations) {
        double largestDistance = 0;
        List<Location> temp = new ArrayList<Location>(locations);
        for (int i = 0; i < temp.size(); i++) {
            Location l1 = temp.get(i);
            for (int j = i + 1; j < temp.size(); j++) {
                Location l2 = temp.get(j);
                largestDistance = Math.max(largestDistance, GeoUtils.getDistance(l1, l2));
            }
        }
        return largestDistance;
    }

    public static <T> Set<T> filterConditionally(Collection<T> set, Filter<T> filter) {
        Set<T> temp = new HashSet<T>(set);
        CollectionHelper.remove(temp, filter);
        return temp.size() > 0 ? temp : new HashSet<T>(set);
    }

    /**
     * <p>
     * Check, whether two {@link Location}s share a common name. Names are normalized according to the rules given in
     * {@link #normalizeName(String)}.
     * </p>
     * 
     * @param l1 First location, not <code>null</code>.
     * @param l2 Second location, not <code>null</code>.
     * @return <code>true</code>, if a common name exists, <code>false</code> otherwise.
     */
    public static boolean commonName(Location l1, Location l2) {
        Set<String> names1 = collectNames(l1);
        Set<String> names2 = collectNames(l2);
        names1.retainAll(names2);
        return names1.size() > 0;
    }

    public static Set<String> collectNames(Location location) {
        Set<String> names = CollectionHelper.newHashSet();
        names.add(normalizeName(location.getPrimaryName()));
        for (AlternativeName alternativeName : location.getAlternativeNames()) {
            names.add(normalizeName(alternativeName.getName()));
        }
        return names;
    }

    /**
     * <p>
     * Get an {@link Iterator} for the TUD-Loc dataset.
     * </p>
     * 
     * @param datasetDirectory Path to the dataset directory containing the annotated text files and a
     *            <code>coordinates.csv</code> file, not <code>null</code>.
     * @return An iterator for the dataset.
     */
    public static Iterator<LocationDocument> iterateDataset(File datasetDirectory) {
        List<File> files = Arrays.asList(FileHelper.getFiles(datasetDirectory.getPath(), "text"));
        File coordinateFile = new File(datasetDirectory, "coordinates.csv");

        final Iterator<File> fileIterator = files.iterator();
        final Map<String, Map<Integer, GeoCoordinate>> coordinates = readCoordinates(coordinateFile);
        final int numFiles = files.size();

        return new Iterator<LocationDocument>() {
            ProgressMonitor monitor = new ProgressMonitor(numFiles, 0);
            @Override
            public boolean hasNext() {
                return fileIterator.hasNext();
            }

            @Override
            public LocationDocument next() {
                monitor.incrementAndPrintProgress();
                File currentFile = fileIterator.next();
                String rawText = FileHelper.readFileToString(currentFile).replace(" role=\"main\"", "");
                String cleanText = HtmlHelper.stripHtmlTags(rawText);
                Map<Integer, GeoCoordinate> currentCoordinates = coordinates.get(currentFile.getName());
                List<LocationAnnotation> annotations = getAnnotations(rawText, currentCoordinates);
                return new LocationDocument(currentFile.getName(), cleanText, annotations);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    }

    /**
     * <p>
     * Read a coordinates CSV file from TUD-Loc dataset. The coordinates file contains the following columns:
     * <code>docId;idx;offset;latitude;longitude;sourceId</code>. <code>docId</code> specifies the filename,
     * <code>idx</code> is a running index for the annotations, starting with zero, <code>offset</code> is the character
     * offset from the beginning of the text, starting with zero, <code>latitude</code> and <code>longitude</code>
     * specify the coordinates, but may be empty, <code>sourceId</code> is a unique, source specific identifier for the
     * location.
     * </p>
     * 
     * @param coordinateFile The path to the coordinate file, not <code>null</code>.
     * @return A nested map; first key is the docId, second key is the character offset, value are {@link GeoCoordinate}
     *         s. In case, the coordinates did not specify longitude/latitude values, the values in the GeoCoordinate
     *         are also <code>null</code>.
     */
    public static Map<String, Map<Integer, GeoCoordinate>> readCoordinates(File coordinateFile) {
        Validate.notNull(coordinateFile, "coordinateFile must not be null");
        final Map<String, Map<Integer, GeoCoordinate>> coordinateMap = LazyMap
                .create(new Factory<Map<Integer, GeoCoordinate>>() {
                    @Override
                    public Map<Integer, GeoCoordinate> create() {
                        return CollectionHelper.newTreeMap();
                    }
                });
        int lines = FileHelper.performActionOnEveryLine(coordinateFile, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber == 0) {
                    return;
                }
                String[] split = StringUtils.splitPreserveAllTokens(line, ";");
                String documentName = split[0];
                int offset = Integer.valueOf(split[2]);
                GeoCoordinate coordinate = null;
                if (!split[3].isEmpty() && !split[4].isEmpty()) {
                    double lat = Double.valueOf(split[3]);
                    double lng = Double.valueOf(split[4]);
                    coordinate = new ImmutableGeoCoordinate(lat, lng);
                }
                coordinateMap.get(documentName).put(offset, coordinate);
            }
        });
        if (lines == -1) {
            throw new IllegalStateException("Could not read " + coordinateFile);
        }
        return coordinateMap;
    }

    private static List<LocationAnnotation> getAnnotations(String rawText, Map<Integer, GeoCoordinate> coordinates) {
        List<LocationAnnotation> annotations = CollectionHelper.newArrayList();
        Annotations<ContextAnnotation> xmlAnnotations = FileFormatParser.getAnnotationsFromXmlText(rawText);
        for (ContextAnnotation xmlAnnotation : xmlAnnotations) {
            int dummyId = xmlAnnotation.getValue().hashCode();
            String name = xmlAnnotation.getValue();
            GeoCoordinate coordinate = coordinates.get(xmlAnnotation.getStartPosition());
            Double lat = coordinate != null ? coordinate.getLongitude() : null;
            Double lng = coordinate != null ? coordinate.getLatitude() : null;
            LocationType type = LocationType.map(xmlAnnotation.getTag());
            Location location = new ImmutableLocation(dummyId, name, type, lng, lat, 0l);
            annotations.add(new LocationAnnotation(xmlAnnotation, location));
        }
        return annotations;
    }

    /**
     * <p>
     * Check, whether the given {@link Collection} contains a {@link Location} of one of the specified
     * {@link LocationType}s.
     * </p>
     * 
     * @param locations The locations, not <code>null</code>.
     * @param types The {@link LocationType}s for which to check.
     * @return <code>true</code> in case there is at least one location of the specified types, <code>false</code>
     *         otherwise.
     */
    public static boolean containsType(Collection<Location> locations, LocationType... types) {
        for (LocationType type : types) {
            for (Location location : locations) {
                if (location.getType() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * Check, whether at least two of the given locations in the {@link Collection} have different names (i.e. the
     * intersection of all names of each {@link Location} is empty).
     * </p>
     * 
     * @param locations The locations, not <code>null</code>.
     * @return <code>true</code> in case there is at least one pair in the given collection which does not share at
     *         least one name.
     */
    public static boolean differentNames(Collection<Location> locations) {
        Set<String> allNames = CollectionHelper.newHashSet();
        for (Location location : locations) {
            Set<String> currentNames = collectNames(location);
            if (allNames.size() > 0) {
                Set<String> tempIntersection = new HashSet<String>(allNames);
                tempIntersection.retainAll(currentNames);
                if (tempIntersection.isEmpty()) {
                    return true;
                }
            }
            allNames.addAll(currentNames);
        }
        return false;
    }

    public static boolean sameNames(Collection<Location> locations) {
        return !differentNames(locations);
    }

    public static class LocationTypeFilter implements Filter<Location> {

        private final LocationType type;

        public LocationTypeFilter(LocationType type) {
            this.type = type;
        }

        @Override
        public boolean accept(Location item) {
            return item.getType() == type;
        }

    }

    public static class CoordinateFilter implements Filter<Location> {
        @Override
        public boolean accept(Location item) {
            return item.getLatitude() != null && item.getLongitude() != null;
        }

    }

    public static class LocationDocument {

        private final String fileName;
        private final String text;
        private final List<LocationAnnotation> annotations;

        public LocationDocument(String fileName, String text, List<LocationAnnotation> annotations) {
            this.fileName = fileName;
            this.text = text;
            this.annotations = annotations;
        }

        public String getFileName() {
            return fileName;
        }

        public String getText() {
            return text;
        }

        public List<LocationAnnotation> getAnnotations() {
            return annotations;
        }

    }

    private LocationExtractorUtils() {
        // thou shalt not instantiate
    }

}
