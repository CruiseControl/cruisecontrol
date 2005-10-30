<%--********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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
 ********************************************************************************--%>
<%@page errorPage="/error.jsp"%>
<%@ taglib uri="/WEB-INF/cruisecontrol-jsp11.tld" prefix="cruisecontrol"%>
<%
    String project = request.getPathInfo().substring(1);
%>
<jsp:useBean id="configuration" class="net.sourceforge.cruisecontrol.Configuration" scope="page" />

<form action="ConfigurationServlet" id="<%= project %>-config" method="post">
    <input type="hidden" name="projectName" value="<%=project%>"/>
    <table xmlns="http://www.w3.org/TR/html4/strict.dtd" width="98%" border="0" cellspacing="0" cellpadding="2" align="center">
        <thead>
            <%
                String resultMsg = (String) session.getAttribute("resultMsg");
                if (resultMsg != null && resultMsg.trim().length() > 0) {
                    out.println("<tr><td class='config-resultmsg'>");
                    out.println(resultMsg);
                    out.println("</td></tr>");
                    session.removeAttribute("resultMsg");
                }
            %>
            <tr>
                <td class="config-sectionheader">
                    <label for="configurationID">Configuration</label>
                </td>
            </tr>
        </thead>
        <tbody>
            <tr><td>
                <textarea name="configuration" id="configurationID" rows="24" cols="80"><jsp:getProperty name="configuration" property="configuration" /></textarea>
            </td></tr>
        </tbody>
        <tfoot><tr><td><input type="submit" name="configure" value="Configure" /></td></tr></tfoot>
    </table>
</form>
