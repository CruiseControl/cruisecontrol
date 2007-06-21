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
package net.sourceforge.cruisecontrol.dashboard.testhelpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;

public final class DataUtils {
    public static final String TEST_DATA_DIR = "test/data/";

    public static final String CONFIG_XML = "config.xml";

    public static final String FAILING_LOG = "cruisecontrollog_internalerror.log";

    public static final String FAILING_BUILD_XML = "log20051209122104.xml";

    public static final String PASSING_BUILD_LBUILD_0_XML = "log20051209122103Lbuild.489.xml";

    public static final String TESTSUITE_IN_BUILD_LBUILD =
            "net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest";

    public static final String LOGFILE_OF_PROJECT2 = "log20060703155722.xml";

    private DataUtils() {
    }

    public static File getProjectDirAsFile() throws Exception {
        return getData(PASSING_BUILD_LBUILD_0_XML).getParentFile();
    }

    public static File getConfigXmlAsFile() throws Exception {
        return getData(CONFIG_XML);
    }

    public static File getConfigXmlOfWebApp() throws Exception {
        File ccroot = new File(FilesystemUtils.getTestRootDir(), "tmpCCRoot");
        File data = new File(ccroot, "data");
        return new File(data, "config.xml");
    }

    public static void cloneCCHome() throws Exception {
        File ccRoot = getConfigXmlAsFile().getParentFile();
        File tmpCCRoot = FilesystemUtils.createDirectory("tmpCCRoot");
        FileUtils.copyDirectoryToDirectory(ccRoot, tmpCCRoot);
    }

    public static File getLogRootOfWebapp() throws Exception {
        return getSubFolderOfWebApp("logs");
    }

    private static File getSubFolderOfWebApp(String subFolder) {
        File ccroot = new File(FilesystemUtils.getTestRootDir(), "tmpCCRoot");
        File data = new File(ccroot, "data");
        return new File(data, subFolder);
    }

    public static File getArtifactRootOfWebapp() {
        return getSubFolderOfWebApp("arbitrary_artifacts/artifacts");
    }

    public static File getProjectsRootOfWebapp() {
        return getSubFolderOfWebApp("projects");
    }

    public static final File getConfigXmlInArbitraryCCHome() throws Exception {
        File ccRoot = getData("arbitrary_cc_home");
        File arbitraryCCHome = FilesystemUtils.createDirectory("arbitraryCCHome");
        FileUtils.copyDirectoryToDirectory(ccRoot, arbitraryCCHome);
        File copiedCCRoot = new File(arbitraryCCHome, ccRoot.getName());
        return new File(copiedCCRoot, "config.xml");
    }

    public static File getPassingBuildLbuildAsFile() throws Exception {
        return getData("logs/project1/" + PASSING_BUILD_LBUILD_0_XML);
    }

    public static File getFailedBuildLbuildAsFile() throws Exception {
        return getData("logs/project1/" + FAILING_BUILD_XML);
    }

    public static File getBigLogFile() throws Exception {
        return getData("misc/" + "log20070511103055.xml");
    }

    public static File getMiscConfigFile() throws Exception {
        return getData("misc/" + "config.xml");
    }

    public static File getProject1ArtifactDirAsFile() throws Exception {
        return getData("arbitrary_artifacts/artifacts/project1");
    }

    public static File getProjectSpaceArtifactDirAsFile() throws Exception {
        return getData("arbitrary_artifacts/artifacts/project space");
    }

    public static File getLogDirAsFile() throws Exception {
        return getData("logs");
    }
    
    public static File getArtifactsDirAsFile() throws Exception {
        return getData("arbitrary_artifacts/artifacts");
    }
    
    public static File getProjectLogDirAsFile(String project) throws Exception {
        return new File(getData("logs"), project);
    }
    
    
    public static File getProject2BuildAsFile() throws Exception {
        return getData("logs/project2/" + LOGFILE_OF_PROJECT2);
    }

    private static File getData(String filename) throws URISyntaxException {
        return new File(TEST_DATA_DIR + filename);
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        return file;
    }

    public static File createTempDirectory(String path) throws IOException {
        File file = File.createTempFile("tmp", "tmp");
        File dir = new File(file.getParent() + File.separator + path);
        dir.deleteOnExit();
        dir.mkdir();
        return dir;
    }

    public static void writeContentToFile(File file, String contents) throws IOException {
        StringBuffer configuration = new StringBuffer();
        configuration.append(contents);
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write(contents);
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }

    public static File createDefaultCCConfigFile() throws IOException {
        File configurationFile = createTempFile("config", ".xml");
        writeContentToFile(configurationFile,
                "<cruisecontrol><project name=\"project1\"/></cruisecontrol>\n");
        return configurationFile;
    }

    public static String readFileContent(File file) throws Exception {
        BufferedReader reader = null;
        StringBuffer sb = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();
    }

}