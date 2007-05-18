package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import net.sourceforge.cruisecontrol.dashboard.Projects;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ConfigurationHandler extends DefaultHandler {
    private final Projects projects;

    private String projectName;

    public ConfigurationHandler(Projects projects) {
        this.projects = projects;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("project".equals(qName)) {
            projectName = attributes.getValue("name");
            projects.addLogsRoot(projectName);
        }
        if ("artifactspublisher".equals(qName)) {
            projects.addArtifactsRoot(projectName, attributes.getValue("dest"));
        }
    }
}
