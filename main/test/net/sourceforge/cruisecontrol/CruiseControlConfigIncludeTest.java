package net.sourceforge.cruisecontrol;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.config.FileResolver;
import net.sourceforge.cruisecontrol.config.XmlResolver;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom2.Element;

public class CruiseControlConfigIncludeTest extends TestCase implements ResolverHolder {

    private Element rootElement;
    private Element includeElement;
    private XmlResolver xmlResolver;
    private FileResolver fileResolver;

    protected void setUp() throws Exception {
        final StringBuilder configText = new StringBuilder(200);
        configText.append("<cruisecontrol>");
        configText.append("  <plugin name='foo.project'");
        configText.append("  classname='net.sourceforge.cruisecontrol.MockProjectInterface'/>");
        configText.append("  <include.projects file='include.xml'/>");
        configText.append("  <foo.project name='in.root'/>");
        configText.append("</cruisecontrol>");
        rootElement = elementFromString(configText.toString());

        final StringBuilder includeText = new StringBuilder(200);
        includeText.append("<cruisecontrol>");
        includeText.append("  <foo.project name='in.include'/>");
        includeText.append("</cruisecontrol>");
        includeElement = elementFromString(includeText.toString());

        xmlResolver = new IncludeXmlResolver(includeElement);
        fileResolver = new EmptyFileResolver();
    }

    protected void tearDown() throws Exception {
        rootElement = null;
        includeElement = null;
        xmlResolver = null;
    }

    public void testShouldLoadIncludedProjects() throws Exception {
        final CruiseControlConfig config = new CruiseControlConfig(rootElement, this, null);
        assertEquals(2, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
        assertIsFooProject(config.getProject("in.include"));
    }
    
    public void testShouldLoadedNestedIncludes() throws Exception {
        final StringBuilder includeText = new StringBuilder(200);
        includeText.append("<cruisecontrol>");
        includeText.append("  <include.projects file='include.xml'/>");
        includeText.append("  <foo.project name='in.first.include'/>");
        includeText.append("</cruisecontrol>");
        final Element includeWithNestedInclude = elementFromString(includeText.toString());
        
        final Element[] elements = new Element[2];
        elements[0] = includeWithNestedInclude;
        elements[1] = includeElement;
        xmlResolver = new IncludeXmlResolver(elements);
        
        final CruiseControlConfig config = new CruiseControlConfig(rootElement, this, null);
        assertEquals(3, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
        assertIsFooProject(config.getProject("in.first.include"));
        assertIsFooProject(config.getProject("in.include"));
    }

    public void testIncludesCanDefinePlugins() throws CruiseControlException {
        final String newProjectTag = "new.project.type";
        
        final Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", newProjectTag);
        pluginElement.setAttribute("classname", MockProjectInterface.class.getName());
        includeElement.addContent(pluginElement);

        final Element barElement = new Element(newProjectTag);
        barElement.setAttribute("name", "bar");
        includeElement.addContent(barElement);
        
        final CruiseControlConfig config = new CruiseControlConfig(rootElement, this, null);
        assertEquals(3, config.getProjectNames().size());
        assertIsFooProject(config.getProject("bar"));
    }
    
    public void testPropertiesShouldBeAvailableToIncludedProjects() throws CruiseControlException {
        final Element property = new Element("property");
        property.setAttribute("name", "baz");
        property.setAttribute("value", "goo");
        rootElement.addContent(property);
        
        final Element project = new Element("foo.project");
        project.setAttribute("name", "${baz}");
        includeElement.addContent(project);
        
        final CruiseControlConfig config = new CruiseControlConfig(rootElement, this);
        assertEquals(3, config.getProjectNames().size());
        assertIsFooProject(config.getProject("goo"));
    }
    
    public void testErrorsInIncludeShouldBeContained() throws CruiseControlException {
        final Element unknownPlugin = new Element("unknown.plugin.error");
        includeElement.addContent(unknownPlugin);
        
        final CruiseControlConfig config = new CruiseControlConfig(rootElement, this);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
    }
    
    public void testErrorsParsingIncludeShouldBeContained() throws CruiseControlException {
        xmlResolver = new XmlResolver() {
            public Element getElement(String path) throws CruiseControlException {
                throw new CruiseControlException("simulate parse error");
            }
        };
        
        final CruiseControlConfig config = new CruiseControlConfig(rootElement, this);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
    }

    public void testIncludeFilenameContainsProperty() throws CruiseControlException {
        xmlResolver = new XmlResolver() {
            private final Element includePropertyElement;
            {
                final StringBuilder includeText = new StringBuilder(200);
                includeText.append("<cruisecontrol>");
                includeText.append("  <foo.project name='in.include.withproperty'/>");
                includeText.append("</cruisecontrol>");
                includePropertyElement = elementFromString(includeText.toString());
            }

            public Element getElement(String path) throws CruiseControlException {
                assertEquals("include_FOO_.xml", path);
                return includePropertyElement;
            }
        };

        final Element propertyElement = new Element("property");
        propertyElement.setAttribute("name", "filenameswitch");
        propertyElement.setAttribute("value", "_FOO_");
        rootElement.addContent(propertyElement);
        rootElement.removeChild("include.projects");
        final Element includeTagElement = new Element("include.projects");
        includeTagElement.setAttribute("file", "include${filenameswitch}.xml");
        rootElement.addContent(includeTagElement);

        final CruiseControlConfig config = new CruiseControlConfig(rootElement, this);
        assertEquals(2, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
        assertIsFooProject(config.getProject("in.include.withproperty"));
    }

    public void testIncludeFilenameContainsUnsetProperty() throws CruiseControlException {
        xmlResolver = new XmlResolver() {
            public Element getElement(String path) throws CruiseControlException {
                throw new CruiseControlException("failed to load file []");
            }
        };

        rootElement.removeChild("include.projects");
        final Element includeTagElement = new Element("include.projects");
        includeTagElement.setAttribute("file", "include${filenameswitch}.xml");
        rootElement.addContent(includeTagElement);

        final CruiseControlConfig config = new CruiseControlConfig(rootElement, this);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
    }

    public static Element elementFromString(final String text) throws CruiseControlException {
        final InputStream is = new ByteArrayInputStream(text.getBytes());
        return Util.loadRootElement(is);
    }

	public FileResolver getFileResolver() {
		return fileResolver;
	}

	public XmlResolver getXmlResolver() {
		return xmlResolver;
	}

    
    private void assertIsFooProject(final ProjectInterface project) {
        assertNotNull(project);
        assertEquals(MockProjectInterface.class.getName(), project.getClass().getName());
    }
    
    private class IncludeXmlResolver implements XmlResolver {
        
        private final Element[] includeElements;
        private int count = 0;
        
        IncludeXmlResolver(final Element element) {
            includeElements = new Element[] {element};
        }
        
        IncludeXmlResolver(final Element[] elements) {
            includeElements = elements;
        }
        
        public Element getElement(final String path) throws CruiseControlException {
            assertEquals("include.xml", path);
            final Element element = includeElements[count];
            count++;
            return element;
        }
    }

    private class EmptyFileResolver implements FileResolver {

        public InputStream getInputStream(final String path)
            throws CruiseControlException {
            // FIXME add correct implementation, if required!
            throw new CruiseControlException("Method not implemented yet! Fix it!");
        }
    }
}
