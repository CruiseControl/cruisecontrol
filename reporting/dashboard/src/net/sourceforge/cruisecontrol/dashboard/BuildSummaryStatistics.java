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

    public BuildSummaryStatistics(List buildSummaryList) {
        for (int i = 0; i < buildSummaryList.size(); i++) {
            Build summary = (Build) buildSummaryList.get(i);
            ((CounterHashMap) counterMap).put(summary.getStatus());
        }
    }

    public Integer failed() {
        return (Integer) counterMap.get(ProjectBuildStatus.FAILED);

    }

    public Integer building() {
        return (Integer) counterMap.get(ProjectBuildStatus.BUILDING);
    }

    public Integer passed() {
        return (Integer) counterMap.get(ProjectBuildStatus.PASSED);
    }

    public Integer inactive() {
        return (Integer) counterMap.get(ProjectBuildStatus.INACTIVE);
    }

    public Integer total() {
        return new Integer((failed().intValue() + building().intValue() + passed().intValue() + inactive()
                .intValue()));
    }

    public String rate() {
        int totalWithOutInActiveBuilds = total().intValue() - inactive().intValue();
        return total().intValue() > 0 ? Math
                .round(((passed().intValue() * 1.0 / totalWithOutInActiveBuilds) * PERCENTAGE)) + "%" : "0%";
    }

    private static class CounterHashMap extends HashMap {
        private static final long serialVersionUID = 1L;

        public void put(ProjectBuildStatus key) {
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
