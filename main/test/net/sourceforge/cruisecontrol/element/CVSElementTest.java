package net.sourceforge.cruisecontrol.element;

import java.util.*;
import junit.framework.*;

/**
 *@author  Robert Watkins
 *@author  Jason Yip, jcyip@thoughtworks.com
 */
public class CVSElementTest extends TestCase {

	public CVSElementTest(java.lang.String testName) {
		super(testName);
	}

	public void testBuildHistoryCommand() {
		Date lastBuildTime = new Date();
		Date currTime = new Date();
		CVSElement element = new CVSElement();
		element.setCvsRoot("cvsroot");
		element.setLocalWorkingCopy(".");

		String[] expectedCommand = new String[]{"cvs", "-d", "cvsroot", "log",
				"-N", "-d", ">" + CVSElement.formatCVSDate(lastBuildTime), "."};

		String[] actualCommand =
				element.buildHistoryCommand(lastBuildTime).getCommandline();

		assertEquals("Mismatched lengths!", expectedCommand.length,
				actualCommand.length);
		for (int i = 0; i < expectedCommand.length; i++) {
			assertEquals(expectedCommand[i], actualCommand[i]);
		}
	}

	public void testHistoryCommandNullLocal() {
		Date lastBuildTime = new Date();
		Date currTime = new Date();

		CVSElement element = new CVSElement();
		element.setCvsRoot("cvsroot");
		element.setLocalWorkingCopy(null);

		String[] expectedCommand = new String[]{"cvs", "-d", "cvsroot", "log",
				"-N", "-d", ">" + CVSElement.formatCVSDate(lastBuildTime)};

		String[] actualCommand =
				element.buildHistoryCommand(lastBuildTime).getCommandline();

		assertEquals("Mismatched lengths!", expectedCommand.length,
				actualCommand.length);
		for (int i = 0; i < expectedCommand.length; i++) {
			assertEquals(expectedCommand[i], actualCommand[i]);
		}
	}

	public void testHistoryCommandNullCVSROOT() {
		Date lastBuildTime = new Date();
		Date currTime = new Date();
		CVSElement element = new CVSElement();
		element.setCvsRoot(null);
		element.setLocalWorkingCopy(".");

		String[] expectedCommand = new String[]{"cvs", "log",
				"-N", "-d", ">" + CVSElement.formatCVSDate(lastBuildTime), "."};

		String[] actualCommand =
				element.buildHistoryCommand(lastBuildTime).getCommandline();
		assertEquals("Mismatched lengths!", expectedCommand.length,
				actualCommand.length);
		for (int i = 0; i < expectedCommand.length; i++) {
			assertEquals(expectedCommand[i], actualCommand[i]);
		}
	}

	public void testFormatLogDate() {
		Date may18_2001_6pm =
				new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
		assertEquals("2001/05/18 18:00:00 "
				 + TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT),
				CVSElement.LOGDATE.format(may18_2001_6pm));
	}

	public void testFormatCVSDateGMTPlusZero() {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+0:00"));
		Date may18_2001_6pm =
				new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
		assertEquals("2001-05-18 18:00:00 GMT",
				CVSElement.formatCVSDate(may18_2001_6pm));
	}

	public void testFormatCVSDateGMTPlusTen() {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+10:00"));
		Date may18_2001_6pm = new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
		assertEquals("2001-05-18 08:00:00 GMT",
				CVSElement.formatCVSDate(may18_2001_6pm));
		Date may8_2001_6pm = new GregorianCalendar(2001, 4, 18, 8, 0, 0).getTime();
		assertEquals("2001-05-17 22:00:00 GMT",
				CVSElement.formatCVSDate(may8_2001_6pm));
	}

	public void testFormatCVSDateGMTMinusTen() {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT-10:00"));
		Date may18_2001_6pm = new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();
		assertEquals("2001-05-19 04:00:00 GMT",
				CVSElement.formatCVSDate(may18_2001_6pm));
		Date may8_2001_6pm = new GregorianCalendar(2001, 4, 18, 8, 0, 0).getTime();
		assertEquals("2001-05-18 18:00:00 GMT",
				CVSElement.formatCVSDate(may8_2001_6pm));
	}

	public static void main(java.lang.String[] args) {
		junit.textui.TestRunner.run(CVSElementTest.class);
	}

}
