/******************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
 ******************************************************************************/
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.log4j.Logger;
import org.jdom.Element;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;

/**
 * Abstract implementation of the <code>Publisher</code> interface, specifically
 * tailored for sending email.  The only abstract method is createMessage, which
 * allows different implementations to send different messages as the body of
 * the email.  As it currently stands, there is one concrete implementation--
 * <code>LinkEmailPublisher</code>, but the ability to create
 * <code>EmailPublisher</code>s that handle sending a text summary or an html
 * summary is there.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2002
 */
public abstract class EmailPublisher implements Publisher {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(EmailPublisher.class);

    private String _mailHost;
    protected String _servletUrl;
    private List _alwaysAddresses = new ArrayList();
    private List _failureAddresses = new ArrayList();
    private List _emailMap = new ArrayList();
    private String _returnAddress;
    private String _defaultSuffix;
    private String _reportSuccess = "always";
    private boolean _spamWhileBroken;

    /**
     *  Implementations of this method will create the email message body.
     *
     *  @param logHelper <code>XMLLogHelper</code> wrapper for the build log.
     *  @return <code>String</code> containing the message
     */
    protected abstract String createMessage(XMLLogHelper logHelper);

    /**
     *  Creates the subject line for the email message.
     *
     *  @param logHelper <code>XMLLogHelper</code> wrapper for the build log.
     *  @return <code>String</code> containing the subject line.
     */
    protected String createSubject(XMLLogHelper logHelper)
            throws CruiseControlException {
        if (logHelper.isBuildSuccessful()) {
            if (_reportSuccess.equalsIgnoreCase("fixes")
                    && !logHelper.wasPreviousBuildSuccessful()) {
                return logHelper.getProjectName() + " " + logHelper.getLabel()
                        + " Build Fixed";
            } else {
                return logHelper.getProjectName() + " " + logHelper.getLabel()
                        + " Build Successful";
            }
        } else {
            return logHelper.getProjectName() + " Build Failed";
        }
    }

    /**
     *  Determines if the conditions are right for the email to be sent.
     *
     *  @param logHelper <code>XMLLogHelper</code> wrapper for the build log.
     *  @return whether or not the mail message should be sent.
     */
    protected boolean shouldSend(XMLLogHelper logHelper)
            throws CruiseControlException {
        if (logHelper.isBuildSuccessful()) {
            if (_reportSuccess.equalsIgnoreCase("success")) {
                return true;
            } else if (_reportSuccess.equalsIgnoreCase("fixes")) {
                if (logHelper.wasPreviousBuildSuccessful()) {
                    log.debug("reportSuccess is set to 'fixes', not sending emails for repeated successful builds.");
                    return false;
                } else {
                    return true;
                }
            } else if (_reportSuccess.equalsIgnoreCase("never")) {
                log.debug("reportSuccess is set to 'never', not sending emails for successful builds.");
                return false;
            }
        } else { //build wasn't successful
            if (!logHelper.wasPreviousBuildSuccessful()
                    && !logHelper.isBuildNecessary() && !_spamWhileBroken) {
                log.debug("spamWhileBroken is set to false, not sending email");
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the list of email addresses to receive the email message.  If an
     * emailmap file is found, usernames will attempt to be mapped to emails
     * from that file.  If no emailmap is found, or no match for the given
     * username is found in the emailmap, then the default email suffix will be
     * used.  Any addresses set in the <code>addAlwaysAddress</code> method will
     * always receive the email if it is sent.  Any address set in the
     * <code>addFailureAddress</code> method will receive the message if the
     * build has failed.
     *
     * @param logHelper <code>XMLLogHelper</code> wrapper for the build log.
     * @return comma delimited <code>String</code> of email addresses to
     * receive the email message.
     */
    protected String createUserList(XMLLogHelper logHelper) {
        Set users = logHelper.getBuildParticipants();

        //add always addresses
        Iterator alwaysAddressIterator = _alwaysAddresses.iterator();
        while (alwaysAddressIterator.hasNext()) {
            users.add(((Always) alwaysAddressIterator.next()).getAddress());
        }

        //if build failed, add failure addresses
        if (!logHelper.isBuildSuccessful()) {
            Iterator failureAddressIterator = _failureAddresses.iterator();
            while (failureAddressIterator.hasNext()) {
                users.add(
                        ((Failure) failureAddressIterator.next()).getAddress());
            }
        }

/*
        Set emails = new TreeSet();
        Properties emailMap = new Properties();
        FileInputStream fis = null;
        if (_emailMapFile != null) {
            try {
                fis = new FileInputStream(_emailMapFile);
                emailMap.load(fis);
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fis = null;
            }
        }
*/
        //move map to hashtable
        Set emails = new TreeSet();
        Hashtable emailMap = new Hashtable();
        Iterator emailMapIterator = _emailMap.iterator();
        while (emailMapIterator.hasNext()) {
            Map map = (Map) emailMapIterator.next();
            log.debug("Mapping alias: " + map.getAlias() + " to address: "
                    + map.getAddress());
            emailMap.put(map.getAlias(), map.getAddress());
        }

        Iterator userIterator = users.iterator();
        while (userIterator.hasNext()) {
            String user = (String) userIterator.next();
            if (emailMap.containsKey(user)) {
                log.debug("User found in email map.  Mailing to: "
                        + emailMap.get(user));
                emails.add(emailMap.get(user));
            } else {
                if (user.indexOf("@") < 0) {
                    user = user + _defaultSuffix;
                }
                log.debug("User not found in email map.  Mailing to: " + user);
                emails.add(user);
            }
        }

        //return set of emails as a comma delimited string
        StringBuffer commaDelimitedString = new StringBuffer();
        int emailCount = 1;
        Iterator emailIterator = emails.iterator();
        while (emailIterator.hasNext()) {
            commaDelimitedString.append((String) emailIterator.next());
            if (emailCount < emails.size()) {
                commaDelimitedString.append(",");
            }
            emailCount++;
        }
        log.debug("Final list of emails: " + commaDelimitedString.toString());

        return commaDelimitedString.toString();
    }

    /**
     *  Implementing the <code>Publisher</code> interface.
     */
    public void publish(Element cruisecontrolLog) {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        try {
            if (shouldSend(helper)) {
                sendMail(createUserList(helper), createSubject(helper),
                        createMessage(helper));
            }
        } catch (CruiseControlException e) {
            log.error("", e);
        }
    }

    /**
     *  Sends an email message.
     *
     *  @param toList comma delimited <code>String</code> of email addresses
     *  @param subject subject line for the message
     *  @param message body of the message
     */
    protected void sendMail(String toList, String subject, String message)
            throws CruiseControlException {
        log.info("Sending mail notifications.");
        Properties props = System.getProperties();
        props.put("mail.smtp.host", _mailHost);
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(log.isDebugEnabled());

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(_returnAddress));
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toList, false));
            msg.setSubject(subject);
            msg.setText(message);
            msg.setSentDate(new Date());
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new CruiseControlException(e.getMessage());
        }
    }

    public void setMailHost(String mailHost) {
        _mailHost = mailHost;
    }

    public void setBuildResultsUrl(String servletUrl) {
        _servletUrl = servletUrl;
    }

    public void addAlwaysAddress(String emailAddress) {
        _alwaysAddresses.add(emailAddress);
    }

    public void addFailureAddress(String emailAddress) {
        _failureAddresses.add(emailAddress);
    }

    public void setReturnAddress(String emailAddress) {
        _returnAddress = emailAddress;
    }

    public void setDefaultSuffix(String defaultEmailSuffix) {
        _defaultSuffix = defaultEmailSuffix;
    }

    public void setReportSuccess(String reportSuccess) {
        _reportSuccess = reportSuccess;
    }

    public void setSpamWhileBroken(boolean spam) {
        _spamWhileBroken = spam;
    }

    public Object createAlways() {
        Always always = new Always();
        _alwaysAddresses.add(always);
        return always;
    }

    public Object createFailure() {
        Failure failure = new Failure();
        _failureAddresses.add(failure);
        return failure;
    }

    public Object createMap() {
        Map map = new Map();
        _emailMap.add(map);
        return map;
    }

    public class Always {
        private String _address;

        public String getAddress() {
            return _address;
        }

        public void setAddress(String address) {
            _address = address;
        }
    }

    public class Failure {
        private String _address;

        public String getAddress() {
            return _address;
        }

        public void setAddress(String address) {
            _address = address;
        }
    }

    public class Map {
        private String _alias;
        private String _address;

        public String getAlias() {
            return _alias;
        }

        public void setAlias(String alias) {
            _alias = alias;
        }

        public String getAddress() {
            return _address;
        }

        public void setAddress(String address) {
            _address = address;
        }
    }

}
