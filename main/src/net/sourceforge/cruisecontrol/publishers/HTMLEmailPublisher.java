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
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Date;

/**
 * Used to publish an HTML e-mail that includes the build report
 *
 * @author Jeffrey Fredrick
 * @author Alden Almagro
 * @author <a href="vwiewior@valuecommerce.ne.jp">Victor Wiewiorowski</a>
 */
public class HTMLEmailPublisher extends EmailPublisher {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(HTMLEmailPublisher.class);

    protected String xslFile;
    protected String xslDir;
    protected String css;
    protected String logDir;
    protected String messageMimeType = "text/html";

    private String[] xslFileNames = {"header.xsl",

                                     "compile.xsl",

                                     "javadoc.xsl",

                                     "unittests.xsl",

                                     "modifications.xsl",

                                     "distributables.xsl"};

    /*
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        super.validate();

        verifyDirectory("HTMLEmailPublisher.logDir", this.logDir);

        if (this.xslFile == null) {
            verifyDirectory("HTMLEmailPublisher.xslDir", this.xslDir);
            verifyFile("HTMLEmailPublisher.css", this.css);

            if (xslFileNames == null) {
                throw new CruiseControlException("HTMLEmailPublisher.xslFileNames can't be null");
            }

            for (int i = 0; i < this.xslFileNames.length; i++) {
                String fileName = this.xslFileNames[i];
                verifyFile("HTMLEmailPublisher.xslDir/" + fileName, new File(this.xslDir, fileName));
            }
        } else {
            verifyFile("HTMLEmailPublisher.xslFile", this.xslFile);
        }
    }

    private void verifyDirectory(String dirName, String dir) throws CruiseControlException {
        if (dir == null) {
            throw new CruiseControlException(dirName + " not specified in configuration file");
        }
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            throw new CruiseControlException(dirName + " does not exist : " + dirFile.getAbsolutePath());
        }
        if (!dirFile.isDirectory()) {
            throw new CruiseControlException(dirName + " is not a directory : " + dirFile.getAbsolutePath());
        }
    }

    private void verifyFile(String fileName, String file) throws CruiseControlException {
        if (file == null) {
            throw new CruiseControlException(fileName + " not specified in configuration file");
        }
        verifyFile(fileName, new File(file));
    }

    private void verifyFile(String fileName, File file) throws CruiseControlException {
        if (!file.exists()) {
            throw new CruiseControlException(fileName + " does not exist: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new CruiseControlException(fileName + " is not a file: " + file.getAbsolutePath());
        }
    }

    /**
     *  @param toList comma delimited <code>String</code> of email addresses
     *  @param subject subject line for the message
     *  @param message body of the message
     *  @throws CruiseControlException
     */
    protected void sendMail(String toList, String subject, String message)
            throws CruiseControlException {
        log.info("Sending mail notifications.");
        Session session = Session.getDefaultInstance(getMailProperties(), null);
        session.setDebug(log.isDebugEnabled());

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(getReturnAddress()));
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toList, false));
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
     * Directory where xsl files are located.
     * @param xslDir
     */
    public void setXSLDir(String xslDir) {
        this.xslDir = xslDir;
    }

    /**
     * Method to override the default list of file names that will be looked
     * for in the directory specified by xslDir. By default these are the
     * standard CruseControl xsl files: <br>
     * <ul>
     *   header.xsl
     *   compile.xsl
     *   unittests.xsl
     *   modifications.xsl
     *   distributables.xsl
     * </ul>
     * I expect this to be used by a derived class to allow someone to
     * change the order of xsl files or to add/remove one to/from the list
     * or a combination.
     * @param fileNames
     */
    protected void setXSLFileNames(String[] fileNames) {
        if (fileNames == null) {
            throw new IllegalArgumentException("xslFileNames can't be null (but can be empty)");
        }
        this.xslFileNames = fileNames;
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
        if (logDir == null) {
            throw new IllegalArgumentException("logDir cannot be null!");
        }

        this.logDir = logDir;
    }

    /**
     * Create the message to be mailed
     *
     * @param logHelper utility object that has parsed the log files
     * @return created message; empty string if logDir not set
     */

    //TODO: address whether this should ever return null;
    // dependent also on transform(File) and createLinkLine()
    protected String createMessage(XMLLogHelper logHelper) {
        String message = "";

        try {
            File logDir = new File(this.logDir);
            File inFile = new File(logDir, logHelper.getLogFileName());
            message = transform(inFile);
        } catch (Exception ex) {
            log.error("", ex);
            try {
                String logFileName = logHelper.getLogFileName();
                message = createLinkLine(logFileName);
            } catch (CruiseControlException ccx) {
                log.error("exception getting logfile name", ccx);
            }
        }

        return message;
    }

    protected String transform(File inFile)
            throws TransformerException, FileNotFoundException, IOException {
        StringBuffer messageBuffer = new StringBuffer();

        TransformerFactory tFactory = TransformerFactory.newInstance();

        if (xslFile != null) {
            File xslFile = new File(this.xslFile);
            appendTransform(inFile, messageBuffer, tFactory, xslFile);
        } else {
            appendHeader(messageBuffer);
            messageBuffer.append(createLinkLine(inFile.getName()));

            File xslDir = new File(this.xslDir);
            for (int i = 0; i < xslFileNames.length; i++) {
                String fileName = xslFileNames[i];
                File xsl = new File(xslDir, fileName);
                appendTransform(inFile, messageBuffer, tFactory, xsl);
            }

            appendFooter(messageBuffer);
        }

        return messageBuffer.toString();
    }

    protected String createLinkLine(String logFileName) {
        String linkLine = "";
        
        if (_servletUrl == null) return linkLine;

        int startName = logFileName.lastIndexOf(File.separator) + 1;
        int endName = logFileName.lastIndexOf(".");
        String baseLogFileName = logFileName.substring(startName, endName);
        String url = _servletUrl + "?log=" + baseLogFileName;

        linkLine = "View results here -> <a href=\"" + url + "\">" + url + "</a>";

        return linkLine;
    }

    protected void appendTransform(File inFile, StringBuffer messageBuffer,
                                   TransformerFactory tFactory, File xslFile)
            throws IOException, TransformerException {
        messageBuffer.append("<p>\n");
        Transformer transformer = tFactory.newTransformer(
                new StreamSource(xslFile));
        File outFile = File.createTempFile("mail", ".html");
        try {
            transformer.transform(new StreamSource(inFile),
                    new StreamResult(outFile));
        } catch (Exception e) {
            log.error("error transforming with xslFile " + xslFile.getName(), e);
            return;
        }
        FileReader outfileReader = new FileReader(outFile);
        BufferedReader reader = new BufferedReader(outfileReader);
        String line = reader.readLine();
        while (line != null) {
            messageBuffer.append(line);
            line = reader.readLine();
        }
    }

    protected void appendHeader(StringBuffer messageBuffer) throws IOException {
        messageBuffer.append("<html><head>\n<style>\n");

        File cssFile = new File(css);
        FileReader cssFileReader = new FileReader(cssFile);
        BufferedReader reader = new BufferedReader(cssFileReader);
        String line = reader.readLine();
        while (line != null) {
            messageBuffer.append(line);
            line = reader.readLine();
        }

        messageBuffer.append("\n</style>\n</head><body>\n");
    }

    protected void appendFooter(StringBuffer messageBuffer) {
        messageBuffer.append("\n</body></html>");
    }
}