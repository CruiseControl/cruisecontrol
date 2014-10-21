package net.sourceforge.cruisecontrol.launch;

import java.io.File;

import net.sourceforge.cruisecontrol.testutil.TestUtil.PropertiesRestorer;
import junit.framework.TestCase;

/**
 * @author Dan Rollo
 */
public class LauncherTest extends TestCase {

    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private PropertiesRestorer origSysProps = new PropertiesRestorer();
    private ClassLoader origClassLoader;

    protected void setUp() throws Exception {
        origSysProps.record();
        origClassLoader = Thread.currentThread().getContextClassLoader();
    }

    protected void tearDown() throws Exception {
        // restore classloader and properties
        Thread.currentThread().setContextClassLoader(origClassLoader);
        origSysProps.restore();
    }

    public void testGetCCHomeDir() throws Exception {
        final Launcher launcher = new LauncherMock();
        final File sourceJar = launcher.getClassSource();
        final File distJarDir = sourceJar.getParentFile();

        final File validHomeDir = distJarDir.getParentFile();
        final Configuration config = Configuration.getInstance(new String[] {"-home", "wrong/path"});

        assertEquals("Wrong default CCHomeDir", validHomeDir, launcher.getCCHomeDir(config, distJarDir).getAbsoluteFile());

        // Need to reset SysProp after successful call to getCCHomeDir() because
        // getCCHomeDir() resets the SysProp when a valid default is found.
        System.setProperty("cc."+Launcher.CCHOME_PROPERTY, "bogusHomeSysProp");
        // this should work since invalid sys prop is overridden if default is valid.
        assertEquals("Wrong default CCHomeDir w/ bad sysprop", validHomeDir, launcher.getCCHomeDir(config, distJarDir));

        System.setProperty(Launcher.CCHOME_PROPERTY, "bogusHomeSysProp");
        try {
            launcher.getCCHomeDir(config, new File("bogus"));
            fail("Wrong default CCHomeDir w/ bad sysprop AND bad distDir should have failed.");
        } catch (LaunchException e) {
            assertEquals(Launcher.MSG_BAD_CCHOME, e.getMessage());
        }
    }

// Not necessary now, since launcher.run(args) would fail anyway if the config file is invalid
//
//    public void testLauncherNullCCHomeProperty() throws Exception {
//        final String[] args = new String[] { "-configfile", "bogusConfigFile" };
//        final Launcher launcher = new LauncherMock();
//        System.getProperties().remove(Launcher.CCHOME_PROPERTY);
//        // prevent system.exit calls from printUsage
//        System.setProperty(Launcher.SYSPROP_CCMAIN_SKIP_USAGE_EXIT, "true");
//
//        // line below fails w/ NPE if sysprop "cc.home" doesn't exist
//        launcher.run(args);
//    }

    public void testArgLog4jconfig() throws Exception {
        assertNull(System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));

        final Launcher launcher = new LauncherMock();

        // prevent printUsage msg from printing
//        MainTest.setSkipUsage();
//        // prevent system.exit calls from printUsage
//        System.setProperty(Launcher.SYSPROP_CCMAIN_SKIP_USAGE_EXIT, "true");

        // When not set, default value (in working directory) is used
        try {
            launcher.run(EMPTY_STRING_ARRAY);
            assertNotNull("log4j sys prop is not set.", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));
        } catch (Exception e) {
            // Here the default file was not found ...
            assertNull("log4j sys prop should not be set.", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));
        }

        // The same is when only -option is set
        try {
            // Set without value will use the default path. This will fail anyway, since the file
            // does not exist, and the same would be if no file is configured
            launcher.run(new String[]{ "-" + Configuration.KEY_LOG4J_CONFIG });
            assertNotNull("log4j sys prop is not set.", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));
        } catch (Exception e) {
            // Here the default file was not found ...
            assertNull("log4j sys prop should not be set.", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));
        }

        // Set the non-existing path - through config
        try {
            final String bogusLog4jConfig = "bogusLog4jConfig";
            final String[] args = new String[] { "-" + Configuration.KEY_LOG4J_CONFIG, bogusLog4jConfig };

            launcher.run(args);
            fail("Exception was expected, since " + bogusLog4jConfig + " should not exist");

        } catch (IllegalArgumentException e) {
            assertEquals("Option 'log4jconfig' = 'bogusLog4jConfig' does not represent URL value!",
                         e.getMessage());
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
