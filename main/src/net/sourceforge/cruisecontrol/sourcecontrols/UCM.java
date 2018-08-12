/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.CommandlineUtil;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom2.Element;

/**
 * This class implements the SourceControlElement methods for ClearCase UCM.
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 * @author Alex Batlin
 */
public class UCM implements SourceControl {

    private static final Logger LOG = Logger.getLogger(UCM.class);

    private String stream;
    private String viewPath;
    private boolean multiVob;
    private boolean contributors = true;
    private boolean rebases = false;

    private final SourceControlProperties properties = new SourceControlProperties();

    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd-MMMM-yyyy.HH:mm:ss");
    private final SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss");

    private String pvob;

    /**
     * Unlikely combination of characters to separate fields in a ClearCase query
     */
    static final String DELIMITER = "#~#";

    /**
     * Even more unlikely combination of characters to indicate end of one line in query. Carriage return (\n) can be
     * used in comments and so is not available to us.
     */
    static final String END_OF_STRING_DELIMITER = "@#@#@#@#@#@#@#@#@#@#@#@";

    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(stream, "stream", this.getClass());
        ValidationHelper.assertIsSet(viewPath, "viewpath", this.getClass());

        if (isRebases()) {
            ValidationHelper.assertIsSet(pvob, "pvob", this.getClass());
        }
    }

    /**
     * get which stream is being checked
     *
     * @return the name of the stream being checked
     */
    public String getStream() {
        return stream;
    }

    /**
     * set the stream to check for changes
     *
     * @param stream
     *            the stream to be checked (via its underlying branch)
     */
    public void setStream(String stream) {
        this.stream = stream;
    }

    /**
     * get the starting point path in a view to check for changes
     *
     * @return path inside a view
     */
    public String getViewPath() {
        return viewPath;
    }

    /**
     * set the starting point path in a view to check for changes
     *
     * @param viewPath
     *            path inside a view
     */
    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }

    /**
     * get whether the view contains multiple vobs
     *
     * @return true, if the view contains multiple vobs, else false
     */
    public boolean isMultiVob() {
        return this.multiVob;
    }

    /**
     * set whether the view contains multiple vobs
     *
     * @param multiVob boolean indicating whether the view contains multiple vobs
     */
    public void setMultiVob(boolean multiVob) {
        this.multiVob = multiVob;
    }

    /**
     * Set the name of the pvob to use for queries.
     *
     * @param pvob
     *            the pvob
     */
    public void setPvob(String pvob) {
        this.pvob = pvob;
    }

    /**
     * Get the name of the pvob to use for queries.
     *
     * @return The name of the pvob
     */
    public String getPvob() {
        return this.pvob;
    }

    /**
     * Gets whether rebases are to be reported as changes.
     *
     * @return true, if rebases are to be reported, else false
     */
    public boolean isRebases() {
        return this.rebases;
    }

    /**
     * Sets whether rebases of the integration stream are reported as changes.
     *
     * @param rebases
     *            boolean indicating whether rebases are to be reported as changes
     */
    public void setRebases(boolean rebases) {
        this.rebases = rebases;
    }

    /**
     * get whether contributors are to be found
     *
     * @return true, if contributors are to be found, else false
     */
    public boolean isContributors() {
        return contributors;
    }

    /**
     * set whether contributors are to be found
     *
     * @param contributors
     *            boolean indicating whether contributors are to be found
     */
    public void setContributors(boolean contributors) {
        this.contributors = contributors;
    }

    /**
     * set the name of the property that will be set if modifications are found
     *
     * @param property
     *            The name of the property to set
     */
    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    /**
     * Get a List of modifications detailing all the changes between now and the last build. Return this as an element.
     * It is not necessary for sourcecontrols to actually do anything other than returning a chunk of XML data back.
     *
     * @param lastBuild
     *            time of last build
     * @param now
     *            time this build started
     * @return a list of XML elements that contains data about the modifications that took place. If no changes, this
     *         method returns an empty list.
     */
    public List<Modification> getModifications(final Date lastBuild, final Date now) {
        final String lastBuildDate = inputDateFormat.format(lastBuild);
        final String nowDate = inputDateFormat.format(now);
        properties.put("ucmlastbuild", lastBuildDate);
        properties.put("ucmnow", nowDate);

        List<Modification> mods = new ArrayList<Modification>();
        try {
            final HashMap<String, String> activityNames = collectActivitiesSinceLastBuild(lastBuildDate);
            if (activityNames.size() == 0) {
                return mods;
            }
            mods = describeAllActivities(activityNames);
        } catch (Exception e) {
            LOG.error("Command failed to execute succesfully", e);
        }

        if (this.isRebases()) {
            try {
                final Commandline commandline = buildDetectRebasesCommand(lastBuildDate);
                commandline.setWorkingDirectory(viewPath);
                final InputStream cmdStream = CommandlineUtil.streamOutput(commandline);

                try {
                    mods.addAll(parseRebases(cmdStream));
                } finally {
                    cmdStream.close();
                }
            } catch (Exception e) {
                LOG.error("Error in executing the Clear Case command : ", e);
            }
        }

        // If modifications were found, set the property
        if (!mods.isEmpty()) {
            properties.modificationFound();
        }

        return mods;
    }

    /**
     * @param lastBuildDate last build date
     * @return all the activities on the stream since the last build date
     */
    private HashMap<String, String> collectActivitiesSinceLastBuild(final String lastBuildDate) {

        LOG.debug("Last build time was: " + lastBuildDate);

        final HashMap<String, String> activityMap = new HashMap<String, String>();

        final Commandline commandLine = buildListStreamCommand(lastBuildDate);
        LOG.debug("Executing: " + commandLine);

        try {
            commandLine.setWorkingDirectory(viewPath);
            final InputStream cmdStream = CommandlineUtil.streamOutput(commandLine);
            final InputStreamReader isr = new InputStreamReader(cmdStream);
            final BufferedReader br = new BufferedReader(isr);
            try {

                String line = br.readLine();
                while ((line != null) && (!line.equals(""))) {
                    final String[] details = getDetails(line);
                    if (details[0].equals("mkbranch") || details[0].equals("rmbranch") || details[0].equals("rmver")) {
                        // if type is create/remove branch then skip
                    } else {
                        final String activityName = details[1];
                        final String activityDate = details[2];
                        // assume the latest change for an activity is listed first
                        if (!activityMap.containsKey(activityName)) {
                            LOG.debug("Found activity name: " + activityName + "; date: " + activityDate);
                            activityMap.put(activityName, activityDate);
                        }
                    }

                    line = br.readLine();
                }
            } finally {
                br.close();
                cmdStream.close();
            }
        } catch (IOException e) {
            LOG.error("IO Error executing ClearCase lshistory command", e);
        } catch (CruiseControlException e) {
            LOG.error("Interrupt Error executing ClearCase lshistory command", e);
        }

        return activityMap;
    }

    private String[] getDetails(String line) {
        // replacing line.split("~#~") for jdk 1.3
        ArrayList<String> details = new ArrayList<String>();
        String delimiter = "~#~";
        int startIndex = 0;
        int index = 0;
        while (index != -1) {
            String detail;
            index = line.indexOf(delimiter, startIndex);
            if (index == -1) {
                detail = line.substring(startIndex, line.length());
            } else {
                detail = line.substring(startIndex, index);
            }
            details.add(detail);
            startIndex = index + delimiter.length();
        }

        return details.toArray(new String[details.size()]);
    }

    /**
     * @param lastBuildDate last build date
     * @return a command to get all the activities on the specified stream
     */
    public Commandline buildListStreamCommand(final String lastBuildDate) {
        final Commandline commandLine = new Commandline();
        if (isMultiVob()) {
            try {
                commandLine.setWorkingDirectory(getViewPath());
            } catch (CruiseControlException e) {
               LOG.error("Error in setting workdirectory", e);
            }
        }
        commandLine.setExecutable("cleartool");
        commandLine.createArgument("lshistory");
        commandLine.createArguments("-branch", getStream());
        if (isMultiVob()) {
            commandLine.createArgument("-avobs");
        } else {
            commandLine.createArgument("-r");
        }
        commandLine.createArgument("-nco");
        commandLine.createArguments("-since", lastBuildDate);
        commandLine.createArguments("-fmt", "%o~#~%[activity]Xp~#~%Nd\n");
        if (!isMultiVob()) {
            commandLine.createArgument(getViewPath());
        }
        return commandLine;
    }

    /**
     * @param activityNames all the activities on the stream since the last build date
     * @return all the activities on the stream since the last build date
     */
    private List<Modification> describeAllActivities(final HashMap<String, String> activityNames) {

        final ArrayList<Modification> activityList = new ArrayList<Modification>();

        for (Map.Entry<String, String> activity : activityNames.entrySet()) {
            final String activityID = activity.getKey();
            final String activityDate = activity.getValue();
            final UCMModification activityMod = describeActivity(activityID, activityDate);
            activityList.add(activityMod);

            // check for contributor activities
            if (activityMod.comment.startsWith("deliver ") && isContributors()) {
                final List<String> contribList = describeContributors(activityID);
                for (final String contribName : contribList) {
                    final UCMModification contribMod = describeActivity(contribName, activityDate);
                    // prefix type to make it stand out in Build Results report
                    contribMod.type = "contributor";
                    LOG.debug("Found contributor name: " + contribName + "; date: " + activityDate);
                    activityList.add(contribMod);
                }
            }
        }

        return activityList;
    }

    /**
     * @param activityID stream ID
     * @param activityDate activity date
     * @return all the activities on the stream since the last build date
     */
    private UCMModification describeActivity(final String activityID, final String activityDate) {

        final UCMModification mod = new UCMModification();

        final Commandline commandLine = buildDescribeActivityCommand(activityID);
        LOG.debug("Executing: " + commandLine);

        try {
            commandLine.setWorkingDirectory(viewPath);
            final InputStream cmdStream = CommandlineUtil.streamOutput(commandLine);
            final InputStreamReader isr = new InputStreamReader(cmdStream);
            final BufferedReader br = new BufferedReader(isr);
            try {

                String line = br.readLine();
                while ((line != null) && (!line.equals(""))) {
                    final String[] details = getDetails(line);
                    try {
                        mod.modifiedTime = outputDateFormat.parse(activityDate);
                    } catch (ParseException e) {
                        LOG.error("Error parsing modification date");
                        mod.modifiedTime = new Date();
                    }
                    mod.type = "activity";
                    // counter for UCM without ClearQuest
                    if (details[0].equals("")) {
                        mod.revision = details[3];
                    } else {
                        mod.revision = details[0];
                    }
                    mod.crmtype = details[1];
                    mod.userName = details[2];
                    mod.comment = details[3];

                    line = br.readLine();
                }
            } finally {
                br.close();
                cmdStream.close();
            }
        } catch (IOException e) {
            LOG.error("IO Error executing ClearCase describe command", e);
        } catch (CruiseControlException e) {
            LOG.error("Interrupt error executing ClearCase describe command", e);
        }

        return mod;
    }

    /**
     * @param activityID a stream id
     * @return a command to get all the activities on the specified stream
     */
    public Commandline buildDescribeActivityCommand(final String activityID) {
        final Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");
        commandLine.createArgument("describe");
        commandLine.createArguments("-fmt", "%[crm_record_id]p~#~%[crm_record_type]p~#~%u~#~%[headline]p~#~");
        commandLine.createArgument(activityID);
        return commandLine;
    }

    /**
     * @param activityName activity name
     * @return all the activities on the stream since the last build date
     */
    private List<String> describeContributors(final String activityName) {

        final ArrayList<String> contribList = new ArrayList<String>();
        final Commandline commandLine = buildListContributorsCommand(activityName);
        LOG.debug("Executing: " + commandLine);

        try {
            commandLine.setWorkingDirectory(viewPath);
            final InputStream cmdStream = CommandlineUtil.streamOutput(commandLine);
            final InputStreamReader isr = new InputStreamReader(cmdStream);
            final BufferedReader br = new BufferedReader(isr);
            try {
                String line;

                while ((line = br.readLine()) != null) {
                    final String[] contribs = splitOnSpace(line);
                    contribList.addAll(Arrays.asList(contribs));
                }
            } finally {
                br.close();
                cmdStream.close();
            }
        } catch (IOException e) {
            LOG.error("IO Error executing ClearCase describe contributors command", e);
        } catch (CruiseControlException e) {
            LOG.error("Interrupt Error executing ClearCase describe contributors command", e);
        }

        return contribList;
    }

    private String[] splitOnSpace(final String string) {
        return string.split(" ");
    }

    /**
     * @param activityID stream ID
     * @return a command to get all the activities on the specified stream
     */
    public Commandline buildListContributorsCommand(final String activityID) {
        final Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");
        commandLine.createArgument("describe");
        commandLine.createArguments("-fmt", "%[contrib_acts]Xp");
        commandLine.createArgument(activityID);
        return commandLine;
    }

    protected Commandline buildDetectRebasesCommand(final String lastBuildDate) {
        final Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");
        commandLine.createArgument().setValue("lshistory");
        commandLine.createArgument().setValue("-since");
        commandLine.createArgument().setValue(lastBuildDate);
        commandLine.createArgument().setValue("-minor");
        commandLine.createArgument().setValue("-fmt");
        final String format = "%u" + DELIMITER + "%Nd" + DELIMITER + "%o" + DELIMITER + "%Nc" + END_OF_STRING_DELIMITER
                + "\\n";
        commandLine.createArgument().setValue(format);
        commandLine.createArgument().setValue("stream:" + stream + "@" + pvob);
        return commandLine;
    }

    /**
     * Parses the given input stream to construct the modifications list. The stream is expected to be the result of
     * listing the history of a UCM stream. Rebases are then detected by delegating to
     * {@link #parseRebaseEntry(String)}.
     * Package-private to make it available to the unit test.
     *
     * @param input
     *            the stream to parse
     * @return a list of modification elements
     * @exception IOException if an IO error occurs
     */
    List<Modification> parseRebases(final InputStream input) throws IOException {
        final ArrayList<Modification> modifications = new ArrayList<Modification>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        try {
            final String ls = System.getProperty("line.separator");

            String line;
            StringBuffer lines = new StringBuffer();

            while ((line = reader.readLine()) != null) {
                if (lines.length() != 0) {
                    lines.append(ls);
                }
                lines.append(line);
                Modification mod = null;

                if (lines.indexOf(END_OF_STRING_DELIMITER) > -1) {
                    mod = parseRebaseEntry(lines.substring(0, lines.indexOf(END_OF_STRING_DELIMITER)));
                    lines = new StringBuffer();
                }

                if (mod != null) {
                    modifications.add(mod);
                }
            }
        } finally {
            reader.close();
        }
        return modifications;
    }

    /**
     * Parses a single line from the reader. Each line contains a signe revision with the format : <br>
     * username#~#date_of_revision#~#operation_type#~#comments <br>
     * <p>
     * This method looks for operations of type rmhlink and mkhlink where the hyperlink name begins with "UseBaseline".
     * These represent changes in the baseline dependencies of the stream.
     * </p>
     *
     * @param line
     *            the line to parse
     * @return a modification element corresponding to the given line
     */
    Modification parseRebaseEntry(final String line) {
        LOG.debug("parsing entry: " + line);
        String[] tokens = tokenizeEntry(line);
        if (tokens == null) {
            return null;
        }
        String username = tokens[0].trim();
        String timeStamp = tokens[1].trim();
        String operationType = tokens[2].trim();
        String comment = tokens[3].trim();

        Modification mod = null;

        // Rebases show up as mkhlink and rmhlink operations
        if (operationType.equals("mkhlink") || operationType.equals("rmhlink")) {
            // Parse the hyperlink name out of the comment field, then
            // get more information on that hyperlink
            mod = new Modification();
            String linkName = parseLinkName(comment);

            // If this isn't a "UseBaseline" hyperlink, we're not interested
            if (!linkName.startsWith("UseBaseline")) {
                return null;
            }

            Hyperlink link = getHyperlink(linkName);
            StringBuffer modComment = new StringBuffer();
            mod.type = "ucmdependency";

            if (operationType.equals("mkhlink")) {
                modComment.append("Added dependency");
            } else {
                modComment.append("Removed dependency");
            }

            if (link.getFrom().length() > 0) {
                modComment.append(" of ");
                modComment.append(link.getFrom());
            }

            if (link.getTo().length() > 0) {
                modComment.append(" on ");
                modComment.append(link.getTo());
                mod.revision = link.getTo();
            } else {
                // Don't know what the revision was to
                mod.revision = "";
            }

            mod.comment = modComment.toString();
            mod.userName = username;

            try {
                mod.modifiedTime = outputDateFormat.parse(timeStamp);
            } catch (ParseException e) {
                LOG.error("Error parsing modification date", e);
                mod.modifiedTime = new Date();
            }

            properties.modificationFound();
        }

        return mod;
    }

    private Hyperlink getHyperlink(String linkName) {
        Commandline commandline = buildGetHyperlinkCommandline(linkName);

        try {
            commandline.setWorkingDirectory(viewPath);
            InputStream cmdStream = CommandlineUtil.streamOutput(commandline);
            Hyperlink link = null;

            try {
                link = parseHyperlinkDescription(cmdStream);
            } finally {
                cmdStream.close();
            }

            return link;
        } catch (Exception e) {
            LOG.error("Error in executing the Clear Case command : ", e);
            return new Hyperlink();
        }
    }

    protected Commandline buildGetHyperlinkCommandline(String linkName) {
        Commandline commandline = new Commandline();
        commandline.setExecutable("cleartool");
        commandline.createArgument().setValue("describe");
        commandline.createArgument().setValue("hlink:" + linkName + "@" + pvob);
        return commandline;
    }

    Hyperlink parseHyperlinkDescription(final InputStream input) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        try {
            String lastLine = "";
            String line = reader.readLine();

            // If the hyperlink wasn't found, cleartool will return no output, giving
            // us an empty stream. This will end up returning an empty string.
            while (line != null) {
                lastLine = line;
                line = reader.readLine();
            }

            final Hyperlink link = new Hyperlink();
            final StringTokenizer tokens = new StringTokenizer(lastLine, " ");

            if (!tokens.hasMoreTokens()) {
                return link;
            }

            // Discard the first one, that's the link name
            tokens.nextToken();

            if (!tokens.hasMoreTokens()) {
                return link;
            }

            link.setFrom(tokens.nextToken());

            // Discard "->"
            if (!tokens.hasMoreTokens()) {
                return link;
            }

            tokens.nextToken();

            if (!tokens.hasMoreTokens()) {
                return link;
            }

            link.setTo(tokens.nextToken());

            return link;

        } finally {
            reader.close();
        }
    }

    String parseLinkName(String comment) {
        // Parse on spaces and quotes (to eliminate them)
        StringTokenizer tokens = new StringTokenizer(comment, " \"");
        String link = "";
        String token = "";

        // The next-to-last token should contain the hyperlink name
        while (tokens.hasMoreTokens()) {
            link = token;
            token = tokens.nextToken();
        }

        int index = link.lastIndexOf('@');

        if (index != -1) {
            // Remove the VOB-qualifier from the end. We'll add it back ourselves
            return link.substring(0, link.lastIndexOf('@'));
        } else {
            // Return it unmodified
            return link;
        }
    }

    private String[] tokenizeEntry(String line) {
        int maxTokens = 4;
        int minTokens = maxTokens - 1; // comment may be absent.
        String[] tokens = new String[maxTokens];
        Arrays.fill(tokens, "");
        int tokenIndex = 0;
        for (int oldIndex = 0, i = line.indexOf(DELIMITER, 0); true; oldIndex = i + DELIMITER.length(), i = line
                .indexOf(DELIMITER, oldIndex), tokenIndex++) {
            if (tokenIndex > maxTokens) {
                LOG.debug("Too many tokens; skipping entry");
                return null;
            }
            if (i == -1) {
                tokens[tokenIndex] = line.substring(oldIndex);
                break;
            } else {
                tokens[tokenIndex] = line.substring(oldIndex, i);
            }
        }
        if (tokenIndex < minTokens) {
            LOG.debug("Not enough tokens; skipping entry");
            return null;
        }
        return tokens;
    }

    /**
     * Class to represent ClearCase hyperlinks.
     */
    class Hyperlink {
        private String from = "";
        private String to = "";

        public String getFrom() {
            return this.from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return this.to;
        }

        public void setTo(String to) {
            this.to = to;
        }
    }

    /**
     * class to hold UCMModifications
     */
    private static class UCMModification extends Modification {
        private static final String TAGNAME_CRMTYPE = "crmtype";

        public String crmtype;

        public int compareTo(final Modification o) {
            UCMModification modification = (UCMModification) o;
            return getActivitityNumber() - modification.getActivitityNumber();
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof UCMModification)) {
                return false;
            }

            UCMModification modification = (UCMModification) o;
            return getActivitityNumber() == modification.getActivitityNumber();
        }

        public int hashCode() {
            return getActivitityNumber();
        }

        private int getActivitityNumber() {
            return Integer.parseInt(revision);
        }

        UCMModification() {
            super("ucm");
        }

        public Element toElement() {
            Element modificationElement = super.toElement();
            Element crmtypeElement = new Element(TAGNAME_CRMTYPE);
            crmtypeElement.addContent(crmtype);
            modificationElement.addContent(crmtypeElement);
            return modificationElement;
        }

    }

}
