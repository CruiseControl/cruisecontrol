package net.sourceforge.cruisecontrol.distributed;

import junit.framework.TestCase;

import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilderTest;

import java.awt.GraphicsEnvironment;

import org.apache.log4j.Logger;

/**
 * @author Dan Rollo
 * Date: Apr 3, 2007
 * Time: 2:39:37 PM
 */
public class BuildAgentEntryOverrideUITest extends TestCase {

    private static final Logger LOG = Logger.getLogger(BuildAgentEntryOverrideUITest.class);

    public void testEditEntryOverrideUI() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            LOG.warn("WARNING: DETECTED HEADLESS ENVIRONMENT. Skipping test: "
                    + getClass().getName() + ".testEditEntryOverrideUI()");
            return;
        }

        final BuildAgent agent = DistributedMasterBuilderTest.createBuildAgent();
        final BuildAgentService agentService = agent.getService();
        agent.clearEntryOverrides(); // otherwise, tests fail if overrides with same test values exist
        final BuildAgentEntryOverrideUI entryOverrideUI = new BuildAgentEntryOverrideUI(null, agentService, null);
        try {
            final PropertyEntry[] origEntries = agent.getEntryOverrides();

            assertFalse(entryOverrideUI.isSaveEnabled());
            entryOverrideUI.doNewRow();
            assertTrue(entryOverrideUI.isSaveEnabled());

            // new entry with empty value
            entryOverrideUI.setValueAt("newName", 0, BuildAgentEntryOverrideUI.COL_NAME);
            entryOverrideUI.doSave();
            assertFalse(entryOverrideUI.isSaveEnabled());
            assertEquals("Empty value should not have been saved.",
                    origEntries.length, agent.getEntryOverrides().length);

            // new entry
            entryOverrideUI.setValueAt("newValue", 0, BuildAgentEntryOverrideUI.COL_VALUE);
            assertTrue(entryOverrideUI.isSaveEnabled());
            entryOverrideUI.doSave();
            assertFalse(entryOverrideUI.isSaveEnabled());
            assertEquals("New entry should have been saved.",
                    origEntries.length + 1, agent.getEntryOverrides().length);

            // clear all
            entryOverrideUI.doClearAll();
            assertTrue(entryOverrideUI.isSaveEnabled());
            assertEquals("Clear should not have been saved yet.",
                    origEntries.length + 1, agent.getEntryOverrides().length);
            entryOverrideUI.doSave();
            assertEquals("All entry overrides should have been cleared.",
                    0, agent.getEntryOverrides().length);

        } finally {
            // terminate JoinManager in BuildAgent
            agent.terminate();
            entryOverrideUI.dispose();
        }
    }
}
