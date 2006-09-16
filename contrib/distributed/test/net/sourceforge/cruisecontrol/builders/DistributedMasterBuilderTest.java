package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;

import java.util.Properties;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.EOFException;
import java.net.URL;
import java.net.InetAddress;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.MalformedURLException;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.Project;
import org.apache.log4j.Logger;
import org.jdom.Element;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.distributed.BuildAgent;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.BuildAgentServiceImplTest;
import net.sourceforge.cruisecontrol.distributed.SearchablePropertyEntries;
import net.sourceforge.cruisecontrol.distributed.util.ReggieUtil;
import net.sourceforge.cruisecontrol.distributed.util.MulticastDiscovery;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Builder;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.discovery.LookupDiscovery;

/**
 * Created by IntelliJ IDEA. User: drollo Date: May 6, 2005 Time: 2:34:24 PM To change this template
 * use File | Settings | File Templates.
 */
public class DistributedMasterBuilderTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(DistributedMasterBuilderTest.class);

    public static final String INSECURE_POLICY_FILENAME = "insecure.policy";
    private static Properties origSysProps;
    public static final String JINI_URL_LOCALHOST = "jini://localhost";

    public static final OSEnvironment OS_ENV = new OSEnvironment();

    /** Common element names. */
    public static final String ELM_NAME_CC = "cruisecontrol";
    public static final String ELM_NAME_PROJECT = "project";
    public static final String ELM_NAME_SCHEDULE = "schedule";
    public static final String ELM_NAME_DIST = "distributed";
    public static final String ELM_NAME_AND = "ant";
    /** Common attribute names. */
    public static final String ATR_NAME_NAME = "name";
    public static final String ATR_NAME_MODULE = "module";
    public static final String ATR_NAME_DAY = "day";
    public static final String ATR_NAME_TIME = "time";
    public static final String ATR_NAME_MULTIPLE = "multiple";

    /** Expose package visible helper method for other Unit Test classes to use. */
    public static void addMissingPluginDefaults(final Element elementToFilter) {
        DistributedMasterBuilder.addMissingPluginDefaults(elementToFilter);
    }
    /**
     * Show what's happening with the jiniProcess
     */
    private static final class PrefixedPrintWriter extends PrintWriter {
        private final String writerPrefix;

        PrefixedPrintWriter(final String prefix) {
            super(System.out);
            writerPrefix = prefix;
        }

        public void write(String s) {
            //if (LOG.isEnabledFor(Priority.DEBUG)) { // gives odd newlines in console
            super.write(writerPrefix + s);
            //}
        }
    }


    /** @return the Process in which Jini Lookup _service is running, for use in killing it. */
    public static Process startJini() throws Exception {
        // make sure local lookup service is not already running
        verifyNoLocalLookupService();

        origSysProps = System.getProperties();

//        <java jar="lib/start.jar" fork="true" >
//            <jvmarg value="-Djava.security.policy=conf/${jini.policy.file}" />
//            <jvmarg value="-Djini.lib=lib" />
//            <jvmarg value="-Djini.httpPort=${jini.port}" />
//            <jvmarg value="-Djini.codebaseURI=file://lib/cc-agent-dl.jar
//                 file://lib/reggie-dl.jar
//                 file://lib/fiddler-dl.jar
//                 file://lib/mahalo-dl.jar
//                 file://lib/mercury-dl.jar
//                 file://lib/norm-dl.jar
//                 file://lib/outrigger-dl.jar
//                 file://lib/phoenix-dl.jar
//                 file://lib/holowaa-dl.jar" />
//            <jvmarg value="-Djini.classpath=.;lib/cc-agent.jar;lib/reggie.jar;lib/fiddler.jar;lib/mahalo.jar;
//                          lib/mercury.jar;lib/norm.jar;lib/outrigger.jar;lib/phoenix.jar;lib/holowaa.jar" />
//            <arg value="conf/${jini.config}"/>
//            <classpath>
//                <pathelement path="conf"/>
//                <fileset dir="lib">
//                    <include name="**/*.jar" />
//                </fileset>
//            </classpath>
//        </java>

        final String[] args = new String[] {
             "-Djava.security.policy=conf/insecure.policy", //${jini.policy.file}
             "-Djini.lib=lib",
             "-Djini.httpPort=8050",    //${jini.port}"
             "-Djini.codebaseURI=file://lib/cc-agent-dl.jar "
                     + "file://lib/reggie-dl.jar "
                     + "file://lib/fiddler-dl.jar "
                     + "file://lib/mahalo-dl.jar "
                     + "file://lib/mercury-dl.jar "
                     + "file://lib/norm-dl.jar "
                     + "file://lib/outrigger-dl.jar "
                     + "file://lib/phoenix-dl.jar "
                     + "file://lib/holowaa-dl.jar",
             "-Djini.classpath=.;lib/cc-agent.jar;lib/reggie.jar;lib/fiddler.jar;lib/mahalo.jar;"
                     + "lib/mercury.jar;lib/norm.jar;lib/outrigger.jar;lib/phoenix.jar;lib/holowaa.jar"
        };

        // @todo There must be a nicer way to do this...
        // @todo Assumes current dir is same as build.xml.
        final String libDir = "lib/";
        FileSet set = new FileSet();
        set.setDir(new File(libDir));
        set.setIncludes("**/*.jar");
        Project project = new Project();
        set.setProject(project);
        String libjars = set.toString();
        libjars = libjars.replaceAll("jar" + File.pathSeparator, "jar" + File.pathSeparator + libDir);
        libjars = libDir + libjars;

        final Commandline cmdLine = new Commandline();
        cmdLine.addArguments(args);
        Commandline.Argument argClasspath = cmdLine.createArgument();
        argClasspath.setLine("-classpath " + "conf" + File.pathSeparator + libjars);

        Commandline.Argument argStart = cmdLine.createArgument();
        argStart.setLine("-jar lib/start.jar");

        Commandline.Argument argProg = cmdLine.createArgument();
        argProg.setValue("conf/start-jini.config"); // ${jini.config}

        cmdLine.setExecutable(getJavaExec());

        LOG.debug("jini startup command: " + Arrays.asList(cmdLine.getCommandline()));
        final Process newJiniProcess = Runtime.getRuntime().exec(cmdLine.getCommandline());

        // show what's happening with the jiniProcess
        new Thread(new StreamPumper(newJiniProcess.getErrorStream(),
                new PrefixedPrintWriter("[JiniErr] "))).start();
        new Thread(new StreamPumper(newJiniProcess.getInputStream(),
                new PrefixedPrintWriter("[JiniOut] "))).start();

        // setup security policy
        URL policyFile = ClassLoader.getSystemClassLoader().getResource(INSECURE_POLICY_FILENAME);
        assertNotNull("Can't load policy file resource: " + INSECURE_POLICY_FILENAME
                + ". Make sure this file is in the classes (bin) directory.",
            policyFile);
        System.setProperty(BuildAgent.JAVA_SECURITY_POLICY, policyFile.toExternalForm());
        ReggieUtil.setupRMISecurityManager();

        ServiceRegistrar serviceRegistrar = findTestLookupService(20);
        assertNotNull("Failed to start local lookup _service.", serviceRegistrar);
        assertEquals("Unexpected local lookup _service host",
            InetAddress.getLocalHost().getCanonicalHostName(),
            serviceRegistrar.getLocator().getHost());

        Thread.sleep(1000); // kludged attempt to avoid occaisional test failures
        return newJiniProcess;
    }

    public static String getJavaExec() {
        final String javaExecFilename;
        if (Util.isWindows()) {
            javaExecFilename = "java.exe";
        } else {
            javaExecFilename = "java";
        }
        // use JAVA_HOME env var to find java
        final String javaHome = getJAVA_HOME();
        final String javaExec;
        if (javaHome != null) {
            javaExec = javaHome + File.separator + "bin" + File.separator + javaExecFilename;
        } else {
            String msg = "Unit Test couldn't find JAVA_HOME env var. Maybe java/bin is in the path? Here goes...";
            System.out.println(msg);
            LOG.warn(msg);
            javaExec = javaExecFilename;
        }
        return javaExec;
    }

    public static String getJAVA_HOME() {
        return OS_ENV.getVariable("JAVA_HOME");
    }

    public static ServiceRegistrar findTestLookupService(int retryTimeoutSecs)
            throws IOException, ClassNotFoundException, InterruptedException {

        // find/wait for lookup _service
        final long startTime = System.currentTimeMillis();
        ServiceRegistrar serviceRegistrar = null;
        final LookupLocator lookup = new LookupLocator(JINI_URL_LOCALHOST);
        while (serviceRegistrar == null
                && (System.currentTimeMillis() - startTime < (retryTimeoutSecs * 1000))) {

            try {
                serviceRegistrar = lookup.getRegistrar();
            } catch (ConnectException e) {
                Thread.sleep(500);
            } catch (SocketException e) {
                Thread.sleep(500);
            } catch (EOFException e) {
                Thread.sleep(500);
            }
            // more exceptions will likely need to added here as the Jini libraries are updated.
            // could catch a generic super class, but I kinda like to know what's being thrown.
        }
        return serviceRegistrar;
    }

    public static void killJini(final Process jiniProcess) throws Exception {
        if (jiniProcess != null) {
            jiniProcess.destroy();

            jiniProcess.getInputStream().close();
            jiniProcess.getOutputStream().close();
            jiniProcess.getErrorStream().close();

            LOG.debug("Jini process killed.");

            // make sure local Lookup Service is dead
            // @todo why do we need to retry this on Linux?
            if (findTestLookupService(1) != null) {
                final int secs = 5;
                LOG.debug("Waiting " + secs + " seconds for Lookup Service to die...need to fix this.");
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


    private Process jiniProcess;



    private static Element getDistElement(final String projectName, final int projectIndex)
            throws CruiseControlException {

        final Element rootElement = Util.loadRootElement(BuildAgentServiceImplTest.TEST_CONFIG_FILE);

        final List projects = rootElement.getChildren(ELM_NAME_PROJECT);
        final Element project = (Element) projects.get(projectIndex);
        assertEquals(ELM_NAME_PROJECT, project.getName());
        assertEquals(projectName, project.getAttributeValue(ATR_NAME_NAME));

        final Element schedule = (Element) project.getChildren(ELM_NAME_SCHEDULE).get(0);
        assertEquals(ELM_NAME_SCHEDULE, schedule.getName());

        final Element dist = (Element) schedule.getChildren().get(0);
        assertEquals(ELM_NAME_DIST, dist.getName());
        return dist;
    }

    private static Element getAntElement(final Element dist) {
        final Element ant = (Element) dist.getChildren().get(0);
        assertEquals(ELM_NAME_AND, ant.getName());
        return ant;
    }



    protected void setUp() throws Exception {
        jiniProcess = DistributedMasterBuilderTest.startJini();
    }

    protected void tearDown() throws Exception {
        DistributedMasterBuilderTest.killJini(jiniProcess);
    }



    public void testDistAttribs() throws Exception {

        final Element dist = getDistElement("testproject2", 1);
        assertEquals("testmodule-attribs", dist.getAttributeValue(ATR_NAME_MODULE));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);
        assertEquals("agent/log", masterBuilder.getAgentLogDir());
        assertEquals("master/log", masterBuilder.getMasterLogDir());
        // check PreconfiguredPlugin attib on distributed tag
        final String preConfMsg = "Are PreConfgured Plugin settings still broken for distributed builds?"
                + "\nSee " + BuildAgentServiceImplTest.TEST_CONFIG_FILE + " for more info.";
        assertEquals(preConfMsg, "build.type=test", dist.getAttributeValue("entries"));

        // check attribs on nested builder
        final Element childBuilder = masterBuilder.getChildBuilderElement();
        assertEquals("testtargetSuccess", childBuilder.getAttributeValue("target"));
        // check PreconfiguredPlugin attribs on nested builder
        assertEquals(preConfMsg, "${env.ANT_HOME}", childBuilder.getAttributeValue("anthome"));
        assertEquals(preConfMsg, "test/testdist.build.xml", childBuilder.getAttributeValue("buildfile"));
        assertEquals(preConfMsg, "true", childBuilder.getAttributeValue("uselogger"));
    }

    public void testScheduleDay() throws Exception {

        final Element dist = getDistElement("testprojectNoModule", 2);
        final Element ant = getAntElement(dist);
        assertEquals("This unit test requires there be a '" + ATR_NAME_DAY + "' attribute",
                "7", ant.getAttributeValue(ATR_NAME_DAY));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);

        assertEquals("Distributed builder should wrap child-builder schedule fields",
                7, masterBuilder.getDay());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getTime());
        // @todo Is this logic correct, or should value be NOT_SET?
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                1, masterBuilder.getMultiple());
    }

    public void testScheduleTime() throws Exception {

        final Element dist = getDistElement("testprojectTime", 3);
        final Element ant = getAntElement(dist);
        assertEquals("This unit test requires there be a '" + ATR_NAME_TIME + "' attribute",
                "0530", ant.getAttributeValue(ATR_NAME_TIME));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);

        assertEquals("Distributed builder should wrap child-builder schedule fields",
                530, masterBuilder.getTime());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getDay());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getMultiple());
    }

    public void testScheduleMultiple() throws Exception {

        final Element dist = getDistElement("testprojectMultiple", 4);
        final Element ant = getAntElement(dist);
        assertEquals("This unit test requires there be a '" + ATR_NAME_MULTIPLE + "' attribute",
                "2", ant.getAttributeValue(ATR_NAME_MULTIPLE));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);

        assertEquals("Distributed builder should wrap child-builder schedule fields",
                2, masterBuilder.getMultiple());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getTime());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getDay());
    }

    public void testDefaultModuleValue() throws Exception {

        final Element dist = getDistElement("testprojectNoModule", 2);
        assertNull("This unit test requires there be no '" + ATR_NAME_MODULE + "' attribute",
                dist.getAttributeValue(ATR_NAME_MODULE));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);  // this would fail if default "module" value didn't work

        assertEquals("agent/log", masterBuilder.getAgentLogDir());
        assertEquals("master/log", masterBuilder.getMasterLogDir());
        // check PreconfiguredPlugin attib on distributed tag
        final String preConfMsg = "Are PreConfgured Plugin settings still broken for distributed builds?"
                + "\nSee " + BuildAgentServiceImplTest.TEST_CONFIG_FILE + " for more info.";
        assertEquals(preConfMsg, "build.type=test", dist.getAttributeValue("entries"));
    }

    public void testPickAgent2Agents() throws Exception {
        // register agent
        final BuildAgent agentAvailable = new BuildAgent(
                BuildAgentServiceImplTest.TEST_AGENT_PROPERTIES_FILE,
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE, true);
        final BuildAgent agentAvailable2 = new BuildAgent(
                BuildAgentServiceImplTest.TEST_AGENT_PROPERTIES_FILE,
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE, true);
        try {
            assertFalse(agentAvailable.getService().isBusy());
            assertFalse(agentAvailable2.getService().isBusy());

            DistributedMasterBuilder masterBuilder = createMasterBuilder();

            // try to find agents
            final BuildAgentService agentFoundFirst = masterBuilder.pickAgent();
            assertNotNull("Couldn't find first agent", agentFoundFirst);
            assertTrue(agentFoundFirst.isBusy());
            final BuildAgentService agentFoundSecond = masterBuilder.pickAgent();
            assertNotNull("Couldn't find second agent", agentFoundSecond);

            assertTrue(agentFoundFirst.isBusy());
            assertTrue(agentFoundSecond.isBusy());
            final BuildAgentService agentFoundThird = masterBuilder.pickAgent();
            assertNull("Shouldn't find third agent", agentFoundThird);

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
        final BuildAgent agentAvailable = new BuildAgent(
                BuildAgentServiceImplTest.TEST_AGENT_PROPERTIES_FILE,
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE, true);
        try {
            assertFalse(agentAvailable.getService().isBusy());
            agentAvailable.getService().claim(); // mark as busy

            DistributedMasterBuilder masterBuilder = createMasterBuilder();

            // try to find agent, shouldn't find any available
            final BuildAgentService agentBusy = masterBuilder.pickAgent();
            assertNull("Shouldn't find any available agents", agentBusy);

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
        final BuildAgent agentAvailable = new BuildAgent(
                BuildAgentServiceImplTest.TEST_AGENT_PROPERTIES_FILE,
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE, true);
        try {
            assertFalse(agentAvailable.getService().isBusy());

            DistributedMasterBuilder masterBuilder = createMasterBuilder();

            final BuildAgentService agent = masterBuilder.pickAgent();
            assertNotNull("Couldn't find agent", agent);
            assertTrue("Claimed agent should show as busy. (Did we find a better way?)",
                    agent.isBusy());

            // try to find agent, shouldn't find any available
            final BuildAgentService agentBusy = masterBuilder.pickAgent();
            assertNull("Shouldn't find any available agents", agentBusy);

            // set Agent to Not busy, then make sure it can be found again.
            BuildAgentServiceImplTest.callTestDoBuild(false, agent); // only needed to clearOuputFiles() will succeed
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

    public void testPickAgentNoAgents() throws Exception {

        DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostAndTestPropsONLY(
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE
        );

        BuildAgentService agent = masterBuilder.pickAgent();
        assertNull(agent);
    }

    public void testPickAgentNoRegistrars() throws Exception {
        // kill local reggie
        DistributedMasterBuilderTest.killJini(jiniProcess);

        DistributedMasterBuilder masterBuilder = getMasterBuilder_LocalhostAndTestPropsONLY(
                BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE
        );

        BuildAgentService agent = masterBuilder.pickAgent();
        assertNull(agent);
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
