/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2004, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.chart;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.cruisecontrol.BuildInfo;
import net.sourceforge.cruisecontrol.BuildInfoSummary;
import net.sourceforge.cruisecontrol.taglib.BuildInfoTag;
import de.laures.cewolf.DatasetProducer;

/**
 * TODO: TYpe comment.
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public abstract class AbstractCruiseControlChartData implements DatasetProducer, Serializable {

    /**
     * Helper method to see if the graph is out of date. We will check the BuildInfoSummary
     * to see if a new log file is available.
     */
    public boolean hasExpired(Map params, Date dateOfCachedData) {
        BuildInfoSummary summary = getBuildInfoSummary(params);
        if (noBuilds(summary)) {
            return false;
        }
        
        Date newestBuild = null;
        Iterator iterator = summary.iterator();
        while (iterator.hasNext()) {
            BuildInfo info = (BuildInfo) iterator.next();
            Date buildDate = info.getBuildDate();
            if (newestBuild == null || buildDate.after(newestBuild)) {
                newestBuild = buildDate;
            }
        }
        
        return newestBuild.after(dateOfCachedData);
    }

    private boolean noBuilds(BuildInfoSummary summary) {
        return summary.getBuildInfoList().size() == 0;
    }

    protected BuildInfoSummary getBuildInfoSummary(Map params) {
        BuildInfoSummary summary = (BuildInfoSummary) params.get(BuildInfoTag.INFO_ATTRIBUTE);
        return summary;
    }


}
