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
<%@page import="net.sourceforge.cruisecontrol.*"%>
<%@ page import="java.io.IOException"%>
<%@ page import="java.net.InetAddress"%>
<%@ page import="java.text.DateFormat"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.io.File"%>
<%@ page import="java.util.Arrays"%>
<%@ page import="java.text.ParseException"%>
<%@ page import="java.io.BufferedReader"%>
<%@ page import="java.io.FileReader"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.Date"%>


<%
  final DateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yy HH:mm");
  final DateFormat dateOnlyFormat = new SimpleDateFormat("MM/dd/yy");
  final DateFormat timeOnlyFormat = new SimpleDateFormat("HH:mm");

  final Date now = new Date();
  final String dateNow = dateTimeFormat.format(now);
%>

<%
  class SortableStatus implements Comparable {
    private ProjectState state;
    private int sortOrder;

    public SortableStatus(ProjectState state, int sortOrder) {
      this.state = state;
      this.sortOrder = sortOrder;
    }

    public String getLabel() {
      return state != null ? state.getName() : "?";
    }

    public int getSortOrder() {
      return sortOrder;
    }

    public int compareTo(Object other) {
      SortableStatus that = (SortableStatus) other;
      return this.sortOrder - that.sortOrder;
    }
  }

  class StatusCollection {
    HashMap statuses = new HashMap();
    private SortableStatus unknown = new SortableStatus(null, -1);

    public void add(ProjectState state) {
      statuses.put(state.getDescription(), new SortableStatus(state, statuses.size()));
    }

    public SortableStatus get(String statusDescription) {
      Object status = statuses.get(statusDescription);
      if(status != null){
        return (SortableStatus) status;
      }
      return unknown;
    }
  }

%>

<%
  final StatusCollection statuses = new StatusCollection();
  statuses.add(ProjectState.PUBLISHING);
  statuses.add(ProjectState.MODIFICATIONSET);
  statuses.add(ProjectState.BUILDING);
  statuses.add(ProjectState.MERGING_LOGS);
  statuses.add(ProjectState.QUEUED);
  statuses.add(ProjectState.WAITING);
  statuses.add(ProjectState.IDLE);
  statuses.add(ProjectState.PAUSED);
  statuses.add(ProjectState.STOPPED);
%>

  <%
    String name = System.getProperty("ccname", "");
    String hostname = InetAddress.getLocalHost().getHostName();
    String port = System.getProperty("cruisecontrol.jmxport", "8000");
    boolean jmxEnabled = port != null;
    String jmxURLPrefix = "http://" + hostname+ ":"+ port + "/invoke?operation=build&objectname=CruiseControl+Project%3Aname%3D";


    String baseURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                     + request.getContextPath() + "/";
  %>


  <%
    final String statusFileName = application.getInitParameter("currentBuildStatusFile");
    class Info implements Comparable {
      public static final int ONE_DAY = 1000 * 60 * 60 * 24;

      private BuildInfo latest;
      private BuildInfo lastSuccessful;
      private SortableStatus status;
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
        BufferedReader reader = null;
        try {
          reader = new BufferedReader(new FileReader(statusFile));
          statusDescription = reader.readLine().replaceAll(" since", "");

          status = statuses.get(statusDescription);
          statusSince = new Date(statusFile.lastModified());
        }
        catch (Exception e) {
        }
        finally {
          if (reader != null) {
            reader.close();
          }
        }
      }

      public String getLastBuildTime() {
        return getTime(latest);
      }

      public String getLastSuccessfulBuildTime() {
        return getTime(lastSuccessful);
      }

      private String getTime(BuildInfo build) {
        return build != null ? format(build.getBuildDate()) : "";
      }

      public String format(Date date) {
        if (date == null) {
          return "";
        }

        if ((now.getTime() < date.getTime())) {
          return dateTimeFormat.format(date);
        }

        if ((now.getTime() - date.getTime()) < ONE_DAY) {
          return timeOnlyFormat.format(date);
        }

        return dateOnlyFormat.format(date);
      }

      public String getStatusSince() {
        return statusSince != null ? format(statusSince) : "?";
      }

      public boolean failed() {
        return latest == null || ! latest.isSuccessful();
      }

      public String getStatus() {
        return status.getLabel();
      }

      public int compareTo(Object other) {
        Info that = (Info) other;

        int order = this.status.compareTo(that.status);
        if (order != 0) {
          return order;
        }

        return (int) (this.statusSince.getTime() - that.statusSince.getTime());
      }

      public String getLabel() {
        return lastSuccessful != null ? lastSuccessful.getLabel() : " ";
      }
    }

  %>

<html>
<head>


<title><%= name%> CruiseControl at <%= hostname %> </title>

  <base href="<%=baseURL%>" />
  <link type="text/css" rel="stylesheet" href="css/cruisecontrol.css"/>
  <META HTTP-EQUIV="Refresh" CONTENT="10" URL="<%=baseURL%>">

  <style type="text/css">
    thead td {padding: 2 5}
    .data {padding: 2 5}
    .failure {color: red; font-weight: bold}
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

  <form>
    <table align="center" border="0" cellpadding="0" cellspacing="0">
<tbody>

<tr><td colspan="2">&nbsp;</td></tr>
<tr>
  <td bgcolor="#FFFFFF"><img border="0" src="images/bluestripestop.gif"/></td>
  <td align="right" bgcolor="#FFFFFF"><img border="0" src="images/bluestripestopright.gif"/></td>
</tr>


<tr><td colspan="2">
  <table class="index" width="100%">
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
    <td>Last build</td>
    <td>Failing since</td>
    <td>Label</td>
    <% if (jmxEnabled) { %>
    <td></td>
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
      <td class="data"><a href="buildresults/<%=info[i].project%>"><%=info[i].project%></a></td>
      <td class="data"><%= info[i].getStatus()%> <em>(<%= info[i].getStatusSince() %>)</em></td>
      <td class="data"><%= info[i].getLastBuildTime()%></td>
      <td class="data failure"><%= (info[i].failed()) ? info[i].getLastSuccessfulBuildTime() : "" %></td>
      <td class="data"><%= info[i].getLabel()%></td>

      <% if (jmxEnabled) { %>
      <td class="data"><input type="button"
                              onclick="callServer('<%= jmxURLPrefix + info[i].project %>', '<%=info[i].project%>')" value="Build"/></td>
      <% } %>
    </tr>

</tbody>
<%
      }
    }
  }
}
%></table>


</td></tr>
<tr>
  <td bgcolor="#FFFFFF"><img border="0" src="images/bluestripesbottom.gif"/></td>
  <td align="right" bgcolor="#FFFFFF"><img border="0" src="images/bluestripesbottomright.gif"/></td>
</tr>
<tr><td colspan="2">&nbsp;</td></tr>
</tbody>
</table>
  </form>
</body>
</html>

