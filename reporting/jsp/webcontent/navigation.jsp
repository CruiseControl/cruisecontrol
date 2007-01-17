<%--********************************************************************************
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
 ********************************************************************************--%>
<%@page import="java.io.File, java.util.Arrays"%>
<%@ taglib uri="/WEB-INF/cruisecontrol-jsp11.tld" prefix="cruisecontrol"%>
        <img src="images/blank8.gif" border="0"/><br/>
        <a href="http://cruisecontrol.sourceforge.net" border="0"><img src="images/logo.gif" border="0"/></a><p>
        <table border="0" align="center" width="98%">
<%
    String singleProjectMode = application.getInitParameter("singleProject");
    if (Boolean.valueOf(singleProjectMode).booleanValue() == false) {  %>
            <tr><td><a class="link" href="index">Project</a></td></tr>
            <tr><td>
              <form action="index" >
                <select name="projecttarget" onchange="self.location.href = this.form.projecttarget.options[this.form.projecttarget.selectedIndex].value">
                  <cruisecontrol:projectnav>
                    <option <%=selected%> value="<%=projecturl%>"><%=linktext%></option>
                  </cruisecontrol:projectnav>
                </select>
              </form>
            </td></tr>
            <tr><td>&nbsp;</td></tr>
    <%
    } 
 %>
            <tr><td><span class="link"><cruisecontrol:currentbuildstatus/></span></td></tr>
            <tr><td>&nbsp;</td></tr>

            <cruisecontrol:link id="baseUrl" />
            <tr><td><a class="link" href="<%=baseUrl%>">Latest Build</a></td></tr>
            <cruisecontrol:nav startingBuildNumber="0" finalBuildNumber="9" >
              <tr><td><a class="link" href="<%= url %>"><%= linktext %></a></td></tr>
            </cruisecontrol:nav>
            <cruisecontrol:navCount startingBuildNumber="10">
              <tr><td>
                <form method="GET" action="<%=baseUrl%>" >
                  <select name="log" onchange="form.submit()">
                    <option>More builds</option>
                    <cruisecontrol:nav startingBuildNumber="10">
                      <option value="<%=logfile%>"><%= linktext %></option>
                    </cruisecontrol:nav>
                  </select>
                </form>
              </td></tr>
            </cruisecontrol:navCount>
            <tr><td>&nbsp;</td></tr>

            <tr><td><a href="rss/<%= request.getPathInfo().substring(1) %>"><img border="0" src="images/rss.png"/></a></td></tr>
        </table>
