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

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.awt.GraphicsEnvironment;

import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.discovery.LookupLocator;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupLocatorDiscovery;
import net.jini.lookup.ServiceIDListener;
import net.jini.lookup.JoinManager;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.core.ReggieUtil;
import net.sourceforge.cruisecontrol.distributed.core.CCDistVersion;
import net.sourceforge.cruisecontrol.util.MainArgs;

import org.apache.log4j.Logger;



public class BuildAgent implements DiscoveryListener,
            ServiceIDListener {

    static final String MAIN_ARG_AGENT_PROPS = "agentprops";
    static final String MAIN_ARG_USER_PROPS = "userprops";
    static final String MAIN_ARG_SKIP_UI = "skipUI";

    // package visible to allow BuildAgentUI console logger access to this Logger
    static final Logger LOG = Logger.getLogger(BuildAgent.class);

    public static final String JAVA_SECURITY_POLICY = "java.security.policy";
    private static final String JINI_POLICY_FILE = "jini.policy.file";

    /** Optional unicast Lookup Registry URL.
     * A Unicast Lookup Locater is useful if multicast isn't working. */
    private static final String REGISTRY_URL = "registry.url";

    private final BuildAgentServiceImpl serviceImpl;
    private final PropertyEntry[] origEntries;
    private final Exporter exporter;
    private final JoinManager joinManager;
    private ServiceID serviceID;
    private final Remote proxy;

    private Properties entryProperties;
    private Properties configProperties;

    private final BuildAgentUI ui;


    static interface LUSCountListener {
        public void lusCountChanged(final int newLUSCount);
    }
    private final List lusCountListeners = new ArrayList();
    void addLUSCountListener(final LUSCountListener listener) {
        lusCountListeners.add(listener);
    }
    void removeLUSCountListener(final LUSCountListener listener) {
        lusCountListeners.remove(listener);
    }

    private int registrarCount = 0;

    private void fireLUSCountChanged() {
        for (int i = 0; i < lusCountListeners.size(); i++) {
            ((LUSCountListener) lusCountListeners.get(i)).lusCountChanged(registrarCount);
        }
    }
    private void setRegCount(final int regCount) {
        registrarCount = regCount;
        LOG.info("Lookup Services found: " + registrarCount);
        fireLUSCountChanged();
    }

    /** Only used for unit testing. */
    private final int testAgentID;
    /** Only used for unit testing. */
    private final ServiceIDListener testListener;

    /**
     * @param propsFile the agent properties file
     * @param userDefinedPropertiesFilename the user defined properties file
     * @param isSkipUI if true, do not show the build agent UI.
     */
    private BuildAgent(final String propsFile, final String userDefinedPropertiesFilename,
                      final boolean isSkipUI) {

        this (propsFile, userDefinedPropertiesFilename, isSkipUI, null, 0);
    }
    /**
     * This constructor only intended for unit tests.
     * @param propsFile the agent properties file
     * @param userDefinedPropertiesFilename the user defined properties file
     * @param isSkipUI if true, do not show the build agent UI.
     * @param testListener only used for unit testing.
     * @param testAgentID only used for unit testing.
     */
    BuildAgent(final String propsFile, final String userDefinedPropertiesFilename,
                      final boolean isSkipUI,
                      final ServiceIDListener testListener, final int testAgentID) {

        // for unit testing only
        this.testAgentID = testAgentID;
        // for unit testing only
        this.testListener = testListener;


        loadProperties(propsFile, userDefinedPropertiesFilename);

        serviceImpl = new BuildAgentServiceImpl(this);
        serviceImpl.setAgentPropertiesFilename(propsFile);

        origEntries = SearchablePropertyEntries.getPropertiesAsEntryArray(entryProperties);
        if (!isSkipUI && !GraphicsEnvironment.isHeadless()) {
            LOG.info("Loading Build Agent UI (use param -" + MAIN_ARG_SKIP_UI + " to bypass).");
            ui = new BuildAgentUI(this);
            //ui.updateAgentInfoUI(getService());
        } else {
            LOG.info("Bypassing Build Agent UI (headless).");
            ui = null;
        }

        exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                new BasicILFactory(), false, true);

        try {
            proxy = exporter.export(getService());
        } catch (ExportException e) {
            final String message = "Error exporting service";
            LOG.error(message, e);
            throw new RuntimeException(message, e);
        }

        // Use a comma separated list of Unicast Lookup Locaters (URL's) if defined in agent.properties. 
        // Useful if multicast isn't working.
        final String registryURLList = configProperties.getProperty(REGISTRY_URL);
        final LookupLocatorDiscovery lld;
        if (registryURLList == null) {
            lld = null;
        } else {
            lld = new LookupLocatorDiscovery(parseUnicastLocators(registryURLList));
        }

        try {
            if (serviceID == null) {
                joinManager = new JoinManager(getProxy(), getEntries(), this, lld, null);
            } else {
                LOG.warn("Didn't expect to have a serviceID: " + serviceID + " (agentID: " + testAgentID
                        + "). Are we storing and re-using the serviceID now?");
                joinManager = new JoinManager(getProxy(), getEntries(), serviceID, lld, null);
            }
        } catch (IOException e) {
            final String message = "Error starting discovery";
            LOG.error(message, e);
            throw new RuntimeException(message, e);
        }

        getJoinManager().getDiscoveryManager().addDiscoveryListener(this);
    }

    /**
     * Parses a comma separated list of Unicast Lookup Locaters (URL's).
     * Useful if multicast isn't working.
     * @param registryURLList a comma separated list of Unicast Lookup Locaters (URL's).
     * @return null if the given registryURLList is null, or a LookupLocator array populated with the given URL's.
     */
    private static LookupLocator[] parseUnicastLocators(String registryURLList) {
        final LookupLocator[] lookups;
        if (registryURLList == null) {
            lookups = null;
        } else {
            final String[] registryURLs = registryURLList.split(",");
            lookups = new LookupLocator[registryURLs.length];
            for (int i = 0; i < registryURLs.length; i++) {
                try {
                    lookups[i] = new LookupLocator(registryURLs[i]);
                } catch (MalformedURLException e) {
                    final String message = "Error creating unicast lookup locator: " + registryURLs[i]
                            + "; " + e.getMessage();
                    LOG.error(message, e);
                    throw new RuntimeException(message, e);
                }
                LOG.info("Using Unicast LookupLocator URL: " + registryURLs[i]);
            }
        }
        return lookups;
    }


    private final Preferences prefsBase = Preferences.userNodeForPackage(this.getClass());
    Preferences getPrefsRoot() { return prefsBase; }
    /**
     * Gets the EntryOverrides preferences node this this user, shared among all BuildAgents running
     * under this userID on the current machine.
     * @todo Should this node be more granular, like per Agent ServiceID? if so we must store/resuse serviceID
     */
    private final Preferences prefsEntryOverrides = prefsBase.node("entryOverrides");

    void setEntryOverrides(final PropertyEntry[] entryOverrides) {
        // clear stored override preferences settings
        clearOverridePrefs();

        // store override props using Preferences api
        for (int i = 0; i < entryOverrides.length; i++) {
            prefsEntryOverrides.put(entryOverrides[i].name, entryOverrides[i].value);
        }

        // publish using entries reloaded via getEntries, which adds entry overrides from prefs
        joinManager.setAttributes(getEntries());
    }

    void clearEntryOverrides() {

        // clear stored override preferences settings
        clearOverridePrefs();

        // publish using entries reloaded via getEntries, which adds entry overrides from prefs
        joinManager.setAttributes(getEntries());
    }

    private void clearOverridePrefs() {
        // clear stored override preferences settings
        try {
            prefsEntryOverrides.clear();
        } catch (BackingStoreException e) {
            LOG.error("Error clearing entry override prefs.", e);
            throw new RuntimeException(e);
        }
    }

    private Properties getEntryOverrideProps() {
        // check for entry overrides in preferences
        final String[] overrideKeys;
        try {
            overrideKeys = prefsEntryOverrides.keys();
        } catch (BackingStoreException e) {
            LOG.error("Error reading entry override prefs keys.", e);
            throw new RuntimeException(e);
        }
        final Properties overrideEntryProps = new Properties();
        if (overrideKeys.length > 0) {
            String key;
            for (int i = 0; i < overrideKeys.length; i++) {
                key = overrideKeys[i];
                overrideEntryProps.put(key, prefsEntryOverrides.get(key, "unknown value"));
            }
        }
        return overrideEntryProps;
    }

    PropertyEntry[] getEntryOverrides() {
        return SearchablePropertyEntries.getPropertiesAsEntryArray(getEntryOverrideProps());
    }


    /**
     * @param propsFile path to config properties file
     * @param userDefinedPropertiesFilename path to user properties file
     */
    private void loadProperties(final String propsFile, final String userDefinedPropertiesFilename) {
        configProperties = (Properties) PropertiesHelper.loadRequiredProperties(propsFile);
        entryProperties = new SearchablePropertyEntries(userDefinedPropertiesFilename).getProperties();

        final String policyFileValue = configProperties.getProperty(JINI_POLICY_FILE);
        LOG.info("policyFileValue: " + policyFileValue);

        // resource loading technique below dies in webstart
        //URL policyFile = ClassLoader.getSystemClassLoader().getResource(policyFileValue);
        final URL policyFile = BuildAgent.class.getClassLoader().getResource(policyFileValue);
        LOG.info("policyFile: " + policyFile);
        System.setProperty(JAVA_SECURITY_POLICY, policyFile.toExternalForm());
        ReggieUtil.setupRMISecurityManager();
    }

    private Exporter getExporter() {
        return exporter;
    }

    private JoinManager getJoinManager() {
        return joinManager;
    }

    PropertyEntry[] getEntries() {

        final PropertyEntry[] currentEntries;
        final Properties entryOverrideProps = getEntryOverrideProps();
        if (entryOverrideProps.size() > 0) {
            // add system entries first (preserves order)
            final Properties systemEntryProps = SearchablePropertyEntries.getSystemEntryProps();
            // use a props object to enforce precendence of overrides over original settings
            final Properties allEntries = new Properties();
            allEntries.putAll(systemEntryProps);
            // add props loaded from user-defined.properties file
            allEntries.putAll(entryProperties);
            // now add override entries that do NOT step on system entries
            String key;
            String value;
            final Enumeration enm = entryOverrideProps.keys();
            while (enm.hasMoreElements()) {
                key = (String) enm.nextElement();
                value = (String) entryOverrideProps.get(key);
                // don't allow override of system entry props
                if (!systemEntryProps.containsKey(key)) {
                    allEntries.put(key, value);
                } else {
                    LOG.warn("WARNING: Can't override system entry: "
                            + key + "=" + systemEntryProps.get(key)
                            + " with new value: " + value);
                }
            }

            // add to original entries
            currentEntries = SearchablePropertyEntries.getPropertiesAsEntryArray(allEntries);
        } else {
            // use original props file entries
            currentEntries = origEntries;
        }
        return currentEntries;
    }

    void addAgentStatusListener(final BuildAgent.AgentStatusListener listener) {
        serviceImpl.addAgentStatusListener(listener);
    }
    void removeAgentStatusListener(final BuildAgent.AgentStatusListener listener) {
        serviceImpl.removeAgentStatusListener(listener);
    }

    /** Only for unit testing. */
    private static boolean isTerminateFast;
    /** Only for unit testing. */
    static void setTerminateFast() { isTerminateFast = true; }
    /** Only for unit testing.
     * @param agent the unit test agent to terminate.
     */
    void terminateTestAgent(final BuildAgent agent) {
        LOG.info("Terminating test agent (agentID: " + agent.testAgentID + ")");
        agent.terminate();
        if (agent.testAgentID == 0) {
            throw new IllegalStateException("This does not appear to be a unit test Agent, agentID: "
                    + agent.testAgentID);
        }
    }

    private void terminate() {
        LOG.info("Terminating build agent.");
        int unexportAttempts = 0;
        while (!getExporter().unexport(false) && unexportAttempts < 10) {
            // wait a bit
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted during terminate.unexport", e);
            }
            unexportAttempts++;
        }
        if (!getExporter().unexport(false)) {
            LOG.warn("Unexport of Agent service failed. Forcing export.");
            getExporter().unexport(true);
        }
        getJoinManager().terminate();

        if (!isTerminateFast) {
            // allow some time for cleanup
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                LOG.warn("Sleep interrupted during terminate", e);
            }
        }

        if (ui != null) {
            ui.dispose();
            LOG.info("UI disposed");
        }
    }


    private Remote getProxy() {
        return proxy;
    }


    public BuildAgentService getService() {
        return serviceImpl;
    }

    /** 
     * Called when the JoinManager gets a valid ServiceID from a lookup
     * service.
     *
     *@param serviceID  the service ID assigned by the lookup service.
     */
    public void serviceIDNotify(final ServiceID serviceID) {
        // @todo technically, should serviceID be stored permanently and reused?....
        this.serviceID = serviceID;
        LOG.info("ServiceID assigned: " + this.serviceID
                + (testAgentID == 0 ? "" : " (agentID: " + testAgentID) + ")");
        if (ui != null) {
            ui.updateAgentInfoUI(getService());
        }


        // for unit testing only
        if (testListener != null) {
            testListener.serviceIDNotify(serviceID);
        }
    }
    ServiceID getServiceID() {
        return serviceID;
    }


    private void logRegistration(final ServiceRegistrar registrar) {
        String host = null;
        try {
            host = registrar.getLocator().getHost();
        } catch (RemoteException e) {
            LOG.warn("Failed to get registrar's hostname");
        }
        LOG.info("Registering BuildAgentService with Registrar: " + host);

        final String machineName = (String) entryProperties.get(SearchablePropertyEntries.HOSTNAME);
        LOG.debug("Registered machineName: " + machineName);

        LOG.debug("Entries: ");
        for (Iterator iter = entryProperties.keySet().iterator(); iter.hasNext();) {
            final String key = (String) iter.next();
            LOG.debug("  " + key + " = " + entryProperties.get(key));
        }
    }

    private boolean isNotFirstDiscovery;

    public void discovered(final DiscoveryEvent evt) {
        final ServiceRegistrar[] registrarsArray = evt.getRegistrars();
        ServiceRegistrar registrar;
        for (int n = 0; n < registrarsArray.length; n++) {
            registrar = registrarsArray[n];
            logRegistration(registrar);
            LOG.debug("Registered with registrar: " + registrar.getServiceID());
        }
        if (!isNotFirstDiscovery) {
            LOG.info("BuildAgentService open for business...");
            isNotFirstDiscovery = true;
        }

        setRegCount(getJoinManager().getDiscoveryManager().getRegistrars().length);
    }

    public void discarded(final DiscoveryEvent evt) {
        final ServiceRegistrar[] registrarsArray = evt.getRegistrars();
        ServiceRegistrar registrar;
        for (int n = 0; n < registrarsArray.length; n++) {
            registrar = registrarsArray[n];
            LOG.debug("Discarded registrar: " + registrar.getServiceID());
        }

        setRegCount(getJoinManager().getDiscoveryManager().getRegistrars().length);
    }


    private static final Object KEEP_ALIVE = new Object();
    private static Thread mainThread;

    private static void setMainThread(final Thread newMainThread) {
        mainThread = newMainThread;
    }
    static Thread getMainThread() {
        return mainThread;
    }

    /** Intended only for unit tests to avoid killing the unit test VM. */
    private static boolean isSkipMainSystemExit;
    static void setSkipMainSystemExit() { isSkipMainSystemExit = true; }

    public static void main(final String[] args) {

        setMainThread(Thread.currentThread());

        LOG.info("Starting agent...args: " + Arrays.asList(args).toString());

        CCDistVersion.printCCDistVersion();
        
        if (shouldPrintUsage(args)) {
            printUsage();
        }

        final BuildAgent buildAgent = new BuildAgent(
                MainArgs.parseArgument(args, MAIN_ARG_AGENT_PROPS,
                        BuildAgentServiceImpl.DEFAULT_AGENT_PROPERTIES_FILE,
                        BuildAgentServiceImpl.DEFAULT_AGENT_PROPERTIES_FILE),

                MainArgs.parseArgument(args, MAIN_ARG_USER_PROPS,
                        BuildAgentServiceImpl.DEFAULT_USER_DEFINED_PROPERTIES_FILE,
                        BuildAgentServiceImpl.DEFAULT_USER_DEFINED_PROPERTIES_FILE),

                MainArgs.argumentPresent(args, MAIN_ARG_SKIP_UI)
        );

        // stay around forever
        synchronized (KEEP_ALIVE) {
           try {
               KEEP_ALIVE.wait();
           } catch (InterruptedException e) {
               LOG.error("Keep Alive wait interrupted", e);
            } finally {
                buildAgent.terminate();
           }
        }

        final String mainThreadName = Thread.currentThread().getName();
        LOG.info("Agent main thread (" + mainThreadName + ") exiting.");
        // don't call sys exit during unit tests
        if (!isSkipMainSystemExit) {
            // on some JVM's (webstart - restart) the BuildAgent.kill() call doesn't return,
            // so sys exit is also done here.            
            LOG.info("Agent main thread (" + mainThreadName + ") calling System.exit().");
            System.exit(0);
        } else {
            LOG.debug("Agent main thread (" + mainThreadName
                    + ") skipping System.exit(), only valid in unit tests.");
        }
    }


    private static boolean shouldPrintUsage(String[] args) {
        return MainArgs.findIndex(args, "?") != MainArgs.NOT_FOUND
                || MainArgs.findIndex(args, "help") != MainArgs.NOT_FOUND;
    }

    private static void printUsage() {
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("");
        System.out.println("Starts a distributed Build Agent");
        System.out.println("");
        System.out.println(BuildAgent.class.getName() + " [options]");
        System.out.println("");
        System.out.println("Build Agent options are:");
        System.out.println("");
        System.out.println("  -" + MAIN_ARG_AGENT_PROPS + " file     agent properties file; default "
                + BuildAgentServiceImpl.DEFAULT_AGENT_PROPERTIES_FILE);
        System.out.println("  -" + MAIN_ARG_USER_PROPS + " file      user defined properties file; default "
                + BuildAgentServiceImpl.DEFAULT_USER_DEFINED_PROPERTIES_FILE);
        System.out.println("  -" + MAIN_ARG_SKIP_UI + "              run in headless mode");
        System.out.println("  -? or -help          print this usage message");
        System.out.println("");
    }

    public static void kill() {
        final Thread main = getMainThread();
        if (main != null) {
            final String mainThreadName = main.getName();
            main.interrupt();
            LOG.info("Waiting for main thread (" + mainThreadName + ") to finish.");
            try {
                main.join(30 * 1000);
                //main.join();
            } catch (InterruptedException e) {
                LOG.error("Error while waiting for Agent thread (" + mainThreadName + ") to die.", e);
            }
            if (main.isAlive()) {
                main.interrupt(); // how can this happen?
                LOG.error("Main thread (" + mainThreadName + ") should have died.");
            }
            setMainThread(null);
        } else {
            LOG.info("WARNING: Kill called, MainThread is null. Doing nothing. Acceptable only in Unit Tests.");
        }
    }

    static interface AgentStatusListener {
        public void statusChanged(BuildAgentService buildAgentServiceImpl);
    }

}
