/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     + Redistributions of source code must retain the above copyright 
 *       notice, this list of conditions and the following disclaimer. 
 *       
 *     + Redistributions in binary form must reproduce the above 
 *       copyright notice, this list of conditions and the following 
 *       disclaimer in the documentation and/or other materials provided 
 *       with the distribution. 
 *       
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the 
 *       names of its contributors may be used to endorse or promote 
 *       products derived from this software without specific prior 
 *       written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
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

		String[] expectedCommand = new String[]{"cvs", "-d", "cvsroot", "-q", "log",
				"-N", "-d", ">" + CVSElement.formatCVSDate(lastBuildTime)};

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

		String[] expectedCommand = new String[] {"cvs", "-d", "cvsroot", "-q", "log",
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

		String[] expectedCommand = new String[]{"cvs", "-q", "log",
				"-N", "-d", ">" + CVSElement.formatCVSDate(lastBuildTime)};

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
