package net.sourceforge.cruisecontrol;

import java.io.StringReader;
import java.util.Map;

import net.sourceforge.cruisecontrol.config.PropertiesPlugin;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.xml.sax.InputSource;

public class CruiseControlConfigCustomPropertiesPluginTest {

    @Test
    public void shouldHandleConfigWithCustomPropertiesPlugin() throws Exception {
        Element rootElement = createElementWithCustomPropertiesPlugin();
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
        public void loadProperties(Map<String, 
                String> properties, boolean failIfMissing) throws CruiseControlException {
        }        
    }

}
