/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * This class implements the SourceControlElement methods for ClearCase UCM.
 *
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 * @author Alex Batlin
 */
public class UCM implements SourceControl {

    private static final Logger LOG = Logger.getLogger(UCM.class);

    private String stream;
    private String viewPath;
    private String property;
    private boolean contributors = true;

    /*  Date format required by commands passed to ClearCase */
    private final SimpleDateFormat inDateFormatter = new SimpleDateFormat("dd-MMMM-yyyy.HH:mm:ss");

    /*  Date format returned in the output of ClearCase commands. */
    private final SimpleDateFormat outDateFormatter = new SimpleDateFormat("yyyyMMdd.HHmmss");

    private Hashtable properties = new Hashtable();

    /**
     * get the properties created via the sourcecontrol
     *
     * @return Hastable containing properties
     */
    public Map getProperties() {
        return properties;
    }

    /**
     * validate whether enough attributes have been passed
     *
     * @throws CruiseControlException
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(stream, "stream", this.getClass());
        ValidationHelper.assertIsSet(viewPath, "viewpath", this.getClass());
    }

    /**
     * get which stream is being checked
     *
     * @return the name of the stream being checked
     */
    public String getStream() {
        return stream;
    }

    /**
     * set the stream to check for changes
     *
     * @param stream the stream to be checked (via its underlying branch)
     */
    public void setStream(String stream) {
        this.stream = stream;
    }

    /**
     * get the starting point path in a view to check for changes
     *
     * @return path inside a view
     */
    public String getViewPath() {
        return viewPath;
    }

    /**
     * set the starting point path in a view to check for changes
     *
     * @param viewPath path inside a view
     */
    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }

    /**
     * get whether contributors are to be found
     *
     * @return true, if contributors are to be found, else false
     */
    public boolean isContributors() {
        return contributors;
    }

    /**
     * set whether contributors are to be found
     *
     * @param contributors boolean indicating whether contributors are to be found
     */
    public void setContributors(boolean contributors) {
        this.contributors = contributors;
    }

    /**
     * set the name of the property that will be set if modifications are found
     *
     * @param property The name of the property to set
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * get the name of the property that will be set if modifications are found
     *
     * @return the property name
     */
    public String getProperty() {
        return this.property;
    }

    /**
     * Get a List of modifications detailing all the changes between now and
     * the last build. Return this as an element. It is not necessary for
     * sourcecontrols to actually do anything other than returning a chunk
     * of XML data back.
     *
     * @param lastBuild time of last build
     * @param now       time this build started
     * @return a list of XML elements that contains data about the modifications
     *         that took place. If no changes, this method returns an empty list.
     */
    public List getModifications(Date lastBuild, Date now) {
        String lastBuildDate = inDateFormatter.format(lastBuild);
        String nowDate = inDateFormatter.format(now);
        properties.put("ucmlastbuild", lastBuildDate);
        properties.put("ucmnow", nowDate);
        List mods = new ArrayList();
        try {
            HashMap activityNames = collectActivitiesSinceLastBuild(lastBuildDate);
            if (activityNames.size() == 0) {
                return mods;
            }
            mods = describeAllActivities(activityNames);
        } catch (Exception e) {
            LOG.error("Command failed to execute succesfully", e);
        }

        // If modifications were found, set the property
        if (!mods.isEmpty() && getProperty() != null) {
            properties.put(getProperty(), "true");
        }

        return mods;
    }

    /*
     * get all the activities on the stream since the last build date
     */
    private HashMap collectActivitiesSinceLastBuild(String lastBuildDate) {

        LOG.debug("Last build time was: " + lastBuildDate);

        HashMap activityMap = new HashMap();

        Commandline commandLine = buildListStreamCommand(lastBuildDate);
        LOG.debug("Executing: " + commandLine);

        try {
            Process p = Runtime.getRuntime().exec(commandLine.getCommandline());
            StreamPumper errorPumper = new StreamPumper(p.getErrorStream(), new PrintWriter(System.err, true));
            new Thread(errorPumper).start();
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;

            while (((line = br.readLine()) != null) && (!br.equals(""))) {
                String[] details = getDetails(line);
                if (details[0].equals("mkbranch") || details[0].equals("rmbranch") || details[0].equals("rmver")) {
                    // if type is create/remove branch then skip
                } else {
                    String activityName = details[1];
                    String activityDate = details[2];
                    // assume the latest change for an activity is listed first
                    if (!activityMap.containsKey(activityName)) {
                        LOG.debug("Found activity name: " + activityName + "; date: " + activityDate);
                        activityMap.put(activityName, activityDate);
                    }
                }
            }

            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (IOException e) {
            LOG.error("IO Error executing ClearCase lshistory command", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupt Error executing ClearCase lshistory command", e);
        }

        return activityMap;
    }

    private String[] getDetails(String line) {
        return line.split("~#~");
    }

    /*
     * construct a command to get all the activities on the specified stream
     */
    public Commandline buildListStreamCommand(String lastBuildDate) {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");
        commandLine.createArgument().setValue("lshistory");
        commandLine.createArgument().setValue("-branch");
        commandLine.createArgument().setValue(getStream());
        commandLine.createArgument().setValue("-r");
        commandLine.createArgument().setValue("-nco");
        commandLine.createArgument().setValue("-since");
        commandLine.createArgument().setValue(lastBuildDate);
        commandLine.createArgument().setValue("-fmt");
        commandLine.createArgument().setValue("%o~#~%[activity]Xp~#~%Nd\n");
        commandLine.createArgument().setValue(getViewPath());
        return commandLine;
    }

    /*
     * get all the activities on the stream since the last build date
     */
    private List describeAllActivities(HashMap activityNames) {

        ArrayList activityList = new ArrayList();

        Iterator it = activityNames.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry activity = (Map.Entry) it.next();
            String activityID = activity.getKey().toString();
            String activityDate = activity.getValue().toString();
            UCMModification activityMod = describeActivity(activityID, activityDate);
            activityList.add(activityMod);

            // check for contributor activities
            if (activityMod.comment.startsWith("deliver ") && isContributors()) {
                List contribList;
                contribList = describeContributors(activityID);
                Iterator contribIter = contribList.iterator();
                while (contribIter.hasNext()) {
                    String contribName = contribIter.next().toString();
                    UCMModification contribMod = describeActivity(contribName, activityDate);
                    // prefix type to make it stand out in Build Results report
                    contribMod.type = "contributor";
                    LOG.debug("Found contributor name: " + contribName + "; date: " + activityDate);
                    activityList.add(contribMod);
                }
            }
        }

        return activityList;
    }

    /*
     * get all the activities on the stream since the last build date
     */
    private UCMModification describeActivity(String activityID, String activityDate) {

        UCMModification mod = new UCMModification();

        Commandline commandLine = buildDescribeActivityCommand(activityID);
        LOG.debug("Executing: " + commandLine);

        try {
            Process p = Runtime.getRuntime().exec(commandLine.getCommandline());
            StreamPumper errorPumper = new StreamPumper(p.getErrorStream(), new PrintWriter(System.err, true));
            new Thread(errorPumper).start();
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;

            while (((line = br.readLine()) != null) && (!br.equals(""))) {
                String[] details = getDetails(line);
                try {
                    mod.modifiedTime = outDateFormatter.parse(activityDate);
                } catch (ParseException e) {
                    LOG.error("Error parsing modification date");
                }
                mod.type = "activity";
                // counter for UCM without ClearQuest
                if (details[0].equals("")) {
                    mod.revision = details[3];
                } else {
                    mod.revision = details[0];
                }
                mod.crmtype = details[1];
                mod.userName = details[2];
                mod.comment = details[3];
            }

            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (IOException e) {
            LOG.error("IO Error executing ClearCase describe command", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupt error executing ClearCase describe command", e);
        }

        return mod;
    }

    /*
     * construct a command to get all the activities on the specified stream
     */
    public Commandline buildDescribeActivityCommand(String activityID) {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");
        commandLine.createArgument().setValue("describe");
        commandLine.createArgument().setValue("-fmt");
        commandLine.createArgument().setValue("%[crm_record_id]p~#~%[crm_record_type]p~#~%u~#~%[headline]p~#~");
        commandLine.createArgument().setValue(activityID);
        return commandLine;
    }

    /*
     * get all the activities on the stream since the last build date
     */
    private List describeContributors(String activityName) {

        ArrayList contribList = new ArrayList();
        Commandline commandLine = buildListContributorsCommand(activityName);
        LOG.debug("Executing: " + commandLine);

        try {
            Process p = Runtime.getRuntime().exec(commandLine.getCommandline());
            StreamPumper errorPumper = new StreamPumper(p.getErrorStream(), new PrintWriter(System.err, true));
            new Thread(errorPumper).start();

            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                String[] contribs = splitOnSpace(line);
                for (int i = 0; i < contribs.length; i++) {
                    contribList.add(contribs[i]);
                }
            }

            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (IOException e) {
            LOG.error("IO Error executing ClearCase describe contributors command", e);
        } catch (InterruptedException e) {
            LOG.error("Interrupt Error executing ClearCase describe contributors command", e);
        }

        return contribList;
    }

    private String[] splitOnSpace(String string) {
        return string.split(" ");
    }

    /*
     * construct a command to get all the activities on the specified stream
     */
    public Commandline buildListContributorsCommand(String activityID) {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");
        commandLine.createArgument().setValue("describe");
        commandLine.createArgument().setValue("-fmt");
        commandLine.createArgument().setValue("\"%[contrib_acts]Xp\"");
        commandLine.createArgument().setValue(activityID);
        return commandLine;
    }

    /*
     * class to hold UCMModifications
     */
    private static class UCMModification extends Modification {
        private static final String TAGNAME_CRMTYPE = "crmtype";

        public String crmtype;

        public int compareTo(Object o) {
            UCMModification modification = (UCMModification) o;
            return getActivitityNumber() - modification.getActivitityNumber();
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof UCMModification)) {
                return false;
            }

            UCMModification modification = (UCMModification) o;
            return getActivitityNumber() == modification.getActivitityNumber();
        }

        public int hashCode() {
            return getActivitityNumber();
        }

        private int getActivitityNumber() {
            return Integer.parseInt(revision);
        }

        UCMModification() {
            super("ucm");
        }

        public Element toElement(DateFormat formatter) {
            Element modificationElement = super.toElement(formatter);
            Element crmtypeElement = new Element(TAGNAME_CRMTYPE);
            crmtypeElement.addContent(crmtype);
            modificationElement.addContent(crmtypeElement);
            return modificationElement;
        }

    }

    /**
     * for testing
     */
    public static void main(String[] args) {
        UCM ucmtest = new UCM();
        ucmtest.setStream("RatlBankModel_Int");
        //ucmtest.setViewPath("C:\\Views\\RatlBankModel_int\\RatlBankSources\\model");
        ucmtest.setViewPath("/view/RatlBankModel_int/vobs/RatlBankSources/model");
        //ucmtest.setContributors(false);
        List changes = new ArrayList();

        try {
            changes = ucmtest.getModifications(new SimpleDateFormat("yyyyMMdd.HHmmss").parse("20050822.095914"),
                    new Date());
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println(changes.toString());
    }

}
