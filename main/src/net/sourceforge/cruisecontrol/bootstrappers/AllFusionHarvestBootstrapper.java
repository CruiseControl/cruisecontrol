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
package net.sourceforge.cruisecontrol.bootstrappers;

import com.trinem.harvest.hsdkwrap.JCaCheckoutWrap;
import com.trinem.harvest.hsdkwrap.JCaConstWrap;
import com.trinem.harvest.hsdkwrap.JCaContextWrap;
import com.trinem.harvest.hsdkwrap.JCaHarvestWrap;
import com.trinem.harvest.hsdkwrap.JCaHarvestLogStreamWrap;
import com.trinem.harvest.hsdkwrap.JCaVersionChooserWrap;
import com.trinem.harvest.hsdkwrap.IJCaLogStreamListenerImpl; // import
                                                                // com.trinem.harvest.hsdkwrap.hutils.JCaAttrKeyWrap;
import com.trinem.harvest.hsdkwrap.hutils.JCaHarvestExceptionWrap;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * BootStrapper for CA's AllFusion Harvest Change Manager.
 *
 * This BootStrapper class is used to connect to Harvest to retrieve files
 * before the build is started. It has a number of properties which must be set
 * in order to connect to Harvest. It also provides a mechanism to pipe Harvest
 * errors through into the CruiseControl LOG.
 *
 * @author <a href="mailto:info@trinem.com">Trinem Consulting Ltd</a>
 */
public class AllFusionHarvestBootstrapper implements Bootstrapper {

    private JCaHarvestWrap harvest = null;

    private String broker = null;
    private String username = null;
    private String password = null;

    private String project = null;
    private String state = null;

    private String process = null;
    private String clientPath = null;
    private String viewPath = null;
    private String filename = null;

    private boolean loggedIn = false;

    private JCaHarvestLogStreamWrap logstream = null;

    private static final Logger LOG = Logger.getLogger(AllFusionHarvestBootstrapper.class);

    /**
     * Default constructor. Creates a new uninitialise Bootstrapper.
     */
    public AllFusionHarvestBootstrapper() {
    }

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
     * Sets the name of the process to use when making calls to HSDK.
     *
     * @param process
     *            String indicating the process name.
     */
    public void setProcess(String process) {
        this.process = process;
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
     * Sets the client path to use when making calls to HSDK.
     *
     * @param clientPath
     *            String indicating the client path.
     */
    public void setClientpath(String clientPath) {
        this.clientPath = clientPath;
    }

    /**
     * Sets the filename to update.
     *
     * @param filename
     *            String indicating the filename.
     */
    public void setFile(String filename) {
        this.filename = filename;
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
        } catch (JCaHarvestExceptionWrap e) {
            LOG.error(e.getMessage());
        }
    }

    // From Bootstrapper
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
        ValidationHelper.assertIsSet(process, "process", this.getClass());
        ValidationHelper.assertIsSet(clientPath, "clientPath", this.getClass());
        ValidationHelper.assertIsSet(viewPath, "viewPath", this.getClass());
        ValidationHelper.assertIsSet(filename, "filename", this.getClass());
    }

    /**
     * Update the specified file.
     */
    public void bootstrap() {

        LOG.debug("bootstrap()");

        if (!login()) {
            return;
        }

        try {
            JCaContextWrap context = harvest.getContext();
            context.setProject(project);
            context.setState(state);

            if (!context.setCheckout(process)) {
                LOG.error("No checkout process named \"" + process + "\" in this project/state");
                return;
            }
            if (!context.isProcessSet(JCaConstWrap.HAR_CHECKOUT_PROCESS_TYPE)) {
                LOG.error("No checkout process in this project/state");
                return;
            }

            JCaCheckoutWrap coproc = context.getCheckout();
            coproc.setCheckoutMode(JCaConstWrap.CO_MODE_SYNCHRONIZE);
            coproc.setPathOption(JCaConstWrap.CO_OPTION_PRESERVE_AND_CREATE);
            coproc.setReplaceFile(true);
            coproc.setClientDir(clientPath);
            coproc.setViewPath(viewPath);
            coproc.setShareWorkDir(true);
            coproc.setUseCITimeStamp(true);

            JCaVersionChooserWrap vc = context.getVersionChooser();

            vc.clear();
            vc.setRecursive(true);
            vc.setVersionItemOption(JCaConstWrap.VERSION_FILTER_ITEM_BOTH);
            vc.setVersionOption(JCaConstWrap.VERSION_FILTER_LATEST_IN_VIEW);
            vc.setVersionStatusOption(JCaConstWrap.VERSION_FILTER_ALL_TAG);
            vc.setBranchOption(JCaConstWrap.BRANCH_FILTER_TRUNK_ONLY);
            vc.setItemName(filename);

            vc.execute();

            /* JCaContainerWrap versionList = */vc.getVersionList();

            /*
             * int numVers = versionList.isEmpty() ? 0 :
             * versionList.getKeyElementCount(JCaAttrKeyWrap.CA_ATTRKEY_NAME);
             *
             * for (int n = 0; n < numVers; n++) {
             * System.out.println(versionList.getString(JCaAttrKeyWrap.CA_ATTRKEY_NAME,
             * n) + ";" +
             * versionList.getString(JCaAttrKeyWrap.CA_ATTRKEY_MAPPED_VERSION_NAME,
             * n) + ";" +
             * versionList.getString(JCaAttrKeyWrap.CA_ATTRKEY_VERSION_STATUS,
             * n));
             *  }
             */

            coproc.execute();

        } catch (JCaHarvestExceptionWrap e) {
            LOG.error(e.toString() /* , getLocation() */);
            // e.printStackTrace();
        }
    }

    /**
     * Simple LogStream Listener class which interprets the message serverity
     * level of Harvest messages and reports them to the CruiseControl LOG.
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
