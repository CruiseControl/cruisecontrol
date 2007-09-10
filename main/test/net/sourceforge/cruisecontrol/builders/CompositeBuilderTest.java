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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Builder;

import org.jdom.Element;

/**
 * CompositeBuilder test class.
 *
 * @author <a href="mailto:frederic.mockel@gillardon.de">Frederic Mockel</a>
 */
public class CompositeBuilderTest extends TestCase {

    private CompositeBuilder builder;

    private static final String BUILD_LOG_TXT_1 = "<cruisecontrol>\n"
            + "<modifications>\n"
            + "<modification type=\"always\">\n"
            + "<file action=\"change\">\n"
            + "<filename>force build</filename>\n"
            + "<project>force build</project>\n"
            + "</file>\n"
            + "<date>09/27/2006 00:00:00</date>\n"
            + "<user>cruisecontroluser</user>\n"
            + "<comment />\n"
            + "</modification>\n"
            + "</modifications>\n"
            + "<info>\n"
            + "<property name=\"projectname\" value=\"TestCruisecontrol\" />\n"
            + "</info>\n"
            + "<build time=\"1 second\" >\n"
            + "<target name=\"exec\">\n"
            + "<task name=\"cvs\">\n"
            + "<message priority=\"info\"><do something></message>\n"
            + "<message priority=\"info\"><do something more></message>\n"
            + "<message priority=\"info\"><boring...></message>\n"
            + "</task>\n"
            + "</target>\n"
            + "</build>\n"
            + "</cruisecontrol>\n";

    private static final String BUILD_LOG_TXT_2 = "<cruisecontrol>\n"
            + "<modifications>\n"
            + "<modification type=\"always\">\n"
            + "<file action=\"change\">\n"
            + "<filename>force build</filename>\n"
            + "<project>force build</project>\n"
            + "</file>\n"
            + "<date>09/27/2006 00:00:00</date>\n"
            + "<user>cruisecontroluser</user>\n"
            + "<comment />\n"
            + "</modification>\n"
            + "</modifications>\n"
            + "<info>\n"
            + "<property name=\"projectname\" value=\"TestCruisecontrol\" />\n"
            + "</info>\n"
            + "<build time=\"1 second\" >\n"
            + "<target name=\"exec\">\n"
            + "<task name=\"cvs\">\n"
            + "<message priority=\"info\"><do something></message>\n"
            + "<message priority=\"info\"><do something more></message>\n"
            + "<message priority=\"info\"><boring...></message>\n"
            + "<message priority=\"error\"><error occured></message>\n"
            + "</task>\n"
            + "</target>\n"
            + "</build>\n"
            + "</cruisecontrol>\n";

    public CompositeBuilderTest(String name) {
        super(name);
    }

    public void testValidateCalledOncePerChildBuilder() throws Exception {
        final Builder mockBuilder = new MockBuilder() {
            private int validateCallCount;
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

    public void testBuildAllBuildersWhenNoErrorOccured() throws Exception {

        Element buildLog1 = new Element("cruisecontrol");
        buildLog1.addContent(BUILD_LOG_TXT_1);

        builder = new CompositeBuilder();
        HashMap buildProperties = new HashMap();
        MockBuilder mock1 = new MockBuilder("builder1");
        MockBuilder mock2 = new MockBuilder("builder2");

        builder.add(mock1);
        builder.add(mock2);

        mock1.setBuildLogXML(buildLog1);
        mock2.setBuildLogXML(buildLog1);

        final Element result = builder.build(buildProperties, null);
        assertNotNull(result);

        assertTrue("builder1 didn't build", mock1.isBuildCalled());
        assertTrue("builder2 didn't build", mock2.isBuildCalled());
    }

    public void testBuildWithTargetAllBuilders() throws Exception {

        Element buildLog1 = new Element("cruisecontrol");
        buildLog1.addContent(BUILD_LOG_TXT_1);

        builder = new CompositeBuilder();
        HashMap buildProperties = new HashMap();
        MockBuilder mock1 = new MockBuilder("builder1");
        MockBuilder mock2 = new MockBuilder("builder2");

        builder.add(mock1);
        builder.add(mock2);

        mock1.setBuildLogXML(buildLog1);
        mock2.setBuildLogXML(buildLog1);

        final String mockTarget = "mockTarget";
        final Element result = builder.buildWithTarget(buildProperties, mockTarget, null);
        assertNotNull(result);

        assertTrue("builder1 didn't build", mock1.isBuildCalled());
        assertEquals("builder1 didn't build with target", mockTarget, mock1.getTarget());
        assertTrue("builder2 didn't build", mock2.isBuildCalled());
        assertEquals("builder2 didn't build with target", mockTarget, mock1.getTarget());
    }

    public void testBuildAllBuildersWhenAnErrorOccured() throws Exception {

        Element buildLog1 = new Element("cruisecontrol");
        buildLog1.addContent(BUILD_LOG_TXT_1);
        Element buildLog2 = new Element("cruisecontrol");
        buildLog1.addContent(BUILD_LOG_TXT_2);

        builder = new CompositeBuilder();
        HashMap buildProperties = new HashMap();
        MockBuilder mock1 = new MockBuilder("builder1");
        MockBuilder mock2 = new MockBuilder("builder2");
        MockBuilder mock3 = new MockBuilder("builder3");

        builder.add(mock1);
        builder.add(mock2);
        builder.add(mock3);

        mock1.setBuildLogXML(buildLog1);
        mock2.setBuildLogXML(buildLog2);
        mock3.setBuildLogXML(buildLog1);

        builder.build(buildProperties, null);
        assertTrue("builder3 didn't build", mock3.isBuildCalled());
        assertEquals("builder3 should not have built with target", null, mock3.getTarget());
    }

    public void testBuildWithTargetWhenAnErrorOccured() throws Exception {

        Element buildLog1 = new Element("cruisecontrol");
        buildLog1.addContent(BUILD_LOG_TXT_1);
        Element buildLog2 = new Element("cruisecontrol");
        buildLog1.addContent(BUILD_LOG_TXT_2);

        builder = new CompositeBuilder();
        HashMap buildProperties = new HashMap();
        MockBuilder mock1 = new MockBuilder("builder1");
        MockBuilder mock2 = new MockBuilder("builder2");
        MockBuilder mock3 = new MockBuilder("builder3");

        builder.add(mock1);
        builder.add(mock2);
        builder.add(mock3);

        mock1.setBuildLogXML(buildLog1);
        mock2.setBuildLogXML(buildLog2);
        mock3.setBuildLogXML(buildLog1);

        final String mockTargetWError = "mockTargetWithError";
        final Element result = builder.buildWithTarget(buildProperties, mockTargetWError, null);
        assertNotNull(result);

        assertTrue("builder3 didn't build", mock3.isBuildCalled());
        assertEquals("builder2 didn't build with target", mockTargetWError, mock3.getTarget());
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
} // CompositeBuilderTest
