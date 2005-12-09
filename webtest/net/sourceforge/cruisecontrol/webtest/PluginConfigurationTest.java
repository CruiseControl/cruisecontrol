/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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
package net.sourceforge.cruisecontrol.webtest;

import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.sourcecontrols.ConcurrentVersionsSystem;
import net.sourceforge.cruisecontrol.sourcecontrols.SVN;
import net.sourceforge.cruisecontrol.PluginConfiguration;
import net.sourceforge.cruisecontrol.Configuration;
import net.sourceforge.cruisecontrol.PluginDetail;
import net.sourceforge.cruisecontrol.GenericPluginDetail;

public class PluginConfigurationTest extends TestCase {
    private PluginConfiguration cvs;

    private PluginConfiguration svn;

    protected void setUp() throws Exception {
        super.setUp();

        Configuration configuration = new Configuration("localhost", 7856);
        PluginDetail cvsDetails = new GenericPluginDetail("cvs", ConcurrentVersionsSystem.class);
        PluginDetail svnDetails = new GenericPluginDetail("svn", SVN.class);

        this.cvs = new PluginConfiguration(cvsDetails, configuration);
        this.svn = new PluginConfiguration(svnDetails, configuration);
    }

    public void testShouldGetName() {
        assertEquals("cvs", cvs.getName());
        assertEquals("svn", svn.getName());
    }

    public void testShouldGetType() {
        assertEquals("sourcecontrol", cvs.getType());
        assertEquals("sourcecontrol", svn.getType());
    }

    public void testShouldGetDetails() {
        Map cvsDetails = cvs.getDetails();
        assertEquals(6, cvsDetails.size());
        assertTrue(cvsDetails.containsKey("cvsRoot"));
        assertNull(cvsDetails.get("cvsRoot"));
        assertTrue(cvsDetails.containsKey("localWorkingCopy"));
        assertEquals("projects/${project.name}", cvsDetails
                .get("localWorkingCopy"));
        assertTrue(cvsDetails.containsKey("module"));
        assertNull(cvsDetails.get("module"));
        assertTrue(cvsDetails.containsKey("property"));
        assertNull(cvsDetails.get("property"));
        assertTrue(cvsDetails.containsKey("propertyOnDelete"));
        assertNull(cvsDetails.get("propertyOnDelete"));
        assertTrue(cvsDetails.containsKey("tag"));
        assertNull(cvsDetails.get("tag"));

        Map svnDetails = svn.getDetails();
        assertEquals(6, svnDetails.size());
        assertTrue(svnDetails.containsKey("localWorkingCopy"));
        assertNull(svnDetails.get("localWorkingCopy"));
        assertTrue(svnDetails.containsKey("password"));
        assertNull(svnDetails.get("password"));
        assertTrue(svnDetails.containsKey("property"));
        assertNull(svnDetails.get("property"));
        assertTrue(svnDetails.containsKey("propertyOnDelete"));
        assertNull(svnDetails.get("propertyOnDelete"));
        assertTrue(svnDetails.containsKey("repositoryLocation"));
        assertNull(svnDetails.get("repositoryLocation"));
        assertTrue(svnDetails.containsKey("username"));
        assertNull(svnDetails.get("username"));
    }
}
