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

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.cglib.MockObjectTestCase;
import org.jmock.Mock;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class TabInterceptorTest extends MockObjectTestCase {
    private Mock mockTabContainer;
    private TabInterceptor tabInterceptor;
    private List tabs;
    private Mock mockTabController;
    private final TabProvider tab1 = new TabProvider(null);

    protected void setUp() throws Exception {
        super.setUp();
        mockTabContainer = mock(TabContainer.class);
        tabInterceptor = new TabInterceptor((TabContainer) mockTabContainer.proxy());
        tabs = new ArrayList();
        tabs.add(tab1);
        tabs.add(new TabProvider(null));
        mockTabController = mock(BaseTabController.class);
    }

    public void testShouldAlwaysReturnTrueForPreHandle() throws Exception {
        assertTrue(tabInterceptor.preHandle(null, null, null));
    }

    public void testShouldDoNothingForAfterCompletion() throws Exception {
        // Not really sure how to prove that nothing happens.
        // We assume we'll get a null pointer exception if you try to do something.
        try {
            tabInterceptor.afterCompletion(null, null, tabController(), null);
        } catch (Exception e) {
            fail("Should not do anything.");
        }
    }

    public void testShouldAddTabsToTheModel() throws Exception {
        expectsCalls();
        ModelAndView view = modelAndView();
        assertTrue(!view.isEmpty());
        tabInterceptor.postHandle(null, null, tabController(), view);
        assertFalse(view.isEmpty());
        assertEquals(tabs, view.getModel().get("tabs"));
    }

    public void testShouldAddCurrentTabToTheModel() throws Exception {
        expectsCalls();
        ModelAndView view = modelAndView();
        tabInterceptor.postHandle(null, null, tabController(), view);
        assertEquals(tab1, view.getModel().get("currentTab"));
    }

    public void testShouldNotAddTabInformationWhenNotUsingTabController() throws Exception {
        ModelAndView view = modelAndView();
        tabInterceptor.postHandle(null, null, new BasicController(), view);
        assertTrue(view.getModel().isEmpty());
    }

    private void expectsCalls() {
        mockTabContainer.expects(once()).method("allTabs").will(returnValue(tabs));
        mockTabController.expects(once()).method("getCurrentTab").will(returnValue(tab1));
    }

    private TabController tabController() {
        return (TabController) mockTabController.proxy();
    }

    private ModelAndView modelAndView() {
        return new ModelAndView("", new HashMap());
    }

    class BasicController implements Controller {
        public ModelAndView handleRequest(HttpServletRequest httpServletRequest,
                                          HttpServletResponse httpServletResponse) throws Exception {
            return null;
        }
    }
}
