/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import net.sourceforge.cruisecontrol.Modification;

/**
 * Data structure which holds data specific to a single modification within a CM
 * Synergy repository.
 *
 * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith</a>
 */
public class CMSynergyModification extends Modification {

    private static final Logger LOG = Logger.getLogger(CMSynergyModification.class);

    private static final String MODIFICATION_TYPE = "ccmtask";
    private static final String TAGNAME_MODIFICATION = "modification";
    private static final String TAGNAME_OBJECT = "ccmobject";
    private static final String TAGNAME_CHANGEREQUEST = "ccmcr";
    private static final String TAGNAME_NAME = "name";
    private static final String TAGNAME_TASKNUMBER = "task";
    private static final String TAGNAME_VERSION = "version";
    private static final String TAGNAME_TYPE = "type";
    private static final String TAGNAME_INSTANCE = "instance";
    private static final String TAGNAME_PROJECT = "project";
    private static final String TAGNAME_COMMENT = "comment";
    private static final String TAGNAME_DATE = "date";
    private static final String TAGNAME_USER = "user";
    private static final String TAGNAME_EMAIL = "email";
    private static final String TAGNAME_REVISION = "revision";
    private static final String TAGNAME_HTML_LINK = "a";
    private static final String TAGNAME_HTML_LINK_HREF = "href";
    private static final String TAGNAME_HTML_INS = "ins";

    /**
     * The CM Synergy task number represented by this modification
     */
    public String taskNumber;

    /**
     * A list of change requests associated with this modification
     */
    public List changeRequests = new ArrayList();

    /**
     * Creates a new <code>CMSynergyModification</code> object and sets it's
     * modification type to "ccmtask".
     */
    public CMSynergyModification() {
        super(MODIFICATION_TYPE);
    }

    /**
     * Creates a new <code>ModifiedObject</code>, and adds it to the list of
     * CM Synergy objects associated with the task.
     *
     * @return A new <code>ModifiedObject</code>
     */
    public final ModifiedObject createModifiedObject() {
        ModifiedObject obj = new ModifiedObject();
        files.add(obj);
        return obj;
    }

    /**
     * Creates a new <code>ModifiedObject</code>, populates the fields, and
     * adds it to the list of CM Synergy objects associated with the task.
     *
     * @param name
     *            The object's name
     * @param version
     *            The object's version
     * @param type
     *            The object's type within CM Synergy
     * @param instance
     *            The object's instance
     * @param project
     *            The project with which the object is associated
     * @param comment
     *            The comment provided when checking in the object
     *
     * @return A new <code>ModifiedObject</code>
     */
    public final ModifiedObject createModifiedObject(String name, String version, String type, String instance,
            String project, String comment) {
        ModifiedObject obj = createModifiedObject();
        obj.name = name;
        obj.version = version;
        obj.type = type;
        obj.instance = instance;
        obj.project = project;
        obj.comment = comment;
        return obj;
    }

    /**
     * Creates a new <code>ChangeRequest</code>, and adds it to the list of
     * change requests associated with the task.
     *
     * @param number
     *            The CR number
     *
     * @return A new <code>ChangeRequest</code>
     */
    public final ChangeRequest createChangeRequest(String number) {
        ChangeRequest cr = new ChangeRequest();
        cr.number = number;
        changeRequests.add(cr);
        return cr;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.cruisecontrol.Modification#toElement(java.text.DateFormat)
     */
    public Element toElement(DateFormat formatter) {
        Element modificationElement = new Element(TAGNAME_MODIFICATION);
        modificationElement.setAttribute(TAGNAME_TYPE, type);

        if (modifiedTime != null) {
            Element dateElement = new Element(TAGNAME_DATE);
            dateElement.addContent(formatter.format(modifiedTime));
            modificationElement.addContent(dateElement);
        }

        if (userName != null) {
            Element userElement = new Element(TAGNAME_USER);
            userElement.addContent(userName);
            modificationElement.addContent(userElement);
        }

        if (comment != null) {
            Element commentElement = new Element(TAGNAME_COMMENT);
            CDATA cd;
            try {
                cd = new CDATA(comment);
            } catch (org.jdom.IllegalDataException e) {
                LOG.error(e);
                cd = new CDATA("Unable to parse comment. It contains illegal data.");
            }
            commentElement.addContent(cd);
            modificationElement.addContent(commentElement);
        }

        if (taskNumber != null) {
            Element taskNumberElement = new Element(TAGNAME_TASKNUMBER);
            taskNumberElement.addContent(taskNumber);
            modificationElement.addContent(taskNumberElement);
        }

        if (revision != null) {
            Element revisionElement = new Element(TAGNAME_REVISION);
            revisionElement.addContent(revision);
            modificationElement.addContent(revisionElement);
        }

        if (emailAddress != null) {
            Element emailAddressElement = new Element(TAGNAME_EMAIL);
            emailAddressElement.addContent(emailAddress);
            modificationElement.addContent(emailAddressElement);
        }

        Iterator i = files.iterator();
        while (i.hasNext()) {
            ModifiedObject obj = (ModifiedObject) i.next();
            modificationElement.addContent(obj.toElement(formatter));
        }

        i = changeRequests.iterator();
        while (i.hasNext()) {
            ChangeRequest cr = (ChangeRequest) i.next();
            modificationElement.addContent(cr.toElement(formatter));
        }

        return modificationElement;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        StringBuffer sb = new StringBuffer();

        sb.append("Task Number: ").append(taskNumber).append('\n');
        sb.append("Owner: ").append(userName).append('\n');
        sb.append("Release: ").append(revision).append('\n');
        sb.append("Completion Date: ").append(formatter.format(modifiedTime)).append('\n');
        sb.append("Synopsis: ").append(comment).append('\n');

        Iterator i = changeRequests.iterator();
        while (i.hasNext()) {
            ChangeRequest cr = (ChangeRequest) i.next();
            sb.append("\tChange Request: ").append(cr.number).append('\n');
        }

        i = files.iterator();
        while (i.hasNext()) {
            ModifiedObject obj = (ModifiedObject) i.next();
            sb.append("\tAssociated Object: ").append(obj.name).append('\n');
            sb.append("\tVersion: ").append(obj.version).append('\n');
            sb.append("\tType: ").append(obj.type).append('\n');
            sb.append("\tInstance: ").append(obj.instance).append('\n');
            sb.append("\tProject: ").append(obj.project).append('\n');
            sb.append("\tComment: ").append(obj.comment).append('\n');
        }

        sb.append('\n');
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.cruisecontrol.Modification#log(java.text.DateFormat)
     */
    public void log(DateFormat formatter) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Task Number: " + taskNumber);
            LOG.debug("Owner: " + userName);
            LOG.debug("Release: " + revision);
            LOG.debug("Completion Date: " + formatter.format(modifiedTime));
            LOG.debug("Synopsis: " + comment);

            Iterator i = changeRequests.iterator();
            while (i.hasNext()) {
                ChangeRequest cr = (ChangeRequest) i.next();
                LOG.debug("\tChange Request: " + cr.number + "\n");
            }

            i = files.iterator();
            while (i.hasNext()) {
                ModifiedObject obj = (ModifiedObject) i.next();
                LOG.debug("\tAssociated Object: " + obj.name);
                LOG.debug("\tVersion: " + obj.version);
                LOG.debug("\tType: " + obj.type);
                LOG.debug("\tInstance: " + obj.instance);
                LOG.debug("\tProject: " + obj.project);
                LOG.debug("\tComment: " + obj.comment);
            }

            LOG.debug("");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.cruisecontrol.Modification#fromElement(org.jdom.Element,
     *      java.text.DateFormat)
     */
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
            modifiedTime = new Date();
        }

        taskNumber = modification.getChildText(TAGNAME_TASKNUMBER);
        revision = modification.getChildText(TAGNAME_REVISION);
        userName = modification.getChildText(TAGNAME_USER);
        comment = modification.getChildText(TAGNAME_COMMENT);
        emailAddress = modification.getChildText(TAGNAME_EMAIL);

        files.clear();
        List modfiles = modification.getChildren(TAGNAME_OBJECT);
        if (modfiles != null) {
            Iterator it = modfiles.iterator();
            while (it.hasNext()) {
                Element modfileElement = (Element) it.next();
                ModifiedObject modfile = new ModifiedObject();
                modfile.fromElement(modfileElement, formatter);
                files.add(modfile);
            }
        }

        changeRequests.clear();
        List crs = modification.getChildren(TAGNAME_CHANGEREQUEST);
        if (crs != null) {
            Iterator it = crs.iterator();
            while (it.hasNext()) {
                Element crElement = (Element) it.next();
                ChangeRequest cr = new ChangeRequest();
                cr.fromElement(crElement, formatter);
                changeRequests.add(cr);
            }
        }
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CMSynergyModification)) {
            return false;
        }
        CMSynergyModification mod = (CMSynergyModification) o;
        return (type.equals(mod.type) && taskNumber.equals(mod.taskNumber));
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode())
                + ((taskNumber == null) ? 0 : taskNumber.hashCode());
        return result;
    }

    /**
     * Data structure which holds data specific to a single object included in a
     * modification within a CM Synergy repository.
     *
     * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith </a>
     */
    public class ModifiedObject {

        // Let's not deal with possible null values
        public String name = "";
        public String version = "";
        public String type = "";
        public String instance = "";
        public String project = "";
        public String comment = "";

        // Only the parent class should call the constructor
        protected ModifiedObject() {
        }

        /*
         * (non-Javadoc)
         *
         * @see net.sourceforge.cruisecontrol.Modification#fromElement(org.jdom.Element,
         *      java.text.DateFormat)
         */
        public Element toElement(DateFormat formatter) {
            Element element = new Element(TAGNAME_OBJECT);

            Element nameElement = new Element(TAGNAME_NAME);
            nameElement.addContent(name);
            element.addContent(nameElement);

            Element versionElement = new Element(TAGNAME_VERSION);
            versionElement.addContent(version);
            element.addContent(versionElement);

            Element typeElement = new Element(TAGNAME_TYPE);
            typeElement.addContent(type);
            element.addContent(typeElement);

            Element instanceElement = new Element(TAGNAME_INSTANCE);
            instanceElement.addContent(instance);
            element.addContent(instanceElement);

            Element projectElement = new Element(TAGNAME_PROJECT);
            projectElement.addContent(project);
            element.addContent(projectElement);

            Element commentElement = new Element(TAGNAME_COMMENT);
            CDATA cd;
            try {
                cd = new CDATA(comment);
            } catch (org.jdom.IllegalDataException e) {
                LOG.error(e);
                cd = new CDATA("Unable to parse comment.  It contains illegal data.");
            }
            commentElement.addContent(cd);
            element.addContent(commentElement);

            return element;
        }

        /*
         * (non-Javadoc)
         *
         * @see net.sourceforge.cruisecontrol.Modification#fromElement(org.jdom.Element,
         *      java.text.DateFormat)
         */
        public void fromElement(Element modification, DateFormat formatter) {
            name = modification.getChildText(TAGNAME_NAME);
            version = modification.getChildText(TAGNAME_VERSION);
            type = modification.getChildText(TAGNAME_TYPE);
            instance = modification.getChildText(TAGNAME_INSTANCE);
            project = modification.getChildText(TAGNAME_PROJECT);
            comment = modification.getChildText(TAGNAME_COMMENT);
        }

    }

    /**
     * Data structure which holds data specific to a Change Request associated
     * with a modification within a CM Synergy repository.
     *
     * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith </a>
     */
    public class ChangeRequest {

        public String href = null;
        public String number = "";

        // Only the parent class should call the constructor
        protected ChangeRequest() {
        }

        /*
         * (non-Javadoc)
         *
         * @see net.sourceforge.cruisecontrol.Modification#fromElement(org.jdom.Element,
         *      java.text.DateFormat)
         */
        public Element toElement(DateFormat formatter) {
            Element element = new Element(TAGNAME_CHANGEREQUEST);

            if (href != null) {
                Element linkElement = new Element(TAGNAME_HTML_LINK);
                linkElement.setAttribute(TAGNAME_HTML_LINK_HREF, href);
                linkElement.addContent(number);
                element.addContent(linkElement);
            } else {
                Element insElement = new Element(TAGNAME_HTML_INS);
                insElement.addContent(number);
                element.addContent(insElement);
            }

            return element;
        }

        /*
         * (non-Javadoc)
         *
         * @see net.sourceforge.cruisecontrol.Modification#fromElement(org.jdom.Element,
         *      java.text.DateFormat)
         */
        public void fromElement(Element modification, DateFormat formatter) {
            Element linkElement = modification.getChild(TAGNAME_HTML_LINK);
            if (linkElement != null) {
                href = linkElement.getAttributeValue(TAGNAME_HTML_LINK_HREF);
                number = linkElement.getText();
            } else {
                Element insElement = modification.getChild(TAGNAME_HTML_INS);
                if (insElement != null) {
                    number = insElement.getText();
                }
            }
        }
    }
}
