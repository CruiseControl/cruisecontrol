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
<%@page contentType="text/html; charset=utf-8"%>
<%@page errorPage="/error.jsp"%>
<%@ taglib uri="/WEB-INF/cruisecontrol-jsp11.tld" prefix="cruisecontrol"%>
<%
    String rmiPort = System.getProperty("cruisecontrol.rmiport");
    boolean rmiEnabled = rmiPort != null;

    String ccname = System.getProperty("ccname", "");
    String project = request.getPathInfo().substring(1);

    final Package pkg = BuildStatus.class.getPackage();
    final String ccVersionString;
    if (pkg != null && pkg.getImplementationVersion() != null) {
        ccVersionString = pkg.getImplementationVersion();
    } else {
        ccVersionString = "";
    }
%>
<html>
<head>
  <title><%= ccname%> CruiseControl Build Results</title>
  <base href="<%=request.getScheme()%>://<%=request.getServerName()%>:<%=request.getServerPort()%><%=request.getContextPath()%>/" />
  <link type="text/css" rel="stylesheet" href="css/cruisecontrol.css"/>
  <link type="application/rss+xml" rel="alternate" href="<%= request.getContextPath() %>/rss/<%= project %>" title="RSS"/>
</head>
<body>
<div class="header">
    <table border="0" cellpadding="0" cellspacing="0" width="100%">
        <tr>
            <td valign="top">
                <div class="logo"><img alt="CruiseControl" src="images/banner.png"/></div>
            </td>
            <td style="text-align:right;vertical-align:bottom">
                <div class="modifications-data" align=right align=top font=10><%=ccVersionString%></div>
            </td>
        </tr>
    </table>
</div>
<div class="container">&nbsp;
        <%@ include file="navigation.jsp" %>
    <div class="content main"> 

        <cruisecontrol:tabsheet>

              <cruisecontrol:tab name="buildResults" label="Build Results" >
                <%@ include file="buildresults.jsp" %>
              </cruisecontrol:tab>

              <cruisecontrol:tab name="testResults" label="Test Results" >
                <%@ include file="testdetails.jsp" %>
              </cruisecontrol:tab>

              <cruisecontrol:loglink id="logs_url"/>
              <cruisecontrol:tab name="log" url="<%=logs_url%>" label="XML Log File" />

              <cruisecontrol:tab name="metrics" label="Metrics" >
                <%@ include file="metrics.jsp" %>
              </cruisecontrol:tab>

              <% if (rmiEnabled) { %>
              <cruisecontrol:tab name="config" label="Config">
                <iframe src="config.jspa?project=<%= project %>" width="90%"
                    height="600" frameborder="0"></iframe>
              </cruisecontrol:tab>
              <% } %>

              <cruisecontrol:tab name="controlPanel" label="Control Panel" >
                <%@ include file="controlpanel.jsp" %>
              </cruisecontrol:tab>

<%--
              <cruisecontrol:tabrow/>
              <cruisecontrol:tab name="checkstyle" label="CheckStyle">
                <%@ include file="checkstyle.jsp" %>
              </cruisecontrol:tab>
              <cruisecontrol:tab name="pmd" label="PMD">
                <%@ include file="pmd.jsp" %>
              </cruisecontrol:tab>
--%>

        </cruisecontrol:tabsheet>
</div>
</div>
</body>
</html>
