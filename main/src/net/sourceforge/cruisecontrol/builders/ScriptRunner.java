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
    private static final Logger LOG = Logger.getLogger(ScriptRunner.class);
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
     */
    public boolean runScript(final File workingDir, final Script script, final long timeout)
            throws CruiseControlException {

        return runScript(workingDir, script, timeout, null);
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
     */
    public boolean runScript(final File workingDir, final Script script, final long timeout,
                             final BuildOutputLogger buildOutputConsumer)
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
            p = commandline.execute();
        } catch (IOException e) {
            throw new CruiseControlException("Encountered an IO exception while attempting to execute '"
                    + script.toString() + "'. CruiseControl cannot continue.", e);
        }

        final CompositeConsumer consumerForError = new CompositeConsumer(StreamLogger.getWarnLogger(LOG));
        final CompositeConsumer consumerForOut = new CompositeConsumer(StreamLogger.getInfoLogger(LOG));

        if (buildOutputConsumer != null) {
            //TODO: The build output buffer doesn't take into account Cruise running in multi-threaded mode.
            consumerForError.add(buildOutputConsumer);
            consumerForOut.add(buildOutputConsumer);
        }

        if (script instanceof StreamConsumer) {
            consumerForError.add((StreamConsumer) script);
            consumerForOut.add((StreamConsumer) script);
        }

        final StreamPumper errorPumper = new StreamPumper(p.getErrorStream(), consumerForError);
        final StreamPumper outPumper = new StreamPumper(p.getInputStream(), consumerForOut);

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
        } catch (InterruptedException e) {
            LOG.info("Was interrupted while waiting for script to finish."
                    + " CruiseControl will continue, assuming that it completed");
        } finally {
            IO.close(p);            
        }

        script.setExitCode(exitCode);

        return !killer.processKilled();
    }
    
    public boolean runScript(Script script, long timeout, BuildOutputLogger logger) throws CruiseControlException {
        return runScript(null, script, timeout, logger);
    }
}
