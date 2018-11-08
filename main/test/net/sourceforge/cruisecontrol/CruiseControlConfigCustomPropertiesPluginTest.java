package net.sourceforge.cruisecontrol;

import java.io.StringReader;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

import net.sourceforge.cruisecontrol.config.PropertiesPlugin;

public class CruiseControlConfigCustomPropertiesPluginTest {

    @Before
    public void setUp() throws Exception {
        CruiseControlOptions.getInstance(this);
    }

    @After
    public void tearDown() throws Exception {
        CruiseControlOptions.delInstance(this);
    }

    @Test
    public void shouldHandleConfigWithCustomPropertiesPlugin() throws Exception {
        final Element rootElement = createElementWithCustomPropertiesPlugin();
        new CruiseControlConfig(rootElement);
    }

    private Element createElementWithCustomPropertiesPlugin() throws Exception {
        String config = "<cruisecontrol>"
            + "<plugin name='my.properties' "
            + "classname='net.sourceforge.cruisecontrol.CruiseControlConfigCustomPropertiesPluginTest$MyProperties'/>"
            + "<my.properties/>"
            + "</cruisecontrol>";

        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(new InputSource(new StringReader(config))).getRootElement();
    }

    public static class MyProperties implements PropertiesPlugin {
        @Override
        public void loadProperties(Map<String,
                String> properties, boolean failIfMissing) throws CruiseControlException {
        }
    }

}
