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
<%@page isErrorPage="true"%>
<%@page import="java.io.CharArrayWriter,
            java.io.PrintWriter,
            javax.servlet.http.HttpUtils"%>
<%
    String baseURL = request.getScheme() + "://" + request.getServerName();
    if (!request.getScheme().equals("http") || request.getServerPort() != 80) {
        baseURL += ":" + request.getServerPort();
    }
    baseURL += request.getContextPath() + "/";
    String message = exception.getMessage();
    if (message == null) {
        message = "(null)";
    }
    CharArrayWriter stackTraceWriter = new CharArrayWriter();
    exception.printStackTrace(new PrintWriter(stackTraceWriter, true));
    String stackTrace = stackTraceWriter.toString();
    application.log(HttpUtils.getRequestURL(request) + ": " + message, exception);
%>
<html>
<head>
  <title>CruiseControl Error</title>
  <base href="<%= baseURL %>" />
  <link type="text/css" rel="stylesheet" href="css/cruisecontrol.css"/>
</head>
<body background="images/bluebg.gif" topmargin="0" leftmargin="0" marginheight="0" marginwidth="0">
  <table border="0" align="center" cellpadding="0" cellspacing="0" width="98%">
    <tr>
      <td valign="top">
        <img src="images/blank8.gif" border="0"/><br/>
        <a href="http://cruisecontrol.sourceforge.net" border="0"><img src="images/logo.gif" border="0"/></a><p>
      </td>
      <td valign="top">
        &nbsp;<br/>
        <table border="0" cellpadding="0" cellspacing="0" width="100%">
          <tbody>
            <tr>
              <td bgcolor="#FFFFFF"><img border="0" src="images/bluestripestop.gif"/></td>
            </tr>
            <tr>
              <td bgcolor="white" >
                <table width="98%" border="0" cellspacing="0" cellpadding="2" align="center">
                  <tr>
                    <td class="header-title"><%= message %></td>
                  </tr>
                  <tr>
                    <td class="header-data"><pre><%= stackTrace %></pre></td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr>
              <td bgcolor="#FFFFFF"><img border="0" src="images/bluestripesbottom.gif"/></td>
            </tr>
          </tbody>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
