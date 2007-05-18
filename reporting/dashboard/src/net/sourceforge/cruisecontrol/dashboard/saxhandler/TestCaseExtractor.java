package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;
import net.sourceforge.cruisecontrol.dashboard.BuildTestCase;
import net.sourceforge.cruisecontrol.dashboard.BuildTestCaseResult;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TestCaseExtractor extends SAXBasedExtractor {

    private boolean readingTestCase;

    private boolean readingError;

    private boolean readingFailure;

    private BuildTestCase testcase;

    private String name;

    private String duration;

    private String classname;

    private String errorOrFailureMessage;

    private String errorOrFailureDetail = "";

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("testcase".equals(qName)) {
            readingTestCase = true;
            name = attributes.getValue("name");
            duration = attributes.getValue("time");
            classname = attributes.getValue("classname");
        }
        if (readingTestCase && "error".equals(qName)) {
            readingError = true;
            errorOrFailureMessage = attributes.getValue("message");
        }
        if (readingTestCase && "failure".equals(qName)) {
            readingFailure = true;
            errorOrFailureMessage = attributes.getValue("message");
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingError || readingFailure) {
            String text = new String(ch, start, length);
            if (StringUtils.isBlank(text)) {
                return;
            }
            errorOrFailureDetail += text;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (readingError || readingFailure) {
            testcase = new BuildTestCase(name, duration, classname, errorOrFailureMessage, errorOrFailureDetail,
                    readingError ? BuildTestCaseResult.ERROR : BuildTestCaseResult.FAILED);
        }
        if ("testcase".equals(qName)) {
            if (!(readingError || readingFailure)) {
                testcase = new BuildTestCase(name, duration, classname, "", "", BuildTestCaseResult.PASSED);
            }
            readingTestCase = false;
            readingError = false;
            readingFailure = false;
            errorOrFailureDetail = "";
        }
    }

    public void report(Map resultSet) {
        if (testcase != null) {
            resultSet.put("testcase", testcase);
        }
    }
}
