/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.web;

import java.io.IOException;
import java.io.File;

import org.apache.log4j.Logger;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;

/**
 * Provides functionality to run an embedded Jetty Server from within the CruiseControl instance. The embedded Jetty
 * server serves the CruiseControl web application on contexts / and /cruisecontrol.
 */
public class EmbeddedJettyServer {

    /** the logger instance for this class. */
    public static final Logger LOG = Logger.getLogger(EmbeddedJettyServer.class);

    /** the port that the embedded Jetty server will listen on. */
    private int webPort;

    /** the path to the CruiseControl webapp served by the embedded server. */
    private String webappPath;

    /** the path to the CruiseControl new web dashboard served by the embedded server. */
    private String newWebappPath;
    private String configFileName;

    /** the embedded Jetty server. */
    private Server jettyServer;

    /** the current state of the embedded Jetty server. */
    private boolean isRunning;

    private int jmxPort;

    private int rmiPort;

    /**
     * Creates a new embeded Jetty server with the given listen port and the given webapp path.
     *
     * @param webPort
     *            the port the embedded Jetty server will listen on.
     * @param webappPath
     *            the path to the CruiseControl web application served by the embedded server.
     *
     * @deprecated Use constructor that also sets up new dashboard
     */
    public EmbeddedJettyServer(int webPort, String webappPath) {
        this.webPort = webPort;
        this.webappPath = webappPath;
    }

    /**
     * Creates a new embeded Jetty server with the given listen port and the given webapp path.
     *
     * @param webPort
     *            the port the embedded Jetty server will listen on.
     * @param webappPath
 *            the path to the CruiseControl web application served by the embedded server.
     * @param newWebappPath
     * @param configFileName
     *         the name of the config file
     */
    public EmbeddedJettyServer(int webPort, String webappPath, String newWebappPath,
            String configFileName, int jmxPort, int rmiPort) {
        this.webPort = webPort;
        this.webappPath = webappPath;
        this.newWebappPath = newWebappPath;
        this.configFileName = configFileName;
        this.jmxPort = jmxPort;
        this.rmiPort = rmiPort;
    }

    private void setUpSystemPropertiesForDashboard() {
        if (configFileName != null) {
            File configFile = new File(configFileName);
            if (!configFile.exists()) {
                throw new RuntimeException("Cannot find config file at " + configFile.getAbsolutePath());
            }
            System.setProperty("cc.config.file", configFile.getAbsolutePath());
        }
        System.setProperty("cc.rmiport", String.valueOf(rmiPort));
        System.setProperty("cc.jmxport", String.valueOf(jmxPort));
    }
    
    /**
     * Starts the embedded Jetty server.
     */
    public void start() {
        if (isRunning) {
            LOG.info("EmbeddedJettyServer.start() called, but server already running.");
            return;
        }
        setUpSystemPropertiesForDashboard();
        jettyServer = new Server();
        SocketListener listener = new SocketListener();
        listener.setPort(webPort);
        jettyServer.addListener(listener);
        try {
            jettyServer.addWebApplication("/cruisecontrol", webappPath);
            jettyServer.addWebApplication("/", webappPath);
            if (newWebappPath != null) {
                jettyServer.addWebApplication("/dashboard", newWebappPath);
                
                String ccConfigWebpath = newWebappPath + "/../cc-config";
                if (new File(ccConfigWebpath).exists()) {
                    jettyServer.addWebApplication("/cc-config", ccConfigWebpath);
                }
            }
        } catch (IOException e) {
            String msg = "Exception adding cruisecontrol webapp to embedded Jetty server: " + e.getMessage();
            LOG.error(msg, e);
            throw new RuntimeException(msg);
        }

        try {
            jettyServer.start();
        } catch (Exception e) {
            String msg = "Unable to start embedded Jetty server: " + e.getMessage();
            LOG.error(msg, e);
            throw new RuntimeException();
        }
        isRunning = true;
    }

    /**
     * Stops the embedded Jetty Server.
     */
    public void stop() {
        if (isRunning) {
            if (jettyServer != null) {
                try {
                    jettyServer.stop();
                } catch (InterruptedException e) {
                    String msg = "Exception occurred while stopping Embedded Jetty server";
                    LOG.error(msg);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
