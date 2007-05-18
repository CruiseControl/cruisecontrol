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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import net.sourceforge.cruisecontrol.util.CruiseRuntime;
import org.apache.commons.lang.StringUtils;

public class MockRuntime extends CruiseRuntime {
    private MockProcess mockProcess;

    private boolean isDynamic;

    public MockRuntime() {
        isDynamic = true;
    }

    public MockRuntime(String output, boolean error) {
        isDynamic = false;
        mockProcess = getProcess(error, output);
    }

    private MockProcess getProcess(boolean error, String output) {
        MockProcess process = new MockProcess();
        process.setOutputStream(new ByteArrayOutputStream());
        if (error) {
            process.setErrorStream(new ByteArrayInputStream(output.getBytes()));
            process.setInputStream(new ByteArrayInputStream("".getBytes()));
        } else {
            process.setErrorStream(new ByteArrayInputStream("".getBytes()));
            process.setInputStream(new ByteArrayInputStream(output.getBytes()));
        }
        return process;
    }

    public Process exec(String[] commandline) throws IOException {
        return isDynamic ? getProcessFromCommandLine(commandline) : mockProcess;
    }

    public Process exec(String[] commandline, String[] o, File workingDir) throws IOException {
        return isDynamic ? getProcessFromCommandLine(commandline) : mockProcess;
    }

    private Process getProcessFromCommandLine(String[] commandLine) {
        String command = StringUtils.join(commandLine, " ");
        boolean isCvsModuleNameProvided =
                StringUtils.contains(command, "cvs") && StringUtils.contains(command, "module");
        boolean isError = !StringUtils.contains(command, "valid") || isCvsModuleNameProvided;
        boolean containsBuild = StringUtils.contains(command, "build");
        return getProcess(isError, containsBuild ? "build.xml" : "Error occurred");
    }
}
