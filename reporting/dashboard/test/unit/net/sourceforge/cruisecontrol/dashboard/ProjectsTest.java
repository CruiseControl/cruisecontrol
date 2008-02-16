/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard;

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.jmock.cglib.MockObjectTestCase;

public class ProjectsTest extends MockObjectTestCase {

    private Projects projects;

    private File logDir;

    private File artifactsDir;

    protected void setUp() throws Exception {
        logDir = DataUtils.getLogDirAsFile();
        artifactsDir = DataUtils.getArtifactsDirAsFile();
        projects = new Projects(logDir, artifactsDir, new String[0]);
    }

    public void testShouldReturnCorrectLogDir() {
        assertEquals(new File(logDir, "project1"), projects.getLogRoot("project1"));
    }
    
    public void testShouldReturnLogDirsForAllProjects() throws Exception {
        projects = new Projects(logDir, artifactsDir, new String[] {"p1", "p2"});
        File[] logDirs = projects.getProjectsRegistedInBuildLoop();
        
        assertEquals(2, logDirs.length);
        assertEquals(new File(logDir, "p1"), logDirs[0]);
    }

    public void testShouldReturnArtifactsWhenArtifactsIsEmpty() throws Exception {
        assertEquals(new File(artifactsDir, "project1"), projects.getArtifactRoot("project1"));
    }
}
