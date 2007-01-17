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
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.GroupChat;
import org.jivesoftware.smack.SSLXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * Abstract publisher which establishes this transport to publish
 * build results via Jabber Instant Messaging framework.
 *
 * @author <a href="mailto:jonas_edgeworth@cal.berkeley.edu">Jonas Edgeworth</a>
 * @version 1.0
 * @see LinkJabberPublisher
 */

public abstract class JabberPublisher implements Publisher {

    private static final Logger LOG = Logger.getLogger(JabberPublisher.class);

    private String host;
    private int port = 5222;
    private String username;
    private String password;
    private String recipient;
    private String service;

    private boolean chatroom = false;
    private boolean ssl = false;

    // one connection for the CC session
    private static XMPPConnection connection;
    private Chat chat;
    private GroupChat groupchat;

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setChatroom(boolean chatroom) {
        this.chatroom = chatroom;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * Initialize the XMPPConnection to the Jabber server and
     * create the necessary chat session to send a message.
     */
    protected void init() {
        // Uncommenting will execute Smack XML-RPC trace GUI
        //XMPPConnection.DEBUG_ENABLED = true;
        if (null == connection || requiresReconnect()) {
            try {
                if (ssl) {
                    if (service != null) {
                        connection = new SSLXMPPConnection(host, port, service);
                    } else {
                        connection = new SSLXMPPConnection(host, port);
                    }
                } else {
                    if (service != null) {
                        connection = new XMPPConnection(host, port, service);
                    } else {
                        connection = new XMPPConnection(host, port);
                    }
                }
            } catch (XMPPException e) {
                LOG.error("Error initializing jabber connection", e);
            }
            try {
                connection.login(username, password);
            } catch (XMPPException e) {
                LOG.error("Authentication error on login", e);
            }
        }

        try {
            if (chatroom) {
                groupchat = connection.createGroupChat(recipient);
                groupchat.join(username);
            } else {
                chat = connection.createChat(recipient);
            }
        } catch (XMPPException e) {
            LOG.error("Could not send message to recipient or chat room", e);
        }
    }

    /**
     * Checks for changes to params or connection failure
     *
     * @return true if a reconnect is required
     */
    private boolean requiresReconnect() {
        return (!connection.isConnected()
                && !connection.isSecureConnection())
                || !connection.isAuthenticated();
    }

    /**
     * Validate that all the mandatory parameters were specified in order
     * to properly initial the Jabber client service. Note that this is called
     * after the configuration file is read.
     *
     * @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {

        ValidationHelper.assertIsSet(host, "host", this.getClass());
        ValidationHelper.assertIsSet(username, "username", this.getClass());
        ValidationHelper.assertFalse(isEmail(username),
                "'username' is not in correct format. "
                        + "'username' should not be of the form user@domain.com");

        ValidationHelper.assertIsSet(password, "password", this.getClass());
        ValidationHelper.assertIsSet(recipient, "recipient", this.getClass());
        ValidationHelper.assertTrue(isEmail(recipient),
                "'recipient' is not in correct format. "
                        + "'recipient' should be of the form user@domain.com");
    }

    private boolean isEmail(final String username) {
        return username.indexOf("@") != -1;
    }

    /**
     * Publish the results to the Jabber transport via an instant message.
     *
     * @param cruisecontrolLog
     * @throws CruiseControlException
     */
    public void publish(Element cruisecontrolLog) throws CruiseControlException {

        // Initialize the XMPP connection
        init();

        // Generate the message to be sent to the recipient
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        String message = createMessage(helper);

        // Send the message to the recipient
        try {
            if (chatroom) {
                groupchat.sendMessage(message);
            } else {
                chat.sendMessage(message);
            }
        } catch (XMPPException e) {
            LOG.error("Unable to send message via Jabber", e);
        }
    }

    /**
     * Creates the IM message body to be sent to the recipient.
     *
     * @return <code>String</code> that makes up the body of the IM message
     * @throws CruiseControlException
     */
    protected abstract String createMessage(XMLLogHelper logHelper) throws CruiseControlException;

}
