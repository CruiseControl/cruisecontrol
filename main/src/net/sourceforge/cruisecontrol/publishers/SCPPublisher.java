/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Commandline.Argument;

import org.jdom.Element;

import java.io.IOException;
import java.io.File;

import org.apache.log4j.Logger;

/**
 * Used to scp a file to a remote location
 *
 * @author <a href="orenmnero@sourceforge.net">Oren Miller</a>
 */

public class SCPPublisher implements Publisher {

    private static final Logger LOG = Logger.getLogger(SCPPublisher.class);

    private String _sourceuser;
    private String _sourcehost;
    private String _sourcedir = ".";
    private String _targetuser;
    private String _targethost;
    private String _targetdir = ".";
    private String _ssh = "ssh";
    private String _options;
    private String _file;
    private String _targetseparator = File.separator;

    public void setSourceUser(String sourceuser) {
        _sourceuser = sourceuser;
    }

    public void setSourceHost(String sourcehost) {
        _sourcehost = sourcehost;
    }

    public void setSourceDir(String sourcedir) {
        _sourcedir = sourcedir;
    }

    public void setTargetUser(String targetuser) {
        _targetuser = targetuser;
    }

    public void setTargetHost(String targethost) {
        _targethost = targethost;
    }

    public void setTargetDir(String targetdir) {
        _targetdir = targetdir;
    }

    public void setSSH(String ssh) {
        _ssh = ssh;
    }

    public void setOptions(String options) {
        _options = options;
    }

    public void setFile(String file) {
        _file = file;
    }

    public void setTargetSeparator(String targetseparator) {
        _targetseparator = targetseparator;
    }

    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        if (_sourceuser == null) {
            if (_sourcehost != null) {
                throw new CruiseControlException("'sourceuser' not specified in configuration file");
            }
        }

        if (_sourcehost == null) {
            if (_sourceuser != null) {
                throw new CruiseControlException("'sourcehost' not specified in configuration file");
            }
        }

        if (_targetuser == null) {
            if (_targethost != null) {
                throw new CruiseControlException("'targetuser' not specified in configuration file");
            }
        }

        if (_targethost == null) {
            if (_targetuser != null) {
                throw new CruiseControlException("'targethost' not specified in configuration file");
            }
        }
    }

    public void publish(Element cruisecontrolLog)
        throws CruiseControlException {

        if (_file == null) {
            XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
            _file = helper.getLogFileName().substring(1);
        }

        Commandline command = createCommandline(_file);
        LOG.info("executing command: " + command);
        try {
            Runtime.getRuntime().exec(command.getCommandline());
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }
    }

    public Commandline createCommandline(String file) {
        String sourcefile = File.separator + file;
        String targetfile = _targetseparator + file;

        Commandline command = new Commandline();
        command.setExecutable("scp");
        command.createArgument().setLine(_options);
        command.createArgument().setValue("-S");
        command.createArgument().setValue(_ssh);

        createFileArgument(
            command.createArgument(),
            _sourceuser,
            _sourcehost,
            _sourcedir,
            sourcefile);

        createFileArgument(
            command.createArgument(),
            _targetuser,
            _targethost,
            _targetdir,
            targetfile);

        return command;

    }

    public void createFileArgument(
        Argument arg,
        String user,
        String host,
        String dir,
        String file) {

        String argValue = new String();

        if (user != null && host != null) {
            argValue = user + "@" + host + ":";
        }

        argValue += dir;
        argValue += file;
        arg.setValue(argValue);
    }

}
