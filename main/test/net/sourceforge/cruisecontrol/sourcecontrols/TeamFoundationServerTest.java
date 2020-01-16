package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

/**
 * Tests for Team Foundation Server source control.
 * 
 * @author <a href="http://www.woodwardweb.com">Martin Woodward</a>
 */
public class TeamFoundationServerTest extends TestCase {

    private final TimeZone originalTimeZone = TimeZone.getDefault();
    private final Locale originalLocal = Locale.getDefault();
    private final Date minDate = (new GregorianCalendar(1976, 3, 10)).getTime();;

    private TeamFoundationServer tfs;

    
    private static final String CHANGESET_DATA = "Changeset: 1645\n"
            + "User: martin\n"
            + "Date: 13 December 2006 21:51:50\n"
            + "\n"
            + "Comment:\n"
            + "  test line 1!\u90B5\n"
            + "  test line 2\n"
            + "\n"
            + "Items:\n"
            + "  edit $/demo/connectfour/src/net/sourceforge/cruisecontrol/sampleproject/\u90B5/connectfour/Cell.java\n"
            + "\n" + "Check-in Notes:\n" + "  Code Reviewer:\n" + "  Performance Reviewer:\n"
            + "  Security Reviewer:\n" + "\n" + "\n";

    private static final String CHANGESET_DATA_US = "Changeset: 1645\n" + "User: martin\n"
            + "Date: Wednesday, December 13, 2006 9:51:50 PM\n" + "\n" + "Comment:\n" + "  test line 1!\n"
            + "  test line 2\n" + "\n" + "Items:\n"
            + "  edit $/demo/connectfour/src/net/sourceforge/cruisecontrol/sampleproject/connectfour/Cell.java\n"
            + "\n" + "Check-in Notes:\n" + "  Code Reviewer:\n" + "  Performance Reviewer:\n"
            + "  Security Reviewer:\n" + "\n" + "\n";

    private static final String CHANGESET_DATA_MIN = "Changeset: 1645\n" + "User: martin\n"
            + "Date: 13 December 2006 21:51:50\n" + "\n" + "Comment:\n" + "\n" + "Items:\n"
            + "  edit $/demo/connectfour/src/net/sourceforge/cruisecontrol/sampleproject/connectfour/Cell.java\n"
            + "\n" + "\n";

    private static final String CHANGESET_DATA_MULTI_ITEM = "Changeset: 1645\n" + "User: martin\n"
            + "Date: 13 December 2006 21:51:50\n" + "\n" + "Comment:\n" + "  test line 1!\n" + "  test line 2\n" + "\n"
            + "Items:\n" + "  edit $/demo/connectfour/README.txt\n" + "  edit, rename "
            + "$/demo/connectfour/src/net/sourceforge/cruisecontrol/sampleproject/connectfour/Cell.java\n"
            + "  add $/demo/connectfour/src/net/sourceforge/cruisecontrol/sampleproject/connectfour/text.txt\n" + "\n"
            + "Check-in Notes:\n" + "  Code Reviewer:\n" + "  Performance Reviewer:\n" + "  Security Reviewer:\n"
            + "\n" + "\n";

    private static final String CHANGESET_DATA_TEAMPRISE = "Changeset: 1650\n" + "User:\tCDESG\\martin\n"
            + "Date:\t14-Dec-2006 16:06:37\n" + "\n" + "Comment:\n" + "  Making a change with a few things in it.\n"
            + "Lets see what happens.\n\nEspecially with carrage returns in the comments !!\n" + "\n" + "Items:\n"
            + "  edit $/demo/connectfour/README.txt\n" + "  edit, rename "
            + "$/demo/connectfour/src/net/sourceforge/cruisecontrol/sampleproject/connectfour/Cell.java\n" + "  add "
            + "$/demo/connectfour/src/net/sourceforge/cruisecontrol/sampleproject/connectfour/"
            + "new file with spaces and a very long file name so we can see what happens with wrapping.txt\n" + "\n"
            + "Check-in Notes:\n" + "\n" + "  Code Reviewer:\n" + "  Performance Reviewer:\n"
            + "  Security Reviewer:\n" + "\n" + "\n";

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        tfs = new TeamFoundationServer();
        tfs.setServer("http://tfsserver:8080");
        tfs.setProjectPath("$/TeamProjectName/path");
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        TimeZone.setDefault(originalTimeZone);
        Locale.setDefault(originalLocal);
        tfs = null;
    }

    /**
     * Test method for
     * {@link net.sourceforge.cruisecontrol.sourcecontrols.TeamFoundationServer#formatUTCDate(java.util.Date)}.
     */
    public void testFormatUTCDate() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        Date janFirstAM2006 = new GregorianCalendar(2006, Calendar.JANUARY, 1, 1, 1, 1).getTime();
        assertEquals("2006-01-01T01:01:01Z", TeamFoundationServer.formatUTCDate(janFirstAM2006));

        Date decThirtyFirstPM2006 = new GregorianCalendar(2006, Calendar.DECEMBER, 31, 23, 59, 59).getTime();
        assertEquals("2006-12-31T23:59:59Z", TeamFoundationServer.formatUTCDate(decThirtyFirstPM2006));

        // Test running in diferent timezones.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-06:00")); // CST

        Date marTenth1976 = new GregorianCalendar(1976, Calendar.MARCH, 10, 12, 13, 14).getTime();
        assertEquals("1976-03-10T18:13:14Z", TeamFoundationServer.formatUTCDate(marTenth1976));

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+10:00"));

        Date aprilEleventh2004 = new GregorianCalendar(2004, Calendar.APRIL, 11, 18, 37, 5).getTime();
        assertEquals("2004-04-11T08:37:05Z", TeamFoundationServer.formatUTCDate(aprilEleventh2004));

    }

    /**
     * History command should be in the following format
     * 
     * tf history /noprompt /server:http://tfsserver:8080 $/TeamProjectName/path
     * /version:D2006-12-01T01:01:01Z~D2006-12-13T20:00:00Z /recursive /user:*
     * /format:detailed
     */
    public void testBuildHistoryCommand() throws CruiseControlException {

        Date checkTime = new Date();
        long tenMinutes = 10 * 60 * 1000;
        Date lastBuild = new Date(checkTime.getTime() - tenMinutes);

        String[] expectedCmd = new String[] {
                "tf",
                "history",
                "-noprompt",
                "-server:http://tfsserver:8080",
                "$/TeamProjectName/path",
                "-version:D" + TeamFoundationServer.formatUTCDate(lastBuild) + "~D"
                        + TeamFoundationServer.formatUTCDate(checkTime), "-recursive", "-format:detailed" };

        String[] actualCmd = tfs.buildHistoryCommand(lastBuild, checkTime).getCommandline();

        assertArraysEquals(expectedCmd, actualCmd);
    }

    /**
     * History command should be in the following format
     * 
     * tf history /noprompt /server:http://tfsserver:8080 $/TeamProjectName/path
     * /version:D2006-12-01T01:01:01Z~D2006-12-13T20:00:00Z /recursive /user:*
     * /format:detailed /login:"DOMAIN\name","password"
     */
    public void testBuildHistoryCommandWithCredentials() throws CruiseControlException {
        tfs.setUsername("DOMAIN\\name");
        tfs.setPassword("password");

        Date checkTime = new Date();
        long tenMinutes = 10 * 60 * 1000;
        Date lastBuild = new Date(checkTime.getTime() - tenMinutes);

        String[] expectedCmd = new String[] {
                "tf",
                "history",
                "-noprompt",
                "-server:http://tfsserver:8080",
                "$/TeamProjectName/path",
                "-version:D" + TeamFoundationServer.formatUTCDate(lastBuild) + "~D"
                        + TeamFoundationServer.formatUTCDate(checkTime), "-recursive", "-format:detailed",
                "-login:DOMAIN\\name,password" };

        String[] actualCmd = tfs.buildHistoryCommand(lastBuild, checkTime).getCommandline();

        assertArraysEquals(expectedCmd, actualCmd);
    }

    public void testParseChangesetGetChangeset() throws ParseException {
        Modification modification = parseChangeset(CHANGESET_DATA);
        assertEquals("1645", modification.revision);
    }

    public void testParseChangesetGetUser() throws ParseException {
        Modification modification = parseChangeset(CHANGESET_DATA);
        assertEquals("martin", modification.userName);
    }

    public void testParseChangesetGetDate() throws ParseException {
        Modification modification1 = parseChangeset(CHANGESET_DATA);
        assertEquals(new GregorianCalendar(2006, Calendar.DECEMBER, 13, 21, 51, 50).getTime(),
                modification1.modifiedTime);

        Modification modification2 = parseChangeset(CHANGESET_DATA_US);
        assertEquals(new GregorianCalendar(2006, Calendar.DECEMBER, 13, 21, 51, 50).getTime(),
                modification2.modifiedTime);
    }

    public void testParseChangesetGetComment() throws ParseException {
        Modification modification1 = parseChangeset(CHANGESET_DATA);
        assertEquals("test line 1!\u90B5\ntest line 2", modification1.comment);

        Modification modification2 = parseChangeset(CHANGESET_DATA_MIN);
        assertEquals("", modification2.comment);
    }

    public void testParseChangesetGetItems() throws ParseException {
        List list = TeamFoundationServer.TFHistoryParser.parseChangeset(CHANGESET_DATA_MULTI_ITEM, minDate, null);

        assertNotNull(list);
        assertEquals(3, list.size());
        assertTrue(list.get(0) instanceof Modification);
        assertTrue(list.get(1) instanceof Modification);
        assertTrue(list.get(2) instanceof Modification);
        assertEquals("$/demo/connectfour/README.txt", 
                ((Modification.ModifiedFile) ((Modification) list.get(0)).files.get(0)).fileName);
        assertEquals("edit", 
                ((Modification.ModifiedFile) ((Modification) list.get(0)).files.get(0)).action);
        assertEquals("$/demo/connectfour/src/net/sourceforge/cruisecontrol/sampleproject/connectfour/Cell.java",
                ((Modification.ModifiedFile) ((Modification) list.get(1)).files.get(0)).fileName);
        assertEquals("edit, rename", 
                ((Modification.ModifiedFile) ((Modification) list.get(1)).files.get(0)).action);
        assertEquals("$/demo/connectfour/src/net/sourceforge/cruisecontrol/sampleproject/connectfour/text.txt",
                ((Modification.ModifiedFile) ((Modification) list.get(2)).files.get(0)).fileName);
        assertEquals("add", 
                ((Modification.ModifiedFile) ((Modification) list.get(2)).files.get(0)).action);
    }

    public void testParseTeampriseChangeset() throws ParseException {
        List list = TeamFoundationServer.TFHistoryParser.parseChangeset(CHANGESET_DATA_TEAMPRISE, minDate, null);

        assertNotNull(list);
        assertEquals(3, list.size());
        assertTrue(list.get(0) instanceof Modification);
        assertTrue(list.get(1) instanceof Modification);
        assertTrue(list.get(2) instanceof Modification);
        assertEquals("$/demo/connectfour/README.txt", ((Modification) list.get(0)).getFileName());
    }

    public void testUnParseableDataFound() throws IOException {
        String tfOutput = "---------------------------------------------------"
                + "------------------------------------------------\r\n" + "Changeset: 1234\r\n" + "User: ethomson\r\n"
                + "Date: 14 December 2006 23:16:59\r\n" + "\r\n" + "Comment:\r\n" + "  Should throw a parse error\r\n"
                + "\r\n" + "Items:\r\n" + "  edit Not a valid TFS PATH.\r\n" + "  edit $Not a valid TFS PATH.\r\n"
                + "\r\n" + "Check-in Notes:\r\n" + "  Code Reviewer:\r\n" + "  Performance Reviewer:\r\n"
                + "  Security Reviewer:\r\n" + "\r\n";

        Reader reader = new StringReader(tfOutput);
        try {
            TeamFoundationServer.TFHistoryParser.parse(reader, minDate, null);
        } catch (ParseException e) {
            assertEquals("Parse error.", e.getMessage().substring(0, "Parse error.".length()));
            return;
        }
        fail("A parse exception should have been raised.");
    }

    public void testParseNoModifications() throws IOException, ParseException {
        String tfOutput = "No history entries were found for the item and version combination specified." + "\r\n";

        Reader reader = new StringReader(tfOutput);
        List list = TeamFoundationServer.TFHistoryParser.parse(reader, minDate, null);

        assertNotNull(list);
        assertEquals(0, list.size());
    }
    
    public void testTFS2008Bug() throws IOException, ParseException {
        // CC-735: Compatibility with TFS2008.
        // TFS2008 RTM has an issue whereby it will return the latest changeset from a query history
        // call even when that change happened before the date range passed in the query.        
        // We must therefore check that any returned changesets happened in the window
        // of time that interests us (i.e. occurred after or equal to our search start date)
        
        String tfOutput = "-----------------------------------------------------"
            + "----------------------------------------------\r\n" + "Changeset: 29\r\n" + "User: ptakale_cp\r\n"
            + "Date: 11 May 2006 20:23:37\r\n" + "\r\n" + "Comment:\r\n" + "  Upgraded to VS8\r\n" + "\r\n"
            + "Items:\r\n" + "  edit $/TestProject7/WindowsApplication1.sln\r\n"
            + "  edit $/TestProject7/WindowsApplication1/WindowsApplication1.vbproj.user\r\n"
            + "  edit $/TestProject7/WindowsApplication1/WindowsApplication1.vbproj\r\n" + "\r\n";

        Date lastBuildDate = (new GregorianCalendar(2007, 1, 1)).getTime();
            
        Reader reader = new StringReader(tfOutput);
        List list = TeamFoundationServer.TFHistoryParser.parse(reader, lastBuildDate, null);

        assertNotNull(list);
        assertEquals(0, list.size());
    }    

    public void testFullParseFromCodePlex() throws IOException, ParseException {
        // A bit of overkill this, but testing parsing routine on real live data
        // from CodePlex just to be sure
        String tfOutput = "-----------------------------------------------------"
                + "----------------------------------------------\r\n" + "Changeset: 29\r\n" + "User: ptakale_cp\r\n"
                + "Date: 11 May 2006 20:23:37\r\n" + "\r\n" + "Comment:\r\n" + "  Upgraded to VS8\r\n" + "\r\n"
                + "Items:\r\n" + "  edit $/TestProject7/WindowsApplication1.sln\r\n"
                + "  edit $/TestProject7/WindowsApplication1/WindowsApplication1.vbproj.user\r\n"
                + "  edit $/TestProject7/WindowsApplication1/WindowsApplication1.vbproj\r\n" + "\r\n"
                + "-----------------------------------------------------"
                + "----------------------------------------------\r\n" + "Changeset: 28\r\n" + "User: ptakale_cp\r\n"
                + "Date: 11 May 2006 20:20:21\r\n" + "\r\n" + "Comment:\r\n" + "  Sample Project check-in\r\n" + "\r\n"
                + "Items:\r\n" + "  add $/TestProject7/WindowsApplication1.sln\r\n"
                + "  add $/TestProject7/WindowsApplication1\r\n"
                + "  add $/TestProject7/WindowsApplication1/AssemblyInfo.vb\r\n"
                + "  add $/TestProject7/WindowsApplication1/Form1.resx\r\n"
                + "  add $/TestProject7/WindowsApplication1/Form1.vb\r\n"
                + "  add $/TestProject7/WindowsApplication1/WindowsApplication1.vbproj.user\r\n"
                + "  add $/TestProject7/WindowsApplication1/WindowsApplication1.vbproj\r\n" + "\r\n"
                + "-------------------------------------------------------"
                + "--------------------------------------------\r\n" + "Changeset: 7\r\n" + "User: RNO\\_MCLWEB\r\n"
                + "Date: 20 April 2006 02:20:30\r\n" + "\r\n" + "Comment:\r\n"
                + "  Created team project folder $/TestProject7 via the Team Project Creation Wizard\r\n" + "\r\n"
                + "Items:\r\n" + "  add $/TestProject7\r\n" + "\r\n";

        Reader reader = new StringReader(tfOutput);
        List list = TeamFoundationServer.TFHistoryParser.parse(reader, minDate, null);

        assertNotNull(list);
        assertEquals(11, list.size());

    }

    public void testParseDate() throws ParseException {
        Date actualDate = TeamFoundationServer.TFHistoryParser.parseDate("20 April 2006 02:20:30", null);
        assertEquals(new GregorianCalendar(2006, Calendar.APRIL, 20, 2, 20, 30).getTime(), actualDate);

        // Custom format
        actualDate = TeamFoundationServer.TFHistoryParser.parseDate(" 02:20:30, 2006, 20 April", "hh:mm:ss, yyy, dd MMMMM");
        assertEquals(new GregorianCalendar(2006, Calendar.APRIL, 20, 2, 20, 30).getTime(), actualDate);
    }

    public void testParseDateSweden() throws ParseException {
        final Date now = new Date();

        // Test a non US or GB locale
        Locale.setDefault(new Locale("sv", "SE"));
        TimeZone.setDefault(TimeZone.getTimeZone("CET"));

        final DateFormat form = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG, new Locale("sv", "SE"));
        final DateFormat out = new SimpleDateFormat("yyyy-MM-dd H:m:s");

        final Date parsedDate = TeamFoundationServer.TFHistoryParser.parseDate(form.format(now), null);
        assertEquals(out.format(now), out.format(parsedDate));
    }

    /**
     * Helper method to return the first changeset from a set of passed data.
     * 
     * @throws ParseException
     */
    private Modification parseChangeset(String data) throws ParseException {
        List list = TeamFoundationServer.TFHistoryParser.parseChangeset(data, minDate, null);

        assertNotNull(list);
        assertTrue(list.get(0) instanceof Modification);

        return (Modification) list.get(0);
    }

    /**
     * Helper method for testing array equality.
     */
    private static void assertArraysEquals(Object[] expected, Object[] actual) {
        assertEquals("array lengths mismatch!", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

}
