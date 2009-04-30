package net.sourceforge.cruisecontrol.listeners;

import junit.framework.TestCase;

import java.util.Properties;
import java.io.FileOutputStream;
import java.io.File;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

/**
 * Test CMSynergySession.
 */
public class CMSynergySessionMonitorTest extends TestCase {

    private static final String ATTR_FILE_NAME
            = new File(TestUtil.getTargetDir(), "test-cmsynergyattribute").getAbsolutePath();

    private CMSynergySessionMonitor monitor;
    private FilesToDelete filesToDelete;

    @Override
    public void setUp() {
        monitor = new CMSynergySessionMonitor();
        filesToDelete = new FilesToDelete();
    }

    @Override
    public void tearDown() {
        filesToDelete.delete();
    }

    /**
     *
     * Test to make sure that {@link CMSynergySessionMonitor.CMSynergySession#validate()}
     * properly checks for complete information.
     * @throws Exception if the test fails
     */
    public void testValidateCompleteSessionInfo() throws Exception {
        final CMSynergySessionMonitor.CMSynergySession session = monitor.createSession();

        session.setName("testname");
        session.setPassword("testpass");
        session.setRole("build_mgr");
        session.setUser("testuser");
        session.setDatabase("/data/test/db");

        session.validate();
    }

    /**
     * Test to make sure that {@link CMSynergySessionMonitor.CMSynergySession#validate()}
     * propery checks for <em>incomplete</em> information.
     */
    public void testValidateIncompleteSessionInfo() {
        final CMSynergySessionMonitor.CMSynergySession session = monitor.createSession();

        try {
            session.validate();
            fail("CMSynergySession.validate() should have thrown an exception");
        } catch (CruiseControlException e) {
        }
    }

    /**
     * @return a new Properties object containing required CMSynergySession attributes.
     */
    private static Properties createRequiredProperties() {

        /*
         * Set required properties
         */
        final Properties requiredProperties = new Properties();
        requiredProperties.setProperty("host", "testhost");
        requiredProperties.setProperty("password", "testpass");
        requiredProperties.setProperty("role", "build_mgr");
        requiredProperties.setProperty("user", "testuser");
        requiredProperties.setProperty("database", "/data/test/db");

        return requiredProperties;
    }

    /**
     * Check that required properties from createRequiredProperties are still there.
     * "name" property should be set by caller for the validation
     * @param expectedProperties holds expected values
     * @param actualSession holds actual values
     */
    private static void checkRequiredProperties(final Properties expectedProperties,
                                         final CMSynergySessionMonitor.CMSynergySession actualSession
    ) {
        assertEquals("Attribute name didn't match.",
                expectedProperties.getProperty("name"), actualSession.getName());

        assertEquals("Attribute password didn't match.",
                expectedProperties.getProperty("password"), actualSession.getPassword());

        assertEquals("Attribute role didn't match.",
                expectedProperties.getProperty("role"), actualSession.getRole());

        assertEquals("Attribute user didn't match.",
                expectedProperties.getProperty("user"), actualSession.getUser());

        assertEquals("Attribute password didn't match.",
                expectedProperties.getProperty("database"), actualSession.getDatabase());

        assertEquals("Attribute name didn't match.",
                expectedProperties.getProperty("name"), actualSession.getName());
     }

    /**
     * Test {@link CMSynergySessionMonitor.CMSynergySession#setAttributeFile(String)}) with
     * remoteclient false.
     * @throws Exception if the test fails
     */
    public void testSetAttributeFileRemoteClientFalse() throws Exception {
        final Properties properties = createRequiredProperties();

        final File file = new File(ATTR_FILE_NAME);
        filesToDelete.add(file);

        final FileOutputStream stream = new FileOutputStream(file);
        try {
            properties.setProperty("remoteclient", "false");
            properties.store(stream, null);
        } finally {
            stream.close();
        }

        CMSynergySessionMonitor.CMSynergySession session = monitor.createSession();
        session.setName("testname");
        properties.setProperty("name", "testname");
        session.setAttributeFile(ATTR_FILE_NAME);
        checkRequiredProperties(properties, session);
        session.validate();
        assertFalse("Remoteclient reported true", session.isRemoteClient());
    }

    /**
     * Test {@link CMSynergySessionMonitor.CMSynergySession#setAttributeFile(String)}) with
     * remoteclient true.
     * @throws Exception if the test fails
     */
    public void testSetAttributeFileRemoteClientTrue() throws Exception {
        final Properties properties = createRequiredProperties();

        final File file = new File(ATTR_FILE_NAME);
        filesToDelete.add(file);

        final FileOutputStream stream = new FileOutputStream(file);
        try {
            properties.setProperty("remoteclient", "true");
            properties.store(stream, null);
        } finally {
            stream.close();
        }

        CMSynergySessionMonitor.CMSynergySession session = monitor.createSession();
        session.setName("testname");
        properties.setProperty("name", "testname");
        session.setAttributeFile(ATTR_FILE_NAME);
        checkRequiredProperties(properties, session);
        session.validate();
        assertTrue("Remoteclient reported true", session.isRemoteClient());
    }

    /**
     * Test that set/get functions are working properly.
     */
    public void testGetSetFunctions() {
        CMSynergySessionMonitor.CMSynergySession session = monitor.createSession();

        final String testExpected = "test";

        session.setName(testExpected);
        assertEquals("Either setName or getName did something wrong.", testExpected, session.getName());

        session.setHost(testExpected);
        assertEquals("Either setHost or getHost did something wrong.", testExpected, session.getHost());

        session.setDatabase(testExpected);
        assertEquals("Either setDatabase or getDatabase did something wrong.", testExpected, session.getDatabase());

        session.setRole(testExpected);
        assertEquals("Either setRole or getRole did something wrong.", testExpected, session.getRole());

        session.setUser(testExpected);
        assertEquals("Either setUser or getUser did something wrong.", testExpected, session.getUser());

        session.setPassword(testExpected);
        assertEquals("Either setPassword or getPassword did something wrong.", testExpected, session.getPassword());

        session.setRemoteClient("true");
        assertTrue("Either setRemoteClient or isRemoteClient did something wrong when testing for "
                + " remoteclient=\"true\". isRemoteClient() returned " + Boolean.toString(session.isRemoteClient()),
                session.isRemoteClient());

        session.setRemoteClient("false");
        assertFalse("Either setRemoteClient or isRemoteClient did something wrong when testing for "
                + " remoteclient=\"false\". isRemoteClient() returned " + Boolean.toString(session.isRemoteClient()),
                session.isRemoteClient());
    }
}
