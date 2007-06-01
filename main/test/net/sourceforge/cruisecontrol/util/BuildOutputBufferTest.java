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

import junit.framework.TestCase;

public class BuildOutputBufferTest extends TestCase {

    public void testShouldReturnEmptyArrayWhenBufferEmpty() throws Exception {

        BuildOutputBuffer buffer = new BuildOutputBuffer(10);

        int lines = buffer.retrieveLines(0).length;

        assertEquals(0, lines);

    }

    public void testShouldReturnEmptyArrayWhenFirstLineBeyondLinesInBuffer() throws Exception {

        BuildOutputBuffer buffer = new BuildOutputBuffer(10);
        fillBuffer(buffer, 5);

        int lines = buffer.retrieveLines(12).length;

        assertEquals(0, lines);

    }

    public void testShouldReturnAllLinesFromFirstLine() throws Exception {

        BuildOutputBuffer buffer = new BuildOutputBuffer(10);
        fillBuffer(buffer, 3);

        String[] lines = buffer.retrieveLines(0);

        assertEquals(3, lines.length);
        assertEquals("3", lines[2]);
        assertEquals("2", lines[1]);
        assertEquals("1", lines[0]);
    }

    public void testShouldReturnAllLinesFromStartLine() throws Exception {

        BuildOutputBuffer buffer = new BuildOutputBuffer(10);
        fillBuffer(buffer, 7);

        String[] lines = buffer.retrieveLines(4);

        assertEquals(3, lines.length);
        assertEquals("5", lines[0]);
        assertEquals("7", lines[2]);
    }

    public void testShouldReturnAllLinesAcrossWrap() throws Exception {

        BuildOutputBuffer buffer = new BuildOutputBuffer(10);
        fillBuffer(buffer, 13);

        String[] lines = buffer.retrieveLines(9);

        assertEquals(4, lines.length);
        assertEquals("10", lines[0]);
        assertEquals("11", lines[1]);
        assertEquals("12", lines[2]);
        assertEquals("13", lines[3]);

    }

    public void testShouldReturnAvailableLinesPlusOmissionMarkerWhenLinesWereOverwritten() {

        BuildOutputBuffer buffer = new BuildOutputBuffer(10);
        fillBuffer(buffer, 25);

        String[] lines = buffer.retrieveLines(10);

        assertEquals(11, lines.length);
        assertEquals("16", lines[1]);
        assertEquals("25", lines[10]);
        assertEquals("(Skipped 5 lines)", lines[0]);
    }

    private void fillBuffer(BuildOutputBuffer buffer, int count) {
        for (int i = 0; i < count; i++) {
            buffer.consumeLine(new Integer(1 + i).toString());
        }
    }
}