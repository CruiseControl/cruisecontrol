/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class TabSheetTag extends BodyTagSupport {
    private List tabs = new ArrayList();
    private Tab selectedTab;
    private static final Tab NONE_SELECTED = null;

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
    public void addTab(Tab tab) {
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
     * @throws JspException if there's an error, like an IO error.
     */
    public int doEndTag() throws JspException {
        try {
            printTabHeaders();
            printBody();
            return Tag.EVAL_PAGE;
        } catch (IOException e) {
            throw new JspTagException("IO Error: " + e.getMessage());
        }
    }

    private PageContext getPageContext() {
        return super.pageContext;
    }

    private void clearTabs() {
        selectedTab = NONE_SELECTED;
        tabs.clear();
    }

    /**
     * Print out the tab headers. The selected tab is rendered as a plain label, the other tabs are rendered as links.
     * @throws IOException  if there's an IO error.
     */
    private void printTabHeaders() throws IOException {
        final Writer out = getPageContext().getOut();
        out.write("<tr>");
        for (Iterator iterator = tabs.iterator(); iterator.hasNext();) {
            Tab tab = (Tab) iterator.next();
            out.write("<th>");
            if (tab == selectedTab) {
                out.write(tab.getLabel());
            } else {
                out.write("<a href=\"");
                out.write(getServletPath());
                out.write("?tab=");
                out.write(tab.getName());
                out.write("\">");
                out.write(tab.getLabel());
                out.write("</a>");
            }
            out.write("</th>");
        }
        out.write("</tr>");
    }

    /**
     * Print out the body of the selected tab (if any).
     * @throws IOException  if there's an IO error.
     */
    private void printBody() throws IOException {
        final Writer out = getPageContext().getOut();
        out.write("<tr>");
        if (selectedTab != NONE_SELECTED) {
            out.write("<td>");
            out.write(getBodyContent().getString());
            out.write("</td>");
        }
        out.write("</tr>");
    }

    private String getServletPath() {
        final HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();
        return request.getContextPath() + request.getServletPath();
    }

    public boolean hasTabs() {
        return !tabs.isEmpty();
    }
}
