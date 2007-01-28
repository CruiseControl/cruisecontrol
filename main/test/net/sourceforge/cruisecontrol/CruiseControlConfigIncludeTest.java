package net.sourceforge.cruisecontrol;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.config.XmlResolver;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

public class CruiseControlConfigIncludeTest extends TestCase {

    private Element rootElement;
    private Element includeElement;
    private XmlResolver resolver;

    protected void setUp() throws Exception {
        StringBuffer configText = new StringBuffer(100);
        configText.append("<cruisecontrol>");
        configText.append("  <plugin name='foo.project'");
        configText.append("  classname='net.sourceforge.cruisecontrol.MockProjectInterface'/>");
        configText.append("  <include.projects file='include.xml'/>");
        configText.append("  <foo.project name='in.root'/>");
        configText.append("</cruisecontrol>");
        rootElement = elementFromString(configText.toString());

        StringBuffer includeText = new StringBuffer(200);
        includeText.append("<cruisecontrol>");
        includeText.append("  <foo.project name='in.include'/>");
        includeText.append("</cruisecontrol>");
        includeElement = elementFromString(includeText.toString());

        resolver = new IncludeXmlResolver(includeElement);
    }

    protected void tearDown() throws Exception {
        rootElement = null;
        includeElement = null;
        resolver = null;
    }

    public void testShouldLoadIncludedProjects() throws Exception {
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolver);
        assertEquals(2, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
        assertIsFooProject(config.getProject("in.include"));
    }

    public void testIncludesCanDefinePlugins() throws CruiseControlException {
        String newProjectTag = "new.project.type";
        
        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", newProjectTag);
        pluginElement.setAttribute("classname", MockProjectInterface.class.getName());
        includeElement.addContent(pluginElement);

        Element barElement = new Element(newProjectTag);
        barElement.setAttribute("name", "bar");
        includeElement.addContent(barElement);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolver);
        assertEquals(3, config.getProjectNames().size());
        assertIsFooProject(config.getProject("bar"));
    }
    
    public void testPropertiesShouldBeAvailableToIncludedProjects() throws CruiseControlException {
        Element property = new Element("property");
        property.setAttribute("name", "baz");
        property.setAttribute("value", "goo");
        rootElement.addContent(property);
        
        Element project = new Element("foo.project");
        project.setAttribute("name", "${baz}");
        includeElement.addContent(project);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolver);
        assertEquals(3, config.getProjectNames().size());
        assertIsFooProject(config.getProject("goo"));
    }
    
    public void testErrorsInIncludeShouldBeContained() throws CruiseControlException {
        Element unknownPlugin = new Element("unknown.plugin.error");
        includeElement.addContent(unknownPlugin);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolver);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
    }
    
    public void testErrorsParsingIncludeShouldBeContained() throws CruiseControlException {
        XmlResolver resolverHitsError = new XmlResolver() {
            public Element getElement(String path) throws CruiseControlException {
                throw new CruiseControlException("simulate parse error");
            }
        };
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolverHitsError);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
    }

    private Element elementFromString(String text) throws CruiseControlException {
        InputStream is = new ByteArrayInputStream(text.getBytes());
        return Util.loadRootElement(is);
    }

    private void assertIsFooProject(ProjectInterface project) {
        assertNotNull(project);
        assertEquals(MockProjectInterface.class.getName(), project.getClass().getName());
    }
    
    private class IncludeXmlResolver implements XmlResolver {
        
        private Element includeElement;
        
        IncludeXmlResolver(Element element) {
            includeElement = element;
        }
        
        public Element getElement(String path) throws CruiseControlException {
            assertEquals("include.xml", path);
            return includeElement;
        }
    }

}
