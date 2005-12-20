<?xml version="1.0" encoding="UTF-8"?>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="net.sourceforge.cruisecontrol.BuildInfo" %>
<%--********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * This file copyright (c) 2004, Mark Doliner.
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
<%@ taglib uri="/WEB-INF/cruisecontrol-jsp11.tld" prefix="cruisecontrol"%>
<%@ page contentType="text/xml" %>

<%
    String project = request.getPathInfo().substring(1);
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US);
%>

<rss version="2.0">
<channel>
<title>CruiseControl Results - <%= project %></title>
<link><%= request.getScheme() %>://<%= request.getServerName() %>:<%= request.getServerPort() %><%= request.getContextPath() %>/buildresults/<%= project %></link>
<description>Summary of the 10 most recent builds for this project.</description>
<language>en-us</language>

<cruisecontrol:nav startingBuildNumber="0" finalBuildNumber="10">

<%
    String label = buildinfo.getLabel();
    Date date = buildinfo.getBuildDate();
%>

<item>
<% if (buildinfo.isSuccessful()) { %>
	<title><%= date %>, passed</title>
	<description>Build passed</description>
<% } else { %>
	<title><%= date %>, FAILED!</title>
	<description>Build FAILED!</description>
<% } %>
<pubDate><%= simpleDateFormat.format(date) %></pubDate>
<link><%= request.getScheme() %>://<%= request.getServerName() %>:<%= request.getServerPort() %><%= request.getContextPath() %>/buildresults/<%= project %>?log=<%= logfile %></link>
</item>
</cruisecontrol:nav>

</channel>
</rss>
