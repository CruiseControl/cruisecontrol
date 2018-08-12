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
import org.jdom2.Element;

/**
 * This class provides a default label incrementation.
 * This class expects the label format to be {@code "x<sep>y"},
 * where x is any String and y is an integer and {@code <sep>} a separator.
 * The default separator is "." and can be modified using {@link #setSeparator}.
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public class DefaultLabelIncrementer implements LabelIncrementer {

    private static final Logger LOG =
        Logger.getLogger(DefaultLabelIncrementer.class);

    private boolean preIncrement = false;

    private String separator = ".";

    private String defaultPrefix = "build";
    private int defaultSuffix = 1;
    /* temporary variable, only used at instance setup time */
    private String defaultLabel = null;

    /**
     * Increments the label when a successful build occurs.
     * Assumes that the label will be in
     * the format of "x.y", where x can be anything, and y is an integer.
     * The y value will be incremented by one, the rest will remain the same.
     *
     * @param oldLabel Label from previous successful build.
     * @return Label to use for most recent successful build.
     */
    public String incrementLabel(String oldLabel, Element buildLog) {
        String prefix =
            oldLabel.substring(0, oldLabel.lastIndexOf(separator) + 1);
        String suffix =
            oldLabel.substring(
                oldLabel.lastIndexOf(separator) + 1,
                oldLabel.length());
        int i = Integer.parseInt(suffix);
        String newLabel = prefix + ++i;
        LOG.debug("Incrementing label: " + oldLabel + " -> " + newLabel);
        return newLabel;
    }

    public boolean isPreBuildIncrementer() {
        return preIncrement;
    }

    /**
     *  Set the pre/post behavior of the label incrementer.
     */
    public void setPreBuildIncrementer(boolean preInc) {
        preIncrement = preInc;
    }

    /**
     * Verify that the label specified is a valid label.  In this case a valid
     * label contains at least one separator character, and an integer after the last
     * occurrence of the separator character.
     */
    public boolean isValidLabel(String label) {

        if (label.indexOf(separator) < 0) {
            return false;
        }

        try {
            String suffix =
                label.substring(
                    label.lastIndexOf(separator) + 1,
                    label.length());
            Integer.parseInt(suffix);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void setSeparator(String newSeparator) {
        separator = newSeparator;
    }

    /**
     * The instance must be fully initialized before calling this method.
     * @throws IllegalStateException if the instance is not properly initialized
     * e.g. if the {@link #setSeparator set separator} doesn't match the
     * {@link #setDefaultLabel set default label}
     */
    public String getDefaultLabel() {
        if (defaultLabel != null) {
            final int separatorIndex = defaultLabel.lastIndexOf(separator);
            if (separatorIndex == -1) {
                 throw new IllegalStateException("separator \"" + separator
                       + "\" not found in default Label " + defaultLabel);
            }
            defaultPrefix = defaultLabel.substring(0, separatorIndex);
            String suffix = defaultLabel.substring(separatorIndex + 1);
            defaultSuffix = Integer.parseInt(suffix);
            defaultLabel = null;
        }
        return defaultPrefix + separator + defaultSuffix;
    }

    public void setDefaultLabel(String label) {
        defaultLabel = label;
    }
}
