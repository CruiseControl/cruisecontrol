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
package net.sourceforge.cruisecontrol.publishers;


import net.sourceforge.cruisecontrol.util.*;
import net.sourceforge.cruisecontrol.*;

import org.apache.log4j.Category;

import java.io.*;
import java.util.Date;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;


public class HTMLEmailPublisher extends EmailPublisher {

  /** enable logging for this class */
  private static Category log = Category.getInstance(HTMLEmailPublisher.class.getName());

  private String _mailHost;
  private String _returnAddress;
  private String xslFile = null;
  private String xslDir;
  private String css;
  private String logDir;
  private String messageMimeType = "text/html";

  /**
   *  Sends an email message.
   *
   *  @param toList comma delimited <code>String</code> of email addresses
   *  @param subject subject line for the message
   *  @param message body of the message
   *  @throws CruiseControlException
   */
  protected void sendMail(String toList, String subject, String message) throws CruiseControlException {
    log.info("Sending mail notifications.");
    Properties props = System.getProperties();
    props.put("mail.smtp.host", _mailHost);
    Session session = Session.getDefaultInstance(props, null);
    session.setDebug(log.isDebugEnabled());

    try {
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(_returnAddress));
      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toList, false));
      msg.setSubject(subject);
      msg.setSentDate(new Date());

      MimeMultipart attachments = new MimeMultipart();
      MimeBodyPart textbody = new MimeBodyPart();
      textbody.setContent(message, messageMimeType);
      attachments.addBodyPart(textbody);

      msg.setContent(attachments);

      Transport.send(msg);
    } catch (MessagingException e) {
      throw new CruiseControlException(e.getMessage());
    }
  }

  public void setMailHost(String mailHost) {
      _mailHost = mailHost;
      super.setMailHost(mailHost);
  }

  public void setReturnAddress(String emailAddress) {
      _returnAddress = emailAddress;
      super.setReturnAddress(emailAddress);
  }

  /**
   * If xslFile is set then both xslDir and css are ignored. Specified xslFile
   * must take care of entire document -- html open/close, body tags, styles,
   * etc.
   * @param xslFile
   */
  public void setXSLFile(String xslFile) {
    this.xslFile = xslFile;
  }

  /**
   * Directory where standard CruiseControl xsl files are located: <br>
   * <ul>
   *   header.xsl
   *   compile.xsl");
   *   unittests.xsl");
   *   modifications.xsl");
   *   distributables.xsl"
   * </ul>
   * @param xslDir
   */
  public void setXSLDir(String xslDir) {
    this.xslDir = xslDir;
  }

  /**
   * Path to cruisecontrol.css.  Only used with xslDir, not xslFile.
   * @param css
   */
  public void setCSS(String css) {
    this.css = css;
  }


  /**
   * Path to the log file as set in the log element of the configuration
   * xml file.
   * @param logDir
   */
  public void setLogDir(String logDir) {
    this.logDir = logDir;
  }

  protected String createMessage(XMLLogHelper logHelper) {
    String message = "";

    try {
      File logDir = new File( this.logDir );
      File inFile = new File( logDir, logHelper.getLogFileName() );
      message = transform( inFile );
    } catch (Exception ex) {
      log.error("", ex);
      try {
        String logFileName = logHelper.getLogFileName();
        message = createLinkLine(logFileName);
      }
      catch (CruiseControlException ccx) {
        log.error("exception getting logfile name", ccx);
      }
    }

    return message;
  }

  private String createLinkLine(String logFileName) {
    StringBuffer messageBuffer = new StringBuffer();

    String baseLogFileName = logFileName.substring(logFileName.lastIndexOf(File.separator) + 1,
          logFileName.lastIndexOf("."));
    String url = _servletUrl + "?log=" + baseLogFileName;

    messageBuffer.append("View results here -> <a href=\"" + url + "\">" + url + "</a>");

    return messageBuffer.toString();
  }

  protected String transform(File inFile) throws TransformerException, FileNotFoundException, IOException {
    StringBuffer messageBuffer = new StringBuffer();

    TransformerFactory tFactory = TransformerFactory.newInstance();

    if (xslFile != null) {
      File xslFile = new File(this.xslFile);
      appendTransform(inFile, messageBuffer, tFactory, xslFile);
    } else {
      File xslDir = new File(this.xslDir);
      File headerxsl         = new File(xslDir, "header.xsl");
      File compilexsl        = new File(xslDir, "compile.xsl");
      File unittestxsl       = new File(xslDir, "unittests.xsl");
      File modificationsxsl  = new File(xslDir, "modifications.xsl");
      File distributablesxsl = new File(xslDir, "distributables.xsl");

      appendHeader(messageBuffer);
      messageBuffer.append(createLinkLine(inFile.getName()));
      messageBuffer.append("<p>\n");
      appendTransform(inFile, messageBuffer, tFactory, headerxsl);
      messageBuffer.append("<p>\n");
      appendTransform(inFile, messageBuffer, tFactory, compilexsl);
      messageBuffer.append("<p>\n");
      appendTransform(inFile, messageBuffer, tFactory, unittestxsl);
      messageBuffer.append("<p>\n");
      appendTransform(inFile, messageBuffer, tFactory, modificationsxsl);
      messageBuffer.append("<p>\n");
      appendTransform(inFile, messageBuffer, tFactory, distributablesxsl);
      appendFooter(messageBuffer);
    }

    return messageBuffer.toString();
  }

  private void appendTransform(File inFile, StringBuffer messageBuffer, TransformerFactory tFactory, File xslFile) throws IOException, TransformerException {
    Transformer transformer = tFactory.newTransformer(new StreamSource(xslFile));
    File outFile = File.createTempFile("mail",".html");
    try {
      transformer.transform( new StreamSource(inFile), new StreamResult(outFile) );
    } catch (Exception e) {
      log.error("error transforming with xslFile " + xslFile.getName(), e);
      return;
    }
    FileReader outfileReader = new FileReader( outFile );
    BufferedReader reader = new BufferedReader( outfileReader );
    String line = reader.readLine();
    while(line!=null) {
      messageBuffer.append(line);
      line = reader.readLine();
    }
  }

  private void appendHeader(StringBuffer messageBuffer) throws IOException {
    messageBuffer.append("<html><head>\n<style>\n");

    File cssFile = new File(css);
    FileReader cssFileReader = new FileReader( cssFile );
    BufferedReader reader = new BufferedReader( cssFileReader );
    String line = reader.readLine();
    while(line!=null) {
      messageBuffer.append(line);
      line = reader.readLine();
    }

    messageBuffer.append("\n</style>\n</head><body>\n");
  }

  private void appendFooter(StringBuffer messageBuffer) {
    messageBuffer.append("\n</body></html>");
  }
}