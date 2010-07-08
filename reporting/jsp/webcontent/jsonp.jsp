<%@ page import="	java.io.FilenameFilter,
					java.io.File,
					java.util.Arrays
					"
%>
<jsp:useBean id="statusHelper" scope="page" class="net.sourceforge.cruisecontrol.StatusHelper" />
<% 
    response.setContentType("text/javascript"); 
	final String logDirPath = application.getInitParameter("logDir");
	final File logDir = new File(logDirPath);
%>
<%= request.getParameter("jsonp") %>({
	"assignedLabels": [{}],
	"mode": "NORMAL",
	"nodeDescription": "CruiseControl",
	"nodeName": "<%= request.getServerName() %>",
	"jobs": [
    
<%
    final String[] projectDirs = logDir.list(new FilenameFilter() {
        public boolean accept(final File dir, final String name) {
            return (new File(dir, name).isDirectory());
        }
    });

    Arrays.sort(projectDirs);
    for (int i = 0; i < projectDirs.length; i++) {
        final String project = projectDirs[i];
        final File projectDir = new File(logDir, project);
        statusHelper.setProjectDirectory(projectDir);
        final String result = statusHelper.getLastBuildResult();
           
%>

	{
	    "name": "<%= project %>",
	    "url": "<%= request.getScheme() %>://<%= request.getServerName() %>:<%= request.getServerPort() %><%= request.getContextPath() %>/buildresults/<%= project %>",
	    "color": "<%= result %>"
	}
<% 
  	final int lastProjectId = projectDirs.length -1;
    if  (i != lastProjectId ) {  
%>
	,
<%
    }
%>

<%
}
%>
	 ],
	"primaryView": {
	  	"name": "All",
	  	"url": "<%= request.getScheme() %>://<%= request.getServerName() %>:<%= request.getServerPort() %><%= request.getContextPath() %>"
	},
	"views": [{
	    "name": "All",
	    "url": "<%= request.getScheme() %>://<%= request.getServerName() %>:<%= request.getServerPort() %><%= request.getContextPath() %>"
	}]
})


