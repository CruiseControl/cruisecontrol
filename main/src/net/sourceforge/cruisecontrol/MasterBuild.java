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
 * @author Alden Almagro (alden@thoughtworks.com)
 * @author Paul Julius (pdjulius@thoughtworks.com)
 * @author Robert Watkins
 * @author Jason Yip, jcyip@thoughtworks.com
 */
public class MasterBuild extends XmlLogger implements BuildListener {

    private static final String BUILDINFO_FILENAME = "buildcycleinfo";
    private static final String DEFAULT_MAP = "emailmap.properties";
    private static final String DEFAULT_PROPERTIES_FILENAME = "cruisecontrol.properties";
    private static final String XML_LOGGER_FILE = "log.xml";
    
    //label/modificationset/build participants
    private static String  _label;
    private static String  _labelIncrementerClassName;
    private static String  _lastGoodBuildTime;
    private static String  _lastBuildTime;
    private static boolean _lastBuildSuccessful;
    private static boolean _buildNotNecessary;
    private static String  _logDir;
    private static String  _logFile;
    private static String  _userList;

    //build iteration info
    private static long _buildInterval;
    private static boolean _isIntervalAbsolute;
    private int _cleanBuildEvery;

    //build properties
    private Properties _properties;
    private static String _propsFileName;

    //xml merge stuff
    private static Vector _auxLogFiles = new Vector();
    private static Vector _auxLogProperties = new Vector();
    private static String _today;

    //email stuff
    private String _defaultEmailSuffix;
    private String _mailhost;
    private String _returnAddress;
    private Set _buildmaster;
    private Set _notifyOnFailure;
    private static String _projectName;
    private boolean _useEmailMap;
    private String _emailmapFilename;

    //ant specific stuff
    private String _antFile;
    private String _antTarget;
    private String _cleanAntTarget;
    
    //(PENDING) Extract class to handle logging
    // static because a new instance is used to do logging
    private static boolean _debug;
    private static boolean _verbose;
    
    private boolean _mapSourceControlUsersToEmail;
    
    //build servlet info
    private String _servletURL;
    private static File _currentBuildStatusFile;

    private static boolean isCompleteTime(String time) {
        int expectedLength = 14;
        if (time.length() < expectedLength) {
            return false;
        }
        
        return true;
    }
    
    private static boolean processLastBuildArg(String lastBuild) {
        if (!isCompleteTime(lastBuild)) {
            throw new IllegalArgumentException(
             "Bad format for last build: " + lastBuild);
        }
        _lastGoodBuildTime = lastBuild;
        _lastBuildTime = lastBuild;
        return true;
    }
    
    private static boolean processLabelArg(String label) {
        _label = label;
        //(PENDING) check format of label
        return true;
    }

    private static boolean processPropertiesArg(String propFile) {
        _propsFileName = propFile;
        return true;
    }
    
    /**
     * Entry point.  Verifies that all command line arguments are correctly 
     * specified.
     */
    
    //(PENDING) Have default values for all arguments
    // Read build info first, overwrite with user specified, default values catch
    // the rest.  Add --help/--usage
    public static void main(String[] args) {
        MasterBuild mb = new MasterBuild();
        log("***** Starting automated build process *****\n");

        boolean lastBuildSpecified = false;
        boolean labelSpecified = false;
        boolean propsSpecified = false;

        for (int i = 0; i < args.length - 1; i++) {
            try {
                if (args[i].equals("-lastbuild")) {
                    lastBuildSpecified = processLastBuildArg(args[i + 1]);
                } else if (args[i].equals("-label")) {
                    labelSpecified = processLabelArg(args[i + 1]);
                } else if (args[i].equals("-properties")) {
                    propsSpecified = processPropertiesArg(args[i + 1]);
                }
            } catch (RuntimeException re) {
                re.printStackTrace();
                mb.usage();
            }
        }

        if (!labelSpecified || !lastBuildSpecified) {
            mb.readBuildInfo();
            labelSpecified = (_label != null);
            lastBuildSpecified = (_lastGoodBuildTime != null);
        }

        if (!propsSpecified) {
            if (new File(DEFAULT_PROPERTIES_FILENAME).exists()) {
                _propsFileName = DEFAULT_PROPERTIES_FILENAME;
                propsSpecified = true;
            }
        }
        
        if (lastBuildSpecified && labelSpecified && propsSpecified) {
            mb.execute();
        } else
            mb.usage();
    }

    /**
     * Serialize the label and timestamp of the last good build
     */
    private void writeBuildInfo() {
        try {
            BuildInfo info = new BuildInfo(_lastGoodBuildTime, _label);
            ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(BUILDINFO_FILENAME));
            s.writeObject(info);
            s.flush();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Deserialize the label and timestamp of the last good build.
     */
    private void readBuildInfo() {
        File infoFile = new File(BUILDINFO_FILENAME);
        log("Reading build information from : " + infoFile.getAbsolutePath());
        if (!infoFile.exists() || !infoFile.canRead()) {
            log("Cannot read build information.");
            return;
        }

        try {
            ObjectInputStream s = new ObjectInputStream(new FileInputStream(infoFile));
            BuildInfo info = (BuildInfo) s.readObject();

            if (_lastGoodBuildTime == null) {
                _lastGoodBuildTime = info.timestamp;
            }
            
            if(_lastBuildTime == null) {
                _lastBuildTime = _lastGoodBuildTime;
            }
            
            if (_label == null) {
                _label = info.label;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load properties file, see masterbuild.properties for descriptions of 
     * properties.
     */
    private void loadProperties() throws Exception {
        File propFile = new File(_propsFileName);
        
        if (!propFile.exists()) {
            throw new FileNotFoundException("Properties file \"" + propFile 
             + "\" not found");
        }
        
        Properties props = new Properties();
        props.load(new FileInputStream(propFile));
        
        StringTokenizer st = new StringTokenizer(props.getProperty("auxlogfiles"), ",");
        _auxLogProperties = new Vector();
        while (st.hasMoreTokens()) {
            String nextFile = st.nextToken().trim();
            _auxLogProperties.add(nextFile);
        }
        
        _buildInterval = Integer.parseInt(props.getProperty("buildinterval"))*1000;
        
        _debug = getBooleanProperty(props, "debug");
        _verbose = getBooleanProperty(props, "verbose");
        _mapSourceControlUsersToEmail = getBooleanProperty(props, 
         "mapSourceControlUsersToEmail");
        
        _defaultEmailSuffix = props.getProperty("defaultEmailSuffix");
        _mailhost = props.getProperty("mailhost");
        _servletURL = props.getProperty("servletURL");
        _returnAddress = props.getProperty("returnAddress");
        _buildmaster = getSetFromString(props.getProperty("buildmaster"));
        _notifyOnFailure = getSetFromString(props.getProperty("notifyOnFailure"));

        _logDir = props.getProperty("logDir"); 
        new File(_logDir).mkdirs();
        
        String buildStatusFileName = _logDir + File.separator 
         + props.getProperty("currentBuildStatusFile");
        log("Creating " + buildStatusFileName);
        _currentBuildStatusFile = new File(buildStatusFileName);
        _currentBuildStatusFile.createNewFile();
        
        _antFile = props.getProperty("antfile");
        _antTarget = props.getProperty("target");
        _cleanAntTarget = props.getProperty("cleantarget");
        _cleanBuildEvery = Integer.parseInt(props.getProperty("cleanBuildEvery"));
        _labelIncrementerClassName = props.getProperty("labelIncrementerClass");
        if (_labelIncrementerClassName == null) {
            _labelIncrementerClassName = DefaultLabelIncrementer.class.getName();
        }
        
        _emailmapFilename = props.getProperty("emailmap");
        _useEmailMap = usingEmailMap(_emailmapFilename);
        
        if (_debug || _verbose)
            props.list(System.out);
    }

    private boolean getBooleanProperty(Properties props, String key) {
        try {
            return props.getProperty(key).equals("true");
        } catch (NullPointerException npe) {
            log("Missing " + key + " property.  Using 'false'.");
            return false;
        }
    }    
    
    /**
     * This method infers from the value of the email
     * map filename, whether or not the email map is being
     * used. For example, if the filename is blank
     * or null, then the map is not being used.
     * 
     * @param emailMapFileName
     *               Name provided by the user.
     * @return true if the email map should be consulted, otherwise false.
     */
    private boolean usingEmailMap(String emailMapFileName) {
        //If the user specified name is null or blank or doesn't exist, then 
        //  the email map is not being used.
        if (emailMapFileName == null || emailMapFileName.trim().length() == 0) {
            return false;
        }
        //Otherwise, check to see if the filename provided exists and is readable.
        File userEmailMap = new File(emailMapFileName);
        return userEmailMap.exists() && userEmailMap.canRead();
    }

    /**
     * Loop infinitely, firing off the build as necessary.  Reloads the 
     * properties file every time so that the build process need not be stopped,
     * only the property file needs to be edited if changes are necessary.  Will
     * execute an alternate ant task every n builds, so that we can possibly 
     * execute a full clean build, etc.
     */
    private void execute() {
        try {
            int buildcounter = 0;
            while (true) {
                Date startTime = new Date();
                startLog();
                loadProperties();

                //Set the security manager to one which will prevent 
                // the Ant Main class from killing the VM.
                SecurityManager oldSecMgr = System.getSecurityManager();
                System.setSecurityManager(new NoExitSecurityManager());
                try {

                    Main.main(getCommandLine(buildcounter));
                } catch (ExitException ee) {
                    //Ignoring the exit exception from Main.
                } finally {
                    //Reset the SecurityManager to the old one.
                    System.setSecurityManager(oldSecMgr);
                }

                if (!_buildNotNecessary) {
                    buildcounter++;
                    Set emails = getEmails(_userList);
                    if (_lastBuildSuccessful) {
                        emailReport(emails, _projectName + " Build " + _label + " Successful");
                        incrementLabel();
                        writeBuildInfo();
                    } else {
                        emailReport(emails, _projectName + " Build Failed");
                    }
                }
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

    private long getSleepTime(Date startTime) {
        if (_isIntervalAbsolute) {
            // We need to sleep up until startTime + buildInterval.
            // Therefore, we need startTime + buildInterval - now.
            Date now = new Date();
            long sleepTime = startTime.getTime() + _buildInterval - now.getTime();
            sleepTime = (sleepTime < 0 ? 0 : sleepTime);
            return sleepTime;
        }
        else {
            return _buildInterval;
        }
    }
    
    private String[] getCommandLine(int buildCounter) {
        Vector v = new Vector();
        v.add("-DlastGoodBuildTime=" + _lastGoodBuildTime);
        v.add("-DlastBuildTime=" + _lastBuildTime);
        v.add("-Dlabel=" + _label);
        v.add("-listener");
        v.add(this.getClass().getName());

        if (_debug)
            v.add("-debug");
        if (_verbose)
            v.add("-verbose");

        v.add("-buildfile");
        v.add(_antFile);

        if (((buildCounter % _cleanBuildEvery) == 0) && _cleanAntTarget != "") {
            log("Using clean target");
            v.add(_cleanAntTarget);
        } else if (_antTarget != "") {
            log("Using normal target");
            v.add(_antTarget);
        }

        return(String[])v.toArray(new String[v.size()]);
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
        _logFile = getFinalLogFileName(antProject);
        log("Final log name is: " + _logFile);
        
        try {
            StringBuffer aggregatedXMLLog = new StringBuffer();
            aggregatedXMLLog.append(readAntBuildLog(antProject));
            
            //for each aux xml file, read and write to aggregated log
            for (Enumeration logFiles = _auxLogFiles.elements(); 
             logFiles.hasMoreElements();) {
                String nextFileName = (String) logFiles.nextElement();
                
                //Read in the entire aux log file, stripping any xml version tags.
                try {
                    String text = stripXMLVersionTags(readFile(nextFileName));
                    aggregatedXMLLog.append(text);
                } catch (FileNotFoundException fnfe) {
                    log(nextFileName + " not found. Skipping...");
                }
            }
            
            //close aggregated build log
            aggregatedXMLLog.append("<label>" + _label + "</label>");
            aggregatedXMLLog.append("<today>" + _today + "</today>");
            aggregatedXMLLog.append("</build>");

            writeFile(_logFile, aggregatedXMLLog.toString());

        } catch (Throwable ioe) {
            ioe.printStackTrace();
        }
    }
    
    /**
     * Read in the ant build log output by the XmlLogger then throw away the 
     * last </build> tag. We will add it later.
     */
    private String readAntBuildLog(Project antProject) throws IOException {
        String antBuildLog = readFile(antProject.getProperty("XmlLogger.file"));
        return antBuildLog.substring(0, antBuildLog.lastIndexOf("</build>"));
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
        if (_lastBuildSuccessful) {
            logFileName += "L" + _label;
        }
        logFileName += ".xml";
        logFileName = _logDir + File.separator + logFileName;

        return logFileName;
    }

    /**
     * Reads the file specified returning a String including
     * all the text from the file. This will include
     * special characters like carriage returns and
     * line feeds.
     *
     * @param filename Filename to read.
     * @return String containing the text of the file.
     * @exception IOException
     */
    private String readFile(String fileName) throws IOException {
        File file = new File(fileName);
        StringBuffer text = new StringBuffer();
        
        if (file.isDirectory()) {
            String[] files = retrieveXMLFiles(file);
            if (files == null) {
                return "";
            }
            
            for (int i = 0; i < files.length; i++) {
                text.append(readFile(fileName + File.separator + files[i]));
            }
            
            return text.toString();
        }
        
        log("Reading file " + file.getAbsolutePath());
        
        FileInputStream in = new FileInputStream(file);
        byte[] allBytes = new byte[in.available()];
        in.read(allBytes);
        in.close();
        in = null;
        
        return text.append(new String(allBytes)).toString();
    }    

    private String[] retrieveXMLFiles(File dir) {
        return dir.list(new FilenameFilter() {
            public boolean accept(File directory, String name) {
                return name.endsWith(".xml");
            }
        });
    }
    
    /**
     * Writes the text to the file specified.
     *
     * @param filename Filename to write to.
     * @param text     Text to write to the file.
     * @exception IOException
     */
    private void writeFile(String filename, String text) throws IOException {
        File f = new File(filename);
        log("Writing file: " + f.getAbsolutePath());
        BufferedWriter out = new BufferedWriter(new FileWriter(f));
        out.write(text);
        out.flush();
        out.close();
        out = null;
    }

    /**
     * This method removes any xml version tags from the
     * String and returns the updated String. This method
     * doesn't care whether the version tag appears
     * in a comment, or anywhere else in the text. It searches
     * for the xml version open tag,<CODE>%lt;?xml version</CODE>,
     * and deletes everything up to and including
     * the closing version tag, <CODE>?%gt;</CODE>. This method
     * also trims any whitespace left.
     *
     * @param text   Text to remove xml version tags from.
     * @return Text with all xml version tags removed.
     */
    private String stripXMLVersionTags(String text) {
        String openTag = "<?xml version";
        String closeTag = "?>";

        StringBuffer buf = new StringBuffer();

        int openTagIndex = -1;
        //As long as opening tags can be found in the text, loop through removing them.
        while ((openTagIndex = text.indexOf(openTag)) > -1) {
            //Find the next closing tag.
            int closeTagIndex = text.indexOf(closeTag, openTagIndex);

            //If no closing tag was found, then an error has occured. This isn't
            //  well formed XML.
            if (closeTagIndex < 0) {
                log("Found an xml version tag that opens"
                    +" but does not close."
                    +" This is most likely not well formed XML,"
                    +" or the xml version tag appears in a comment."
                    +" Please remove the xml version tag from any"
                    +" comments if it exists, or create well formed xml.");
            }

            //Everything before the opening tag can be appended to the new text.
            buf.append(text.substring(0, openTagIndex).trim());
            //Everything after the closing tag will now be the text. This eliminates
            //  everything between the opening tag and the ending tag.
            int indexAfterClosingTag = closeTagIndex + closeTag.length() + 1;
            if (indexAfterClosingTag <= text.length()) {
                text = text.substring(indexAfterClosingTag);
            } else {
                text = "";
            }
        }

        //Add whatever is left in the text buffer to the new text.
        buf.append(text);

        return buf.toString().trim();
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
            Class incrementerClass = Class.forName(_labelIncrementerClassName);
            LabelIncrementer incr = (LabelIncrementer)incrementerClass.newInstance();
            _label = incr.incrementLabel(_label);
        } catch (Exception e) {
            log("Error incrementing label.");
            e.printStackTrace();
        }

    }

    //(PENDING) Extract e-mail stuff into another class
    private Set getEmails(String list) {
        //The buildmaster is always included in the email names.
        Set emails = new HashSet(_buildmaster);

        //If the build failed then the failure notification emails are included.
        if (!_lastBuildSuccessful) {
            emails.addAll(_notifyOnFailure);
        }
        
        if (_mapSourceControlUsersToEmail) {
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

        String message = "View results here -> " + _servletURL + "?"
         + _logFile.substring(_logFile.lastIndexOf(File.separator) + 1, 
         _logFile.lastIndexOf("."));

        try {
            Mailer mailer = new Mailer(_mailhost, emails, _returnAddress);
            mailer.sendMessage(subject, message);
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
            } else if (_useEmailMap) {
                File emailmapFile = new File(_emailmapFilename);
                Properties emailmap = new Properties();
                try {
                    emailmap.load(new FileInputStream(emailmapFile));
                } catch (Exception e) {
                    log("error reading email map file: " + _emailmapFilename);
                    e.printStackTrace();
                }

                String mappedNames = emailmap.getProperty(nextName);
                if (mappedNames == null) {
                    if (_defaultEmailSuffix != null) {
                        nextName += _defaultEmailSuffix;
                    }
                    returnAddresses.add(nextName);
                } else {
                    returnAddresses.addAll(getSetFromString(mappedNames));
                    aliasPossible = true;
                }
            } else {
                if (_defaultEmailSuffix != null) {
                    nextName += _defaultEmailSuffix;
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
     *	convenience method for logging
     */
    private static void log(String s) {
        log(s, System.out);
    }

    /**
     *	divert logging to any printstream
     */
    private static void log(String s, PrintStream out) {
        out.println("[masterbuild] " + s);
    }

    /**
     *	Print header for each build attempt.
     */
    private void startLog() {
        log("***** Starting Build Cycle");
        log("***** Label: " + _label);
        log("***** Last Good Build: " + _lastGoodBuildTime);
        log("\n");
    }

    /**
     *	Print footer for each build attempt.
     */
    private void endLog(long sleepTime) {
        log("\n");
        log("***** Ending Build Cycle, sleeping " + (sleepTime/1000.0) + " seconds until next build.\n\n\n");
        log("***** Label: " + _label);
        log("***** Last Good Build: " + _lastGoodBuildTime);
        log("\n");
    }

    /**
     *	Print usage instructions if command line arguments are not correctly specified.
     */
    private void usage() {
        System.out.println("Usage:");
        System.out.println("");
        System.out.println("Starts a continuous integration loop");
        System.out.println("");
        System.out.println("java MasterBuild [options]");
        System.out.println("where options must include:");
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
         = new SimpleDateFormat("MM/dd/yyyy hh:mma");
        Date buildTime = new Date();
        if (!isRunning) {
            buildTime = new Date(buildTime.getTime() + _buildInterval);
        }

        try {        
            FileWriter currentBuildWriter = new FileWriter(_currentBuildStatusFile);
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
     *	Overrides method in XmlLogger.  Gets us the timestamp that we performed a "get" on
     *	our source control repository and whether or not the build was successful.  Calls the
     *	method on XmlLogger afterward.
     */
    public void buildFinished(BuildEvent buildevent) {
        logCurrentBuildStatus(false);

        Project proj = buildevent.getProject();
        _projectName = proj.getName();
        _buildNotNecessary = (proj.getProperty(ModificationSet.BUILDUNNECESSARY) != null);
        if (_buildNotNecessary) {
            return;
        }

        _today = proj.getProperty("TODAY");

        _userList = proj.getProperty(ModificationSet.USERS);

        _lastBuildSuccessful = false;

        _lastBuildTime = proj.getProperty(ModificationSet.SNAPSHOTTIMESTAMP);
        if (buildevent.getException() == null) {
            _lastBuildSuccessful = true;
            _lastGoodBuildTime = _lastBuildTime;
        }

        //get the exact filenames from the ant properties that tell us what aux xml files we have...
        _auxLogFiles = new Vector();
        for (Enumeration e = _auxLogProperties.elements(); e.hasMoreElements();) {
            String propertyName = (String)e.nextElement();
            String fileName = proj.getProperty(propertyName);
            if (fileName == null) {
                log("Auxillary Log File Property '" + propertyName + "' not set.");
            } else {
                _auxLogFiles.add(fileName);
            }            

        }

        //If the XmlLogger.file property doesn't exist, we will set it here to a default
        //  value. This will short circuit XmlLogger from setting the default value.
        String prop = proj.getProperty("XmlLogger.file");
        if (prop == null || prop.trim().length() == 0) {
            proj.setProperty("XmlLogger.file", XML_LOGGER_FILE);
        }

        super.buildFinished(buildevent);
        mergeAuxXmlFiles(proj);
    }

    /**
     *	Overrides method in XmlLogger.  writes snippet of html to disk
     *	specifying the start time of the running build, so that the build servlet can pick this up.
     */
    public void buildStarted(BuildEvent buildevent) {
        if (!canWriteXMLLoggerFile()) {
            throw new BuildException("No write access to " + XML_LOGGER_FILE);
        }

        logCurrentBuildStatus(true);
        super.buildStarted(buildevent);
    }

    boolean canWriteXMLLoggerFile() {
        File logFile = new File(XML_LOGGER_FILE);
        if (!logFile.exists() || logFile.canWrite()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Wraps the XmlLogger's method with a logging level check
     */
    public void messageLogged(BuildEvent event) {
        int logLevel = event.getPriority();
        if (false == _debug && Project.MSG_DEBUG == logLevel) {
            return;
        }
        
        if (false == _verbose && Project.MSG_VERBOSE == logLevel) {
            return;
        }
        
        super.messageLogged(event);
    }    
    
}

/**
 * Inner class to hold the build information elements
 * which will be serialized and deseralized by the
 * MasterBuild process.
 */
class BuildInfo implements Serializable {
    String timestamp;
    String label;

    BuildInfo(String timestamp, String label) {
        this.timestamp = timestamp;
        this.label = label;
    }
}
