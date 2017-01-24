/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Locale;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.DescriptionFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.util.DiscardConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamLogger;

import org.apache.log4j.Logger;

/**
 * This class implements the SourceControlElement methods for a Clear Case
 * repository.
 *
 * @author Thomas Leseney
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Eric Lefevre
 * @author Ralf Krakowski
 */
@DescriptionFile
public class ClearCase implements SourceControl {
    private static final int DEFAULT = 0;
    private static final int DISABLED = 1;
    private static final int ENABLED = 2;

    private static final Logger LOG = Logger.getLogger(ClearCase.class);

    private final SourceControlProperties properties = new SourceControlProperties();

    /**
     * The path of the clear case view
     */
    private String viewPath;

    /**
     * The branch to check for modifications
     */
    private String branch;
    private int recursive = DEFAULT; // default is true
    private int all = DEFAULT; // default is false

    /**
     * Date format required by commands passed to Clear Case
     * As per: http://jira.public.thoughtworks.org/browse/CC-818
     * The cleartool command line always take date in US format and does not mind of the
     * language set in the operating System.
     */
    private final SimpleDateFormat inDateFormatter =
            new SimpleDateFormat("dd-MMMM-yyyy.HH:mm:ss", Locale.US);

    /**
     * Date format returned in the output of Clear Case commands.
     */
    private final SimpleDateFormat outDateFormatter =
            new SimpleDateFormat("yyyyMMdd.HHmmss");

    /**
     * Unlikely combination of characters to separate fields in a ClearCase query
     */
    static final String DELIMITER = "#~#";

    /**
     * Even more unlikely combination of characters to indicate end of one line in query.
     * Carriage return (\n) can be used in comments and so is not available to us.
     */
    static final String END_OF_STRING_DELIMITER = "@#@#@#@#@#@#@#@#@#@#@#@";

    /**
     * Sets the local working copy to use when making queries.
     *
     * @param path the local working copy to use when making queries.
     */
    @Description("Local working copy to use when making queries")
    @Required
    public void setViewpath(final String path) {
        //_viewPath = getAntTask().getProject().resolveFile(path).getAbsolutePath();
        viewPath = new File(path).getAbsolutePath();
    }

    /**
     * Sets the branch that we're concerned about checking files into.
     *
     * @param branch the branch that we're concerned about checking files into.
     */
    @Description("The ClearCase branch")
    @Required
    public void setBranch(final String branch) {
        this.branch = branch;
    }

    /**
     * Set whether to check against sub-folders in the view path
     * @param recursive whether to check against sub-folders in the view path
     */
    @Description("Whether to check sub-folders in the viewpath.")
    @Optional
    @Default("true")
    public void setRecursive(final boolean recursive) {
        this.recursive = recursive ? ENABLED : DISABLED;
    }


    /**
     * Set when checking the entire view path.
     * <p>
     * When checking the entire view path this option invokes 'lshistory -all'
     * instead of 'lshistory -recursive', which is much faster.
     * <p>
     * This option is mutually exclusive with the recursive property.
     * <p>
     * Note that 'all' does not use your view's config-spec rules. It behaves
     * like having a single line config-spec that selects just ELEMENT * /{@code <branch>}/LATEST
     * (i.e. 'lshistory -all' results that contain @@ are discarded). This differs from
     * 'recurse', which only shows items selected by your current view.
     * @param all true when checking the entire view path
     */
    @Description("Set when checking the entire view path. When checking the entire view "
            + "path this option invokes 'lshistory -all' instead of 'lshistory -recursive', "
            + "which is much faster.<br/><br/> This option is mutually exclusive with the "
            + "recursive property.<br/><br/> Note that 'all' does not use your view's "
            + "config-spec rules. It behaves like having a single line config-spec that "
            + "selects just ELEMENT * /&lt;branch&gt;/LATEST (i.e. 'lshistory -all' results "
            + "that contain @@ are discarded). This differs from 'recurse', which only "
            + "shows items selected by your current view.")
    @Optional
    @Default("false")
    public void setAll(final boolean all) {
        this.all = all ? ENABLED : DISABLED;

        if (this.recursive == DEFAULT && all) {
            this.recursive = DISABLED;
        }
    }

    @Description("Will set this property if a modification has occurred. For use in "
            + "conditionally controlling the build later.")
    @Optional
    public void setProperty(final String property) {
        properties.assignPropertyName(property);
    }

    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(viewPath, "viewpath", this.getClass());
        if (recursive == ENABLED && all == ENABLED) {
            ValidationHelper.fail("'recursive' and 'all' are mutually exclusive attributes for ClearCase");
        }
    }

    /**
     * Returns an {@link java.util.List List} of {@link ClearCaseModification}
     * detailing all the changes between now and the last build.
     *
     * @param lastBuild the last build time
     * @param now       time now, or time to check, NOT USED
     * @return the list of modifications, an empty (not null) list if no
     *         modifications.
     */
    public List<Modification> getModifications(final Date lastBuild, final Date now) {
        final String lastBuildDate = inDateFormatter.format(lastBuild);
        properties.put("clearcaselastbuild", lastBuildDate);
        properties.put("clearcasenow", inDateFormatter.format(now));

        /*
         * let's try a different clearcase command--this one just takes
             * waaaaaaaay too long.
         * String command = "cleartool find " + _viewPath +
         * " -type f -exec \"cleartool lshistory" +
         * " -since " + lastBuildDate;
         * if(_branch != null)
         * command += " -branch " + _branch;
         * command += " -nco" + // exclude check out events
         * " -fmt \\\" %u;%Nd;%n;%o \\n \\\" \\\"%CLEARCASE_XPN%\\\" \"";
         */
        String command = "cleartool lshistory";

        if (branch != null) {
            command += " -branch " + branch;
        }

        if (recursive == DEFAULT || recursive == ENABLED) {
            command += " -r";
        } else if (all == ENABLED) {
            command += " -all";
        }

        command += " -nco -since " + lastBuildDate;
        command += " -fmt %u"
                + DELIMITER
                + "%Nd"
                + DELIMITER
                + "%En"
                + DELIMITER
                + "%Vn"
                + DELIMITER
                + "%o"
                + DELIMITER
                + "!%l"
                + DELIMITER
                + "!%a"
                + DELIMITER
                + "%Nc"
                + END_OF_STRING_DELIMITER
                + "\\n";

        final File root = new File(viewPath);

        LOG.info("ClearCase: getting modifications for " + viewPath);

        LOG.debug("Command to execute : " + command);
        List<Modification> modifications = null;
        try {
            Process p = Runtime.getRuntime().exec(command, null, root);
            p.getOutputStream().close();

            Thread stderr = logErrorStream(p);

            InputStream input = p.getInputStream();
            modifications = parseStream(input);

            getRidOfLeftoverData(input);
            p.waitFor();
            stderr.join();
            IO.close(p);
        } catch (Exception e) {
            LOG.error("Error in executing the Clear Case command : ", e);
        }

        if (modifications == null) {
            modifications = new ArrayList<Modification>();
        }

        return modifications;
    }

    private Thread logErrorStream(final Process process) {
        final Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, process));
        stderr.start();
        return stderr;
    }

    private void getRidOfLeftoverData(final InputStream stream) {
        new StreamPumper(stream, new DiscardConsumer()).run();
    }

    /**
     * Parses the input stream to construct the modifications list.
     * Package-private to make it available to the unit test.
     *
     * @param input the stream to parse
     * @return a list of modification elements
     * @throws IOException if something breaks
     */
    List<Modification> parseStream(final InputStream input) throws IOException {
        final ArrayList<Modification> modifications = new ArrayList<Modification>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        final String ls = System.getProperty("line.separator");

        String line;
        String lines = "";

        while ((line = reader.readLine()) != null) {
            if (!lines.equals("")) {
                lines += ls;
            }
            lines += line;
            ClearCaseModification mod = null;
            if (lines.indexOf(END_OF_STRING_DELIMITER) > -1) {
                mod = parseEntry(lines.substring(0, lines.indexOf(END_OF_STRING_DELIMITER)));
                lines = "";
            }
            if (mod != null) {
                modifications.add(mod);
            }
        }
        return modifications;
    }

    /**
     * Parses a single line from the reader. Each line contains a single revision
     * with the format : <br>
     * username#~#date_of_revision#~#element_name#~#operation_type#~#comments  <br>
     *
     * @param line the line to parse
     * @return a modification element corresponding to the given line
     */
    private ClearCaseModification parseEntry(final String line) {
        LOG.debug("parsing entry: " + line);
        final String[] tokens = tokeniseEntry(line);
        if (tokens == null) {
            return null;
        }
        final String username = tokens[0].trim();

        final String timeStamp = tokens[1].trim();
        final String elementName = tokens[2].trim();
        final String version = tokens[3].trim();
        final String operationType = tokens[4].trim();

        final String labelList = tokens[5].substring(1).trim();
        final List<String> labels = extractLabelsList(labelList);

        final String attributeList = tokens[6].substring(1).trim();
        final Hashtable<String, String> attributes = extractAttributesMap(attributeList);

        final String comment = tokens[7].trim();

        // A branch event shouldn't trigger a build
        if (operationType.equals("mkbranch") || operationType.equals("rmbranch")) {
            return null;
        }

        // Element names that contain @@ are discarded (see setAll(boolean))
        if (elementName.indexOf("@@") >= 0) {
            return null;
        }

        final ClearCaseModification mod = new ClearCaseModification();

        mod.userName = username;
        mod.revision = version;

        final String folderName, fileName;
        final int sep = elementName.lastIndexOf(File.separator);
        if (sep > -1) {
            folderName = elementName.substring(0, sep);
            fileName = elementName.substring(sep + 1);
        } else {
            folderName = null;
            fileName = elementName;
        }
        final ClearCaseModification.ModifiedFile modfile = mod.createModifiedFile(fileName, folderName);

        try {
            mod.modifiedTime = outDateFormatter.parse(timeStamp);
        } catch (ParseException e) {
            mod.modifiedTime = null;
        }

        modfile.action = operationType;
        modfile.revision = version;

        mod.type = "clearcase";
        mod.labels = labels;
        mod.attributes = attributes;

        mod.comment = comment;
        properties.modificationFound();

        // TODO: check if operation type is a delete

        return mod;
    }

    private String[] tokeniseEntry(final String line) {
        final int maxTokens = 8;
        final int minTokens = maxTokens - 1; // comment may be absent.
        final String[] tokens = new String[maxTokens];
        Arrays.fill(tokens, "");
        int tokenIndex = 0;
        for (int oldIndex = 0, index = line.indexOf(DELIMITER, 0); true;
             oldIndex = index + DELIMITER.length(), index = line.indexOf(DELIMITER, oldIndex), tokenIndex++) {
            if (tokenIndex > maxTokens) {
                LOG.debug("Too many tokens; skipping entry");
                return null;
            }
            if (index == -1) {
                tokens[tokenIndex] = line.substring(oldIndex);
                break;
            } else {
                tokens[tokenIndex] = line.substring(oldIndex, index);
            }
        }
        if (tokenIndex < minTokens) {
            LOG.debug("Not enough tokens; skipping entry");
            return null;
        }
        return tokens;
    }

    /**
     * @param attributeList attribute list
     * @return parsed list
     */
    private Hashtable<String, String> extractAttributesMap(final String attributeList) {
        Hashtable<String, String> attributes = null;
        if (attributeList.length() > 0) {
            attributes = new Hashtable<String, String>();
            StringTokenizer attrST = new StringTokenizer(attributeList, "(), ");
            while (attrST.hasMoreTokens()) {
                String attr = attrST.nextToken();
                int idx = attr.indexOf('=');
                if (idx > 0) {
                    String attrName = attr.substring(0, idx);
                    String attrValue = attr.substring(idx + 1);
                    if (attrValue.startsWith("\"")) {
                        attrValue = attrValue.substring(1, attrValue.length() - 1);
                    }
                    attributes.put(attrName, attrValue);
                }
            }
        }
        return attributes;
    }

    /**
     * @param labelList label list
     * @return parsed list
     */
    private List<String> extractLabelsList(final String labelList) {
        List<String> labels = null;
        if (labelList.length() > 0) {
            labels = new ArrayList<String>();
            final StringTokenizer labelST = new StringTokenizer(labelList, "(), ");
            while (labelST.hasMoreTokens()) {
                labels.add(labelST.nextToken().trim());
            }
        }
        return labels;
    }
}
