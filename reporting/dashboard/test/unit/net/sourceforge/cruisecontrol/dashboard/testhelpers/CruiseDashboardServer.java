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
package net.sourceforge.cruisecontrol.dashboard.testhelpers;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.deployer.WebAppDeployer;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.thread.BoundedThreadPool;
import org.springframework.util.Assert;

public class CruiseDashboardServer {
    private static final String DEFAULT_WEBAPP_PATH = "dist/";

    private String warPath;

    private Server jettyServer;
    private static final int PORT = 9090;

    public CruiseDashboardServer() {
        this(DEFAULT_WEBAPP_PATH);
    }

    public CruiseDashboardServer(String warPath) {
        super();
        this.warPath = warPath;
        this.jettyServer = new Server();
        init();
    }

    public void start() throws Exception {
        jettyServer.start();
    }

    private void init() {
        Assert.notNull(warPath);
        BoundedThreadPool threadPool = new BoundedThreadPool();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(PORT);

        ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
        WebAppDeployer deployer = new WebAppDeployer();
        deployer.setContexts(contextHandlerCollection);
        deployer.setWebAppDir(this.warPath);
        deployer.setExtract(true);

        jettyServer.setThreadPool(threadPool);
        jettyServer.addConnector(connector);
        jettyServer.setHandler(contextHandlerCollection);
        jettyServer.addLifeCycle(deployer);
    }

    public void stop() throws Exception {
        jettyServer.stop();
    }

    public static void main(String[] strings) throws Exception {
        CruiseDashboardServer server = new CruiseDashboardServer(DEFAULT_WEBAPP_PATH);
        server.start();
    }
}
