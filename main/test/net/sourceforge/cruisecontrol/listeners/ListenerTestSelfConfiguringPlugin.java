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
package net.sourceforge.cruisecontrol.listeners;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.ProjectXMLHelper;
import net.sourceforge.cruisecontrol.SelfConfiguringPlugin;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.jdom.Element;
import org.apache.log4j.Logger;

/**
 * Test plugin for self configuration.
 *
 * Expected config:
 * <pre>
 * &lt;plugin string='testnested' class='net.sourceforge.cruisecontrol.listeners.ListenerTestSelfConfiguringPlugin'/&gt;
 *
 * &lt;testselfconfiguring string='aaa'&gt;
 *   &lt;testnested string='bbb'/&gt;
 * &lt;/testselfconfiguring&gt;
 * </pre>
 *
 * @author jerome@coffeebreaks.org
 */
public class ListenerTestSelfConfiguringPlugin implements SelfConfiguringPlugin {
    private static final Logger LOG = Logger.getLogger(ListenerTestSelfConfiguringPlugin.class);

    private ListenerTestNestedPlugin nested;
    private String string;

    public void configure(Element element) throws CruiseControlException {
        LOG.debug("configure()");
        // FIXME check properties??
        if (element.getAttribute("string") != null) {
            setString(element.getAttribute("string").getValue());
        }
        PluginXMLHelper helper = new PluginXMLHelper(new ProjectXMLHelper());
        if (null != element.getChild("testnested")) {
            nested = (ListenerTestNestedPlugin) helper.configure(element.getChild("testnested"),
                                                                 ListenerTestNestedPlugin.class, false);
        }
    }

    public void validate() throws CruiseControlException {
        LOG.debug("validate()");
        ValidationHelper.assertIsSet(string, "string", this.getClass());
        ValidationHelper.assertIsSet(nested, "testnested", this.getClass());
    }

    public void setString(String string) {
        this.string = string.trim();
    }

    public String getString() {
        return string;
    }

    public ListenerTestNestedPlugin getNested() {
        return nested;
    }
}
