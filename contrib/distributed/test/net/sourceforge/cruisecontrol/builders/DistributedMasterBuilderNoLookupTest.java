package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscoveryTest;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.core.MulticastDiscovery;
import net.sourceforge.cruisecontrol.distributed.BuildAgentServiceImplTest;

import java.util.Map;

/**
 * @author: Dan Rollo
 * Date: Jan 5, 2007
 * Time: 2:08:30 AM
 */
public class DistributedMasterBuilderNoLookupTest extends TestCase {

    protected void setUp() throws Exception {
        if (MulticastDiscovery.isDiscoverySet()) {
            MulticastDiscoveryTest.setDiscovery(null);
        }
    }
    protected void tearDown() throws Exception {
        if (MulticastDiscovery.isDiscoverySet()) {
            MulticastDiscoveryTest.setDiscovery(null);
        }
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
        try {
            masterBuilder.validate();
            fail("missing module attrib");
        } catch (CruiseControlException e) {
            assertEquals(DistributedMasterBuilder.MSG_REQUIRED_ATTRIB_MODULE, e.getMessage());
        }
        masterBuilder.setModule("testModule");
        masterBuilder.validate();
    }

    public void testScheduleDay() throws Exception {

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();

        final Builder nestedBuilder = new MockBuilder();
        nestedBuilder.setDay("Saturday");
        nestedBuilder.setMultiple(2);

        masterBuilder.add(nestedBuilder);
        masterBuilder.setModule("testModule");
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
        masterBuilder.setModule("testModule");
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
        masterBuilder.setModule("testModule");
        masterBuilder.validate();
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                2, masterBuilder.getMultiple());
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                Builder.NOT_SET, masterBuilder.getTime());
        assertEquals("Distributed builder should wrap nested builder schedule fields",
                Builder.NOT_SET, masterBuilder.getDay());
    }

    public void testDefaultModuleValue() throws Exception {

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();

        final Builder nestedBuilder = new MockBuilder();
        masterBuilder.add(nestedBuilder);
        // @todo Find a way to use Project.name as default value for "module" attribute
        try {
            masterBuilder.validate();
        } catch (CruiseControlException e) {
            assertEquals(DistributedMasterBuilder.MSG_REQUIRED_ATTRIB_MODULE, e.getMessage());
        }
    }

    /**
     * Ensures discovery in pickAgent() is referenced via getDiscovery() and not by static member var.  
     * @throws Exception if test fails
     */
    public void testPickAgentDiscoveryNonNull() throws Exception {
        DistributedMasterBuilderTest.setupInsecurePolicy();
        DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.setFailFast(true);
        // need to set Entries to prevent finding non-local LUS and/or non-local Build Agents
        masterBuilder.setEntries(getTestDMBEntries());
        assertNull(masterBuilder.pickAgent());
    }

    /**
     * This test lives here so we don't have to kill the Jini LUS to run it.
     * @throws Exception if test fails
     */
    public void testPickAgentNoRegistrars() throws Exception {
        DistributedMasterBuilderTest.setupInsecurePolicy();
        // setup discovery so default logic will not wait to find a LUS
        MulticastDiscoveryTest.setDiscovery(MulticastDiscoveryTest.getLocalDiscovery());

        DistributedMasterBuilder masterBuilder = DistributedMasterBuilderTest.getMasterBuilder_LocalhostONLY();
        assertNull(masterBuilder.pickAgent());
    }

    private static String getTestDMBEntries() {
        final Map userProps
                = PropertiesHelper.loadRequiredProperties(BuildAgentServiceImplTest.TEST_USER_DEFINED_PROPERTIES_FILE);

        final Object retval = userProps.get(BuildAgentServiceImplTest.ENTRY_NAME_BUILD_TYPE);
        assertNotNull("Missing required entry for DMB unit test: " + BuildAgentServiceImplTest.ENTRY_NAME_BUILD_TYPE,
                retval);
        assertTrue(retval instanceof String);

        return BuildAgentServiceImplTest.ENTRY_NAME_BUILD_TYPE + "=" + retval;
    }
}
