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

package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.CompositeConsumer;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.StreamLogger;

import org.apache.log4j.Logger;

/**
 * Takes a script and runs it.  Monitors how long the script takes to run, and
 * returns whether the script completed or not.
 */
public class ScriptRunner  {
    static final Logger LOG = Logger.getLogger(ScriptRunner.class);
    public static final long NO_TIMEOUT = -1;

    private static class AsyncKiller implements Runnable {
        private final Process p;
        private final long timeout;
        private boolean killed;

        AsyncKiller(final Process p, final long timeout) {
            this.p = p;
            this.timeout = timeout;
        }

        public void run() {
            try {
                Thread.sleep(timeout * 1000L);
                synchronized (this) {
                    p.destroy();
                    killed = true;
                }
            } catch (InterruptedException expected) {
                // ignore, this is expected if the script was killed
            }
        }

        public synchronized boolean processKilled() {
            return killed;
        }
    }

    
    /**
     * build and return the results via xml. debug status can be determined from
     * log4j category once we get all the logging in place.
     *
     * @param workingDir  The directory to run the script from.
     * @param script  The details on the script to be run.
     * @param timeout Time in seconds after which the script should be killed.
     * @return true if the script was killed due to timeout expiring
     * @throws CruiseControlException if it breaks
     * @see #runScript(File, Script, long, InputStream, BuildOutputLogger)
     */
    public boolean runScript(final File workingDir, final Script script, final long timeout)
            throws CruiseControlException {
        return runScript(workingDir, script, timeout, null, null);
    }

    /**
     * build and return the results via xml. debug status can be determined from
     * log4j category once we get all the logging in place.
     *
     * @param workingDir  The directory to run the script from.
     * @param script  The details on the script to be run.
     * @param timeout Time in seconds after which the script should be killed.
     * @param buildOutputConsumer  Optional script output consumer.
     * @return true if the script was killed due to timeout expiring
     * @throws CruiseControlException if it breaks
     * @see #runScript(File, Script, long, InputStream, BuildOutputLogger)
     */
    public boolean runScript(final File workingDir, final Script script, final long timeout,
                             final BuildOutputLogger buildOutputConsumer)
            throws CruiseControlException {
        return runScript(workingDir, script, timeout, null, buildOutputConsumer);
    }

    
    /**
     * Run the given script, let it run on maximum the given timeout, (optionally) feed its STDIN by data from the given
     * stream, and (optionally) feed its STDOUT/STDERR to the given consumer. 
     * 
     * NOTE: everything printed to STDOUT and STDERR by the script is automatically passed to the CruiseControl log file
     * through consumers returned by {@link #getDirectOutLogger()} and {@link #getDirectErrLogger()}.
     * NOTE: everything printed to STDOUT and STDERR is passed to {@link Script} (if it is the instance of 
     * {@link StreamConsumer}), if allowed by the value returned by {@link #letConsumeOut()} and 
     * {@link #letConsumeErr()} methods.
     * NOTE: everything printed to STDOUT and STDERR is passed to {@link BuildOutputLogger}, if allowed by the value 
     * returned by {@link #letConsumeOut()} and {@link #letConsumeErr()} methods.
     *
     * @param workingDir  The directory to run the script from.
     * @param script  The details on the script to be run.
     * @param timeout Time in seconds after which the script is killed, if still running.
     * @param scriptInputProvider Optional script input provider. If set, data read from it are passed into the STDIN 
     *        of the script.
     * @param buildOutputConsumer Optional script output consumer. Everything printed on STDOUT by the script is passed
     *        into the consumer. Can be <code>null</code>, if you don't care about it.  
     * @return true if the script was killed due to timeout expiring
     * @throws CruiseControlException if it breaks
     */
    public boolean runScript(final File workingDir, final Script script, final long timeout,
                             final InputStream scriptInputProvider, final BuildOutputLogger buildOutputConsumer)
            throws CruiseControlException {

        final Commandline commandline = script.buildCommandline();

        if (workingDir != null) {
            // TODO: workingDir should be set already by the script
            // Remove workingDir parameter from the interface
            commandline.setWorkingDir(workingDir);
        }

        if (buildOutputConsumer != null) {
            buildOutputConsumer.clear();
        }

        final Process p;
        try {
            // Do not close STDOUT of the process. It will be closed by Pumper, once read (see Pumper#run())
            commandline.setCloseStdIn(false);
            p = commandline.execute();
        } catch (IOException e) {
            throw new CruiseControlException("Encountered an IO exception while attempting to execute '"
                    + script.toString() + "'. CruiseControl cannot continue.", e);
        }

        final CompositeConsumer consumerForError = new CompositeConsumer(getDirectErrLogger());
        final CompositeConsumer consumerForOut = new CompositeConsumer(getDirectOutLogger());

        if (buildOutputConsumer != null) {
            if (letConsumeErr()) {
                consumerForError.add(buildOutputConsumer);
            }
            if (letConsumeOut()) {
                consumerForOut.add(buildOutputConsumer);
            }
        }

        // Pass the output through the script if required (required by default), since it searches for error 
        // string in the messages, and includes the messages in build report.   
        if (script instanceof StreamConsumer) {
            if (letConsumeErr()) {
                consumerForError.add((StreamConsumer) script);
            }
            if (letConsumeOut()) {
                consumerForOut.add((StreamConsumer) script);
            }
        }

        final StreamPumper errorPumper = getErrPumper(p, consumerForError);
        final StreamPumper outPumper = getOutPumper(p, consumerForOut);
        final StreamPumper inPumper = getInPumper(p, scriptInputProvider);

        final Thread stdin = new Thread(inPumper);
        stdin.start();
        final Thread stderr = new Thread(errorPumper);
        stderr.start();
        final Thread stdout = new Thread(outPumper);
        stdout.start();
        final AsyncKiller killer = new AsyncKiller(p, timeout);
        final Thread asyncKillerThread;
        if (timeout > 0) {
            asyncKillerThread = new Thread(killer);
            asyncKillerThread.start();
        } else {
            asyncKillerThread = null;
        }

        int exitCode = -1;
        try {
            exitCode = p.waitFor();
            if (asyncKillerThread != null) {
                asyncKillerThread.interrupt();
            }
            stderr.join();
            stdout.join();
            stdin.join();
        } catch (InterruptedException e) {
            LOG.info("Was interrupted while waiting for script to finish."
                    + " CruiseControl will continue, assuming that it completed");
        } finally {
            IO.close(p);            
        }

        script.setExitCode(exitCode);

        if (buildOutputConsumer != null) {
            buildOutputConsumer.clear();
        }

        return !killer.processKilled();
    }
    
    public boolean runScript(Script script, long timeout, BuildOutputLogger logger) throws CruiseControlException {
        return runScript(null, script, timeout, null, logger);
    }

    /**
     * Returns the instance of StreamPumper which reads data from STDOUT of the process. This default
     * implementation returns new instance of StreamPumper class filled by <code>p.getInputStream()</code>
     * and <code>consumer</code>.
     *
     * @param  p the process to read STDOUT from. Note that the p.getInputStream() is called here!
     * @param  consumer the consumer to which the STDOUT is pushed by the pumper
     * @return the instance of stream pumper.
     * @see    #runScript(File, Script, long, InputStream, BuildOutputLogger) where the method 
     *         is called.
     */
    StreamPumper getOutPumper(final Process p, final StreamConsumer consumer) {
        return new StreamPumper(p.getInputStream(), consumer);
    } // getOutPumper

    /**
     * Returns the instance of StreamPumper which reads data from STDERR of the process. This default
     * implementation returns new instance of StreamPumper class filled by <code>p.getErrorStream()</code>
     * and <code>consumer</code>.
     *
     * @param  p the process to read STDERR from. Note that the p.getErrorStream() is called here!
     * @param  consumer the consumer to which the STDERR is pushed by the pumper
     * @return the instance of stream pumper.
     * @see    #runScript(File, Script, long, InputStream, BuildOutputLogger) where the method 
     *         is called.
     */
    StreamPumper getErrPumper(final Process p, final StreamConsumer consumer) {
        return new StreamPumper(p.getErrorStream(), consumer);
    } // getErrPumper

    /**
     * Returns the instance of StreamPumper which writes data to STDIN of the process. This default
     * implementation returns new instance of StreamPumper class filling <code>p.getOutputStream()</code>
     * from <code>source</code> stream.
     *
     * @param  p the process to write STDIN to. Note that the p.getOutputStream() is called here!
     * @param  source the stream from which to read data fill to the process
     * @return the instance of stream pumper.
     * @see    #runScript(File, Script, long, InputStream, BuildOutputLogger) where the method 
     *         is called.
     */
    StreamPumper getInPumper(final Process p, final InputStream source) {
        return new StreamPumper(source, true, null, p.getOutputStream());
    } // getInPumper

    /**
     * Returns the consumer through which everything printed to STDOUT of the script is stored 
     * directly into the log (through {@link #LOG} instance). This default implementation stores 
     * the STDOUT on {@link org.apache.log4j.Level#INFO} level.
     * 
     * @return the instance of stream consumer.
     * @see    #runScript(File, Script, long, InputStream, BuildOutputLogger) where the method 
     *         is called.
     */
    StreamConsumer getDirectOutLogger() {
        return StreamLogger.getInfoLogger(LOG);
    } // getDirectOutLogger
    /**
     * Returns the consumer through which everything printed to STDERR of the script is stored 
     * directly into the log (through {@link #LOG} instance). This default implementation stores 
     * the STDOUT on {@link org.apache.log4j.Level#WARN} level. 
     * 
     * @return the instance of stream consumer.
     * @see    #runScript(File, Script, long, InputStream, BuildOutputLogger) where the method 
     *         is called.
     */
    StreamConsumer getDirectErrLogger() {
        return StreamLogger.getWarnLogger(LOG);
    } // getDirectErrLogger

    /**
     * The value returned controls if everything printed to STDOUT of the script is passed to
     * other consumers in {@link #runScript(File, Script, long, InputStream, BuildOutputLogger)}. 
     * Mind that the output may be quite large! This default implementation returns <code>true</code>.
     * 
     * @return let the STDOUT of the script be consumed by Script and BuildOutputLogger
     * @see    #runScript(File, Script, long, InputStream, BuildOutputLogger) where the method 
     *         is called.
     */
    boolean letConsumeOut() {
        return true;
    } // letConsumeOut
    /**
     * The value returned controls if everything printed to STDERR of the script is passed to 
     * other consumers in {@link #runScript(File, Script, long, InputStream, BuildOutputLogger)}. 
     * Mind that the output may be quite large! This default implementation returns <code>true</code>.
     * 
     * @return let the STDERR of the script be consumed by Script and BuildOutputLogger
     * @see    #runScript(File, Script, long, InputStream, BuildOutputLogger) where the method 
     *         is called.
     */
    boolean letConsumeErr() {
        return true;
    } // letConsumeErr
    
}
