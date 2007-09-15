package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.extensions.TestSetup;

import java.util.Properties;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.net.URL;
import java.net.InetAddress;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.distributed.BuildAgent;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.BuildAgentServiceImplTest;
import net.sourceforge.cruisecontrol.distributed.BuildAgentTest;
import net.sourceforge.cruisecontrol.distributed.core.ReggieUtil;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscoveryTest;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.MockProject;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.discovery.LookupLocator;
import net.jini.config.ConfigurationProvider;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.DiscoveryEvent;

/**
 * @author Dan Rollo
 * Date: May 6, 2005
 * Time: 2:34:24 PM
 */
public class DistributedMasterBuilderTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(DistributedMasterBuilderTest.class);

    private static final String INSECURE_POLICY_FILENAME = "insecure.policy";
    private static Properties origSysProps;

    private static final String CONFIG_START_JINI = "conf/start-jini.config";
    public static final String JINI_URL_LOCALHOST;
    static {
        final Configuration config;
        String hostname;
        try {
            config = ConfigurationProvider.getInstance(new String[] {CONFIG_START_JINI});            
            hostname = (String) config.getEntry("com.sun.jini.start", "hostname", String.class);
        } catch (ConfigurationException e) {
            throw new RuntimeException("Error loading jini config: " + CONFIG_START_JINI, e);
        }
        if (hostname != null) {
            JINI_URL_LOCALHOST = "jini://" + hostname;
        } else {
            JINI_URL_LOCALHOST = "jini://localhost";
        }
        LOG.info("Using local Jini URL: " + JINI_URL_LOCALHOST);
    }

    private static final OSEnvironment OS_ENV = new OSEnvironment();

    private static ProcessInfoPump jiniProcessPump;

    /** Common error message if multicast is being blocked. */
    private static final String MSG_DISOCVERY_CHECK_FIREWALL =
            "1. Make sure MULTICAST is enabled on your network devices (ifconfig -a).\n"
            + "2. No Firewall is blocking multicasts.\n"
            + "3. Using an IDE? See @todo in setUp/tearDown (LUS normally started by LUSTestSetup decorator).\n";

    public static final String MSG_PREFIX_STATS = "STATS: ";

    private static String getTestDMBEntries() {
        final Map userProps
                = PropertiesHelper.loadRequiredProperties(BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE);

        final Object retval = userProps.get(BuildAgentServiceImplTest.ENTRY_NAME_BUILD_TYPE);
        assertNotNull("Missing required entry for DMB unit test: " + BuildAgentServiceImplTest.ENTRY_NAME_BUILD_TYPE,
                retval);
        assertTrue(retval instanceof String);

        return BuildAgentServiceImplTest.ENTRY_NAME_BUILD_TYPE + "=" + retval;
    }

    /**
     * Show what's happening with the Jini Process.
     */
    private static final class PrefixedStreamConsumer implements StreamConsumer {
        private final String prefix;
        private final Logger logger;
        private final Level level;

        PrefixedStreamConsumer(final String prefix, final Logger logger, final Level level) {
            this.prefix = prefix;
            this.logger = logger;
            this.level = level;
        }

        /** {@inheritDoc} */
        public void consumeLine(String line) {
            logger.log(level, prefix + line);
        }
    }



    /**
     * @return the Process in which Jini Lookup _service is running, for use in killing it.
     * @throws Exception if we can't start jini lookup service
     */
    public static ProcessInfoPump startJini() throws Exception {
        final long begin = System.currentTimeMillis();

        final Logger logger = LOG;
        final Level level = Level.INFO;

        // make sure local lookup service is not already running
        verifyNoLocalLookupService();

        origSysProps = System.getProperties();

        // Build Lookup Service command line just like the one in lookup-build.xml

//        <java jar="jini-lib/start.jar" fork="true" >
//            <jvmarg value="-Djava.security.policy=conf/${jini.policy.file}" />
//            <jvmarg value="-Djini.lib=jini-lib" />
//            <jvmarg value="-Djini.lib.dl=jini-lib-dl" />
//            <jvmarg value="-Djini.httpPort=${jini.port}" />
//            <arg value="conf/${jini.config}"/>

        final String jiniLibDir = "jini-lib";

        final String[] args = new String[] {
             "-Djava.security.policy=conf/insecure.policy", //${jini.policy.file}
             "-Djini.lib=" + jiniLibDir,
             "-Djini.lib.dl=jini-lib-dl",                   //Downloadable Jini jars
             "-Djini.httpPort=8050",                        //${jini.port}"
        };

        final Commandline cmdLine = new Commandline();
        cmdLine.addArguments(args);
        Commandline.Argument argClasspath = cmdLine.createArgument();
        argClasspath.setLine("-classpath " + "conf");

        Commandline.Argument argStart = cmdLine.createArgument();
        argStart.setLine("-jar " + jiniLibDir + "/start.jar");

        Commandline.Argument argProg = cmdLine.createArgument();
        argProg.setValue(CONFIG_START_JINI); // ${jini.config}

        cmdLine.setExecutable(getJavaExec());


        LOG.debug("jini startup command: " + Arrays.asList(cmdLine.getCommandline()));
        final Process newJiniProcess = Runtime.getRuntime().exec(cmdLine.getCommandline());

        newJiniProcess.getOutputStream().close();

        final ProcessInfoPump jiniProcessInfoPump = new ProcessInfoPump(newJiniProcess,
            // show what's happening with the jiniProcessPump
            new StreamPumper(newJiniProcess.getErrorStream(),
                    new PrefixedStreamConsumer("[JiniErr] ", logger, level)),
            new StreamPumper(newJiniProcess.getInputStream(),
                    new PrefixedStreamConsumer("[JiniOut] ", logger, level)),
            logger, level);

        Thread.sleep(2000); // allow LUS some spin up time, first time is longest (~3.5 secs)


        // Verify the Lookup Service started

        // setup security policy
        setupInsecurePolicy();

        // The startup time for the first LUS appears to be much longer than subsequent startups... 
        ServiceRegistrar serviceRegistrar = findTestLookupService(50 * 1000);
        try {
            assertNotNull("Failed to start local lookup _service.", serviceRegistrar);
        } finally {
            if (serviceRegistrar == null) {
                // kill service in case it is just really slow to start, to help avoid orphaned LUS processes
                killJini(jiniProcessInfoPump);
            }
        }
        assertEquals("Unexpected local lookup _service host",
            InetAddress.getLocalHost().getCanonicalHostName(),
            serviceRegistrar.getLocator().getHost());

        LOG.info(MSG_PREFIX_STATS + "Jini Startup took: " + (System.currentTimeMillis() - begin) / 1000f + " sec");
        
        return jiniProcessInfoPump;
    }

    /**
     * Setup an insecure policy file for use during unit tests that require Jini.
     */
    public static void setupInsecurePolicy() {
        URL policyFile = ClassLoader.getSystemClassLoader().getResource(INSECURE_POLICY_FILENAME);
        assertNotNull("Can't load policy file resource: " + INSECURE_POLICY_FILENAME
                + ". Make sure this file is in the classes (bin) directory.",
            policyFile);
        System.setProperty(BuildAgent.JAVA_SECURITY_POLICY, policyFile.toExternalForm());
        ReggieUtil.setupRMISecurityManager();
    }


    private static String javaExecutable;

    private static String getJavaExec() {
        if (javaExecutable == null) {
            final String javaExecFilename;
            if (Util.isWindows()) {
                javaExecFilename = "java.exe";
            } else {
                javaExecFilename = "java";
            }
            // use javaHome env var to find java
            if (getJavaHome() != null) {
                javaExecutable = getJavaHome() + File.separator + "bin" + File.separator + javaExecFilename;
            } else {
                final String msg
                        = "Unit Test couldn't find JAVA_HOME env var. Maybe java/bin is in the path? Here goes...";
                System.out.println(msg);
                LOG.warn(msg);
                javaExecutable = javaExecFilename;
            }
        }
        return javaExecutable;
    }


    private static String javaHome;

    private static String getJavaHome() {
        if (javaHome == null) {
            String envJavaHome = OS_ENV.getVariable("JAVA_HOME");
            if (envJavaHome != null) {
                javaHome = envJavaHome;
            } else {
                // try system prop for java.home
                javaHome = System.getProperty("java.home");
            }
        }
        return javaHome;
    }


    public static ServiceRegistrar findTestLookupService(int retryTimeoutMillis)
            throws IOException, ClassNotFoundException, InterruptedException {

        // find/wait for lookup _service
        final long startTime = System.currentTimeMillis();

        ServiceRegistrar serviceRegistrar = null;
        final LookupLocator lookup = new LookupLocator(JINI_URL_LOCALHOST);

        // making this polling loop pause too short actually slows the unit tests down
        final int sleepMillisAfterException = 250;

        while (serviceRegistrar == null
                && (System.currentTimeMillis() - startTime < retryTimeoutMillis)) {

            try {
                serviceRegistrar = lookup.getRegistrar();
            } catch (ConnectException e) {
                Thread.sleep(sleepMillisAfterException);
            } catch (SocketException e) {
                Thread.sleep(sleepMillisAfterException);
            } catch (EOFException e) {
                Thread.sleep(sleepMillisAfterException);
            }
            // more exceptions will likely need to added here as the Jini libraries are updated.
            // could catch a generic super class, but I kinda like to know what's being thrown.
        }

        LOG.info(MSG_PREFIX_STATS + "Find Test LUS took: "
                + (System.currentTimeMillis() - startTime) / 1000f + " sec; Timeout: " + retryTimeoutMillis
                + "; Found: " + (serviceRegistrar != null));

        return serviceRegistrar;
    }

    public static void killJini(final ProcessInfoPump jiniProcessPump) throws Exception {
        if (jiniProcessPump != null) {

            final long begin = System.currentTimeMillis();

            jiniProcessPump.kill();
            LOG.debug("Jini process killed.");

            verifyNoLocalLookupService();

            LOG.info(MSG_PREFIX_STATS + "Jini Shutdown took: " + (System.currentTimeMillis() - begin) / 1000f + " sec");
        }

        // restore original system properties
        System.setProperties(origSysProps);
    }

    private static void verifyNoLocalLookupService() throws IOException, ClassNotFoundException, InterruptedException {

        final String msgLUSFoundCheckForOrphanedProc
                = "Found local lookup service, but it should be dead. Is an orphaned java process still running?";

        final ServiceRegistrar serviceRegistrar;
        try {
            serviceRegistrar = findTestLookupService(1000);
        } catch (ClassNotFoundException e) {
            assertFalse(msgLUSFoundCheckForOrphanedProc,
                    "com.sun.jini.reggie.ConstrainableRegistrarProxy".equals(e.getMessage()));
            throw e;
        }
        assertNull(msgLUSFoundCheckForOrphanedProc, serviceRegistrar);
    }


    /**
     * Holds a executing process and it's associated stream pump threads.
     */
    public static final class ProcessInfoPump {
        private final Process process;
        private final Thread inputPumpThread;
        private final Thread errorPumpThread;

        private final Logger logger;
        private final Level level;

        public ProcessInfoPump(final Process process, final StreamPumper inputPump, final StreamPumper errorPump,
                               final Logger logger, final Level level) {

            this.process = process;

            this.logger = logger;
            this.level = level;

            errorPumpThread = new Thread(errorPump);
            inputPumpThread = new Thread(inputPump);

            errorPumpThread.start();
            inputPumpThread.start();
        }

        public void kill() throws IOException, InterruptedException {
            process.destroy();

            logger.log(level, "Process destroyed.");

            // wait for stream pumps to end
            if (errorPumpThread != null) {
                errorPumpThread.join();
            }
            if (inputPumpThread != null) {
                inputPumpThread.join();
            }

            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();

            logger.log(level, "Process pumps finished.");
        }
    }


    /**
     * Test Decorator to launch Jini LUS once for this class.
     */
    public static final class LUSTestSetup extends TestSetup {

        public LUSTestSetup(Test test) {
            super(test);
        }

        protected void setUp() throws Exception {
            jiniProcessPump = DistributedMasterBuilderTest.startJini();
        }

        protected void tearDown() throws Exception {
            DistributedMasterBuilderTest.killJini(jiniProcessPump);
        }
    }
    /**
     * Use LUSTestSetup decorator to run Jini LUS once for this test class.
     * @return  a TestSuite wrapper by the LUSTestSetup decorator
     */
    public static Test suite() {
        final TestSuite ts = new TestSuite();
        ts.addTestSuite(DistributedMasterBuilderTest.class);
        return new LUSTestSetup(ts);
    }
    // @todo Add one slash in front of "/*" below to run individual tests in an IDE
    /*
    protected void setUp() throws Exception {
        jiniProcessPump = DistributedMasterBuilderTest.startJini();
    }
    protected void tearDown() throws Exception {
        DistributedMasterBuilderTest.killJini(jiniProcessPump);
    }
    //*/


    public void testRemoteProgress() throws Exception {
        // register agent
        final BuildAgent agentAvailable = createBuildAgent();
        try {
            final DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostONLY();

            final MockBuilder mockBuilder = new MockBuilder();
            masterBuilder.add(mockBuilder);
            masterBuilder.validate();
            
            final Map projectProperties = new HashMap();
            projectProperties.put(PropertiesHelper.PROJECT_NAME, "testProjectName");

            final MockProject mockProject = new MockProject();
            final Progress progress = mockProject.getProgress();

            final ProjectConfig projectConfig = new ProjectConfig();
            projectConfig.add(new DefaultLabelIncrementer());
            mockProject.setProjectConfig(projectConfig);

            masterBuilder.build(projectProperties, progress);

            final BuildAgentService agentService = masterBuilder.pickAgent(null, null);
            assertNotNull("Couldn't find released agent.\n" + MSG_DISOCVERY_CHECK_FIREWALL, agentService);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agentService.isBusy());

            assertTrue("Wrong progress value: " + progress.getValue(),
                    progress.getValue().indexOf(" retrieving results from ") == 8);
        } finally {
            // terminate JoinManager in BuildAgent
            BuildAgentTest.terminateTestAgent(agentAvailable);
        }
    }

    public void testPickAgent2Agents() throws Exception {
        // register agent
        final BuildAgent agentAvailable = createBuildAgent();
        final BuildAgent agentAvailable2 = createBuildAgent();
        try {
            assertFalse(agentAvailable.getService().isBusy());
            assertFalse(agentAvailable2.getService().isBusy());

            final DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostONLY();

            // try to find agents
            final BuildAgentService agentFoundFirst = masterBuilder.pickAgent(null, null);
            assertNotNull("Couldn't find first agent.\n" + MSG_DISOCVERY_CHECK_FIREWALL, agentFoundFirst);
            assertTrue(agentFoundFirst.isBusy());

            final BuildAgentService agentFoundSecond = masterBuilder.pickAgent(null, null);
            assertNotNull("Couldn't find second agent", agentFoundSecond);
            assertTrue(agentFoundFirst.isBusy());
            assertTrue(agentFoundSecond.isBusy());

            assertNull("Shouldn't find third agent", masterBuilder.pickAgent(null, null));

            // set Agent to Not busy, then make sure it can be found again.
            // callTestDoBuildSuccess() only needed to clearOuputFiles() will succeed
            assertNotNull(BuildAgentServiceImplTest.callTestDoBuildSuccess(agentAvailable.getService()));
            agentAvailable.getService().clearOutputFiles();

            final BuildAgentService agentRefound = masterBuilder.pickAgent(null, null);
            assertNotNull("Couldn't find released agent", agentRefound);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agentRefound.isBusy());

        } finally {
            // terminate JoinManager in BuildAgent
            BuildAgentTest.terminateTestAgent(agentAvailable);
            BuildAgentTest.terminateTestAgent(agentAvailable2);
        }
    }

    public void testPickAgentAfterReleased() throws Exception {
        // register agent
        final BuildAgent agentAvailable = createBuildAgent();
        try {
            assertFalse(agentAvailable.getService().isBusy());
            agentAvailable.getService().claim(); // mark as busy

            final DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostONLY();

            // try to find agent, shouldn't find any available
            assertNull("Shouldn't find any available agents", masterBuilder.pickAgent(null, null));

            // set Agent to Not busy, then make sure it can be found again.
            // callTestDoBuildSuccess() only needed to clearOuputFiles() will succeed
            assertNotNull(BuildAgentServiceImplTest.callTestDoBuildSuccess(agentAvailable.getService()));
            agentAvailable.getService().clearOutputFiles();
            final BuildAgentService agentRefound = masterBuilder.pickAgent(null, null);
            assertNotNull("Couldn't find released agent.\n" + MSG_DISOCVERY_CHECK_FIREWALL, agentRefound);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agentRefound.isBusy());

        } finally {
            // terminate JoinManager in BuildAgent
            BuildAgentTest.terminateTestAgent(agentAvailable);
        }
    }

    public void testPickAgentAgentNotBusy() throws Exception {
        // register agent
        final BuildAgent agentAvailable = createBuildAgent();
        try {
            assertFalse(agentAvailable.getService().isBusy());

            final DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostONLY();

            final BuildAgentService agent = masterBuilder.pickAgent(null, null);
            assertNotNull("Couldn't find agent.\n" + MSG_DISOCVERY_CHECK_FIREWALL, agent);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agent.isBusy());

            // try to find agent, shouldn't find any available
            assertNull("Shouldn't find any available agents", masterBuilder.pickAgent(null, null));

            // set Agent to Not busy, then make sure it can be found again.

            // only needed so clearOuputFiles() will succeed
            assertNotNull(BuildAgentServiceImplTest.callTestDoBuildSuccess(agent)); 
            agent.clearOutputFiles();
            final BuildAgentService agentRefound = masterBuilder.pickAgent(null, null);
            assertNotNull("Couldn't find released agent", agentRefound);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agentRefound.isBusy());
        } finally {
            // terminate JoinManager in BuildAgent
            BuildAgentTest.terminateTestAgent(agentAvailable);
        }
    }

    public void testPickAgentNoAgents() throws Exception {

        DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostONLY();

        assertNull("Shouldn't find any available agents", masterBuilder.pickAgent(null, null));
    }


    private static int testAgentID = 0;

    public static BuildAgent createBuildAgent() throws InterruptedException {

        testAgentID++;
        final int thisAgentID = testAgentID;

        final long begin = System.currentTimeMillis();

        BuildAgentTest.setSkipMainSystemExit();
        BuildAgentTest.setTerminateFast();

        // listen for agent to discover LUS
        final DiscoveryListener utestListener = new DiscoveryListener() {

            public void discovered(DiscoveryEvent evt) {
                synchronized (this) {
                    LOG.info("Agent discovered. (agentID: " + thisAgentID + ")");
                    this.notifyAll();
                }
            }
            public void discarded(DiscoveryEvent evt) {
                synchronized (this) {
                    LOG.info("Agent discarded. (agentID: " + thisAgentID + ")");
                    this.notifyAll();
                }
            }
        };

        LOG.info("Creating test Agent (agentID: " + thisAgentID + ")");

        final BuildAgent agent = BuildAgentTest.createTestBuildAgent(
                BuildAgentServiceImplTest.TEST_AGENT_PROPERTIES_FILE,
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE, true, utestListener, testAgentID);


        synchronized (utestListener) {
            int count = 0;
            while (!BuildAgentTest.isServiceIDAssigned(agent) && count < 6) {
                utestListener.wait(10 * 1000);
                count++;
            }
        }

        assertTrue("Unit test Agent was not discovered before timeout. elapsed: "
                + (System.currentTimeMillis() - begin) / 1000f + " sec \n"
                + MSG_DISOCVERY_CHECK_FIREWALL,
                BuildAgentTest.isServiceIDAssigned(agent));

        LOG.info(MSG_PREFIX_STATS + "Unit test Agent (agentID: " + thisAgentID + ") discovery took: "
                + (System.currentTimeMillis() - begin) / 1000f + " sec");

        return agent;
    }

    static DistributedMasterBuilder getMasterBuilder_LocalhostONLY()
            throws MalformedURLException, InterruptedException {

        if (!MulticastDiscoveryTest.isDiscoverySet()) {
            final long begin = System.currentTimeMillis();

            final MulticastDiscovery discovery = MulticastDiscoveryTest.getLocalDiscovery();
            MulticastDiscoveryTest.setDiscovery(discovery);
            // wait to discover lookupservice
            final DiscoveryListener discoveryListener = new DiscoveryListener() {

                public void discovered(DiscoveryEvent e) {
                    synchronized (discovery) {
                        discovery.notifyAll();
                    }
                }

                public void discarded(DiscoveryEvent e) {
                    synchronized (discovery) {
                        discovery.notifyAll();
                    }
                }
            };
            MulticastDiscoveryTest.addDiscoveryListener(discoveryListener);
            try {
                synchronized (discovery) {
                    if (!MulticastDiscoveryTest.isDiscovered()) {
                        discovery.wait(60 * 1000);
                    }
                }
            } finally {
                MulticastDiscoveryTest.removeDiscoveryListener(discoveryListener);
            }
            assertTrue("MulticastDiscovery was not discovered before timeout.\n" + MSG_DISOCVERY_CHECK_FIREWALL,
                    MulticastDiscoveryTest.isDiscovered());

            LOG.info(MSG_PREFIX_STATS + "Unit test MulticastDiscovery took: "
                    + (System.currentTimeMillis() - begin) / 1000f + " sec");
        }

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        // need to set Entries to prevent finding non-local LUS and/or non-local Build Agents
        masterBuilder.setEntries(getTestDMBEntries());
        masterBuilder.setFailFast(); // don't block until an available agent is found

        return masterBuilder;
    }

}
