/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit                              *
 * Copyright (C) 2001  ThoughtWorks, Inc.                                       *
 * 651 W Washington Ave. Suite 500                                              *
 * Chicago, IL 60661 USA                                                        *
 *                                                                              *
 * This program is free software; you can redistribute it and/or                *
 * modify it under the terms of the GNU General Public License                  *
 * as published by the Free Software Foundation; either version 2               *
 * of the License, or (at your option) any later version.                       *
 *                                                                              *
 * This program is distributed in the hope that it will be useful,              *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 * GNU General Public License for more details.                                 *
 *                                                                              *
 * You should have received a copy of the GNU General Public License            *
 * along with this program; if not, write to the Free Software                  *
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  *
 ********************************************************************************/

package net.sourceforge.cruisecontrol;

import java.util.*;
import junit.framework.*;

/**
 * Class Description
 *
 * @author  robertdw
 * @version 18 May 2001
 */
public class CVSElementTest extends TestCase {
    
    public CVSElementTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(CVSElementTest.class);
        
        return suite;
    }
    
    /**
     * Verify that the CVSDate formats the date, for a variety of time zones,
     * into a GMT time zone string.
     */
    public void testCVSDate() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+0:00"));
        Date testDate = new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();    // 6pm, 18 May, 2001
        assertEquals("Failed for GMT", "2001-05-18 18:00:00 GMT", CVSElement.CVSDATE.format(testDate));

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+10:00"));
        testDate = new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();    // 6pm, 18 May, 2001
        assertEquals("Failed for GMT", "2001-05-18 08:00:00 GMT", CVSElement.CVSDATE.format(testDate));
        testDate = new GregorianCalendar(2001, 4, 18, 8, 0, 0).getTime();    // 6pm, 18 May, 2001
        assertEquals("Failed for GMT", "2001-05-17 22:00:00 GMT", CVSElement.CVSDATE.format(testDate));

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-10:00"));
        testDate = new GregorianCalendar(2001, 4, 18, 18, 0, 0).getTime();    // 6pm, 18 May, 2001
        assertEquals("Failed for GMT", "2001-05-19 04:00:00 GMT", CVSElement.CVSDATE.format(testDate));
        testDate = new GregorianCalendar(2001, 4, 18, 8, 0, 0).getTime();    // 6pm, 18 May, 2001
        assertEquals("Failed for GMT", "2001-05-18 18:00:00 GMT", CVSElement.CVSDATE.format(testDate));
    }
}
