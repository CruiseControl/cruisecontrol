/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit                              *
 * Copyright (C) 2001  ThoughtWorks, Inc.                                       *
 * 651 W Washington Ave. Suite 500                                              *
 * Chicago, IL 60661 USA                                                        *
 *                                                                              *
 * This program is free software; you can redistribute it and/or                *
 * modify it under the terms of the GNU General Public License                  *
 * as published by the Free Software Foundation; either version 2               *
 * of the License, or (at your option) any later version.                       *
 *                                                                              *
 * This program is distributed in the hope that it will be useful,              *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 * GNU General Public License for more details.                                 *
 *                                                                              *
 * You should have received a copy of the GNU General Public License            *
 * along with this program; if not, write to the Free Software                  *
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  *
 ********************************************************************************/

package net.sourceforge.cruisecontrol;

import java.io.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.xalan.xslt.*;

/**
 * Servlet designed to use an XSL transform to display the information
 * from the log files output by CruiseControl in a convenient manner
 * for viewing.
 * 
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 */
public class BuildServlet extends HttpServlet {

    // **** these are coming from the properties file
    private String _logDir = "";
    private String _xslFile = "";
    private String _title = "";
    private String _logo = "";
    private String _currentBuildStatusFile = "";
    private String _servletURL = "";
    private String _imageDir = "";

    private String _logFile;
    private FileReader _xml;
    private FileReader _xsl;

    protected String getLogDir() {
        return _logDir;
    }
    
    protected String getXSLFile() {
        return _xslFile;
    }
    
    protected String getPageTitle() {
        return _title;
    }
    
    protected String getLogo() {
        return _logo;
    }
    
    protected String getCurrentBuildStatusFile() {
        return _currentBuildStatusFile;
    }
    
    protected String getServletURL() {
        return _servletURL;
    }
    
    protected String getImageDir() {
        return _imageDir;
    }
    
    /**
     * Load properties file
     * @throws ServletException
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            _logDir = getServletConfig().getInitParameter("logDir");
            _xslFile = getServletConfig().getInitParameter("xslFile");
            _title = getServletConfig().getInitParameter("pageTitle");
            _imageDir = getServletConfig().getInitParameter("imageDir");
            _logo = getServletConfig().getInitParameter("logo");
            _currentBuildStatusFile = getServletConfig().getInitParameter("currentBuildStatusFile");
            _servletURL = getServletConfig().getInitParameter("servletURL");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *	print out html for a given log, or the latest if no log is specified
     *	on the query string
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) {
        try {

            _logFile = req.getQueryString();
            if (_logFile == null || _logFile.equals(""))
                _logFile = getLastBuildLogFilename();
            else
                _logFile += ".xml";
            _logFile = new File(_logDir, _logFile).getAbsolutePath();

            PrintWriter out = res.getWriter();
            res.setContentType("text/html");

            out.println("<html>");
            out.println("<head><title>" + _title + "</title></head>");
            out.println("<body link=\"#FFFFFF\" vlink=\"#FFFFFF\" background=\"" + _imageDir + "/bluebg.gif\" topmargin=\"0\" leftmargin=\"0\" marginheight=\"0\" marginwidth=\"0\">");
            out.println("<table border=\"0\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" width=\"98%\">");
            out.println("   <tr>");
            out.println("      <td>&nbsp;</td>");
            out.println("      <td background=\"" + _imageDir + "/bluestripestop.gif\"><img src=\"" + _imageDir + "/blank20.gif\" border=\"0\"></td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("      <td width=\"180\" valign=\"top\"><img src=\"" + _imageDir + "/" + _logo + "\" border=\"0\"><br><table border=\"0\" align=\"center\" width=\"98%\"><tr><td>");
            out.println("<font face=\"arial\" size=\"2\" color=\"#FFFFFF\">");


            // ***** print current build info (when did we start the running build?) *****
            printCurrentBuildStatus(out);


            out.println("<br>&nbsp;<br><b>Previous Builds:</b><br>");


            // ***** all logs for navigation *****
            printLogsAsLinks(out);


            out.println("      </td></tr></table></td>");
            out.println("      <td valign=\"top\" bgcolor=\"#FFFFFF\" width=\"640\">");
            out.println("      &nbsp;<br>");

            transformBuildLogToHTML(out);

            out.println("      &nbsp;<br>");
            out.println("   </td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("      <td>&nbsp;</td>");
            out.println("      <td background=\"" + _imageDir + "/bluestripesbottom.gif\"><img src=\"" + _imageDir + "/blank20.gif\" border=\"0\"></td>");
            out.println("   </tr>");
            out.println("</table>");

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void transformBuildLogToHTML(PrintWriter out) throws FileNotFoundException, org.xml.sax.SAXException {
        File xmlFile = new File(_logFile);
        if (xmlFile.exists()) {
            FileReader xml = new FileReader(xmlFile);
            FileReader xsl = new FileReader(_xslFile);

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

    private String getLastBuildLogFilename() {
        String logFile = "";
        File logDirFile = new File(_logDir);

        String[] prevBuildLogs = logDirFile.list();
        for (int i=prevBuildLogs.length-1; i >=0; i--) {
            if (prevBuildLogs[i].startsWith("log") &&
                prevBuildLogs[i].endsWith(".xml") &&
                prevBuildLogs[i].length() > 7)
                return prevBuildLogs[i];
        }
        return null;
    }

    /**
     *
     */
    private void printCurrentBuildStatus(PrintWriter out)
        throws FileNotFoundException, IOException {

        File buildStatusFile = new File(_logDir + File.separator + _currentBuildStatusFile);
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
            log(_currentBuildStatusFile + " does not exist");
        }
    }

    /**
     *	scans log directory for all log files, and formats file name accordingly for
     *	navigation links.
     */
    private void printLogsAsLinks(PrintWriter out) throws ParseException{
        final int START_TSTAMP = 3;
        final int END_TSTAMP = 15;
        
        File logDirFile = new File(_logDir);
        
        String[] prevBuildLogs = logDirFile.list();
        
        Arrays.sort(prevBuildLogs);
        
        for (int i = prevBuildLogs.length - 1; i >= 0; i--) {
            String currFileName = prevBuildLogs[i];
            
            if (currFileName.startsWith("log") && currFileName.endsWith(".xml")) {
                String label = "";
                if (currFileName.indexOf("L") != -1) {
                    label = "&nbsp;(" 
                     + currFileName.substring(currFileName.indexOf("L") + 1, 
                     currFileName.length() - 4) + ")";
                } else {
                    label = "";
                }
                String timestamp = currFileName.substring(START_TSTAMP, END_TSTAMP);
                SimpleDateFormat currFormat = new SimpleDateFormat("yyyyMMddHHmm");
                SimpleDateFormat targetFormat = 
                 new SimpleDateFormat ("MM/dd/yyyy HH:mm");
                
                String dateString = targetFormat.format(
                 new Date(currFormat.parse(timestamp).getTime()));
                String fileNameWithoutExt = 
                 currFileName.substring(0, currFileName.lastIndexOf("."));
                out.println("<a href=\"" + _servletURL + "?" + fileNameWithoutExt 
                 + "\">" + dateString + label + "</a><br>");
            }
        }
    }
    
}
