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

import net.sourceforge.cruisecontrol.dashboard.utils.Pipe;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.CruiseRuntime;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

public class Svn implements VCS {
    private static final String SVN = "svn";

    private final String url;

    private final CruiseRuntime runtime;

    private String error;

    public Svn(String url, CruiseRuntime runtime) {
        this.url = url;
        this.runtime = runtime;
    }

    private Commandline getCheckoutCommandLine(String path) {
        Commandline command = new Commandline(null, runtime);
        command.setExecutable(SVN);
        command.createArgument("co");
        command.createArgument("--non-interactive");
        command.createArgument(url);
        command.createArgument(path);
        return command;
    }

    private Commandline testConnectionCommandLine() {
        Commandline command = new Commandline(null, runtime);
        command.setExecutable(SVN);
        command.createArgument("info");
        command.createArgument("--non-interactive");
        command.createArgument(url);
        return command;
    }

    public ConnectionResultContext checkConnection() {
        try {
            Pipe pipe = new Pipe(testConnectionCommandLine());
            pipe.waitFor();
            error = pipe.error();
        } catch (Exception e) {
            error = ExceptionUtils.getRootCauseMessage(e);
        }
        return new ConnectionResultContext(error);
    }

    public void checkout(final String path) {
        Thread checkout = new Thread() {
            public void run() {
                new Pipe(getCheckoutCommandLine(path));
            }
        };
        checkout.start();
    }

    private CheckoutAdapter list() {
        Pipe pipe = new Pipe(getListCommandLine());
        return new CheckoutAdapter(pipe);
    }

    private Commandline getListCommandLine() {
        Commandline command = new Commandline(null, runtime);
        command.setExecutable(SVN);
        command.createArgument("list");
        command.createArgument("--non-interactive");
        command.createArgument(url);
        return command;
    }

    public boolean checkBuildFile() {
        return StringUtils.contains(list().checkoutMessage(), BUILD_FILE_NAME);
    }

    public String getBootStrapper() {
        return "svnbootstrapper";
    }

    public String getRepository() {
        return SVN;
    }
}
