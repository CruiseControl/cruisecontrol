/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006 Liberto Enterprises LLC.  All rights reserved.
 * This software is the property of Liberto Enterprises LLC.
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.IO;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

/**
 * This class implements the SourceControl methods for an AllFusion Harvest CM 5.1.1 repository.
 * The call to AllFusionHarvestCM511 is assumed to work without any setup. This implies
 * that the login parameters are specified in the cc configuration file.
 *
 * @author <a href="mailto:lj@libertoenterprises.com">Larry Liberto</a>
 *
 * <ul>
 *  <li>You can use this plug-in within the modificationset.
 *      <br /><font size=-2>For Example:</font>
 *      <br /><font color=blue size=-2>
 *            &#60harvest userName="myUserName" password="myPassword" brokerName="myHarvestBrokerName"
 *               projectName="myHarvestPackage"/&#62
 *            </font>
 *  </li>
 * </ul>
 * <br /><br />
 * <ul>
 *  <li><b>Required Parameters:</b><br />
 *      <ul>
 *          <li><i>userName</i> - the user name used for authentication to the AllFusion Harvest server.</li>
 *          <li><i>password</i> - the password used for authentication to the AllFusion Harvest server.</li>
 *          <li><i>brokerName</i> - the name of the AllFusion Harvest server.</li>
 *          <li><i>projectName</i> - the name of the project/repository under source control within the
 *               AllFusion Harvest server.</li>
 *      </ul>
 *  </li>
 *  <li><b>Optional Parameters:</b><br />
 *      <ul>
 *          <li><i>repositoryName</i> - the name of the project/repository under source control within the
 *                AllFusion Harvest server.<br /><font size=-2><b><u>Note</u>:</b>This parameter can be used in place
 *                of the <i><u><b>projectName</b></u></i> parameter.</font></li>
 *          <li><i>timeAdjustmentFactor</i> - a value representing the milliseconds needed to adjust any item's
 *                modification time due to time differences between the cc Server and the AllFusion Harvest server.</li>
 *          <li><i>debug</i> - <font color=red>true</font> or <font color=red>false</font> used to display
 *                additional information within the cc Server log.</li>
 *      </ul>
 *  </li>
 * </ul>
 * @see <a href="http://libertoenterprises.com/harvest.html/">libertoenterprises.com/harvest.html</a>
 */
public class AllFusionHarvestCM511 implements SourceControl {
    private static final Logger LOG = Logger.getLogger(AllFusionHarvestCM511.class);
    private static final int ITEMNAME = 0;
    private static final int COMMENT = 1;
    private static final int VERSION = 2;
    private static final int MODIFIEDTIME = 3;
    private static final int USERNAME = 4;
    private static final int EMAIL = 6;
    private static final String JAVA_DATE_FORMAT = "MM/dd/yyyy:hh:mm:ssaa";
    private static final String ORACLE_DATE_FORMAT = "MM/DD/YYYY:HH:MI:SSam";

    /**
     * Date format expected by AllFusionHarvestCM511
     */
    static final SimpleDateFormat HARVEST_DATE_FORMAT_IN = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Date format returned by AllFusionHarvestCM511 in XML output
     */
    static final SimpleDateFormat HARVEST_DATE_FORMAT_OUT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS");

    static {
        HARVEST_DATE_FORMAT_IN.setTimeZone(TimeZone.getTimeZone("GMT"));
        HARVEST_DATE_FORMAT_OUT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private Map properties = new Hashtable();
    private String property;
    private String propertyOnDelete;

    /**
     * Configuration parameters
     */
    private String userName;
    private String password;
    private String brokerName;
    private String projectName;
    private String stateName;
    private long timeAdjustmentFactor = 0;
    private boolean debug = false;

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String name) {
        this.stateName = name;
    }

    /**
     * Determines whether to display debug information.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug true or false to set the debugging level.
     */
    public void setDebug(String debug) {
        this.debug = Boolean.valueOf(debug).booleanValue();
        LOG.info("Setting debuggerOn to " + this.debug + "!");
    }

    /**
     * Retrieve the time adjustment factor used
     * to calculate the modification time of an item
     * when the AllFusion Harvest Server times are different
     * then that of the cc Server.
     *
     * @return Time in milliseconds.
     */
    public long getTimeAdjustmentFactor() {
        return timeAdjustmentFactor;
    }

    /**
     * Used to calculate the modification time of an item
     * when the AllFusion Harvest Server times are different
     * then that of the cc Server.
     *
     * @param factor Time in milliseconds.
     */
    public void setTimeAdjustmentFactor(String factor) {
        this.timeAdjustmentFactor = Long.parseLong(factor);
    }

    /**
     * The AllFusionHarvestCM511 properties.
     *
     * @return Contains all AllFusionHarvestCM511 properties.
     */
    public Map getProperties() {
        return properties;
    }

    /**
     * Set an AllFusionHarvestCM511 properties.
     *
     * @param p String representing the property to set.
     */
    public void setProperty(String p) {
        this.property = p;
    }

    /**
     * Method to set the property on delete.
     *
     * @param p String representing the property to set.
     */
    public void setPropertyOnDelete(String p) {
        this.propertyOnDelete = p;
    }

    /**
     * Sets the username for authentication.
     */
    public void setUsername(String u) {
        this.userName = u;
    }

    /**
     * Sets the password for authentication.
     */
    public void setPassword(String p) {
        this.password = p;
    }

    /**
     * Gets the broker name for authentication
     */
    public String getBrokerName() {
        return brokerName;
    }

    /**
     * Sets the broker name for authentication
     */
    public void setBrokerName(String b) {
        this.brokerName = b;
    }

    /**
     * Gets the project/repository name used to check for modifications.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Sets the project/repository name used to check for modifications.
     */
    public void setProjectName(String p) {
        this.projectName = p;
    }

    /**
     * Gets the project/repository name used to check for modifications.
     */
    public String getRepositoryName() {
        return projectName;
    }

    /**
     * Sets the project/repository name used to check for modifications.
     */
    public void setRepositoryName(String r) {
        this.projectName = r;
    }

    /**
     * @throws CruiseControlException Thrown when the brokerName, userName, password or projectName are null
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue((brokerName != null),
                "'brokerName' is a required attribute on the AllFusionHarvestCM511 task ");
        ValidationHelper.assertTrue((userName != null),
                "'userName' is a required attribute on the AllFusionHarvestCM511 task ");
        ValidationHelper.assertTrue((password != null),
                "'password' is a required attribute on the AllFusionHarvestCM511 task ");
        ValidationHelper.assertTrue((projectName != null),
                "'projectName' is a required attribute on the AllFusionHarvestCM511 task ");
    }

    /**
     * Returns a list of type <code>net.sourceforge.cruisecontrol.Modification</code> detailing all the changes between
     * the last build and the latest revision in the repository.
     *
     * @param lastBuildTime Date representing the last time the build executed.
     * @param now           Date representing the current system time (i.e. the cc server system time).
     * @return List of type <code>net.sourceforge.cruisecontrol.Modification</code>, or an empty list if we failed
     *  to retrieve the changes.
     */
    public List getModifications(Date lastBuildTime, Date now) {
        List modifications = new ArrayList();
        Commandline command;

        LOG.info("Checking for modification since " + lastBuildTime
                + " at current time of " + now + "!");

        try {
            command = buildHistoryCommand(lastBuildTime, now);
        } catch (CruiseControlException e) {
            LOG.error("Error building history command", e);

            return modifications;
        }

        try {
            modifications = execHistoryCommand(command, lastBuildTime, now);
        } catch (Exception e) {
            LOG.error("Error executing harvest hsql command " + command, e);
        }

        fillPropertiesIfNeeded(modifications);

        return modifications;
    }

    /**
     * Generates the command line used to execute the hsql command.
     *
     * @param lastBuildTime Date representing the last time the build executed.
     * @param ccSystemTime  Date representing the current system time of the cc server.
     * @return <code>net.sourceforge.cruisecontrol.util.Commandline</code> representing the command to
     * use to query harvest for changes.
     *         <br /><br />
     *         <ul>
     *         <li>
     *         For example:
     *         <br /><i>'hsql -t -b broker -usr user -pw password -f inputfile -o outputfile'</i>
     *         </li>
     *         </ul>
     */
    Commandline buildHistoryCommand(Date lastBuildTime, Date ccSystemTime)
            throws CruiseControlException {
        Commandline command = new Commandline();
        command.setExecutable("hsql");

        command.createArgument().setValue("-t");
        command.createArgument().setValue("-b");
        command.createArgument().setValue(brokerName);
        command.createArgument().setValue("-usr");
        command.createArgument().setValue(userName);
        command.createArgument().setValue("-pw");
        command.createArgument().setValue(password);

        if (isDebug()) {
            LOG.info("Executing command: " + command);
        }

        return command;
    }

    /**
     * Method used to format a date in a form that AllFusionHarvestCM511 can process.
     *
     * @param lastBuildTime
     * @return SimpleDateFormat
     */
    static String formatHarvestDate(Date lastBuildTime) {
        return HARVEST_DATE_FORMAT_IN.format(lastBuildTime);
    }

    /**
     * Executes the AllFusionHarvestCM511 command to find modifications since the last build.
     *
     * @return List of type <code>net.sourceforge.cruisecontrol.Modification</code>
     */
    private List execHistoryCommand(Commandline c, Date lastBuildTime,
                                    Date ccSystemTime) throws InterruptedException, IOException {
        FileOutputStream out = null; // declare a file output object
        PrintStream ps = null; // declare a print stream object
        File rsltsFile = null;
        File tmpSQLFile = null;
        List modifications = null;
        Process p = null;
        String cmd;

        try {
            try {
                // Create a new file output stream connected to some temp file
                tmpSQLFile = File.createTempFile("HarvestCI", ".sql");
                tmpSQLFile.deleteOnExit(); //Make sure we attempt to clean up the temp file.

                out = new FileOutputStream(tmpSQLFile);

                // Connect print stream to the output stream
                ps = new PrintStream(out);

                //Query to find all packages with modifications . . .
                String sql = sql();

                if (isDebug()) {
                    LOG.info("sql=" + sql);
                }

                ps.println(sql);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                LOG.warn("Error writing to file!  Error is  " + ioe.toString());
                throw ioe;
            } finally {
                IO.close(ps);
                IO.close(out);
            }

            //Create a temp file to house the results from the harvest command since letting the results come
            //back via command line tends to lock the system.
            rsltsFile = File.createTempFile("harvest", ".out");
            rsltsFile.deleteOnExit(); //Make sure we attempt to clean up the temp file.

            //Add the temp file to the arguments
            c.createArgument().setValue("-o " + rsltsFile.getAbsolutePath());

            //execute the harvest command.
            cmd = "hsql -t -b " + brokerName + " -usr " + userName + " -pw " + password
                    + " -f " + tmpSQLFile.getAbsolutePath() + " -o "
                    + rsltsFile.getAbsolutePath();

            if (isDebug()) {
                LOG.info("cmd=" + cmd);
            }

            p = Runtime.getRuntime().exec(cmd);

            //Log any errors.
            logErrorStream(p);

            //Wait for command to finish.
            p.waitFor();

            //Parse the results
            modifications = parseFile(rsltsFile, lastBuildTime, ccSystemTime);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn("Unable to execute command: " + c + "  Error is "
                    + e.toString());
            rethrow(e);
        } finally {
            IO.close(p.getInputStream());
            IO.close(p.getOutputStream());
            IO.close(p.getErrorStream());

            // Since WindoZ can not delete temp files try to force the deletion.
            IO.delete(tmpSQLFile, isDebug(), LOG);
            IO.delete(rsltsFile, isDebug(), LOG);
        }

        return modifications;
    }

    private static void rethrow(Exception e) throws InterruptedException, IOException {
        if (e instanceof InterruptedException) {
            throw (InterruptedException) e;
        } else if (e instanceof IOException) {
            throw (IOException) e;
        }
    }

    private String sql() {
        //TODO: See if we can just get changes to a certain state.
        return "select i.itemname, (p.packagename || ' ' || v.description),"
                + " v.MAPPEDVERSION as version, TO_CHAR(v.modifiedtime, '"
                + ORACLE_DATE_FORMAT
                + "') as MODIFIEDTIME, U.username, u.realname, u.email, TO_CHAR(sysdate, '"
                + ORACLE_DATE_FORMAT
                + "') as current_date from HARVERSIONS v, HARITEMS i, harpackage p, HARUSER u"
                + " where i.ITEMOBJID = v.ITEMOBJID and p.packageobjid = v.packageobjid and"
                + " v.MODIFIERID = u.USROBJID and P.ENVOBJID = (select ENVOBJID from HARENVIRONMENT"
                + " where ENVIRONMENTNAME = '"
                + projectName + "') order by v.MODIFIEDTIME desc";
    }

    /**
     * Logs any errors for the AllFusionHarvestCM511 command.
     *
     * @param p
     */
    private void logErrorStream(Process p) {
        StreamPumper errorPumper = new StreamPumper(p.getErrorStream(),
                new PrintWriter(System.err, true));
        new Thread(errorPumper).start();
    }

    /**
     * Parse the results from the AllFusionHarvestCM511 command.
     *
     * @param harvestRsltsFile
     * @param lastBuildTime
     * @return List of type <code>net.sourceforge.cruisecontrol.Modification</code>, or an empty list
     *  if we failed to retrieve the changes.
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    List parseFile(File harvestRsltsFile, Date lastBuildTime,
                   Date ccSystemTime) throws IOException, UnsupportedEncodingException {
        // Create a FileChannel, get the file size and map the file to a ByteBuffer
        Vector rows;
        BufferedReader fileBR;

        List modifications;

        int totalItemsCounter;
        int modifiedItemsCounter = 0;

        try {
            rows = new Vector();

            String[] row;
            fileBR = new BufferedReader(new FileReader(harvestRsltsFile));

            while (fileBR.ready()) {
                int columnIndex = 0;
                String line = fileBR.readLine();
                StringTokenizer tokenizer = new StringTokenizer(line, "\t");
                row = new String[tokenizer.countTokens()];

                while (tokenizer.hasMoreTokens()) {
                    try {
                        row[columnIndex++] = tokenizer.nextToken();
                    } catch (NumberFormatException e) {
                        System.err.println("Your file is dirty");

                        //do nice and well-documented error handling here :-)
                    }
                }

                rows.add(row);
            }

            modifications = new ArrayList();

            //Ignore the header row (row 0)
            LOG.info("Checking " + (new Integer(rows.size() - 1)).toString() + " item(s)!");
            totalItemsCounter = rows.size();

            for (int i = 1; i < rows.size(); i++) {
                String[] values = (String[]) rows.elementAt(i);

                Modification modification = new Modification("hsql - " + values[ITEMNAME]);
                modification.modifiedTime = new Date(createDateFromStringWithFormat(values[MODIFIEDTIME],
                        JAVA_DATE_FORMAT).getTime() + timeAdjustmentFactor);
                modification.userName = values[USERNAME];
                modification.emailAddress = values[EMAIL];
                modification.comment = values[COMMENT];
                modification.revision = values[VERSION];

                Modification.ModifiedFile modfile = modification.createModifiedFile(values[ITEMNAME],
                        null);
                modfile.action = "modified";
                modfile.revision = modification.revision;

                //Only Store the data in the list if the change has taken place since the last build.

                //Check to see if file has been modified since the last build.
                if (wasModifiedSinceLastBuild(modification, lastBuildTime,
                        isDebug())) {
                    modifiedItemsCounter++;
                    modifications.add(modification);
                    LOG.info(values[ITEMNAME] + " has been modified["
                            + modification.modifiedTime + "] since last build time of "
                            + lastBuildTime);
                }
            }

            LOG.info("Found " + modifiedItemsCounter + " modified item(s) out of a posible " + totalItemsCounter + "!");
        } catch (FileNotFoundException e) {
            System.err.println("Can't find that file");

            return null;
        } catch (IOException e) {
            System.err.println("IO trouble");
            e.printStackTrace();

            return null;
        } catch (Exception e) {
            System.err.println("Data trouble");
            IO.dumpTo(harvestRsltsFile, System.err);

            return null;
        }

        return modifications;
    }

    /**
     * Determines if a AllFusionHarvestCM511 Item has been modified since the last build.
     * In doing so, make sure the system dates between the AllFusionHarvestCM511 Server
     * and the cc Server are the same by compenstating for any differences
     * between these two system times.
     *
     * @param item              <code>net.sourceforge.cruisecontrol.Modification</code> object representing
     *  the item in AllFusionHarvestCM511 that has been modified.
     * @param lastBuildTime     Date representing the last time the build occured.
     * @return boolean indicating success or failure.
     */
    private static boolean wasModifiedSinceLastBuild(Modification item,
                                                     Date lastBuildTime,
                                                     boolean debugOn) {
        boolean modified;

        if (debugOn) {
            LOG.info("Checking for modifications on " + item.getFileName() + " with time="
                    + item.modifiedTime + " and last build=" + lastBuildTime);
        }
        if (lastBuildTime.before(item.modifiedTime)) {
            LOG.info("Found modifications for " + item.getFileName() + " with time="
                    + item.modifiedTime + " and last build=" + lastBuildTime);
            modified = true;
        } else {
            modified = false;
        }
        return modified;
    }

    /**
     * Fills any properties if needed.
     *
     * @param modifications List of type <code>net.sourceforge.cruisecontrol.Modification</code>
     */
    void fillPropertiesIfNeeded(List modifications) {
        if (property != null) {
            if (modifications.size() > 0) {
                properties.put(property, "true");
            }
        }

        if (propertyOnDelete != null) {
            for (int i = 0; i < modifications.size(); i++) {
                Modification modification = (Modification) modifications.get(i);
                Modification.ModifiedFile file = (Modification.ModifiedFile) modification.files.get(0);

                if (file.action.equals("deleted")) {
                    properties.put(propertyOnDelete, "true");

                    break;
                }
            }
        }
    }

    /**
     * Method used to create a date based on a string and a format
     *
     * @param dateStr       String representing the date
     * @param dateFormatStr String representing the format of the date
     */
    private static Date createDateFromStringWithFormat(String dateStr,
                                                       String dateFormatStr) {
        DateFormat myDateFormat = new SimpleDateFormat(dateFormatStr);
        Date myDate;

        try {
            myDate = myDateFormat.parse(dateStr);
        } catch (ParseException e) {
            LOG.warn("Invalid Date Parser Exception ");
            e.printStackTrace();
            myDate = new Date(); //Just to return something valid...
            //throw e;
        }

        //LOG.info("new date="+myDate);
        return myDate;
    }

    /**
     * Main method used to test the plug-in.
     *
     * @param args String[] array containing all commandline arguments.
     */
    public static void main(String[] args) {
        /*
         * hsql -t -b chgctrl1 -usr someUser -pw somePassword -f versiondata9.sql -o out.xls
         */
        AllFusionHarvestCM511 h = new AllFusionHarvestCM511();
        h.setBrokerName("myBrokerName");
        h.setUsername("myUserName");
        h.setPassword("myPassword");
        h.setProjectName("myProjectName");
        h.setStateName("myStateName");

        try {
            h.validate();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn("ERROR = " + e.toString());
        }

        try {
            h.getModifications(createDateFromStringWithFormat(
                    "06/28/2006:13:01:01", "MM/dd/yyyy:hh:mm:ss"), new Date());
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warn("ERROR = " + e.toString());
        }
        LOG.info("Done");
    }

}

