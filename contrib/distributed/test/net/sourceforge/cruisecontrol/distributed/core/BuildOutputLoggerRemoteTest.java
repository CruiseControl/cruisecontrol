package net.sourceforge.cruisecontrol.distributed.core;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.distributed.BuildAgentService;
import net.sourceforge.cruisecontrol.distributed.BuildAgentServiceImplTest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dan Rollo
 *         Date: May 19, 2010
 *         Time: 11:05:31 PM
 */
public class BuildOutputLoggerRemoteTest extends TestCase {

    public void testCreateNullAgent() throws Exception {
        final String projectName = "projectName";

        final BuildOutputLoggerRemote buildOutputLoggerRemote = new BuildOutputLoggerRemote(projectName, null);
        try {
            buildOutputLoggerRemote.getID();
            fail("npe");
        } catch (NullPointerException e) {
            assertNull(e.getMessage());
        }
        
        try {
            buildOutputLoggerRemote.retrieveLines(0);
            fail("npe");
        } catch (NullPointerException e) {
            assertNull(e.getMessage());
        }
    }

    public void testNullProjectName() throws Exception {
        final String projectName = "projectName";

        final BuildAgentService agent = BuildAgentServiceImplTest.createTestAgentImpl();

        final BuildOutputLoggerRemote buildOutputLoggerRemote = new BuildOutputLoggerRemote(projectName, agent);
        final String orig = buildOutputLoggerRemote.getID();
        assertNotNull(orig);

        assertEquals(0, buildOutputLoggerRemote.retrieveLines(0).length);

        assertFalse(orig.equals(buildOutputLoggerRemote.getID()));
    }

    public void testProjectName() throws Exception {
        final String projectName = "projectName";

        final BuildAgentService agent = BuildAgentServiceImplTest.createTestAgentImpl();
        final Map<String, String> projectProperties = new HashMap<String, String>();
        projectProperties.put(PropertiesHelper.PROJECT_NAME, projectName);
        try {
            // gets far enough to set Project name...
            agent.doBuild(null, projectProperties, null, null, null);
            fail("should fail w/ NPE");
        } catch (NullPointerException e) {
            assertEquals(null, e.getMessage());
        }

        final BuildOutputLoggerRemote buildOutputLoggerRemote = new BuildOutputLoggerRemote(projectName, agent);
        final String orig = buildOutputLoggerRemote.getID();
        assertNotNull(orig);

        assertEquals(0, buildOutputLoggerRemote.retrieveLines(0).length);

        assertEquals(orig, buildOutputLoggerRemote.getID());
    }

    public void testIllegalCalls() throws Exception {
        final BuildOutputLoggerRemote buildOutputLoggerRemote = new BuildOutputLoggerRemote(null, null);

        try {
            buildOutputLoggerRemote.clear();
            fail();
        } catch (IllegalStateException e) {
        }

        try {
            buildOutputLoggerRemote.consumeLine(null);
            fail();
        } catch (IllegalStateException e) {
        }

        try {
            buildOutputLoggerRemote.isDataFileEquals(null);
            fail();
        } catch (IllegalStateException e) {
        }

        try {
            buildOutputLoggerRemote.isDataFileSet();
            fail();
        } catch (IllegalStateException e) {
        }
    }
}
