/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class implements the SourceControlElement methods
 * for a MKS repository. The call to MKS is assumed to work without
 * any setup. This implies that if the authentication type is pserver
 * the call to MKS login should be done prior to calling this class. This class is
 * is developed from the CVSElement code base from ThoughtWorks Inc.
 *
 * @author Suresh K Bathala Skila, Inc.
 */
public class MKS implements SourceControl {

    private static final Logger LOG = Logger.getLogger(MKS.class);

    private Hashtable properties = new Hashtable();
    private String property;
    private String propertyOnDelete;

    /**
     * This is the date format required by commands passed
     * to MKS.
     */
    private static final SimpleDateFormat MKSDATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * This is the date format returned in the log information
     * from MKS.
     */
    private static final SimpleDateFormat LOGDATE = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    /**
     * This line delimits seperate files in the MKS
     * log information.
     */
    private static final String MKS_FILE_DELIM =
        "===============================================================================";
        
    /**
     * This is the keyword that precedes the name of the
     * RCS filename in the MKS log information.
     */
    private static final String MKS_RCSFILE_LINE = "Archive file: ";
    /**
     * This is the keyword that precedes the name of the
     * working filename in the MKS log information.
     */
    private static final String MKS_WORKINGFILE_LINE = "Working file: ";
    /**
     * This line delimits the different revisions of a file
     * in the MKS log information.
     */
    private static final String MKS_REVISION_DELIM = "----------------------------";
    /**
     * This is the keyword that precedes the timestamp of a
     * file revision in the MKS log information.
     */
    private static final String MKS_REVISION_DATE = "date: ";
    /**
     * This is the keyword that precedes the author of a
     * file revision in the MKS log information.
     */
    private static final String MKS_REVISION_AUTHOR = "author: ";
    /**
     * This is the keyword that precedes the state keywords of a
     * file revision in the MKS log information.
     */
    private static final String MKS_REVISION_STATE = "state: ";
    /**
     * This is a state keyword which indicates that a revision
     * to a file consists of the deletion of that file.
     */
    private static final String MKS_REVISION_DELETED = "Delete";

    /**
     * System dependent new line seperator.
     */
    private static final String NEW_LINE = System.getProperty("line.separator");

    private String mksroot;
    private File localWorkingCopy;

    /**
     * Sets the MKSROOT for all calls to MKS.
     *
     * @param mksroot MKSROOT to use.
     */
    public void setMksroot(String mksroot) {
        this.mksroot = mksroot;
    }

    /**
     * Sets the local working copy to use when making calls
     * to MKS.
     *
     * @param local  String indicating the relative or absolute path
     *               to the local working copy of the module of which
     *               to find the log history.
     */
    public void setLocalWorkingCopy(String local) {
        localWorkingCopy = new File(local);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        this.propertyOnDelete = propertyOnDelete;
    }

    public Hashtable getProperties() {
        return properties;
    }

    public void validate() throws CruiseControlException {
        if (mksroot == null) {
            throw new CruiseControlException("'mksroot' is a required attribute on MKS");
        }
    }

    /**
     * Returns an ArrayList of Modifications detailing all
     * the changes between now and the last build.
     *
     * @param lastBuild Last build time.
     * @param now       Time now, or time to check.
     * @return maybe empty, never null.
     */
    public List getModifications(Date lastBuild, Date now) {
        List mods = null;

        //TODO: update to use CommandLine
        //TODO: use localWorkingCopy? (currently ignored)

        String dateRange =
            "\"" + MKSDATE.format(lastBuild) + "<" + MKSDATE.format(now) + "\"";
        String commandArray = "rlog -q -d" + dateRange + " -P" + mksroot;
        LOG.debug("Executing: " + commandArray);

        try {
            Process p = Runtime.getRuntime().exec(commandArray);

            //Logging the error stream.
            StreamPumper errorPumper =
                new StreamPumper(p.getErrorStream(),
                                 new PrintWriter(System.err, true));
            new Thread(errorPumper).start();

            //The input stream has the log information that we want to parse.
            InputStream input = p.getInputStream();
            mods = parseStream(input);

            //Using another stream pumper here will get rid of any leftover data in the stream.
            StreamPumper outPumper = new StreamPumper(input,
                                                      (PrintWriter) null);
            new Thread(outPumper).start();

            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        if (mods == null) {
            mods = new ArrayList();
        }
        /*********************************************/
       Iterator itr = mods.iterator();

        while (itr.hasNext()) {
            Modification mod = (Modification) itr.next();
            Modification.ModifiedFile modfile = (Modification.ModifiedFile) mod.files.get(0);

            System.out.println(
                " File Modified :"
                    + modfile.fileName
                    + "Time Modified :"
                    + mod.modifiedTime.toString());
        }


        /*********************************************/

        return mods;
    }


    /**
     * Parses the input stream, which should be from the
     * MKS log command. This method will format the data
     * found in the input stream into a List of Modification
     * instances.
     *
     * @param input  InputStream to get log data from.
     * @return List of Modification elements, maybe empty never null.
     */
    private List parseStream(InputStream input) throws IOException {
        ArrayList mods = new ArrayList();

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));


        //Read to the first RCS file name. The first entry in the log information will begin
        //  with this line. A MKS_FILE_DELIMITER is NOT present. If no RCS file lines
        // are found then there is nothing to do.
       // String line = readToNotPast(reader,MKS_RCSFILE_LINE, null);
        String line = readToNotPast(reader, MKS_RCSFILE_LINE, null);

        while (line != null) {

            //Parse the single file entry, which may include several modifications.
            List returnList = parseEntry(reader, line);
            //Add all the modifications to the local list.
            mods.addAll(returnList);
            //Read to the next RCS file line. The MKS_FILE_DELIMITER may have been
            //  consumed by the parseEntry method, so we cannot read to it.
            line = readToNotPast(reader, MKS_RCSFILE_LINE, null);


        }

        return mods;
    }

    /**
     * Parses a single file entry from the reader. This entry
     * may contain zero or more revisions. This method may
     * consume the next MKS_FILE_DELIMITER line from the
     * reader, but no further.
     *
     * @param reader Reader to parse data from.
     * @return modifications found in this entry; maybe empty, never null.
     * @exception IOException
     */
    private List parseEntry(BufferedReader reader, String archFileLine) throws IOException {
        ArrayList mods = new ArrayList();

        String nextLine = "";

        //Read to the working file name line to get the filename. It is ASSUMED
        //  that a line will exist with the working file name on it.
        //String workingFileLine = readToNotPast(reader, MKS_WORKINGFILE_LINE, null);
        String workingFileLine =  archFileLine;

        String workingFilename =
            workingFileLine.substring(
                workingFileLine.indexOf(MKS_WORKINGFILE_LINE)
                    + MKS_WORKINGFILE_LINE.length());
       // System.err.println("WorkingFilename :" + workingFilename);

        while (nextLine != null && !nextLine.startsWith(MKS_FILE_DELIM)) {

            //Read to the revision date. It is ASSUMED that each revision section will
            //  include this date information line.
            nextLine = readToNotPast(reader, MKS_REVISION_DATE, MKS_FILE_DELIM);
            if (nextLine == null) {
                //No more revisions for this file.
                break;
            }

            StringTokenizer tokens = new StringTokenizer(nextLine, " \t\n\r\f;");
            //First token is the keyword for date, then the next two should be the date and time stamps.
            tokens.nextToken();
            String dateStamp = tokens.nextToken();
            String timeStamp = tokens.nextToken();

            //The next token should be the author keyword, then the author name.
            tokens.nextToken();
            String authorName = tokens.nextToken();

            //The next token should be the state keyword, then the state name.
            tokens.nextToken();
            String stateKeyword = tokens.nextToken();

            //All the text from now to the next revision delimiter or working file delimiter
            //  constitutes the messsage.
            String message = "";
            nextLine = reader.readLine();
            boolean multiLine = false;
            while (nextLine != null
                   && !nextLine.startsWith(MKS_FILE_DELIM)
                   && !nextLine.startsWith(MKS_REVISION_DELIM)) {

                if (multiLine) {
                    message += NEW_LINE;
                } else {
                    multiLine = true;
                }
                message += nextLine;

                //Go to the next line.
                nextLine = reader.readLine();
            }

            Modification nextModification = new Modification("mks");
            Modification.ModifiedFile modfile = nextModification.createModifiedFile(workingFilename, null);

            try {

               // nextModification.modifiedTime = LOGDATE.parse(dateStamp + " " + timeStamp + " GMT");
                nextModification.modifiedTime = LOGDATE.parse(dateStamp + " " + timeStamp + " GMT");
            } catch (ParseException pe) {
                LOG.error("Error parsing date stamp.", pe);
            }

            nextModification.userName = authorName;

            nextModification.comment = (message != null ? message : "");

            if (stateKeyword.equalsIgnoreCase(MKS_REVISION_DELETED)) {
                nextModification.type = "deleted";
                if (propertyOnDelete != null) {
                    properties.put(propertyOnDelete, "true");
                }
                if (property != null) {
                    properties.put(property, "true");
                }
            } else {
                nextModification.type = "modified";
                if (property != null) {
                    properties.put(property, "true");
                }
            }

            mods.add(nextModification);
        }
        return mods;
    }

    /**
     * This method will consume lines from the reader up
     * to the line that begins with the String specified
     * but not past a line that begins with the notPast
     * String. If the line that begins with the beginsWith
     * String is found then it will be returned. Otherwise
     * null is returned.
     *
     * @param reader     Reader to read lines from.
     * @param beginsWith String to match to the beginning of a line.
     * @param notPast    String which indicates that lines should stop being
     *                   consumed, even if the begins with match has not been
     *                   found. Pass null to this method to ignore this string.
     * @return String that begin as indicated, or null if none matched
     *         to the end of the reader or the notPast line was found.
     * @exception IOException
     */
    private String readToNotPast(BufferedReader reader, String beginsWith,
                                 String notPast) throws IOException {
        boolean checkingNotPast = notPast != null;


        String nextLine = "";
        while (nextLine != null
            && (!nextLine.startsWith(beginsWith))
            && !(nextLine.indexOf(beginsWith) != -1)) {

            if (checkingNotPast && nextLine.startsWith(notPast)) {
                return null;
            }
            nextLine = reader.readLine();

        }
        return nextLine;
    }
}