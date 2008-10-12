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

import java.io.File;
import java.io.FileInputStream;

import net.sourceforge.cruisecontrol.util.MainArgs;

import org.apache.log4j.Logger;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.xml.XmlConfiguration;

/**
 * Provides functionality to run an embedded Jetty Server from within the CruiseControl instance. The embedded Jetty
 * server serves the CruiseControl web application on contexts / and /cruisecontrol.
 */
public class EmbeddedJettyServer {

    /**
     * the logger instance for this class.
     */
    public static final Logger LOG = Logger.getLogger(EmbeddedJettyServer.class);

    /**
     * the embedded Jetty server.
     */
    private Server jettyServer;

    /**
     * the current state of the embedded Jetty server.
     */
    private boolean isRunning;

    private File jettyXml;

    private int webPort;

    /**
     * Creates a new embeded Jetty server using the supplied jetty.xml.
     */
    public EmbeddedJettyServer(File jettyXml, int webPort) {
        this.jettyXml = jettyXml;
        this.webPort = webPort;
    }


    /**
     * Starts the embedded Jetty server.
     */
    public void start() {
        if (isRunning) {
            LOG.info("EmbeddedJettyServer.start() called, but server already running.");
            return;
        }

        jettyServer = new Server();
        
        try {
            XmlConfiguration configuration = new XmlConfiguration(new FileInputStream(jettyXml));
            configuration.configure(jettyServer);
            if (webPort != MainArgs.NOT_FOUND && webPort != 8080) {
                Connector connector = new SelectChannelConnector();
                connector.setPort(webPort);
                connector.setMaxIdleTime(30000);
                jettyServer.addConnector(connector);
            }
            
            jettyServer.start();
        } catch (Exception e) {
            String msg = "Unable to start embedded Jetty server: " + e.getMessage();
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
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
                } catch (Exception e) {
                    String msg = "Exception occurred while stopping Embedded Jetty server";
                    LOG.error(msg);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
