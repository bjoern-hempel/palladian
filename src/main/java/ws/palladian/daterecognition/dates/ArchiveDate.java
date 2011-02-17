package ws.palladian.daterecognition.dates;

/**
 * Represents a date found in an archive.
 * 
 * @author Martin Greogr
 * 
 */
public class ArchiveDate extends ExtractedDate {

    @Override
    public int getType() {
        return TECH_ARCHIVE;
    }
}