/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

package net.sourceforge.cruisecontrol;

import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * data structure for holding data about a single modification
 * to a source control tool.
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 */
public class Modification implements Comparable {
    private static final String TAGNAME_MODIFICATION = "modification";
    private static final String TAGNAME_TYPE = "type";
    private static final String TAGNAME_FILENAME = "filename";
    private static final String TAGNAME_FOLDERNAME = "project";
    private static final String TAGNAME_DATE = "date";
    private static final String TAGNAME_USER = "user";
    private static final String TAGNAME_COMMENT = "comment";
    private static final String TAGNAME_EMAIL = "email";
    private static final String TAGNAME_REVISION = "revision";

    private static final Logger LOG = Logger.getLogger(Modification.class);

    public String type = "unknown";
    public String fileName;
    public String folderName;
    public Date modifiedTime;
    public String userName;
    public String emailAddress;
    public String revision;
    public String comment = "";

    public Element toElement(DateFormat formatter) {
        Element modificationElement = new Element(TAGNAME_MODIFICATION);
        modificationElement.setAttribute(TAGNAME_TYPE, type);
        Element filenameElement = new Element(TAGNAME_FILENAME);
        filenameElement.addContent(fileName);
        Element projectElement = new Element(TAGNAME_FOLDERNAME);
        projectElement.addContent(folderName);
        Element dateElement = new Element(TAGNAME_DATE);
        dateElement.addContent(formatter.format(modifiedTime));
        Element userElement = new Element(TAGNAME_USER);
        userElement.addContent(userName);
        Element commentElement = new Element(TAGNAME_COMMENT);

        CDATA cd = null;
        try {
            cd = new CDATA(comment);
        } catch (org.jdom.IllegalDataException e) {
            LOG.error(e);
            cd =
                new CDATA("Unable to parse comment.  It contains illegal data.");
        }
        commentElement.addContent(cd);

        modificationElement.addContent(filenameElement);
        modificationElement.addContent(projectElement);
        modificationElement.addContent(dateElement);
        modificationElement.addContent(userElement);
        modificationElement.addContent(commentElement);

        if (revision != null) {
            Element revisionElement = new Element(TAGNAME_REVISION);
            revisionElement.addContent(revision);
            modificationElement.addContent(revisionElement);
        }

        // not all sourcecontrols guarantee a non-null email address
        if (emailAddress != null) {
            Element emailAddressElement = new Element(TAGNAME_EMAIL);
            emailAddressElement.addContent(emailAddress);
            modificationElement.addContent(emailAddressElement);
        }

        return modificationElement;
    }

    public String toXml(DateFormat formatter) {
        XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString(toElement(formatter));
    }

    public String toString() {
        SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        StringBuffer sb = new StringBuffer();
        sb.append("FileName: " + fileName + "\n");
        sb.append("FolderName: " + folderName + "\n");
        sb.append("Revision: " + revision + "\n");
        sb.append("Last Modified: " + formatter.format(modifiedTime) + "\n");
        sb.append("UserName: " + userName + "\n");
        sb.append("EmailAddress: " + emailAddress + "\n");
        sb.append("Comment: " + comment + "\n");
        return sb.toString();
    }

    public void log(DateFormat formatter) {
        LOG.debug("FileName: " + fileName);
        LOG.debug("FolderName: " + folderName);
        LOG.debug("Revision: " + revision);
        LOG.debug("Last Modified: " + formatter.format(modifiedTime));
        LOG.debug("UserName: " + userName);
        LOG.debug("EmailAddress: " + emailAddress);
        LOG.debug("Comment: " + comment);
        LOG.debug("");
    }

    public int compareTo(Object o) {
        Modification modification = (Modification) o;
        return modifiedTime.compareTo(modification.modifiedTime);
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof Modification)) {
            return false;
        }

        Modification mod = (Modification) o;

        boolean emailsAreEqual = (emailAddress != null)
            ? emailAddress.equals(mod.emailAddress)
            : (mod.emailAddress == null);

        boolean revisionsAreEqual = (revision != null)
            ? revision.equals(mod.revision)
            : (mod.revision == null);

        return (
            type.equals(mod.type)
                && fileName.equals(mod.fileName)
                && folderName.equals(mod.folderName)
                && modifiedTime.equals(mod.modifiedTime)
                && userName.equals(mod.userName)
                && emailsAreEqual
                && revisionsAreEqual
                && comment.equals(mod.comment));
    }

    //for brief testing only
    public static void main(String[] args) {
        Date now = new Date();
        Modification mod = new Modification();
        mod.fileName = "File\"Name&";
        mod.folderName = "Folder'Name";
        mod.modifiedTime = now;
        mod.userName = "User<>Name";
        mod.comment = "Comment";
        System.out.println(
            mod.toXml(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")));
    }

    public void fromElement(Element modification, DateFormat formatter) {
        type = modification.getAttributeValue(TAGNAME_TYPE);
        fileName = modification.getChildText(TAGNAME_FILENAME);
        folderName = modification.getChildText(TAGNAME_FOLDERNAME);
        try {
            modifiedTime =
                formatter.parse(modification.getChildText(TAGNAME_DATE));
        } catch (ParseException e) {
            //maybe we should do something different
            modifiedTime = new Date();
        }
        revision = modification.getChildText(TAGNAME_REVISION);
        userName = modification.getChildText(TAGNAME_USER);
        comment = modification.getChildText(TAGNAME_COMMENT);
        emailAddress = modification.getChildText(TAGNAME_EMAIL);
    }
}