/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
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

package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.publishers.MockPublisher;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;
import org.jdom.Element;

//(POLICE) Been here

public class PluginXMLHelperTest extends TestCase {
	private static Category log = Category.getInstance(PluginXMLHelperTest.class.getName());

	public PluginXMLHelperTest(String name) {
		super(name);
		//(POLICE) This is one way of making _really_ sure that log4j is keeping its mouth shut.
		// This is probably defined in a log4j.config file somewhere. We usually have it in a
		// JUnit suite (but CC doesn't have that right now).
		BasicConfigurator.configure();
		log.getHierarchy().disableAll();
	}

	public void testConfigure() throws Exception {
		Element testElement = new Element("test");
		testElement.setAttribute("somestring", "expectedString");
		testElement.setAttribute("someint", "15");
		Element childElement = new Element("mockpluginchild");
		childElement.setAttribute("somestring", "childString");
		childElement.setAttribute("someint", "16");
		testElement.addContent(childElement);

		PluginXMLHelper helper = new PluginXMLHelper();
		MockPublisher plugin = null;

		//(POLICE) if this goes wrong, the method will throw an exception that will be handled
		// by the JUnit framework. This is the recommended way of doing things like this.
		// Notice that I added a throws clause to the method signature.
		plugin = (MockPublisher) helper.configure(testElement, "net.sourceforge.cruisecontrol.publishers.MockPublisher");

		//(POLICE) all asserts in JUnit is on the form
		// assertXXX("description", expectedValue, actualValue);
		assertEquals("expectedString", plugin.getSomeString());
		assertEquals(15, plugin.getSomeInt());
		assertEquals("childString", plugin.getMockPluginChild().getSomeString());
		assertEquals(16, plugin.getMockPluginChild().getSomeInt());

		//(POLICE) Slightly rewrote this method. Having assertTrue(false) is pretty
		// much the same as having a if(true) then return true else return false :-)
		try {
			plugin = (MockPublisher) helper.configure(testElement, "noclass");
			//(POLICE) a real overkill on the nextline would be to check so that noclass really
			// doesn't exist. But .. hmm, don't think so.
			fail("Expected an exception because noclass shouldn't exist");
		} catch (CruiseControlException e) {
			// this is as what we expect to happen
		}
	}
}
