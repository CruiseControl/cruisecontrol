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
package net.sourceforge.cruisecontrol.publishers;

import java.io.File;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;

import ymsg.network.Session;
import ymsg.network.StatusConstants;
import ymsg.network.AccountLockedException;
import ymsg.network.LoginRefusedException;

/**
 * Publisher which establishes this transport to publish build results via Yahoo
 * Instant Messaging framework.
 * 
 * Parameters are: username - required - YahooId (should not be an eMail
 * address) password - required - Password for YahooId recipient - required -
 * YahooId (no eMail, please), to which the message should be send
 * buildResultsURL - required - You know... proxyHost - not required - since we
 * are using HTTP connection, a HTTP Proxy proxyPort - not required - the port
 * for the HTTP Proxy
 * 
 * @author <a href="mailto:mmay@gmx.net">Markus M. May</a>
 * @version 1.0
 */

public class YahooPublisher implements Publisher {

    private static final Logger LOG = Logger.getLogger(YahooPublisher.class);

    private static Session connection;

    private String username;
    private String password;
    private String recipient;
    private String buildResultsURL;

    private String proxyHost = null;
    private String proxyPort = null;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setBuildResultsURL(String buildResultsURL) {
        this.buildResultsURL = buildResultsURL;
    }

    /**
     * Setter for proxyHost
     * 
     * @param pProxyHost
     *            proxyHost
     */
    public void setProxyHost(String pProxyHost) {
        this.proxyHost = pProxyHost;
    }

    /**
     * Setter for proxyPort
     * 
     * @param pProxyPort
     *            proxyPort
     */
    public void setProxyPort(String pProxyPort) {
        this.proxyPort = pProxyPort;
    }

    /**
     * Validate that all the mandatory parameters were specified in order to
     * properly initial the Yahoo client service. Note that this is called after
     * the configuration file is read.
     * 
     * @throws CruiseControlException
     *             if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper
                .assertIsSet(this.username, "username", this.getClass());
        ValidationHelper
                .assertFalse(
                        this.isEmail(this.username),
                        "'username' is not in correct format. "
                                + "'username' should not be of the form user@domain.com");

        ValidationHelper
                .assertIsSet(this.password, "password", this.getClass());
        ValidationHelper.assertIsSet(this.recipient, "recipient", this
                .getClass());
        ValidationHelper
                .assertFalse(
                        this.isEmail(this.recipient),
                        "'recipient' is not in correct format. "
                                + "'recipient' should not be of the form user@domain.com");
    }

    private boolean isEmail(final String name) {
        return name.indexOf("@") != -1;
    }

    /**
     * Publish the results to the Yahoo transport via an instant message.
     */
    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        boolean status = this.connect();

        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        String message = createMessage(helper);

        if (status) {
            try {
                YahooPublisher.connection.sendMessage(recipient, message);
                Thread.sleep(5 * 1000);
            } catch (Exception e) {
                LOG.error("Error sending message to buddy: " + e.getMessage());
            }
        }
    }

    /**
     * connect to the server and set necessary info.
     */
    public synchronized boolean connect() {
        boolean result = false;

        // Set proxy-information, when needed
        if (this.proxyHost != null) {
            System.setProperty("http.proxyHost", this.proxyHost);
        }

        if (this.proxyPort != null) {
            System.setProperty("http.proxyPort", this.proxyPort);
        }

        // Check if connection already exists
        if (YahooPublisher.connection != null
                && YahooPublisher.connection.getSessionStatus() == StatusConstants.MESSAGING) {
            result = true;
        } else {
            // Create new connection with given parameters
            YahooPublisher.connection = new Session();

            try {
                YahooPublisher.connection.login(this.username, this.password);

                // Did we successfully log in?
                if (YahooPublisher.connection.getSessionStatus() == StatusConstants.MESSAGING) {
                    result = true;
                    LOG.debug("User " + this.username
                            + " sucessfully connected");
                } else {
                    LOG.error("Error logging " + this.username
                            + " in to the Yahoo-system");
                }
            } catch (AccountLockedException e) {
                LOG.error("The account " + this.username
                        + " seems to be locked.");
            } catch (LoginRefusedException e) {
                LOG
                        .error("Yahoo refused the connection.  Username/password incorrect?");
            } catch (Exception e) {
                LOG.error("Error logging in: " + e.getMessage());
            }

            if (result) {
                LOG.debug(this.username + " connected...");
                try {
                    YahooPublisher.connection
                            .setStatus(StatusConstants.STATUS_INVISIBLE);
                } catch (Exception e) {
                    LOG.error("Error setting status: " + e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * Creates the IM message body. This currently creates a message that is a
     * link to a web page with the details of the build.
     * 
     * @return <code>String</code> the link that makes up the body of the IM
     *         message
     */
    protected String createMessage(XMLLogHelper logHelper)
            throws CruiseControlException {
        String logFileName = logHelper.getLogFileName();
        String baseLogFileName = logFileName.substring(logFileName
                .lastIndexOf(File.separator) + 1, logFileName.lastIndexOf("."));

        StringBuffer message = new StringBuffer();
        message.append("Build results for ");
        message.append(logHelper.isBuildSuccessful() ? "successful" : "failed");
        message.append(" build of project ");
        message.append(logHelper.getProjectName());
        message.append(": ");

        message.append(this.buildResultsURL);

        if (this.buildResultsURL.indexOf("?") == -1) {
            message.append("?");
        } else {
            message.append("&");
        }

        message.append("log=");
        message.append(baseLogFileName);

        return message.toString();
    }

}
