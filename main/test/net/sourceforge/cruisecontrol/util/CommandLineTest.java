package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;

/**
 * @author DRollo
 * Date: Jan 2, 2007
 * Time: 12:08:33 PM
 */
public class CommandLineTest extends TestCase {

    private static final String DBL_QUOTE = "\"";

    private static final String EXEC_WITH_SPACES = "dummyExecutable with spaces";

    public void testToStrings() throws Exception {
        final Commandline cl = new Commandline();
        cl.setExecutable(EXEC_WITH_SPACES);

        final String argWithSpacesAndQuotes = "arg1='spaced single quoted value'";
        final String arg2NoSpaces = "arg2NoSpaces=value2";
        final String arg3Spaces = "arg3Spaces=value for 3";
        cl.addArguments(new String[] { argWithSpacesAndQuotes, arg2NoSpaces, arg3Spaces});

        final String expectedWithQuotes = DBL_QUOTE + EXEC_WITH_SPACES + DBL_QUOTE
                + " " + DBL_QUOTE + argWithSpacesAndQuotes  + DBL_QUOTE
                + " " + arg2NoSpaces
                + " " + DBL_QUOTE + arg3Spaces + DBL_QUOTE;
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
}
