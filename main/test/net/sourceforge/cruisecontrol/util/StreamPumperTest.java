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
        new Thread(pumper).run();

        //Check the consumer to see if it got both lines.
        assertTrue(consumer.wasLineConsumed(line1, 1000));
        assertTrue(consumer.wasLineConsumed(line2, 1000));
    }

    public void testNoSystemOut() {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream newOut = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(newOut));
            InputStream input = new ByteArrayInputStream(
                    "some input".getBytes());
            new StreamPumper(input, null).run();
            assertEquals(0, newOut.toByteArray().length);
        } finally {
            System.setOut(oldOut);
        }
    }
}

/**
 * Used by the test to track whether a line actually got consumed or not.
 */
class TestConsumer implements StreamConsumer {

    private List lines = new ArrayList();

    /**
     * Checks to see if this consumer consumed a particular line. This method
     * will wait up to timeout number of milliseconds for the line to get
     * consumed.
     *
     * @param testLine Line to test for.
     * @param timeout Number of milliseconds to wait for the line.
     * @return true if the line gets consumed, else false.
     */
    public boolean wasLineConsumed(String testLine, long timeout) {

        long start = System.currentTimeMillis();
        long trialTime = 0;

        do {
            if (lines.contains(testLine)) {
                return true;
            }

            //Sleep a bit.
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                //ignoring...
            }

            //How long have been waiting for the line?
            trialTime = System.currentTimeMillis() - start;

        } while (trialTime < timeout);

        //If we got here, then the line wasn't consume within the timeout
        return false;
    }

    public void consumeLine(String line) {
        lines.add(line);
    }
}
