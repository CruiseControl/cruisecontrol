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

/**
 * Tracks a "buffer" of lines from a build, which allows a caller to ask all lines after a certain starting line number.
 * Note that the buffer rotates and will only include up to the last maxLines lines written to the buffer.
 */
public class BuildOutputBuffer implements StreamConsumer {

    private final int maxLines;
    private String[] lineBuffer;
    private int totalLines;
    private File data;
    private int fileLocation;

    /**
     * @param maxLines Maximum number of lines that can be placed into the buffer.
     */
    public BuildOutputBuffer(int maxLines) {
        this.maxLines = maxLines;
        clear();
    }

    public void clear() {
        lineBuffer = new String[this.maxLines];
        totalLines = 0;
    }

    /**
     * Consumes the line provided by adding it next in the buffer. If the buffer is full, it starts back at the
     * beginning of the buffer!!!
     */
    public synchronized void consumeLine(String line) {
        lineBuffer[totalLines % lineBuffer.length] = line;
        totalLines += 1;
    }

    /**
     * @return All lines available from firstLine (inclusive) up to maxLines.
     */
    public String[] retrieveLines(int firstLine) {
        fileLoad();
        // TODO: this implementation needs refactoring to make it clearer
        int count = totalLines - firstLine;
        if (count <= 0) {
            return new String[0];
        }
        String[] result;
        int i = 0;
        if (count > lineBuffer.length) {
            int linesSkipped = count - lineBuffer.length;
            firstLine += linesSkipped;
            count = lineBuffer.length;
            result = new String[count + 1];
            result[i++] = "(Skipped " + linesSkipped + " lines)";
        } else {
            result = new String[count];
        }
        for (; count-- > 0; i++) {
            result[i] = lineBuffer[firstLine++ % lineBuffer.length];
        }
        return result;
    }

    public void setFile(final File outFile) {
        this.data = outFile;
        this.fileLocation = 0;
    }

    private void fileLoad() {
        if (data == null || !data.exists()) { return; }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(data));

            skipLines(reader, fileLocation);

            String line = reader.readLine();
            while (line != null) {
                consumeLine(line);
                line = reader.readLine();
                fileLocation++;
            }
        } catch (IOException e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void skipLines(final BufferedReader inFile, final int numToSkip) throws IOException {
        for (int i = 0; i < numToSkip; i++) { inFile.readLine(); }
    }
}
