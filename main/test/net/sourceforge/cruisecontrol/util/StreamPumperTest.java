/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public class StreamPumperTest extends TestCase {

    public void testPumping() {
        String line1 = "line1";
        String line2 = "line2";
        String lines = line1 + "\n" + line2;
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(lines.getBytes());

        TestConsumer consumer = new TestConsumer();
        StreamPumper pumper = new StreamPumper(inputStream, consumer);
        pumper.run();

        //Check the consumer to see if it got both lines.
        assertTrue(consumer.wasLineConsumed(line1));
        assertTrue(consumer.wasLineConsumed(line2));
    }

    public void testInvalidStreamChars() {
        String line1 = "pre:-line1\u001b";
        String line2 = "li\u0008ne2";
        String lines = line1 + "\n" + line2;
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(lines.getBytes());

        TestConsumer consumer = new TestConsumer();
        StreamPumper pumper = new StreamPumper(inputStream, consumer);
        pumper.run();

        //Check the consumer to see if it got both lines, less invalid chars.
        assertTrue(consumer.wasLineConsumed("pre:-line1"));
        assertTrue(consumer.wasLineConsumed("line2"));
    }

    public void testNoSystemOut() {
        final PrintStream oldOut = System.out;
        final ByteArrayOutputStream newOut = new ByteArrayOutputStream();
        final PrintStream newPrintStreamOut = new PrintStream(newOut);
        try {
            System.setOut(newPrintStreamOut);
            InputStream input = new ByteArrayInputStream(
                    "some input".getBytes());
            new StreamPumper(input, null).run();
            assertEquals(0, newOut.toByteArray().length);
        } finally {
            System.setOut(oldOut);
            newPrintStreamOut.close();
        }
    }
}

/**
 * Used by the test to track whether a line actually got consumed or not.
 */
class TestConsumer implements StreamConsumer {

    private final List lines = new ArrayList();

    /**
     * Checks to see if this consumer consumed a particular line.
     *
     * @param testLine Line to test for.
     * @return true if the line gets consumed, else false.
     */
    boolean wasLineConsumed(String testLine) {

        if (lines.contains(testLine)) {
            return true;
        }

        //If we got here, then the line wasn't consumed
        return false;
    }

    public void consumeLine(String line) {
        lines.add(line);
    }
}
