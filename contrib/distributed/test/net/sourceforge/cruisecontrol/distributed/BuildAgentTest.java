package net.sourceforge.cruisecontrol.distributed;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilderTest;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.entry.Entry;

import java.rmi.RemoteException;

/**
 * @author: Dan Rollo
 * Date: Jul 6, 2005
 * Time: 4:12:20 PM
 */
public class BuildAgentTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(net.sourceforge.cruisecontrol.distributed.BuildAgentTest.class);

    private DistributedMasterBuilderTest.ProcessInfoPump jiniProcessPump;

    protected void setUp() throws Exception {
        jiniProcessPump = DistributedMasterBuilderTest.startJini(LOG, Level.INFO);
    }

    protected void tearDown() throws Exception {
        BuildAgent.kill();
        DistributedMasterBuilderTest.killJini(jiniProcessPump);
    }

    private static Object findAgent(final ServiceRegistrar reg,
                                    final int retries, final boolean expectedFoundResult)
            throws RemoteException, InterruptedException {

        final Entry[] entries = SearchablePropertyEntries.getPropertiesAsEntryArray(
                new SearchablePropertyEntries(
                        BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE).getProperties()
        );
        return findAgent(reg, retries, expectedFoundResult, entries);
    }

    private static Object findAgent(final ServiceRegistrar reg,
                                    final int retries, final boolean expectedFoundResult, final Entry[] entries)
            throws RemoteException, InterruptedException {

        int retryCount = 0;
        Object result;
        boolean isFound;
        do {
            if (retryCount > 0) {
                LOG.info("\tFind agent unit test retry " + retryCount + "...");
                Thread.sleep(1000); // wait a bit before retrying
            }

            result = reg.lookup(new ServiceTemplate(null,
                new Class[]{BuildAgentService.class},
                entries));

            isFound = (result != null);

            retryCount++;

        } while ((expectedFoundResult != isFound) && (retryCount < retries));

        if (expectedFoundResult) {
            assertNotNull("Should find agent", result);
        } else {
            assertNull("Should not find agent", result);
        }

        return result;
    }

    /**
     * This test requires a bunch of manual steps:
     * 1. Build the cc-agent.war (created via: ant war-agent).
     * 2. Deploy cc-agent.war to a web server.
     * 3. Manually launch agent via webstat (http://localhost:8080/cc-agent/agent.jnlp).
     * 4. Manually run this test.
     * @throws Exception if anything unexpected goes wrong in the test
     */
    public void manual_testRestart() throws Exception {
        final ServiceRegistrar reg = DistributedMasterBuilderTest.findTestLookupService(20);
        assertNotNull("Couldn't find registrar.", reg);

        final Entry[] entries = SearchablePropertyEntries.getPropertiesAsEntryArray(
                new SearchablePropertyEntries(
                        BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE).getProperties()
        );
        // work around timestamp prefix in build.type entry
        final int idxBuildTypeEntry = 3;
        assertEquals("Wrong entry in position where we expected to find 'build.type'.",
                "build.type", ((PropertyEntry) entries[idxBuildTypeEntry]).name);
        entries[idxBuildTypeEntry] = new PropertyEntry(((PropertyEntry) entries[idxBuildTypeEntry]).name, "test");

        final BuildAgentService agentService = (BuildAgentService) findAgent(reg, 3, true, entries);
        assertNotNull(agentService.getMachineName());
        agentService.restart(false);
        // allow time for the relaunched agent to spin up and register
        Thread.sleep(60 * 1000);
        // verify first agent is dead
        try {
            agentService.getMachineName();
            fail("Agent should be dead");
        } catch (Exception e) {
            // good, this is what we want.
        }
        // find the newly relaunched agent
        final BuildAgentService agentService2 = (BuildAgentService) findAgent(reg, 3, true, entries);
        assertNotNull(agentService2.getMachineName());
        agentService2.kill(false);
    }

    public void testKillNoUI() throws Exception {
        ServiceRegistrar reg = DistributedMasterBuilderTest.findTestLookupService(20);
        assertNotNull("Couldn't find registrar.", reg);
        findAgent(reg, 3, false);

        final Thread t = new Thread() {
            public void run() {
                BuildAgent.main(new String[] {
                    "-" + BuildAgent.MAIN_ARG_AGENT_PROPS, BuildAgentServiceImplTest.TEST_AGENT_PROPERTIES_FILE,
                    "-" + BuildAgent.MAIN_ARG_USER_PROPS, BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE,
                    "-" + BuildAgent.MAIN_ARG_SKIP_UI
                });
            }
        };
        t.start();
        // allow BuildAgent main to load and register
        final int maxWaitSecsStartup = 30;
        int count = 0;
        while (count < maxWaitSecsStartup && BuildAgent.getMainThread() == null) {
            Thread.sleep(1000);
            count++;
        }
        assertNotNull("Agent didn't start before timeout.", BuildAgent.getMainThread());
        assertTrue("Agent didn't init before timeout.", BuildAgent.getMainThread().isAlive());
        findAgent(reg, 10, true);
        final Thread mainThread = BuildAgent.getMainThread(); // hold onto main thread since kill nullifies it
        assertNotNull("Main thread should not be null.", mainThread);

        BuildAgent.kill();
        assertFalse("Agent didn't die before timeout.", mainThread.isAlive()); // check held thread
        findAgent(reg, 10, false);
    }

    // @todo Find way to skip this test if we are running in a "headless" environment
    public void xxx_testKill() throws Exception {
        ServiceRegistrar reg = DistributedMasterBuilderTest.findTestLookupService(20);
        assertNotNull("Couldn't find registrar.", reg);
        findAgent(reg, 3, false);

        final Thread t = new Thread() {
            public void run() {
                BuildAgent.main(new String[] {
                    "-" + BuildAgent.MAIN_ARG_AGENT_PROPS, BuildAgentServiceImplTest.TEST_AGENT_PROPERTIES_FILE,
                    "-" + BuildAgent.MAIN_ARG_USER_PROPS, BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE
                });
            }
        };
        t.start();
        // allow BuildAgent main to load and register
        final int maxWaitSecsStartup = 30;
        int count = 0;
        while (count < maxWaitSecsStartup && BuildAgent.getMainThread() == null) {
            Thread.sleep(1000);
            count++;
        }
        assertNotNull("Agent didn't start before timeout.", BuildAgent.getMainThread());
        assertTrue("Agent didn't init before timeout.", BuildAgent.getMainThread().isAlive());
        findAgent(reg, 10, true);
        final Thread mainThread = BuildAgent.getMainThread(); // hold onto main thread since kill nullifies it
        assertNotNull("Main thread should not be null.", mainThread);

        BuildAgent.kill();
        assertFalse("Agent didn't die before timeout.", mainThread.isAlive()); // check held thread
        findAgent(reg, 10, false);
    }
}
