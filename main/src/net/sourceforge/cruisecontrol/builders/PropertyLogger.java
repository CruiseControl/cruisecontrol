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
package net.sourceforge.cruisecontrol.builders;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

/**
 *  After running ant, we need a way to get properties back.  Running ant in process ends up causing a lot of work
 *  for us, so using this implementation of <code>BuildListener</code> allows us to have access to all the properties
 *  as XML, which will be merged into our build log.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2002
 */
public class PropertyLogger implements BuildListener {

    /**
     *  Write out an XML file with all of the ant properties after the build is complete
     */
    public void buildFinished(BuildEvent event) {
        Hashtable propsHashtable = event.getProject().getProperties();
        Iterator propertyIterator = propsHashtable.keySet().iterator();
        Element propertiesElement = new Element("properties");
        while (propertyIterator.hasNext()) {
            Element propertyElement = new Element("property");
            String name = (String) propertyIterator.next();
            propertyElement.setAttribute("name", name);
            propertyElement.setAttribute("value", (String) propsHashtable.get(name));
            propertiesElement.addContent(propertyElement);
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("propertylogger.xml"));
            XMLOutputter outputter = new XMLOutputter("   ", true);
            outputter.output(propertiesElement, bw);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bw = null;
        }
    }

    //don't care about these methods

    public void buildStarted(BuildEvent event) {
    }

    public void targetStarted(BuildEvent event) {
    }

    public void targetFinished(BuildEvent event) {
    }

    public void taskStarted(BuildEvent event) {
    }

    public void taskFinished(BuildEvent event) {
    }

    public void messageLogged(BuildEvent event) {
    }
}