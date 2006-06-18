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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * data structure for holding data about a single modification
 * to a source control tool.
 *
 * <modification type="" date="" user="" email="">
 *     <comment></comment>
 *     <file >
 * </modification>
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 */
public class Modification implements Comparable {

    private static final Logger LOG = Logger.getLogger(Modification.class);

    private static final String TAGNAME_MODIFICATION = "modification";
    private static final String TAGNAME_TYPE = "type";
    private static final String TAGNAME_FILE = "file";
    private static final String TAGNAME_FILENAME = "filename";
    private static final String TAGNAME_FOLDERNAME = "project";
    private static final String TAGNAME_DATE = "date";
    private static final String TAGNAME_USER = "user";
    private static final String TAGNAME_COMMENT = "comment";
    private static final String TAGNAME_EMAIL = "email";
    private static final String TAGNAME_REVISION = "revision";
    private static final String TAGNAME_ACTION = "action";

    public class ModifiedFile {

        public String fileName;
        public String revision;
        public String folderName;
        public String action = "unknown";

        protected ModifiedFile() {
        }

        public Element toElement(DateFormat formatter) {

            Element element = new Element(TAGNAME_FILE);

            if (revision != null && revision.trim().length() > 0) {
                Element revisionElement = new Element(TAGNAME_REVISION);
                revisionElement.addContent(revision);
                element.addContent(revisionElement);
            }

            if (action != null && action.trim().length() > 0) {
                element.setAttribute(TAGNAME_ACTION, action);
            }

            Element fileElement = new Element(TAGNAME_FILENAME);
            fileElement.addContent(fileName);
            element.addContent(fileElement);

            if (folderName != null && folderName.trim().length() > 0) {
                Element folderElement = new Element(TAGNAME_FOLDERNAME);
                folderElement.addContent(folderName);
                element.addContent(folderElement);
            }

            return element;

        }

        public void fromElement(Element modification, DateFormat formatter) {
            fileName = modification.getChildText(TAGNAME_FILENAME);
            folderName = modification.getChildText(TAGNAME_FOLDERNAME);
            revision = modification.getChildText(TAGNAME_REVISION);
            action = modification.getAttributeValue(TAGNAME_ACTION);
        }

        public boolean equals(Object o) {
            if (!(o instanceof ModifiedFile)) {
                return false;
            }

            ModifiedFile mod = (ModifiedFile) o;

            boolean folderNamesAreEqual = (folderName != null)
                ? folderName.equals(mod.folderName)
                : (mod.folderName == null);

            boolean revisionsAreEqual = (revision != null)
                ? revision.equals(mod.revision)
                : (mod.revision == null);

            return (action.equals(mod.action)
                    && fileName.equals(mod.fileName)
                    && folderNamesAreEqual
                    && revisionsAreEqual);
        }

        public int hashCode() {
            int code = 1;
            if (fileName != null) {
                code += fileName.hashCode() * 2;
            }
            if (revision != null) {
                code += revision.hashCode() * 3;
            }
            if (folderName != null) {
                code += folderName.hashCode() * 5;
            }
            if (action != null) {
                code += action.hashCode() * 7;
            }
            return code;
        }
    }

    public String type = "unknown";
    public Date modifiedTime;
    public String userName;
    public String emailAddress;
    public String revision;
    public String comment = "";

    public List files = new ArrayList();

    public Modification() {
        this("unknown");
    }

    public Modification(String type) {
        this.type = type;
    }


    public final ModifiedFile createModifiedFile(String filename, String folder) {
        ModifiedFile file = newModifiedFile();
        file.fileName = filename;
        file.folderName = folder;
        files.add(file);
        return file;
    }

    protected ModifiedFile newModifiedFile() {
        return new ModifiedFile();
    }

    public Element toElement(DateFormat formatter) {
        Element modificationElement = new Element(TAGNAME_MODIFICATION);
        modificationElement.setAttribute(TAGNAME_TYPE, type);

        for (Iterator i = files.iterator(); i.hasNext(); ) {
            modificationElement.addContent(((ModifiedFile) i.next()).toElement(formatter));
        }

        Element dateElement = new Element(TAGNAME_DATE);
        dateElement.addContent(formatter.format(modifiedTime));
        Element userElement = new Element(TAGNAME_USER);
        userElement.addContent(userName);
        Element commentElement = new Element(TAGNAME_COMMENT);

        CDATA cd;
        try {
            cd = new CDATA(comment);
        } catch (org.jdom.IllegalDataException e) {
            LOG.error(e);
            cd = new CDATA("Unable to parse comment.  It contains illegal data.");
        }
        commentElement.addContent(cd);

        modificationElement.addContent(dateElement);
        modificationElement.addContent(userElement);
        modificationElement.addContent(commentElement);

        if (revision != null && revision.trim().length() > 0) {
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
        return new XMLOutputter().outputString(toElement(formatter));
    }

    public String toString() {
        SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        StringBuffer sb = new StringBuffer();
        sb.append("Type: ").append(type).append('\n');
        sb.append("Last Modified: ").append(formatter.format(modifiedTime)).append('\n');
        sb.append("Revision: ").append(revision).append('\n');
        sb.append("UserName: ").append(userName).append('\n');
        sb.append("EmailAddress: ").append(emailAddress).append('\n');
        sb.append("Comment: ").append(comment).append('\n');
        return sb.toString();
    }

    public void log(DateFormat formatter) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Type: " + type);
            LOG.debug("Last Modified: " + formatter.format(modifiedTime));
            LOG.debug("UserName: " + userName);
            LOG.debug("EmailAddress: " + emailAddress);
            LOG.debug("Comment: " + comment);
            LOG.debug("");
        }
    }

    /**
     * Convenience method for getting the filename of the first file
     */
    public String getFileName() {
        if (files.isEmpty()) {
            return null;
        } else {
            return ((ModifiedFile) files.get(0)).fileName;
        }
    }

    /**
     * Convenience method for getting the foldername of the first file
     */
    public String getFolderName() {
        if (files.isEmpty()) {
            return null;
        } else {
            return ((ModifiedFile) files.get(0)).folderName;
        }
    }


    public int compareTo(Object o) {
        Modification modification = (Modification) o;
        return modifiedTime.compareTo(modification.modifiedTime);
    }

    public boolean equals(Object o) {
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

        boolean filesAreEqual = files.size() == mod.files.size();
        for (int i = 0; filesAreEqual && i < files.size(); i++) {
            filesAreEqual = mod.files.get(i).equals(files.get(i));
        }

        return (
            type.equals(mod.type)
                && modifiedTime.equals(mod.modifiedTime)
                && userName.equals(mod.userName)
                && revisionsAreEqual
                && emailsAreEqual
                && comment.equals(mod.comment));
    }

    public int hashCode() {
        int code = 1;
        if (type != null) {
            code += type.hashCode() * 2;
        }
        if (modifiedTime != null) {
            code += modifiedTime.getTime();
        }
        if (userName != null) {
            code += userName.hashCode() * 5;
        }
        if (emailAddress != null) {
            code += emailAddress.hashCode() * 7;
        }
        if (comment != null) {
            code += comment.hashCode() * 11;
        }
        if (revision != null) {
            code += revision.hashCode() * 13;
        }
        if (files != null) {
            code += fileHashComponent();
        }
        return code;
    }

    private int fileHashComponent() {
        int code = 1;
        for (int i = 0; i < files.size(); i++) {
            Modification.ModifiedFile file = (ModifiedFile) files.get(i);
            code += file.hashCode();
        }
        return code;
    }

    public void fromElement(Element modification, DateFormat formatter) {

        type = modification.getAttributeValue(TAGNAME_TYPE);
        try {
            String s = modification.getChildText(TAGNAME_DATE);
            if (s == null) {
                XMLOutputter outputter = new XMLOutputter();
                LOG.info("XML: " + outputter.outputString(modification));
            }

            modifiedTime = formatter.parse(s);
        } catch (ParseException e) {
            //maybe we should do something different
            modifiedTime = new Date();
        }

        revision = modification.getChildText(TAGNAME_REVISION);
        userName = modification.getChildText(TAGNAME_USER);
        comment = modification.getChildText(TAGNAME_COMMENT);
        emailAddress = modification.getChildText(TAGNAME_EMAIL);

        files.clear();
        List modfiles = modification.getChildren(TAGNAME_FILE);
        if (modfiles != null && modfiles.size() > 0) {

            Iterator it = modfiles.iterator();
            while (it.hasNext()) {
                Element modfileElement = (Element) it.next();
                ModifiedFile modfile = newModifiedFile();
                modfile.fromElement(modfileElement, formatter);
                files.add(modfile);
            }
        }
    }

    /**
     * Concatenates the folderName and fileName of the Modification into a
     * <code>String</code>. If the folderName is null then it is not included.
     * All backward slashes ("\") are converted to forward slashes
     * ("/").
     *
     * @return A <code>String</code> containing the full path
     *         of the modification
     */
    public String getFullPath() {
        StringBuffer result = new StringBuffer();
        String folderName = getFolderName();

        if (folderName != null) {
            result.append(folderName).append("/");
        }

        result.append(getFileName());
        return result.toString().replace('\\', '/');
    }

}
