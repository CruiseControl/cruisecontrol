/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a ClearCase UCM baseline for the specified view's integration stream.
 * Uses the value of the CruiseControl generated ${label} property as well as the
 * value of the baselineprefix attribute (if specified) to name the baseline.
 * A baseline is only created if UCM modifications are recorded in the build log.
 * By default an incremental baseline is created although a full baseline can be
 * created too (incremental baselines are recommended for Continuous Integration).
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class ClearCaseBaselinePublisher implements Publisher {
    private boolean full = false;
    private String baselineprefix;
    private String viewtag;
    private String component;

    private static final Logger LOG = Logger.getLogger(ClearCaseBaselinePublisher.class);

    /**
     * Set the baselineprefix flag
     *
     * @param baselineprefix the status to set the flag to
     */
    public void setBaselineprefix(String baselineprefix) {
        this.baselineprefix = baselineprefix;
    } // setBaselineprefix

    /**
     * Get baselineprefix flag status
     *
     * @return String containing status of baselineprefix flag
     */
    public String getBaselineprefix() {
        return this.baselineprefix;
    } // getBaselineprefix

    /**
     * Set the viewtag status flag
     *
     * @param viewtag the status to set the flag to
     */
    public void setViewtag(String viewtag) {
        this.viewtag = viewtag;
    } // setViewtag

    /**
     * Get viewtag flag status
     *
     * @return String containing status of viewtag flag
     */
    public String getViewtag() {
        return this.viewtag;
    } // getViewtag

    /**
     * Set the full flag
     *
     * @param full the status to set the flag to
     */
    public void setFull(boolean full) {
        this.full = full;
    } // setFull

    /**
     * Get full flag status
     *
     * @return boolean containing status of full flag
     */
    public boolean getFull() {
        return this.full;
    } // getFull

    /**
     * Set the component to generate the baseline for
     *
     * @param comp the name of the component
     */
    public void setComponent(String comp) {
        this.component = comp;
    } // setComponent

    /**
     * Get the component flag status
     *
     * @return the component the baseline is to be applied to
     */
    public String getComponent() {
        return this.component;
    } // getComponent

    /*
    * check whether the appropriate attributes have been set
    */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(viewtag, "viewtag", this.getClass());
    } // validate

    /**
     * extract the list of UCM modifications from the CruiseControl log
     * (assumes the UCM sourcecontrol was used)
     *
     * @param log The Cruise Control log (as a JDOM element).
     * @return a <code>List</code> of UCM activities
     */
    public List getActivities(final Element log) {
        final List<String> activityList = new ArrayList<String>();

        // get the modification list from the log
        if (log != null) {
            final Element modifications = log.getChild("modifications");
            if (modifications != null) {
                // from this list, extract all UCM activities
                for (final Object o : modifications.getChildren("modification")) {
                    final Element modification = (Element) o;
                    final String type = modification.getAttributeValue("type");
                    if (type != null && type.equals("activity")) {
                        final String activity = modification.getChild("revision").getText();

                        if (activity != null) {
                            activityList.add(activity.trim());
                        }
                    }
                }
            }
        }
        return activityList;

    } // getActivities

    /**
     * determines if the publish should take place
     *
     * @param log the CruiseControl log (as a JDOM element).
     * @return true if the build was successful and new activities were found
     */
    public boolean shouldPublish(Element log) {
        // do not publish if no activities were found
        List newActivities = getActivities(log);
        if (newActivities.size() < 1) {
            LOG.info("No UCM activities in build. Skipping publisher.");
            return false;
        }
        return true;
    } // shouldPublish

    /* (non-Javadoc)
    * @see net.sourceforge.cruisecontrol.Publisher#publish(org.jdom.Element)
    */
    public void publish(final Element log) throws CruiseControlException {
        final XMLLogHelper helper = new XMLLogHelper(log);

        // only publish if the build includes UCM activities
        if (!shouldPublish(log)) {
            return;
        }

        // should the baselinename include the prefix?
        final String baselinename;
        if (getBaselineprefix() != null) {
            baselinename = getBaselineprefix() + helper.getLabel();
        } else {
            baselinename = helper.getLabel();
        }

        // create the "cleartool mkbl" command line
        final ManagedCommandline cmd = new ManagedCommandline();
        cmd.setExecutable("cleartool");
        cmd.createArgument("mkbl");
        cmd.createArguments("-view", getViewtag());
        if (getFull()) {
            cmd.createArgument("-full");
        }
        if (getComponent() != null) {
            cmd.createArguments("-component", getComponent());
        }
        cmd.createArgument(baselinename);

        // execute it
        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            final StringBuilder error = new StringBuilder("Failed to create baseline: ");
            error.append(baselinename);
            throw new CruiseControlException(error.toString(), e);
        }

        // log success
        final StringBuilder message = new StringBuilder("Created baseline: ");
        message.append(baselinename);
        LOG.info(message.toString());

        // TODO: update ClearQuest records/activities with build number

    } // publish

    /**
     * for testing
     * @param args command line args
     */
    public static void main(String[] args) {

        // create a fake cruisecontrol log
        Element logElement = new Element("cruisecontrol");
        // add a single modification
        Element mods = new Element("modifications");
        logElement.addContent(mods);
        Element mod = new Element("modification");
        mod.setAttribute("type", "activity");
        mods.addContent(mod);
        Element rev = new Element("revision");
        rev.addContent(new CDATA("Some activitiy"));
        mod.addContent(rev);
        // and a build label
        Element info = new Element("info");
        logElement.addContent(info);
        Element prop = new Element("property");
        prop.setAttribute("name", "label");
        prop.setAttribute("value", "1_TST");
        info.addContent(prop);

        ClearCaseBaselinePublisher baseline = new ClearCaseBaselinePublisher();
        baseline.setViewtag("RatlBankModel_int");
        //baseline.setFull(true);
        //baseline.setComponent("RATLBANKMODEL_REL@\\RatlBankProjects");
        //baseline.setbaselineprefix("testbl");
        try {
            baseline.publish(logElement);
        } catch (CruiseControlException ex) {
            ex.printStackTrace();
        }
    }

} // ClearCaseBaselinePublisher
