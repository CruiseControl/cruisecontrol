/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers.sfee;

import com.vasoftware.sf.soap42.types.SoapNamedValues;
import com.vasoftware.sf.soap42.webservices.ClientSoapStubFactory;
import com.vasoftware.sf.soap42.webservices.sfmain.ISourceForgeSoap;
import com.vasoftware.sf.soap42.webservices.tracker.ITrackerAppSoap;
import com.vasoftware.sf.soap42.webservices.tracker.TrackerSoapList;
import com.vasoftware.sf.soap42.webservices.tracker.TrackerSoapRow;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.gendoc.annotations.ManualChildName;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XPathAwareChild;
import net.sourceforge.cruisecontrol.util.NamedXPathAwareChild;
import org.jdom.Element;

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
public class SfeeTrackerPublisher extends SfeePublisher {

    private String trackerName;
    private final Collection<NamedXPathAwareChild> fields = new ArrayList<NamedXPathAwareChild>();
    private String projectName;
    private XPathAwareChild title;
    private XPathAwareChild description;
    private XPathAwareChild status;

    public void setTrackerName(String name) {
        this.trackerName = name;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @ManualChildName("field")
    public NamedXPathAwareChild createField() {
        NamedXPathAwareChild field = new NamedXPathAwareChild();
        fields.add(field);
        return field;
    }

    @ManualChildName("title")
    public XPathAwareChild createTitle() {
        title = new XPathAwareChild();
        return title;
    }

    @ManualChildName("description")
    public XPathAwareChild createDescription() {
        description = new XPathAwareChild();
        return description;
    }

    @ManualChildName("status")
    public XPathAwareChild createStatus() {
        status = new XPathAwareChild();
        return status;
    }

    public void publish(final Element cruisecontrolLog) throws CruiseControlException {

        final ISourceForgeSoap soapStub = (ISourceForgeSoap) ClientSoapStubFactory
                .getSoapStub(ISourceForgeSoap.class, getServerURL());

        try {
            final String sessionID = soapStub.login(getUsername(), getPassword());
            final String projectID = SfeeUtils.findProjectID(soapStub, sessionID, projectName);

            final ITrackerAppSoap tracker = (ITrackerAppSoap) ClientSoapStubFactory
                    .getSoapStub(ITrackerAppSoap.class, getServerURL());
            final TrackerSoapList trackerList = tracker.getTrackerList(sessionID, projectID);
            final TrackerSoapRow[] trackerListRows = trackerList.getDataRows();
            String trackerID = null;
            for (final TrackerSoapRow trackerListRow : trackerListRows) {
                String nextTitle = trackerListRow.getTitle();
                if (nextTitle.equals(trackerName)) {
                    trackerID = trackerListRow.getId();
                }
            }
            SfeeUtils.assertFoundValue(trackerID, "trackerName", trackerName);

            tracker.createArtifact(sessionID, trackerID, title.lookupValue(cruisecontrolLog),
                    description.lookupValue(cruisecontrolLog), null, null, status.lookupValue(cruisecontrolLog), null,
                    0, 0, null, null, buildFlexFields(cruisecontrolLog),
                    null, null, null);
        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        }

    }

    private SoapNamedValues buildFlexFields(Element log) throws CruiseControlException {
        SoapNamedValues namedValues = new SoapNamedValues();
        String[] names = new String[fields.size()];
        String[] values = new String[fields.size()];
        int i = 0;
        for (final Iterator<NamedXPathAwareChild> iterator = fields.iterator(); iterator.hasNext(); i++) {
            final NamedXPathAwareChild nextField = iterator.next();
            names[i] = nextField.getName();
            values[i] = nextField.lookupValue(log);
        }
        namedValues.setNames(names);
        namedValues.setValues(values);
        return namedValues;
    }

    public void subValidate() throws CruiseControlException {
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
        for (final NamedXPathAwareChild nextField : fields) {
            nextField.validate();
        }
    }


}
