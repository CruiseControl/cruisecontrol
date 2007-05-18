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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import net.sourceforge.cruisecontrol.dashboard.ModificationKey;
import org.apache.log4j.Logger;

public class CruiseControlJMXService {

    public static final String JMXATTR_BUILD_STATUS = "Status";
    public static final String JMXCOMMAND_BUILD = "build";
    public static final String JMXCOMMAND_COMMIT_MESSAGE = "CommitMessages";
    public static final String JMXCOMMAND_BUILD_OUTPUT = "getBuildOutput";
    public static final String JMXCOMMAND_ALL_PROJECT_STATUS = "AllProjectsStatus";

    private static final Logger LOGGER = Logger.getLogger(CruiseControlJMXService.class);

    private MBeanServerConnection jmxConnection;
    private int httpPort = -1;

    public String getBuildStatus(String projectName) {
        try {
            String buildStatus = (String) jmxConnection.getAttribute(getObjectName(projectName), JMXATTR_BUILD_STATUS);
            return buildStatus;
        } catch (Exception e) {
            return null;
        }
    }

    private ObjectName getObjectName(String projectName) throws MalformedObjectNameException {
        return ObjectName.getInstance("CruiseControl Project:name=" + projectName);
    }

    public void fourceBuild(String projectName) throws Exception {
        jmxConnection.invoke(getObjectName(projectName), JMXCOMMAND_BUILD, null, null);
    }

    public MBeanServerConnection getJmxConnector() {
        return jmxConnection;
    }

    public void setJmxConnector(MBeanServerConnection jmxConnector) {
        jmxConnection = jmxConnector;
    }

    public List getCommitMessages(String projectName) {
        List list = new ArrayList();
        String[][] commitMessages;
        try {
            commitMessages = (String[][]) jmxConnection
                    .getAttribute(getObjectName(projectName), JMXCOMMAND_COMMIT_MESSAGE);
        } catch (Exception e) {
            return list;
        }
        for (int i = 0; i < commitMessages.length; i++) {
            ModificationKey key = new ModificationKey(commitMessages[i][1], commitMessages[i][0]);
            list.add(key);
        }
        return list;
    }

    public Map getAllProjectsStatus() {
        Map result = new HashMap();
        String ccManagerName = "CruiseControl Manager:id=unique";
        try {
            ObjectName objectName = ObjectName.getInstance(ccManagerName);
            Map projectsInfo = (Map) jmxConnection.getAttribute(objectName, JMXCOMMAND_ALL_PROJECT_STATUS);
            for (Iterator iter = projectsInfo.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                result.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to connect to CruiseControl build loop via JMX.", e);
        }
        return result;
    }

    public String[] getBuildOutput(String projectName, int firstLine) {
        try {
            return (String[]) jmxConnection.invoke(getObjectName(projectName), JMXCOMMAND_BUILD_OUTPUT,
                    new Object[]{new Integer(firstLine)}, new String[]{Integer.class.getName()});
        } catch (Exception e) {
            return new String[]{e.toString()};
        }
    }

    public int getHttpPortForMBeanConsole() {
        if (httpPort == -1) {
            httpPort = getPortOfFirstHttpAdapter();
        }
        return httpPort;
    }

    private int getPortOfFirstHttpAdapter() {
        try {
            Set httpAdpaters = jmxConnection.queryNames(new ObjectName("Adapter:name=HttpAdaptor,*"), null);
            if (httpAdpaters.isEmpty()) {
                return -1;
            }
            ObjectName httpAdapter = (ObjectName) httpAdpaters.iterator().next();
            return ((Integer) jmxConnection.getAttribute(httpAdapter, "Port")).intValue();
        } catch (Exception e) {
            LOGGER.warn("Failed to connect to CruiseControl build loop via JMX.", e);
            return -1;
        }
    }
}