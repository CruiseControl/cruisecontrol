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
package net.sourceforge.cruisecontrol.dashboard.jwebunittests;

import org.apache.commons.lang.StringUtils;

public class BuildDetailTest extends BaseFunctionalTest {

    public void testShouldShowArtifactsForFailedBuild() {
        tester.beginAt("/tab/build/detail/project1/20051209122104");
        tester.assertTextPresent("Artifacts");
        tester.assertTextPresent("Merged Check Style");
        tester.assertTextPresent("Line has trailing spaces.");
        tester.assertLinkPresentWithExactText("#123");
    }

    public void testShouldNotShowArtifactsIfNoPublishersInConfigFile() throws Exception {
        tester.beginAt("/tab/build/detail/projectWithoutPublishers/20060704155755");
        tester.assertLinkNotPresentWithText("artifact.txt");
    }

    public void testShouldNotShowArtifactsForProjectsWithoutConfiguration() throws Exception {
        tester.beginAt("/tab/build/detail/projectWithoutConfiguration/20060704155710");
        tester.assertLinkNotPresentWithText("artifact.txt");
    }

    public void testShouldBeAbleToOpenProjectNameWithSpace() throws Exception {
        tester.beginAt("/tab/build/detail/project%20space");
        tester.assertTextPresent("project space");
    }

    public void testShouldShowDurationFromLastSuccessfulBuild() throws Exception {
        tester.beginAt("/tab/build/detail/projectWithoutConfiguration/20060704160010");
        tester.assertTextPresent("Previous successful build");
        tester.assertTextPresent("minutes");
        tester.assertTextPresent("ago");
    }

    public void testShouldShowErrorsAndWarningTabAndBuildErrorMessages() throws Exception {
        tester.beginAt("/tab/build/detail/project1/20051209122104");
        tester.assertTextPresent("Errors and Warnings");
        tester.assertTextPresent("Detected OS: Windows XP");
        tester.assertTextPresent("Cannot find something");
        String projectsFolder = "'C:\\pdj\\src\\cruisecontrol\\target\\webtest\\cruisecontrol-bin-2.4.0-dev\\projects";
        tester.assertTextPresent("Compilation arguments: "
                + "'-d' "
                + projectsFolder + "\\connectfour\\target\\classes' "
                + "'-classpath' "
                + projectsFolder + "\\connectfour\\target\\classes;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-launcher.jar;"
                    + "C:\\Program Files\\Java\\jre1.5.0_05\\lib\\ext\\QTJava.zip;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-antlr.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-bcel.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-bsf.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-log4j.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-oro.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-regexp.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-apache-resolver.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-commons-logging.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-commons-net.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-icontract.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-jai.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-javamail.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-jdepend.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-jmf.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-jsch.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-junit.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-netrexx.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-nodeps.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-starteam.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-stylebook.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-swing.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-trax.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-vaj.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-weblogic.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-xalan1.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant-xslp.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\ant.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\junit-3.8.1.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\xercesImpl.jar;"
                    + "C:\\pdj\\java\\apache-ant-1.6.5\\lib\\xml-apis.jar;"
                    + "C:\\pdj\\java\\j2sdk1.4.2_09\\lib\\tools.jar' "
                + "'-sourcepath' "
                + projectsFolder + "\\connectfour\\src' "
                + "'-g:none' "
                + "The ' characters around the executable and arguments are "
                + "not part of the command.");

        tester.assertTextPresent("Build Error Message");
        tester.assertTextPresent("This is my error message");

        tester.assertTextPresent("Stacktrace");
        tester.assertTextPresent("This is my stacktrace");
    }

    public void testShouldEscapeSpecialCharacterInJUnitMessages() throws Exception {
        tester.beginAt("/tab/build/detail/project1/20051209122104");
        assertTrue(StringUtils.contains(tester.getPageSource(), "expected:&lt;1&gt; but was:&lt;2&gt;"));
    }

    public void testShouldEscapeSpecialCharacterinCommitMessages() throws Exception {
        tester.beginAt("/tab/build/detail/project1/20051209122104");
        String pageSource = tester.getPageSource();
        assertTrue(StringUtils.contains(pageSource, "project name changed to &lt;b&gt;cache&lt;/b&gt;"));
        assertTrue(StringUtils.contains(pageSource, "[rev. 1.2]"));
        assertTrue(StringUtils.contains(pageSource, "modified"));
        assertTrue(StringUtils.contains(pageSource, "cc-build.xml"));
    }

    public void testShouldShowDefaultMessagewhenNoErrors() throws Exception {
        tester.beginAt("/tab/build/detail/project1/20051209122103");
        tester.assertTextPresent("No error message");
        tester.assertTextPresent("No stacktrace");
        tester.assertTextPresent("No errors or warnings");
    }

    public void testShouldListGZippedLogs() throws Exception {
        tester.beginAt("/tab/build/detail/project3");
        tester.assertTextPresent(convertedTime("20070209122100"));
        tester.assertTextPresent(convertedTime("20061209122100"));
        tester.assertTextPresent("build.489");
        tester.assertTextPresent(convertedTime("20061109122100"));
        tester.assertTextPresent(convertedTime("20051209122100"));
    }
    
    public void testShouldShowModifications() throws Exception {
        tester.beginAt("/tab/build/detail/project2");
        String pageSource = tester.getPageSource();

        assertTrue(StringUtils.contains(pageSource, "[rev. 1.2]"));
        assertTrue(StringUtils.contains(pageSource, "build.xml"));
        assertTrue(StringUtils.contains(pageSource, "cc-build.xml"));
        assertTrue(StringUtils.contains(pageSource, "cc-build-deleted.xml"));
    }
}