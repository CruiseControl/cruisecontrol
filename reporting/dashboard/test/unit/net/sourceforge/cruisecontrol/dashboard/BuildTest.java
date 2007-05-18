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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemFixture;
import org.apache.commons.io.FileUtils;

public class BuildTest extends TestCase {

    private BuildDetail build;

    private Map defaultProps;

    protected void setUp() {
        defaultProps = new HashMap();
        defaultProps.put("logfile", new File("log20051209122103.xml"));
        build = new BuildDetail(defaultProps);
    }

    public void testShouldBeComparedByDate() {
        BuildDetail earlierBuild = new BuildDetail(defaultProps);

        Map newprops = new HashMap();
        newprops.put("logfile", new File("log21111209122103.xml"));
        BuildDetail laterBuild = new BuildDetail(newprops);

        assertEquals(0, earlierBuild.compareTo(earlierBuild));
        assertEquals(-1, earlierBuild.compareTo(laterBuild));
        assertEquals(1, laterBuild.compareTo(earlierBuild));
        earlierBuild.compareTo(laterBuild);
    }

    public void testCanGetNumberOfTestsFromBuild() {
        BuildTestSuite suiteWithFiveTests = new BuildTestSuite(0.0f, 5, 0, "", 0);
        BuildTestSuite suiteWithFourTests = new BuildTestSuite(0.0f, 4, 0, "", 0);
        List nineTests = new ArrayList();
        nineTests.add(suiteWithFiveTests);
        nineTests.add(suiteWithFourTests);

        Map props = new HashMap();
        props.put("testsuites", nineTests);
        BuildDetail laterBuild = new BuildDetail(props);
        assertEquals(9, laterBuild.getNumberOfTests());
    }

    public void testCanGetNumberOfFailedTests() {
        BuildTestSuite suiteWithTwoFailures = new BuildTestSuite(0.0f, 5, 2, "", 0);
        BuildTestSuite suiteWithOneFailure = new BuildTestSuite(0.0f, 4, 1, "", 0);
        List threeFailures = new ArrayList();
        threeFailures.add(suiteWithTwoFailures);
        threeFailures.add(suiteWithOneFailure);

        Map props = new HashMap();
        props.put("testsuites", threeFailures);
        BuildDetail laterBuild = new BuildDetail(props);

        assertEquals(3, laterBuild.getNumberOfFailures());
    }

    public void testCanGetNumberOfTestErrors() {
        BuildTestSuite suiteWithTwoErrors = new BuildTestSuite(0.0f, 5, 0, "", 2);
        BuildTestSuite suiteWithThreeErrors = new BuildTestSuite(0.0f, 4, 0, "", 3);
        List fiveErrors = new ArrayList();
        fiveErrors.add(suiteWithTwoErrors);
        fiveErrors.add(suiteWithThreeErrors);

        Map props = new HashMap();
        props.put("testsuites", fiveErrors);
        BuildDetail laterBuild = new BuildDetail(props);

        assertEquals(5, laterBuild.getNumberOfErrors());
    }

    public void testShouldReturnPassedAsStringWhenTheBuildPassed() {
        Map props = new HashMap();
        props.put("logfile", new File("log20001212050505Lbuild.2.xml"));
        BuildDetail laterBuild = new BuildDetail(props);
        assertEquals("Passed", laterBuild.getStatus());
    }

    public void testShouldReturnPassedAsStringWhenTheBuildFailed() {
        Map props = new HashMap();
        props.put("logfile", new File("log20001212050505.xml"));
        BuildDetail laterBuild = new BuildDetail(props);
        assertEquals("Failed", laterBuild.getStatus());
    }

    public void testPluginOutputShouldBeInOrder() throws Exception {
        build.addPluginOutput("cate2", "out1");
        build.addPluginOutput("cate1", "out1");
        build.addPluginOutput("cate3", "out1");
        Iterator iterator = build.getPluginOutputs().keySet().iterator();
        assertEquals("cate2", (String) iterator.next());
        assertEquals("cate1", (String) iterator.next());
        assertEquals("cate3", (String) iterator.next());
    }

    public void testShouldReturnAllArtifactsForTheBuild() throws IOException {
        String projectName = "p1";
        String timeStamp = "20001212050505";
        File artifactsRoot = FilesystemFixture.createDirectory(projectName);
        File artifactsDir = FilesystemFixture.createDirectory(timeStamp, projectName);
        FilesystemFixture.createFile("p1.jar", artifactsDir);
        FilesystemFixture.createFile("p1.war", artifactsDir);
        FilesystemFixture.createFile("p1.ear", artifactsDir);

        Map props = new HashMap();
        props.put("logfile", new File("log20001212050505.xml"));
        props.put("artifactfolder", artifactsRoot);
        BuildDetail detail = new BuildDetail(props);

        assertEquals(3, detail.getArtifacts().size());
    }

    public void testShouldGetArtifactsInSubDirectories() throws Exception {
        String projectName = "p2";
        String timeStamp = "20001212050505";
        File artifactsRoot = FilesystemFixture.createDirectory(projectName);
        File artifactsDir = FilesystemFixture.createDirectory(timeStamp, projectName);
        FilesystemFixture.createFile("p2.jar", artifactsDir);
        FilesystemFixture.createFile("p2.war", artifactsDir);
        FilesystemFixture.createFile("p2.ear", artifactsDir);
        File subDir = new File(artifactsDir, "subdir");

        FileUtils.forceMkdir(subDir);
        FilesystemFixture.createFile("chnakd.ear", subDir);
        Map props = new HashMap();
        props.put("logfile", new File("log20001212050505.xml"));
        props.put("artifactfolder", artifactsRoot);
        BuildDetail detail = new BuildDetail(props);

        assertEquals(4, detail.getArtifacts().size());
    }

    public void testShouldIgnoreFilesWithDotPrefix() throws Exception {
        String projectName = "p3";
        String timeStamp = "20001212050505";
        File artifactsRoot = FilesystemFixture.createDirectory(projectName);
        File artifactsDir = FilesystemFixture.createDirectory(timeStamp, projectName);
        FilesystemFixture.createFile("p3.jar", artifactsDir);
        FilesystemFixture.createFile("p3.war", artifactsDir);
        FilesystemFixture.createFile("p3.ear", artifactsDir);

        File startWithDot = FilesystemFixture.createFile(".start.with.file", artifactsDir);
        assertTrue(startWithDot.exists());

        Map props = new HashMap();
        props.put("logfile", new File("log20001212050505.xml"));
        props.put("artifactfolder", artifactsRoot);
        BuildDetail detail = new BuildDetail(props);

        assertEquals(3, detail.getArtifacts().size());
    }
}
