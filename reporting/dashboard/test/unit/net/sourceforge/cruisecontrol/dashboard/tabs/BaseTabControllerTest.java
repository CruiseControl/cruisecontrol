/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.tabs;

import junit.framework.TestCase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

public class BaseTabControllerTest extends TestCase {
    public void testShouldFailIfNoTabsAddedWhenRequestingCurrentTab() throws Exception {
        try {
            new TestTabController().getCurrentTab();
            fail("Should not be able to getCurrentTab when no tabs added");
        } catch (Exception e) {
            assertEquals("No TabProvider added to this controller.", e.getMessage());
        }
    }

    public void testShouldReturnTabWhenOnlyOneTabAvailable() throws Exception {
        TestTabController controller = new TestTabController();
        TabProvider tabProvider = new TabProvider(null);
        controller.addTabProvider(tabProvider);
        assertEquals(tabProvider, controller.getCurrentTab());
    }

    public void testShouldFailIfMultipleTabsAddedWhenRequestingCurrentTab() throws Exception {
        TestTabController controller = new TestTabController();
        controller.addTabProvider(new TabProvider(null));
        controller.addTabProvider(new TabProvider(null));
        try {
            controller.getCurrentTab();
            fail("Should not be able to getCurrentTab when multiple tabs added");
        } catch (Exception e) {
            assertEquals("Multiple TabProvider added to this controller. "
                    + "Must override 'getCurrentTab' to support multiple TabProviders", e.getMessage());
        }
    }

    class TestTabController extends BaseTabController {
        protected String[] getCssFiles() {
            return new String[0];
        }

        protected ModelAndView handleTabRequest(HttpServletRequest httpServletRequest,
                                                HttpServletResponse httpServletResponse) throws Exception {
            return null;
        }
    }
}
