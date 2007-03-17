package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;

import java.util.Properties;
import java.util.Arrays;
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
import net.sourceforge.cruisecontrol.distributed.SearchablePropertyEntries;
import net.sourceforge.cruisecontrol.distributed.core.ReggieUtil;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.discovery.LookupDiscovery;

/**
 * @author: Dan Rollo
 * Date: May 6, 2005
 * Time: 2:34:24 PM
 */
public class DistributedMasterBuilderTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(DistributedMasterBuilderTest.class);

    public static final String INSECURE_POLICY_FILENAME = "insecure.policy";
    private static Properties origSysProps;
    public static final String JINI_URL_LOCALHOST = "jini://localhost";

    public static final OSEnvironment OS_ENV = new OSEnvironment();

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
     * @param logger the the logger to write Jini process messages to
     * @param level the logger level at which to write Jini process messages
     * @return the Process in which Jini Lookup _service is running, for use in killing it.
     * @throws Exception if we can't start jini lookup service
     */
    public static ProcessInfoPump startJini(final Logger logger, final Level level) throws Exception {

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
        argProg.setValue("conf/start-jini.config"); // ${jini.config}

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


        // Verify the Lookup Service started

        // setup security policy
        setupInsecurePolicy();

        ServiceRegistrar serviceRegistrar = findTestLookupService(20);
        assertNotNull("Failed to start local lookup _service.", serviceRegistrar);
        assertEquals("Unexpected local lookup _service host",
            InetAddress.getLocalHost().getCanonicalHostName(),
            serviceRegistrar.getLocator().getHost());

        Thread.sleep(1000); // kludged attempt to avoid occaisional test failures
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


    public static ServiceRegistrar findTestLookupService(int retryTimeoutSecs)
            throws IOException, ClassNotFoundException, InterruptedException {

        // find/wait for lookup _service
        final long startTime = System.currentTimeMillis();
        ServiceRegistrar serviceRegistrar = null;
        final LookupLocator lookup = new LookupLocator(JINI_URL_LOCALHOST);

        final int sleepMillisAfterException = 500;

        while (serviceRegistrar == null
                && (System.currentTimeMillis() - startTime < (retryTimeoutSecs * 1000))) {

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
        return serviceRegistrar;
    }

    public static void killJini(final ProcessInfoPump jiniProcessPump) throws Exception {
        if (jiniProcessPump != null) {

            jiniProcessPump.kill();
            LOG.debug("Jini process killed.");

            // make sure local Lookup Service is dead
            // @todo why do we need to retry this on Linux?
            if (findTestLookupService(1) != null) {
                final int secs = 5;
                LOG.warn("********* Waiting " + secs + " seconds for Lookup Service to die...need to fix this.");
                Thread.sleep(secs * 1000);
            }

            verifyNoLocalLookupService();

            Thread.sleep(1000); // kludged attempt to avoid occaisional test failures
        }

        // restore original system properties
        System.setProperties(origSysProps);
    }

    private static void verifyNoLocalLookupService() throws IOException, ClassNotFoundException, InterruptedException {
        ServiceRegistrar serviceRegistrar = findTestLookupService(1);
        assertNull("Found local lookup service, but it should be dead. Is an orphaned java process still running?",
                serviceRegistrar);
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

    private ProcessInfoPump jiniProcessPump;


    protected void setUp() throws Exception {
        jiniProcessPump = DistributedMasterBuilderTest.startJini(LOG, Level.INFO);
    }

    protected void tearDown() throws Exception {
        DistributedMasterBuilderTest.killJini(jiniProcessPump);
    }


    public void testPickAgent2Agents() throws Exception {
        // register agent
        final BuildAgent agentAvailable = createBuildAgent();
        final BuildAgent agentAvailable2 = createBuildAgent();
        try {
            assertFalse(agentAvailable.getService().isBusy());
            assertFalse(agentAvailable2.getService().isBusy());

            final DistributedMasterBuilder masterBuilder = createMasterBuilder();

            // try to find agents
            final BuildAgentService agentFoundFirst = masterBuilder.pickAgent();
            assertNotNull("Couldn't find first agent", agentFoundFirst);
            assertTrue(agentFoundFirst.isBusy());

            final BuildAgentService agentFoundSecond = masterBuilder.pickAgent();
            assertNotNull("Couldn't find second agent", agentFoundSecond);
            assertTrue(agentFoundFirst.isBusy());
            assertTrue(agentFoundSecond.isBusy());

            assertNull("Shouldn't find third agent", masterBuilder.pickAgent());

            // set Agent to Not busy, then make sure it can be found again.
            // callTestDoBuild() only needed to clearOuputFiles() will succeed
            BuildAgentServiceImplTest.callTestDoBuild(false, agentAvailable.getService());
            agentAvailable.getService().clearOutputFiles();
            final BuildAgentService agentRefound = masterBuilder.pickAgent();
            assertNotNull("Couldn't find released agent", agentRefound);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agentRefound.isBusy());

        } finally {
            // terminate JoinManager in BuildAgent
            agentAvailable.terminate();
            agentAvailable2.terminate();
        }
    }

    public void testPickAgentAfterReleased() throws Exception {
        // register agent
        final BuildAgent agentAvailable = createBuildAgent();
        try {
            assertFalse(agentAvailable.getService().isBusy());
            agentAvailable.getService().claim(); // mark as busy

            final DistributedMasterBuilder masterBuilder = createMasterBuilder();

            // try to find agent, shouldn't find any available
            assertNull("Shouldn't find any available agents", masterBuilder.pickAgent());

            // set Agent to Not busy, then make sure it can be found again.
            // callTestDoBuild() only needed to clearOuputFiles() will succeed
            BuildAgentServiceImplTest.callTestDoBuild(false, agentAvailable.getService());
            agentAvailable.getService().clearOutputFiles();
            final BuildAgentService agentRefound = masterBuilder.pickAgent();
            assertNotNull("Couldn't find released agent", agentRefound);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agentRefound.isBusy());

        } finally {
            // terminate JoinManager in BuildAgent
            agentAvailable.terminate();
        }
    }

    public void testPickAgentAgentNotBusy() throws Exception {
        // register agent
        final BuildAgent agentAvailable = createBuildAgent();
        try {
            assertFalse(agentAvailable.getService().isBusy());

            final DistributedMasterBuilder masterBuilder = createMasterBuilder();

            final BuildAgentService agent = masterBuilder.pickAgent();
            assertNotNull("Couldn't find agent", agent);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agent.isBusy());

            // try to find agent, shouldn't find any available
            assertNull("Shouldn't find any available agents", masterBuilder.pickAgent());

            // set Agent to Not busy, then make sure it can be found again.
            BuildAgentServiceImplTest.callTestDoBuild(false, agent); // only needed so clearOuputFiles() will succeed
            agent.clearOutputFiles();
            final BuildAgentService agentRefound = masterBuilder.pickAgent();
            assertNotNull("Couldn't find released agent", agentRefound);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agentRefound.isBusy());
        } finally {
            // terminate JoinManager in BuildAgent
            agentAvailable.terminate();
        }
    }

    public void testPickAgentNoAgents() throws Exception {

        DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostAndTestPropsONLY(
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE
        );

        assertNull(masterBuilder.pickAgent());
    }

    public void testPickAgentNoRegistrars() throws Exception {
        // kill local reggie
        DistributedMasterBuilderTest.killJini(jiniProcessPump);

        DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostAndTestPropsONLY(
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE
        );

        assertNull(masterBuilder.pickAgent());
    }



    private static DistributedMasterBuilder createMasterBuilder() throws MalformedURLException, InterruptedException {
        DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostAndTestPropsONLY(
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE
        );

        // wait for cache to discover agent
        int i = 0;
        int waitSecs = 15;
        while (!masterBuilder.getDiscovery().isDiscovered() && i < waitSecs) {
            Thread.sleep(1000);
            i++;
        }
        assertTrue("MasterBuilder was not discovered before timeout.\n"
                + "1. Make sure MULTICAST is enabled on your network devices (ifconfig -a).\n"
                + "2. No Firewall is blocking multicasts.\n",
                masterBuilder.getDiscovery().isDiscovered());
        return masterBuilder;
    }

    private static BuildAgent createBuildAgent() {
        return new BuildAgent(
                BuildAgentServiceImplTest.TEST_AGENT_PROPERTIES_FILE,
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE, true);
    }

    private static DistributedMasterBuilder getMasterBuilder_LocalhostAndTestPropsONLY(
            final String testUserPropsFilename)
            throws MalformedURLException {

        DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();

        final LookupLocator[] unicastLocators = new LookupLocator[] {
                new LookupLocator(DistributedMasterBuilderTest.JINI_URL_LOCALHOST)
        };

        final Entry[] entries = SearchablePropertyEntries.getPropertiesAsEntryArray(
                new SearchablePropertyEntries(testUserPropsFilename).getProperties()
        );

        final MulticastDiscovery discovery = new MulticastDiscovery(
                LookupDiscovery.ALL_GROUPS, unicastLocators, BuildAgentService.class, entries
        );
        masterBuilder.setDiscovery(discovery);
        masterBuilder.setFailFast(true); // don't block until an available agent is found
        return masterBuilder;
    }

}
