package net.sourceforge.cruisecontrol.launch;

import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.testutil.TestUtil.PropertiesRestorer;

/**
 * @author Dan Rollo
 */
public class LauncherTest extends TestCase {

    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private final PropertiesRestorer origSysProps = new PropertiesRestorer();
    private ClassLoader origClassLoader;

    @Override
    protected void setUp() throws Exception {
        origSysProps.record();
        origClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    protected void tearDown() throws Exception {
        // restore classloader and properties
        Thread.currentThread().setContextClassLoader(origClassLoader);
        origSysProps.restore();
    }

    public void testGetCCProjDir() throws Exception {
        final Launcher launcher = new LauncherMock();
        final File validProjDir = new File(".");

        // proj is not set explicitly
        try {
            final Configuration conf = Configuration.getInstance(new String[0]);
            assertEquals(validProjDir.getCanonicalPath(), launcher.getCCProjDir(conf).getCanonicalPath());
        } finally {
            origSysProps.restore();
        }

        // proj is set explicitly and valid
        try {
            final Configuration conf = Configuration.getInstance(new String[] {"-proj", "./"});
            assertEquals(validProjDir.getCanonicalPath(), launcher.getCCProjDir(conf).getCanonicalPath());
        } finally {
            origSysProps.restore();
        }

        // proj is set explicitly but invalid
        try {
            final Configuration conf = Configuration.getInstance(new String[] {"-proj", "wrong/path"});
            launcher.getCCProjDir(conf);
            fail("Wrong explicit CCProjDir should have failed.");
        } catch (LaunchException e) {
            assertEquals(Launcher.MSG_BAD_CCPROJ, e.getMessage());
        }
    }

    public void testGetCCDistDir() throws Exception {
        final Launcher launcher = new LauncherMock();
        final File sourceJar = launcher.getClassSource();
        final File sourceJarDir = sourceJar.getParentFile();
        final File validDistDir = sourceJarDir.getParentFile();

        // Dist is not set explicitly
        final Configuration conf1 = Configuration.getInstance(new String[0]);
        assertEquals(validDistDir.getCanonicalPath(), launcher.getCCDistDir(conf1).getCanonicalPath());

        // Dist is set explicitly and valid
        final Configuration conf2 = Configuration.getInstance(new String[] {"-dist", validDistDir.getPath()});
        assertEquals(validDistDir.getCanonicalPath(), launcher.getCCDistDir(conf2).getCanonicalPath());

        // Dist is set explicitly but invalid
        final Configuration conf3 = Configuration.getInstance(new String[] {"-dist", "wrong/path"});
        try {
            launcher.getCCDistDir(conf3);
            fail("Wrong explicit CCDistDir should have failed.");
        } catch (LaunchException e) {
            assertEquals(Launcher.MSG_BAD_CCDIST, e.getMessage());
        }
    }

    public void testArgLog4jconfig() throws Exception {
        assertNull(System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));

        final Launcher launcher = new LauncherMock();
        // prevent printUsage msg from printing
//        MainTest.setSkipUsage();
//        // prevent system.exit calls from printUsage
//        System.setProperty(Launcher.SYSPROP_CCMAIN_SKIP_USAGE_EXIT, "true");

        // Correct log4j config (at least file exists)
        try {
            final FilesToDelete f = new FilesToDelete();
            final File log4jConfig = f.add("log4j.user.config");

            launcher.run(new String[]{ "-" + Configuration.KEY_LOG4J_CONFIG, "file://" + log4jConfig.getAbsolutePath()});
            assertNotNull("log4j sys prop should be set", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));
        } catch (Exception e) {
            // Here the default file was not found ...
            fail("log4j sys prop error: " + e.getMessage());
        } finally {
            origSysProps.restore();
        }

        // When not set, no property is set
        try {
            launcher.run(EMPTY_STRING_ARRAY);
            assertNull("log4j sys prop should not be set", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));
        } catch (Exception e) {
            // Here the default file was not found ...
            fail("log4j sys prop error: " + e.getMessage());
        } finally {
            origSysProps.restore();
        }

        // The same is when only -option is set
        try {
            // Set without value will use the default path. This will fail anyway, since the file
            // does not exist, and the same would be if no file is configured
            launcher.run(new String[]{ "-" + Configuration.KEY_LOG4J_CONFIG });
            assertNull("log4j sys prop should not be set", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));
        } catch (Exception e) {
            // Here the default file was not found ...
            fail("log4j sys prop error: " + e.getMessage());
        } finally {
            origSysProps.restore();
        }

        // Set the non-URL path - through config
        try {
            final String bogusLog4jConfig = "bogusLog4jConfig";
            final String[] args = new String[] { "-" + Configuration.KEY_LOG4J_CONFIG, bogusLog4jConfig };

            launcher.run(args);
            fail("Exception was expected, since " + bogusLog4jConfig + " should not exist");

        } catch (IllegalArgumentException e) {
            assertEquals("Option 'log4jconfig' = 'bogusLog4jConfig' does not represent URL value!",
                         e.getMessage());
        } finally {
            origSysProps.restore();
        }
    }

    /** Mock object for {@link Launcher} which disables the System.exit() call on failure */
    private static class LauncherMock extends Launcher {

        /** Does nothing to prevent tests cancellation */
        @Override
        protected void exitWithErrorCode() {
            // Nothing here
        }
    }
}
