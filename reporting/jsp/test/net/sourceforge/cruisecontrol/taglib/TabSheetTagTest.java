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

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

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
    private MockPageContext pageContext;
    private static final String START_SHEET = "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
            + "<tbody>\r\n"
            + "  <tr>\r\n"
            + "    <td bgcolor=\"#FFFFFF\"><img border=\"0\" src=\"images/bluestripestop.gif\"></td>\r\n"
            + "  </tr>\r\n";
    private static final String END_OF_TABLE = "  <tr>\r\n"
            + "    <td bgcolor=\"#FFFFFF\"><img border=\"0\" src=\"images/bluestripesbottom.gif\"></td>\r\n"
            + "  </tr>\r\n"
            + "</tbody></table>\r\n";
    private static final String START_OF_HEADERS = "<tr><td bgcolor=\"#FFFFFF\"><div align=\"left\"><table"
            + " class=\"tab-table\" align=\"center\" valign=\"middle\" cellspacing=\"0\""
            + " cellpadding=\"0\" border=\"1\"><tbody><tr>";
    private static final String END_OF_HEADERS = "</tr></tbody></table></div></td></tr>";
    private static final String NEW_TAB_ROW = "</tr><tr>";

    public TabSheetTagTest(String name) {
        super(name);
    }

    protected void setUp() {
        tabSheet = new TabSheetTag();
        content = new MockBodyContent();
        tabSheet.setBodyContent(content);
        pageContext = new MockPageContext();
        request = new MockServletRequest("context", "servlet");
        pageContext.setHttpServletRequest(request);
        tabSheet.setPageContext(pageContext);
    }

    public void testClearTabsOnStart() throws JspException {
        tabSheet.addTab(new Tab("tabname1", null, "Tab Name 1", false));
        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag()); // side effect of this should clear
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());
        final String expected = START_SHEET
                + START_OF_HEADERS
                + END_OF_HEADERS
                + END_OF_TABLE;
        assertEquals(expected, pageContext.getOut().toString());
    }

    public void testClearTabsOnRelease() throws JspException {
        tabSheet.addTab(new Tab("tabname1", null, "Tab Name 1", false));
        tabSheet.release();
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag()); // wouldn't normally call this after a release
                                                          // but good enough way to test.
        String expected = START_SHEET
                          + START_OF_HEADERS + END_OF_HEADERS + END_OF_TABLE;

        assertEquals(expected, pageContext.getOut().toString());
    }

    // Let's put it all together...
    public void testPrintTabSheetTab1Selected() throws JspException, IOException {
        final Tab tab1 = new Tab("tabname1", null, "Tab Name 1", true);
        final Tab tab2 = new Tab("tabname2", null, "Tab Name 2", false);
        final Tab tab3 = new Tab("tabname3", "mylink", "Tab Name 3", false);

        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag());

        tabSheet.getBodyContent().write("This is Tab 1");

        tabSheet.addTab(tab1);
        tabSheet.addTab(tab2);
        tabSheet.addTab(tab3);
        assertEquals(Tag.SKIP_BODY, tabSheet.doAfterBody());
        assertEquals("", pageContext.getOut().toString());  // don't write out anything during afterbody
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());

        String expected = START_SHEET
                          + START_OF_HEADERS
                          + "<td class=\"tabs-selected\">"
                          + "Tab Name 1</td>"
                          + "<td class=\"tabs\"><a class=\"tabs-link\" href=\"/context/servlet?tab=tabname2\">"
                          + "Tab Name 2</a></td>"
                          + "<td class=\"tabs\"><a class=\"tabs-link\" href=\"mylink\">"
                          + "Tab Name 3</a></td>"
                          + END_OF_HEADERS
                          + "This is Tab 1" + END_OF_TABLE;

        assertEquals(expected, pageContext.getOut().toString());
    }

    public void testPrintTabSheetTab2Selected() throws JspException, IOException {
        final Tab tab1 = new Tab("tabname1", null, "Tab Name 1", false);
        final Tab tab2 = new Tab("tabname2", null, "Tab Name 2", true);

        tabSheet.getBodyContent().write("This is Tab 2");

        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag());

        tabSheet.addTab(tab1);
        tabSheet.addTab(tab2);
        assertEquals(Tag.SKIP_BODY, tabSheet.doAfterBody());
        assertEquals("", pageContext.getOut().toString());  // don't write out anything during afterbody
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());

        String expected = START_SHEET
                          + START_OF_HEADERS
                          + "<td class=\"tabs\"><a class=\"tabs-link\" href=\"/context/servlet?tab=tabname1\">"
                          + "Tab Name 1</a></td>"
                          + "<td class=\"tabs-selected\">Tab Name 2</td>"
                          + END_OF_HEADERS
                          + "This is Tab 2" + END_OF_TABLE;
        assertEquals(expected, pageContext.getOut().toString());
    }

    public void testPrintTabSheetTwoRows() throws JspException, IOException {
        final Tab tab1 = new Tab("tabname1", null, "Tab Name 1", false);
        final Tab tab2 = new Tab("tabname2", null, "Tab Name 2", false, true);
        final Tab tab3 = new Tab("tabname3", null, "Tab Name 3", true);

        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag());
        tabSheet.addTab(tab1);
        tabSheet.addTab(tab2);
        tabSheet.addTab(tab3);

        String expected = START_SHEET
                          + START_OF_HEADERS
                          + "<td class=\"tabs\"><a class=\"tabs-link\" href=\"/context/servlet?tab=tabname1\">"
                          + "Tab Name 1</a></td>"
                          + NEW_TAB_ROW
                          + "<td class=\"tabs-selected\">Tab Name 3</td>"
                          + END_OF_HEADERS
                          + END_OF_TABLE;
        assertEquals(Tag.SKIP_BODY, tabSheet.doAfterBody());
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());
        assertEquals(expected, pageContext.getOut().toString());
    }

    public void testPrintTabSheetNoTab() throws JspException {
        assertEquals(BodyTag.EVAL_BODY_TAG, tabSheet.doStartTag());
        assertEquals(Tag.SKIP_BODY, tabSheet.doAfterBody());
        assertEquals("", pageContext.getOut().toString());  // don't write out anything during afterbody
        assertEquals(Tag.EVAL_PAGE, tabSheet.doEndTag());

        String expected = START_SHEET + START_OF_HEADERS + END_OF_HEADERS + END_OF_TABLE;
        assertEquals(expected, pageContext.getOut().toString());
    }

}
