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
package net.sourceforge.cruisecontrol;

import org.jdom2.Element;

import java.util.Date;
import java.util.Map;

public class MockSchedule extends Schedule {

    private Map<String, String> properties = null;
    private boolean validateWasCalled = false;

    public MockSchedule() {
        //a schedule isn't valid without at least one builder.
        add(new Builder() {
            public Element build(Map map, Progress progress) throws CruiseControlException { return null; }
            public Element buildWithTarget(Map map, String target, Progress progress)
                    throws CruiseControlException { return null; }
        });
    }

    public Element build(int buildNumber, Date lastBuild, Date now, Map<String, String> propMap, String buildTarget,
                         Progress progress)
      throws CruiseControlException {
        this.properties = propMap;
        return new Element("build");
    }

    public boolean isPaused(Date now) {
        return false;
    }

    protected Map<String, String> getBuildProperties() {
        return properties;
    }


    public long getTimeToNextBuild(Date date, long interval) {
        return interval;
    }

    public void validate() throws CruiseControlException {
        super.validate();
        validateWasCalled = true;
    }

    public boolean validateWasCalled() {
        return validateWasCalled;
    }
}