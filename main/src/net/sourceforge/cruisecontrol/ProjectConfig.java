/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Iterator;
import java.util.Date;

import net.sourceforge.cruisecontrol.util.ValidationHelper;

/** 
 * A plugin that represents the project node
 * @author <a href="mailto:jerome@coffeebreaks.org">Jerome Lacoste</a>
 */
public class ProjectConfig implements Serializable {

    private String name;
    private boolean buildAfterFailed = true;
    private boolean forceOnly = false;
    private boolean requiremodification = true;
    private CCDateFormat dateFormat;

    private Bootstrappers bootstrappers;
    private LabelIncrementer labelIncrementer;
    private Listeners listeners;
    private Log log;
    private ModificationSet modificationSet;
    private Publishers publishers;
    private Schedule schedule;

    private Map properties;

    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        if (dateFormat != null) {
             dateFormat.validate();
        }
        ValidationHelper.assertTrue(schedule != null, "project requires a schedule");

        if (bootstrappers != null) {
            bootstrappers.validate();
        }

        if (listeners != null) {
            listeners.validate();
        }

        if (log != null) {
            log.validate();
        }

        if (modificationSet != null) {
            modificationSet.validate();
        }

        if (schedule != null) {
            schedule.validate();
        }

        if (publishers != null) {
            publishers.validate();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    // TODO think about how to turn properties into plugins
    void setProperties(Map properties) {
        this.properties = properties;
    }

    public void setBuildAfterFailed(boolean buildAfterFailed) {
        this.buildAfterFailed = buildAfterFailed;
    }

    public void add(CCDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void add(ModificationSet modificationSet) {
        this.modificationSet = modificationSet;
    }

    public void add(Bootstrappers bootstrappers) {
        this.bootstrappers = bootstrappers;
    }

    public void add(Listeners listeners) {
        this.listeners = listeners;
    }

    public void add(Publishers publishers) {
        this.publishers = publishers;
    }

    public void add(Schedule schedule) {
        this.schedule = schedule;
    }

    public void add(Log log) {
        this.log = log;
    }

    public void add(LabelIncrementer labelIncrementer) {
        this.labelIncrementer = labelIncrementer;
    }

    public CCDateFormat getDateFormat() { return dateFormat; }
    
    public boolean shouldBuildAfterFailed() { return buildAfterFailed; }

    //TODO: This method seems useless. I suspect that callers aren't asking ProjectConfig the "right" questions.
    public Log getLog() { return log; }

    public List getBootstrappers() { 
      return bootstrappers == null ? Collections.EMPTY_LIST : bootstrappers.getBootstrappers();
    }

    public List getListeners() { return listeners == null ? Collections.EMPTY_LIST : listeners.getListeners(); }

    public List getPublishers() { return publishers == null ? Collections.EMPTY_LIST : publishers.getPublishers(); }

    public ModificationSet getModificationSet() { return modificationSet; }

    public Schedule getSchedule() { return schedule; }

    public LabelIncrementer getLabelIncrementer() { return labelIncrementer; }

    public String getName() { return name; }

    // TODO: keep as Map ??
    public Properties getProperties() {
        Properties props = new Properties();
        props.putAll(properties);
        return props;
    }

    public void writeLogFile(Date now) throws CruiseControlException {
        log.writeLogFile(now);
    }

    public boolean wasBuildSuccessful() {
        return log.wasBuildSuccessful();
    }

    public static class Bootstrappers {
        private List bootstrappers = new ArrayList();
        public void add(Bootstrapper bootstrapper) {
            bootstrappers.add(bootstrapper);
        }
        public List getBootstrappers() { return bootstrappers; }

        public void validate() throws CruiseControlException {
            for (Iterator iterator = bootstrappers.iterator(); iterator.hasNext();) {
                Bootstrapper nextBootstrapper = (Bootstrapper) iterator.next();
                nextBootstrapper.validate();
            }
        }
    }

    public static class Listeners {
        private List listeners = new ArrayList();
        public void add(Listener listener) {
            listeners.add(listener);
        }
        public List getListeners() { return listeners; }

        public void validate() throws CruiseControlException {
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                Listener nextListener = (Listener) iterator.next();
                nextListener.validate();
            }
        }
    }

    public static class Publishers {
        private List publishers = new ArrayList();
        public void add(Publisher publisher) {
            publishers.add(publisher);
        }
        public List getPublishers() { return publishers; }

        public void validate() throws CruiseControlException {
            for (Iterator iterator = publishers.iterator(); iterator.hasNext();) {
                Publisher nextPublisher = (Publisher) iterator.next();
                nextPublisher.validate();
            }

        }
    }

    /**
     * @param forceOnly the forceOnly to set
     */
    public void setForceOnly(boolean forceOnly) {
        this.forceOnly = forceOnly;
    }

    /**
     * @return the forceOnly
     */
    public boolean isForceOnly() {
        return forceOnly;
    }

    /**
     * @return the requiremodification
     */
    public boolean isRequiremodification() {
        return requiremodification;
    }

    /**
     * @param requiremodification the requiremodification to set
     */
    public void setRequiremodification(boolean requiremodification) {
        this.requiremodification = requiremodification;
    }
}
