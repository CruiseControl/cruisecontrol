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
 *
 ********************************************************************************/
package net.sourceforge.cruisecontrol.builders;

import org.apache.log4j.Logger;
import org.jdom2.Element;

import net.sourceforge.cruisecontrol.Builder.EnvConf;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

/**
 * Class for the configuration script to execute. It has the same arguments as
 * {@link ExecBuilder}, plus the ID of script, the ID of script from which it is supposed to
 * read data through STDIN (optional), and the ID of script which the current should wait
 * for.
 *
 * The class is the implementation of {@link Runnable} interface, as several scripts piped
 * one with another are started simultaneously.
 */
@Description("Standard exec builder extended with attributes required for a builder to be piped "
        + "into the pipedexec builder. ")
public final class PipedExecScript extends PipedScriptBase {

    /**
     * Override of {@link ScriptRunner} piping STDIN and STDOUT from/to other scripts
     * @author dtihelka
     */
    private final class PipedScriptRunner extends ScriptRunner {
        /** Disable script consumption of STDOUT - although errors cannot be found in it now, it is
         *  expected that errors are printed to STDERR when a sequence of piped commands is started.
         *  Also, STDOUT of the script may contain binary data - it is generally bad idea pass through
         *  text-expected classes. */
        @Override
        protected boolean letConsumeOut() {
            return false;
        }
        /** Returns the consumer printing STDOUT of the script on {@link org.apache.log4j.Level#DEBUG}
         *  level. */
        @Override
        protected StreamConsumer getDirectOutLogger() {
            /* Log only non-binary output */
            if (Boolean.FALSE.equals(getBinaryOutput())) {
                return StreamLogger.getDebugLogger(ScriptRunner.LOG);
            }
            /* Disable logging otherwise */
            return new StreamConsumer() {
                public void consumeLine(final String arg0) { /* Ignore data */ }
            };
        }
        /** Assign STDOUT of the process directly to the StdoutBuffer (as byte stream) in addition to the
         *  (text) consumer given. */
        @Override
        protected StreamPumper getOutPumper(final Process p, final StreamConsumer consumer) {
            return new StreamPumper(p.getInputStream(), getBinaryOutput().booleanValue(), consumer,
                    getOutputBuffer());
        } // getPumperOut
    }

    /**
     * The override of {@link ExecBuilder} class customising {@link ExecBuilder#createScriptRunner()}
     * and {@link Builder#mergeEnv(OSEnvironment)} methods; see their description for further
     * details.
     */
    private final ExecBuilder builder = new ExecBuilder() {
        /** Returns script runner piped STDIN/STDOUT, see {@link PipedScriptRunner} */
        @Override
        protected ScriptRunner createScriptRunner() {
            return new PipedScriptRunner();
        }
        /** Calls the env merge from the glue (set by {@link PipedExecScript#setEnvGlue(PipedScript.EnvGlue)})
         *  followed by the call of the parent's implementation. */
        @Override
        public void mergeEnv(OSEnvironment env) {
            if (envGlue != null) {
                envGlue.mergeEnv(env);
            }
            super.mergeEnv(env);
        };
        
        /** Serialization UID */
        private static final long serialVersionUID = 2452456256173465623L;
    };

    @Override
    public void validate() throws CruiseControlException {
        super.validate();
        builder.validate();
        /* Only single pipe is allowed! */
        ValidationHelper.assertTrue(getPipeFrom().length <= 1, 
                "ID " + getID() + ": only single piped script is allowed", getClass());
    }

    @Override
    protected Element build() throws CruiseControlException {
        final String[] pipe = getPipeFrom();
        return builder.build(getBuildProperties(), getProgress(), 
                (pipe != null && pipe.length == 1) ? getInputProvider(pipe[0]) : null);
    }

    @Override
    protected Logger log() {
        return ExecBuilder.LOG;
    }

    /** Just caller of {@link ExecBuilder#setTimeout(long)} */
    public void setTimeout(long time) {
        builder.setTimeout(time);
    }
    /** Just caller of {@link ExecBuilder#getTimeout()} */
    public long getTimeout() {
        return builder.getTimeout();
    }

    /** Just caller of {@link ExecBuilder#setWorkingDir(String)} */
    public void setWorkingDir(String workingDir) {
        builder.setWorkingDir(workingDir);
    }
    /** Just caller of {@link ExecBuilder#getWorkingDir()} */
    public String getWorkingDir() {
        return builder.getWorkingDir();
    }

    /** Raw caller of {@link ExecBuilder#setCommand(String)} for the script configuration purposes */
    @SuppressWarnings("javadoc")
    public void setCommand(String cmd) {
        this.builder.setCommand(cmd);
    }
    /** Raw caller of {@link ExecBuilder#setArgs(String)} for the script configuration purposes */
    @SuppressWarnings("javadoc")
    public void setArgs(String args) {
        this.builder.setArgs(args);
    }
    /** Raw caller of {@link ExecBuilder#setErrorStr(String)} for the script configuration purposes. */
    @SuppressWarnings("javadoc")
    public void setErrorStr(String errStr) {
        this.builder.setErrorStr(errStr);
    } // setErrorStr

    /** Raw caller of {@link ExecBuilder#createEnv()} for the script configuration purposes. */
    @SuppressWarnings("javadoc")
    public EnvConf createEnv() {
        return builder.createEnv();
    } // createEnv

    /** Stores the glue object */
    @SkipDoc
    @Override
    public void setEnvGlue(final EnvGlue glue) {
        envGlue = glue;
    }
    
    
    /** Prints string representation of the object */
    @Override
    public String toString() {
        return getClass().getName() + "[ID " + getID() + ", piped from "
                + (getPipeFrom() != null ? getPipeFrom() : "-") + ", wait for "
                + (getWaitFor() != null ? getWaitFor() : "-") + " Command: "
                + builder.getCommand() + ' ' + builder.getArgs() + "]";
    }


    /** {@link PipedScript.EnvGlue} object set through {@link #setEnvGlue(PipedScript.EnvGlue)} */
    private PipedScript.EnvGlue envGlue = null;
}
