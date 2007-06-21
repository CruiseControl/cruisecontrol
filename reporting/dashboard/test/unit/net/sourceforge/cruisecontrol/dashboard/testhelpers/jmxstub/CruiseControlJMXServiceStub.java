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
package net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.ModificationKey;
import net.sourceforge.cruisecontrol.dashboard.service.CruiseControlJMXService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.service.SystemService;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.joda.time.DateTime;

public class CruiseControlJMXServiceStub extends CruiseControlJMXService {
    public CruiseControlJMXServiceStub() {
        super(null, new EnvironmentService(new SystemService()));
    }

    private static final int DEFAULT_HTTP_PORT = 8000;

    private static final int WAITING_LOOP = 6;

    public static final String WAITING = "waiting for next time to build";

    public static final String BUILDING;

    public static final String BOOTSTRAPPING = "bootstrapping";

    public static final String MODIFICATIONSET = "checking for modifications";

    private Map projectStatues = new HashMap();

    private static int waitingLoop;

    static {
        // Tests should not depend on the actual timestamp. And this way
        // you get more meaninful output when using the stub to run the
        // application locally.
        BUILDING = "now building since " + CCDateFormatter.yyyyMMddHHmmss(new DateTime());
    }

    public String getBuildStatus(String projectName) {
        String status = (String) projectStatues.get(projectName);
        if (status == null) {
            status = WAITING;
            projectStatues.put(projectName, status);
            return status;
        }
        if (!WAITING.equals(status)) {
            projectStatues.put(projectName, getNextStatus(status));
            return status;
        } else {
            return WAITING;
        }
    }

    private String getNextStatus(final String status) {
        String nextStatus = "";
        if (BOOTSTRAPPING.equals(status)) {
            return MODIFICATIONSET;
        }
        if (MODIFICATIONSET.equals(status)) {
            waitingLoop = WAITING_LOOP;
            return BUILDING;
        }
        if (BUILDING.equals(status)) {
            if (--waitingLoop == 0) {
                return WAITING;
            }
            return BUILDING;
        }
        return nextStatus;
    }

    public Map getAllProjectsStatus() {
        return new HashMap() {
            private static final long serialVersionUID = 1L;

            public boolean containsKey(Object key) {
                return !"projectWithoutConfiguration".equals(key);
            }

            public Object get(Object key) {
                return getBuildStatus((String) key);
            }

            public boolean isEmpty() {
                return false;
            }
        };
    }

    public List getCommitMessages(String projectName) {
        List list = new ArrayList();
        if ("projectWithoutPublishers".equals(projectName)) {
            return list;
        }
        String status = (String) projectStatues.get(projectName);
        if (BUILDING.equals(status)) {
            ModificationKey key = new ModificationKey("Some random change", "joe");
            list.add(key);
            key = new ModificationKey("Fixed the build", "dev");
            list.add(key);
            return list;
        } else {
            return list;
        }
    }

    public void forceBuild(String projectName) {
        projectStatues.put(projectName, BOOTSTRAPPING);
    }

    public String[] getBuildOutput(String projectName, int firstLine) {
        return new String[] {"Build Failed.\nBuild Duration: 0s"};
    }

    public int getHttpPortForMBeanConsole() {
        return DEFAULT_HTTP_PORT;
    }

    public boolean isCruiseAlive() {
        return true;
    }
}
