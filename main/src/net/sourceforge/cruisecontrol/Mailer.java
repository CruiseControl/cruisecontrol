/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit                              *
 * Copyright (C) 2001  ThoughtWorks, Inc.                                       *
 * 651 W Washington Ave. Suite 500                                              *
 * Chicago, IL 60661 USA                                                        *
 *                                                                              *
 * This program is free software; you can redistribute it and/or                *
 * modify it under the terms of the GNU General Public License                  *
 * as published by the Free Software Foundation; either version 2               *
 * of the License, or (at your option) any later version.                       *
 *                                                                              *
 * This program is distributed in the hope that it will be useful,              *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 * GNU General Public License for more details.                                 *
 *                                                                              *
 * You should have received a copy of the GNU General Public License            *
 * along with this program; if not, write to the Free Software                  *
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  *
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
     * Stores who the mail will be sent to.
     */
    private String to;
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
    public Mailer(String mailhost, String to, String from) {
        this.mailhost = mailhost;
        this.to = to;
        this.from = from;
    }
    
    /**
     * Creates a Mailer for a Collection of "to" addresses.
     *
     * @param to     Collection of email address to which mail should be sent.
     * @param from   Address from which the mail should be sent.
     */
    public Mailer(String mailhost, Collection to, String from) {
        this.mailhost = mailhost;
        
        StringBuffer buf = new StringBuffer();
        for (Iterator toIter = to.iterator(); toIter.hasNext(); ) {
            String nextName = (String)toIter.next();
            buf.append(nextName);
            if (toIter.hasNext()) {
                buf.append(", ");
            }
        }
        this.to = buf.toString();
        this.from = from;
    }
    
    /**
     * Creates a Mailer for an array of "to" addresses.
     *
     * @param to     Array of email addresses to which mail should be sent.
     * @param from   Address from which the mail is sent.
     */
    public Mailer(String mailhost, String[] to, String from) {
        this.mailhost = mailhost;
        
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<to.length; i++ ) {
            String nextName = to[i];
            buf.append(nextName);
            if (i<(to.length - 1)) {
                buf.append(", ");
            }
        }
        this.to = buf.toString();
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
    public void sendMessage(String subject, String message) throws MessagingException {
         sendMessage(subject, message, false);
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
     public void sendMessage(String subject, String message, boolean debug) 
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
