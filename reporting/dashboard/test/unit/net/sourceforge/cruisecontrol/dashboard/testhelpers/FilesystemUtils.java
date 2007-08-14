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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public final class FilesystemUtils {

    private static final String ROOT = "target" + File.separator + "testfiles";

    private FilesystemUtils() {
    }

    public static File createDirectory(String directoryName) {
        File directory = new File(getTestRootDir(), directoryName);
        deleteDirectory(directory);
        directory.mkdir();
        directory.deleteOnExit();
        return directory;
    }

    private static void deleteDirectory(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
        }
    }

    public static File createDirectory(String directoryName, String parent) {
        File directory = new File(getTestRootDir(), parent + File.separator + directoryName);
        deleteDirectory(directory);
        directory.mkdir();
        directory.deleteOnExit();
        return directory;
    }

    public static File getTestRootDir() {
        File root = new File(ROOT);
        if (!root.exists() && !root.mkdir()) {
            throw new RuntimeException("Failed to create directory for test data [" + root.getAbsolutePath() + "]");
        }
        return root;
    }

    public static File createFile(String filename, File directory) throws IOException {
        File file = new File(directory.getAbsolutePath() + File.separator + filename);
        file.createNewFile();
        file.deleteOnExit();
        return file;
    }

    public static File createFile(String filename) throws IOException {
        File file = new File(ROOT + File.separator + filename);
        file.createNewFile();
        file.deleteOnExit();
        return file;
    }

}
