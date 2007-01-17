/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.cruisecontrol.LogFile;


/**
 * Streams raw log files.
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class LogFileServlet extends FileServlet {

    /** Creates a new instance of LogFileServlet. */
    public LogFileServlet() {
    }

    protected File getRootDir(ServletConfig servletconfig) throws ServletException {
        File rootDirectory = getLogDir(servletconfig);
        if (rootDirectory == null) {
            String message = "LogFileServlet not configured correctly in web.xml.\n"
                 + "logDir must point to existing directory.\n"
                 + "logDir is currently set to <"
                 + getLogDirParameter(servletconfig) + ">";
            throw new ServletException(message);
        }

        return rootDirectory;
    }

    protected String getMimeType(String filename) {
        return "application/xml";
    }

    LogFile getLogFile(final String subFilePath) {
        int logIndex = subFilePath.lastIndexOf('/');
        String project = subFilePath.substring(0, logIndex);
        String logName = subFilePath.substring(logIndex + 1);
        return new LogFile(new File(getRootDir(), project), logName);
    }
    protected WebFile getSubWebFile(final String subFilePath) {
        return new LogWebFile(getLogFile(subFilePath));
    }

    private static class LogWebFile extends WebFile {
        private LogFile logfile;

        public LogWebFile(LogFile file) {
            super(file.getFile());
            logfile = file;
        }

        protected InputStream getInputStream() throws IOException {
            return logfile.getInputStream();
        }
    }
}
