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

import org.apache.log4j.Logger;
import org.jdom.Element;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 */
public class ModificationSet {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(ModificationSet.class);

    protected List _modifications = new ArrayList();
    protected List _sourceControls = new ArrayList();
    protected SimpleDateFormat _formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    protected int _quietPeriod;
    protected Date _now;

    /**
     *
     */
    public void setQuietPeriod(int seconds) {
        _quietPeriod = seconds * 1000;
    }

    /**
     *
     */
    public void setDateFormat(String dateFormat) {
        _formatter = new SimpleDateFormat(dateFormat);
    }

    public void addSourceControl(SourceControl sourceControl) {
        _sourceControls.add(sourceControl);
    }

    protected boolean isLastModificationInQuietPeriod(Date now, List modifications) {
        return (getLastModificationMillis(modifications) + _quietPeriod) >= now.getTime();
    }

    protected long getLastModificationMillis(List modifications) {
        long lastBuildMillis = 0;
        for (int i = 0; i < modifications.size(); i++) {
            long temp = 0;
            if (modifications.get(i) instanceof Modification) {
                temp = ((Modification) modifications.get(i)).modifiedTime.getTime();
            } else if (modifications.get(i) instanceof org.jdom.Element) {
                //set the temp date
            }
            lastBuildMillis = Math.max(lastBuildMillis, temp);
        }

        return lastBuildMillis;
    }

    protected long getQuietPeriodDifference(Date now, List modifications) {
        long diff = _quietPeriod - (now.getTime() - getLastModificationMillis(modifications));
        return Math.max(0, diff);
    }

    /**
     * Returns a Hashtable of name-value pairs representing any properties set by the
     * SourceControl.
     * @return Hashtable of properties.
     */
    public Hashtable getProperties() {
        Hashtable table = new Hashtable();
        for (Iterator iter = _sourceControls.iterator(); iter.hasNext();) {
            SourceControl control = (SourceControl) iter.next();
            table.putAll(control.getProperties());
        }
        return table;
    }

    /**
     *
     */
    public Element getModifications(Date lastBuild) {
        Element modificationsElement = null;
        do {
            _now = new Date();
            _modifications = new ArrayList();
            Iterator sourceControlIterator = _sourceControls.iterator();
            while (sourceControlIterator.hasNext()) {
                SourceControl sourceControl = (SourceControl) sourceControlIterator.next();
                _modifications.addAll(sourceControl.getModifications(lastBuild, _now));
            }
            modificationsElement = new Element("modifications");
            Iterator modificationIterator = _modifications.iterator();
            if (_modifications.size() > 0) {
                log.info(_modifications.size() + ((_modifications.size() > 1) ? " modifications have been detected." : " modification has been detected."));
            }
            while (modificationIterator.hasNext()) {
                Object object = (Object) modificationIterator.next();
                if (object instanceof org.jdom.Element) {
                    modificationsElement.addContent(((Element) object).detach());
                } else {
                    Modification modification = (Modification) object;
                    Element modificationElement = (modification).toElement(_formatter);
                    modification.log(_formatter);
                    modificationsElement.addContent(modificationElement);
                }
            }

            if(isLastModificationInQuietPeriod(_now, _modifications)) {
                log.info("A modification has been detected in the quiet period.  ");
                log.debug(_formatter.format(new Date(_now.getTime() - _quietPeriod)) + " <= Quiet Period <= " + _formatter.format(_now));
                log.debug("Last modification: " + _formatter.format(new Date(getLastModificationMillis(_modifications))));
                log.info("Sleeping for " + getQuietPeriodDifference(_now, _modifications)/1000 + " seconds before retrying.");
                try {
                    Thread.sleep(getQuietPeriodDifference(_now, _modifications));
                } catch (InterruptedException e) {
                    log.error("", e);
                }
            }
        } while (isLastModificationInQuietPeriod(_now, _modifications));


        return modificationsElement;
    }

    public Date getNow() {
        return _now;
    }

    public boolean isModified() {
        return _modifications.size() > 0;
    }
}
