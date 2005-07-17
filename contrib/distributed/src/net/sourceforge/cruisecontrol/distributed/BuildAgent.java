/****************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2001, ThoughtWorks, Inc.
* 651 W Washington Ave. Suite 600
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed;

import java.io.IOException;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Properties;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.lookup.ServiceIDListener;
import net.jini.lookup.JoinManager;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.sourceforge.cruisecontrol.distributed.util.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.util.ReggieUtil;

import org.apache.log4j.Logger;

public class BuildAgent implements DiscoveryListener,
            ServiceIDListener {

    private static final Logger LOG = Logger.getLogger(BuildAgent.class);

    public static final String JAVA_SECURITY_POLICY = "java.security.policy";
    public static final String JINI_POLICY_FILE = "jini.policy.file";

    private final JoinManager joinManager;
    private ServiceID serviceID;
    private final Remote proxy;

    private Properties entryProperties;
    private Properties configProperties;
    
    private int registrarCount = 0;
    private synchronized void incrementRegCount() {
        registrarCount++;
    }
    private synchronized void decrementRegCount() {
        registrarCount--;
    }
    private synchronized int getRegCount() {
        return registrarCount;
    }

    public BuildAgent() {
        this(BuildAgentServiceImpl.DEFAULT_AGENT_PROPERTIES_FILE,
                BuildAgentServiceImpl.DEFAULT_USER_DEFINED_PROPERTIES_FILE);
    }

    public BuildAgent(final String propsFile, final String userDefinedPropertiesFilename) {
        loadProperties(propsFile, userDefinedPropertiesFilename);

        final BuildAgentServiceImpl serviceImpl = new BuildAgentServiceImpl();
        serviceImpl.setAgentPropertiesFilename(propsFile);
        setService(serviceImpl);

        final Entry[] entries = SearchablePropertyEntries.getPropertiesAsEntryArray(entryProperties);
        Exporter exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                new BasicILFactory(), false, true);

        try {
            proxy = exporter.export(getService());
            joinManager = new JoinManager(getProxy(), entries, this, null, null);
        } catch (IOException e) {
            String message = "Error starting discovery";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }

        getJoinManager().getDiscoveryManager().addDiscoveryListener(this);
    }

    /**
     * @param propsFile
     */
    private void loadProperties(final String propsFile, final String userDefinedPropertiesFilename) {
        configProperties = (Properties) PropertiesHelper.loadRequiredProperties(propsFile);
        entryProperties = new SearchablePropertyEntries(userDefinedPropertiesFilename).getProperties();
        URL policyFile = ClassLoader.getSystemClassLoader().getResource(configProperties.getProperty(JINI_POLICY_FILE));
        System.setProperty(JAVA_SECURITY_POLICY, policyFile.toExternalForm());
        ReggieUtil.setupRMISecurityManager();
    }

    private JoinManager getJoinManager() {
        return joinManager;
    }

    public void terminate() {
        getJoinManager().terminate();
    }


    private Remote getProxy() {
        return proxy;
    }


    private BuildAgentService service;
    private synchronized void setService(BuildAgentService service) {
        this.service = service;
    }
    public synchronized BuildAgentService getService() {
        return service;
    }


    public synchronized void serviceIDNotify(ServiceID serviceID) {
        // @todo technically, should serviceID be stored permanently and reused?....
        this.serviceID = serviceID;
        LOG.info("ServiceID assigned: " + this.serviceID);
    }
    private synchronized ServiceID getServiceID() {
        return serviceID;
    }


    private void logRegistration(ServiceRegistrar registrar) {
        String host = null;
        String message = "";
        try {
            host = registrar.getLocator().getHost();
        } catch (RemoteException e) {
            message = "Failed to get registrar's hostname";
            LOG.warn(message, e);
            System.err.println(message + " - " + e.getMessage());
        }
        message = "Registering BuildAgentService with Registrar: " + host;
        System.out.println(message);
        LOG.info(message);

        String machineName = (String) entryProperties.get("hostname");
        message = "Registered machineName: " + machineName;
        System.out.println(message);
        LOG.debug(message);

        System.out.println("Entries: ");
        for (Iterator iter = entryProperties.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            message = "  " + key + " = " + entryProperties.get(key);
            System.out.println(message);
            LOG.debug(message);
        }
    }

    private boolean isNotFirstDiscovery;

    public void discovered(DiscoveryEvent evt) {
        ServiceRegistrar[] registrarsArray = evt.getRegistrars();
        ServiceRegistrar registrar;
        for (int n = 0; n < registrarsArray.length; n++) {
            incrementRegCount();
            registrar = registrarsArray[n];
            logRegistration(registrar);
            String message = "Registered with registrar: " + registrar.getServiceID();
            System.out.println(message);
            LOG.debug(message);
        }
        if (!isNotFirstDiscovery) {
            final String message = "BuildAgentService open for business...";
            System.out.println(message);
            LOG.info(message);
            isNotFirstDiscovery = true;
        }
    }

    public void discarded(DiscoveryEvent evt) {
        ServiceRegistrar[] registrarsArray = evt.getRegistrars();
        ServiceRegistrar registrar;
        for (int n = 0; n < registrarsArray.length; n++) {
            decrementRegCount();
            registrar = registrarsArray[n];
            String message = "Discarded registrar: " + registrar.getServiceID();
            System.out.println(message);
            LOG.debug(message);
        }
    }


    private static final Object KEEP_ALIVE = new Object();
    public static void terminateMain() {
        KEEP_ALIVE.notifyAll();
    }

    public static void main(String[] args) {
        String message = "Starting agent...";
        System.out.println(message);
        LOG.info(message);
        final BuildAgent buildAgent;
        if (args.length > 0) {
            if (args.length > 1) {
                buildAgent = new BuildAgent(args[0], args[1]);
            } else {
                buildAgent = new BuildAgent(args[0], BuildAgentServiceImpl.DEFAULT_USER_DEFINED_PROPERTIES_FILE);
            }
        } else {
            buildAgent = new BuildAgent();
        }

        // stay around forever
        synchronized (KEEP_ALIVE) {
           try {
               KEEP_ALIVE.wait();
           } catch (InterruptedException e) {
               LOG.error("Keep Alive wait interrupted", e);
           }
        }
        buildAgent.terminate();
    }

}
