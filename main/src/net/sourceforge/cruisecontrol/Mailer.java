/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol;

import java.io.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.net.InetAddress;
import java.util.*;

/**
 * The Mailer can handle sending simple character based mail messages.
 * 
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author <a href="mailto:jmbolles@thoughtworks.com">Jack Bolles</a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author neillr
 * @since November 28, 2000
 */
public class Mailer {
    /**
     * Stores the address from which the mail was sent.
     */
    private String from;
    /**
     * Stores the address of the mail(SMTP) server.
     */
    private String mailhost;

    /**
     * Construct a mailer for the given recipient(s) and
     * sender.
     *
     * @param to     Either a single address, or a comma delimited list
     *               of addresses, to which mail should be sent.
     * @param from   The sender's address.
     */
    public Mailer(String mailhost, String from) {
        this.mailhost = mailhost;
        this.from = from;
    }

    /**
     * Sends a message to the recipient(s) managed by this
     * Mailer instance.
     *
     * @param subject Subject of the mail message.
     * @param message Message body.
     * @throws MessagingException
     */
    public void sendMessage(String to, String subject, String message) throws MessagingException {
        sendMessage(to, subject, message, false);
    }

    /**
     * Sends a message to the recipient(s) managed by this
     * Mailer instance.
     *
     * @param subject Subject of the mail message.
     * @param message Message body.
     * @throws MessagingException
     */
    public void sendMessage(Collection to, String subject, String message) throws MessagingException {
        StringBuffer buf = new StringBuffer();
        for (Iterator toIter = to.iterator(); toIter.hasNext();) {
            String nextName = (String)toIter.next();
            buf.append(nextName);
            if (toIter.hasNext()) {
                buf.append(", ");
            }
        }

        sendMessage(buf.toString(), subject, message, false);
    }

    /**
     * Sends a message to the recipient(s) managed by this
     * Mailer instance.
     *
     * @param subject Subject of the mail message.
     * @param message Message body.
     * @throws MessagingException
     */
    public void sendMessage(String[] to, String subject, String message) throws MessagingException {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<to.length; i++) {
            String nextName = to[i];
            buf.append(nextName);
            if (i<(to.length - 1)) {
                buf.append(", ");
            }
        }

        sendMessage(buf.toString(), subject, message, false);
    }

    /**
     * Sends a message to the recipient(s) managed by this
     * Mailer instance.
     * 
     * @param subject Subject of the mail message.
     * @param message Message body.
     * @param debug   true to output standard debug information from the
     *                JavaMail Transport Provider.
     * @exception MessagingException
     */
    public void sendMessage(String to, String subject, String message, boolean debug) 
    throws MessagingException  {
        if (mailhost == null || mailhost.equals("")) {
            System.out.println(
                              "\nMail was not sent, as no mailhost has been specified");
            return;
        }

        Properties props = System.getProperties();
        props.put("mail.smtp.host", mailhost);
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(debug);

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, 
                          InternetAddress.parse(to, false));
        msg.setSubject(subject);
        msg.setText(message);
        msg.setSentDate(new Date());

        // send the message off
        Transport.send(msg);
        System.out.println("\nMail was sent successfully.");         
    } 

}
