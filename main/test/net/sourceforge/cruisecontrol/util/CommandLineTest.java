package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * @author DRollo
 * Date: Jan 2, 2007
 * Time: 12:08:33 PM
 */
public class CommandLineTest extends TestCase {

    private static final String DBL_QUOTE = "\"";

    private static final String EXEC_WITH_SPACES = "dummyExecutable with spaces";

    private static final String ARG_SPACES_NOQUOTES = "arg1='spaced single quoted value'";
    private static final String ARG_NOSPACES = "arg2=value2";
    private static final String ARG_SPACES = "arg3=value for 3";

    public void testToStringWithSeparator() throws Exception {
        final String separator = "], [";
        assertEquals("", Commandline.toString(null, false, separator));

        assertEquals(ARG_SPACES_NOQUOTES,
                Commandline.toString(new String[]{ARG_SPACES_NOQUOTES}, false, separator));

        assertEquals(ARG_SPACES_NOQUOTES + separator + ARG_NOSPACES,
                Commandline.toString(new String[]{ARG_SPACES_NOQUOTES, ARG_NOSPACES}, false, separator));

        assertEquals(ARG_SPACES_NOQUOTES + separator + ARG_NOSPACES + separator + ARG_SPACES,
                Commandline.toString(new String[]{ARG_SPACES_NOQUOTES, ARG_NOSPACES, ARG_SPACES},
                        false, separator));
    }

    public void testToStrings() throws Exception {
        final Commandline cl = new Commandline();
        cl.setExecutable(EXEC_WITH_SPACES);

        cl.addArguments(new String[] {ARG_SPACES_NOQUOTES, ARG_NOSPACES, ARG_SPACES});

        final String expectedWithQuotes = DBL_QUOTE + EXEC_WITH_SPACES + DBL_QUOTE
                + " " + DBL_QUOTE + ARG_SPACES_NOQUOTES + DBL_QUOTE
                + " " + ARG_NOSPACES
                + " " + DBL_QUOTE + ARG_SPACES + DBL_QUOTE; 
        assertEquals(expectedWithQuotes, cl.toString());

        assertEquals(expectedWithQuotes.replaceAll(DBL_QUOTE, ""), cl.toStringNoQuoting());

        assertEquals("Did the impl of CommandLine.toString() change?", expectedWithQuotes, cl + "");
    }

    public void testToStringMisMatchedQuote() {
        final Commandline cl2 = new Commandline();
        cl2.setExecutable(EXEC_WITH_SPACES);
        final String argWithMismatchedDblQuote = "argMisMatch='singlequoted\"WithMismatchedDblQuote'";
        cl2.addArguments(new String[] { argWithMismatchedDblQuote });
        assertEquals("Did behavior of mismatched quotes change? Previously it would truncate args.",
                DBL_QUOTE + EXEC_WITH_SPACES + DBL_QUOTE + " ", cl2.toString());
    }

    public void testShouldInvokeProvidedRuntime() throws IOException {
        MockRuntime mockRuntime = new MockRuntime();
        Commandline command = new Commandline("doesnt matter", mockRuntime);
        command.execute();
        assertTrue(mockRuntime.wasCalled());
    }

    class MockRuntime extends CruiseRuntime {
        private boolean wasCalled;

        public Process exec(String[] commandline) throws IOException {
            wasCalled = true;
            return new MockProcess();
        }

        public Process exec(String[] commandline, String[] o, File workingDir) throws IOException {
            wasCalled = true;
            return new MockProcess();
        }

        public boolean wasCalled() {
            return wasCalled;
        }
    }
}
