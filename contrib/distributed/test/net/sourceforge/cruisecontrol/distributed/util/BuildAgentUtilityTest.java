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


    protected void setUp() throws Exception {
        DistributedMasterBuilderTest.setupInsecurePolicy();
        mockUI = new MockUI();
    }
    protected void tearDown() throws Exception {
        // clear all agent util prefs        
        mockUI.getPrefsRoot().removeNode();
        mockUI.getPrefsRoot().flush();
    }


    public void testDiscoveryInstanceReuse() throws Exception {

        final BuildAgentUtility buildAgentUtility = new BuildAgentUtility(mockUI);
        final List agents = new ArrayList();
        buildAgentUtility.getAgentInfoAll(agents);
        // call again to make sure disovery instance is not terminated
        buildAgentUtility.getAgentInfoAll(agents);
    }

}
