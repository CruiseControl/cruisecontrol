package net.sourceforge.cruisecontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class has the logic to determine current build status message.
 * 
 * <br/><br/>Note the use of the <code>READ_*_LINES</code> constants for the
 * <code>maxReadLines</code> parameter:
 * <ul>
 * <li>READ_ONLY_STATUS_LINES only returns the first two lines that contain the
 * current summary build status and time.</li>
 * <li>READ_ALL_LINES returns the entire contents for the build status file.
 * This includes any output from other loggers, such as the XmlLoggerWithStatus.</li>
 * </ul>
 * You can always pass a number to get a specific number of lines.
 * 
 * @author <a href="mailto:jeffjensen@upstairstechnology.com">Jeff Jensen </a>
 */
public class BuildStatus {
    /** Constant meaning to read all lines from the build status file. */
    public static final int READ_ALL_LINES = -2;

    /**
     * Constant meaning to read only the project status and time lines from the
     * build status file.
     */
    public static final int READ_ONLY_STATUS_LINES = 2;

    protected BuildStatus() {
    }

    /**
     * Generate the current build status string formatted for plain text.
     * 
     * @param isSingleProject
     *            Specify true if this is a single project config, or false if
     *            it is a multi project config.
     * @param dir
     *            The dir containing the build status file.
     * @param projectName
     *            The name of the project to get the build status for.
     * @param statusFileName
     *            The name of the status file.
     * @param maxReadLines
     *            The maximum number of lines to read from the file. Use the
     *            READ_ALL_LINES (value of zero) and READ_ONLY_STATUS_LINES
     *            (value of 2) constants when applicable.
     * @return The build status string formatted for plain text usage or the
     *            text <code>(build status file not found)</code> if the
     *            status file cannot be not found.
     */
    public static String getStatusPlain(boolean isSingleProject, String dir,
        String projectName, String statusFileName, int maxReadLines) {
        String status = genStatus(isSingleProject, dir, projectName,
            statusFileName, false, maxReadLines);

        return status;
    }

    /**
     * Generate the current build status string formatted for HTML.
     * 
     * @param isSingleProject
     *            Specify true if this is a single project config, or false if
     *            it is a multi project config.
     * @param dir
     *            The dir containing the build status file.
     * @param projectName
     *            The name of the project to get the build status for.
     * @param statusFileName
     *            The name of the status file.
     * @param maxReadLines
     *            The maximum number of lines to read from the file. Use the
     *            READ_ALL_LINES (value of zero) and READ_ONLY_STATUS_LINES
     *            (value of 2) constants when applicable.
     * @return The build status string formatted for plain text usage or the
     *            text <code>(build status file not found)</code> if the
     *            status file cannot be not found.
     */
    public static String getStatusHtml(boolean isSingleProject, String dir,
        String projectName, String statusFileName, int maxReadLines) {
        String status = genStatus(isSingleProject, dir, projectName,
            statusFileName, true, maxReadLines);

        return status;
    }

    /**
     * Generate the current build status string.
     * 
     * @param isSingleProject
     *            Specify true if this is a single project config, or false if
     *            it is a multi project config.
     * @param dir
     *            The dir containing the build status file.
     * @param projectName
     *            The name of the project to get the build status for.
     * @param statusFileName
     *            The name of the status file.
     * @param insertBreaks
     *            If true, inserts XHTML br tag between content lines from the
     *            status file.
     * @param maxReadLines
     *            The maximum number of lines to read from the file. Use the
     *            READ_ALL_LINES (value of zero) and READ_ONLY_STATUS_LINES
     *            (value of 2) constants when applicable.
     * @return The build status string formatted as per the insertBreaks param
     *            or the text <code>(build status file not found)</code> if the
     *            status file is not found.
     */
    private static String genStatus(boolean isSingleProject, String dir,
        String projectName, String statusFileName, boolean insertBreaks,
        int maxReadLines) {

        File statusFile = getFile(isSingleProject, dir, projectName,
            statusFileName);

        String status;
        if (statusFile.exists()) {
            status = getStatus(statusFile, insertBreaks, maxReadLines);
        } else {
            status = "(build status file not found)";
        }

        return status;
    }

    /**
     * Get the status from the build status file. Need to consider a &lt;br&gt;
     * a new line to account for XmlLoggerWithStatus output.
     * 
     * @param statusFile
     *            The status file to get the status info from.
     * @param insertBreaks
     *            true to insert HTML break elements at newlines.
     * @param maxReadLines
     *            The maximum number of lines to read from the file. Use the
     *            READ_ALL_LINES (value of zero) and READ_ONLY_STATUS_LINES
     *            (value of 2) constants when applicable.
     * @return The status string from the specified status file.
     */
    private static String getStatus(File statusFile, boolean insertBreaks,
        int maxReadLines) {
        StringBuffer sb = new StringBuffer(101);
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(statusFile));
            String line = br.readLine();
            int linesRead = 1;

            while (line != null && readMoreLines(linesRead, maxReadLines)) {
              
                if (line.indexOf("<br>") == -1) {
                    addLine(line, sb, insertBreaks);                  
                } else {
                    int startIndex = 0;
                    for (int endIndex = line.indexOf("<br>");
                             endIndex != -1 && readMoreLines(linesRead, maxReadLines);
                             linesRead++) {
                        String substring = line.substring(startIndex, endIndex);
                        if (substring.length() > 0) {
                            addLine(substring, sb, insertBreaks);
                        } else {
                            linesRead--;
                        }
                        startIndex = endIndex + "<br>".length();
                        endIndex = line.indexOf("<br>", startIndex);
                    }                    
                    String substring = line.substring(startIndex);
                    if (substring.length() > 0 && readMoreLines(linesRead, maxReadLines)) {
                        addLine(substring, sb, insertBreaks);
                    }
                }

                line = br.readLine();
                linesRead++;
            }
        } catch (IOException e) {
            throw new CruiseControlWebAppException(
                "Error reading status file: " + statusFile.getName() + " : "
                    + e.getMessage(), e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                // skip action on close error
            }
            br = null;
        }

        return sb.toString();
    }

    private static boolean readMoreLines(int linesRead, int maxReadLines) {
        return linesRead <= maxReadLines || maxReadLines == READ_ALL_LINES;
    }

    private static void addLine(String line, StringBuffer sb, boolean insertBreaks) {
        sb.append(line);
        sb.append('\n');
        if (insertBreaks) {
            sb.append("<br/>");
        }
    }

    /**
     * Get the status file to read the status info from.
     * 
     * @param isSingleProject
     *            Specify true if this is a single project config, or false if
     *            it is a multi project config.
     * @param dir
     *            The dir containing the build status file.
     * @param projectName
     *            The name of the project to get the build status for.
     * @param statusFileName
     *            The name of the status file.
     * @return The status file.
     */
    private static File getFile(boolean isSingleProject, String dir,
        String projectName, String statusFileName) {
        String statusFileDir = null;

        if (isSingleProject) {
            statusFileDir = dir;
        } else {
            statusFileDir = dir + System.getProperty("file.separator")
                + projectName;
        }

        File statusFile = new File(statusFileDir, statusFileName);

        if (statusFile.isDirectory()) {
            final String msg = "CruiseControl: currentBuildStatusFile "
                + statusFile.getAbsolutePath() + " is a directory."
                + " Edit the web.xml to provide the path to the correct file.";
            throw new CruiseControlWebAppException(msg);
        }

        return statusFile;
    }
}
