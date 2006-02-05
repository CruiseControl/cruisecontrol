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
java.util.Arrays,
<%@page import="net.sourceforge.cruisecontrol.*"%>
<%@ page import="java.io.IOException"%>
<%@ page import="java.util.Calendar"%>
<%@ page import="java.net.InetAddress"%>
<%@ page import="java.text.DateFormat"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.io.File"%>
<%@ page import="java.util.Arrays"%>
<%@ page import="java.util.Comparator"%>
<%@ page import="java.text.ParseException"%>
<%@ page import="java.io.BufferedReader"%>
<%@ page import="java.io.FileReader"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Date"%>


<%
  boolean autoRefresh = "true".equals(request.getParameter("auto_refresh"));

  DateFormat dateFormat = new SimpleDateFormat("MM/dd/yy HH:mm");
  final DateFormat dateOnlyFormat = new SimpleDateFormat("MM/dd/yy");
  final DateFormat timeOnlyFormat = new SimpleDateFormat("HH:mm");

  final Date now = new Date();
  String dateNow = dateFormat.format(now);
%>


<%
  final HashMap projectStatuses = new HashMap();
  projectStatuses.put(ProjectState.QUEUED.getDescription(), ProjectState.QUEUED);
  projectStatuses.put(ProjectState.IDLE.getDescription(), ProjectState.IDLE);
  projectStatuses.put(ProjectState.MODIFICATIONSET.getDescription(), ProjectState.MODIFICATIONSET);
  projectStatuses.put(ProjectState.BUILDING.getDescription(), ProjectState.BUILDING);
  projectStatuses.put(ProjectState.MERGING_LOGS.getDescription(), ProjectState.MERGING_LOGS);
  projectStatuses.put(ProjectState.PUBLISHING.getDescription(), ProjectState.PUBLISHING);
  projectStatuses.put(ProjectState.PAUSED.getDescription(), ProjectState.PAUSED);
  projectStatuses.put(ProjectState.STOPPED.getDescription(), ProjectState.STOPPED);
  projectStatuses.put(ProjectState.WAITING.getDescription(), ProjectState.WAITING);
%>

<html>
<head>

  <%
    String name = System.getProperty("ccname", "");
    String hostname = InetAddress.getLocalHost().getHostName();
    String port = System.getProperty("cruisecontrol.jmxport", "8000");
    boolean jmxEnabled = port != null;
    String jmxURLPrefix = "http://" + hostname+ ":"+ port + "/invoke?operation=build&objectname=CruiseControl+Project%3Aname%3D";

    String baseURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                     + request.getContextPath() + "/";
  %>


  <title><%= name%> CruiseControl Status Page</title>

  <base href="<%=baseURL%>" />
  <link type="text/css" rel="stylesheet" href="css/cruisecontrol.css"/>
  <META HTTP-EQUIV="Refresh" CONTENT="10">

  <style type="text/css">
    thead td {padding: 2 5}
  </style>

  <script language="JavaScript">
    function callServer(url, projectName) {
      document.getElementById('serverData').innerHTML = '<iframe src="' + url + '" width="0" height="0" frameborder="0"></iframe>';
      alert('Scheduling build for ' + projectName);
    }

    function checkIframe(stylesheetURL) {
      if (top != self) {//We are being framed!
        //For Internet Explorer
        if(document.createStyleSheet) {
          document.createStyleSheet(stylesheetURL);
        } else { //Non-ie browsers
          var styles = "@import url('"+stylesheetURL+"');";
          var newSS=document.createElement('link');
          newSS.rel='stylesheet';
          newSS.href='data:text/css,'+escape(styles);
          document.getElementsByTagName("head")[0].appendChild(newSS);
        }
      }
    }
  </script>
</head>


<body background="images/bluebg.gif" topmargin="0" leftmargin="0" marginheight="0" marginwidth="0" onload="checkIframe('<%=baseURL + "css/cruisecontrol.css"%>')">
<p>&nbsp;</p>

<h1 class="white" align="center"><%= name%> CruiseControl at <%= hostname %> <span style="font-size: smaller; font-style: italic">[<%= dateNow %>]</span> </h1>

<div id="serverData" class="hidden" ></div>

<table align="center" border="0" cellpadding="0" cellspacing="0">
<tbody>

<tr><td colspan="2">&nbsp;</td></tr>
<tr>
  <td bgcolor="#FFFFFF"><img border="0" src="images/bluestripestop.gif"/></td>
  <td align="right" bgcolor="#FFFFFF"><img border="0" src="images/bluestripestopright.gif"/></td>
</tr>

  <%
    final String statusFileName = application.getInitParameter("currentBuildStatusFile");
    class Info implements Comparable {
      public static final int ONE_DAY = 1000 * 60 * 60 * 24;

      private BuildInfo latest;
      private BuildInfo lastSuccessful;
      private ProjectState status;
      private Date statusSince;
      private String project;
      private String statusDescription;

      public Info(File logsDir, String project) throws ParseException, IOException {
        this.project = project;

        File projectLogDir = new File(logsDir, project);
        LogFile latestLogFile = LogFile.getLatestLogFile(projectLogDir);
        LogFile latestSuccessfulLogFile = LogFile.getLatestSuccessfulLogFile(projectLogDir);


        if (latestLogFile != null) {
          latest = new BuildInfo(latestLogFile);
        }
        if (latestSuccessfulLogFile != null) {
          lastSuccessful = new BuildInfo(latestSuccessfulLogFile);
        }

        File statusFile = new File(projectLogDir, statusFileName);
        if (statusFile.exists()) {
          BufferedReader reader = new BufferedReader(new FileReader(statusFile));
          try {
            statusDescription = reader.readLine().replaceAll(" since", "");

            ProjectState projectState = (ProjectState) projectStatuses.get(statusDescription);
            status =  projectState != null ? projectState : null;
            statusSince = new Date(statusFile.lastModified());
          }
          catch(Exception e){
          }
          finally {
            reader.close();
          }
        }
      }

      public String getLastBuildTime(){
        return format(latest.getBuildDate());
      }

      public String getLastSuccessfulBuildTime(){
        return getTime(lastSuccessful);
      }

      private String getTime(BuildInfo build) {
        return build != null ? format(build.getBuildDate()) : "";
      }

      public String format(Date date) {
        if(date == null){
          return "";
        }

        if ((date.getTime() - now.getTime()) < ONE_DAY) {
          return timeOnlyFormat.format(date);
        }
        return dateOnlyFormat.format(date);
      }

      public String getStatusSince() {
        return format(statusSince);
      }

      public boolean failed() {
        return latest == null || ! latest.isSuccessful();
      }

      public String getStatus() {
        return status == null
            ? ""
            : status.getName();
      }

      public int compareTo(Object other) {
        Info that = (Info) other;

        if(this.status == null || that.status == null){
          return -1;
        }

        return (int) (that.statusSince.getTime() - this.statusSince.getTime());
      }

      public String getLabel() {
        return lastSuccessful.getLabel();
      }
    }

  %>


<tr><td colspan="2"><table class="index" width="100%">
  <%
    String statusFile = application.getInitParameter("currentBuildStatusFile");
    String singleProject = application.getInitParameter("singleProject");

    String logDirPath = application.getInitParameter("logDir");
    if (logDirPath == null) {
  %><tr><td>You need to provide a value for the context parameter <code>&quot;logDir&quot;</code></td></tr><%
} else {
  java.io.File logDir = new java.io.File(logDirPath);
  if (logDir.isDirectory() == false) {
%><tr><td>Context parameter logDir needs to be set to a directory. Currently set to &quot;<%=logDirPath%>&quot;</td></tr><%
} else {
  String[] projectDirs = logDir.list(new java.io.FilenameFilter() {
    public boolean accept(File dir, String name) {
      return (new File(dir, name).isDirectory());
    }
  });

  if (projectDirs.length == 0) {
%><tr><td>no project directories found under <%=logDirPath%></td></tr><%
}
else {
%>  <thead class="index-header">
  <tr>
    <td>Project</td>
    <td>Status <em>(since)</em></td>
    <td>Last Build</td>
    <td>Failing since</td>
    <td>Label</td>
    <% if (jmxEnabled) { %>
    <td align="center">Force build</td>
    <% } //end if jmxEnabled %>
  </tr>
</thead>


  <tbody>
    <%
      Info[] info = new Info[projectDirs.length];
      for (int i = 0; i < info.length; i++) {
        info[i] = new Info(logDir, projectDirs[i]);
      }

      Arrays.sort(info);

      for (int i = 0; i < info.length; i++) {
      %>

    <tr style="background-color: <%= (i % 2 == 1) ? "white" : "lightblue" %>  ">

      <td><a href="buildresults/<%=info[i].project%>"><%=info[i].project%></a></td>
      <td><%= info[i].getStatus()%> <em>(<%= info[i].getStatusSince() %>)</em></td>
      <td><%= info[i].getLastBuildTime()%></td>
      <td style="color: red; font-weight: bold"><%= (info[i].failed()) ? info[i].getLastSuccessfulBuildTime() : "" %></td>
      <td><%= info[i].getLabel()%></td>

      <% if (jmxEnabled) { %>
      <td><form id="force_<%=info[i].project%>" onsubmit="callServer('<%= jmxURLPrefix + info[i].project %>', '<%=info[i].project%>'); return false">
          <input type="submit" value="build" alt="run build" title="run build"/>
      </form></td>
  <!--<td><a href="javascript: callServer('<%= jmxURLPrefix + info[i].project %>', '<%=info[i].project%>')">build</a></td>-->
      <% } %>

    </tr>

</tbody>
<%
      }
    }
  }
}
%></table></td></tr>
<tr>
  <td bgcolor="#FFFFFF"><img border="0" src="images/bluestripesbottom.gif"/></td>
  <td align="right" bgcolor="#FFFFFF"><img border="0" src="images/bluestripesbottomright.gif"/></td>
</tr>
<tr><td colspan="2">&nbsp;</td></tr>
</tbody>
</table>
</body>
</html>

