<%@page contentType="text/html" import="java.io.*,java.util.*,java.text.DateFormat,org.apache.xalan.xslt.*" %>
<html>
<%
  InitData _initData = new InitData(request);
%>
<head><title><%=_initData.title%></title></head>
<body link="#FFFFFF" vlink="#FFFFFF" background="<%=_initData.imageDir%>/bluebg.gif" 
      topmargin="0" leftmargin="0" marginheight="0" marginwidth="0">
  <table border="0" align="center" cellpadding="0" cellspacing="0" width="98%">
    <tr>
      <td>&nbsp;</td>
      <td background="<%=_initData.imageDir%>/bluestripestop.gif">
        <img src="<%=_initData.imageDir%>/blank20.gif" border="0">
      </td>
    </tr>
    <tr>
      <td width="180" valign="top">
        <img src="<%=_initData.imageDir%>/<%=_initData.logo%>" border="0"><br>
        <table border="0" align="center" width="98%">
          <tr>
            <td>
              <font face="arial" size="2" color="#FFFFFF">
<%-- We could do this if we knew the logs were in the web app.
              <jsp:include page="/logs/currentbuild.txt" flush="true" />
--%>
              <% printCurrentBuildStatus(_initData, out); %>
              <br>&nbsp;<br><b>Previous Builds:</b><br>
<%
    List logList = buildLogList(_initData);
    Iterator i = logList.iterator();
    while (i.hasNext()) {
        LogData log = (LogData)i.next();
%>
              <a href="<%=_initData.servletURL%>?<%=log.fileName%>"><%=log.dateString%><%=log.label%></a><br>
<%
    }
%>
            </td>
          </tr>
        </table>
      </td>
      <td valign="top" bgcolor="#FFFFFF" width="640">
        &nbsp;<br>
        <% transformBuildLogToHTML(_initData, out); %>
        &nbsp;<br>
      </td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td background="<%=_initData.imageDir%>/bluestripesbottom.gif">
        <img src="<%=_initData.imageDir%>/blank20.gif" border="0">
      </td>
    </tr>
  </table>
</body>
</html>

<%!
  public class InitData {
    final String logDir;
    final String xslFile;
    final String title;
    final String imageDir;
    final String logo;
    final String currentBuildStatusFile;
    final String servletURL;
    final String logFile;

    public InitData(HttpServletRequest request) {
        ServletConfig config = getServletConfig();
        logDir = config.getInitParameter("logDir");
        xslFile = config.getInitParameter("xslFile");
        title = config.getInitParameter("pageTitle");
        imageDir = config.getInitParameter("imageDir");
        logo = config.getInitParameter("logo");
        currentBuildStatusFile = config.getInitParameter("currentBuildStatusFile");
        servletURL = config.getInitParameter("servletURL");

        String possibleLogFile = request.getQueryString();
        if (possibleLogFile == null || possibleLogFile.equals("")) {
            possibleLogFile = getLastBuildLogFilename();
        }
        else {
            possibleLogFile += ".xml";
        }
        logFile = new File(logDir, possibleLogFile).getAbsolutePath();
    }

    private String getLastBuildLogFilename() {
        String logFile = "";
        File logDirFile = new File(logDir);

        String[] prevBuildLogs = logDirFile.list();
        for (int i=prevBuildLogs.length-1; i >=0; i--) {
            if (prevBuildLogs[i].startsWith("log") &&
                prevBuildLogs[i].endsWith(".xml") &&
                prevBuildLogs[i].length() > 7)
                return prevBuildLogs[i];
        }
        return null;
    }
  }
 %>

<%!
    private void printCurrentBuildStatus(InitData init, JspWriter out)
        throws FileNotFoundException, IOException {

        File buildStatusFile = new File(init.logDir + File.separator + init.currentBuildStatusFile);
        if (buildStatusFile.exists()) {
            BufferedReader br = new BufferedReader(new FileReader(buildStatusFile), 1024);
            String s = br.readLine();
            while (s != null) {
                out.println(s);
                s = br.readLine();
            }
            br.close();
            br = null;
        }
        else {
            log(init.currentBuildStatusFile + " does not exist");
        }
    }
%>

<%!
    public static class LogData {
        public final String label;
        public final String dateString;
        public final String fileName;
        public LogData(String fileName, String date, String label) {
            this.fileName = fileName;
            this.dateString = date;
            this.label = label;
        }
    }

    private List buildLogList(InitData init) throws java.text.ParseException {
        final int START_TSTAMP = 3;
        final int END_TSTAMP = 15;
        
        File logDirFile = new File(init.logDir);
        
        String[] prevBuildLogs = logDirFile.list();
        
        Arrays.sort(prevBuildLogs);
        
        List logList = new ArrayList();
        DateFormat currFormat = new java.text.SimpleDateFormat("yyyyMMddHHmm");
        DateFormat targetFormat =  DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
        for (int i = prevBuildLogs.length - 1; i >= 0; i--) {
            String currFileName = prevBuildLogs[i];
            
            if (!(currFileName.startsWith("log") && currFileName.endsWith(".xml"))) {
                continue;
            }

            String label = "";
            if (currFileName.indexOf("L") != -1) {
                label = "&nbsp;(" +
                        currFileName.substring(currFileName.indexOf("L") + 1, 
                                               currFileName.length() - 4) + ")";
            } else {
                label = "";
            }
            String timestamp = currFileName.substring(START_TSTAMP, END_TSTAMP);

            String dateString = targetFormat.format(new Date(currFormat.parse(timestamp).getTime()));
            String fileNameWithoutExt = currFileName.substring(0, currFileName.lastIndexOf("."));

            LogData data = new LogData(fileNameWithoutExt, dateString, label);
            logList.add(data);
        }
        return logList;
    }
 %>

<%!
    private void transformBuildLogToHTML(InitData init, JspWriter out) 
    throws FileNotFoundException, org.xml.sax.SAXException {
        File xmlFile = new File(init.logFile);
        if (xmlFile.exists()) {
            FileReader xml = new FileReader(xmlFile);
            FileReader xsl = new FileReader(init.xslFile);

            // Instantiate an XSLTProcessor.
            org.apache.xalan.xslt.XSLTProcessor processor = org.apache.xalan.xslt.XSLTProcessorFactory.getProcessor();

            // Create the 3 objects the XSLTProcessor needs to perform the transformation.
            XSLTInputSource xmlSource = new XSLTInputSource(xml);
            XSLTInputSource xslSheet = new XSLTInputSource(xsl);
            XSLTResultTarget xmlResult = new XSLTResultTarget(out);

            // Perform the transformation.
            processor.process(xmlSource, xslSheet, xmlResult);
        }
    }
 %>