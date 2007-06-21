package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.BuildTestSuite;

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
        float duration = Float.parseFloat(attributes.getValue("time"));
        int tests = Integer.parseInt(attributes.getValue("tests"));
        int failures = Integer.parseInt(attributes.getValue("failures"));
        int errors = Integer.parseInt(attributes.getValue("errors"));
        return new BuildTestSuite(duration, tests, failures, name, errors);
    }

    public void report(Map resultSet) {
        resultSet.put("testsuites", testSuites);
    }
}
