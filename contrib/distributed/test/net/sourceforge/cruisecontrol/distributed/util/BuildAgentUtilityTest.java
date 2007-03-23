package net.sourceforge.cruisecontrol.distributed.util;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;

import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilderTest;

/**
 * @author Dan Rollo
 * Date: Mar 22, 2007
 * Time: 9:23:58 PM
 */
public class BuildAgentUtilityTest extends TestCase {

    private static final class MockUI extends BuildAgentUtility.UI {
        private MockUI() {
            super();
        }
    }
    private MockUI mockUI;

    private static final String MSG_AGENTS_FOUND_ZERO = "Found: 0 agents.\n";

    protected void setUp() throws Exception {
        DistributedMasterBuilderTest.setupInsecurePolicy();
        mockUI = new MockUI();
    }


    public void testDiscoveryInstanceReuse() throws Exception {

        final BuildAgentUtility buildAgentUtility = new BuildAgentUtility(mockUI);
        final List agents = new ArrayList();
        assertEquals(MSG_AGENTS_FOUND_ZERO, buildAgentUtility.getAgentInfoAll(agents));
        assertEquals(0, agents.size());
        // call again to make sure disovery instance is not terminated
        assertEquals(MSG_AGENTS_FOUND_ZERO, buildAgentUtility.getAgentInfoAll(agents));
    }

}
