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
package net.sourceforge.cruisecontrol;

import org.jdom.Element;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

/**
 * Inner class to hold the build information elements
 * which will be serialized and deseralized by the
 * MasterBuild process.
 */
public class BuildInfo implements Serializable {
    
    public static final String DEFAULT_BUILDINFO_FILENAME = "buildcycleinfo";
    private static final int COMPLETE_TIME_LENGTH = new String("YYYYMMDDHHmmss").length();
    
    private String  label;
    private String  lastGoodBuild;
    private String  lastBuild;

    private transient String  userList;
    private transient String logfile;
    private transient boolean lastBuildSuccessful = true;
    private transient boolean buildNotNecessary;

    /**
     *  Since we'll be storing everything in the build log, we need the build info in an XML representation.  The
     *  XML format is as follows:<br>
     *
     *  <pre>
     *  <info>
     *      <label value=""/>
     *      <lastgoodbuild value=""/>
     *      <lastbuild value=""/>
     *      <previousbuildsuccessful value=""/>
     *      <logfile value=""/>
     *  </info>
     *  </pre>
     *
     *  @return JDOM <code>Element</code> of all of the build info.
     */
    public Element toElement() {
        Element infoElement = new Element("info");

        Element labelElement = new Element("label");
        labelElement.setAttribute("value", label);
        Element lastGoodBuildElement = new Element("lastgoodbuild");
        lastGoodBuildElement.setAttribute("value", lastGoodBuild);
        Element lastBuildElement = new Element("lastbuild");
        lastBuildElement.setAttribute("value", lastBuild);
        Element previousBuildSuccessfulElement = new Element("previousbuildsuccessful");
        previousBuildSuccessfulElement.setAttribute("value", "" + lastBuildSuccessful);

        infoElement.addContent(labelElement);
        infoElement.addContent(lastGoodBuildElement);
        infoElement.addContent(lastBuildElement);
        infoElement.addContent(previousBuildSuccessfulElement);

        return infoElement;
    }

    /**
     *  Ant is going to require a subset of data stored here to be passed in as Ant properties.  This is a convenience
     *  method to populate a map of these key/value pairs.
     *
     *  @return Map consisting of key/value pairs of properties that need to be set for Ant to run correctly.
     */
    public Map toMap() {
        Map result = new HashMap();
        result.put("label", label);
        result.put("lastGoodBuildTime", lastGoodBuild);
        result.put("lastBuildAttemptTime", lastBuild);
        return result;
    }

    /** Getter for property buildNotNecessary.
     * @return Value of property buildNotNecessary.
     */
    public boolean isBuildNotNecessary() {
        return buildNotNecessary;
    }
    
    /** Setter for property buildNotNecessary.
     * @param buildNotNecessary New value of property buildNotNecessary.
     */
    public void setBuildNotNecessary(boolean buildNotNecessary) {
        this.buildNotNecessary = buildNotNecessary;
    }
    
    /** Getter for property label.
     * @return Value of property label.
     */
    public String getLabel() {
        return label;
    }
    
    /** Setter for property label.
     * @param label New value of property label.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * This method delegates to the dynamically loaded LabelIncrementer. The actual
     * implementing class can be declared in the masterbuild.properties file, or
     * the class DefaultLabelIncrementer will be used.
     *
     */
    public void incrementLabel(String labelIncrementClassName) {
        try {
            Class incrementerClass = Class.forName(labelIncrementClassName);
            LabelIncrementer incr = (LabelIncrementer)incrementerClass.newInstance();

            setLabel(incr.incrementLabel(getLabel(), null));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    /** Getter for property lastBuildAttemptTime.
     * @return Value of property lastBuildAttemptTime.
     */
    public String getLastBuild() {
        return lastBuild;
    }
    
    /** Setter for property lastBuildAttemptTime.
     * @param lastBuildAttemptTime New value of property lastBuildAttemptTime.
     */
    public void setLastBuild(String lastBuild) {
        if (!isCompleteTime(lastBuild)) {
            throw new IllegalArgumentException(
                "Bad format for last build: " + lastBuild);
        }
        this.lastBuild = lastBuild;
    }
    
    private boolean isCompleteTime(String time) {
        if (time == null || time.length() < COMPLETE_TIME_LENGTH) {
            return false;
        }

        return true;
    }
    
    /** Getter for property lastBuildSuccessful.
     * @return Value of property lastBuildSuccessful.
     */
    public boolean isLastBuildSuccessful() {
        return lastBuildSuccessful;
    }
    
    /** Setter for property lastBuildSuccessful.
     * @param lastBuildSuccessful New value of property lastBuildSuccessful.
     */
    public void setLastBuildSuccessful(boolean lastBuildSuccessful) {
        this.lastBuildSuccessful = lastBuildSuccessful;
    }
    
    /** Getter for property lastGoodBuildTime.
     * @return Value of property lastGoodBuildTime.
     */
    public String getLastGoodBuild() {
        return lastGoodBuild;
    }
    
    /** Setter for property lastGoodBuildTime.
     * @param lastGoodBuildTime New value of property lastGoodBuildTime.
     */
    public void setLastGoodBuild(String lastGoodBuildTime) {
        this.lastGoodBuild = lastGoodBuildTime;
    }
    
    /** Getter for property userList.
     * @return Value of property userList.
     */
    public String getUserList() {
        return userList;
    }
    
    /** Setter for property userList.
     * @param userList New value of property userList.
     */
    public void setUserList(String userList) {
        this.userList = userList;
    }
    
    public String getLogfile() {
        return logfile;
    }
    
    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }

    public void read() {
        read(DEFAULT_BUILDINFO_FILENAME);
    }

    public void read(String filename) {
        File infoFile = new File(filename);
        System.out.println("Reading build information from : " + infoFile.getAbsolutePath());
        if (!infoFile.exists() || !infoFile.canRead()) {
            System.out.println("Cannot read build information.");
            return;
        }

        try {
            ObjectInputStream s = new ObjectInputStream(new FileInputStream(infoFile));
            
            BuildInfo info = (BuildInfo) s.readObject();
            lastGoodBuild = info.lastGoodBuild;
            lastBuild = info.lastBuild;
            label = info.label;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write() {
        write(DEFAULT_BUILDINFO_FILENAME);
    }

    public void write(String filename) {
        try {
            ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(filename));
            s.writeObject(this);
            s.flush();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean ready() {
        return (lastBuild != null) && (label != null);
    }
    
}
