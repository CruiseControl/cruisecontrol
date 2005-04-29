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
import java.rmi.server.ExportException;
import java.util.Iterator;
import java.util.Properties;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceRegistration;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lease.LeaseRenewalManager;
import net.sourceforge.cruisecontrol.distributed.util.PropertiesHelper;

import org.apache.log4j.Logger;

public class BuildAgent implements DiscoveryListener {

    private static final Logger LOG = Logger.getLogger(BuildAgent.class);

    private static final String JAVA_SECURITY_POLICY = "java.security.policy";
    private static final String JINI_POLICY_FILE = "jini.policy.file";
    private static final String AGENT_PROPERTIES_FILE = "agent.properties";

    private LeaseRenewalManager leaseRenewalManager = new LeaseRenewalManager();
    private LookupDiscovery discover;

    private Properties entryProperties;
    private Properties configProperties;
    
    private int registrarCount = 0;

    public BuildAgent() {
        new BuildAgent(AGENT_PROPERTIES_FILE);
    }

    public BuildAgent(String propsFile) {
        loadProperties(propsFile);
        try {
            discover = new LookupDiscovery(LookupDiscovery.ALL_GROUPS);
            discover.addDiscoveryListener(this);
        } catch (IOException e) {
            String message = "Error starting discovery";
            LOG.error(message, e);
            System.err.println(message + " - " + e.getMessage());
            throw new RuntimeException(message, e);
        }
        while (registrarCount < 1) {
            try {
                Thread.sleep(15 * 1000); // Delay long enough for listener to be registered
                                    // with Discovery thread. 5 secs not enough on 1.4.
            } catch (InterruptedException e) {
                LOG.error(e);
                System.err.println(e);
            }
        }
    }

    /**
     * @param propsFile
     */
    private void loadProperties(String propsFile) {
        configProperties = (Properties) PropertiesHelper.loadRequiredProperties(propsFile);
        entryProperties = new SearchablePropertyEntries().getProperties();
        URL policyFile = ClassLoader.getSystemClassLoader().getResource(configProperties.getProperty(JINI_POLICY_FILE));
        System.setProperty(JAVA_SECURITY_POLICY, policyFile.toExternalForm());
        System.setSecurityManager(new java.rmi.RMISecurityManager());
    }

    private void registerAgent(ServiceRegistrar registrar) {
        try {
            logRegistration(registrar);
            BuildAgentService service = new BuildAgentServiceImpl();
            Exporter exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0), new BasicILFactory(), false,
                    true);
            Remote proxy = exporter.export(service);
            Entry[] entries = SearchablePropertyEntries.getPropertiesAsEntryArray(entryProperties);
            ServiceItem serviceItem = new ServiceItem(null, proxy, entries);
            ServiceRegistration register = registrar.register(serviceItem, Lease.FOREVER);
            leaseRenewalManager.renewFor(register.getLease(), Lease.FOREVER, null);
        } catch (ExportException e) {
            String message = "Failed to export BuildAgentService";
            LOG.warn(message, e);
            System.err.println(message + " - " + e.getMessage());
        } catch (RemoteException e1) {
            String message = "Failed to register BuildAgentService with registrar";
            LOG.warn(message, e1);
            System.err.println(message + " - " + e1.getMessage());
        }
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

    public void discovered(DiscoveryEvent evt) {
        registrarCount++;
        ServiceRegistrar[] registrarsArray = evt.getRegistrars();
        ServiceRegistrar registrar = null;
        for (int n = 0; n < registrarsArray.length; n++) {
            registrar = registrarsArray[n];
            registerAgent(registrar);
            String message = "Registered with registrar: " + registrar.getServiceID();
            System.out.println(message);
            LOG.debug(message);
        }
    }

    public void discarded(DiscoveryEvent evt) {
        registrarCount--;
        ServiceRegistrar[] registrarsArray = evt.getRegistrars();
        ServiceRegistrar registrar = null;
        for (int n = 0; n < registrarsArray.length; n++) {
            registrar = registrarsArray[n];
            String message = "Discarded registrar: " + registrar.getServiceID();
            System.out.println(message);
            LOG.debug(message);
        }
    }

    public static void main(String[] args) {
        String message = "Starting agent...";
        System.out.println(message);
        LOG.info(message);
        BuildAgent agent;
        if (args.length > 0) {
            agent = new BuildAgent(args[0]);
        } else {
            agent = new BuildAgent();
        }
        message = "BuildAgentService open for business...";
        System.out.println(message);
        LOG.info(message);
    }

}
