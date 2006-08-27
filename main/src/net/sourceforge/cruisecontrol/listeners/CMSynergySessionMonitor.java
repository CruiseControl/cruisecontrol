/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.listeners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.sourcecontrols.CMSynergy;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

/**
 * Monitors a set of one or more CM Synergy sessions, launching new sessions as
 * needed. The session information is persisted and made available to other CM
 * Synergy plugins through the session file - a simple properties file which
 * maps a session name to a CM Synergy session ID.
 * 
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith </a>
 */
public class CMSynergySessionMonitor implements Listener {
    
    private static final Logger LOG = Logger
            .getLogger(CMSynergySessionMonitor.class);

    private File sessionFile;
    private String ccmExe = CMSynergy.CCM_EXE;
    private ArrayList sessions = new ArrayList();

    /**
     * Sets the name of the CM Synergy executable to use when issuing commands.
     * 
     * @param ccmExe
     *            the name of the CM Synergy executable
     */
    public void setCcmExe(String ccmExe) {
        this.ccmExe = ccmExe;
    }

    /**
     * Sets the file which contains the mapping between CM Synergy session names
     * and IDs. This file should be in the standard properties file format. Each
     * line should map one name to a CM Synergy session ID (as returned by the
     * "ccm status" command).
     * <p>
     * example: <br>
     * <br>
     * session1=localhost:65024:192.168.1.17
     * 
     * @param sessionFile
     *            The session file
     */
    public void setSessionFile(String sessionFile) {
        this.sessionFile = new File(sessionFile);
    }

    /**
     * Creates a new <code>CMSynergySession</code> object and adds it to our
     * list of monitored sessions.
     * 
     * @return The newly created <code>CMSynergySession</code> object.
     */
    public CMSynergySession createSession() {
        CMSynergySession session = new CMSynergySession();
        sessions.add(session);
        return session;
    }

    /**
     * A simple representation of a CM Synergy commandline session
     * 
     * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith</a>
     */
    public class CMSynergySession {
                
        private String name;
        private String db;
        private String role;
        private String user;
        private String password;
        private String host;
        
        /**
         * Gets the given name of the session.
         * 
         * @return The name.
         */
        public String getName() {
            return name;
        }
 
        /**
         * Sets the name of the session as it will be referenced in the 
         * Cruise Control config file
         * 
         * @param name The session's given name.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the password used to start the session.
         * 
         * @return The password.
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets the password which will be used to start the session.
         * 
         * @param password The password.
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Gets the CM Synergy role under which the session was started.
         * 
         * @return The role.
         */
        public String getRole() {
            return role;
        }

        /**
         * Sets the CM Synergy role under which the session will be started.
         * 
         * @param role The role.
         */
        public void setRole(String role) {
            this.role = role;
        }

        /**
         * Gets the user ID under which the session was started.
         * 
         * @return The user ID.
         */
        public String getUser() {
            return user;
        }

        /**
         * Sets the user ID under which the session will be started.
         * 
         * @param user The user ID.
         */
        public void setUser(String user) {
            this.user = user;
        }
        
        /**
         * Gets the CM Synergy database with which the session is associated
         * 
         * @return The database.
         */
        public String getDatabase() {
            return db;
        }

        /**
         * Sets the CM Synergy database with which the session will be
         * associated
         * 
         * @param db The database.
         */
        public void setDatabase(String db) {
            this.db = db;
        }
        
        /**
         * Gets the host upon which the session is running.
         * 
         * @return The host.
         */
        public String getHost() {
            return host;
        }
        
        /**
         * Sets the host upon which the session will run.
         * 
         * @param host The host.
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * Sets the attribute (properties) file from which the session
         * information will be loaded.
         * 
         * @param attributeFile
         *            The file from which to read our session attributes.
         */
        public void setAttributeFile(String attributeFile) {
            try {
                Properties properties = Util.loadPropertiesFromFile(new File(
                        attributeFile));
                db = properties.getProperty("database");
                role = properties.getProperty("role");
                user = properties.getProperty("user");
                password = properties.getProperty("password");
                host = properties.getProperty("host");
            } catch (Exception e) {
                LOG.error(
                        "Could not load CM Synergy session properties from file \""
                                + attributeFile + "\".", e);
            }
        }

        /**
         * Validates the fields of this object.
         * 
         * @throws CruiseControlException
         */
        public void validate() throws CruiseControlException {
            ValidationHelper.assertIsSet(name, "name", "the <session> child element");
            ValidationHelper.assertIsSet(db, "db", "the <session> child element");
            ValidationHelper.assertIsSet(role, "role", "the <session> child element");
            ValidationHelper.assertIsSet(user, "user", "the <session> child element");
            ValidationHelper.assertIsSet(password, "password", "the <session> child element");
        }
    }

    /**
     * Checks the given session file. If it is does not exist, it is created.
     * This method is synchronized to prevent multiple threads from attempting
     * to create the same file.
     * 
     * @param sessionFile
     *            The session file to check
     * 
     * @throws CruiseControlException
     */
    private static synchronized void checkSessionFile(File sessionFile) throws CruiseControlException {
        // Create the session file if it does not already exist
        if (!sessionFile.exists()) {
            try {
                if (sessionFile.createNewFile()) {
                    LOG.info("Created CM Synergy session file at "
                            + sessionFile.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new CruiseControlException(
                        "Could not create CM Synergy session file at "
                                + sessionFile.getAbsolutePath(), e);
            }
        }
        
        // Make certain that it's writable
        if (!sessionFile.canWrite()) {
            throw new CruiseControlException("Session file \""
                    + sessionFile.getAbsolutePath()
                    + "\" does not exist, or is not writable.");
        }
    }

    /**
     * Checks that all named sessions given to the listener are still running
     * (and accessible). If they are not, new sessions are started as needed.
     * This method is synchronized to prevent multiple threads from each
     * starting their own CM Synergy sessions.
     * 
     * @param ccmExe
     *            The CM Synergy command line executable
     * @param sessionFile
     *            The CM Synergy session map file
     * @param sessions
     *            A list of monitored CM Synergy sessions
     * 
     * @throws CruiseControlException
     */
    private static synchronized void checkSessions(String ccmExe,
            File sessionFile, List sessions) throws CruiseControlException {
        LOG.debug("Using persisted data from " + sessionFile.getAbsolutePath());

        // Load the persisted session information from file
        Properties sessionMap;
        try {
            sessionMap = Util.loadPropertiesFromFile(sessionFile);
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }

        // Get a list of currently running CM Synergy sessions
        ManagedCommandline cmd = new ManagedCommandline(ccmExe);
        cmd.createArgument("status");
        String availableSessions;
        try {
            cmd.execute();
            cmd.assertExitCode(0);
            availableSessions = cmd.getStdoutAsString();
        } catch (Exception e) {
            LOG.warn("CM Synergy failed to provide a list of valid sessions.",
                    e);
            availableSessions = "";
        }

        // Check each monitored session in turn
        for (Iterator it = sessions.iterator(); it.hasNext();) {
            CMSynergySession session = (CMSynergySession) it.next();
            String name = session.getName();
            String id = sessionMap.getProperty(name);
            LOG.info("Checking " + name + ".");
            if (id == null || availableSessions.indexOf(id) < 0) {
                // Start a new session and record the ID in the map
                String newID = startSession(ccmExe, session);
                if (newID != null) {
                    LOG.info("Started CM Synergy session \"" + newID + "\".");
                    sessionMap.setProperty(name, newID);
                }
            } else {
                LOG.info("Using existing session \"" + id + "\".");
            }
        }

        // Update the persisted session information
        try {
            Util.storePropertiesToFile(sessionMap, "CM Synergy session map",
                    sessionFile);
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }
    }

    /**
     * Launches a new CM Synergy command line session
     * 
     * @param session
     *            The session information
     */
    private static String startSession(String ccmExe, CMSynergySession session) {
        
        LOG.info("Starting a new CM Synergy session for \"" + session.getName()
                + "\".");

        // Create CM Synergy startup command
        ManagedCommandline cmd = new ManagedCommandline(ccmExe);
        cmd.createArgument("start");
        cmd.createArgument("-q");
        cmd.createArgument("-nogui");
        cmd.createArgument("-m");
        cmd.createArguments("-d", session.getDatabase());
        cmd.createArguments("-r", session.getRole());
        cmd.createArguments("-n", session.getUser());
        cmd.createArguments("-pw", session.getPassword());
        if (session.getHost() != null) {
            cmd.createArguments("-h", session.getHost());
        }

        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            LOG.error("Could not start a CM Synergy session for "
                    + session.getName(), e);
            return null;
        }

        return cmd.getStdoutAsString().trim();
    }    
        
    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.cruisecontrol.Listener#handleEvent(net.sourceforge.cruisecontrol.ProjectEvent)
     */
    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        if (event instanceof ProjectStateChangedEvent) {
            final ProjectStateChangedEvent stateChanged = (ProjectStateChangedEvent) event;
            // Check sessions before the bootstrappers run
            if (stateChanged.getNewState().getCode() == ProjectState.BOOTSTRAPPING
                    .getCode()) {
                checkSessionFile(sessionFile);
                checkSessions(ccmExe, sessionFile, sessions);
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Listener#validate()
     */
    public void validate() throws CruiseControlException {
        // We must have at least one session to monitor
        ValidationHelper.assertTrue(sessions.size() > 0,
            "You must provide at least one nested <session> element.");

        // Validate the details of each provided session
        for (Iterator it = sessions.iterator(); it.hasNext(); ) {
            CMSynergySession session = (CMSynergySession) it.next();
            session.validate();
        }

        // If no session file was provided, use the default
        if (sessionFile == null) {
            sessionFile = new File(CMSynergy.CCM_SESSION_FILE);
        }
    }

}
