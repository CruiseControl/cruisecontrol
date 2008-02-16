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
package net.sourceforge.cruisecontrol;

import java.util.List;
import java.util.ArrayList;

import net.sourceforge.cruisecontrol.util.BuildInformationHelper;
import net.sourceforge.cruisecontrol.util.UniqueBuildloopIdentifier;

public class BuildLoopInformation {

    private String uuid;

    private JmxInfo jmx;

    private String servername = "";

    private String timestamp = "";

    private ProjectInfo[] projects;

    public BuildLoopInformation(ProjectInfo[] projects, JmxInfo jmxinfo, String serverName, String timestamp) {
        this.projects = projects;
        this.jmx = jmxinfo;
        this.servername = serverName;
        this.timestamp = timestamp;
        this.uuid = UniqueBuildloopIdentifier.id().toString();
    }

    public String getServerName() {
        return servername;
    }

    public JmxInfo getJmxInfo() {
        return jmx;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public ProjectInfo[] getProjects() {
        return projects;
    }

    public String getUuid() {
        return uuid;
    }

    public String toXml() {
        return new BuildInformationHelper().toXml(this);
    }

    public static class JmxInfo {

        public static final String CRUISECONTROL_RMIPORT = "cruisecontrol.rmiport";

        public static final String CRUISECONTROL_JMXPORT = "cruisecontrol.jmxport";

        public static final String JMX_HTTP_USERNAME = "jmx.http.username";

        public static final String JMX_HTTP_PASSWORD = "jmx.http.password";

        private String httpurl = "";

        private String rmiurl = "";

        private String username = System.getProperty(JMX_HTTP_USERNAME);

        private String password = System.getProperty(JMX_HTTP_PASSWORD);

        public JmxInfo(String serverName) {

            String httpPort = System.getProperty(CRUISECONTROL_JMXPORT);
            if (httpPort != null) {
                this.httpurl = "http://" + serverName + ":" + httpPort;
            }

            String rmiPort = System.getProperty(CRUISECONTROL_RMIPORT);
            if (rmiPort != null) {
                this.rmiurl = "rmi://" + serverName + ":" + rmiPort;
            }
        }

        public String getHttpAdpatorUrl() {
            return httpurl;
        }

        public String getRmiUrl() {
            return rmiurl;
        }

        public String getUserName() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + httpurl.hashCode();
            result = prime * result + ((password == null) ? 0 : password.hashCode());
            result = prime * result + rmiurl.hashCode();
            result = prime * result + ((username == null) ? 0 : username.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            return equals((JmxInfo) obj);
        }

        private boolean equals(final JmxInfo other) {
            if (!httpurl.equals(other.httpurl)) {
                return false;
            }
            if (!rmiurl.equals(other.rmiurl)) {
                return false;
            }
            if (password == null) {
                if (other.password != null) {
                    return false;
                }
            } else if (!password.equals(other.password)) {
                return false;
            }
            if (username == null) {
                if (other.username != null) {
                    return false;
                }
            } else if (!username.equals(other.username)) {
                return false;
            }
            return true;
        }
    }

    public static class ProjectInfo {
        private String name;

        private String status;

        private String buildstarttime;

        private List modifications = new ArrayList();

        public ProjectInfo(String name, String status, String buildStartTime) {
            this.name = name;
            this.status = status;
            this.buildstarttime = buildStartTime;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public String getBuildStartTime() {
            return buildstarttime;
        }

        public List getModifications() {
            return modifications;
        }

        public void setModifications(List modifications) {
            this.modifications = modifications;
        }
    }
}
