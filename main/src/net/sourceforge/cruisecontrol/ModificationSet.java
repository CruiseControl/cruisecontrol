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

package net.sourceforge.cruisecontrol;

import org.apache.log4j.Logger;
import org.jdom.Element;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * Set of modifications collected from included SourceControls
 *
 * @see SourceControl
 */
public class ModificationSet {

    private boolean lieOnIsModified = false;
    private static final Logger LOG = Logger.getLogger(ModificationSet.class);
    private static final int ONE_SECOND = 1000;

    private List modifications = new ArrayList();
    private List sourceControls = new ArrayList();
    private int quietPeriod = 60 * ONE_SECOND;
    private Date timeOfCheck;

    /**
     * Set the amount of time in which there is no source control activity
     * after which it is assumed that it is safe to update from the source
     * control system and initiate a build.
     */
    public void setQuietPeriod(int seconds) {
        quietPeriod = seconds * ONE_SECOND;
    }
    
    public void addSourceControl(SourceControl sourceControl) {
        sourceControls.add(sourceControl);
    }

    protected boolean isLastModificationInQuietPeriod(Date now, List modificationList) {
        return (getLastModificationMillis(modificationList) + quietPeriod) >= now.getTime();
    }

    protected long getLastModificationMillis(List modificationList) {
        long lastBuildMillis = 0;
        for (int i = 0; i < modificationList.size(); i++) {
            long temp = 0;
            if (modificationList.get(i) instanceof Modification) {
                temp = ((Modification) modificationList.get(i)).modifiedTime.getTime();
            } 
//            else if (modifications.get(i) instanceof org.jdom.Element) {
//                //set the temp date
//            }
            lastBuildMillis = Math.max(lastBuildMillis, temp);
        }

        return lastBuildMillis;
    }

    protected long getQuietPeriodDifference(Date now, List modificationList) {
        long diff = quietPeriod - (now.getTime() - getLastModificationMillis(modificationList));
        return Math.max(0, diff);
    }

    /**
     * Returns a Hashtable of name-value pairs representing any properties set by the
     * SourceControl.
     * @return Hashtable of properties.
     */
    public Hashtable getProperties() {
        Hashtable table = new Hashtable();
        for (Iterator iter = sourceControls.iterator(); iter.hasNext();) {
            SourceControl control = (SourceControl) iter.next();
            table.putAll(control.getProperties());
        }
        return table;
    }

    /**
     *
     */
    public Element getModifications(Date lastBuild) {
        SimpleDateFormat formatter = new SimpleDateFormat(DateFormatFactory.getFormat());
        Element modificationsElement = null;
        do {
            timeOfCheck = new Date();
            modifications = new ArrayList();
            Iterator sourceControlIterator = sourceControls.iterator();
            while (sourceControlIterator.hasNext()) {
                SourceControl sourceControl = (SourceControl) sourceControlIterator.next();
                modifications.addAll(sourceControl.getModifications(lastBuild, timeOfCheck));
            }
            modificationsElement = new Element("modifications");
            Iterator modificationIterator = modifications.iterator();
            if (modifications.size() > 0) {
                LOG.info(
                    modifications.size()
                        + ((modifications.size() > 1)
                            ? " modifications have been detected."
                            : " modification has been detected."));
            }
            while (modificationIterator.hasNext()) {
                Object object = (Object) modificationIterator.next();
                if (object instanceof org.jdom.Element) {
                    modificationsElement.addContent(((Element) object).detach());
                } else {
                    Modification modification = (Modification) object;
                    Element modificationElement = (modification).toElement(formatter);
                    modification.log(formatter);
                    modificationsElement.addContent(modificationElement);
                }
            }

            if (isLastModificationInQuietPeriod(timeOfCheck, modifications)) {
                LOG.info("A modification has been detected in the quiet period.  ");
                LOG.debug(
                    formatter.format(
                        new Date(timeOfCheck.getTime() - quietPeriod))
                        + " <= Quiet Period <= "
                        + formatter.format(timeOfCheck));
                LOG.debug(
                    "Last modification: "
                        + formatter.format(
                            new Date(
                                getLastModificationMillis(modifications))));
                LOG.info(
                    "Sleeping for "
                        + getQuietPeriodDifference(timeOfCheck, modifications)
                            / 1000
                        + " seconds before retrying.");
                try {
                    Thread.sleep(getQuietPeriodDifference(timeOfCheck, modifications));
                } catch (InterruptedException e) {
                    LOG.error("", e);
                }
            }
        } while (isLastModificationInQuietPeriod(timeOfCheck, modifications));


        return modificationsElement;
    }

    public Date getTimeOfCheck() {
        return timeOfCheck;
    }

    public boolean isModified() {
        return (modifications.size() > 0) || lieOnIsModified;
    }

    public void validate() throws CruiseControlException {
        if (sourceControls.size() == 0) {
            throw new CruiseControlException(
                "modificationset element requires at least one nested source control element");
        }
    }

    int getQuietPeriod() {
        return quietPeriod;
    }

    public void setRequireModification(boolean isModifiedAccurate) {
        lieOnIsModified = !isModifiedAccurate;
    }
}
