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
package net.sourceforge.cruisecontrol.util;

import org.jdom.Element;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *  Wrapper for the cruisecontrol build log.  This class serves two purposes:<br>
 *  <ul>
 *      <li>Provide a convenient way to get relevant information about the build</li>
 *      <li>Abstract the build information so that the XML for the build log can change easily</li>
 *  </ul>
 */
public class XMLLogHelper {

    private Element _log;

    public XMLLogHelper(Element log) {
        _log = log;
    }

    /**
     *  @return the build log name
     */
    public String getLogFileName() {
        return _log.getChild("cruisecontrol").getChild("logfile").getAttributeValue("value");
    }

    /**
     *  @return the label for this build
     */
    public String getLabel() {
        return _log.getChild("cruisecontrol").getChild("label").getAttributeValue("value");
    }

    /**
     *  @return true if the previous build was successful, false if it was not
     */
    public boolean wasPreviousBuildSuccessful() {
        return _log.getChild("cruisecontrol").getChild("lastbuildsuccessful").getAttributeValue("value").equals("true");
    }

    /**
     *  @param propertyName the name of the ant property
     *  @return the value of the ant property
     */
    protected String getAntProperty(String propertyName) {
        Iterator propertyIterator = _log.getChild("properties").getChildren("property").iterator();
        while(propertyIterator.hasNext()) {
            Element property = (Element) propertyIterator.next();
            if(property.getAttributeValue("name").equals(propertyName)) {
                return property.getAttributeValue("value");
            }
        }
        return null;
    }

    /**
     *  @return true if the build was necessary
     */
    public boolean isBuildNecessary() {
        if(_log.getAttribute("error") != null) {
            return !_log.getAttributeValue("error").equalsIgnoreCase("Build Not Necessary");
        }
        return true;
    }

    /**
     *  @return project name as defined in the ant build file
     */
    public String getProjectName() {
        return getAntProperty("ant.project.name");
    }

    /**
     *  @return true if the build was successful, false otherwise
     */
    public boolean isBuildSuccessful() {
        if (_log.getAttribute("error") != null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     *  <code>Set</code> of usernames that have modified code since the last build
     */
    public Set getBuildParticipants() {
        Set results = new HashSet();
        Iterator modificationIterator = _log.getChild("modifications").getChildren("modification").iterator();
        while (modificationIterator.hasNext()) {
            Element modification = (Element) modificationIterator.next();
            results.add(modification.getChild("user").getText());
        }
        return results;
    }
}