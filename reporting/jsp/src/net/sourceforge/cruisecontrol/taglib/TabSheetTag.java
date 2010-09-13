/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
import java.util.ArrayList;
import java.util.List;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import net.sourceforge.cruisecontrol.util.CCTagException;

/**
 * A sheet of navigation tabs.
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class TabSheetTag extends CruiseControlBodyTagSupport {
    private static final long serialVersionUID = -4009246347755254851L;

    private final List<Tab> tabs = new ArrayList<Tab>();
    private Tab selectedTab;
    private static final Tab NONE_SELECTED = null;
    private static final String EOL = "\r\n";
    private static final String START_SHEET = "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
            + "<tbody>" + EOL;
    private static final String END_SHEET = EOL + "</tbody></table>" + EOL;

    /**
     * On starting the tag, we clear out the per-instance state (should be clear already, but hey).
     * @return  EVAL_BODY_TAG, indicating that we should look in the body.
     */
    public int doStartTag() {
        clearTabs();
        return BodyTag.EVAL_BODY_TAG;
    }

    /**
     * Add a Tab to the list of tabs.
     * @param tab   the tab to add.
     */
    public void addTab(final Tab tab) {
        if (tab.isSelected()) {
            selectedTab = tab;
        }
        tabs.add(tab);
    }

    /** Clear up state when told to. */
    public void release() {
        super.release();
        clearTabs();
    }

    /**
     * Having finished the tag, we iterate over the tabs, printing them out correctly.
     * @return  EVAL_PAGE
     * @throws JspTagException if there's an error, like an IO error.
     */
    public int doEndTag() throws JspTagException {
        try {
            final JspWriter out = getPageContext().getOut();
            startTable(out);
            printTabHeaders(out);
            printBody(out);
            endTable(out);
            return Tag.EVAL_PAGE;
        } catch (IOException e) {
            err(e);
            throw new CCTagException("IO Error: " + e.getMessage(), e);
        }
    }

    private void endTable(final JspWriter out) throws IOException {
        out.write(END_SHEET);
    }

    private void startTable(final JspWriter out) throws IOException {
        out.write(START_SHEET);
    }

    private void clearTabs() {
        selectedTab = NONE_SELECTED;
        tabs.clear();
    }

    /**
     * Print out the tab headers. The selected tab is rendered as a plain label, the other tabs are rendered as links.
     * @param out the output writer
     * @throws IOException  if there's an IO error.
     */
    private void printTabHeaders(final JspWriter out) throws IOException {
        out.write("<tr>");
        out.write("<td bgcolor=\"#FFFFFF\">");
        out.write("<div align=\"left\">");
        out.write("<table class=\"tab-table\" align=\"center\" valign=\"middle\" cellspacing=\"0\"");
        out.write(" cellpadding=\"0\" border=\"1\"><tbody><tr>");
        for (final Tab tab : tabs) {
            if (tab.isRow()) {
                out.write("</tr><tr>");
            } else if (tab == selectedTab) {
                out.write("<td class=\"tabs-selected\">");
                out.write(tab.getLabel());
                out.write("</td>");
            } else {
                out.write("<td class=\"tabs\">");
                out.write("<a class=\"tabs-link\" href=\"");
                out.write(tab.getUrl() != null ? tab.getUrl() : createUrl("tab", tab.getName()));
                out.write("\">");
                out.write(tab.getLabel());
                out.write("</a>");
                out.write("</td>");
            }
        }
        out.write("</tr></tbody></table></div>");
        out.write("</td>");
        out.write("</tr>");
    }

    /**
     * Print out the body of the selected tab (if any).
     * @param out the output writer
     * @throws IOException  if there's an IO error.
     */
    private void printBody(final JspWriter out) throws IOException {
        if (selectedTab != NONE_SELECTED) {
            getBodyContent().writeOut(out);
        }
    }

    public boolean hasTabs() {
        return !tabs.isEmpty();
    }
}
