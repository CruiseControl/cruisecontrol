/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.builders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlConfigIncludeTest;
import net.sourceforge.cruisecontrol.Progress;

import org.jdom2.CDATA;
import org.jdom2.Content;
import org.jdom2.Element;

/**
 * CompositeBuilder test class.
 *
 * @author <a href="mailto:frederic.mockel@gillardon.de">Frederic Mockel</a>
 */
public class CompositeBuilderTest extends TestCase {

    private CompositeBuilder builder;

    private static final String BUILD_LOG_TXT_SUCCESS
            = "<build time=\"1 second\" >"
            + "<target name=\"exec\">"
            + "<task name=\"cvs\">"
            + "<message priority=\"info\">do something</message>"
            + "<message priority=\"info\">do something more</message>"
            + "<message priority=\"info\">boring...</message>"
            + "<message priority=\"error\">non-fatal error occured</message>"
            + "</task>"
            + "</target>"
            + "</build>";

    private static final String BUILD_LOG_TXT_FAILED
            = "<build time=\"1 second\" error=\"Mock build failed\">"
            + "<target name=\"exec\">"
            + "<task name=\"cvs\">"
            + "<message priority=\"info\">do something</message>"
            + "<message priority=\"info\">do something more</message>"
            + "<message priority=\"info\">boring...</message>"
            + "<message priority=\"error\">error occured</message>"
            + "</task>"
            + "</target>"
            + "</build>";

    public CompositeBuilderTest(String name) {
        super(name);
    }

    public void testValidateCalledOncePerChildBuilder() throws Exception {
        final Builder mockBuilder = new MockBuilder() {
            private static final long serialVersionUID = 8004066753999645164L;

            private int validateCallCount;

            @Override
            public void validate() {
                assertFalse("builder.validate() has been called multiple times", validateCallCount > 0);
                validateCallCount++;
            }
        };

        builder = new CompositeBuilder();
        builder.add(mockBuilder);
        builder.validate();
    }

    public void testValidateValidatesAllChildBuilders() throws Exception {
        final Builder mockBuilder = new MockBuilder();
        // make child builder invalid
        mockBuilder.setTime("0000");
        mockBuilder.setMultiple(2);

        builder = new CompositeBuilder();

        builder.add(mockBuilder);
        try {
            builder.validate();
            fail("CompositeBuilder should have failed validating an invalid child builder");
        } catch (CruiseControlException e) {
            assertEquals("Only one of 'time' or 'multiple' are allowed on builders.", e.getMessage());
        }

        // make child builer valid
        mockBuilder.setTime(Builder.NOT_SET + "");
        builder.validate();
    }

    public void testValidateShouldThrowExceptionWhenNoBuilderIsAdded() {
        builder = new CompositeBuilder();
        try {
            builder.validate();
            fail();
        } catch (CruiseControlException expected) {
        }
    }

    @SuppressWarnings(value = "unchecked")
    public void testInsertBuildLogHeader() throws Exception {

        final String buildElementName = "build";
        final Element buildResult = new Element(buildElementName);
        assertEquals(0, buildResult.getContent().size());

        final String buildLogMsg = "buildLogMsg; child";
        final String attribName = "attribName";
        final String attribSubName = "attribSubName";

        // @todo Rearrange these elements (even nesting childLog elements?), might display this info in reporting apps

        CompositeBuilder.insertBuildLogHeader(buildResult, buildLogMsg, 0, attribName, attribSubName);

        assertEquals(buildElementName, buildResult.getName());

        final List<Content> content = buildResult.getContent();
        int idx = 0;
        final Element elmTarget = (Element) content.get(idx++);
        assertEquals("[Element: <target/>]", elmTarget.toString());
        assertEquals(attribName, elmTarget.getAttribute("name").getValue());
        assertNotNull(elmTarget.getAttribute("time").getValue());
        final Element elmTask = (Element) elmTarget.getContent(0);
        assertEquals(attribSubName, elmTask.getAttribute("name").getValue());

        final Element elmMessage = (Element) content.get(idx);
        assertEquals("[Element: <message/>]", elmMessage.toString());
        final CDATA elmCData = (CDATA) elmMessage.getContent(0);
        assertEquals(buildLogMsg + " build attributes: ", elmCData.getValue());

        assertEquals(2, content.size());
    }

    public void testBuildAllBuildersWhenNoErrorOccured() throws Exception {

        final Element buildLogSuccess = CruiseControlConfigIncludeTest.elementFromString(BUILD_LOG_TXT_SUCCESS);

        builder = new CompositeBuilder();
        final Map<String, String> buildProperties = new HashMap<String, String>();
        MockBuilder mock1 = new MockBuilder("builder1");
        MockBuilder mock2 = new MockBuilder("builder2");

        builder.add(mock1);
        builder.add(mock2);

        mock1.setBuildLogXML(buildLogSuccess);
        mock2.setBuildLogXML(buildLogSuccess);

        final Element result = builder.build(buildProperties, null);
        assertNotNull(result);

        assertTrue(mock1.getName() + " didn't build", mock1.isBuildCalled());
        assertTrue(mock2.getName() + " didn't build", mock2.isBuildCalled());
    }

    public void testBuildWithTargetAllBuilders() throws Exception {

        final Element buildLogSucess = CruiseControlConfigIncludeTest.elementFromString(BUILD_LOG_TXT_SUCCESS);

        builder = new CompositeBuilder();
        final Map<String, String> buildProperties = new HashMap<String, String>();
        MockBuilder mock1 = new MockBuilder("builder1");
        MockBuilder mock2 = new MockBuilder("builder2");

        builder.add(mock1);
        builder.add(mock2);

        mock1.setBuildLogXML(buildLogSucess);
        mock2.setBuildLogXML(buildLogSucess);

        final String mockTarget = "mockTarget";
        final Element result = builder.buildWithTarget(buildProperties, mockTarget, null);
        assertNotNull(result);

        assertTrue(mock1.getName() + " didn't build", mock1.isBuildCalled());
        assertEquals(mock1.getName() + " missing target", mockTarget, mock1.getTarget());

        assertTrue(mock2.getName() + " didn't build", mock2.isBuildCalled());
        assertEquals(mock2.getName() + " missing target", mockTarget, mock2.getTarget());
    }

    public void testBuildAllBuildersWhenAnErrorOccured() throws Exception {

        final Element buildLogSuccess = CruiseControlConfigIncludeTest.elementFromString(BUILD_LOG_TXT_SUCCESS);

        final Element buildLogFailed = CruiseControlConfigIncludeTest.elementFromString(BUILD_LOG_TXT_FAILED);

        builder = new CompositeBuilder();
        final Map<String, String> buildProperties = new HashMap<String, String>();
        MockBuilder mock1 = new MockBuilder("builder1");
        MockBuilder mock2WithError = new MockBuilder("builder2-Fail");
        MockBuilder mock3NotRun = new MockBuilder("builder3-NotRun");

        builder.add(mock1);
        builder.add(mock2WithError);
        builder.add(mock3NotRun);

        mock1.setBuildLogXML(buildLogSuccess);
        mock2WithError.setBuildLogXML(buildLogFailed);
        mock3NotRun.setBuildLogXML(buildLogSuccess);

        builder.build(buildProperties, null);

        assertTrue(mock1.isBuildCalled());

        assertTrue(mock2WithError.isBuildCalled());

        assertFalse(mock3NotRun.getName() + " should not have built", mock3NotRun.isBuildCalled());
        assertEquals(mock3NotRun.getName() + " should not have built with target", null, mock3NotRun.getTarget());
    }

    public void testBuildWithTargetWhenAnErrorOccured() throws Exception {

        final Element buildLogSuccess = CruiseControlConfigIncludeTest.elementFromString(BUILD_LOG_TXT_SUCCESS);

        final Element buildLogFailed = CruiseControlConfigIncludeTest.elementFromString(BUILD_LOG_TXT_FAILED);

        builder = new CompositeBuilder();
        final Map<String, String> buildProperties = new HashMap<String, String>();
        MockBuilder mock1 = new MockBuilder("builder1");
        MockBuilder mock2WithError = new MockBuilder("builder2-Fail");
        MockBuilder mock3NotRun = new MockBuilder("builder3-NotRun");

        builder.add(mock1);
        builder.add(mock2WithError);
        builder.add(mock3NotRun);

        mock1.setBuildLogXML(buildLogSuccess);
        mock2WithError.setBuildLogXML(buildLogFailed);
        mock3NotRun.setBuildLogXML(buildLogSuccess);

        final String mockTargetWError = "mockTargetWithError";
        final Element result = builder.buildWithTarget(buildProperties, mockTargetWError, null);
        assertNotNull(result);

        assertTrue(mock1.isBuildCalled());
        assertEquals(mock1.getName() + " missing target", mockTargetWError, mock1.getTarget());

        assertTrue(mock2WithError.isBuildCalled());
        assertEquals(mock2WithError.getName() + " missing target", mockTargetWError, mock2WithError.getTarget());

        assertFalse(mock3NotRun.getName() + " should not have built", mock3NotRun.isBuildCalled());
        assertNull(mockTargetWError, mock3NotRun.getTarget());
    }

    public void testGetBuilders() throws Exception {
        builder = new CompositeBuilder();
        assertEquals(0, builder.getBuilders().length);

        MockBuilder mock1 = new MockBuilder("builder1");
        builder.add(mock1);
        assertEquals(1, builder.getBuilders().length);

        MockBuilder mock2 = new MockBuilder("builder2");
        builder.add(mock2);
        assertEquals(2, builder.getBuilders().length);
    }


    private void assertBuildTimeoutSuccess() throws CruiseControlException {
        final long startTime = System.currentTimeMillis();
        final Element buildElement = builder.build(null, null);
        assertTrue((System.currentTimeMillis() - startTime) < 9 * 1000L);
        assertNull(buildElement.getAttributeValue("error"));
    }
    private void assertBuildWithTargetTimeoutSuccess() throws CruiseControlException {
        final long startTime = System.currentTimeMillis();
        final Element buildElement = builder.buildWithTarget(null, null, null);
        assertTrue((System.currentTimeMillis() - startTime) < 9 * 1000L);
        assertNull(buildElement.getAttributeValue("error"));
    }

    public void testNoBuildTimeout() throws Exception {
        // test build
        builder = new CompositeBuilder();
        final MockBuilder mock1 = new MockBuilder("builder1-noTimeout");
        builder.add(mock1);
        assertBuildTimeoutSuccess();

        // test buildWithTarget
        builder = new CompositeBuilder();
        builder.add(mock1);
        assertBuildWithTargetTimeoutSuccess();
    }

    public void testBuildTimeoutWithNoTimeout() throws Exception {
        // test build
        builder = new CompositeBuilder();
        // test with timeout
        final int timeoutSecs = 1;
        builder.setTimeout(timeoutSecs);
        final MockBuilder mock1 = new MockBuilder("builder1-timeout");
        builder.add(mock1);
        assertBuildTimeoutSuccess();

        // test buildWithTarget
        builder = new CompositeBuilder();
        // test with timeout
        builder.setTimeout(timeoutSecs);
        builder.add(mock1);
        assertBuildWithTargetTimeoutSuccess();
    }

    private void assertBuildTimeoutError() throws CruiseControlException {
        final long startTime = System.currentTimeMillis();
        final Element buildElement = builder.build(null, null);
        assertTrue((System.currentTimeMillis() - startTime) < 9 * 1000L);
        assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);
    }
    private void assertBuildWithTargetTimeoutError() throws CruiseControlException {
        final long startTime = System.currentTimeMillis();
        final Element buildElement = builder.buildWithTarget(null, null, null);
        assertTrue((System.currentTimeMillis() - startTime) < 9 * 1000L);
        assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);
    }

    public void testBuildTimeoutWithTimeout() throws Exception {
        // test build
        builder = new CompositeBuilder();
        // test with timeout
        final int timeoutSecs = 1;
        builder.setTimeout(timeoutSecs);
        final MockBuilder mock1 = new MockBuilder("builder1-timeout") {
            @Override
            public Element build(final Map<String, String> properties, final Progress progress) {
                final Element result = super.build(properties, progress);

                // wait for longer than timeout
                try {                               // + 1 works on linux, not on winz
                    Thread.sleep((timeoutSecs * 1000L) + 500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return result;
            }
        };
        builder.add(mock1);
        assertBuildTimeoutError();

        // test buildWithTarget
        builder = new CompositeBuilder();
        // test with timeout
        builder.setTimeout(timeoutSecs);
        builder.add(mock1);
        assertBuildWithTargetTimeoutError();
    }

} // CompositeBuilderTest
