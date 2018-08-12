/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
package net.sourceforge.cruisecontrol.publishers.rss;

import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.jdom2.Element;

/*
 * Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 * Licensed under the CruiseControl BSD license
 */
public class CruiseControlItemTest extends TestCase {

    private XMLLogHelper successLogHelper;

    protected XMLLogHelper createLogHelper(boolean success, boolean lastBuildSuccess) {
        Element cruisecontrolElement = TestUtil.createElement(success, lastBuildSuccess);

        return new XMLLogHelper(cruisecontrolElement);
    }

    public void setUp() {
        successLogHelper = createLogHelper(true, true);
    }


    public void testConstructors() throws Exception {
        CruiseControlItem item = new CruiseControlItem(successLogHelper, "link");

        assertEquals("someproject somelabel Build Successful", item.getTitle());
        Date date = DateUtil.parseFormattedTime("20020313120000", "mockcctimestamp");
        assertEquals("<em>Build Time:</em> " + date + "<br/><em>Label:</em> somelabel<br/>"
                + "<em>Modifications: </em>4"
                + "<li>filename1  by user1 (The comment)</li><li>filename2  by user2 (The comment)</li>"
                + "<li>filename3  by user2 (The comment)</li><li>filename4  by user3 (The comment)</li>"
                + "</ul><br/>\n<ul>",
            item.getDescription());

        // Can't validate the date published field because the TestUtil.createElement
        // method doesn't insert a build date, so we don't know what the date
        // should be...
        // FIXME

        //assertEquals("", item.getLink());
   }
}