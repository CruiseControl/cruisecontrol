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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.MockCommandline;

/**
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class UCMTest extends TestCase {

    private UCM ucm;
    private static final byte[] BUF_EMPTY = new byte[0];

    protected void setUp() throws Exception {
        super.setUp();

        ucm = new UCM();
    }

    public void testSuccessfulValidation() throws Exception {
        ucm.setViewPath("path");
        ucm.setStream("branch");

        try {
            ucm.validate();
        } catch (CruiseControlException e) {
            fail("UCM should not throw exceptions when required attributes are set.");
        }
    }

    public void testValidateShouldFailWhenRequiredAttributesAreNotSet() throws Exception {
        try {
            ucm.validate();
            fail("UCM should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    public void testShouldThrowFailWhenRebasesAreDetectedWithoutAPVOB() throws Exception {
        ucm.setViewPath("path");
        ucm.setStream("branch");
        ucm.setRebases(true);

        try {
            ucm.validate();
            fail("UCM should throw exceptions when rebases are detected without a pvob.");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    public void testSuccessfulValidationWithRebasesAndPVOB() {
        ucm.setViewPath("path");
        ucm.setStream("branch");
        ucm.setRebases(true);
        ucm.setPvob("pvob");

        try {
            ucm.validate();
        } catch (CruiseControlException e) {
            fail("UCM should not throw exceptions when required attributes are set.");
        }
    }

    public void testBuildListContributorsCommand() throws Exception {
        final String dummyActivityID = "dummyActivityID";
        final Commandline actualCommandline = ucm.buildListContributorsCommand(dummyActivityID);
        TestUtil.assertArray("Wrong commandline",
                new String[] {
                        "cleartool",
                        "describe",
                        "-fmt",
                        "%[contrib_acts]Xp", // CC-815 - tested on Linux, needs testing on Windows, Unix
                        dummyActivityID
                },
                actualCommandline.getCommandline());
    }

    public void testParseStream() throws Exception {
        List<Commandline> commands = new ArrayList<Commandline>();
        commands.add(emptyOutput());
        commands.add(loadCommandOutput("ucmstream_hyperlink.txt"));
        commands.add(emptyOutput());
        commands.add(emptyOutput());
        commands.add(emptyOutput());

        UCM control = createControl(commands);
        List mods = control.parseRebases(loadAsStream("ucmstream_output.txt"));

        assertEquals(5, mods.size());
    }

    public void testParseAttachEntry() throws Exception {
        List<Commandline> commands = new ArrayList<Commandline>();
        commands.add(loadCommandOutput("ucmstream_hyperlink.txt"));

        UCM control = createControl(commands);
        Modification mod = control.parseRebaseEntry(loadAsString("ucmstream_attach_entry.txt"));

        assertNotNull(mod);
        assertEquals("sxc25", mod.userName);

        Calendar cal = Calendar.getInstance();
        // Make sure any fields we're not specifying match
        cal.setTime(mod.modifiedTime);

        assertEquals("Year incorrect", 2006, cal.get(Calendar.YEAR));
        assertEquals("Month incorrect", Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals("Day incorrect", 16, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals("Hour incorrect", 17, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Minute incorrect", 46, cal.get(Calendar.MINUTE));
        assertEquals("Second incorrect", 45, cal.get(Calendar.SECOND));
        assertEquals("Added dependency of stream:protocol.1_integration@/vobs/oscar_pvob on "
                + "baseline:tools.1.67.1352@/vobs/oscar_pvob", mod.comment);
    }

    public void testParseRemoveEntry() throws Exception {
        List<Commandline> commands = new ArrayList<Commandline>();
        commands.add(emptyOutput());

        UCM control = createControl(commands);
        Modification mod = control.parseRebaseEntry(loadAsString("ucmstream_remove_entry.txt"));

        assertNotNull(mod);
        assertEquals("sxc25", mod.userName);

        Calendar cal = Calendar.getInstance();
        // Make sure any fields we're not specifying match
        cal.setTime(mod.modifiedTime);
        assertEquals("Year incorrect", 2006, cal.get(Calendar.YEAR));
        assertEquals("Month incorrect", Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals("Day incorrect", 16, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals("Hour incorrect", 17, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Minute incorrect", 50, cal.get(Calendar.MINUTE));
        assertEquals("Second incorrect", 4, cal.get(Calendar.SECOND));
        assertEquals("Removed dependency", mod.comment);
    }

    public void testParseEmptyHyperlinkDescription() throws Exception {
        UCM control = new UCM();
        UCM.Hyperlink link = control.parseHyperlinkDescription(new ByteArrayInputStream(BUF_EMPTY));

        assertEquals("", link.getFrom());
        assertEquals("", link.getTo());
    }

    public void testParseHyperlinkDescription() throws Exception {
        UCM control = createControl(new ArrayList());
        UCM.Hyperlink link = control.parseHyperlinkDescription(loadAsStream("ucmstream_hyperlink.txt"));

        assertEquals("stream:protocol.1_integration@/vobs/oscar_pvob", link.getFrom());
        assertEquals("baseline:tools.1.67.1352@/vobs/oscar_pvob", link.getTo());
    }

    public void testParseAttachName() throws Exception {
        UCM control = createControl(new ArrayList());
        String linkName = control.parseLinkName(loadAsString("ucmstream_attached_hyperlink.txt"));

        assertEquals("UseBaseline@61974", linkName);
    }

    public void testParseRemoveName() throws Exception {
        UCM control = createControl(new ArrayList());
        String linkName = control.parseLinkName(loadAsString("ucmstream_removed_hyperlink.txt"));

        assertEquals("UseBaseline@61977", linkName);
    }

    private MockCommandline loadCommandOutput(String outputResource) {
        MockCommandline commandline = new MockCommandline();
        commandline.setAssertCorrectCommandline(false);
        commandline.setProcessInputStream(loadAsStream(outputResource));
        commandline.setProcessOutputStream(new ByteArrayOutputStream());
        commandline.setProcessErrorStream(new ByteArrayInputStream(BUF_EMPTY));
        return commandline;
    }

    private InputStream loadAsStream(String resource) {
        ClassLoader loader = getClass().getClassLoader();
        String pkg = getClass().getPackage().getName();
        String prefix = pkg.replace('.', '/');
        return loader.getResourceAsStream(prefix + "/" + resource);
    }

    private String loadAsString(String resource) throws Exception {
        InputStream input = loadAsStream(resource);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];

        for (int i = input.read(buf); i != -1; i = input.read(buf)) {
            baos.write(buf, 0, i);
        }

        return new String(baos.toByteArray(), "UTF-8");
    }

    private MockCommandline emptyOutput() {
        MockCommandline commandline = new MockCommandline();
        commandline.setAssertCorrectCommandline(false);
        commandline.setProcessErrorStream(new ByteArrayInputStream(BUF_EMPTY));
        commandline.setProcessOutputStream(new ByteArrayOutputStream());
        commandline.setProcessInputStream(new ByteArrayInputStream(BUF_EMPTY));
        return commandline;
    }

    public UCM createControl(Collection commands) {
        final Iterator iter = commands.iterator();

        return new UCM() {
            protected Commandline buildGetHyperlinkCommandline(String linkName) {
                return (Commandline) iter.next();
            }

            protected Commandline buildDetectRebasesCommand(String lastBuildDate) {
                return (Commandline) iter.next();
            }
        };
    }
}
