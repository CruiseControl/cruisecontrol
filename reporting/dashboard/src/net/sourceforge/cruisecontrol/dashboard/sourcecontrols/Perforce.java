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
package net.sourceforge.cruisecontrol.dashboard.sourcecontrols;

import java.io.File;
import com.perforce.api.Client;
import com.perforce.api.CommitException;
import com.perforce.api.Env;
import com.perforce.api.Utils;
import net.sourceforge.cruisecontrol.dashboard.utils.Pipe;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.CruiseRuntime;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

public class Perforce implements VCS {
    private static final String PERFORCE = "p4";

    private String port;

    private String depotPath;

    private CruiseRuntime runtime;

    private String clientName;

    public Perforce(String projectName, String port, String depotPath, CruiseRuntime runtime) {
        this.port = port;
        this.depotPath = depotPath;
        this.runtime = runtime;
        this.clientName = projectName;
    }

    private Commandline getCheckConnectionCommandLine() {
        Commandline command = new Commandline(null, runtime);
        command.setExecutable(PERFORCE);
        command.createArgument("-p");
        command.createArgument(port);
        command.createArgument("info");
        return command;
    }

    public boolean checkBuildFile() {
        return false;
    }

    public ConnectionResult checkConnection() {
        String error;
        try {
            Pipe pipe = new Pipe(getCheckConnectionCommandLine());
            pipe.waitFor();
            error = pipe.error();
        } catch (Exception e) {
            error = ExceptionUtils.getRootCauseMessage(e);
        }
        return new ConnectionResult(StringUtils.isEmpty(error), error);
    }

    public void checkout(final String path) {
        Thread checkout = new Thread() {
            public void run() {
                File checkoutPath = new File(path);
                checkoutPath.mkdir();
                createClient(path);
                new Pipe(getSyncCommandline());
            }
        };
        checkout.start();
    }

    private Commandline getSyncCommandline() {
        Commandline command = new Commandline(null, new CruiseRuntime());
        command.setExecutable(PERFORCE);
        command.createArgument("-p");
        command.createArgument(port);
        command.createArgument("-c");
        command.createArgument(clientName);
        command.createArgument("sync");
        return command;
    }

    private void createClient(final String path) {
        Env env = new Env();
        env.setPort(port);
        env.setExecutable(PERFORCE);

        Client client = new Client(env, clientName);
        client.setRoot(path);
        client.addView(depotPath, "//" + clientName + "/...");

        try {
            client.commit();
        } catch (CommitException e1) {
            // We don't fail adding project if checkout failed
        }
        Utils.cleanUp();
    }

    public String getBootStrapper() {
        return "p4bootstrapper";
    }

    public String getRepository() {
        return PERFORCE;
    }

}
