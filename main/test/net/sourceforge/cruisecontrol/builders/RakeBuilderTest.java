/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.builders;

// import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.MockCommandline;
import net.sourceforge.cruisecontrol.util.MockProcess;

public class RakeBuilderTest extends TestCase {

    static class InputBasedMockCommandLineBuilder {
        Commandline buildCommandline(final InputStream inputStream) {
            final MockCommandline mockCommandline = getMockCommandline();
            mockCommandline.setAssertCorrectCommandline(false);
            mockCommandline.setProcessErrorStream(new PipedInputStream());
            mockCommandline.setProcessInputStream(inputStream);
            mockCommandline.setProcessOutputStream(new PipedOutputStream());
            return mockCommandline;
        }

        MockCommandline getMockCommandline() {
            return new MockCommandline();
        }
    }

     // process that times out...
    static class TimeoutProcess extends MockProcess {
    private long timeoutMillis;
    TimeoutProcess(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public synchronized void destroy() {
       notifyAll();
    }

    public int waitFor() throws InterruptedException {
        synchronized (this) {
            try {
                this.wait(timeoutMillis);
            } catch (InterruptedException e) {
                }
            }
            return super.waitFor();
        }
    }

    protected void setUp() throws Exception {
        //properties = new Hashtable();
        //properties.put("label", "200.1.23");
    }

    public void tearDown() {
        //properties = null;
    }

    public void testValidate() {
        RakeBuilder builder = new RakeBuilder();

        try {
            builder.validate();
        } catch (CruiseControlException e) {
            fail("rakebuilder has no required attributes");
        }

        builder.setTime("0100");
        builder.setBuildFile("buildfile");
        builder.setTarget("target");

        try {
            builder.validate();
        } catch (CruiseControlException e) {
            fail("validate should not throw exceptions when options are set.");
        }
    }

    /*
    public void testBuild() throws Exception {
        final InputStream emptyInputStream = new ByteArrayInputStream("".getBytes());
        final RakeBuilder builder = new RakeBuilder() {
           protected RakeScript getRakeScript() {
               return new RakeScript() {
                   public Commandline getCommandLine() {
                       return new InputBasedMockCommandLineBuilder().buildCommandline(emptyInputStream);
                   }
               };
            }
        };
        builder.setBuildFile("rakefile.rb");
        builder.setTarget("target");
        builder.validate();
        HashMap buildProperties = new HashMap();
        Element buildElement = builder.build(buildProperties);
        int infoCount = getInfoCount(buildElement);
        assertEquals(1, infoCount);
    }
    */

    /*
    public void testBuildTimeout() throws Exception {
        final InputStream emptyInputStream = new ByteArrayInputStream("".getBytes());
        final RakeBuilder builder = new RakeBuilder() {
           protected RakeScript getRakeScript() {
               return new RakeScript() {
                   public Commandline getCommandLine() {
                       return new InputBasedMockCommandLineBuilder().buildCommandline(emptyInputStream);
                   }
               };
            }
        };
        builder.setBuildFile("rakefile.rb");
        builder.setTarget("target");
        builder.setTimeout(5);
        builder.validate();
        HashMap buildProperties = new HashMap();
        long startTime = System.currentTimeMillis();
        Element buildElement = builder.build(buildProperties);
        assertTrue((System.currentTimeMillis() - startTime) < 9 * 1000L);
        assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);

    }
    */

    /*public void testValidateBuildFileWorksForNonDefaultDirectory() throws IOException, CruiseControlException {
        File rakeworkdir = new File("rakeworkdir");
        rakeworkdir.mkdir();
        File file = File.createTempFile("rakefile", ".rb", rakeworkdir);
        RakeBuilder builder = new RakeBuilder();
        builder.setTarget("target");
        builder.setBuildFile("buildfile");
        builder.setWorkingDir(rakeworkdir.getAbsolutePath());
        builder.setBuildFile(file.getName());

        builder.validateBuildFileExists();

        builder.setBuildFile(file.getAbsolutePath());
        builder.validateBuildFileExists();

        file.delete();
        try {
            builder.validateBuildFileExists();
            fail();
        } catch (CruiseControlException expected) {
        }

        builder.setBuildFile(file.getName());
        try {
            builder.validateBuildFileExists();
            fail();
        } catch (CruiseControlException expected) {
        }
    }*/

    /*
    private int getInfoCount(Element buildElement) {
        int infoFoundCount = 0;
        Iterator targetIterator = buildElement.getChildren("message").iterator();
        String name;
        while (targetIterator.hasNext()) {
            name = ((Element) targetIterator.next()).getAttributeValue("priority");
            if (name.equals("info")) {
                infoFoundCount++;
            }
        }
        return infoFoundCount;
    }
    */
}
