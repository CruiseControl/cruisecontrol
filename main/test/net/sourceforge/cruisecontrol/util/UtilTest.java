package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;
import java.util.Calendar;

/**
 * @author jfredrick
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class UtilTest extends TestCase {

	private Calendar cal;
	private Calendar cal2;

	/**
	 * Constructor for UtilTest.
	 * @param arg0
	 */
	public UtilTest(String arg0) {
		super(arg0);
	}
	
	public void setUp() {
		//create a couple calendars/dates
		cal = Calendar.getInstance();
		cal.set(2001, Calendar.NOVEMBER, 22, 10, 01, 01);
		cal2 = Calendar.getInstance();
		cal2.set(2001, Calendar.NOVEMBER, 22, 11, 01, 01);
	}

	public void testGetTimeFromDate() {
		assertEquals(Util.getTimeFromDate(cal.getTime()), 1001);
		assertEquals(Util.getTimeFromDate(cal2.getTime()), 1101);
	}
    
	public void testMilliTimeDifference() {
		int noon = 1200;
		int elevenThirty = 1130;
		int oneFifteen = 1315;
    	
		long thirtyMinutes = 30 * 60 * 1000;
		long hourFifteenMinutes = (60 + 15) * 60 * 1000;
		long hourFortyFiveMinutes = (60+45) * 60 * 1000;
    	
		assertEquals(thirtyMinutes, Util.milliTimeDiffernce(elevenThirty,noon));
		assertEquals(hourFifteenMinutes, Util.milliTimeDiffernce(noon,oneFifteen));
		assertEquals(hourFortyFiveMinutes, Util.milliTimeDiffernce(elevenThirty,oneFifteen));   	
	}
    
	public void testConvertToMillis() {
		int noon = 1200;
		int oneAM = 100;
		int elevenFifteenPM = 2315;
    	
		long noonMillis = 12 * 60 * 60 * 1000;
		long oneAMmillis = 1 * 60 * 60 * 1000;
		long elevenFifteenPMmillis = (23*60+15) * 60 * 1000;
    	
		assertEquals(noonMillis, Util.convertToMillis(noon));
		assertEquals(oneAMmillis, Util.convertToMillis(oneAM));
		assertEquals(elevenFifteenPMmillis, Util.convertToMillis(elevenFifteenPM));
	}    
}
