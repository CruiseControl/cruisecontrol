package net.sourceforge.cruisecontrol;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;

import javax.management.JMException;
import javax.management.MBeanServer;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.config.XmlResolver;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

public class CruiseControlConfigIncludeTest extends TestCase {

    private File configFile;
    private ArrayList filesToDelete = new ArrayList();

    protected void setUp() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/testconfig-include.xml");
        configFile = new File(URLDecoder.decode(url.getPath(), "utf-8"));
    }

    protected void tearDown() throws Exception {
        for (Iterator iter = filesToDelete.iterator(); iter.hasNext();) {
            File file = (File) iter.next();
            file.delete();
        }
        filesToDelete = null;
    }
    
    public void testConfigShouldHaveIncludedProjects() throws Exception {
        StringBuffer includeText = new StringBuffer(100);
        includeText.append("<cruisecontrol>").append("\n");
        includeText.append("<plugin name='foo.project'").append("\n");
        includeText.append("classname='net.sourceforge.cruisecontrol.CruiseControlConfigIncludeTest$FooProject'/>");
        includeText.append("\n");
        includeText.append("<foo.project name='bar'>").append("\n");
        includeText.append("</foo.project>").append("\n");
        includeText.append("</cruisecontrol>").append("\n");
        writeIncludeXml(includeText.toString());
        
        Element rootElement = Util.loadRootElement(configFile);
        CruiseControlConfig config = new CruiseControlConfig(rootElement, new Resolver());

        assertEquals(1, config.getProjectNames().size());
    }

    private void writeIncludeXml(String string) throws IOException {
        File file = new File(configFile.getParentFile(), "include.xml");
        filesToDelete.add(file);
        FileWriter writer = new FileWriter(file);
        writer.write(string);
        writer.close();
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

    private class Resolver implements XmlResolver {

        public Element getElement(String path) throws CruiseControlException {
            File file = new File(configFile.getParentFile(), path);
            return Util.loadRootElement(file);
        }
        
    }
    
}
