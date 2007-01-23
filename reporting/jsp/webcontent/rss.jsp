<?xml version="1.0" encoding="UTF-8"?>
<%@ page import="
            java.io.File,
            java.text.SimpleDateFormat,
            java.util.Arrays,
            java.util.Date,
            java.util.Locale,
            net.sourceforge.cruisecontrol.BuildInfo" %>
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
<jsp:useBean id="statusHelper" scope="page" class="net.sourceforge.cruisecontrol.StatusHelper" />
<%@ page contentType="text/xml" %>

<%
    String project = null;

    // pathinfo is null when getting rss from project-status page.
    String pathinfo = request.getPathInfo();
    if(pathinfo != null) {
        project = pathinfo.substring(1);
    }

    // The publication date is in RFC 822 format which is in english
    // see http://blogs.law.harvard.edu/tech/rss
    SimpleDateFormat rfc822Format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
%>

<rss version="2.0">
<channel>
<%
    // Check if a project is selected
    if(project != null && project.length() > 0)
    {
%>
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
<pubDate><%= rfc822Format.format(date) %></pubDate>
<link><%= request.getScheme() %>://<%= request.getServerName() %>:<%= request.getServerPort() %><%= request.getContextPath() %>/buildresults/<%= project %>?log=<%= logfile %></link>
</item>
</cruisecontrol:nav>
<%
    } else {
        // Do RSS for all CruiseControl projects, one item for every project with the last build results.
        String logDirPath = application.getInitParameter("logDir");
        java.io.File logDir = new java.io.File(logDirPath);
%>
<title>CruiseControl Results</title>
<link><%= request.getScheme() %>://<%= request.getServerName() %>:<%= request.getServerPort() %><%= request.getContextPath() %></link>
<description>Summary of the project build results.</description>
<language>en-us</language>
<%
        String[] projectDirs = logDir.list(new java.io.FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (new File(dir, name).isDirectory());
            }
        });

        Arrays.sort(projectDirs);
        for (int i = 0; i < projectDirs.length; i++) {
            project = projectDirs[i];
            File projectDir = new File(logDir, project);
            statusHelper.setProjectDirectory(projectDir);
            Date lastBuildTime = statusHelper.getLastBuildTime();
            if (lastBuildTime == null) continue;
            String date   = rfc822Format.format(lastBuildTime);
            String result = statusHelper.getLastBuildResult();
            if ("failed".equalsIgnoreCase(result)) {
                result = result.toUpperCase();
            }
%>
<item>
<title><%= project %> <%= result %> <%= date %></title>
<description>Build <%= result %></description>
<pubDate><%= date %></pubDate>
<link><%= request.getScheme() %>://<%= request.getServerName() %>:<%= request.getServerPort() %><%= request.getContextPath() %>/buildresults/<%= project %></link>
</item>
<%
}
    }
%>
</channel>
</rss>
