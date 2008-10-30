package net.sourceforge.cruisecontrol.launch;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.launch.util.Locator;
import net.sourceforge.cruisecontrol.MainTest;

/**
 * @author Dan Rollo
 */
public class LauncherTest extends TestCase {

    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private Properties origSysProps;
    private ClassLoader origClassLoader;

    protected void setUp() throws Exception {
        // shallow copy should be enough, but maybe deep copy would be better?
        origSysProps = (Properties) System.getProperties().clone();

        origClassLoader = Thread.currentThread().getContextClassLoader();
    }

    protected void tearDown() throws Exception {
        // restore classloader
        Thread.currentThread().setContextClassLoader(origClassLoader);

        // restore sys props
        System.setProperties(origSysProps);
    }

    public void testGetCCHomeDir() throws Exception {

        final Launcher launcher = new Launcher();
        final File sourceJar = Locator.getClassSource(launcher.getClass());
        final File distJarDir = sourceJar.getParentFile();

        final File validHomeDir = distJarDir.getParentFile();

        assertEquals("Wrong default CCHomeDir", validHomeDir, launcher.getCCHomeDir(distJarDir));

        // Need to reset SysProp after successful call to getCCHomeDir() because
        // getCCHomeDir() resets the SysProp when a valid default is found.
        System.setProperty(Launcher.CCHOME_PROPERTY, "bogusHomeSysProp");
        // this should work since invalid sys prop is overridden if default is valid.
        assertEquals("Wrong default CCHomeDir w/ bad sysprop", validHomeDir, launcher.getCCHomeDir(distJarDir));

        System.setProperty(Launcher.CCHOME_PROPERTY, "bogusHomeSysProp");
        try {
            launcher.getCCHomeDir(new File("bogus"));
            fail("Wrong default CCHomeDir w/ bad sysprop AND bad distDir should have failed.");
        } catch (LaunchException e) {
            assertEquals(Launcher.MSG_BAD_CCHOME, e.getMessage());
        }
    }

    public void testLauncherNullCCHomeProperty() throws Exception {
        final String[] args = new String[] { "-configfile", "bogusConfigFile" };
        final Launcher launcher = new Launcher();
        System.getProperties().remove(Launcher.CCHOME_PROPERTY);
        // prevent system.exit calls from printUsage
        System.setProperty(Launcher.SYSPROP_CCMAIN_SKIP_USAGE_EXIT, "true");

        // line below fails w/ NPE if sysprop "cc.home" doesn't exist
        launcher.run(args);
    }

    public void testArgLog4jconfig() throws Exception {
        assertNull(System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));

        final Launcher launcher = new Launcher();

        // prevent printUsage msg from printing
        MainTest.setSkipUsage();
        // prevent system.exit calls from printUsage
        System.setProperty(Launcher.SYSPROP_CCMAIN_SKIP_USAGE_EXIT, "true");

        launcher.run(EMPTY_STRING_ARRAY);
        assertNull("log4j sys prop should not be set.", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));

        try {
            launcher.run(new String[]{ "-" + Launcher.ARG_LOG4J_CONFIG });
            fail("missing log4j config filename should have failed.");
        } catch (LaunchException e) {
            assertEquals(Launcher.ERR_MSG_LOG4J_CONFIG, e.getMessage());
        }
        assertNull("log4j sys prop should not be set.", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));

        final String bogusLog4jConfig = "bogusLog4jConfig";
        final String[] args = new String[] { "-" + Launcher.ARG_LOG4J_CONFIG, bogusLog4jConfig };

        try {
            launcher.run(args);
            assertEquals(bogusLog4jConfig, System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));

            // ensure we override existing sys prop
            System.setProperty(Launcher.PROP_LOG4J_CONFIGURATION, "dummy");
            launcher.run(args);
            assertEquals(bogusLog4jConfig, System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));
        } finally {
            System.getProperties().remove(Launcher.PROP_LOG4J_CONFIGURATION);
        }
    }
}
