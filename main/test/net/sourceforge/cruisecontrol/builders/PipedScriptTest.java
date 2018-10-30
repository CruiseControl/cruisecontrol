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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jdom2.Element;
import org.junit.Test;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestCase;
import net.sourceforge.cruisecontrol.util.Util;


public class PipedScriptTest {

    /** Tests {@link PipedScript.Helpers#split(String)}*/
    @Test
    public void testSplit() {
        TestCase.assertEquals(new String[0], PipedScript.Helpers.split(null));
        TestCase.assertEquals(new String[0], PipedScript.Helpers.split(""));

        TestCase.assertEquals(new String[] {"A"}, PipedScript.Helpers.split("A"));
        TestCase.assertEquals(new String[] {"B"}, PipedScript.Helpers.split("B "));
        TestCase.assertEquals(new String[] {"C"}, PipedScript.Helpers.split(" C"));
        TestCase.assertEquals(new String[] {"D"}, PipedScript.Helpers.split(" D "));


        final String[] res = new String[] {"1","2","3","4"};

        TestCase.assertEquals(res, PipedScript.Helpers.split("1,2,3,4"));
        TestCase.assertEquals(res, PipedScript.Helpers.split("1, 2, 3, 4"));
        TestCase.assertEquals(res, PipedScript.Helpers.split("1 , 2 , 3 , 4"));
        TestCase.assertEquals(res, PipedScript.Helpers.split("1 ,2 , 3,4 "));
        TestCase.assertEquals(res, PipedScript.Helpers.split(" 1, 2 ,3,4"));
    }

    /** Tests {@link PipedScript.Helpers#split(String)}*/
    @Test
    public void testJoin() {
        TestCase.assertEquals("", PipedScript.Helpers.join((String[])null));
        TestCase.assertEquals("", PipedScript.Helpers.join((Collection<String>)null));

        TestCase.assertEquals("", PipedScript.Helpers.join(new String[0]));
        TestCase.assertEquals("", PipedScript.Helpers.join(new ArrayList<String>()));

        TestCase.assertEquals("A", PipedScript.Helpers.join(new String[] {"A"}));
        TestCase.assertEquals("1,2,3,4", PipedScript.Helpers.join(new String[] {"1","2","3","4"}));
    }


    /**
     * Checks if the STDIN of an external command is closed when it is not piped from another command.
     * When STDIO is not closed <code>cat</code> command should wait for infinite time.
     *
     * @throws CruiseControlException
     */
    @Test
    public void testBuild_stdinClose() throws CruiseControlException {
        // Can test only under Linux
        // I don't know an alternative on windows, which would accept STDIN
        if (Util.isWindows()) {
            return;
        }
        // ----

        final PipedExecScript script = new PipedExecScript();
        final Element buildLog = new Element("script");
        final long startTime = System.currentTimeMillis();

        /* Set 20 seconds long timeout. */
        script.setTimeout(20);
        script.setID("cat");
        script.setBuildLogParent(buildLog);
        script.setBuildProperties(Collections.<String, String> emptyMap());
        /* Read from stdin, but do not have it piped. The command must end immediately
         * without error, as the stdin MUST BE closed when determined that nothing can be
         * read from */
        script.setCommand("cat");  /* by default, 'cat' waits until its STDIN is closed */

        /* Run */
        script.validate();
        script.initialize();
        script.run();
        script.finish();

        /* First of all, the command must not run. 5 seconds must be far enough! */
        TestCase.assertTrue("command run far longer than it should!", (System.currentTimeMillis() - startTime) / 1000 < 5);
        /* No 'error' attribute must exist in the build log */
        TestCase.assertNull("error attribute was found in build log!", buildLog.getAttribute("error"));
    }

}
