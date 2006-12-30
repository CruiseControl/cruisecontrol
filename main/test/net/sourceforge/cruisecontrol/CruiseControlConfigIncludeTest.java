package net.sourceforge.cruisecontrol;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.management.JMException;
import javax.management.MBeanServer;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.config.XmlResolver;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

public class CruiseControlConfigIncludeTest extends TestCase {

    public void testShouldLoadIncludedProjects() throws Exception {
        StringBuffer configText = new StringBuffer(100);
        configText.append("<cruisecontrol>");
        configText.append("  <include.projects file='include.xml'/>");
        configText.append("</cruisecontrol>");
        Element rootElement = elementFromString(configText.toString());
        
        StringBuffer includeText = new StringBuffer(200);
        includeText.append("<cruisecontrol>");
        includeText.append("  <plugin name='foo.project'");
        includeText.append("  classname='net.sourceforge.cruisecontrol.CruiseControlConfigIncludeTest$FooProject'/>");
        includeText.append("  <foo.project name='bar'/>");
        includeText.append("</cruisecontrol>");
        Element includeElement = elementFromString(includeText.toString());
        
        XmlResolver resolver = new IncludeXmlResolver(includeElement);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolver);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("bar"));
    }

    public void testPluginShouldBeAvailableToIncludedProjects() throws CruiseControlException {
        StringBuffer configText = new StringBuffer(100);
        configText.append("<cruisecontrol>");
        configText.append("  <plugin name='foo.project'");
        configText.append("  classname='net.sourceforge.cruisecontrol.CruiseControlConfigIncludeTest$FooProject'/>");
        configText.append("  <include.projects file='include.xml'/>");
        configText.append("  <foo.project name='goo'/>");
        configText.append("</cruisecontrol>");
        Element rootElement = elementFromString(configText.toString());
        
        StringBuffer includeText = new StringBuffer(200);
        includeText.append("<cruisecontrol>");
        includeText.append("  <foo.project name='bar'/>");
        includeText.append("</cruisecontrol>");
        Element includeElement = elementFromString(includeText.toString());
        
        XmlResolver resolver = new IncludeXmlResolver(includeElement);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolver);
        assertEquals(2, config.getProjectNames().size());
        assertIsFooProject(config.getProject("bar"));
    }
    
    public void testPropertiesShouldBeAvailableToIncludedProjects() throws CruiseControlException {
        StringBuffer configText = new StringBuffer(100);
        configText.append("<cruisecontrol>");
        configText.append("  <property name='baz' value='goo'/>");
        configText.append("  <include.projects file='include.xml'/>");
        configText.append("</cruisecontrol>");
        Element rootElement = elementFromString(configText.toString());
        
        StringBuffer includeText = new StringBuffer(200);
        includeText.append("<cruisecontrol>");
        includeText.append("  <plugin name='foo.project'");
        includeText.append("  classname='net.sourceforge.cruisecontrol.CruiseControlConfigIncludeTest$FooProject'/>");
        includeText.append("  <foo.project name='${baz}'/>");
        includeText.append("</cruisecontrol>");
        Element includeElement = elementFromString(includeText.toString());
        
        XmlResolver resolver = new IncludeXmlResolver(includeElement);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolver);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("goo"));
    }
    
    public void testErrorsInIncludeShouldBeContained() throws CruiseControlException {
        StringBuffer configText = new StringBuffer(100);
        configText.append("<cruisecontrol>");
        configText.append("  <plugin name='foo.project'");
        configText.append("  classname='net.sourceforge.cruisecontrol.CruiseControlConfigIncludeTest$FooProject'/>");
        configText.append("  <include.projects file='include.xml'/>");
        configText.append("  <foo.project name='goo'/>");
        configText.append("</cruisecontrol>");
        Element rootElement = elementFromString(configText.toString());
        
        StringBuffer includeText = new StringBuffer(200);
        includeText.append("<cruisecontrol>");
        includeText.append("  <unknown.plugin.error/>");
        includeText.append("  <foo.project name='bar'/>");
        includeText.append("</cruisecontrol>");
        Element includeElement = elementFromString(includeText.toString());
        
        XmlResolver resolver = new IncludeXmlResolver(includeElement);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolver);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("goo"));
    }

    private Element elementFromString(String text) throws CruiseControlException {
        InputStream is = new ByteArrayInputStream(text.getBytes());
        return Util.loadRootElement(is);
    }

    private void assertIsFooProject(ProjectInterface project) {
        assertNotNull(project);
        assertEquals(FooProject.class.getName(), project.getClass().getName());
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
    
    public static class FooProject implements ProjectInterface {

        private String name;

        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }

        public void configureProject() throws CruiseControlException {
        }

        public void execute() {
        }

        public void getStateFromOldProject(ProjectInterface project) throws CruiseControlException {
        }

        public void register(MBeanServer server) throws JMException {
        }

        public void setBuildQueue(BuildQueue buildQueue) {
        }

        public void start() {
        }

        public void stop() {
        }

        public void validate() throws CruiseControlException {
        }
        
    }

}
