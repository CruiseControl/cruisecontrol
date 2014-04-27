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
package net.sourceforge.cruisecontrol.labelincrementers;

import net.sourceforge.cruisecontrol.LabelIncrementer;
import org.apache.log4j.Logger;
import org.jdom.Element;
import java.util.Set;
import java.util.HashSet;

/**
 * This class provides a label incrementation for creating consistent, formatted upper
 * case labels. This class expects the label format to be either "x_y_z" or "y_z"
 * where x is any String, y is an integer and z is one of REL, INT or BLD.
 *
 *  * Usage:
 *
 *     &lt;formattedlabelincrementer prefix="false" defaultlabel="1.INT"/%gt;
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 * @author Jeff Brekke (Jeff.Brekke@qg.com)
 * @author alden almagro (alden@thoughtworks.com)
 * @author Paul Julius (pdjulius@thoughtworks.com)
 */
public class FormattedLabelIncrementer implements LabelIncrementer {

    private static final Logger LOG =
        Logger.getLogger(DefaultLabelIncrementer.class);

    private boolean preIncrement = false;
    private boolean includePrefix = true;
    private String defaultPrefix = "CC";
    private int defaultBuildNum = 1;
    private String defaultSuffix = "INT";
    private String separator = "_";

    private Set<String> customSuffixes = new HashSet<String>(5);

    public FormattedLabelIncrementer() {
        setSeparator(separator);

        customSuffixes.add(defaultSuffix);
        customSuffixes.add("BLD");
        customSuffixes.add("REL");
    }

    /**
     * set the separtor to be use between parts of the build label, default is "_"
     * @param newSeparator the character string to use as a separator
     */
    public void setSeparator(String newSeparator) {
        separator = newSeparator;
    }
    /**
     * set the separtor to be use between parts of the build label, default is "_"
     * @param newSeparator the character string to use as a separator
     */
    public void setSuffix(String newSuffix) {
        customSuffixes.add(newSuffix);
    }

    /**
     *  Set the pre/post behavior of the label incrementer
     *  @param preInc whether to increment the build before the build, default is false
     */
    public void setPreBuildIncrementer(boolean preInc) {
        preIncrement = preInc;
    }

    /**
     * Set whether a prefix is required or no
     * @param prefix whether to include a prefix with the label, default is true
     */
    public void setPrefix(boolean prefix) {
        includePrefix = prefix;
    }

    /**
     * Get the default label
     * @return string containing the default label
     */
    public String getDefaultLabel() {
        if (includePrefix) {
            return defaultPrefix + separator
                + defaultBuildNum + separator + defaultSuffix;
        } else {
            return defaultBuildNum + separator + defaultSuffix;
        }
    }

    /**
     * Set the default label
     * @param label string to set the default label to
     */
    public void setDefaultLabel(String label) {
        LOG.debug("Setting default label: " + label);
        if (includePrefix) {
            int separatorIndex = label.lastIndexOf(separator);
            defaultSuffix = label.substring(separatorIndex + 1, label.length()).toUpperCase();
            defaultPrefix = label.substring(0, separatorIndex);
            separatorIndex = defaultPrefix.lastIndexOf(separator);
            defaultBuildNum = Integer.parseInt(
                defaultPrefix.substring(separatorIndex + 1,
                defaultPrefix.length()));
            defaultPrefix = defaultPrefix.substring(0, separatorIndex).toUpperCase();
        } else {
            defaultSuffix = label.substring(label.indexOf(separator) + 1, label.length()).toUpperCase();
            defaultBuildNum = Integer.parseInt(
                label.substring(0, label.indexOf(separator)));
        }
    }

    /**
     * Checks whether the label should be incremented pre/post build
     * @return true if the label will be incremented before the build, else false
     */
    public boolean isPreBuildIncrementer() {
        return preIncrement;
    }

    /**
     * Increments the label when a successful build occurs.
     * Assumes that the label will be in the format of "x_y_z" or "y_z",
     * where x can be anything, y is an integer and z is one of REL, INT or BLD
     * The y value will be incremented by one, the rest will remain the same.
     * The label is converted to uppercase by default.
     *
     * @param oldLabel Label from previous successful build.
     * @return Label to use for most recent successful build.
     */
    public String incrementLabel(String oldLabel, Element buildLog) {
        String newLabel;

        if (includePrefix) {
            String prefix1 = oldLabel.substring(0, oldLabel.lastIndexOf(separator));
            String prefix2 = prefix1.substring(0, prefix1.lastIndexOf(separator));
            String suffix  = oldLabel.substring(
                oldLabel.lastIndexOf(separator) + 1,
                oldLabel.length());
            String buildnum = prefix1.substring(
                prefix1.lastIndexOf(separator) + 1,
                prefix1.length());
            int i = Integer.parseInt(buildnum);
            newLabel = prefix2.toUpperCase() + separator
                + ++i + separator + suffix.toUpperCase();
        } else {
            String suffix  = oldLabel.substring(
                    oldLabel.lastIndexOf(separator) + 1,
                    oldLabel.length());
            String buildnum = oldLabel.substring(
                    0, oldLabel.indexOf(separator));
            int i = Integer.parseInt(buildnum);
            newLabel = ++i + separator + suffix.toUpperCase();
        }
        LOG.debug("Incrementing label: " + oldLabel + " -> " + newLabel);
        return newLabel;
    }

    /**
     * Verify that the label specified is a valid label.  In this case a valid
     * label contains at least one '_' character, and an integer after the last
     * but one occurrence of the '_' character, followed by REL, INT or BLD
     *
     * @param label the label to check for validity
     * @return true if label is valid, else false
     */
    public boolean isValidLabel(String label) {

        // the label does not include a separator
        if (label.indexOf(separator) < 0) {
            return false;
        }

        try {
            String suffix;
            String buildnum;

            // check for label format
            if (includePrefix) {
                String prefix1 = label.substring(0, label.lastIndexOf(separator));
                suffix = label.substring(label.lastIndexOf(separator) + 1,
                    label.length());
                buildnum = prefix1.substring(prefix1.lastIndexOf(separator) + 1,
                    prefix1.length());
            } else {
                suffix = label.substring(label.lastIndexOf(separator) + 1,
                    label.length());
                buildnum = label.substring(0, label.indexOf(separator));
            }

            // check for consistent suffix
            if (customSuffixes.contains(suffix)) { //.equals("BLD") || suffix.equals("INT") || suffix.equals("REL")) {
                Integer.parseInt(buildnum);
                return true;
            } else { 
                return false; 
            }
        } catch (NumberFormatException e) {
            return false;
        } catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

}
