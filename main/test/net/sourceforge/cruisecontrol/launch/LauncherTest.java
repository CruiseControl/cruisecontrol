package net.sourceforge.cruisecontrol.launch;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlSettings;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.testutil.TestUtil.PropertiesRestorer;

/**
 * @author Dan Rollo
 */
public class LauncherTest extends TestCase {

    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private final FilesToDelete files2delete = new FilesToDelete();
    private final PropertiesRestorer origSysProps = new PropertiesRestorer();
    private final LogInterface log = new StdErrLogger();
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
        files2delete.delete();
    }

    public void testGetCCProjDir() throws Exception {
        final Launcher launcher = new LauncherMock();
        final File validProjDir = new File(".");

        // proj is not set explicitly
        try {
            final LaunchConfiguration conf = new LaunchConfiguration(new String[0], log, launcher);
            assertEquals(validProjDir.getCanonicalPath(), launcher.getCCProjDir(conf, log).getCanonicalPath());
            assertEquals(validProjDir.getCanonicalPath(), conf.getOptionDir(conf.KEY_PROJ_DIR).getCanonicalPath());
        } finally {
            origSysProps.restore();
        }

        // proj is set explicitly and valid
        try {
            final LaunchConfiguration conf = new LaunchConfiguration(new String[] {"-proj", "./"}, log, launcher);
            assertEquals(validProjDir.getCanonicalPath(), launcher.getCCProjDir(conf, log).getCanonicalPath());
            assertEquals(validProjDir.getCanonicalPath(), conf.getOptionDir(conf.KEY_PROJ_DIR).getCanonicalPath());
        } finally {
            origSysProps.restore();
        }

        // proj is set explicitly but invalid
        try {
            final LaunchConfiguration conf = new LaunchConfiguration(new String[] {"-proj", "wrong/path"}, log, launcher);
            launcher.getCCProjDir(conf, log);
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
        final LaunchConfiguration conf1 = new LaunchConfiguration(new String[0], log, launcher);
        assertEquals(validDistDir.getCanonicalPath(), launcher.getCCDistDir(conf1, log).getCanonicalPath());
        assertEquals(validDistDir.getCanonicalPath(), conf1.getOptionDir(conf1.KEY_DIST_DIR).getCanonicalPath());

        // Dist is set explicitly and valid
        final LaunchConfiguration conf2 = new LaunchConfiguration(new String[] {"-dist", validDistDir.getPath()}, log, launcher);
        assertEquals(validDistDir.getCanonicalPath(), launcher.getCCDistDir(conf2, log).getCanonicalPath());
        assertEquals(validDistDir.getCanonicalPath(), conf2.getOptionDir(conf2.KEY_DIST_DIR).getCanonicalPath());

        // Dist is set explicitly but invalid
        final LaunchConfiguration conf3 = new LaunchConfiguration(new String[] {"-dist", "wrong/path"}, log, launcher);
        try {
            launcher.getCCDistDir(conf3, log);
            fail("Wrong explicit CCDistDir should have failed.");
        } catch (LaunchException e) {
            assertEquals(Launcher.MSG_BAD_CCDIST, e.getMessage());
        }
    }

    public void testArgLog4jconfig() throws Exception {
        assertNull(System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));

        final LauncherMock launcher = new LauncherMock();
        // prevent printUsage msg from printing
//        MainTest.setSkipUsage();
//        // prevent system.exit calls from printUsage
//        System.setProperty(Launcher.SYSPROP_CCMAIN_SKIP_USAGE_EXIT, "true");

        // Correct log4j config (at least file exists)
        try {
            final FilesToDelete f = new FilesToDelete();
            final File log4jConfig = f.add("log4j.user.config");

            launcher.run(new String[]{ "-" + LaunchConfiguration.KEY_LOG4J_CONFIG, "file://" + log4jConfig.getAbsolutePath()});
            assertNotNull("log4j sys prop should be set", System.getProperty(Launcher.PROP_LOG4J_CONFIGURATION));
        } catch (Exception e) {
            // Here the default file was not found ...
            fail("log4j sys prop error: " + e.getMessage());
        } finally {
            launcher.clearConfig();
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
            launcher.clearConfig();
            origSysProps.restore();
        }

        // Set the non-URL path - through config
        try {
            final String bogusLog4jConfig = "bogusLog4jConfig";
            final String[] args = new String[] { "-" + LaunchConfiguration.KEY_LOG4J_CONFIG, bogusLog4jConfig };

            launcher.run(args);
            fail("Exception was expected, since " + bogusLog4jConfig + " should not exist");

        } catch (IllegalArgumentException e) {
            assertEquals("Option 'log4jconfig' = 'bogusLog4jConfig' does not represent URL value!",
                         e.getMessage());
        } finally {
            launcher.clearConfig();
            origSysProps.restore();
        }
    }

    /**
     * Tests passing the config options to the CC main process
     */
    public void testPassConfigToMain() throws Exception {
        final File launchConf = files2delete.add("launch", ".conf");

        final List<Map.Entry<String,String>> opts = new ArrayList<Map.Entry<String,String>>();
        opts.add(new AbstractMap.SimpleEntry<String, String>("dist", "."));
        opts.add(new AbstractMap.SimpleEntry<String, String>("proj", "ccHome"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "lib"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "main/lib"));
        // Make XML cruisecontrol config (containing the <launch> section)
        LaunchConfigurationTest.storeXML(LaunchConfigurationTest.makeConfigXML(LaunchConfigurationTest.makeLauchXML(opts)), launchConf);


        final CruiseControlSettings conf;
        final LauncherMock launcher = new LauncherMock();

        try {
            launcher.run(new String[] {"-" + LaunchConfiguration.KEY_CONFIG_FILE, launchConf.getAbsolutePath(),
                                       "-" + CruiseControlSettings.KEY_USER, "DummyUser",  //Some CC-specific options
                                       "-" + CruiseControlSettings.KEY_CC_NAME,  "CC_Tester"});
            // Launch must be OK
            assertEquals(false, launcher.exitedWithErrorCode());

            // Check he configuration
            conf = CruiseControlSettings.getInstance();
            // And check the arguments
            assertEquals((new File("./ccHome")).getCanonicalPath(), conf.getOptionDir(LaunchConfiguration.KEY_PROJ_DIR).getCanonicalPath());
            assertEquals((new File(".")).getCanonicalPath(), conf.getOptionDir(LaunchConfiguration.KEY_DIST_DIR).getCanonicalPath());

            assertEquals("DummyUser", conf.getOptionStr(CruiseControlSettings.KEY_USER));
            assertEquals("CC_Tester", conf.getOptionStr(CruiseControlSettings.KEY_CC_NAME));

        } finally {
            launcher.clearConfig();
        }
    }

    /** Mock object for {@link Launcher} which disables the System.exit() call on failure */
    private static class LauncherMock extends Launcher {
        /** Releases the {@link CruiseControlSettings} config it it has been instantiated in {@link #main(String[])}
         *  <b>do not forget to call the method when {@link #run(String[])} or {@link #main(String[])} was called in a test!</b> */
        void clearConfig() {
            CruiseControlSettings.delInstance(this);
        }
        /** Override of {@link Launcher#newConfOwner()} - it returns this instance */
        @Override
        protected Object newConfOwner() {
            return this;
        }
        /** @return gets <code>true</code> when {@link #exitWithErrorCode()} was called.  */
        boolean exitedWithErrorCode() {
            return error;
        }
        /** Does nothing to prevent tests cancellation */
        @Override
        protected void exitWithErrorCode() {
            error = true;
        }

        // Exit code
        private boolean error = false;
    }
}
