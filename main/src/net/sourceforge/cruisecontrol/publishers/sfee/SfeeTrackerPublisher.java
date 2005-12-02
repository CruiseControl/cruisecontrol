/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers.sfee;

import com.vasoftware.sf.soap42.types.SoapNamedValues;
import com.vasoftware.sf.soap42.webservices.ClientSoapStubFactory;
import com.vasoftware.sf.soap42.webservices.sfmain.ISourceForgeSoap;
import com.vasoftware.sf.soap42.webservices.sfmain.ProjectSoapList;
import com.vasoftware.sf.soap42.webservices.sfmain.ProjectSoapRow;
import com.vasoftware.sf.soap42.webservices.tracker.ITrackerAppSoap;
import com.vasoftware.sf.soap42.webservices.tracker.TrackerSoapList;
import com.vasoftware.sf.soap42.webservices.tracker.TrackerSoapRow;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * <p>Publishes to a SourceForge Enterprise Edition Tracker. The publisher allows for arbitrary "fields", since tracker
 * artifacts are configurable. Every tracker artifact must have a title, description, and status. So, those fields are
 * not optional.</p>
 *
 * @author <a href="mailto:kspillne@thoughtworks.com">Kent Spillner</a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public class SfeeTrackerPublisher implements Publisher {
    private String trackerName;
    private String serverUrl;
    private String username;
    private String password;
    private final Collection fields = new ArrayList();
    private String projectName;
    private TrackerChildElement title;
    private TrackerChildElement description;
    private TrackerChildElement status;

    public void setTrackerName(String name) {
        this.trackerName = name;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setServerURL(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Field createField() {
        Field field = new Field();
        fields.add(field);
        return field;
    }

    public TrackerChildElement createTitle() {
        title = new TrackerChildElement();
        return title;
    }

    public TrackerChildElement createDescription() {
        description = new TrackerChildElement();
        return description;
    }

    public TrackerChildElement createStatus() {
        status = new TrackerChildElement();
        return status;
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        ISourceForgeSoap soap = (ISourceForgeSoap) ClientSoapStubFactory
                .getSoapStub(ISourceForgeSoap.class, serverUrl);

        try {
            String sessionID = soap.login(username, password);
            ProjectSoapList projectList = soap.getProjectList(sessionID);
            ProjectSoapRow[] rows = projectList.getDataRows();
            String projectID = null;
            for (int i = 0; i < rows.length; i++) {
                ProjectSoapRow nextProjectRow = rows[i];
                if (nextProjectRow.getTitle().equals(projectName)) {
                    projectID = nextProjectRow.getId();
                }
            }
            assertFoundValue(projectID, "projectName", projectName);

            ITrackerAppSoap tracker = (ITrackerAppSoap) ClientSoapStubFactory
                    .getSoapStub(ITrackerAppSoap.class, serverUrl);
            TrackerSoapList trackerList = tracker.getTrackerList(sessionID, projectID);
            TrackerSoapRow[] trackerListRows = trackerList.getDataRows();
            String trackerID = null;
            for (int i = 0; i < trackerListRows.length; i++) {
                TrackerSoapRow trackerListRow = trackerListRows[i];
                String nextTitle = trackerListRow.getTitle();
                if (nextTitle.equals(trackerName)) {
                    trackerID = trackerListRow.getId();
                }
            }
            assertFoundValue(trackerID, "trackerName", trackerName);

            title.setCurrentLog(cruisecontrolLog);
            description.setCurrentLog(cruisecontrolLog);
            status.setCurrentLog(cruisecontrolLog);

            tracker.createArtifact(sessionID, trackerID, title.getValue(), description.getValue(), null, null,
                    status.getValue(), null, 0, 0, null, null, buildFlexFields(cruisecontrolLog), null, null, null);
        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        }

    }

    private static void assertFoundValue(Object object, String lookupName, String lookupValue)
            throws CruiseControlException {
        if (object == null) {
            throw new CruiseControlException(lookupName + " [" + lookupValue + "] not found");
        }
    }

    private SoapNamedValues buildFlexFields(Element log) throws CruiseControlException {
        SoapNamedValues namedValues = new SoapNamedValues();
        String[] names = new String[fields.size()];
        String[] values = new String[fields.size()];
        int i = 0;
        for (Iterator iterator = fields.iterator(); iterator.hasNext(); i++) {
            Field nextField = (Field) iterator.next();
            nextField.setCurrentLog(log);
            names[i] = nextField.getName();
            values[i] = nextField.getValue();
        }
        namedValues.setNames(names);
        namedValues.setValues(values);
        return namedValues;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertNotEmpty(serverUrl, "serverurl", this.getClass());
        ValidationHelper.assertNotEmpty(username, "username", this.getClass());
        ValidationHelper.assertNotEmpty(password, "password", this.getClass());
        ValidationHelper.assertNotEmpty(projectName, "projectName", this.getClass());
        ValidationHelper.assertNotEmpty(trackerName, "trackerName", this.getClass());
        ValidationHelper.assertHasChild(title, "title", this.getClass());
        ValidationHelper.assertHasChild(description, "description", this.getClass());
        ValidationHelper.assertHasChild(status, "status", this.getClass());

        //Validate required children
        title.validate();
        description.validate();
        status.validate();

        //Validate all fields
        for (Iterator iterator = fields.iterator(); iterator.hasNext();) {
            Field nextField = (Field) iterator.next();
            nextField.validate();
        }
    }

    /**
     * TrackerChildElements represent the general subelement within the publisher definition, i.e. title, status and
     * description. This class includes the logic to handle xpath expressions.
     */
    public static class TrackerChildElement {
        private String value;
        private String xpathExpression;
        private InputStream in;
        private Element currentLog;
        private String xmlFile;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() throws CruiseControlException {
            if (value != null) {
                return value;
            } else {
                try {
                    return evaluateXpath();
                } catch (Exception e) {
                    throw new CruiseControlException(e);
                }
            }
        }

        private String evaluateXpath() throws IOException, JDOMException, CruiseControlException {

            Object searchContext;
            if (in == null && xmlFile == null && currentLog == null) {
                throw new CruiseControlException("current cruisecontrol log not set.");
            } else if (xmlFile != null) {
                System.out.println("SfeeTrackerPublisher$TrackerChildElement.evaluateXpath with xmlfile");
                searchContext = new SAXBuilder().build(new FileInputStream(new File(xmlFile)));
            } else if (in != null) {
                System.out.println("SfeeTrackerPublisher$TrackerChildElement.evaluateXpath with input stream");
                searchContext = new SAXBuilder().build(in);
            } else {
                System.out.println("SfeeTrackerPublisher$TrackerChildElement.evaluateXpath with current log");
                searchContext = new Document(currentLog);
            }

            XPath xpath = XPath.newInstance(xpathExpression);
            return xpath.valueOf(searchContext);
        }

        public void setXPathExpression(String xpathExpression) {
            this.xpathExpression = xpathExpression;
        }

        public void setInputStream(InputStream in) {
            this.in = in;
        }

        public void setXMLFile(String filename) throws FileNotFoundException {
            System.out.println("SfeeTrackerPublisher$TrackerChildElement.setInputStream");
            xmlFile = filename;
        }

        public void validate() throws CruiseControlException {
            if (value == null && xpathExpression == null) {
                throw new CruiseControlException("Either value or xpathExpression must be set.");
            }
            if (value != null && xpathExpression != null) {
                throw new CruiseControlException("value and xpathExpression should not both be set.");
            }
        }

        public void setCurrentLog(Element log) {
            currentLog = log;
        }
    }

    /**
     * Extends TrackerChildElement to allow for adding the name of a field.
     */
    public static class Field extends TrackerChildElement {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void validate() throws CruiseControlException {
            super.validate();
            if (name == null) {
                throw new CruiseControlException("name must be set.");
            }
        }
    }
}
