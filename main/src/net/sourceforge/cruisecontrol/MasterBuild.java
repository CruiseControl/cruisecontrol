package net.sourceforge.cruisecontrol;

import java.io.*;
import java.util.*;

import javax.mail.*;
import java.text.*;
import org.apache.tools.ant.*;

/**
 * Class that will run the "Master Build" -- a
 * loop over the build process so that builds can
 * be automatically run.  Extends XmlLogger so
 * this is the only listener that needs to be declared.
 *
 * @author alden almagro (alden@thoughtworks.com), Paul Julius (pdjulius@thoughtworks.com), ThoughtWorks, Inc. 2001
 */
public class MasterBuild extends XmlLogger implements BuildListener {

    private static final String BUILDINFO_FILENAME = "buildcycleinfo";
    private static final String DEFAULT_EMAILMAP = "emailmap.properties";

    //label/modificationset/build participants
    private static String  _label;
    private static String  _labelIncrementerClassName;
    private static String  _lastGoodBuildTime;
    private static boolean _lastBuildSuccessful;
    private static boolean _buildNotNecessary;
    private static String  _logDir;
    private static String  _logFile;
    private static String  _userList;

    //build iteration info
    private static long _buildInterval;
    private int _cleanBuildEvery;

    //build properties
    private Properties _properties;
    private static String _propfilename;

    //xml merge stuff
    private static Vector _auxLogFiles = new Vector();
    private static Vector _auxLogProperties = new Vector();
    private static String _today;

    //email stuff
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
    private boolean _debug = false;
    private boolean _verbose = false;

    //build servlet info
    private String _servletURL;
    private static String _currentBuildStatusFile;

    /**
     *	entry point.  verifies that all command line arguments are correctly specified.
     */
    public static void main(String[] args) {

        MasterBuild mb = new MasterBuild();
        log("***** Starting automated build process *****\n");
        boolean lastBuildSpecified = false;
        boolean labelSpecified = false;
        boolean propsSpecified = false;

        for (int i=0; i<args.length - 1; i++) {

            if (args[i].equals("-lastbuild")) {
                try {
                    _lastGoodBuildTime = args[i+1];
                    lastBuildSpecified = true;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    log("The last successful build time must be specified.");
                    mb.usage();
                }
            }

            if (args[i].equals("-label")) {
                try {
                    _label = args[i+1];
                    labelSpecified = true;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    log("The next label must be specified");
                    mb.usage();
                }
            }

            if (args[i].equals("-properties")) {
                try {
                    _propfilename = args[i+1];
                    propsSpecified = true;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    log("The masterbuild properties file must be specified.");
                    mb.usage();
                }
            }
        }

        if (!labelSpecified || !lastBuildSpecified) {
            mb.readBuildInfo();
            labelSpecified = (_label != null);
            lastBuildSpecified = (_lastGoodBuildTime != null);
        }

        if (lastBuildSpecified && labelSpecified && propsSpecified) {
            mb.execute();
        } else
            mb.usage();
    }

    /**
     * serialize the label and timestamp of the last good build
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
     * deserialize the label and timestamp of the last good build.
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
            if (_label == null) {
                _label = info.label;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load properties file, see masterbuild.properties for descriptions of properties.
     */
    private void loadProperties() {
        File propFile = new File(_propfilename);

        try {
            Properties props = new Properties();
            props.load(new FileInputStream(propFile));

            StringTokenizer st = new StringTokenizer(props.getProperty("auxlogfiles"), ",");
            _auxLogProperties = new Vector();
            while (st.hasMoreTokens()) {
                String nextFile = st.nextToken().trim();
                _auxLogProperties.add(nextFile);
            }

            _buildInterval = Integer.parseInt(props.getProperty("buildinterval"))*1000;
            _debug = props.getProperty("debug").equals("true");
            _verbose = props.getProperty("verbose").equals("true");
            _mailhost = props.getProperty("mailhost");
            _servletURL = props.getProperty("servletURL");
            _returnAddress = props.getProperty("returnAddress");
            _buildmaster = getSetFromString(props.getProperty("buildmaster"));
            _notifyOnFailure = getSetFromString(props.getProperty("notifyOnFailure"));
            _currentBuildStatusFile = props.getProperty("currentBuildStatusFile");
            _antFile = props.getProperty("antfile");
            _antTarget = props.getProperty("target");
            _cleanAntTarget = props.getProperty("cleantarget");
            _logDir = props.getProperty("logdir");
            _cleanBuildEvery = Integer.parseInt(props.getProperty("cleanBuildEvery"));
            _labelIncrementerClassName = props.getProperty("labelIncrementerClass");
            if (_labelIncrementerClassName == null) {
                _labelIncrementerClassName = DefaultLabelIncrementer.class.getName();
            }

            _emailmapFilename = props.getProperty("emailmap");
            _useEmailMap = usingEmailMap(_emailmapFilename);

            if (_debug || _verbose)
                props.list(System.out);

        } catch (Exception e) {
            log("Properties file: " + propFile.getAbsolutePath() + " not found.");
            e.printStackTrace();
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
     * loop infinitely, firing off the build as necessary.  reloads the properties file every time so that the build process need not be stopped, only the property
     *	file needs to be edited if changes are necessary.  will execute an alternate ant task every n builds, so that we can possibly execute a full clean build, etc.
     */
    private void execute() {
        try {
            int buildcounter = 0;
            while (true) {
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

                if (_buildNotNecessary) {
                    log("Sleeping for " + (_buildInterval/1000.0)
                        + " seconds.");
                    Thread.sleep(_buildInterval);
                } else {
                    buildcounter++;
                    Set emails = getEmails(_userList);
                    if (_lastBuildSuccessful) {
                        emailReport(emails, _projectName + " Build " + _label + " Successful");
                        incrementLabel();
                        writeBuildInfo();
                    } else {
                        emailReport(emails, _projectName + " Build Failed");
                        log("Sleeping for " + (_buildInterval/1000.0)
                            + " seconds.");
                        Thread.sleep(_buildInterval);
                    }
                }

                endLog();
            }

        } catch (InterruptedException e) {
            System.out.println("[masterbuild] exception trying to sleep");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] getCommandLine(int buildCounter) {
        Vector v = new Vector();
        v.add("-DlastGoodBuildTime=" + _lastGoodBuildTime);
        v.add("-Dlabel=" + _label);
        v.add("-listener");
        v.add(this.getClass().getName());

        if (_debug)
            v.add("-debug");
        if (_verbose)
            v.add("-verbose");

        v.add("-buildfile");
        v.add(_antFile);

        if ((buildCounter % _cleanBuildEvery) == 0)
            v.add(_cleanAntTarget);
        else
            v.add(_antTarget);


        return(String[])v.toArray(new String[v.size()]);
    }

    /**
     * Merge any auxiliary xml-based files into the main log xml,
     * e.g. xml from junit, modificationset, bugs, etc.
     * only copies text from aux files to the main ant log,
     * and thus assumes that your aux xml has valid syntax.
     * The auxiliary xml files are declared in the masterbuild properties
     * file.
     */
    private void mergeAuxXmlFiles(Project proj) {
        _logFile = getFinalLogFileName(proj);

        try {
            StringBuffer xml = new StringBuffer();
            //read in the ant build log output by the XmlLogger then throw away the last
            //  </build> tag. We will add it later.
            String entireLog = readFile(proj.getProperty("XmlLogger.file"));
            xml.append(entireLog.substring(0, entireLog.lastIndexOf("</build>")));

            //for each aux xml file, read and write to ant buildlog
            for (Enumeration logFiles = _auxLogFiles.elements(); logFiles.hasMoreElements();) {
                String nextFile = (String)logFiles.nextElement();

                //Read in the entire aux log file, stripping any xml version tags.
                String text = stripXMLVersionTags(readFile(nextFile));

                //Add the text to the composite xml file.
                xml.append(text);
            }
            //close ant buildlog
            xml.append("<label>" + _label + "</label>");
            xml.append("<today>" + _today + "</today>");
            xml.append("</build>");

            //write appended buildlog
            writeFile(_logFile, xml.toString());

        } catch (Throwable ioe) {
            ioe.printStackTrace();
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

        String logFile = "log" + dateStamp + timeStamp;
        if (_lastBuildSuccessful) {
            logFile += "L" + _label;
        }
        logFile += ".xml";
        logFile = _logDir + File.separator + logFile;

        return logFile;
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
    private String readFile(String filename) throws IOException {
        File f = new File(filename);
        log("Reading file " + f.getAbsolutePath());
        FileInputStream in = new FileInputStream(f);
        byte[] allBytes = new byte[in.available()];
        in.read(allBytes);
        in.close();
        in = null;
        String text = new String(allBytes);
        return text;
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
            System.err.println("[masterbuild] Error incrementing label.");
            e.printStackTrace();
        }

    }

    private Set getEmails(String list) {
        //The buildmaster is always included in the email names.
        Set emails = new HashSet(_buildmaster);
        if (_debug) {
            log("List of emails is: " + list);
        }
        

        //If the build failed then the failure notification emails are included.
        if (!_lastBuildSuccessful) {
            emails.addAll(_notifyOnFailure);
        }
        
        //we aren't using the email map, so the list provided is real email names.
        if (!_debug) {
            emails.addAll(getSetFromString(list));
        }

        return translateAliases(emails);
    }

    private void emailReport(Set emails, String subject) {

        String message = "view results here -> " + _servletURL + "?"
                         + _logFile.substring(_logFile.lastIndexOf(File.separator)+1,_logFile.lastIndexOf("."));
        try {
            Mailer mailer = new Mailer(_mailhost, emails, _returnAddress);
            mailer.sendMessage(subject, message);
        } catch (MessagingException be) {
            System.out.println("unable to send email.");
            be.printStackTrace();
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

        StringTokenizer st = new StringTokenizer(commaDelim, ", ");
        while (st.hasMoreTokens()) {
            String mapped = st.nextToken().trim();
            elements.add(mapped);
        }

        return elements;
    }

    private Set translateAliases(Set possibleAliases) {
        if (!_useEmailMap) {
            return possibleAliases;
        }

        Set returnAddresses = new HashSet();
        boolean aliasPossible = false;
        for (Iterator iter=possibleAliases.iterator(); iter.hasNext();) {
            String nextName = (String)iter.next();
            if (nextName.indexOf("@") > -1) {
                //The address is already fully qualified.
                returnAddresses.add(nextName);
            } else {
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
                    returnAddresses.add(nextName);
                } else {
                    returnAddresses.addAll(getSetFromString(mappedNames));
                    aliasPossible = true;
                }
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
    private void endLog() {
        log("\n");
        log("***** Ending Build Cycle, sleeping " + (_buildInterval/1000.0) + " seconds until next build.\n\n\n");
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
        try {
            String currentlyRunning = "<br>&nbsp;<br><b>Current Build Started At:</b><br>";
            String notRunning = "<br>&nbsp;<br><b>Next Build Starts At:</b><br>";
            SimpleDateFormat numericDateFormatter = new SimpleDateFormat("MM/dd/yyyy hh:mma");
            Date buildTime = new Date();
            if (!isRunning) {
                buildTime = new Date(buildTime.getTime() + _buildInterval);
            }

            FileWriter currentBuildWriter = new FileWriter(new File(_currentBuildStatusFile));
            currentBuildWriter.write((isRunning ? currentlyRunning : notRunning) + numericDateFormatter.format(buildTime) + "<br>");
            currentBuildWriter.close();
            currentBuildWriter = null;
        } catch (IOException ioe) {
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
        if (buildevent.getException() == null) {
            _lastBuildSuccessful = true;
            _lastGoodBuildTime = proj.getProperty(ModificationSet.SNAPSHOTTIMESTAMP);
        }

        //get the exact filenames from the ant properties that tell us what aux xml files we have...
        _auxLogFiles = new Vector();
        for (Enumeration e = _auxLogProperties.elements(); e.hasMoreElements();) {
            _auxLogFiles.add(proj.getProperty((String) e.nextElement()));
        }

        //If the XmlLogger.file property doesn't exist, we will set it here to a default
        //  value. This will short circuit XmlLogger from setting the default value.
        String prop = proj.getProperty("XmlLogger.file");
        if (prop == null || prop.trim().length() == 0) {
            proj.setProperty("XmlLogger.file", "log.xml");
        }
        super.buildFinished(buildevent);

        mergeAuxXmlFiles(proj);
    }

    /**
     *	Overrides method in XmlLogger.  writes snippet of html to disk
     *	specifying the start time of the running build, so that the build servlet can pick this up.
     */
    public void buildStarted(BuildEvent buildevent) {
        logCurrentBuildStatus(true);
        super.buildStarted(buildevent);
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