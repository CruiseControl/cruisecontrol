package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.BuildTestSuite;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TestSuiteExtractor extends SAXBasedExtractor {

    private TestCaseExtractor testcaseExtractor = new TestCaseExtractor();

    private List testSuites = new ArrayList();

    private BuildTestSuite singleTestSuite;

    private List testcasesForSingleTestSuite;

    private boolean readingTestSuite;

    private Map testcaseResult = new HashMap();

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("testsuite".equals(qName)) {
            readingTestSuite = true;
            singleTestSuite = createSingleTestSuite(attributes);
            testcasesForSingleTestSuite = new ArrayList();
            return;
        }
        if (readingTestSuite) {
            testcaseExtractor.startElement(uri, localName, qName, attributes);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingTestSuite) {
            testcaseExtractor.characters(ch, start, length);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (readingTestSuite) {
            testcaseExtractor.endElement(uri, localName, qName);
        }
        if (readingTestSuite && "testcase".equals(qName)) {
            testcaseExtractor.report(testcaseResult);
            testcasesForSingleTestSuite.add(testcaseResult.get("testcase"));
        }

        if ("testsuite".equals(qName)) {
            readingTestSuite = false;
            singleTestSuite.appendTestCases(testcasesForSingleTestSuite);
            testSuites.add(singleTestSuite);
        }
    }

    private BuildTestSuite createSingleTestSuite(Attributes attributes) {
        String name = attributes.getValue("name");
        float duration;
        try {
            NumberFormat format = NumberFormat.getInstance();
            duration = format.parse(StringUtils.defaultString(attributes.getValue("time"), "0.0")).floatValue();
        } catch (ParseException e) {
            duration = (float) 0.0;
        }
        int tests = Integer.parseInt(StringUtils.defaultString(attributes.getValue("tests"), "0"));
        int failures = Integer.parseInt(StringUtils.defaultString(attributes.getValue("failures"), "0"));
        int errors = Integer.parseInt(StringUtils.defaultString(attributes.getValue("errors"), "0"));
        return new BuildTestSuite(name, duration);
    }

    public void report(Map resultSet) {
        resultSet.put("testsuites", testSuites);
    }
}
