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

import java.util.*;
import java.text.SimpleDateFormat;

/**
 *
 */
public class ModificationSet {

    private List _modifications = new ArrayList();
    private List _sourceControls = new ArrayList();
    private SimpleDateFormat _formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    private int _quietPeriod;

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

    /**
     *
     */
    public Element getModifications(Date lastBuild) {
        Date now = new Date();
        Iterator sourceControlIterator = _sourceControls.iterator();
        while (sourceControlIterator.hasNext()) {
            SourceControl sourceControl = (SourceControl) sourceControlIterator.next();
            _modifications.addAll(sourceControl.getModifications(lastBuild, now, _quietPeriod));
        }

        Collections.sort(_modifications);
        Element modificationsElement = new Element("modifications");
        Iterator modificationIterator = _modifications.iterator();
        while (modificationIterator.hasNext()) {
            Modification modification = (Modification) modificationIterator.next();
            Element modificationElement = (modification).toElement(_formatter);
            modificationsElement.addContent(modificationElement);
            modification.log(_formatter);
        }

        return modificationsElement;
    }

    public int size() {
        return _modifications.size();
    }
}
