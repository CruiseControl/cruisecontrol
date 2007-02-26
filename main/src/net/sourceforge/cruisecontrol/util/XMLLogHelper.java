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
package net.sourceforge.cruisecontrol.util;

import java.text.DateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.DateFormatFactory;
import net.sourceforge.cruisecontrol.Modification;

import org.jdom.Element;

/**
 *  Wrapper for the cruisecontrol build log.  This class serves two purposes:<br>
 *  <ul>
 *      <li>Provide a convenient way to get relevant information about the build</li>
 *      <li>Abstract the build information so that the XML for the build log can change easily</li>
 *  </ul>
 *  <p>
 *  The CruiseControl log is expected to be in the following format: <p>
 *
 *  <pre>
 *      <cruisecontrol>
 *          <info>
 *              <property name="label" value=""/>
 *              <property name="lastbuildtime" value=""/>
 *              <property name="lastgoodbuildtime" value=""/>
 *              <property name="lastbuildsuccessful" value=""/>
 *              <property name="buildfile" value=""/>
 *              <property name="buildtarget" value=""/>
 *          </info>
 *          <build error="">
 *              <properties>
 *              </properties>
 *          </build>
 *          <modifications>
 *          </modifications>
 *      </cruisecontrol>
 *  </pre>
 *
 *  Note: buildtarget is only present when a target is forced via the JMX interface.
 *
 *  @author Alden Almagro
 *  @author Jonny Boman
 *  @version $Id$
 */
public class XMLLogHelper {

    private Element log;
    private DateFormat dateFormat;

    public XMLLogHelper(Element log) {
        this(log, DateFormatFactory.getDateFormat());
    }

    XMLLogHelper(Element log, DateFormat dateFormat) {
        this.log = log;
        this.dateFormat = dateFormat;
    }

    /**
     *  @return the build log name
     */
    public String getLogFileName() throws CruiseControlException {
        return getCruiseControlInfoProperty("logfile");
    }

    /**
     *  @return the label for this build
     */
    public String getLabel() throws CruiseControlException {
        return getCruiseControlInfoProperty("label");
    }

    public String getBuildTimestamp() throws CruiseControlException {
        return getCruiseControlInfoProperty("cctimestamp");
    }

    /**
     *  @return true if the previous build was successful, false if it was not
     */
    public boolean wasPreviousBuildSuccessful() throws CruiseControlException {
        return getCruiseControlInfoProperty("lastbuildsuccessful").equals("true");
    }

    /**
     *  @return true if the build was necessary
     */
    public boolean isBuildNecessary() {
        if (log.getChild("build") != null && log.getChild("build").getAttributeValue("error") != null) {

        return !log.getChild("build").getAttributeValue("error").equals("No Build Necessary"); }
        return true;
    }

    /**
     *  @return project name as defined in the ant build file
     */
    public String getProjectName() throws CruiseControlException {
        return getCruiseControlInfoProperty("projectname");
    }

    /**
     *  @return true if the build was successful, false otherwise
     */
    public boolean isBuildSuccessful() {
        return (log.getChild("build").getAttribute("error") == null);
    }

    /**
     *  Looks in modifications/changelist/ or modifications/modification/user depending on SouceControl implementation.
     *  @return <code>Set</code> of usernames that have modified code since the last build
     */
    public Set getBuildParticipants() {
        Set results = new HashSet();
        if (isP4Modifications()) {

            Iterator changelistIterator = log.getChild("modifications").getChildren("changelist").iterator();
            while (changelistIterator.hasNext()) {
                Element changelistElement = (Element) changelistIterator.next();
                String val = changelistElement.getAttributeValue("email");
                 if ((val == null) || (val.length() == 0)) {
                   val = changelistElement.getAttributeValue("user");
                 }
                 results.add(val);
            }
        } else {
            Iterator modificationIterator = log.getChild("modifications").getChildren("modification")
                    .iterator();
            while (modificationIterator.hasNext()) {
                Element modification = (Element) modificationIterator.next();
                Element emailElement = modification.getChild("email");
                if (emailElement == null) {
                    emailElement = modification.getChild("user");
                }
                results.add(emailElement.getText());
            }
        }
        return results;
    }

    private boolean isP4Modifications() {
        return log.getChild("modifications").getChildren("changelist") != null
                && !log.getChild("modifications").getChildren("changelist").isEmpty();
    }

    /**
     *  @param propertyName the name of the ant property
     *  @return the value of the ant property
     */
    public String getAntProperty(String propertyName) throws CruiseControlException {
        Iterator props = log.getChild("build").getChild("properties").getChildren("property").iterator();
        return findProperty(props, propertyName);
    }

    private String findProperty(Iterator props, String expected) throws CruiseControlException {
        while (props.hasNext()) {
            Element property = (Element) props.next();
            if (property.getAttributeValue("name").equals(expected)) { return property
                    .getAttributeValue("value"); }
        }
        throw new CruiseControlException("Property: " + expected + " not found.");
    }

    public String getCruiseControlInfoProperty(String name) throws CruiseControlException {
        Iterator props = log.getChild("info").getChildren("property").iterator();
        return findProperty(props, name);
    }

    public Set getModifications() {
        Set results = new HashSet();
        if (isP4Modifications()) {
            //TODO: implement this
        } else {
            Iterator modificationIterator = log.getChild("modifications").getChildren("modification")
                    .iterator();
            while (modificationIterator.hasNext()) {
                Element modification = (Element) modificationIterator.next();
                Modification mod = new Modification();
                mod.fromElement(modification, dateFormat);
                results.add(mod);
            }
        }
        return results;
    }

    public boolean isBuildFix() throws CruiseControlException {
        return !this.wasPreviousBuildSuccessful() && this.isBuildSuccessful();
    }

}
