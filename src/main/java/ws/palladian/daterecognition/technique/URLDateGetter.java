package ws.palladian.daterecognition.technique;

import java.util.ArrayList;

import ws.palladian.daterecognition.DateConverter;
import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.URLDate;
import ws.palladian.helper.RegExp;

/**
 * 
 * This class searches for dates in a url-tring.<br>
 * Therefore it uses other regular expression then other techniques.
 * 
 * @author Martin Gregor
 * 
 */
public class URLDateGetter extends TechniqueDateGetter<URLDate> {

    @Override
    public ArrayList<URLDate> getDates() {
        ArrayList<URLDate> result = new ArrayList<URLDate>();
        if (url != null) {
            result.add(getURLDate(url));
        }
        return result;
    }

    /**
     * An url has only one date. So first date is this one. <br>
     * Use setUrl before.
     * @return
     */
    public URLDate getFirstDate() {

        URLDate date = new URLDate();
        if (url != null) {
            date = getURLDate(url);
        }
        return date;

    }
    /**
     * An url has only one date. So first date is this one.
     * 
     * @return
     */
    public URLDate getFirstDate(String url) {

        URLDate date = new URLDate();
        if (url != null) {
            date = getURLDate(url);
        }
        return date;

    }

    /**
     *Looks up for a date in the URL.
     * 
     * @param url
     * @return a extracted Date
     */
    private URLDate getURLDate(String url) {
        ExtractedDate date = null;
        URLDate temp = null;
        Object[] regExpArray = RegExp.getURLRegExp();
        int index = 0;
        while (date == null && index < regExpArray.length) {
            date = DateGetterHelper.getDateFromString(url, (String[]) regExpArray[index]);
            index++;
        }
        if (date != null) {
            temp = DateConverter.convert(date, DateConverter.TECH_URL);
            temp.setUrl(url);
        }
        return temp;
    }

}