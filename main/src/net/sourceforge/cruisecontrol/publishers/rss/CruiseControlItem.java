/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers.rss;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import net.sourceforge.cruisecontrol.util.DateUtil;
import org.apache.log4j.Logger;

/**
 *  A generic RSS Feed Item (includes the contents of an RSS feed between the
 *  &lt:item&gt; and &lt;/item&gt; tags).
 *
 *  Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 *  @author Patrick Conant
 */
public class CruiseControlItem extends Item {

    private static final Logger LOG = Logger.getLogger(CruiseControlItem.class);

    /**
     *  Construct from an XMLLogHelper.
     */
    public CruiseControlItem(final XMLLogHelper logHelper, final String buildResultsURL) throws CruiseControlException {
        super();
        this.setTitle(createTitle(logHelper));
        this.setLink(createLink(logHelper, buildResultsURL));
        this.setDescription(createDescription(logHelper));
        try {
            this.setPublishDate(DateUtil.parseFormattedTime(logHelper.getBuildTimestamp(), "cctimestamp"));
        } catch (CruiseControlException ccex) {
            // default to the current date -- won't be too far off.
            this.setPublishDate(new Date());
        }
    }




    /**
     *  Create a title based on the contents of an XML log file.  This method
     *  is largely copied from the e-mail publisher classes.
     */
    private String createTitle(final XMLLogHelper logHelper) throws CruiseControlException {

        final StringBuffer title = new StringBuffer();
        title.append(logHelper.getProjectName());
        if (logHelper.isBuildSuccessful()) {
            final String label = logHelper.getLabel();
            if (label.length() > 0) {
                title.append(" ");
                title.append(label);
            }

            //  Anytime the build is "fixed" the title line
            //  should read "fixed".
            if (logHelper.isBuildFix()) {
                title.append(" Build Fixed");
            } else {
                title.append(" Build Successful");
            }
        } else {
            title.append(" Build Failed");
        }
        return title.toString();
    }

    /**
     *  Create a link to the build results URL based on the contents of the
     *  XML log file.  THis method is borrowed from the email publisher classes.
     */
    private String createLink(final XMLLogHelper logHelper, final String buildResultsURL)
            throws CruiseControlException {

        if (buildResultsURL == null) {
            return "";
        }
        final String logFileName = logHelper.getLogFileName();

        final int startName = logFileName.lastIndexOf(File.separator) + 1;
        final int endName = logFileName.lastIndexOf(".");
        final String baseLogFileName = logFileName.substring(startName, endName);
        final StringBuffer url = new StringBuffer(buildResultsURL);

        if (buildResultsURL.indexOf("?") == -1) {
            url.append("?");
        } else {
            url.append("&");
        }
        url.append("log=");
        url.append(baseLogFileName);

        return url.toString();
    }

    /**
     * To compare modifications happening in the same project.
     */
    static class ModificationComparator implements Comparator<Modification> {
        public int compare(final Modification mod1, final Modification mod2) {
            final long modifiedTimeDifference = mod1.modifiedTime.getTime() - mod2.modifiedTime.getTime();
            if (modifiedTimeDifference != 0) {
                return modifiedTimeDifference > 0 ? +1 : -1;
            }
            return mod1.getFileName().compareTo(mod2.getFileName());
        }
    }

    private String createDescription(final XMLLogHelper logHelper) throws CruiseControlException {
        final StringBuffer description = new StringBuffer();

        // Write out the build time and label
        description.append("<em>Build Time:</em> ");
        try {
            description.append(DateUtil.parseFormattedTime(logHelper.getBuildTimestamp(), "cctimestamp"));
        } catch (CruiseControlException ccex) {
            LOG.error("exception trying to resolve cctimestamp", ccex);
            description.append("not available");
        } catch (NullPointerException npe) { // FIXME why is that possible?
            LOG.error("NPE trying to resolve cctimestamp", npe);
            description.append("not available");
        }

        description.append("<br/>");

        description.append("<em>Label:</em> ");
        if (logHelper.getLabel() != null) {
            description.append(logHelper.getLabel());
        }
        description.append("<br/>");

        // Write out all of the modifications...
        description.append("<em>Modifications: </em>");
        try {
            final List<Modification> modifications = new ArrayList<Modification>(logHelper.getModifications());
            Collections.sort(modifications, new ModificationComparator());
            description.append(modifications.size());
            final Iterator it = modifications.iterator();
            while (it.hasNext()) {
                final Modification mod = (Modification) it.next();
                description.append("<li>");
                description.append(mod.getFileName());
                description.append("  by ");
                if (mod.userName != null) {
                    description.append(mod.userName);
                } else {
                    description.append("[no user]");
                }
                description.append(" (");
                if (mod.comment != null) {
                    description.append(mod.comment);
                } else {
                    description.append("[no comment]");
                }
                description.append(")</li>");
            }
            description.append("</ul>");
        } catch (NullPointerException npe) { // FIXME
            LOG.error("NPE trying to build String representation of modifications in description", npe);
            //throws NullPointerException during tests for XMLLogHelpers
            // generated from scratch...
            description.append("0");
        }
        description.append("<br/>\n<ul>");

        return description.toString();
    }
}
