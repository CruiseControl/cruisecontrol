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

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.publishers.email.EmailMapper;
import net.sourceforge.cruisecontrol.publishers.email.EmailMapperHelper;
import net.sourceforge.cruisecontrol.publishers.email.EmailMapping;
import net.sourceforge.cruisecontrol.publishers.email.EmailTransmissionResult;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.log4j.Logger;
import org.apache.oro.io.GlobFilenameFilter;
import org.apache.oro.text.MalformedCachePatternException;
import org.apache.commons.validator.EmailValidator;
import org.jdom.Element;
import org.masukomi.aspirin.core.MailQue;

/**
 * Abstract implementation of the <code>Publisher</code> interface, specifically tailored for sending email. The only
 * abstract method is createMessage, which allows different implementations to send different messages as the body of
 * the email. As it currently stands, there is one concrete implementation-- <code>LinkEmailPublisher</code>, but the
 * ability to create <code>EmailPublisher</code>s that handle sending a text summary or an html summary is there.
 * 
 * @author alden almagro, ThoughtWorks, Inc. 2002
 */
public abstract class EmailPublisher implements Publisher {
    private static final Logger LOG = Logger.getLogger(EmailPublisher.class);

    private String mailHost;
    private String userName;
    private String password;
    private String mailPort;
    private boolean useSSL;
    private String buildResultsURL;

    private static final Always[] EMPTY_ALWAYS_ADDRESSES = new Always[0];
    private Always[] alwaysAddresses = EMPTY_ALWAYS_ADDRESSES;

    private static final Failure[] EMPTY_FAILURE_ADDRESSES = new Failure[0];
    private Failure[] failureAddresses = EMPTY_FAILURE_ADDRESSES;

    private static final Success[] EMPTY_SUCCESS_ADDRESSES = new Success[0];
    private Success[] successAddresses = EMPTY_SUCCESS_ADDRESSES;

    private static final Alert[] EMPTY_ALERT_ADDRESSES = new Alert[0];
    private Alert[] alertAddresses = EMPTY_ALERT_ADDRESSES;

    private static final Ignore[] EMPTY_IGNORE_ADDRESSES = new Ignore[0];
    private Ignore[] ignoreUsers = EMPTY_IGNORE_ADDRESSES;

    private static final EmailMapper[] EMPTY_EMAIL_MAPPERS = new EmailMapper[0];
    private EmailMapper[] emailMapper = EMPTY_EMAIL_MAPPERS;
    private final EmailMapperHelper mapperHelper = new EmailMapperHelper();

    private String returnAddress;
    private String returnName;
    private String defaultSuffix = "";
    private String reportSuccess = "always";
    private boolean spamWhileBroken = true;
    private boolean skipUsers = false;
    private String subjectPrefix;
    private boolean failAsImportant = true;

    /**
     * Implementations of this method will create the email message body.
     * 
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return <code>String</code> containing the message
     */
    protected abstract String createMessage(XMLLogHelper logHelper);

    /*
     * Called after the configuration is read to make sure that all the mandatory parameters were specified.. @throws
     * CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(getReturnAddress(), "returnaddress", this.getClass());
        ValidationHelper.assertFalse(getUsername() != null && getPassword() == null,
                "'password' is required if 'username' is set for email.");
        ValidationHelper.assertFalse(getPassword() != null && getUsername() == null,
                "'username' is required if 'password' is set for email.");

        validateAddresses(alwaysAddresses);
        validateAddresses(alertAddresses);
        validateAddresses(failureAddresses);
        validateAddresses(successAddresses);

        for (int i = 0; i < ignoreUsers.length; i++) {
            ignoreUsers[i].validate();
        }
    }

    private void validateAddresses(final Address[] addresses) throws CruiseControlException {
        for (int i = 0; i < addresses.length; i++) {
            addresses[i].validate();
        }
    }

    /**
     * Creates the subject line for the email message.
     * 
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return <code>String</code> containing the subject line.
     * @throws CruiseControlException if an error occurs while building subject string
     */
    protected String createSubject(final XMLLogHelper logHelper) throws CruiseControlException {
        final StringBuffer subjectLine = new StringBuffer();
        if (subjectPrefix != null) {
            subjectLine.append(subjectPrefix).append(" ");
        }
        subjectLine.append(logHelper.getProjectName());
        if (logHelper.isBuildSuccessful()) {
            final String label = logHelper.getLabel();
            if (label.trim().length() > 0) {
                subjectLine.append(" ").append(logHelper.getLabel());
            }

            // Anytime the build is "fixed" the subjest line
            // should read "fixed". It might confuse recipients...but
            // it shouldn't
            if (logHelper.isBuildFix()) {
                subjectLine.append(" Build Fixed");
            } else {
                subjectLine.append(" Build Successful");
            }
        } else {
            subjectLine.append(" Build Failed");
        }
        return subjectLine.toString();
    }

    /**
     * Determines if the conditions are right for the email to be sent.
     * 
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return whether or not the mail message should be sent.
     * @throws CruiseControlException if the "lastbuildsuccessful" property name can not be found.
     */
    protected boolean shouldSend(final XMLLogHelper logHelper) throws CruiseControlException {
        if (logHelper.isBuildSuccessful()) {
            if (reportSuccess.equalsIgnoreCase("always")) {
                return true;
            }
            if (reportSuccess.equalsIgnoreCase("fixes")) {
                if (logHelper.wasPreviousBuildSuccessful()) {
                    LOG.debug("reportSuccess is set to 'fixes', not sending emails for repeated successful builds.");
                    return false;
                } else {
                    return true;
                }
            }
            if (reportSuccess.equalsIgnoreCase("never")) {
                LOG.debug("reportSuccess is set to 'never', not sending emails for successful builds.");
                return false;
            }
        } else { // build wasn't successful
            if (!logHelper.wasPreviousBuildSuccessful() && logHelper.isBuildNecessary() && !spamWhileBroken) {

                LOG.debug("spamWhileBroken is set to false, not sending email");
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the list of email addresses to receive the email message. Uses configured emailmappers to map user names
     * to email addresses. After all mappings are done, mapped users are checked for existence of the domain part
     * (i.e @mydomain.com) in the mapped email address. If it doesn't exist, default (if configured) is appended Any
     * addresses set in the <code>addAlwaysAddress</code> method will always receive the email if it is sent. Any
     * address set in the <code>addFailureAddress</code> method will receive the message if the build has failed.
     *
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return comma delimited <code>String</code> of email addresses to receive the email message.
     * @throws CruiseControlException if an error occurs while building userList string
     */
    protected String createUserList(final XMLLogHelper logHelper) throws CruiseControlException {

        final Set<String> emails = createUserSet(logHelper);
        return createEmailString(emails);
    }

    /**
     * Creates the <code>Set</code> of email addresses to receive the email message.
     * <p>
     * Uses configured emailmappers to map user names to email addresses. After all mappings are done, mapped users are
     * checked for existence of the domain part (i.e @mydomain.com) in the mapped email address. If it doesn't exist,
     * default (if configured) is appended Any addresses set in the <code>addAlwaysAddress</code> method will always
     * receive the email if it is sent. Any address set in the <code>addFailureAddress</code> method will receive the
     * message if the build has failed.
     *
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return A <code>Set</code> of email addresses to receive the email message.
     * @throws CruiseControlException if an error occurs while building userSet
     */
    protected Set<String> createUserSet(final XMLLogHelper logHelper) throws CruiseControlException {

        final Set<String> users = skipUsers ? new HashSet<String>() : logHelper.getBuildParticipants();

        // remove users we want to exclude from the mail spam
        for (int i = 0; i < ignoreUsers.length; i++) {
            users.remove(ignoreUsers[i].getUser());
        }

        // add always addresses
        for (int i = 0; i < alwaysAddresses.length; i++) {
            users.add(alwaysAddresses[i].getAddress());
        }

        // if build failed, add failure addresses
        if (!logHelper.isBuildSuccessful()) {
            for (int i = 0; i < failureAddresses.length; i++) {
                users.add(failureAddresses[i].getAddress());
            }
        }

        // If build fixed, add failure addresses that want to know about the fix
        if (logHelper.isBuildFix()) {
            for (int i = 0; i < failureAddresses.length; i++) {
                if (failureAddresses[i].shouldReportWhenFixed()) {
                    users.add(failureAddresses[i].getAddress());
                }
            }
        }

        // if build succeeded, add success addresses
        if (logHelper.isBuildSuccessful()) {
            for (int i = 0; i < successAddresses.length; i++) {
                users.add(successAddresses[i].getAddress());
            }
        }

        final Set<String> emails = new TreeSet<String>();
        mapperHelper.mapUsers(this, users, emails);

        for (Iterator iterator = emails.iterator(); iterator.hasNext();) {
            String email = (String) iterator.next();
            if (!isValid(email)) {
                LOG.warn("Invalid email: " + email);
            }
        }

        return emails;
    }

    boolean isValid(final String address) {
        return EmailValidator.getInstance().isValid(address);
    }

    /**
     * Implementing the <code>Publisher</code> interface. Sends modification alert and regular build emails. If a user
     * is supposed to receive a modification alert and a regular build email, they will only receive the modification
     * alert. This prevents duplicate emails (currently only the subject is different).
     */
    public void publish(final Element cruisecontrolLog) throws CruiseControlException {
        final XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        final boolean important = failAsImportant && !helper.isBuildSuccessful();

        final Set<String> userSet = new HashSet<String>();
        final Set<String> alertSet = createAlertUserSet(helper);
        final String subject = createSubject(helper);

        if (!alertSet.isEmpty()) {
            final String alertSubject = "[MOD ALERT] " + subject;
            sendMail(createEmailString(alertSet), alertSubject, createMessage(helper), important);
        }

        if (shouldSend(helper)) {
            userSet.addAll(createUserSet(helper));
            // Do not send duplicates to users who received an alert email
            userSet.removeAll(alertSet);                              

            if (!userSet.isEmpty()) {
                sendMail(createEmailString(userSet), subject, createMessage(helper), important);
            } else {
                if (alertSet.isEmpty()) {
                    LOG.info("No recipients, so not sending email");
                }
            }
        }
    }

    /**
     * builds the properties object for the mail session
     * 
     * @return a properties object containing configured properties.
     */
    protected Properties getMailProperties() {
        final Properties props = System.getProperties();
        props.put("mail.smtp.host", mailHost);
        props.put("mail.smtp.sendpartial", "true");
        if (mailPort != null) {
            props.put("mail.smtp.port", mailPort);
        }
        LOG.debug("mailHost is " + mailHost + ", mailPort is " + (mailPort == null ? "default" : mailPort));
        if (userName != null && password != null) {
            props.put("mail.smtp.auth", "true");
            if (useSSL) {
                if (mailPort != null) {
                    props.put("mail.smtp.socketFactory.port", mailPort);
                }
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
            }
        }
        return props;
    }

    /**
     * Sends an email message.
     * 
     * @param toList
     *            comma delimited <code>String</code> of email addresses
     * @param subject
     *            subject line for the message
     * @param message
     *            body of the message
     * @param important
     *            if true, send mail with "High" importance.
     * @return Boolean value indicating if an email was sent.
     * @throws CruiseControlException if a MessagingException occurs.
     */
    protected boolean sendMail(final String toList, final String subject, final String message, final boolean important)
            throws CruiseControlException {

        boolean emailSent = false;

        if (toList != null && toList.trim().length() != 0) {

            LOG.debug("Sending email to: " + toList);

            final Session session = initializeSession();

            try {
                final MimeMessage msg = new MimeMessage(session);
                msg.setFrom(getFromAddress());
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toList, false));
                msg.setSubject(subject);
                msg.setSentDate(new Date());
                final String importance = (important) ? "High" : "Normal";
                msg.addHeader("Importance", importance);

                addContentToMessage(message, msg);

                emailSent = send(session, msg);

            } catch (SendFailedException e) {
                LOG.warn(e.getMessage(), e);
            } catch (MessagingException e) {
                throw new CruiseControlException(e.getClass().getName() + ": " + e.getMessage(), e);
            }
        }

        return emailSent;
    }

    private boolean send(Session session, MimeMessage message) throws MessagingException {
        if (shouldUseSMTPServer()) {
            if (userName != null && password != null) {
                message.saveChanges(); // implicit with send()
                final Transport transport = session.getTransport("smtp");
                transport.connect(mailHost, userName, password);
                transport.sendMessage(message, message.getAllRecipients());
                transport.close();
            } else {
                Transport.send(message);
            }

            return true;
        } else {
            final MailQue queue = new MailQue();
            final EmailTransmissionResult result = new EmailTransmissionResult();
            queue.addWatcher(result);
            queue.queMail(message);

            return result.isSuccess();
        }
    }

    private Session initializeSession() {
        if (shouldUseSMTPServer()) {
            final Session session = Session.getDefaultInstance(getMailProperties(), null);
            session.setDebug(LOG.isDebugEnabled());
            return session;
        } else {
            final Properties configuration = new Properties();
            configuration.setProperty("mail.transport.protocol", "smtp");
            return Session.getDefaultInstance(configuration);
        }
    }

    private boolean shouldUseSMTPServer() {
        return !(getMailHost() == null || "".equals(getMailHost().trim()));
    }

    /**
     * Subclasses can override this method to control how the content is added to the Message.
     * 
     * @param content
     *            content returned by createMessage
     * @param msg
     *            mail Message with headers and addresses added elsewhere
     * @throws MessagingException see Message.setText()
     */
    protected void addContentToMessage(final String content, final Message msg) throws MessagingException {
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

    public void setMailHost(final String hostname) {
        mailHost = hostname;
    }

    public String getMailHost() {
        return mailHost;
    }

    public void setUsername(final String name) {
        userName = name;
    }

    public String getUsername() {
        return userName;
    }

    public void setPassword(final String passwd) {
        password = passwd;
    }

    public String getPassword() {
        return password;
    }

    public void setMailPort(final String port) {
        mailPort = port;
    }

    public String getMailPort() {
        return mailPort;
    }

    public void setUseSSL(final boolean useSSL) {
        this.useSSL = useSSL;
    }

    public void setSubjectPrefix(final String prefix) {
        subjectPrefix = prefix;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public String getBuildResultsURL() {
        return buildResultsURL;
    }

    public void setBuildResultsURL(final String url) {
        buildResultsURL = url;
    }

    public EmailMapper[] getEmailMapper() {
        return emailMapper;
    }

    public String getReturnAddress() {
        return returnAddress;
    }

    public void setReturnAddress(final String emailAddress) {
        returnAddress = emailAddress;
    }

    public String getReturnName() {
        return returnName;
    }

    public void setReturnName(final String emailReturnName) {
        returnName = emailReturnName;
    }

    public String getDefaultSuffix() {
        return defaultSuffix;
    }

    public void setDefaultSuffix(final String defaultEmailSuffix) {
        defaultSuffix = defaultEmailSuffix;
    }

    public void setReportSuccess(final String report) {
        reportSuccess = report;
    }

    public void setSkipUsers(final boolean skip) {
        skipUsers = skip;
    }

    public void setSpamWhileBroken(final boolean spam) {
        spamWhileBroken = spam;
    }

    public void setFailAsImportant(final boolean important) {
        failAsImportant = important;
    }

    public Ignore createIgnore() {
        final List<Ignore> ignoreList = new ArrayList<Ignore>();
        ignoreList.addAll(Arrays.asList(ignoreUsers));

        final Ignore ignore = new Ignore();
        ignoreList.add(ignore);

        ignoreUsers = ignoreList.toArray(new Ignore[ignoreList.size()]);

        return ignore;
    }

    public Always createAlways() {
        final List<Always> alwaysList = new ArrayList<Always>();
        alwaysList.addAll(Arrays.asList(alwaysAddresses));

        final Always always = new Always();
        alwaysList.add(always);

        alwaysAddresses = alwaysList.toArray(new Always[alwaysList.size()]);

        return always;
    }

    public Failure createFailure() {
        final List<Failure> failureList = new ArrayList<Failure>();
        failureList.addAll(Arrays.asList(failureAddresses));

        final Failure failure = new Failure();
        failureList.add(failure);

        failureAddresses = failureList.toArray(new Failure[failureList.size()]);

        return failure;
    }

    public Success createSuccess() {
        final List<Success> successList = new ArrayList<Success>();
        successList.addAll(Arrays.asList(successAddresses));

        final Success success = new Success();
        successList.add(success);

        successAddresses = successList.toArray(new Success[successList.size()]);

        return success;
    }

    public Alert createAlert() {
        final List<Alert> alertsList = new ArrayList<Alert>();
        alertsList.addAll(Arrays.asList(alertAddresses));

        final Alert alert = new Alert();
        alertsList.add(alert);

        alertAddresses = alertsList.toArray(new Alert[alertsList.size()]);

        return alert;
    }

    /*
     * for the <map ... /> entries, just stuff them into the cache in EmailMapperHelper
     */
    public void add(final EmailMapping mapping) {
        EmailMapperHelper.addCacheEntry(this, mapping.getAlias(), mapping.getAddress());
    }

    public void add(final EmailMapper mapper) {
        final List<EmailMapper> mapperList = new ArrayList<EmailMapper>();
        mapperList.addAll(Arrays.asList(emailMapper));

        mapper.setPublisher(this);
        mapperList.add(mapper);

        emailMapper = mapperList.toArray(new EmailMapper[mapperList.size()]);
    }

    public static class Ignore implements Serializable {

        private static final long serialVersionUID = 4763763343885476189L;

        private String user;

        public String getUser() {
            return user;
        }

        public void validate() throws CruiseControlException {
            ValidationHelper.assertIsSet(user, "user", getClass());
        }

        public void setUser(final String theUser) {
            user = theUser;
        }
    }

    public static class Address implements Serializable {

        private static final long serialVersionUID = -7143350548506511400L;

        private String address;

        public String getAddress() {
            return address;
        }

        public void setAddress(final String theAddress) {
            address = theAddress;
        }

        public void validate() throws CruiseControlException {
            ValidationHelper.assertIsSet(address, "address", getClass());
            ValidationHelper.assertFalse(address.equals(""), "empty string is not a valid value of address for "
                    + getClass().getName());
        }
    }

    public static class Always extends Address {

        private static final long serialVersionUID = -4906620821397147420L;
    }

    public static class Failure extends Address {
        private static final long serialVersionUID = -6027290532428946586L;
        /**
         * Set to true to send an email to this "address" when the build gets fixed.
         */
        private boolean reportWhenFixed = false;

        public boolean shouldReportWhenFixed() {
            return reportWhenFixed;
        }

        public void setReportWhenFixed(final boolean reportWhenFixed) {
            this.reportWhenFixed = reportWhenFixed;
        }
    }

    public static class Success extends Address {

        private static final long serialVersionUID = 8001068483405344504L;
    }

    public static class Alert extends Address {
        private static final long serialVersionUID = 3644326033589789766L;

        public void validate() throws CruiseControlException {
            super.validate();

            ValidationHelper.assertIsSet(fileRegExpr, "fileregexpr", getClass());

            try {
                fileFilter = new GlobFilenameFilter(fileRegExpr);
            } catch (MalformedCachePatternException mcpe) {
                ValidationHelper.fail("invalid regexp '" + fileRegExpr + "'", mcpe);
            }
        }

        /**
         * A regExpr for the file you are interested in watching
         */
        private String fileRegExpr = null;

        /**
         * The compiled regExp, set on validation of the Publisher
         */
        private GlobFilenameFilter fileFilter = null;

        /**
         * A <code>String</code> representing the regexp to match against modified files
         * 
         * @param f
         *            A <code>String</code> representing the file that was modified
         */
        public void setFileRegExpr(final String f) {
            this.fileRegExpr = f;
        }
    }

    /**
     * Creates the list of email addresses to receive an alert email message.
     * <p>
     * The full path of each modified file is compared against the regular expressions specified in the configuration.
     * If a modified file's path matches a regular expression, the user's email address is included in the returned
     * <code>String</code>.
     * <p>
     * Uses configured emailmappers to map user names to email addresses. After all mappings are done, mapped users are
     * checked for existence of the domain part (i.e @mydomain.com) in the mapped email address. If it doesn't exist,
     * default (if configured) is appended.
     *
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return comma delimited <code>String</code> of email addresses to receive the email message.
     */
    protected String createAlertUserList(final XMLLogHelper logHelper) {
        return createEmailString(createAlertUserSet(logHelper));
    }

    /**
     * Creates a <code>Set</code> of email addresses to receive an alert email message based on the logHelper.
     * 
     * @param logHelper
     *            <code>XMLLogHelper</code> wrapper for the build log.
     * @return A <code>Set</code> of email addresses to receive the email message.
     */
    protected Set<String> createAlertUserSet(final XMLLogHelper logHelper)  {
        if (alertAddresses.length == 0) {
            return Collections.emptySet();
        }

        final Set<String> users = new HashSet<String>();
        final Set<Modification> modificationSet = logHelper.getModifications();

        for (Iterator modificationIter = modificationSet.iterator(); modificationIter.hasNext();) {
            final Modification mod = (Modification) modificationIter.next();
            final String modifiedFile = mod.getFullPath();

            LOG.debug("Modified file: " + modifiedFile);

            // Compare the modified file to the regExpr's
            for (int i = 0; i < alertAddresses.length; i++) {
                final String emailAddress = alertAddresses[i].getAddress();

                if (emailAddress != null && !"".equals(emailAddress.trim()) && !users.contains(emailAddress)
                        && matchRegExpr(modifiedFile, alertAddresses[i].fileFilter)) {

                    // We have a new match, send an alert email
                    users.add(emailAddress);

                    LOG.info("Alert '" + emailAddress + "' because their fileRegExpr '"
                            + alertAddresses[i].fileRegExpr + "' matched " + modifiedFile);
                }
            }
        }

        final Set<String> emails = new TreeSet<String>();
        mapperHelper.mapUsers(this, users, emails);
        return emails;
    }

    /**
     * Creates a comma delimited <code>String</code> from a <code>Set</code> of <code>String</code>s.
     * 
     * @param emails
     *            A <code>Set</code> containing <code>String</code>s of emails addresses
     * @return A comma delimited <code>String</code> of email addresses
     */
    protected String createEmailString(final Set<String> emails) {
        final StringBuffer commaDelimitedString = new StringBuffer();
        final Iterator emailIterator = appendDefaultSuffix(emails).iterator();

        while (emailIterator.hasNext()) {
            final String mappedUser = (String) emailIterator.next();
            commaDelimitedString.append(mappedUser);
            if (emailIterator.hasNext()) {
                commaDelimitedString.append(",");
            }
        }

        LOG.debug("List of emails: " + commaDelimitedString);

        return commaDelimitedString.toString();
    }

    private Set<String> appendDefaultSuffix(final Set emails) {
        final Set<String> result = new TreeSet<String>();
        final Iterator emailIterator = emails.iterator();

        while (emailIterator.hasNext()) {
            String mappedUser = (String) emailIterator.next();
            // append default suffix if need to
            if (mappedUser.indexOf("@") < 0) {
                mappedUser += defaultSuffix;
            }
            result.add(mappedUser);
        }
        return result;
    }

    /**
     * Compare the input <code>String</code> against a regular expression pattern.
     * 
     * @param input
     *            A <code>String</code> to compare against the regExpr pattern
     * @param pattern
     *            A <code>GlobFilenameFilter</code> pattern
     * @return True if the file matches the regular expression pattern. Otherwise false.
     */
    protected boolean matchRegExpr(final String input, final GlobFilenameFilter pattern) {
        final File file = new File(input);
        String path = file.toString();

        // On systems with a '\' as pathseparator convert it to a forward slash '/'
        // That makes patterns platform independent
        if (File.separatorChar == '\\') {
            path = path.replace('\\', '/');
        }

        return pattern.accept(file, path);
    }
}
