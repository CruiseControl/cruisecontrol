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
package net.sourceforge.cruisecontrol.taglib;

import java.io.IOException;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.JspException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.mock.MockBodyContent;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletRequest;

/**
 *  Test Details... take the following set up:
 * <cruisecontrol:tabsheet>
 *   <cruisecontrol:tab name="tabname1" label="Tab Name 1">Some Text 1</cruisecontrol:tab>
 *   <cruisecontrol:tab name="tabname2" label="Tab Name 2">Some Text 2</cruisecontrol:tab>
 * </cruisecontrol:tabsheet>
 *
 * And turn it into this:
 * <tr>
 *    <th>Tab Name 1</th>
 *    <th>Tab Name 2</th>
 * </tr>
 * <tr>
 *    <td><!-- BODY OF SELECTED TAB HERE -->
 * </tr>
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net>Robert Watkins</a>
 */
public class TabSheetTagTest extends TestCase {
    private TabSheetTag tabSheet;
    private MockBodyContent content;
    private MockServletRequest request;

    public TabSheetTagTest(String name) {
        super(name);
    }

    protected void setUp() {
        tabSheet = new TabSheetTag();
        content = new MockBodyContent();
        tabSheet.setBodyContent(content);
        MockPageContext pageContext = new MockPageContext();
        request = new MockServletRequest("context", "servlet");
        pageContext.setHttpServletRequest(request);
        tabSheet.setPageContext(pageContext);
    }

    public void testClearTabsOnStart() throws IOException, JspException {
        tabSheet.addTab(new Tab("tabname1", "Tab Name 1", false));
        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag()); // side effect of this should clear
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());
        assertEquals("<tr></tr><tr></tr>", content.getString());
    }

    public void testClearTabsOnRelease() throws IOException, JspException {
        tabSheet.addTab(new Tab("tabname1", "Tab Name 1", false));
        tabSheet.release();
        // reset the content writer.
        tabSheet.setBodyContent(content);
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag()); // wouldn't normally call this after a release
                                                          // but good enough way to test.
        assertEquals("<tr></tr><tr></tr>", content.getString());
    }

    // Let's put it all together...
    public void testPrintTabSheetTab1Selected() throws JspException {
        final Tab tab1 = new Tab("tabname1", "Tab Name 1", true);
        final Tab tab2 = new Tab("tabname2", "Tab Name 2", false);
        tab1.setText("This is Tab 1");
        tab2.setText("This is Tab 2");

        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag());

        tabSheet.addTab(tab1);
        tabSheet.addTab(tab2);
        assertEquals(Tag.SKIP_BODY, tabSheet.doAfterBody());
        assertEquals("", content.getString());  // don't write out anything during afterbody
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());

        final String headerText = "<tr>"
                + "<th>Tab Name 1</th>"
                + "<th><a href=\"/context/servlet?tab=tabname2\">Tab Name 2</a></th>"
                + "</tr>";
        final String bodyText = "<tr><td>This is Tab 1</td></tr>";
        assertEquals(headerText + bodyText, content.getString());
    }

    public void testPrintTabSheetTab2Selected() throws JspException {
        final Tab tab1 = new Tab("tabname1", "Tab Name 1", false);
        final Tab tab2 = new Tab("tabname2", "Tab Name 2", true);
        tab1.setText("This is Tab 1");
        tab2.setText("This is Tab 2");

        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag());

        tabSheet.addTab(tab1);
        tabSheet.addTab(tab2);
        assertEquals(Tag.SKIP_BODY, tabSheet.doAfterBody());
        assertEquals("", content.getString());  // don't write out anything during afterbody
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());

        final String headerText = "<tr>"
                + "<th><a href=\"/context/servlet?tab=tabname1\">Tab Name 1</a></th>"
                + "<th>Tab Name 2</th>"
                + "</tr>";
        final String bodyText = "<tr><td>This is Tab 2</td></tr>";
        assertEquals(headerText + bodyText, content.getString());
    }

    public void testPrintTabSheetTabNoneSelected() throws JspException {
        final Tab tab1 = new Tab("tabname1", "Tab Name 1", false);
        final Tab tab2 = new Tab("tabname2", "Tab Name 2", false);
        tab1.setText("This is Tab 1");
        tab2.setText("This is Tab 2");

        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag());

        tabSheet.addTab(tab1);
        tabSheet.addTab(tab2);
        assertEquals(Tag.SKIP_BODY, tabSheet.doAfterBody());
        assertEquals("", content.getString());  // don't write out anything during afterbody
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());

        final String headerText = "<tr>"
                + "<th>Tab Name 1</th>"
                + "<th><a href=\"/context/servlet?tab=tabname2\">Tab Name 2</a></th>"
                + "</tr>";
        final String bodyText = "<tr><td>This is Tab 1</td></tr>";
        assertEquals(headerText + bodyText, content.getString());
    }

    public void testPrintTabSheetNoTab() throws JspException {
        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag());
        assertEquals(Tag.SKIP_BODY, tabSheet.doAfterBody());
        assertEquals("", content.getString());  // don't write out anything during afterbody
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());

        final String headerText = "<tr>"
                + "</tr>";
        final String bodyText = "<tr></tr>";
        assertEquals(headerText + bodyText, content.getString());
    }

}
