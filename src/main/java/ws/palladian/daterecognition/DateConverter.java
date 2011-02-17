package ws.palladian.daterecognition;

import ws.palladian.daterecognition.dates.ArchiveDate;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.daterecognition.dates.HeadDate;
import ws.palladian.daterecognition.dates.ReferenceDate;
import ws.palladian.daterecognition.dates.StructureDate;
import ws.palladian.daterecognition.dates.URLDate;

public class DateConverter {
    public static final int TECH_URL = ExtractedDate.TECH_URL;
    public static final int TECH_HTTP_HEADER = ExtractedDate.TECH_HTTP_HEADER;
    public static final int TECH_HTML_HEAD = ExtractedDate.TECH_HTML_HEAD;
    public static final int TECH_HTML_STRUC = ExtractedDate.TECH_HTML_STRUC;
    public static final int TECH_HTML_CONT = ExtractedDate.TECH_HTML_CONT;
    public static final int TECH_REFERENCE = ExtractedDate.TECH_REFERENCE;
    public static final int TECH_ARCHIVE = ExtractedDate.TECH_ARCHIVE;

    /**
     * Converts an extracted date into a specific one. <br>
     * Use technique flags of this class or {@linkplain ExtractedDate}.
     * 
     * @param <T>
     * @param date
     * @param techniqueFlag
     * @return
     */
    public static <T> T convert(ExtractedDate date, int techniqueFlag) {
        T newDate = null;
        if (date != null) {
            switch (techniqueFlag) {
                case TECH_ARCHIVE:
                    newDate = (T) new ArchiveDate();
                    break;
                case TECH_URL:
                    newDate = (T) new URLDate();
                    break;
                case TECH_HTTP_HEADER:
                    newDate = (T) new HTTPDate();
                    break;
                case TECH_HTML_HEAD:
                    newDate = (T) new HeadDate();
                    break;
                case TECH_HTML_STRUC:
                    newDate = (T) new StructureDate();
                    break;
                case TECH_HTML_CONT:
                    newDate = (T) new ContentDate();
                    break;
                case TECH_REFERENCE:
                    newDate = (T) new ReferenceDate();
                    break;

            }
            ((ExtractedDate) newDate).setAll(date.getAll());
        }
        return newDate;
    }
}