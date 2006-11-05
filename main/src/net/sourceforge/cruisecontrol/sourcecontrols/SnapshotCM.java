/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2004, ThoughtWorks, Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.IO;

import org.apache.log4j.Logger;

/**
 *  Retrieves change history from SnapshotCM source control using whist command.
 *
 *  @author patrick.conant@hp.com
 */

public class SnapshotCM implements SourceControl {
    /**  Date format required by commands passed to SnapshotCM */
    private final SimpleDateFormat inDateFormatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

    public static final String OUT_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
    /**  Date format returned in the output of SnapshotCM commands. */
    private final SimpleDateFormat outDateFormatter = new SimpleDateFormat(OUT_DATE_FORMAT);

    private static final MessageFormat EXECUTABLE = new MessageFormat("whist -RA -c{0} \"{1}\"");

    private static final String FILE_HEADER = "=============================================================";

    private static final String REVISION_HEADER = "----------------------------";

    private static final String CHANGE_DELETE = "Delete";

    /** enable logging for this class */
    private static final Logger LOG = Logger.getLogger(SnapshotCM.class);

    private SourceControlProperties properties = new SourceControlProperties();

    /**
     *  List of source path values provided either with sourcePath="...",
     *  sourcePaths="...;...", or nested &lt;sourcePath path="..."&gt; elements.
     */
    private List sourcePaths = new ArrayList();

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    /**
     *  From SourceControl interface.  n
     */
    public void setPropertyOnDelete(String propertyOnDelete) {
        properties.assignPropertyOnDeleteName(propertyOnDelete);
    }

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void setSourcePaths(String sourcePaths) {
        StringTokenizer st = new StringTokenizer(sourcePaths, ";");
        while (st.hasMoreTokens()) {
            setSourcePath(st.nextToken());
        }
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePaths.add(new SourcePath(sourcePath));
    }

    public SourcePath createSourcePath() {
        SourcePath sourcePath = new SourcePath();
        this.sourcePaths.add(sourcePath);
        return sourcePath;
    }

    /**
     *  From SourceControl interface.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(this.sourcePaths.isEmpty(),
            "'sourcePaths' or 'sourcePath' attribute, or nested sourcepath element(s)"
            + " is a required attribute for SnapshotCM.");
    }

    /**
     *  Returns an {@link java.util.List List} of {@link Modification}
     *  detailing all the changes between now and the last build.
     *
     *  @param  lastBuild the last build time
     *  @param  now time now, or time to check, NOT USED
     *  @return  the list of modifications, an empty (not null) list if no
     *      modifications.
     */
    public List getModifications(Date lastBuild, Date now) {
        // Return value
        List modificationList = new ArrayList();

        //Command parameters
        String[] parameters = new String[2];
        parameters[0] = inDateFormatter.format(lastBuild);

        for (Iterator i = this.sourcePaths.iterator(); i.hasNext(); ) {
            parameters[1] = ((SourcePath) i.next()).getPath();

            String command = EXECUTABLE.format(parameters);
            LOG.info("Running command: " + command);
            try {
                Process p = Runtime.getRuntime().exec(command);

                Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, p));
                stderr.start();

                InputStream input = p.getInputStream();
                modificationList.addAll(parseStream(input));

                p.waitFor();
                stderr.join();
                IO.close(p);
            } catch (Exception e) {
                LOG.error("Error in executing the SnapshotCM command : ", e);
            }
        }

        properties.modificationFound();
        
        return modificationList;
    }

    /**
     *  Parses the input stream to construct the modifications list.
     *  Parser splits the returned string into seperate sections
     *  based on the token string FILE_HEADER.  Each file in the list
     *  is passed to the parseEntry method in order to pull out the
     *  user &amp; file names and modification dates.
     *
     *  Package-private to make it available to the unit test.
     *
     *  @param  input the stream to parse
     *  @return  a list of modification elements
     *  @exception  IOException
     */
    List parseStream(InputStream input) throws IOException {
        List modifications = new ArrayList();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        StringBuffer sb = new StringBuffer();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(FILE_HEADER)) {
                List fileMods = parseEntry(sb.toString());
                modifications.addAll(fileMods);
                sb = new StringBuffer();
            } else {
                sb.append(line);
                sb.append('\n');
            }
        }
        List fileMods = parseEntry(sb.toString());
        modifications.addAll(fileMods);
        return modifications;
    }


    /**
     *  Parses a single line from the reader. Each entry looks like this:
     *
     *
     *  File: /src/Backend/CEUI/tomcat/build.xml
     *  Snapshot: /RSTDevelopment/A.03.50/Develop
     *  Current revision: 46
     *  I/O mode: text
     *  Keyword expansion: keyword and value
     *  Permissions: r--r--r--
     *  ----------------------------
     *  Revision: 46 (current)   Derivation:    45 --&gt; (46)
     *  Date: 2004/01/06 17:00:38 -0700;  Size:    39459 bytes
     *  Author: pacon (Patrick Conant)
     *  Snapshot: /RSTDevelopment/A.03.50/Develop
     *  Used in: /RSTDevelopment/A.03.50/Develop
     *  Change: Content
     *  Removed -D param from SnapshotCM wco and wci commands.
     *  ----------------------------
     *  Revision: 45             Derivation:    44 --&gt; (45) --&gt; 46
     *  Date: 2004/01/06 13:10:31 -0700;  Size:    39468 bytes
     *  Author: pacon (Patrick Conant)
     *  Snapshot: /RSTDevelopment/A.03.50/Develop
     *  Change: Content
     *  Checked in via UltraEdit
     *
     *  There may be one or more revision entries.
     *
     *  @param  entry the entry to parse.
     *  @return an array of modification elements corresponding to the given
     *          entry.
     */
    private List parseEntry(String entry) {
        List modifications = new ArrayList();

        StringTokenizer st = new StringTokenizer(entry, "\n");
        /*
         *  We should get at least 13 lines if there has been a modification.
         */
        if (st.countTokens() < 13) {
            return modifications;
        }

        /*
         *  Read the header, which is the first 6 lines.
         */
        String line = st.nextToken();
        String entryname = line.substring(6);
        String fileName;
        String folderName;
        int sep = entryname.lastIndexOf("/");
        if (sep == -1) {
            sep = entryname.lastIndexOf("\\");
        }
        if (sep > -1) {
            folderName = entryname.substring(0, sep);
            fileName = entryname.substring(sep + 1);
        } else {
            folderName = "";
            fileName = entryname;
        }

        // ignore next 5 lines.
        String nextToken;
        do {
            nextToken = st.nextToken();
        } while (!(nextToken).equals(REVISION_HEADER));

        /*
         *  Now read in each modification.
         */
        Modification mod = new Modification("snapshotcm");
        mod.createModifiedFile(fileName, folderName);

        while (st.hasMoreTokens()) {
            line = st.nextToken();
            if (line.equals(REVISION_HEADER) || !st.hasMoreTokens()) {
                if (!line.trim().equals("") && !line.equals(REVISION_HEADER)) {
                    //consider this part of the comment.
                    mod.comment += line;
                }

                //Save the modification
                modifications.add(mod);
                mod = new Modification("snapshotcm");
                mod.createModifiedFile(fileName, folderName);
            } else if (line.startsWith("Revision: ")) {  //e.g. Revision: 46 (current)   Derivation:    45 --> (46)
                int nextSpaceDelimiterIndex = line.indexOf(" ", 10);
                int endIndex = nextSpaceDelimiterIndex > -1 ? nextSpaceDelimiterIndex : line.length();
                mod.revision = line.substring(10, endIndex);
            } else if (line.startsWith("Date: ")) {  //e.g. Date: 2004/01/06 17:00:38 -0700;  Size:    39459 bytes
                try {
                    mod.modifiedTime = outDateFormatter.parse(line.substring(6, line.indexOf("-") - 1));
                } catch (ParseException pe) {
                    LOG.warn("Unable to parse date " + line.substring(6, line.indexOf("-") - 1));
                    mod.modifiedTime = new Date(0);
                }
            } else if (line.startsWith("Author: ")) {  //e.g. Author: pacon (Patrick Conant)
                mod.userName = line.substring(8).trim();
                if (mod.userName.indexOf(" ") > -1) {
                    mod.userName = mod.userName.substring(0, mod.userName.indexOf(" "));
                }
            } else if (line.startsWith("Change: ")) {  //e.g. Change: Content
                mod.type = line.substring(8).trim();
                if (mod.type.equals(CHANGE_DELETE)) {
                    properties.deletionFound();
                }
            } else if (line.startsWith("Snapshot: ")) {
                //ignore
            } else if (line.startsWith("Used in: ") || line.startsWith("         /")) {
                //e.g.  Used in: /aaaa/cccc/bbbb/ccccdddd
                //               /aaaa/cccc/bbbb/

                //ignore
            } else if (!line.trim().equals("")) {
                //consider this part of the comment.
                mod.comment += line;
            }
        }

        return modifications;
    }

    public static class SourcePath {
        private String path;

        public SourcePath() {
        }

        public SourcePath(String path) {
            this.path = path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return this.path;
        }
    }
}
