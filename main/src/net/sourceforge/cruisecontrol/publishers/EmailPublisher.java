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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

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
    private static final Logger LOG = Logger.getLogger(EmailPublisher.class);
    private static final String DEFAULT_EMAILADDRESSMAPPER
            = "net.sourceforge.cruisecontrol.publishers.EmailAddressMapper";

    private String mailHost;
    private String userName;
    private String password;
    private String mailPort;
    private String buildResultsURL;
    private Always[] alwaysAddresses = new Always[0];
    private Failure[] failureAddresses = new Failure[0];
    private Success[] successAddresses = new Success[0];
    private EmailMapping[] emailMap = new EmailMapping[0];
    private String returnAddress;
    private String returnName;
    private String defaultSuffix = "";
    private String reportSuccess = "always";
    private boolean spamWhileBroken = true;
    private boolean skipUsers = false;
    private String subjectPrefix;
    private boolean failAsImportant = true;
    private String emailAddressMapper = DEFAULT_EMAILADDRESSMAPPER;

    /**
     *  Implementations of this method will create the email message body.
     *
     *  @param logHelper <code>XMLLogHelper</code> wrapper for the build log.
     *  @return <code>String</code> containing the message
     */
    protected abstract String createMessage(XMLLogHelper logHelper);

    /*
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        if (getMailHost() == null) {
            throw new CruiseControlException("'mailhost' not specified in configuration file.");
        }
        if (getReturnAddress() == null) {
            throw new CruiseControlException("'returnaddress' not specified in configuration file.");
        }
        if (getUsername() != null && getPassword() == null) {
            throw new CruiseControlException("'password' is required if 'username' is set for email.");
        }
        if (getPassword() != null && getUsername() == null) {
            throw new CruiseControlException("'username' is required if 'password' is set for email.");
        }
    }

    /**
     *  Creates the subject line for the email message.
     *
     *  @param logHelper <code>XMLLogHelper</code> wrapper for the build log.
     *  @return <code>String</code> containing the subject line.
     */
    protected String createSubject(XMLLogHelper logHelper) throws CruiseControlException {
        StringBuffer subjectLine = new StringBuffer();
        if (subjectPrefix != null) {
            subjectLine.append(subjectPrefix);
            subjectLine.append(" ");
        }
        subjectLine.append(logHelper.getProjectName());
        if (logHelper.isBuildSuccessful()) {
            String label = logHelper.getLabel();
            if (label.length() > 0) {
                subjectLine.append(" ");
                subjectLine.append(logHelper.getLabel());
            }

            //Anytime the build is "fixed" the subjest line
            //  should read "fixed". It might confuse recipients...but
            //  it shouldn't
            if (logHelper.isBuildFix()) {
                subjectLine.append(" Build Fixed");
                return subjectLine.toString();
            } else {
                subjectLine.append(" Build Successful");
                return subjectLine.toString();
            }
        } else {
            subjectLine.append(" Build Failed");
            return subjectLine.toString();
        }
    }

    /**
     *  Determines if the conditions are right for the email to be sent.
     *
     *  @param logHelper <code>XMLLogHelper</code> wrapper for the build log.
     *  @return whether or not the mail message should be sent.
     */
    protected boolean shouldSend(XMLLogHelper logHelper) throws CruiseControlException {
        if (logHelper.isBuildSuccessful()) {
            if (reportSuccess.equalsIgnoreCase("always")) {
                return true;
            } else if (reportSuccess.equalsIgnoreCase("fixes")) {
                if (logHelper.wasPreviousBuildSuccessful()) {
                    LOG.debug(
                        "reportSuccess is set to 'fixes', not sending emails for repeated successful builds.");
                    return false;
                } else {
                    return true;
                }
            } else if (reportSuccess.equalsIgnoreCase("never")) {
                LOG.debug(
                    "reportSuccess is set to 'never', not sending emails for successful builds.");
                return false;
            }
        } else { //build wasn't successful
            if (!logHelper.wasPreviousBuildSuccessful()
                && logHelper.isBuildNecessary()
                && !spamWhileBroken) {
                LOG.debug("spamWhileBroken is set to false, not sending email");
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
    protected String createUserList(XMLLogHelper logHelper)
            throws CruiseControlException {

        Set users = logHelper.getBuildParticipants();
        if (skipUsers) {
            users = new HashSet();
        }

        //add always addresses
        for (int i = 0; i < alwaysAddresses.length; i++) {
            users.add(alwaysAddresses[i].getAddress());
        }

        //if build failed, add failure addresses
        if (!logHelper.isBuildSuccessful()) {
            for (int i = 0; i < failureAddresses.length; i++) {
                users.add(failureAddresses[i].getAddress());
            }
        }
        //If build fixed, add failure addresses that want to know about the fix
        if (logHelper.isBuildFix()) {
             for (int i = 0; i < failureAddresses.length; i++) {
                 if (failureAddresses[i].shouldReportWhenFixed()) {
                    users.add(failureAddresses[i].getAddress());
                 }
            }
        }

        //if build succeeded, add success addresses
        if (logHelper.isBuildSuccessful()) {
            for (int i = 0; i < successAddresses.length; i++) {
                users.add(successAddresses[i].getAddress());
            }
        }

        EmailAddressMapper mapper = null;
        Class cl = null;
        try {
            cl = Class.forName(getEmailAddressMapper());
            mapper = (EmailAddressMapper) cl.newInstance();
        } catch (InstantiationException ie) {
            LOG.fatal("Could not instantiate class", ie);
            throw new CruiseControlException("Could not instantiate class: "
                + getEmailAddressMapper());
        } catch (ClassNotFoundException cnfe) {
            LOG.fatal("Could not find class", cnfe);
            throw new CruiseControlException("Could not find class: "
                + getEmailAddressMapper());
        } catch (IllegalAccessException iae) {
            LOG.fatal("Illegal Access", iae);
            throw new CruiseControlException("Illegal Access class: "
                + getEmailAddressMapper());
        }

        mapper.open(this);

        Set emails = new TreeSet();
        Iterator userIterator = users.iterator();
        while (userIterator.hasNext()) {
            String user = (String) userIterator.next();
            String mappedUser = (mapper != null) ? mapper.mapUser(user) : null;
            if (mappedUser != null) {
                LOG.debug("User found in email map.  Mailing to: " + mappedUser);
            } else {
                LOG.debug("User not found in email map.  Mailing to: " + mappedUser);
                mappedUser = user;
            }
            if (mappedUser.indexOf("@") < 0) {
                mappedUser = mappedUser + defaultSuffix;
            }
            emails.add(mappedUser);
        }

        mapper.close();

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
        LOG.debug("Final list of emails: " + commaDelimitedString.toString());

        return commaDelimitedString.toString();
    }

    /**
     *  Implementing the <code>Publisher</code> interface.
     */
    public void publish(Element cruisecontrolLog) {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        boolean important = !helper.isBuildSuccessful() && failAsImportant;
        try {
            if (shouldSend(helper)) {
                sendMail(
                    createUserList(helper),
                    createSubject(helper),
                    createMessage(helper),
                    important);
            }
        } catch (CruiseControlException e) {
            LOG.error("", e);
        }
    }

    /**
     * builds the properties object for the mail session
     * @return a properties object containing configured properties.
     */
    protected Properties getMailProperties() {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", mailHost);
        if (mailPort != null) {
            props.put("mail.smtp.port", mailPort);
        }
        LOG.debug(
            "mailHost is " + mailHost + ", mailPort is " + mailPort == null ? "default" : mailPort);
        if (userName != null && password != null) {
            props.put("mail.smtp.auth", "true");
        }
        return props;
    }

    /**
     *  Sends an email message.
     *
     *  @param toList comma delimited <code>String</code> of email addresses
     *  @param subject subject line for the message
     *  @param message body of the message
     */
    protected void sendMail(String toList, String subject, String message, boolean important)
        throws CruiseControlException {
        LOG.info("Sending mail notifications.");
        Session session = Session.getDefaultInstance(getMailProperties(), null);
        session.setDebug(LOG.isDebugEnabled());

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(getFromAddress());
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toList, false));
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            String importance = (important) ? "High" : "Normal";
            msg.addHeader("Importance", importance);

            addContentToMessage(message, msg);

            if (userName != null && password != null) {
                msg.saveChanges(); // implicit with send()
                Transport transport = session.getTransport("smtp");
                transport.connect(mailHost, userName, password);
                transport.sendMessage(msg, msg.getAllRecipients());
                transport.close();
            } else {
                Transport.send(msg);
            }
        } catch (MessagingException e) {
            throw new CruiseControlException(e.getMessage());
        }
    }

    /**
     * Subclasses can override this method to control how the content
     * is added to the Message.
     *
     * @param content content returned by createMessage
     * @param msg mail Message with headers and addresses added elsewhere
     * @throws MessagingException
     */
    protected void addContentToMessage(String content, Message msg) throws MessagingException {
        msg.setText(content);
    }

    protected InternetAddress getFromAddress() throws AddressException {
        InternetAddress fromAddress = new InternetAddress(returnAddress);
        if (returnName != null) {
            try {
                fromAddress = new InternetAddress(returnAddress, returnName);
            } catch (UnsupportedEncodingException e) {
                LOG.error("error setting returnName [" + returnName + "]: " + e.getMessage());
            }
        }
        return fromAddress;
    }

    public void setMailHost(String hostname) {
        mailHost = hostname;
    }

    public String getMailHost() {
        return mailHost;
    }

    public void setUsername(String name) {
        userName = name;
    }

    public String getUsername() {
        return userName;
    }

    public void setPassword(String passwd) {
        password = passwd;
    }

    public String getPassword() {
        return password;
    }

    public void setMailPort(String port) {
        mailPort = port;
    }

    public String getMailPort() {
        return mailPort;
    }

    public void setSubjectPrefix(String prefix) {
        subjectPrefix = prefix;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public String getBuildResultsURL() {
        return buildResultsURL;
    }

    public void setBuildResultsURL(String url) {
        buildResultsURL = url;
    }

    public String getEmailAddressMapper() {
        return emailAddressMapper;
    }

    public void setEmailAddressMapper(String classname) {
        emailAddressMapper = classname;
    }

    public EmailMapping[] getEmailMapping() {
        return emailMap;
    }

    public String getReturnAddress() {
        return returnAddress;
    }

    public void setReturnAddress(String emailAddress) {
        returnAddress = emailAddress;
    }

    public String getReturnName() {
        return returnName;
    }

    public void setReturnName(String emailReturnName) {
        returnName = emailReturnName;
    }

    public String getDefaultSuffix() {
        return defaultSuffix;
    }

    public void setDefaultSuffix(String defaultEmailSuffix) {
        defaultSuffix = defaultEmailSuffix;
    }

    public void setReportSuccess(String report) {
        reportSuccess = report;
    }

    public void setSkipUsers(boolean skip) {
        skipUsers = skip;
    }

    public void setSpamWhileBroken(boolean spam) {
        spamWhileBroken = spam;
    }

    public void setFailAsImportant(boolean important) {
        failAsImportant = important;
    }

    public Always createAlways() {
        List alwaysList = new ArrayList();
        alwaysList.addAll(Arrays.asList(alwaysAddresses));

        Always always = new Always();
        alwaysList.add(always);

        alwaysAddresses = (Always[]) alwaysList.toArray(new Always[0]);

        return always;
    }

    public Failure createFailure() {
        List failureList = new ArrayList();
        failureList.addAll(Arrays.asList(failureAddresses));

        Failure failure = new Failure();
        failureList.add(failure);

        failureAddresses = (Failure[]) failureList.toArray(new Failure[0]);

        return failure;
    }

    public Success createSuccess() {
        List successList = new ArrayList();
        successList.addAll(Arrays.asList(successAddresses));

        Success success = new Success();
        successList.add(success);

        successAddresses = (Success[]) successList.toArray(new Success[0]);

        return success;
    }

    public EmailMapping createMap() {
        List emailList = new ArrayList();
        emailList.addAll(Arrays.asList(emailMap));

        EmailMapping map = new EmailMapping();
        emailList.add(map);

        emailMap = (EmailMapping[]) emailList.toArray(new EmailMapping[0]);

        return map;
    }

    public class Address {
        private String address;

        public String getAddress() {
            return address;
        }

        public void setAddress(String theAddress) {
            address = theAddress;
        }
    }

    public class Always extends Address {
    }

    public class Failure extends Address {
        /**
         * Set to true to send an email to this "address" when the build gets
         * fixed.
         */
        private boolean reportWhenFixed = false;

        public boolean shouldReportWhenFixed() {
            return reportWhenFixed;
        }

        public void setReportWhenFixed(boolean reportWhenFixed) {
            this.reportWhenFixed = reportWhenFixed;
        }
    }

    public class Success extends Address {
    }

    public static void main(String[] args) {
        EmailPublisher pub = new EmailPublisher() {
            protected String createMessage(XMLLogHelper logHelper) {
                return "This is a test message.";
            }
        };
        pub.setMailHost(args[0]);
        pub.setUsername(args[1]);
        pub.setPassword(args[2]);
        pub.setReturnAddress(args[3]);
        try {
            pub.sendMail(args[4], "test subject", "test message", false);
        } catch (CruiseControlException e) {
            LOG.error("test failed", e);
        }
    }

}
