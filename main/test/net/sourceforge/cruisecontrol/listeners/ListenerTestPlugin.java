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
import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.apache.log4j.Logger;

/**
 * Test listener.
 *
 * This is basically a dummy test plugin.
 *
 * Register with
 * <pre>
 * &lt;plugin string='listenertest' class='net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin'/&gt;
 *
 * &lt;testlistener string='aaa'&gt;
 *   &lt;testnested string='ddd'/&gt;
 *   &lt;stringwrapper string='eee'/&gt;
 * &lt;/testlistener&gt;
 *
 * </pre>
 * @author jerome@coffeebreaks.org
 */
public class ListenerTestPlugin implements Listener {
    private static final Logger LOG = Logger.getLogger(ListenerTestPlugin.class);

    private ProjectEvent lastEvent;
    private String string;
    private String otherString;
    private StringWrapper stringWrapper;
    private ListenerTestNestedPlugin nested;

    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        lastEvent = event;
    }

    public void validate() throws CruiseControlException {
        LOG.debug("validate()");
        ValidationHelper.assertIsSet(string, "string", this.getClass());
        // ValidationHelper.assertIsSet(nested, "testnested", this.getClass());
    }

    public ProjectEvent getLastEvent() {
        return lastEvent;
    }

    public void setString(String string) {
        this.string = string.trim();
    }

    public void setOtherString(String otherString) {
        this.otherString = otherString;
    }

    public static class StringWrapper {
        private String string;

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }
    }

    /*
    public void setStringWrapper(StringWrapper stringWrapper) {
        this.stringWrapper = stringWrapper;
    }
    */

    public Object createStringWrapper() {
        stringWrapper = new StringWrapper();
        return stringWrapper;
    }

    public void add(ListenerTestNestedPlugin nestedPlugin) {
        this.nested = nestedPlugin;
    }

    public String getString() {
        return string;
    }

    public String getOtherString() {
        return otherString;
    }

    public StringWrapper getStringWrapper() {
        return stringWrapper;
    }

    public ListenerTestNestedPlugin getNested() {
        return nested;
    }
}
