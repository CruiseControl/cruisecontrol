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

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
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
 * @see net.sourceforge.cruisecontrol.publishers.LinkJabberPublisher
 *
 * @author <a href="mailto:jonas_edgeworth@cal.berkeley.edu">Jonas Edgeworth</a>
 * @version 1.0
 */

public abstract class JabberPublisher implements Publisher {

    private static final Logger LOG = Logger.getLogger(JabberPublisher.class);
    private static final int ONE_SECOND = 1000;

    private String host;
    private int port = 5222;
    private String username;
    private String password;
    private String recipient;
    private boolean chatroom = false;
    private boolean ssl = false;

    private XMPPConnection connection;
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
        try {
            // Uncommenting will execute Smack XML-RPC trace GUI
            //XMPPConnection.DEBUG_ENABLED = true;
            if (ssl) {
                connection = new SSLXMPPConnection(host, port);
            } else {
                connection = new XMPPConnection(host, port);
            }
            try {
                connection.login(username, password);

                if (chatroom) {
                    groupchat = connection.createGroupChat(recipient);
                    groupchat.join(username);
                } else {
                    chat = connection.createChat(recipient);
                }

            } catch (XMPPException e) {
                LOG.error("Authentication error on login", e);
            }
        } catch (XMPPException e) {
            LOG.error("Error initializing jabber connection", e);
        }
    }

    /**
     *  Validate that all the mandatory parameters were specified in order
     * to properly initial the Jabber client service. Note that this is called
     * after the configuration file is read.
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {

        if (null == host) {
            throw new CruiseControlException("'host' not specified in configuration file");
        }

        if (null == username) {
            throw new CruiseControlException("'username' not specified in configuration file");
        } else {
            int domainIncluded = username.indexOf("@");
            if (-1 != domainIncluded) {
                throw new CruiseControlException("'username' is not in correct format. "
                        + "'username' should not be of the form: user@domain.com");
            }
        }

        if (null == password) {
            throw new CruiseControlException("'password' not specified in configuration file");
        }

        if (null == recipient) {
            throw new CruiseControlException("'recipient' not specified in configuration file");
        } else {
            int domainIncluded = recipient.indexOf("@");
            if (-1 == domainIncluded) {
                throw new CruiseControlException("'recipient' is not in correct format. 'recipient' "
                        + "should be of the form: user@domain.com");
            }
        }

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

        try {
            Thread.sleep(ONE_SECOND);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     *  Creates the IM message body to be sent to the recipient.
     *
     *  @return <code>String</code> that makes up the body of the IM message
     * @throws CruiseControlException
     */
    protected abstract String createMessage(XMLLogHelper logHelper) throws CruiseControlException;

}
