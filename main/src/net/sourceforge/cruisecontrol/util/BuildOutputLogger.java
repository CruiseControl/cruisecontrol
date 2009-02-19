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
package net.sourceforge.cruisecontrol.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

/**
 * Logs all sysout and syserr to a file.
 */
public class BuildOutputLogger implements StreamConsumer {

    public static final int MAX_LINES = 1000;
    private File data;

    public BuildOutputLogger(File outputFile) {
        data = outputFile;
    }

    public void clear() {
        if (noDataFile()) { return; }
        data.delete();
    }

    public synchronized void consumeLine(String line) {
        if (data == null) { throw new RuntimeException("No log file specified"); }
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(data, true));
            out.println(line);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (out != null) { out.close(); }
        }
    }

    /**
     * @param firstLine line to skip to.
     * @return All lines available from firstLine (inclusive) up to MAX_LINES.
     */
    public String[] retrieveLines(int firstLine) {
        if (noDataFile()) { return new String[0]; }
        List<String> lines = loadFile(firstLine);
        return lines.toArray(new String[lines.size()]);
    }

    private List<String> loadFile(int firstLine) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(data));
            skipLines(reader, firstLine);
            return readUptoMaxLines(reader);
        } catch (IOException e) {
            return new ArrayList<String>();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private List<String> readUptoMaxLines(BufferedReader reader) throws IOException {
        List<String> result = new ArrayList<String>();
        String line = reader.readLine();
        while (line != null && result.size() < MAX_LINES) {
            result.add(line);
            line = reader.readLine();
        }
        return result;
    }

    private void skipLines(BufferedReader inFile, int numToSkip) throws IOException {
        for (int i = 0; i < numToSkip; i++) { inFile.readLine(); }
    }

    private boolean noDataFile() {
        return data == null || !data.exists();
    }

    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null) { return false; }
        if (this.getClass() != other.getClass()) { return false; }

        return equals((BuildOutputLogger) other);
    }

    private boolean equals(BuildOutputLogger other) {
        return dataEquals(this.data, other.data);
    }

    private boolean dataEquals(File mine, File other) {
        if (mine == null) { return other == null; }
        boolean pathSame = mine.getPath().equals(other.getPath());
        boolean nameSame = mine.getName().equals(other.getName());
        return pathSame && nameSame;
    }

    public int hashCode() {
        return (data != null ? data.hashCode() : 0);
    }

    public String toString() {
        String path = data == null ? "null" : (data.getAbsolutePath());
        return "<BuildOutputLogger data=" + path + ">";
    }
}
