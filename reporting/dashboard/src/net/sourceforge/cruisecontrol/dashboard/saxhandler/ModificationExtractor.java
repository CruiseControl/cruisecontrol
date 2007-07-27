package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.ModificationAction;
import net.sourceforge.cruisecontrol.dashboard.ModificationSet;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ModificationExtractor extends SAXBasedExtractor {
    private ModificationSet modificationsSet = new ModificationSet();

    private String type = "";

    private String user = "";

    private String comment = "";

    private String revision = "";

    private String filename = "";

    private String action = "";

    private boolean readingModification;

    private boolean readingUser;

    private boolean readingComment;

    private boolean readingRivision;

    private boolean readingFileName;

    private boolean readingFile;

    public void endElement(String uri, String localName, String qName) throws SAXException {
        endElementsInModification(qName);
        if ("modification".equals(qName)) {
            modificationsSet.add(type, user, comment, revision, ModificationAction
                    .fromDisplayName(action), filename);
            readingModification = false;
            reset();
        }
        if ("modifications".equals(qName)) {
            canStop(true);
        }
    }

    private void reset() {
        type = "";
        user = "";
        comment = "";
        revision = "";
        filename = "";
        action = "";
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (!readingModification) {
            return;
        }
        String text = new String(ch, start, length);
        if (StringUtils.isBlank(text)) {
            return;
        }
        setUser(text);
        setComment(text);
        setRevision(text);
        setFile(text);
    }

    private void setFile(String text) {
        if (readingFileName) {
            filename += text;
        }
    }

    private void setRevision(String text) {
        if (readingRivision) {
            revision += text;
        }
    }

    private void setComment(String text) {
        if (readingComment) {
            comment += text;
        }
    }

    private void setUser(String text) {
        if (readingUser) {
            user += text;
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if ("modification".equals(qName)) {
            type = attributes.getValue("type");
            readingModification = true;
        }
        if (readingModification) {
            startElementsInModification(qName, attributes);
        }
    }

    private void startElementsInModification(String qName, Attributes attributes) {
        startFile(qName, attributes);
        if (readingFile) {
            startElementInFile(qName);
        }
        startUser(qName);
        startComment(qName);
    }

    private void startFile(String qName, Attributes attributes) {
        if ("file".equals(qName)) {
            readingFile = true;
            action = attributes.getValue("action");
        }
    }

    private void startComment(String qName) {
        if ("comment".equals(qName)) {
            readingComment = true;
        }
    }

    private void startUser(String qName) {
        if ("user".equals(qName)) {
            readingUser = true;
        }
    }

    private void startElementInFile(String qName) {
        if ("filename".equals(qName)) {
            readingFileName = true;
        }
        if ("revision".equals(qName)) {
            readingRivision = true;
        }
    }

    public void report(Map resultSet) {
        resultSet.put("modifications", modificationsSet);
    }

    private void endElementsInModification(String qName) {
        if (readingModification) {
            if (readingFile) {
                endElementsInFile(qName);
            }
            endFileElement(qName);
            endUser(qName);
            endComment(qName);
        }
    }

    private void endComment(String qName) {
        if ("comment".equals(qName)) {
            readingComment = false;
        }
    }

    private void endUser(String qName) {
        if ("user".equals(qName)) {
            readingUser = false;
        }
    }

    private void endElementsInFile(String qName) {
        if ("filename".equals(qName)) {
            readingFileName = false;
        }
        if ("revision".equals(qName)) {
            readingRivision = false;
        }
    }

    private void endFileElement(String qName) {
        if ("file".equals(qName)) {
            readingFile = false;
        }
    }
}