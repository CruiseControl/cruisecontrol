package net.sourceforge.cruisecontrol;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;


/**
 * This class provides specific behavior for merging XML files created
 * during the build process managed by CruiseControl.
 */
public class FileMerger {
    
    private List auxLogFiles = new ArrayList();

    /**
     * Creates a new FileMerger which will handle merging files found from
     * the Ant Project instance provided.
     * 
     * @param proj   Ant project instance through which properties specifying the files to
     *               merge can be found.
     */
    public FileMerger(Project proj, List auxLogProperties) {
        //get the exact filenames from the ant properties that tell us what aux xml files we have...
        for (Iterator e = auxLogProperties.iterator(); 
            e.hasNext();) {
            String propertyName = (String)e.next();
            String fileName = proj.getProperty(propertyName);
            if (fileName == null) {
                System.out.println("Auxillary Log File Property '" + propertyName + "' not set.");
            } else {
                auxLogFiles.add(fileName);
            }
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
     * 
     * @param proj   Ant project instance from which property values can be retrieved.
     * @param info   BuildInfo instance containing the information for the specific build.
     * @return The name of the log file to which all the auxilary logs were merged.
     */
    public String mergeAuxXmlFiles(Project proj, BuildInfo info, String logDir) {

        String logFile = createLogName(proj.getProperty("DSTAMP"),
                                       proj.getProperty("TSTAMP"),
                                       logDir, info);
        System.out.println("Final log name is: " + logFile);

        try {
            //REDTAG - Paul - Reading the contents of all the logs
            //  into a StringBuffer can result in an OutOfMemoryError
            //  if the log files are big. This code should be reworked
            //  to write log files to the aggregated log file as it goes.
            StringBuffer aggregatedXMLLog = new StringBuffer();
            aggregatedXMLLog.append(readAntBuildLog(proj));

            //for each aux xml file, read and write to aggregated log
            for (Iterator logFiles = auxLogFiles.iterator(); 
                logFiles.hasNext();) {
                String nextFileName = (String) logFiles.next();

                //Read in the entire aux log file, stripping any xml version tags.
                try {
                    String text = stripXMLVersionTags(readFile(nextFileName));
                    aggregatedXMLLog.append(text);
                } catch (IOException fnfe) {
                    System.out.println(nextFileName + " not found. Skipping...");
                }
            }

            //close aggregated build log
            aggregatedXMLLog.append("<label>" + info.getLabel()+ "</label>");
            aggregatedXMLLog.append("<today>" + proj.getProperty("TODAY") + "</today>");
            aggregatedXMLLog.append("</build>");

            info.setLogfile(logFile);
            writeFile(logFile, aggregatedXMLLog.toString());

        } catch (Throwable ioe) {
            ioe.printStackTrace();
        }

        return logFile;
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
     * The resultant file name should be in the form:
     * the keyword "log" followed by the datestamp and timestamp.
     * If the build was successful then the key letter "L" followed
     * by the build label.
     * 
     * @param dateStamp Date stamp to use in the log file name. Cannot be null, see the
     *                  RuntimeException for more details.
     * @param timeStamp Time stamp to use in the log file name. Cannot be null, see the
     *                  RuntimeException for more details.
     * @param logdir    Directory where the log file should exist.
     * @param info      BuildInfo instance containing values used while creating the log file
     *                  name.
     * @return Absolute path and name of the composite log file.
     * @exception RuntimeException
     *                   Thrown if either the time stamp or date stamp are null. They are
     *                   required to create a well-formed log file name.
     */
    public String createLogName(String dateStamp, String timeStamp, 
                                String logdir, BuildInfo info) 
        throws RuntimeException {

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
        logFileName = logdir + File.separator + logFileName;

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
    public String readFile(String fileName) throws IOException {
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

        System.out.println("Reading file " + file.getAbsolutePath());

        FileInputStream in = new FileInputStream(file);
        byte[] allBytes = new byte[in.available()];
        in.read(allBytes);
        in.close();
        in = null;

        return text.append(new String(allBytes)).toString();
    }    

    /**
     * Returns the names of all the XML files in the specified directory.
     * A file is considered to be an XML file if it ends with a "xml"
     * extension. Hence, myfile.xml is an XML file, whereas myfile.xsl is
     * NOT an XML file.
     * <P>
     * Note that this method is not recursive. 
     * <P>
     * Note this method will only return names of regular files, not 
     * directories. So, if the OS allows directories to be named with 
     * extensions like ".xml" they will not be included.
     * 
     * @param dir    Directory to search for XML files.
     * @return String array containing the names of all the XML files found in the
     *         directory, null if the File instance provided is not a directory or
     *         is null, and empty if the directory contains no XML files.
     */
    public String[] retrieveXMLFiles(File dir) {
        if (dir == null) {
            return null;
        }
        
        return dir.list(
            new FilenameFilter() {
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
        System.out.println("Writing file: " + f.getAbsolutePath());
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
                System.out.println("Found an xml version tag that opens"
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
}


