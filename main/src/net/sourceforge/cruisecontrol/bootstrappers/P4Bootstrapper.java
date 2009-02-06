/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * Bootstrapper for Perforce. Accepts one view that we sync.
 *
 * @author <a href="mailto:mroberts@thoughtworks.com">Mike Roberts</a>
 * @author <a href="mailto:cstevenson@thoughtworks.com">Chris Stevenson</a>
 * @author J D Glanville
 */
public class P4Bootstrapper implements Bootstrapper {
    private static final Logger LOG = Logger.getLogger(P4Bootstrapper.class);
    private String view;
    private String port;
    private String client;
    private String user;
    private String passwd;

    public void setPort(String port) {
        this.port = port;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setView(String view) {
        this.view = view;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(view, "view", this.getClass());
        ValidationHelper.assertNotEmpty(view, "view", this.getClass());
        ValidationHelper.assertNotEmpty(port, "P4Port", this.getClass());
        ValidationHelper.assertNotEmpty(client, "P4Client", this.getClass());
        ValidationHelper.assertNotEmpty(user, "P4User", this.getClass());
        ValidationHelper.assertNotEmpty(passwd, "P4Passwd", this.getClass());
    }

    public void bootstrap() throws CruiseControlException {
        Commandline commandline = createCommandline();
        LOG.debug("Executing commandline [" + commandline + "]");
        executeCommandLine(commandline);
    }

    public Commandline createCommandline() throws CruiseControlException {
        validate();
        Commandline cmd = new Commandline();
        cmd.setExecutable("p4");
        cmd.createArgument("-s");
        if (port != null) {
            cmd.createArguments("-p", port);
        }
        if (client != null) {
            cmd.createArguments("-c", client);
        }
        if (user != null) {
            cmd.createArguments("-u", user);
        }
        if (passwd != null) {
            cmd.createArguments("-P", passwd);
        }
        cmd.createArguments("sync", view);

        return cmd;
    }

    private void executeCommandLine(Commandline commandline) throws CruiseControlException {
        commandline.executeAndWait(LOG);
    }
}
