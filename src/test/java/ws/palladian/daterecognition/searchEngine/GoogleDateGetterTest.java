package ws.palladian.daterecognition.searchEngine;

import static org.junit.Assert.*;

import org.junit.Test;

import ws.palladian.control.AllTests;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.searchengine.GoogleDateGetter;

public class GoogleDateGetterTest {

	@Test
	public void testGetGoogleDate() {
		if(AllTests.ALL_TESTS){
			String url = "http://www.spiegel.de/politik/deutschland/0,1518,731921,00.html";
			//url = "http://www.patrickswayze.net/";
			GoogleDateGetter gd = new GoogleDateGetter();
			ExtractedDate date = gd.getGoogleDate(url);
			
			assertEquals("2010-11-30", date.getNormalizedDateString());
		}
	}

}