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

import com.ca.harvest.jhsdk.JCaConst;
import com.ca.harvest.jhsdk.JCaContext;
import com.ca.harvest.jhsdk.JCaHarvest;
import com.ca.harvest.jhsdk.JCaHarvestLogStream;
import com.ca.harvest.jhsdk.JCaSQL;
import com.ca.harvest.jhsdk.JCaVersionChooser;
import com.ca.harvest.jhsdk.IJCaLogStreamListener;
import com.ca.harvest.jhsdk.hutils.JCaAttrKey;
import com.ca.harvest.jhsdk.hutils.JCaContainer;
import com.ca.harvest.jhsdk.hutils.JCaHarvestException;
import com.ca.harvest.jhsdk.hutils.JCaTimeStamp;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * SourceControl for CA's AllFusion Harvest Change Manager
 *
 * @author <a href="mailto:nayan@chikli.com">Nayan Hajratwala</a>
 * @author <a href="mailto:info@trinem.com">Trinem Consulting Ltd</a>
 */
public class AllFusionHarvest implements SourceControl {

    private JCaHarvest harvest = null;

    private String broker = null;
    private String username = null;
    private String password = null;

    private String project = null;
    private String state = null;
    private String prevState = null;
    private String viewPath = null;

    private int itemOption = JCaConst.VERSION_FILTER_ITEM_BOTH;
    private int versionOption = JCaConst.VERSION_FILTER_LATEST_IN_VIEW;
    private int statusOption = JCaConst.VERSION_FILTER_ALL_TAG;
    private int branchOption = JCaConst.BRANCH_FILTER_TRUNK_ONLY;

    public static final int CHECK_VERSION_CHOOSER = 1;
    public static final int CHECK_PACKAGE_HISTORY = 2;

    private int checkMode = CHECK_VERSION_CHOOSER;

    private String property = null;
    private String propertyOnDelete = null;

    private Map properties = new HashMap();

    private boolean loggedIn = false;

    private static Calendar gc = GregorianCalendar.getInstance();
    private static HashMap userEmailMapping = new HashMap();

    private JCaHarvestLogStream logstream = null;

    private static final Logger LOG = Logger.getLogger(AllFusionHarvest.class);

    private static DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

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
     * Required.
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
     * Required.
     *
     * @param username
     *            Harvest username to use.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the Harvest password for all calls to HSDK.
     * Required.
     *
     * @param password
     *            Harvest password to use.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the Harvest project for all calls to HSDK.
     * Required.
     *
     * @param project
     *            Harvest project to use.
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Sets the Harvest state for all calls to HSDK.
     * Required.
     *
     * @param state
     *            Harvest state to use.
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Sets the previous Harvest state for package history based monitoring.
     * Optional.
     *
     * @param state
     *            Harvest state to use.
     */
    public void setPrevState(String state) {
        this.prevState = state;
    }

    /**
     * Sets the view path to use when making calls to HSDK.
     *
     * @param viewPath
     *            String indicating the view path.
     */
    public void setViewpath(String viewPath) {
        this.viewPath = viewPath;
    }

    /**
     * Sets the version item option to use when making calls to HSDK.
     * Optional.
     *
     * @param itemOption
     *            String indicating the item option.
     */
    public void setItem(String io)
            throws CruiseControlException {
        if (io.equals("baseline") || io.equals("not_modified")) {
            this.itemOption = JCaConst.VERSION_FILTER_ITEM_BASELINE;
        } else if (io.equals("modified")) {
            this.itemOption = JCaConst.VERSION_FILTER_ITEM_MODIFIED;
        } else if (io.equals("both")) {
            this.itemOption = JCaConst.VERSION_FILTER_ITEM_BOTH;
        } else {
            throw new CruiseControlException("item must be one of: "
                      + "baseline, modified, (both)");
        }
    }

    /**
     * Sets the version item option to use when making calls to HSDK.
     * Optional.
     *
     * @param versionOption
     *            String indicating the version option.
     */
    public void setVersion(String vo)
            throws CruiseControlException {
        if (vo.equals("latest_in_view")) {
            this.versionOption = JCaConst.VERSION_FILTER_LATEST_IN_VIEW;
        } else if (vo.equals("all_in_view")) {
            this.versionOption = JCaConst.VERSION_FILTER_ALL_IN_VIEW;
        } else if (vo.equals("all")) {
            this.versionOption = JCaConst.VERSION_FILTER_ALL;
        } else if (vo.equals("latest")) {
            this.versionOption = JCaConst.VERSION_FILTER_LATEST;
        } else {
            throw new CruiseControlException("version must be one of: "
                      + "(latest_in_view), all_in_view, all, latest");
        }
    }

    /**
     * Sets the version status option to use when making calls to HSDK.
     * Optional.
     *
     * @param statusOption
     *            String indicating the status option.
     */
    public void setStatus(String so)
            throws CruiseControlException {
        if (so.equals("all") || so.equals("all_tags")) {
            this.statusOption = JCaConst.VERSION_FILTER_ALL_TAG;
        } else if (so.equals("no_tag") || so.equals("normal")) {
            this.statusOption = JCaConst.VERSION_FILTER_NORMAL_VERSION;
        } else if (so.equals("reserved")) {
            this.statusOption = JCaConst.VERSION_FILTER_RESERVED_VERSION;
        } else if (so.equals("merged")) {
            this.statusOption = JCaConst.VERSION_FILTER_MERGED_VERSION;
        } else if (so.equals("removed") || so.equals("deleted")) {
            this.statusOption = JCaConst.VERSION_FILTER_DELETED_VERSION;
        } else if (so.equals("any") || so.equals("any_tag")) {
            this.statusOption = JCaConst.VERSION_FILTER_ANY_TAG;
        } else {
            throw new CruiseControlException("status must be one of: "
                      + "(all), no_tag, reserved, merged, removed, any");
        }
    }

    /**
     * Sets the version branch option to use when making calls to HSDK.
     * Optional.
     *
     * @param branchOption
     *            String indicating the branch option.
     */
    public void setBranch(String bo)
            throws CruiseControlException {
        /* Also: BRANCH_FILTER_MERGED_ONLY, BRANCH_FILTER_VCI_ONLY? */
        if (bo.equals("trunk") || bo.equals("trunk_only")) {
            this.branchOption = JCaConst.BRANCH_FILTER_TRUNK_ONLY;
        } else if (bo.equals("branch") || bo.equals("branch_only")) {
            this.branchOption = JCaConst.BRANCH_FILTER_BRANCH_ONLY;
        } else if (bo.equals("trunk_and_branch")) {
            this.branchOption = JCaConst.BRANCH_FILTER_TRUNK_AND_BRANCH;
        } else if (bo.equals("unmerged") || bo.equals("unmerged_branch")) {
            this.branchOption = JCaConst.BRANCH_FILTER_UNMERGED_ONLY;
        } else {
            throw new CruiseControlException("branch must be one of: "
                      + "(trunk), branch, trunk_and_branch, unmerged");
        }
    }

    /**
     * Sets the mode used to check for modifications. In version mode, the
     * version chooser is used to find versions visible in the selected state
     * which have been created after the last build occurred. In package mode,
     * the package history is checked to find packages which have been promoted
     * or demoted into the selected state since the last build occurred.
     * Optional.
     * 
     * @param mode
     *            String indicating the mode to use.
     */
    public void setMode(String mode)
            throws CruiseControlException {
        if (mode.equals("version")) {
            this.checkMode = CHECK_VERSION_CHOOSER;
        } else if (mode.equals("package")) {
            this.checkMode = CHECK_PACKAGE_HISTORY;
        } else {
            throw new CruiseControlException("mode must be one of: "
                      + "(version), package");
        }
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
        JCaContainer versionList = null;
        JCaContainer demotedVersionList = null;

        try {
            if (checkMode == CHECK_VERSION_CHOOSER) {
                versionList = getVersionsInRange(lastBuild, now);
            } else {
                versionList = getPromotedAndDemotedVersions(lastBuild, true);
                if (prevState != null) {
                    demotedVersionList = getPromotedAndDemotedVersions(lastBuild, false);
                }
            }

            // This test is critical, as sometimes the count throws an exception
            int numVers = (versionList == null) || versionList.isEmpty() ? 0
                    : versionList.getKeyElementCount(JCaAttrKey.CA_ATTRKEY_NAME);

            for (int n = 0; n < numVers; n++) {
                String status = versionList.getString(JCaAttrKey.CA_ATTRKEY_VERSION_STATUS, n);

                // Don't add reserved tagged files - the file hasn't actually changed
                if (!status.equals("R")) {
                    list.add(transformJCaVersionContainerToModification(versionList, n, true));
                }
            }

            // This test is critical, as sometimes the count throws an exception
            numVers = (demotedVersionList == null) || demotedVersionList.isEmpty() ? 0
                    : demotedVersionList.getKeyElementCount(JCaAttrKey.CA_ATTRKEY_NAME);

            for (int n = 0; n < numVers; n++) {
                String status = demotedVersionList.getString(JCaAttrKey.CA_ATTRKEY_VERSION_STATUS, n);

                // Don't add reserved tagged files - the file hasn't actually
                // changed
                if (!status.equals("R")) {
                    list.add(transformJCaVersionContainerToModification(demotedVersionList, n, false));
                }
            }
        } catch (JCaHarvestException e) {
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
    private JCaContainer getVersionsInRange(Date startDate, Date endDate) throws JCaHarvestException {

        JCaContext context = harvest.getContext();

        context.setProject(project);
        context.setState(state);

        JCaVersionChooser vc = context.getVersionChooser();

        vc.clear();
        vc.setRecursive(true);
        if (viewPath != null) {
            vc.setParentPath(viewPath);
        }
        vc.setVersionItemOption(itemOption);      // Defaults to JCaConst.VERSION_FILTER_ITEM_BOTH
        vc.setVersionOption(versionOption);       // Defaults to JCaConst.VERSION_FILTER_LATEST_IN_VIEW
        vc.setVersionStatusOption(statusOption);  // Defaults to JCaConst.VERSION_FILTER_ALL_TAG
        vc.setBranchOption(branchOption);         // Defaults to JCaConst.BRANCH_FILTER_TRUNK_ONLY
        vc.setVersionDateOption(JCaConst.VERSION_OPTION_DATE_BETWEEN);
        vc.setFromDate(convertDateToJCaTimeStamp(startDate));
        vc.setToDate(convertDateToJCaTimeStamp(endDate));

        vc.execute();

        return vc.getVersionList();
    }

    /**
     * Returns all the versions that were promoted or demoted after the given date.
     *
     * @param startDate
     *         the start date
     * @return an container of properties representing the versions
     * @throws JCaHarvestException
     */
    private JCaContainer getPromotedAndDemotedVersions(Date startDate, boolean promote)
            throws JCaHarvestException {

        JCaContext context = harvest.getContext();

        context.setProject(project);
        context.setState(state);

        int[] pkgobjids = null;
        JCaSQL sql = context.getSQL();
        if (promote) {
            sql.setSQLStatement("SELECT DISTINCT packageObjId FROM harPkgHistory"
                + " WHERE environmentName='" + project + "'"
                + " AND stateName='" + state + "'"
                + " AND action='Promote'"
                + " AND execdTime > '" + convertDateToDatabaseDate(startDate) + "'");
        } else {
            if (prevState.indexOf(",") != -1) {
                StringTokenizer tok = new StringTokenizer(prevState, ",");
                StringBuffer buf = new StringBuffer();
                while (tok.hasMoreTokens()) {
                    if (buf.length() > 0) {
                        buf.append("','");
                    }
                    buf.append(tok.nextToken());
                }
                prevState = buf.toString();
            }
            sql.setSQLStatement(" SELECT DISTINCT packageObjId FROM harPkgHistory"
                + " WHERE environmentName='" + project + "'"
                + " AND stateName IN ('" + prevState + "')"
                + " AND action='Demote'"
                + " AND execdTime > '" + convertDateToDatabaseDate(startDate) + "'");
        }
        sql.execute();
        JCaContainer sqlData = sql.getSQLResult();
        if (sqlData.getKeyCount() > 0) {
            int count = sqlData.getKeyElementCount("PACKAGEOBJID");
            if (count > 0) {
                pkgobjids = new int[count];
                for (int n = 0; n < count; n++) {
                   pkgobjids[n] = sqlData.getInt("PACKAGEOBJID", n);
                   LOG.info("Package " + pkgobjids[n] + " has been "
                            + (promote ? "promoted" : "demoted"));
                }
            }
        }

        if ((pkgobjids == null) || (pkgobjids.length == 0)) {
            return new JCaContainer();
        }

        JCaVersionChooser vc = context.getVersionChooser();

        vc.clear();
        vc.setRecursive(true);
        if (viewPath != null) {
            vc.setParentPath(viewPath);
        }
        vc.setVersionItemOption(itemOption);      // Defaults to JCaConst.VERSION_FILTER_ITEM_BOTH
        vc.setVersionOption(versionOption);       // Defaults to JCaConst.VERSION_FILTER_LATEST_IN_VIEW
        vc.setVersionStatusOption(statusOption);  // Defaults to JCaConst.VERSION_FILTER_ALL_TAG
        vc.setBranchOption(branchOption);         // Defaults to JCaConst.BRANCH_FILTER_TRUNK_ONLY
        for (int n = 0; n < pkgobjids.length; n++) {
            vc.setPackageObjId(pkgobjids[n], n);
        }

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
    private JCaTimeStamp convertDateToJCaTimeStamp(Date date) {
        gc.setTime(date);

        return new JCaTimeStamp(gc.get(Calendar.YEAR),
                                gc.get(Calendar.MONTH) - Calendar.JANUARY + 1,
                                gc.get(Calendar.DAY_OF_MONTH),
                                gc.get(Calendar.HOUR_OF_DAY),
                                gc.get(Calendar.MINUTE),
                                gc.get(Calendar.SECOND),
                                gc.get(Calendar.MILLISECOND));
    }

    /**
     * Takes a Date object and converts it into a String suitable for use in
     * database queries. As the database stores dates in UTC, we must also
     * convert the passed date to UTC, otherwise we may be an hour out due to
     * daylight savings times.
     *
     * @param date
     *         the date to be converted
     * @return the date as a String
     */
    private String convertDateToDatabaseDate(Date date) {

        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
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
    protected Modification transformJCaVersionContainerToModification(
            JCaContainer versionList, int n, boolean promote) {

        Modification mod = new Modification("harvest");
        mod.revision =  versionList.getString(JCaAttrKey.CA_ATTRKEY_MAPPED_VERSION_NAME, n);

        Modification.ModifiedFile modfile = mod.createModifiedFile(
            versionList.getString(JCaAttrKey.CA_ATTRKEY_NAME, n),
            versionList.getString(JCaAttrKey.CA_ATTRKEY_FULL_PATH_NAME, n));
        modfile.revision = mod.revision;

        JCaTimeStamp created = versionList.getTimeStamp(JCaAttrKey.CA_ATTRKEY_MODIFIED_TIME, n);
        mod.modifiedTime = created.toDate();

        mod.userName = versionList.getString(JCaAttrKey.CA_ATTRKEY_MODIFIER_NAME, n);
        mod.emailAddress = getEmailAddress(mod.userName);
        mod.comment = versionList.getString(JCaAttrKey.CA_ATTRKEY_DESCRIPTION, n);

        String status = versionList.getString(JCaAttrKey.CA_ATTRKEY_VERSION_STATUS, n);

        if (promote) {
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
        } else {
            // Any versions which have been demoted are effectively deleted from the state
            modfile.action = "deleted";
            if (propertyOnDelete != null) {
                properties.put(propertyOnDelete, "true");
            }
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

        harvest = new JCaHarvest(broker);

        logstream = new JCaHarvestLogStream();
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
        } catch (JCaHarvestException e) {
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
                JCaContainer userList = harvest.getUserList();
                int iNumUsers = userList.getKeyElementCount(JCaAttrKey.CA_ATTRKEY_NAME);
                for (int i = 0; i < iNumUsers; i++) {
                    userEmailMapping.put(userList.getString(JCaAttrKey.CA_ATTRKEY_NAME, i),
                                         userList.getString(JCaAttrKey.CA_ATTRKEY_EMAIL, i));
                }

                emailAddress = (String) userEmailMapping.get(username);
            }

            return emailAddress;
        } catch (JCaHarvestException e) {
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
    public class MyLogStreamListener implements IJCaLogStreamListener {

        // From IJCaLogStreamListener
        /**
         * Takes the given message from Harvest, figures out its severity and
         * reports it back to CruiseControl.
         *
         * @param message
         *            The message to process.
         */
        public void handleMessage(String message) {
            int level = JCaHarvestLogStream.getSeverityLevel(message);

            // Convert Harvest level to log4j level
            switch (level) {
            case JCaHarvestLogStream.INFO:
                LOG.info(message);
                break;
            case JCaHarvestLogStream.WARNING:
                LOG.warn(message);
                break;
            case JCaHarvestLogStream.ERROR:
                LOG.error(message);
                break;
            case JCaHarvestLogStream.OK:
            default:
                LOG.debug(message);
                break;
            }
        }
    }
}
