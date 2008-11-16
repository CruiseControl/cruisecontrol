package net.sourceforge.cruisecontrol.jmx;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilderTest;
import net.sourceforge.cruisecontrol.distributed.util.BuildAgentUtilityTest;

/**
 * @author Dan Rollo
 * Date: Sep 25, 2008
 * Time: 12:23:55 AM
 */
public class JMXBuildAgentUtilityTest extends TestCase {

    protected void setUp() throws Exception {
        DistributedMasterBuilderTest.setupInsecurePolicy();
        BuildAgentUtilityTest.setFailFast();
    }

    protected void tearDown() throws Exception {
        BuildAgentUtilityTest.clearFailFast();
    }

    // NOTE: Do NOT call killAll() or restartAll() unless you update unit tests to avoid killing/restarting
    // production agents.

    public void testCreate() throws Exception {
        final JMXBuildAgentUtilityMBean mBean = new JMXBuildAgentUtility();

        assertTrue(mBean.getLookupServiceCount() >= 0);
        assertTrue(mBean.getBuildAgents().startsWith("Found: "));
        assertNotNull(mBean.getBuildAgentServiceIds());

        try {
            mBean.kill(null);
            fail("should have failed");
        } catch (IllegalArgumentException e) {
            assertEquals(JMXBuildAgentUtility.MSG_NULL_SERVICEID, e.getMessage());
        }

        try {
            mBean.restart(null);
            fail("should have failed");
        } catch (IllegalArgumentException e) {
            assertEquals(JMXBuildAgentUtility.MSG_NULL_SERVICEID, e.getMessage());
        }

        try {
            mBean.destroyLUS(null);
            fail("should have failed");
        } catch (IllegalArgumentException e) {
            assertEquals(JMXBuildAgentUtility.MSG_NULL_SERVICEID, e.getMessage());
        }
    }

    public void testAfterBuildFinished() throws Exception {
        final JMXBuildAgentUtilityMBean mBean = new JMXBuildAgentUtility();
        assertTrue(mBean.isKillOrRestartAfterBuildFinished());

        mBean.setKillOrRestartAfterBuildFinished(false);
        assertFalse(mBean.isKillOrRestartAfterBuildFinished());
    }

    public void testRefresh() throws Exception {
        final JMXBuildAgentUtilityMBean mBean = new JMXBuildAgentUtility();
        mBean.refresh();
    }
}
