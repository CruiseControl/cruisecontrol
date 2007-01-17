/*******************************************************************************
 * CruiseControl, a Continuous Integration Toolkit Copyright (c) 2001,
 * ThoughtWorks, Inc. 200 E. Randolph, 25th Floor Chicago, IL 60601 USA All
 * rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  + Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  + Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the names of
 * its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.Calendar;
import java.util.List;
import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

/**
 * @version $Id$
 */
public class TimeBuildTest extends TestCase {

    public void testValidate() {
        TimeBuild timeBuild = new TimeBuild();
        try {
            timeBuild.validate();
            fail("TimeBuild should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("the 'time' attribute is mandatory", e.getMessage());
        }
    }

    public void testTimes() {
        Calendar calender1400 = Calendar.getInstance();
        calender1400.set(2002, Calendar.DECEMBER, 23, 14, 00, 00);

        Calendar calender1600 = Calendar.getInstance();
        calender1600.set(2002, Calendar.DECEMBER, 23, 16, 00, 00);

        Calendar calender1601 = Calendar.getInstance();
        calender1601.set(2002, Calendar.DECEMBER, 23, 16, 01, 00);
        calender1601.set(Calendar.MILLISECOND, 0);

        Calendar calender1602 = Calendar.getInstance();
        calender1602.set(2002, Calendar.DECEMBER, 23, 16, 02, 00);

        Calendar calender1603 = Calendar.getInstance();
        calender1603.set(2002, Calendar.DECEMBER, 23, 16, 03, 00);

        String modifierUserName = "epugh";
        TimeBuild timeBuild = createTimeBuildForTime(modifierUserName, "1601");

        assertSinceLastBuildNoModificationsAtTime(calender1400, calender1600, timeBuild);
        assertSinceLastBuildNoModificationsAtTime(calender1400, calender1601, timeBuild);

        List modifications = timeBuild.getModifications(calender1400.getTime(), calender1602.getTime());
        assertHasSingleModificationThatMatchesNameAndTime(modifications, modifierUserName, calender1601.getTime());

        assertSinceLastBuildNoModificationsAtTime(calender1602, calender1603, timeBuild);
    }

    private void assertSinceLastBuildNoModificationsAtTime(Calendar lastBuild, Calendar now, TimeBuild timeBuild) {
        List modifications = timeBuild.getModifications(lastBuild.getTime(), now.getTime());
        assertEquals(0, modifications.size());
    }

    private TimeBuild createTimeBuildForTime(String userName, String buildTime) {
        TimeBuild timeBuild = new TimeBuild();
        timeBuild.setTime(buildTime);
        timeBuild.setUserName(userName);
        return timeBuild;
    }

    public void testTimeBuildWorksAcrossDays() {
        Calendar calender2000Previousday = Calendar.getInstance();
        calender2000Previousday.set(2002, Calendar.DECEMBER, 22, 20, 00, 00);

        Calendar calender1400 = Calendar.getInstance();
        calender1400.set(2002, Calendar.DECEMBER, 23, 14, 00, 00);

        Calendar calender1600 = Calendar.getInstance();
        calender1600.set(2002, Calendar.DECEMBER, 23, 16, 00, 00);
        calender1600.set(Calendar.MILLISECOND, 0);

        Calendar calender1601 = Calendar.getInstance();
        calender1601.set(2002, Calendar.DECEMBER, 23, 16, 01, 00);

        Calendar calender1603 = Calendar.getInstance();
        calender1603.set(2002, Calendar.DECEMBER, 23, 16, 03, 00);

        // Schedule a timed build for 16:00
        TimeBuild timeBuild = new TimeBuild();
        timeBuild.setTime("1600");
        timeBuild.setUserName("epugh");

        Date lastBuildEightPMYesterday = calender2000Previousday.getTime();

        // No "modifications" at 14:00
        List modifications = timeBuild.getModifications(lastBuildEightPMYesterday, calender1400.getTime());
        assertEquals(0, modifications.size());

        // No "modifications" at 16:00
        modifications = timeBuild.getModifications(lastBuildEightPMYesterday, calender1600.getTime());
        assertEquals(0, modifications.size());

        // Should have one "modification" at 16:01 which is the TimedBuild
        // modification for 16:00
        modifications = timeBuild.getModifications(lastBuildEightPMYesterday, calender1601.getTime());
        assertEquals(1, modifications.size());
        assertHasSingleModificationThatMatchesNameAndTime(modifications, "epugh", calender1600.getTime());

        // No "modifications" from 16:01 to 16:03
        modifications = timeBuild.getModifications(calender1601.getTime(), calender1603.getTime());
        assertEquals(0, modifications.size());
    }

    private void assertHasSingleModificationThatMatchesNameAndTime(List modifications, String userName,
            Date expectedModifiedTime) {
        assertEquals(1, modifications.size());
        Modification modification = (Modification) modifications.get(0);
        assertEquals(userName, modification.userName);
        assertEquals(expectedModifiedTime, modification.modifiedTime);
    }
}
