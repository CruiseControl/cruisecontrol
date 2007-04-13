package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscoveryTest;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Dan Rollo
 * Date: Jan 5, 2007
 * Time: 2:08:30 AM
 */
public class DistributedMasterBuilderNoLookupTest extends TestCase {

    protected void setUp() throws Exception {
        DistributedMasterBuilderTest.setupInsecurePolicy();
    }

    public void testDistAttribs() throws Exception {

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();

        try {
            masterBuilder.validate();
            fail("mising child builder");
        } catch (CruiseControlException e) {
            assertEquals("A nested Builder is required for DistributedMasterBuilder", e.getMessage());
        }
        final Builder mockBuilder = new MockBuilder();
        masterBuilder.add(mockBuilder);

        masterBuilder.validate();
    }

    public void testScheduleDay() throws Exception {

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();

        final Builder nestedBuilder = new MockBuilder();
        nestedBuilder.setDay("Saturday");
        nestedBuilder.setMultiple(2);

        masterBuilder.add(nestedBuilder);
        masterBuilder.validate();
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                7, masterBuilder.getDay());
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                Builder.NOT_SET, masterBuilder.getTime());
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                2, masterBuilder.getMultiple());
    }

    public void testScheduleTime() throws Exception {

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        final Builder nestedBuilder = new MockBuilder();
        nestedBuilder.setTime("530");
        masterBuilder.add(nestedBuilder);
        masterBuilder.validate();
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                530, masterBuilder.getTime());
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                Builder.NOT_SET, masterBuilder.getDay());
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                Builder.NOT_SET, masterBuilder.getMultiple());
    }

    public void testScheduleMultiple() throws Exception {

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        final Builder nestedBuilder = new MockBuilder();
        nestedBuilder.setMultiple(2);
        masterBuilder.add(nestedBuilder);
        masterBuilder.validate();
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                2, masterBuilder.getMultiple());
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                Builder.NOT_SET, masterBuilder.getTime());
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                Builder.NOT_SET, masterBuilder.getDay());
    }

    public void testProjectNameValue() throws Exception {

        final DistributedMasterBuilder masterBuilder = DistributedMasterBuilderTest.getMasterBuilder_LocalhostONLY();

        final Builder nestedBuilder = new MockBuilder();
        masterBuilder.add(nestedBuilder);

        // @todo Remove this when deprecated "module" attribute is deleted
        masterBuilder.setModule("deprecatedModule");

        try {
            masterBuilder.build(new HashMap());
            fail("Missing projectname property should have failed.");
        } catch (CruiseControlException e) {
            assertEquals(DistributedMasterBuilder.MSG_MISSING_PROJECT_NAME, e.getMessage());
        }

        final Map projectProperties = new HashMap();
        projectProperties.put(PropertiesHelper.PROJECT_NAME, "testProjectName");
        masterBuilder.setFailFast(); // to avoid pickAgent() retry loop
        try {
            masterBuilder.build(projectProperties);
            fail("Null agent should have failed.");
        } catch (CruiseControlException e) {
            assertEquals("Distributed build runtime exception", e.getMessage());
        }
    }

    /**
     * This test lives here so we don't have to kill the Jini LUS to run it.
     * @throws Exception if test fails
     */
    public void testPickAgentNoRegistrars() throws Exception {
        // setup discovery so default logic will not wait to find a LUS
        MulticastDiscoveryTest.setDiscovery(MulticastDiscoveryTest.getLocalDiscovery());

        DistributedMasterBuilder masterBuilder = DistributedMasterBuilderTest.getMasterBuilder_LocalhostONLY();
        assertNull("Shouldn't find any available agents", masterBuilder.pickAgent(null));
    }

}
