/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.mail.*;
import org.apache.tools.ant.*;
import org.w3c.dom.*;

/**
 * Class that will run the "Master Build" -- a
 * loop over the build process so that builds can
 * be automatically run.  Extends XmlLogger so
 * this is the only listener that needs to be declared.
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author Robert Watkins
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author <a href="mailto:johnny.cass@epiuse.com">Johnny Cass</a>
 * @author <a href="mailto:davidl@iis.com">David Le Strat</a>
 */
public class MasterBuild {

    private static String BUILDINFO_FILENAME = "buildcycleinfo";
    private final String DEFAULT_EMAILMAP = "emailmap.properties";
    private final String DEFAULT_PROPERTIES_FILENAME = "cruisecontrol.properties";
    public static final String XML_LOGGER_FILE = "log.xml";
    private final String DEFAULT_BUILD_STATUS_FILENAME = "currentbuild.txt";
    private final String DEFAULT_LOG_DIR = "logs";
    private final String DEFAULT_BUILD_FILE = "build.xml";
    private final String DEFAULT_TARGET = "masterbuild";
    private final String DEFAULT_CLEAN_TARGET = "cleanbuild";

    //label/modificationset/build participants
    private String  _labelIncrementerClassName;

    //build properties
    private String _propsFileName;

    //xml merge stuff
    private Vector _auxLogFiles = new Vector();
    private String _today;

    private String _projectName;
    
    private BuildInfo info;
    private CruiseControlProperties props;

    private int _buildCounter;
    /**
     * Entry point.  Verifies that all command line arguments are correctly 
     * specified.
     */
    public static void main(String[] args) {
        MasterBuild mb = new MasterBuild();
        mb.log("***** Starting automated build process *****\n");

        mb.readBuildInfo();
        mb.overwriteWithUserArguments(args);
        mb.setDefaultPropsFileIfNecessary();

        if (mb.buildInfoSpecified()) {
            mb.execute();
        } else {
            mb.usage();
        }
    }

    // (PENDING) Extract class
    ////////////////////////////
    // BEGIN Handling arguments
    ////////////////////////////
    /**
     * Deserialize the label and timestamp of the last good build.
     */
    public void readBuildInfo() {
        info = new BuildInfo();
        info.read();
    }

    public void overwriteWithUserArguments(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            try {
                if (args[i].equals("-lastbuild")) {
                    info.setLastBuild(processLastBuildArg(args[i + 1]));
                    info.setLastGoodBuild(info.getLastBuild());
                } else if (args[i].equals("-label")) {
                    //(PENDING) check format of label
                    info.setLabel(args[i + 1]);
                } else if (args[i].equals("-properties")) {
                    _propsFileName = args[i + 1];
                }
            } catch (RuntimeException re) {
                re.printStackTrace();
                usage();
            }
        }
    }

    public void setDefaultPropsFileIfNecessary() {
        if (_propsFileName == null) {
            if (new File(DEFAULT_PROPERTIES_FILENAME).exists()) {
                _propsFileName = DEFAULT_PROPERTIES_FILENAME;
            }
        }
    }
    
    public boolean buildInfoSpecified() {
        return info.ready();
    }

    private String processLastBuildArg(String lastBuild) {
        if (!isCompleteTime(lastBuild)) {
            throw new IllegalArgumentException(
             "Bad format for last build: " + lastBuild);
        }
        return lastBuild;
    }    

    private boolean isCompleteTime(String time) {
        int expectedLength = 14;
        if (time.length() < expectedLength) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Loop infinitely, firing off the build as necessary.  Reloads the 
     * properties file every time so that the build process need not be stopped,
     * only the property file needs to be edited if changes are necessary.  Will
     * execute an alternate ant task every n builds, so that we can possibly 
     * execute a full clean build, etc.
     */
    public void execute() {
        try {
            _buildCounter = 0;
            while (true) {
                Date startTime = new Date();
                startLog();
                performBuild();
                long timeToSleep = getSleepTime(startTime);
                endLog(timeToSleep);
                Thread.sleep(timeToSleep);
            }

        } catch (InterruptedException e) {
            log("Exception trying to sleep");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performBuild() throws Exception {
        //Reload the properties file.
        props = new CruiseControlProperties(_propsFileName);

        logCurrentBuildStatus(true);

        int messageLevel = props.isDebug() ? Project.MSG_DEBUG :
                                    (props.isVerbose() ? Project.MSG_VERBOSE : Project.MSG_INFO);
        CruiseLogger logger = new CruiseLogger(messageLevel);
        
        log("Opening build file: " + props.getAntFile());
        String target = null; // remember: Null target means default target.
        if (((_buildCounter % props.getCleanBuildEvery()) == 0) 
            && props.getCleanAntTarget() != "") {

            log("Using clean target: " + props.getCleanAntTarget());
            target = props.getCleanAntTarget();

        } else if (props.getAntTarget() != "") {
            log("Using normal target: " + props.getAntTarget());
            target = props.getAntTarget();
        }
        BuildRunner runner = new BuildRunner(props.getAntFile(), target, 
                                             info.getLastGoodBuild(),
                                             info.getLabel(), logger);
        
        boolean successful = runner.runBuild();
        
        logCurrentBuildStatus(false);

        buildFinished(runner.getProject(), successful);

        //(PENDING) do this in buildFinished?
        if (info.isBuildNotNecessary()) {
            if (!info.isLastBuildSuccessful() && props.shouldSpamWhileBroken()) {
                sendBuildEmail(_projectName + "Build still failing...");
            }
        } else {
            if (info.isLastBuildSuccessful()) {
                _buildCounter++;
                if(props.shouldReportSuccess()) {
                    sendBuildEmail(_projectName + " Build " + info.getLabel() + " Successful");
                } else {
                    log("Skipping email notifications for successful builds");
                }
                incrementLabel();
            } else {
                sendBuildEmail(_projectName + "Build Failed");
            }
            info.write();
        }
        runner.reset();
    }

    private void sendBuildEmail(String message) {
        Set emails = getEmails(info.getUserList());
        emailReport(emails, message);
    }
    
    private long getSleepTime(Date startTime) {
        if (props.isIntervalAbsolute()) {
            // We need to sleep up until startTime + buildInterval.
            // Therefore, we need startTime + buildInterval - now.
            Date now = new Date();
            long sleepTime = startTime.getTime() + props.getBuildInterval() - now.getTime();
            sleepTime = (sleepTime < 0 ? 0 : sleepTime);
            return sleepTime;
        }
        else {
            return props.getBuildInterval();
        }
    }
    
    /**
     * Merge any auxiliary xml-based files into the main log xml,
     * e.g. xml from junit, modificationset, bugs, etc.
     * only copies text from aux files to the main ant log,
     * and thus assumes that your aux xml has valid syntax.
     * The auxiliary xml files are declared in the masterbuild properties
     * file.  If the auxiliary xml File is a directory, all files with extension
     * xml will be appended.
     **/
    private void mergeAuxXmlFiles(Project antProject) {
        info.setLogfile(getFinalLogFileName(antProject));
        XMLLogMerger merger = 
         new XMLLogMerger(info.getLogfile(), 
                          antProject.getProperty("XmlLogger.file"), 
                          _auxLogFiles, info.getLabel(), _today);
        try {
            merger.merge();
        } catch (IOException ioe) {
            System.err.println("Failure merging XML files: " + ioe.getMessage());
        }
    }
    
    /**
     * Returns the filename that should be used as the
     * composite log file. This method uses information
     * from previous steps in the build process, like
     * whether or not the build was successful, to determine
     * what the filename will be.
     *
     * @param proj   Project to get property values from.
     * @return Name of the composite log file.
     */
    private String getFinalLogFileName(Project proj) {
        String dateStamp = proj.getProperty("DSTAMP");
        String timeStamp = proj.getProperty("TSTAMP");
        if (dateStamp == null || timeStamp == null) {
            throw new RuntimeException("Datestamp and timestamp are not set."
                                       + " The ANT tstamp task must be called"
                                       + " before MasterBuild will work.");
        }

        String logFileName = "log" + dateStamp + timeStamp;
        if (info.isLastBuildSuccessful()) {
            logFileName += "L" + info.getLabel();
        }
        logFileName += ".xml";
        logFileName = props.getLogDir() + File.separator + logFileName;

        return logFileName;
    }

    /**
     * This method delegates to the dynamically loaded LabelIncrementer. The actual
     * implementing class can be declared in the masterbuild.properties file, or
     * the class DefaultLabelIncrementer will be used.
     *
     * @see loadProperties
     */
    private void incrementLabel() {
        //REDTAG - Paul - How explicit should we make the error messages? Is ClassCastException
        //  enough?
        try {
            Class incrementerClass = Class.forName(props.getLabelIncrementerClassName());
            LabelIncrementer incr = (LabelIncrementer)incrementerClass.newInstance();
            info.setLabel(incr.incrementLabel(info.getLabel()));
        } catch (Exception e) {
            log("Error incrementing label.");
            e.printStackTrace();
        }

    }

    //(PENDING) Extract e-mail stuff into another class
    private Set getEmails(String list) {
        //The buildmaster is always included in the email names.
        Set emails = new HashSet(props.getBuildmaster());

        //If the build failed then the failure notification emails are included.
        if (!info.isLastBuildSuccessful()) {
            emails.addAll(props.getNotifyOnFailure());
        }
        
        if (props.isMapSourceControlUsersToEmail()) {
            log("Adding source control users to e-mail list: " + list);
            emails.addAll(getSetFromString(list));
        }

        return translateAliases(emails);
    }

    private void emailReport(Set emails, String subject) {
        StringBuffer logMessage = new StringBuffer("Sending mail to:");
        for(Iterator iter = emails.iterator(); iter.hasNext();) {
            logMessage.append(" " + iter.next());
        }
        log(logMessage.toString());

        String logFile = info.getLogfile();
        String message = "View results here -> " + props.getServletURL() + "?"
         + logFile.substring(logFile.lastIndexOf(File.separator) + 1, 
         logFile.lastIndexOf("."));

        try {
            Mailer mailer = new Mailer(props.getMailhost(), props.getReturnAddress());
            mailer.sendMessage(emails, subject, message);
        } catch (javax.mail.MessagingException me) {
            System.out.println("Unable to send email.");
            me.printStackTrace();
        }
    }

    /**
     * Forms a set of unique words/names from the comma
     * delimited list provided. Maybe empty, never null.
     * 
     * @param commaDelim String containing a comma delimited list of words,
     *                   e.g. "paul,Paul, Tim, Alden,,Frank".
     * @return Set of words; maybe empty, never null.
     */
    private Set getSetFromString(String commaDelim) {
        Set elements = new TreeSet();
        if (commaDelim == null) {
            return elements;
        }

        StringTokenizer st = new StringTokenizer(commaDelim, ",");
        while (st.hasMoreTokens()) {
            String mapped = st.nextToken().trim();
            elements.add(mapped);
        }

        return elements;
    }

    private Set translateAliases(Set possibleAliases) {
        Set returnAddresses = new HashSet();
        boolean aliasPossible = false;
        for (Iterator iter = possibleAliases.iterator(); iter.hasNext();) {
            String nextName = (String) iter.next();
            if (nextName.indexOf("@") > -1) {
                //The address is already fully qualified.
                returnAddresses.add(nextName);
            } else if (props.useEmailMap()) {
                File emailmapFile = new File(props.getEmailmapFilename());
                Properties emailmap = new Properties();
                try {
                    emailmap.load(new FileInputStream(emailmapFile));
                } catch (Exception e) {
                    log("error reading email map file: " + props.getEmailmapFilename());
                    e.printStackTrace();
                }

                String mappedNames = emailmap.getProperty(nextName);
                if (mappedNames == null) {
                    if (props.getDefaultEmailSuffix() != null) {
                        nextName += props.getDefaultEmailSuffix();
                    }
                    returnAddresses.add(nextName);
                } else {
                    returnAddresses.addAll(getSetFromString(mappedNames));
                    aliasPossible = true;
                }
            } else {
                if (props.getDefaultEmailSuffix() != null) {
                    nextName += props.getDefaultEmailSuffix();
                }
                returnAddresses.add(nextName);
            }
        }
        
        if (aliasPossible) {
            returnAddresses = translateAliases(returnAddresses);
        }
        
        return returnAddresses;
    }

    /**
     *  convenience method for logging
     */
    public void log(String s) {
        log(s, System.out);
    }

    /**
     *  divert logging to any printstream
     */
    public void log(String s, PrintStream out) {
        out.println("[masterbuild] " + s);
    }

    /**
     *  Print header for each build attempt.
     */
    private void startLog() {
        log("***** Starting Build Cycle");
        log("***** Label: " + info.getLabel());
        log("***** Last Good Build: " + info.getLastGoodBuild());
        log("\n");
    }

    /**
     *  Print footer for each build attempt.
     */
    private void endLog(long sleepTime) {
        log("\n");
        log("***** Ending Build Cycle, sleeping " + (sleepTime/1000.0) + " seconds until next build.\n\n\n");
        log("***** Label: " + info.getLabel());
        log("***** Last Good Build: " + info.getLastGoodBuild());
        log("\n");
    }

    /**
     *  Print usage instructions if command line arguments are not correctly specified.
     */
    public void usage() {
        System.out.println("Usage:");
        System.out.println("");
        System.out.println("Starts a continuous integration loop");
        System.out.println("");
        System.out.println("java MasterBuild [options]");
        System.out.println("where options are:");
        System.out.println("   -lastbuild timestamp   where timestamp is in yyyyMMddHHmmss format.  note HH is the 24 hour clock.");
        System.out.println("   -label label           where label is in x.y format, y being an integer.  x can be any string.");
        System.out.println("   -properties file       where file is the masterbuild properties file, and is available in the classpath");
        System.out.println("");
        System.exit(0);
    }

    /**
     * Writes a file with a snippet of html regarding
     * whether the build is currently running or
     * when it will start again.  The build servlet
     * will then look for this file to report the
     * current build status ("build started at x" or
     * "next build at x").
     *
     * @param isRunning true if the build is currently
     *                  running, otherwise false.
     */
    private void logCurrentBuildStatus(boolean isRunning) {
        String currentlyRunning = "<br>&nbsp;<br><b>Current Build Started At:</b><br>";
        String notRunning = "<br>&nbsp;<br><b>Next Build Starts At:</b><br>";
        SimpleDateFormat numericDateFormatter 
         = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        Date buildTime = new Date();
        if (!isRunning) {
            buildTime = new Date(buildTime.getTime() + props.getBuildInterval());
        }

        try {        
            FileWriter currentBuildWriter = new FileWriter(props.getCurrentBuildStatusFile());
            currentBuildWriter.write((isRunning ? currentlyRunning : notRunning) 
             + numericDateFormatter.format(buildTime) + "<br>");
            currentBuildWriter.close();
            currentBuildWriter = null;
        } catch (IOException ioe) {
            log("Problem writing current build status");
            ioe.printStackTrace();
        }
    }

    /**
     * Overrides method in XmlLogger.  Gets us the timestamp that we performed 
     * a "get" on our source control repository and whether or not the build was 
     * successful.  Calls the method on XmlLogger afterward.
     */
    public void buildFinished(Project proj, boolean successful) {
        _projectName = proj.getName();
        info.setBuildNotNecessary(proj.getProperty(ModificationSet.BUILDUNNECESSARY) != null);
        if (info.isBuildNotNecessary()) {
            return;
        }

        _today = proj.getProperty("TODAY");

        info.setUserList(proj.getProperty(ModificationSet.USERS));

        info.setLastBuildSuccessful(false);

        info.setLastBuild(proj.getProperty(ModificationSet.SNAPSHOTTIMESTAMP));
        if (successful == true) {
            info.setLastBuildSuccessful(true);
            info.setLastGoodBuild(info.getLastBuild());
        }

        //get the exact filenames from the ant properties that tell us what aux xml files we have...
        _auxLogFiles = new Vector();
        for (Iterator e = props.getAuxLogProperties().iterator(); 
             e.hasNext();) {
            String propertyName = (String)e.next();
            String fileName = proj.getProperty(propertyName);
            if (fileName == null) {
                log("Auxillary Log File Property '" + propertyName + "' not set.");
            } else {
                _auxLogFiles.add(fileName);
            }            

        }

        mergeAuxXmlFiles(proj);
    }

    boolean canWriteXMLLoggerFile() {
        File logFile = new File(XML_LOGGER_FILE);
        if (!logFile.exists() || logFile.canWrite()) {
            return true;
        }
        
        return false;
    }
}
