package net.sourceforge.cruisecontrol.launch;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.launch.util.Locator;

/**
 * @author Dan Rollo
 */
public class LauncherTest extends TestCase {

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
}
