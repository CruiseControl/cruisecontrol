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
package net.sourceforge.cruisecontrol.taglib;

import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.JspException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletRequest;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class TabTagTest extends TestCase {
    private TabTag tabTag;
    private MockServletRequest request;

    public TabTagTest(String name) {
        super(name);
    }

    public void setUp() {
        tabTag = new TabTag();
        tabTag.setName("tab1");
        tabTag.setLabel("Tab 1");

        final MockPageContext pageContext = new MockPageContext();
        tabTag.setPageContext(pageContext);
        request = new MockServletRequest();
        pageContext.setHttpServletRequest(request);
    }

    public void testTabsGetAdded() throws JspException {
        setTabNameParam("tab1");

        TestTabSheetTag tabSheet = new TestTabSheetTag("tab1", "Tab 1", true);
        tabTag.setParent(tabSheet);
        assertEquals(Tag.EVAL_BODY_INCLUDE, tabTag.doStartTag());
        assertTrue(tabSheet.tabsWereAdded);
    }

    public void testTabsGetSkippedWhenDifferent() throws JspException {
        setTabNameParam("tab2");

        TestTabSheetTag tabSheet = new TestTabSheetTag("tab1", "Tab 1", false);
        tabTag.setParent(tabSheet);
        assertEquals(Tag.SKIP_BODY, tabTag.doStartTag());
        assertTrue(tabSheet.tabsWereAdded);
    }

    public void testTabsGetAddedWhenNonePicked() throws JspException {
        TestTabSheetTag tabSheet = new TestTabSheetTag("tab1", "Tab 1", true);
        tabTag.setParent(tabSheet);
        assertEquals(Tag.EVAL_BODY_INCLUDE, tabTag.doStartTag());
        assertTrue(tabSheet.tabsWereAdded);
    }

    private void setTabNameParam(final String tabName) {
        request.addParameter("tab", tabName);
    }

    class TestTabSheetTag extends TabSheetTag {
        private final String expectedName;
        private final String expectedLabel;
        private boolean tabsWereAdded;
        private final boolean expectedSelected;

        public TestTabSheetTag(String expectedName, String expectedLabel, boolean expectedSelected) {
            this.expectedName = expectedName;
            this.expectedLabel = expectedLabel;
            this.expectedSelected = expectedSelected;
        }

        public void addTab(Tab tab) {
            tabsWereAdded = true;
            assertEquals(expectedName, tab.getName());
            assertEquals(expectedLabel, tab.getLabel());
            assertEquals(expectedSelected, tab.isSelected());
        }
    }

}
