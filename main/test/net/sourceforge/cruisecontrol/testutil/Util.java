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

package net.sourceforge.cruisecontrol.testutil;

import org.jdom.Element;

import java.util.Hashtable;
import java.util.Iterator;

public class Util {
    public static Element createElement(boolean success, boolean lastBuildSuccess) {
        return createElement(success, lastBuildSuccess, "2 minutes 20 seconds", 4, null);
    }

    public static Element createModsElement(int numMods) {
        Element modificationsElement = new Element("modifications");
        for (int i = 1; i <= numMods; i++) {
            Element modificationElement = new Element("modification");
            Element userElement = new Element("user");
            Element projectElement = new Element("project");
            projectElement.addContent("basedir/subdirectory" + i);
            int userNumber = (i > 2) ? i - 1 : i;
            userElement.addContent("user" + userNumber);
            modificationElement.addContent(userElement);
            modificationElement.addContent(projectElement);
            modificationsElement.addContent(modificationElement);
        }
        return modificationsElement;
    }

    public static Element createElement(
        boolean success,
        boolean lastBuildSuccess,
        String time,
        int modCount,
        String failureReason) {
        Element cruisecontrolElement = new Element("cruisecontrol");
        Element buildElement = new Element("build");
        buildElement.setAttribute("time", time);

        if (!success) {
            buildElement.setAttribute(
                "error",
                (failureReason == null) ? "Compile failed" : failureReason);
        }

        cruisecontrolElement.addContent(createModsElement(modCount));
        cruisecontrolElement.addContent(buildElement);
        cruisecontrolElement.addContent(
            createInfoElement("somelabel", lastBuildSuccess));
        return cruisecontrolElement;
    }

    public static Element createInfoElement(String label, boolean lastBuildSuccess) {
        Element infoElement = new Element("info");

        Hashtable properties = new Hashtable();
        properties.put("label", label);
        properties.put("lastbuildsuccessful", lastBuildSuccess + "");
        properties.put("logfile", "log20020206120000.xml");
        properties.put("projectname", "TestProject");
        properties.put("builddate", "12/09/2002 13:43:15");

        Iterator propertyIterator = properties.keySet().iterator();
        while (propertyIterator.hasNext()) {
            String propertyName = (String) propertyIterator.next();
            Element propertyElement = new Element("property");
            propertyElement.setAttribute("name", propertyName);
            propertyElement.setAttribute("value", (String) properties.get(propertyName));
            infoElement.addContent(propertyElement);
        }

        return infoElement;
    }
}
