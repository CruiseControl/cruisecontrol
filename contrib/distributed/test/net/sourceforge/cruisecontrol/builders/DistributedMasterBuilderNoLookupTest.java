package net.sourceforge.cruisecontrol.builders;

import junit.framework.TestCase;
import org.jdom.Element;
import net.sourceforge.cruisecontrol.distributed.BuildAgentServiceImplTest;
import net.sourceforge.cruisecontrol.Builder;

import java.util.List;

/**
 * @auther dan rollo
 * Date: Jan 5, 2007
 * Time: 2:08:30 AM
 */
public class DistributedMasterBuilderNoLookupTest extends TestCase {

    public void testDistAttribs() throws Exception {

        final Element dist = DistributedMasterBuilderTest.getDistElement("testproject2", 1);
        assertEquals("testmodule-attribs", dist.getAttributeValue(DistributedMasterBuilderTest.ATR_NAME_MODULE));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);
        assertEquals("agent/log", masterBuilder.getAgentLogDir());
        assertEquals("master/log", masterBuilder.getMasterLogDir());
        // check PreconfiguredPlugin attib on distributed tag
        final String preConfMsg = "Are PreConfgured Plugin settings still broken for distributed builds?"
                + "\nSee " + BuildAgentServiceImplTest.TEST_CONFIG_FILE + " for more info.";
        assertEquals(preConfMsg, "build.type=test", dist.getAttributeValue("entries"));

        // check attribs on nested builder
        final Element childBuilder = masterBuilder.getChildBuilderElement();
        assertEquals("testtargetSuccess", childBuilder.getAttributeValue("target"));
        // check PreconfiguredPlugin attribs on nested builder
        assertEquals(preConfMsg, "${env.ANT_HOME}", childBuilder.getAttributeValue("anthome"));
        assertEquals(preConfMsg, "test/testdist.build.xml", childBuilder.getAttributeValue("buildfile"));
        assertEquals(preConfMsg, "true", childBuilder.getAttributeValue("uselogger"));

        // check preconfigured child "property" element
        final List builderChildren = childBuilder.getChildren();
        assertEquals(preConfMsg + " Wrong number of preconfigured child <property> elements.",
                4, builderChildren.size());
        checkChildProperty(preConfMsg, (Element) builderChildren.get(0), "childInline", "childInlineValue");
        checkChildProperty(preConfMsg, (Element) builderChildren.get(1), "testChild", "testChildValueInline");
        checkChildProperty(preConfMsg, (Element) builderChildren.get(2), "testChild",
                "testChildValuePre"); // this is a duplicate default, and should also appear, unless logic changes
        checkChildProperty(preConfMsg, (Element) builderChildren.get(3), "testPreConfChildNew",
                "testPreConfAntChildNewValue"); // this is a non-duplicate default
    }
    private static void checkChildProperty(String preConfMsg, Element childProperty,
                                           String expectedName, String expectedValue) {
        assertEquals(preConfMsg + " Check <property> child.",
                "property", childProperty.getName());
        assertEquals(preConfMsg + " Check <property> child name.",
                expectedName, childProperty.getAttributeValue("name"));
        assertEquals(preConfMsg + " Check <property> child value.",
                expectedValue, childProperty.getAttributeValue("value"));
    }


    public void testScheduleDay() throws Exception {

        final Element dist = DistributedMasterBuilderTest.getDistElement("testprojectNoModule", 2);
        final Element ant = DistributedMasterBuilderTest.getAntElement(dist);
        assertEquals("This unit test requires there be a '" + DistributedMasterBuilderTest.ATR_NAME_DAY + "' attribute",
                "7", ant.getAttributeValue(DistributedMasterBuilderTest.ATR_NAME_DAY));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);

        assertEquals("Distributed builder should wrap child-builder schedule fields",
                7, masterBuilder.getDay());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getTime());
        // @todo Is this logic correct, or should value be NOT_SET?
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                1, masterBuilder.getMultiple());
    }

    public void testScheduleTime() throws Exception {

        final Element dist = DistributedMasterBuilderTest.getDistElement("testprojectTime", 3);
        final Element ant = DistributedMasterBuilderTest.getAntElement(dist);
        assertEquals("This unit test requires there be a '"
                + DistributedMasterBuilderTest.ATR_NAME_TIME + "' attribute",
                "0530", ant.getAttributeValue(DistributedMasterBuilderTest.ATR_NAME_TIME));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);

        assertEquals("Distributed builder should wrap child-builder schedule fields",
                530, masterBuilder.getTime());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getDay());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getMultiple());
    }

    public void testScheduleMultiple() throws Exception {

        final Element dist = DistributedMasterBuilderTest.getDistElement("testprojectMultiple", 4);
        final Element ant = DistributedMasterBuilderTest.getAntElement(dist);
        assertEquals("This unit test requires there be a '"
                + DistributedMasterBuilderTest.ATR_NAME_MULTIPLE + "' attribute",
                "2", ant.getAttributeValue(DistributedMasterBuilderTest.ATR_NAME_MULTIPLE));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);

        assertEquals("Distributed builder should wrap child-builder schedule fields",
                2, masterBuilder.getMultiple());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getTime());
        assertEquals("Distributed builder should wrap child-builder schedule fields",
                Builder.NOT_SET, masterBuilder.getDay());
    }

    public void testDefaultModuleValue() throws Exception {

        final Element dist = DistributedMasterBuilderTest.getDistElement("testprojectNoModule", 2);
        assertNull("This unit test requires there be no '"
                + DistributedMasterBuilderTest.ATR_NAME_MODULE + "' attribute",
                dist.getAttributeValue(DistributedMasterBuilderTest.ATR_NAME_MODULE));

        final DistributedMasterBuilder masterBuilder = new DistributedMasterBuilder();
        masterBuilder.configure(dist);  // this would fail if default "module" value didn't work

        assertEquals("agent/log", masterBuilder.getAgentLogDir());
        assertEquals("master/log", masterBuilder.getMasterLogDir());
        // check PreconfiguredPlugin attib on distributed tag
        final String preConfMsg = "Are PreConfgured Plugin settings still broken for distributed builds?"
                + "\nSee " + BuildAgentServiceImplTest.TEST_CONFIG_FILE + " for more info.";
        assertEquals(preConfMsg, "build.type=test", dist.getAttributeValue("entries"));
    }

}
