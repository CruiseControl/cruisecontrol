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
    <div id="menu">
    	<ul id="menulist">
<%
    String singleProjectMode = application.getInitParameter("singleProject");
    if (Boolean.valueOf(singleProjectMode).booleanValue() == false) {  %>
            <li><a href="index">Status Page</a></li>

              <form action="index" >
                <select name="projecttarget" onchange="self.location.href = this.form.projecttarget.options[this.form.projecttarget.selectedIndex].value">
                  <cruisecontrol:projectnav>
                    <option <%=selected%> value="<%=projecturl%>"><%=linktext%></option>
                  </cruisecontrol:projectnav>
                </select>
              </form>
    <%
    } 
 %>
            <li><p><cruisecontrol:currentbuildstatus/></p></li>

            <cruisecontrol:link id="baseUrl" />
            <li><a href="<%=baseUrl%>">Latest Build</a></li>
            <cruisecontrol:nav startingBuildNumber="0" finalBuildNumber="9" >
              <li><a href="<%= url %>"><%= linktext %></a></li>
            </cruisecontrol:nav>
            <cruisecontrol:navCount startingBuildNumber="10">
                <form method="GET" action="<%=baseUrl%>" >
                  <select name="log" onchange="form.submit()">
                    <option>More builds</option>
                    <cruisecontrol:nav startingBuildNumber="10">
                      <option value="<%=logfile%>"><%= linktext %></option>
                    </cruisecontrol:nav>
                  </select>
                </form>
            </cruisecontrol:navCount>
            <li><a href="rss/<%= request.getPathInfo().substring(1) %>"><img border="0" src="images/rss.png"/></a></li>
        </ul>
    </div>
