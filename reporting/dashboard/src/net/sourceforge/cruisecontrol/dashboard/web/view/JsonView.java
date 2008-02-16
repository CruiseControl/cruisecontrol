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
package net.sourceforge.cruisecontrol.dashboard.web.view;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.web.servlet.view.AbstractView;

public class JsonView extends AbstractView {

    public static final String RENDER_DIRECT = " ";

    protected void renderMergedOutputModel(Map map, HttpServletRequest httpServletRequest,
                                           HttpServletResponse httpServletResponse) throws Exception {
        String json = StringUtils.replaceChars(renderJson(map), "\r\t\n", "");
        PrintWriter writer = httpServletResponse.getWriter();
        writer.write(json);
        writer.close();
    }

    public String renderJson(Map map) {
        StringBuffer sb = new StringBuffer();
        if ((map.size() == 1) && (map.containsKey(RENDER_DIRECT))) {
            renderObject(map.get(RENDER_DIRECT), sb);
        } else {
            renderMap(map, sb);
        }
        return sb.toString();
    }

    private void renderObject(Object obj, StringBuffer sb) {
        if (obj instanceof Map) {
            renderMap((Map) obj, sb);
        } else if (obj instanceof List) {
            renderList((List) obj, sb);
        } else {
            renderAsString(obj, sb);
        }
    }

    private void renderMap(Map map, StringBuffer sb) {
        sb.append("{ ");
        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            renderAsString(key, sb);
            sb.append(" : ");
            renderObject(map.get(key), sb);
            if (iter.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(" }");
    }

    private void renderList(List list, StringBuffer sb) {
        sb.append("[ ");
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            renderObject(iter.next(), sb);
            if (iter.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(" ]");
    }

    private void renderAsString(Object value, StringBuffer sb) {
        String valueStr = value == null ? "" : value.toString();
        valueStr = StringUtils.replace(valueStr, "\n", "");
        valueStr = StringUtils.replace(valueStr, "\t", "");
        valueStr = StringUtils.replace(valueStr, "\r", "");
        sb.append("\"").append(StringEscapeUtils.escapeJavaScript(valueStr)).append("\"");
    }

}
