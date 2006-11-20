/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.bootstrappers;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.builders.ExecBuilder;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

public class ExecBootstrapper implements Bootstrapper {

    private static final Logger LOG = Logger.getLogger(ExecBootstrapper.class);
    private final ExecBuilder delegate = new ExecBuilder();

    /**
     * Called after the configuration is read to make sure that all the mandatory parameters
     * were specified..
     *
     * @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        // assert with this classname in error message, otherwise delegate classname is used
        ValidationHelper.assertIsSet(delegate.getCommand(), "command", this.getClass());

        delegate.validate();
    }

    public void bootstrap() throws CruiseControlException {

        final Map properties = new HashMap();
        // Run ExecuteBuilder
        final Element result = delegate.build(properties);
        if (result == null) {
            LOG.error("ExecBootstrapper failed.\n\n");
            throw new CruiseControlException("ExecBootstrapper failed with null result");
        }

        final Attribute error = result.getAttribute("error");
        if (error == null) {
            LOG.info("ExecBootstrapper successful.");
        } else {
            LOG.error("ExecBootstrapper failed.\n\n"
                    + error.getValue()
                    + "\n");
            throw new CruiseControlException("ExecBootstrapper failed: " + error.getValue());
        }
    }

    /**
     * Sets build timeout in seconds.
     *
     * @param timeout long build timeout
     */
    public void setTimeout(long timeout) {
        delegate.setTimeout(timeout);
    }

    /**
     * Sets the command to execute
     *
     * @param cmd the command to execute
     */
    public void setCommand(String cmd) {
        delegate.setCommand(cmd);
    }

    /**
     * Sets the arguments for the command to execute
     *
     * @param args arguments for the command to execute
     */
    public void setArgs(String args) {
        delegate.setArgs(args);
    }

    /**
     * Sets the error string to search for in the command output
     *
     * @param errStr the error string to search for in the command output
     */
    public void setErrorStr(String errStr) {
        delegate.setErrorStr(errStr);
    }

    /**
     * Sets the working directory where the command is to be executed
     *
     * @param dir the directory where the command is to be executed
     */
    public void setWorkingDir(String dir) {
        delegate.setWorkingDir(dir);
    }
}
