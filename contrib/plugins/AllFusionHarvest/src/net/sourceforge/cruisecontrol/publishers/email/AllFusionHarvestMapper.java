/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers.email;

import com.ca.harvest.jhsdk.JCaContext;
import com.ca.harvest.jhsdk.JCaHarvest;
import com.ca.harvest.jhsdk.JCaHarvestLogStream;
import com.ca.harvest.jhsdk.JCaSQL;
import com.ca.harvest.jhsdk.IJCaLogStreamListener;
import com.ca.harvest.jhsdk.hutils.JCaContainer;
import com.ca.harvest.jhsdk.hutils.JCaHarvestException;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/*
 * Mapper that extracts email addresses from the Harvest database. It performs
 * a SQL query to map the username of the given user to their email address.
 */
public class AllFusionHarvestMapper extends EmailAddressMapper {

    //private static final long serialVersionUID = -2121211825257529130L;
    private JCaHarvest harvest = null;

    private String broker = null;
    private String username = null;
    private String password = null;

    private boolean loggedIn = false;

    private JCaHarvestLogStream logstream = null;

    private static Logger log = Logger.getLogger(AllFusionHarvestMapper.class);

    public AllFusionHarvestMapper() {
        super();
    }

    /**
     * Sets the Harvest Broker for all calls to HSDK.
     *
     * @param broker
     *            Harvest Broker to use.
     */
    public void setBroker(String broker) {
        log.debug("Broker: " + broker);
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
            log.error("Login failed: " + harvest.getLastMessage());
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
        } catch (JCaHarvestException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * @see net.sourceforge.cruisecontrol.publishers.email.EmailAddressMapper#open()
     */
    public void open() throws CruiseControlException {         
        if (!login()) {
            return;
        }
    }

    /**
     * Check if plugin has been configured properly.
     * @throws net.sourceforge.cruisecontrol.CruiseControlException If the pluing isn't valid.
     */
    public void validate()
            throws CruiseControlException {
        ValidationHelper.assertIsSet(username, "username", this.getClass());
        ValidationHelper.assertIsSet(password, "password", this.getClass());
        ValidationHelper.assertIsSet(broker, "broker", this.getClass());
    }

    /**
     * The actual purpose of the plugin: Map a username to the respective email address.
     * @see net.sourceforge.cruisecontrol.publishers.email.EmailAddressMapper#mapUser(java.lang.String)
     */
    public String mapUser(String user) {

        if (!login()) {
            return null;
        }

        try {
            JCaContext context = harvest.getContext();
            JCaSQL sql = context.getSQL();
            //log.info("Looking up user '" + user + "'");
            sql.setSQLStatement("Select EMAIL from HARUSER where USERNAME='" + user + "'");
            sql.execute();
            JCaContainer sqlData = sql.getSQLResult();
            if (sqlData.getKeyCount() > 0) {
                if (sqlData.getKeyElementCount("EMAIL") > 0) {
                    String email = sqlData.getString("EMAIL", 0);
                    log.info("User '" + user + "' mapped to email '" + email + "'");
                    return email;
                }
            }
        } catch (JCaHarvestException e) {
            log.error(e.toString() /*, getLocation()*/);
            //e.printStackTrace();
        }
        return null;
    }

    /**
     * Simple LogStream Listener class which interprets the message serverity
     * level of Harvest messages and reports them to the CruiseControl log.
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
                log.info(message);
                break;
            case JCaHarvestLogStream.WARNING:
                log.warn(message);
                break;
            case JCaHarvestLogStream.ERROR:
                log.error(message);
                break;
            case JCaHarvestLogStream.OK:
            default:
                log.debug(message);
                break;
            }
        }
    }
}
