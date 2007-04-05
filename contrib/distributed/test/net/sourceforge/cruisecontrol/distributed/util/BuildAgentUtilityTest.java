package net.sourceforge.cruisecontrol.distributed.util;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.awt.GraphicsEnvironment;

import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilderTest;
import org.apache.log4j.Logger;

/**
 * @author Dan Rollo
 * Date: Mar 22, 2007
 * Time: 9:23:58 PM
 */
public class BuildAgentUtilityTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(BuildAgentUtilityTest.class);

    private static final class MockUI extends BuildAgentUtility.UI {
        private MockUI() {
            super();
        }
    }
    private MockUI mockUI;


    protected void setUp() throws Exception {
        DistributedMasterBuilderTest.setupInsecurePolicy();

        if (GraphicsEnvironment.isHeadless()) {
            LOG.warn("WARNING: DETECTED HEADLESS ENVIRONMENT. Skipping test: "
                    + getClass().getName() + ".setUp() MockUI creation");
        } else {
            mockUI = new MockUI();
        }
    }
    protected void tearDown() throws Exception {
        if (mockUI != null) {
            // clear all agent util prefs
            mockUI.getPrefsRoot().removeNode();
            mockUI.getPrefsRoot().flush();
        }
    }


    public void testDiscoveryInstanceReuse() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            LOG.warn("WARNING: DETECTED HEADLESS ENVIRONMENT. Skipping test: "
                    + getClass().getName() + ".testDiscoveryInstanceReuse()");
            return;
        }

        final BuildAgentUtility buildAgentUtility = new BuildAgentUtility(mockUI);
        final List agents = new ArrayList();
        buildAgentUtility.getAgentInfoAll(agents);
        // call again to make sure disovery instance is not terminated
        buildAgentUtility.getAgentInfoAll(agents);
    }

}
