package net.sourceforge.cruisecontrol.distributed.util;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.awt.Window;

import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilderTest;
import net.sourceforge.cruisecontrol.distributed.core.PreferencesHelper;
import org.apache.log4j.Logger;

/**
 * @author Dan Rollo
 * Date: Mar 22, 2007
 * Time: 9:23:58 PM
 */
public class BuildAgentUtilityTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(BuildAgentUtilityTest.class);

    private static final class MockUI implements PreferencesHelper.UIPreferences, BuildAgentUtility.UISetInfo {

        public Preferences getPrefsBase() {
            return Preferences.userNodeForPackage(BuildAgentUtility.UI.class);
        }

        public Window getWindow() {
            final String msg = "Dummy unit test method not implemented, and should NOT have been called.";
            LOG.error(msg);
            throw new RuntimeException(msg);
        }

        private String infoTextSet;
        public void setInfo(final String infoText) { infoTextSet = infoText; }
        public String getLastInfo() { return infoTextSet; }
    }
    private MockUI mockUI;


    protected void setUp() throws Exception {
        DistributedMasterBuilderTest.setupInsecurePolicy();

        mockUI = new MockUI();
    }
    protected void tearDown() throws Exception {
        // clear all agent util prefs
        mockUI.getPrefsBase().removeNode();

        // @todo Revmove this if/when java 6 prefs impl is fixed on Unix (and annotate which jre contains the fix)
        // see Sun Bug ID: 6568540 - (prefs) Preferences not saved in Webstart app, even after synch()
        try {
            mockUI.getPrefsBase().flush();
        } catch (BackingStoreException e) {
            if ((e.getCause() instanceof IllegalArgumentException)
                    && Double.parseDouble(System.getProperty("java.specification.version")) == 1.6) {
                LOG.error("Warning: Unit Test Exception ignored due to known Java 6 bug (Sun Bug ID: 6568540)");
            } else {
                throw e;
            }
        }
    }


    public void testDiscoveryInstanceReuse() throws Exception {

        final BuildAgentUtility buildAgentUtility = new BuildAgentUtility(mockUI);
        final List agents = new ArrayList();

        assertNull(mockUI.getLastInfo());
        buildAgentUtility.getAgentInfoAll(agents);
        assertNotNull(mockUI.getLastInfo());

        // reset
        mockUI.setInfo(null);
        assertNull(mockUI.getLastInfo());

        // call again to make sure disovery instance is not terminated
        buildAgentUtility.getAgentInfoAll(agents);
        assertNotNull(mockUI.getLastInfo());
    }

}
