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

import net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.TimeConverter;

import org.apache.commons.io.FileUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class BuildDetailTest extends MockObjectTestCase {

    private BuildDetail build;

    private LogFile defaultLogFile;

    protected void setUp() {
        defaultLogFile = new LogFile("log20051209122103.xml");
        build = new BuildDetail(defaultLogFile);
    }

    public void testShouldBeComparedByDate() {
        BuildDetail laterBuild = new BuildDetail(new LogFile("log21111209122103.xml"));

        assertEquals(0, build.compareTo(build));
        assertEquals(-1, build.compareTo(laterBuild));
        assertEquals(1, laterBuild.compareTo(build));
    }

    public void testCanGetNumberOfTestsFromBuild() {
        BuildTestSuite suiteWithFiveTests = new BuildTestSuite("", 0.0f);
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFiveTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        BuildTestSuite suiteWithFourTests = new BuildTestSuite("", 0.0f);
        suiteWithFourTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFourTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFourTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        suiteWithFourTests.addTestCase(new BuildTestCase(null, null, null, null, null, null));
        List nineTests = new ArrayList();
        nineTests.add(suiteWithFiveTests);
        nineTests.add(suiteWithFourTests);

        Map props = new HashMap();
        props.put("testsuites", nineTests);
        BuildDetail laterBuild = new BuildDetail(defaultLogFile, props);
        assertEquals(9, laterBuild.getNumberOfTests());
    }

    public void testCanGetNumberOfFailedTests() {
        BuildTestSuite suiteWithTwoFailures = new BuildTestSuite("", 0.0f);
        suiteWithTwoFailures.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.FAILED));
        suiteWithTwoFailures.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.FAILED));
        BuildTestSuite suiteWithOneFailure = new BuildTestSuite("", 0.0f);
        suiteWithOneFailure.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.FAILED));
        List threeFailures = new ArrayList();
        threeFailures.add(suiteWithTwoFailures);
        threeFailures.add(suiteWithOneFailure);

        Map props = new HashMap();
        props.put("testsuites", threeFailures);
        BuildDetail laterBuild = new BuildDetail(defaultLogFile, props);

        assertEquals(3, laterBuild.getNumberOfFailures());
    }

    public void testCanGetNumberOfTestErrors() {
        BuildTestSuite suiteWithTwoErrors = new BuildTestSuite("", 0.0f);
        suiteWithTwoErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        suiteWithTwoErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        BuildTestSuite suiteWithThreeErrors = new BuildTestSuite("", 0.0f);
        suiteWithThreeErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        suiteWithThreeErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        suiteWithThreeErrors.addTestCase(new BuildTestCase(null, null, null, null, null, BuildTestCaseResult.ERROR));
        List fiveErrors = new ArrayList();
        fiveErrors.add(suiteWithTwoErrors);
        fiveErrors.add(suiteWithThreeErrors);

        Map props = new HashMap();
        props.put("testsuites", fiveErrors);
        BuildDetail laterBuild = new BuildDetail(defaultLogFile, props);

        assertEquals(5, laterBuild.getNumberOfErrors());
    }

    public void testShouldReturnPassedAsStringWhenTheBuildPassed() {
        LogFile laterLogFile = new LogFile("log20001212050505Lbuild.2.xml");
        BuildDetail laterBuild = new BuildDetail(laterLogFile);
        assertEquals(PreviousResult.PASSED, laterBuild.getPreviousBuildResult());
    }

    public void testShouldReturnPassedAsStringWhenTheBuildFailed() {
        LogFile laterLogFile = new LogFile("log20001212050505.xml");
        BuildDetail laterBuild = new BuildDetail(laterLogFile);
        assertEquals(PreviousResult.FAILED, laterBuild.getPreviousBuildResult());
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
        File artifactsRoot = FilesystemUtils.createDirectory(projectName);
        File artifactsDir = FilesystemUtils.createDirectory(timeStamp, projectName);
        FilesystemUtils.createFile("p1.jar", artifactsDir);
        FilesystemUtils.createFile("p1.war", artifactsDir);
        FilesystemUtils.createFile("p1.ear", artifactsDir);

        Map props = new HashMap();
        LogFile logFile = new LogFile("log20001212050505.xml");
        props.put("artifactfolder", artifactsRoot);
        BuildDetail detail = new BuildDetail(logFile, props);

        assertEquals(3, detail.getArtifactFiles().size());
    }

    public void testShouldGetArtifactsInSubDirectories() throws Exception {
        String projectName = "p2";
        String timeStamp = "20001212050505";
        File artifactsRoot = FilesystemUtils.createDirectory(projectName);
        File artifactsDir = FilesystemUtils.createDirectory(timeStamp, projectName);
        FilesystemUtils.createFile("p2.jar", artifactsDir);
        FilesystemUtils.createFile("p2.war", artifactsDir);
        FilesystemUtils.createFile("p2.ear", artifactsDir);
        File subDir = new File(artifactsDir, "subdir");

        FileUtils.forceMkdir(subDir);
        FilesystemUtils.createFile("p3.ear", subDir);
        FilesystemUtils.createFile("p4.ear", subDir);
        Map props = new HashMap();
        LogFile logFile = new LogFile("log20001212050505.xml");
        props.put("artifactfolder", artifactsRoot);
        BuildDetail detail = new BuildDetail(logFile, props);
        List artifactNames = toFileNameList(detail.getArtifactFiles());
        assertEquals(4, artifactNames.size());
        assertTrue(artifactNames.contains("p2.war"));
        assertTrue(artifactNames.contains("p2.jar"));
        assertTrue(artifactNames.contains("p2.ear"));
        assertTrue(artifactNames.contains("subdir"));
    }

    private List toFileNameList(List files) {
        List fileNames = new ArrayList();
        for (Iterator iter = files.iterator(); iter.hasNext();) {
            File file = (File) iter.next();
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    public void testShouldInvokeTheTimeConverter() throws Exception {
        Mock timeConverterMock = mock(TimeConverter.class);
        BuildDetail laterBuild =
                new BuildDetail(new LogFile("log21111209122103.xml"), new HashMap(),
                        (TimeConverter) timeConverterMock.proxy());
        timeConverterMock.expects(once()).method("getConvertedTime");
        laterBuild.getConvertedTime();
    }
}
