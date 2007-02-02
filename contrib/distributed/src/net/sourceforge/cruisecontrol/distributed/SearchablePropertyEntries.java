/****************************************************************************
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.jini.core.entry.Entry;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;

import org.apache.log4j.Logger;

public class SearchablePropertyEntries {

    private static final Logger LOG = Logger.getLogger(SearchablePropertyEntries.class);

    private static final String OS_NAME = "os.name";
    private static final String JAVA_VM_VERSION = "java.vm.version";
    private static final String HOSTNAME = "hostname";

    private Properties entryProperties = new Properties();

    public Properties getProperties() {
        return entryProperties;
    }

    public SearchablePropertyEntries(final String userDefinedPropertiesFilename) {
        try {
            String osName = System.getProperty(OS_NAME);
            entryProperties.put(OS_NAME, osName);
            LOG.debug("Set search entry " + OS_NAME + " to: " + osName);

            String javaVmVersion = System.getProperty(JAVA_VM_VERSION);
            entryProperties.put(JAVA_VM_VERSION, javaVmVersion);
            LOG.debug("Set search entry " + JAVA_VM_VERSION + " to: " + javaVmVersion);

            String hostname = InetAddress.getLocalHost().getHostName();
            entryProperties.put(HOSTNAME, hostname);
            LOG.debug("Set search entry " + HOSTNAME + " to: " + hostname);

            Map tempProperties = PropertiesHelper.loadOptionalProperties(userDefinedPropertiesFilename);
            for (Iterator iter = tempProperties.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                String value = (String) tempProperties.get(key);
                entryProperties.put(key, value);
                LOG.debug("Set user-defined search entry " + key + " to: " + value);
            }
        } catch (UnknownHostException e) {
            String message = "Failed to set hostname";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    public static Entry[] getPropertiesAsEntryArray(Properties properties) {
        List entries = new ArrayList();
        for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            entries.add(new PropertyEntry((String) entry.getKey(), (String) entry.getValue()));
        }
        return (Entry[]) entries.toArray(new PropertyEntry[entries.size()]);
    }

}
