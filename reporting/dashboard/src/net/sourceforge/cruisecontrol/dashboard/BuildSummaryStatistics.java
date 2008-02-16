/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildSummaryStatistics {
    private static final int PERCENTAGE = 100;

    private Map counterMap = new CounterHashMap();

    private final List buildSummaryList;

    public BuildSummaryStatistics(List buildSummaryList) {
        this.buildSummaryList = buildSummaryList;
        for (int i = 0; i < buildSummaryList.size(); i++) {
            BuildSummary summary = (BuildSummary) buildSummaryList.get(i);
            if (summary.isInactive()) {
                continue;
            }
            if (summary.getCurrentStatus().equals(CurrentStatus.BUILDING)
                    || summary.getCurrentStatus().equals(CurrentStatus.DISCONTINUED)) {
                ((CounterHashMap) counterMap).put(summary.getCurrentStatus());
            } else {
                ((CounterHashMap) counterMap).put(summary.getPreviousBuildResult());
            }
        }
    }

    public Integer failed() {
        return (Integer) counterMap.get(PreviousResult.FAILED);

    }

    public Integer building() {
        return (Integer) counterMap.get(CurrentStatus.BUILDING);
    }

    public Integer passed() {
        return (Integer) counterMap.get(PreviousResult.PASSED);
    }

    public Integer inactive() {
        int count = 0;
        for (int i = 0; i < buildSummaryList.size(); i++) {
            BuildSummary summary = (BuildSummary) buildSummaryList.get(i);
            if (summary.isInactive()) {
                count++;
            }
        }
        return new Integer(count);
    }

    public Integer discontinued() {
        Integer currentInactive = (Integer) counterMap.get(CurrentStatus.DISCONTINUED);
        return new Integer(currentInactive.intValue());
    }

    public Integer total() {
        return new Integer((failed().intValue() + building().intValue() + passed().intValue()));
    }

    public String rate() {
        int totalWithOutInActiveBuilds = total().intValue();
        return total().intValue() > 0 ? Math
                .round(((passed().intValue() * 1.0 / totalWithOutInActiveBuilds) * PERCENTAGE))
                + "%" : "0%";
    }

    public int hashCode() {
        return counterMap.hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        return equals((BuildSummaryStatistics) other);
    }

    private boolean equals(final BuildSummaryStatistics other) {
        return this.counterMap.equals(other.counterMap);
    }

    private static class CounterHashMap extends HashMap {
        private static final long serialVersionUID = 1L;

        public void put(ViewableStatus key) {
            Object value = this.get(key);
            if (value == null) {
                this.put(key, new Integer(1));
            } else {
                int increased = ((Integer) value).intValue();
                this.put(key, new Integer(++increased));
            }
        }

        public Object get(Object key) {
            if (!this.containsKey(key)) {
                return new Integer(0);
            }
            return super.get(key);
        }
    }

}
