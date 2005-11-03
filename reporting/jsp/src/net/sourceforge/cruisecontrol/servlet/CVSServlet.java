/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.servlet;

import java.io.IOException;
import java.net.InetAddress;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import net.sourceforge.cruisecontrol.Configuration;
import net.sourceforge.cruisecontrol.JDOMSearcher;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import com.opensymphony.xwork.ActionSupport;

public class CVSServlet extends ActionSupport {
    private Configuration configuration;

    private Element cvs;

    public CVSServlet() throws MalformedObjectNameException,
            NumberFormatException, IOException, AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            JDOMException {
        super();

        String jmxServer;
        try {
            jmxServer = InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            jmxServer = "localhost";
        }

        String rmiPort = System.getProperty("cruisecontrol.rmiport");

        configuration = new Configuration(jmxServer, Integer.parseInt(rmiPort));
        cvs = JDOMSearcher.getElement(configuration.getDocument(), "cvs");
    }

    public String execute() throws Exception {
        Document doc = configuration.getDocument();
        Element parent = JDOMSearcher.getElement(doc,
                cvs.getParentElement().getName());
        cvs = detachElement(cvs);
        parent.removeChild("cvs");
        parent.addContent(cvs);
        configuration.setConfiguration(doc);

        return SUCCESS;
    }

    public String getCvsroot() {
        return getAttribute("cvsroot");
    }

    public void setCvsroot(String cvsroot) {
        setAttribute("cvsroot", cvsroot);
    }

    public String getLocalWorkingCopy() {
        return getAttribute("localWorkingCopy");
    }

    public void setLocalWorkingCopy(String localWorkingCopy) {
        setAttribute("localWorkingCopy", localWorkingCopy);
    }

    public String getModule() {
        return getAttribute("module");
    }

    public void setModule(String module) {
        setAttribute("module", module);
    }

    public String getProperty() {
        return getAttribute("property");
    }

    public void setProperty(String property) {
        setAttribute("property", property);
    }

    public String getPropertyOnDelete() {
        return getAttribute("propertyOnDelete");
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        setAttribute("propertyOnDelete", propertyOnDelete);
    }

    public String getTag() {
        return getAttribute("tag");
    }

    public void setTag(String tag) {
        setAttribute("tag", tag);
    }
    
    private Element detachElement(Element element) {
        Element parent = element.getParentElement();
        if (parent != null) {
            parent.removeContent(element);
        }
        return element;
    }

    private String getAttribute(String name) {
        return cvs.getAttributeValue(name);
    }

    private void setAttribute(String name, String property) {
        if (property != null && !("".equals(property))) {
            cvs.setAttribute(name, property);
        }
    }
}
