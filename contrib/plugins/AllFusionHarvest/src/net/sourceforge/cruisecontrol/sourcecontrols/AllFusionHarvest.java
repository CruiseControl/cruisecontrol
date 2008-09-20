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

import com.trinem.harvest.hsdkwrap.JCaConstWrap;
import com.trinem.harvest.hsdkwrap.JCaContextWrap;
import com.trinem.harvest.hsdkwrap.JCaHarvestWrap;
import com.trinem.harvest.hsdkwrap.JCaHarvestLogStreamWrap;
import com.trinem.harvest.hsdkwrap.JCaVersionChooserWrap;
import com.trinem.harvest.hsdkwrap.IJCaLogStreamListenerImpl;
import com.trinem.harvest.hsdkwrap.hutils.JCaAttrKeyWrap;
import com.trinem.harvest.hsdkwrap.hutils.JCaContainerWrap;
import com.trinem.harvest.hsdkwrap.hutils.JCaHarvestExceptionWrap;
import com.trinem.harvest.hsdkwrap.hutils.JCaTimeStampWrap;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SourceControl for CA's AllFusion Harvest Change Manager
 *
 * @author <a href="mailto:nayan@chikli.com">Nayan Hajratwala</a>
 * @author <a href="mailto:info@trinem.com">Trinem Consulting Ltd</a>
 */
public class AllFusionHarvest implements SourceControl {

    private JCaHarvestWrap harvest = null;

    private String broker = null;
    private String username = null;
    private String password = null;

    private String project = null;
    private String state = null;

    private String property = null;
    private String propertyOnDelete = null;

    private Map properties = new HashMap();

    private boolean loggedIn = false;

    private static Calendar gc = GregorianCalendar.getInstance();
    private static Map userEmailMapping = new HashMap();

    private JCaHarvestLogStreamWrap logstream = null;

    private static final Logger LOG = Logger.getLogger(AllFusionHarvest.class);

    /**
     * Default contructor. Creates a new uninitialise Bootstrapper.
     */
    public AllFusionHarvest() {
    }

    // ------------------------------------------------------------------------
    // Property accessors
    // ------------------------------------------------------------------------

    /**
     * Sets the Harvest Broker for all calls to HSDK.
     *
     * @param broker
     *            Harvest Broker to use.
     */
    public void setBroker(String broker) {
        LOG.debug("Broker: " + broker);
        this.broker = broker;
    }

    /**
     * Sets the Harvest username for all calls to HSDK.
     *
     * @param username
     *            Harvest username to use.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the Harvest password for all calls to HSDK.
     *
     * @param password
     *            Harvest password to use.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the Harvest project for all calls to HSDK.
     *
     * @param project
     *            Harvest project to use.
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Sets the Harvest state for all calls to HSDK.
     *
     * @param state
     *            Harvest state to use.
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Sets the name of the property to set if a modification is detected.
     *
     * @param property
     *            The name of the property.
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Sets the name of the property to set if a file has been deleted.
     *
     * @param propertyOnDelete
     *            The name of the property.
     */
    public void setPropertyOnDelete(String propertyOnDelete) {
        this.propertyOnDelete = propertyOnDelete;
    }

    // ------------------------------------------------------------------------
    // SourceControl implementation methods
    // ------------------------------------------------------------------------

    // From SourceControl
    public Map getProperties() {
        return properties;
    }

    // From SourceControl
    /**
     * Standard Bootstrapper validation method. Throws an exception if any of
     * the required properties are not set.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(username, "username", this.getClass());
        ValidationHelper.assertIsSet(password, "password", this.getClass());
        ValidationHelper.assertIsSet(broker, "broker", this.getClass());
        ValidationHelper.assertIsSet(state, "state", this.getClass());
        ValidationHelper.assertIsSet(project, "project", this.getClass());
    }

    /**
     * Returns a List of Modifications detailing all the changes between the
     * last build and the latest revision at the repository
     *
     * @param lastBuild
     *            last build time
     * @return maybe empty, never null.
     */
    public List getModifications(Date lastBuild, Date now) {

        LOG.debug("getModifications( " + lastBuild + ", " + now + " )");

        if (!login()) {
            return new ArrayList();
        }

        List list = new ArrayList();

        try {
            JCaContainerWrap versionList = getVersionsInRange(lastBuild, now);

            // This test is critical, as sometimes the count throws an exception
            int numVers = versionList.isEmpty() ? 0 : versionList.getKeyElementCount(JCaAttrKeyWrap.CA_ATTRKEY_NAME);

            for (int n = 0; n < numVers; n++) {
                String status = versionList.getString(JCaAttrKeyWrap.CA_ATTRKEY_VERSION_STATUS, n);

                // Don't add reserved tagged files - the file hasn't actually
                // changed
                if (!status.equals("R")) {
                    list.add(transformJCaVersionContainerToModification(versionList, n));
                }
            }
        } catch (JCaHarvestExceptionWrap e) {
            LOG.error(e.getMessage());
        }

        return list;
    }

    // ------------------------------------------------------------------------
    // Support code
    // ------------------------------------------------------------------------

    /**
     * Returns all the versions that were checked in between two dates.
     *
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return an container of properties representing the versions between the
     *         specified dates
     * @throws JCaHarvestException
     */
    private JCaContainerWrap getVersionsInRange(Date startDate, Date endDate) throws JCaHarvestExceptionWrap {

        JCaContextWrap context = harvest.getContext();

        context.setProject(project);
        context.setState(state);

        JCaVersionChooserWrap vc = context.getVersionChooser();

        vc.clear();
        vc.setRecursive(true);
        vc.setVersionItemOption(JCaConstWrap.VERSION_FILTER_ITEM_BOTH);
        vc.setVersionOption(JCaConstWrap.VERSION_FILTER_LATEST_IN_VIEW);
        vc.setVersionStatusOption(JCaConstWrap.VERSION_FILTER_ALL_TAG);
        vc.setBranchOption(JCaConstWrap.BRANCH_FILTER_TRUNK_ONLY);
        vc.setVersionDateOption(JCaConstWrap.VERSION_OPTION_DATE_BETWEEN);
        vc.setFromDate(convertDateToJCaTimeStamp(startDate));
        vc.setToDate(convertDateToJCaTimeStamp(endDate));

        vc.execute();

        return vc.getVersionList();
    }

    /**
     * Takes a Date object and converts it into a JCaTimeStamp
     *
     * @param date
     *            the date to be converted
     * @return the date as a JCaTimeStamp
     */
    private JCaTimeStampWrap convertDateToJCaTimeStamp(Date date) {
        gc.setTime(date);

        return new JCaTimeStampWrap(gc.get(Calendar.YEAR), gc.get(Calendar.MONTH) - Calendar.JANUARY + 1, gc
                .get(Calendar.DAY_OF_MONTH), gc.get(Calendar.HOUR_OF_DAY), gc.get(Calendar.MINUTE), gc
                .get(Calendar.SECOND), gc.get(Calendar.MILLISECOND));
    }

    /**
     * Transforms a set of version properties into a CruiseControl Modification
     * object.
     *
     * @param versionList
     *            a set of version information properties
     * @param n
     *            the index of the property information to use
     * @return a Modification object representing the change
     */
    protected Modification transformJCaVersionContainerToModification(JCaContainerWrap versionList, int n) {

        Modification mod = new Modification("harvest");
        mod.revision = versionList.getString(JCaAttrKeyWrap.CA_ATTRKEY_MAPPED_VERSION_NAME, n);

        Modification.ModifiedFile modfile = mod.createModifiedFile(versionList.getString(
                JCaAttrKeyWrap.CA_ATTRKEY_NAME, n), versionList.getString(JCaAttrKeyWrap.CA_ATTRKEY_FULL_PATH_NAME, n));
        modfile.revision = mod.revision;

        JCaTimeStampWrap created = versionList.getTimeStamp(JCaAttrKeyWrap.CA_ATTRKEY_MODIFIED_TIME, n);
        mod.modifiedTime = created.toDate();

        mod.userName = versionList.getString(JCaAttrKeyWrap.CA_ATTRKEY_MODIFIER_NAME, n);
        mod.emailAddress = getEmailAddress(mod.userName);
        mod.comment = versionList.getString(JCaAttrKeyWrap.CA_ATTRKEY_DESCRIPTION, n);

        String status = versionList.getString(JCaAttrKeyWrap.CA_ATTRKEY_VERSION_STATUS, n);

        if (status.equals("N")) {
            // If this is the first revision, then the file has been newly added
            if (mod.revision.equals("0")) {
                modfile.action = "added";
            } else {
                modfile.action = "modified";
            }

        } else if (status.equals("D")) {
            modfile.action = "deleted";
            if (propertyOnDelete != null) {
                properties.put(propertyOnDelete, "true");
            }
        } else if (status.equals("R")) {
            modfile.action = "reserved";
        } else if (status.equals("M")) {
            modfile.action = "merge_tagged";
        }

        if (property != null) {
            properties.put(property, "true");
        }

        return mod;
    }

    /**
     * Internal method which connects to Harvest using the details provided.
     */
    protected boolean login() {
        if (loggedIn) {
            return true;
        }

        harvest = new JCaHarvestWrap(broker);

        logstream = new JCaHarvestLogStreamWrap();
        logstream.addLogStreamListener(new MyLogStreamListener());

        harvest.setStaticLog(logstream);
        harvest.setLog(logstream);

        if (harvest.login(username, password) != 0) {
            LOG.error("Login failed: " + harvest.getLastMessage());
            return false;
        }

        loggedIn = true;
        return true;
    }

    /**
     * Internal method which disconnects from Harvest.
     */
    protected void logout() {
        try {
            harvest.logout();
            loggedIn = false;
        } catch (JCaHarvestExceptionWrap e) {
            LOG.error(e.getMessage());
        }
    }

    /**
     * Returns an email address for a given username as defined in Harvest.
     *
     * @param username
     *            a username
     * @return the email address corresponding to the username
     */
    private String getEmailAddress(String username) {

        try {
            String emailAddress = (String) userEmailMapping.get(username);

            // If we couldn't find the email address, it's probably the first
            // time we're trying
            // or it's a new one, so just reload the list.
            if (emailAddress == null) {

                if (!login()) {
                    return null;
                }

                userEmailMapping.clear();
                JCaContainerWrap userList = harvest.getUserList();
                int iNumUsers = userList.getKeyElementCount(JCaAttrKeyWrap.CA_ATTRKEY_NAME);
                for (int i = 0; i < iNumUsers; i++) {
                    userEmailMapping.put(userList.getString(JCaAttrKeyWrap.CA_ATTRKEY_NAME, i), userList.getString(
                            JCaAttrKeyWrap.CA_ATTRKEY_EMAIL, i));
                }

                emailAddress = (String) userEmailMapping.get(username);
            }

            return emailAddress;
        } catch (JCaHarvestExceptionWrap e) {
            LOG.error(e.getMessage());
        }

        return null;
    }

    /**
     * This is an accessor is only intended to be used for testing. It inserts a
     * dummy entry into the userEmailMapping table.
     *
     * @param username
     *            The name of the user who's email address is being set
     * @param emailAddress
     *            The corresponding email address of that user
     */
    protected void setEmailAddress(String username, String emailAddress) {
        userEmailMapping.put(username, emailAddress);
    }

    /**
     * This class implements a Harvest LOG stream listener and takes messages
     * from Harvest and gives them appropriate LOG levels in the Log4J stream
     * for the AllFusionHarvest sourcecontrol. Without this class you would not
     * see errors from Harvest, nor would warnings and info messages be handled
     * correctly.
     *
     * @author <a href="mailto:info@trinem.com">Trinem Consulting Ltd</a>
     */
    public class MyLogStreamListener implements IJCaLogStreamListenerImpl {

        // From IJCaLogStreamListenerImpl
        /**
         * Takes the given message from Harvest, figures out its severity and
         * reports it back to CruiseControl.
         *
         * @param message
         *            The message to process.
         */
        public void handleMessage(String message) {
            int level = JCaHarvestLogStreamWrap.getSeverityLevel(message);

            // Convert Harvest level to log4j level
            switch (level) {
            case JCaHarvestLogStreamWrap.OK:
                LOG.debug(message);
                break;
            case JCaHarvestLogStreamWrap.INFO:
                LOG.info(message);
                break;
            case JCaHarvestLogStreamWrap.WARNING:
                LOG.warn(message);
                break;
            case JCaHarvestLogStreamWrap.ERROR:
                LOG.error(message);
                break;
            default:
            }
        }
    }
}
