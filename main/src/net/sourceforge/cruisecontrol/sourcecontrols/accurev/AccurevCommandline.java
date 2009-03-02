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
/*
 * Created on 29-Jun-2005 by norru
 *
 * Copyright (C) Sony Computer Entertainment Europe
 *               Studio Liverpool Server Group
 * Licensed under the CruiseControl BSD license
 *
 * Authors:
 *     Nicola Orru' <Nicola_Orru@scee.net>
 */
package net.sourceforge.cruisecontrol.sourcecontrols.accurev;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.sourcecontrols.Accurev;
import net.sourceforge.cruisecontrol.util.EnvCommandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Allows to build and execute a valid accurev command line.
 *
 * @author <a href="mailto:Nicola_Orru@scee.net">Nicola Orru'</a>
 * @author <a href="mailto:jason_chown@scee.net">Jason Chown </a>
 */
public class AccurevCommandline extends EnvCommandline implements AccurevInputParser, Runner {
    private static final Logger LOG = Logger.getLogger(Accurev.class);
    private boolean verbose;
    private int runCount;
    private int returnCode;
    private AccurevCommand command;
    private AccurevInputParser inputParser;
    private static final int SUCCESS = 0;
    private boolean syntaxError;
    private Runner runner;

    /**
     * Creates a new AccurevCommandline.
     *
     * @param command
     */
    public AccurevCommandline(AccurevCommand command) {
        super("accurev");
        this.inputParser = this;
        this.runner = this;
        this.command = command;
        createArgument().setValue(command.toString());
    }

    /**
     * Sets the Accurev stream to work in (-s stream)
     *
     * @param stream the stream name
     */
    public void setStream(String stream) {
        addOption("-s", stream);
    }

    /**
     * Sets the Accurev depot to work in (-d depot)
     *
     * @param depot the depot name
     */
    public void setDepot(String depot) {
        addOption("-d", depot);
    }

    /**
     * Sets the transaction comment (-c comment)
     *
     * @param comment the comment text. Quotes and escapes are not required.
     */
    public void setComment(String comment) {
        addOption("-c", comment);
    }

    /**
     * Switches the -i option on
     */
    public void setInfoOnly() {
        addArgument("-i");
    }

    /**
     * Selects a transaction range for hist as in (-t), single timespec
     *
     * @param time a timespec (can be a DateTimespec, a KeywordTimespec
     */
    public void setTransactionRange(Timespec time) {
        addOption("-t", time.toString());
    }

    /**
     * Selects a transaction range for hist as in (-t), timespec span (a-b, a-, -b)
     */
    public void setTransactionRange(Timespec begin, Timespec end) {
        StringBuffer buf = new StringBuffer();
        if (begin != null) {
            buf.append(begin);
        }
        buf.append("-");
        if (end != null) {
            buf.append(end);
        }
        addOption("-t", buf.toString());
    }

    /**
     * Selects a format for hist as in (-f)
     */
    public void setFormatExpanded(char format) throws CruiseControlException {
        if ("evstx".indexOf(format) < 0) {
            throw new CruiseControlException(
                    "Invalid format specifier (use one of 'e' 'v' 's' 't' 'x') " + format);
        }
        addOption("-f", new String(new char[]{format}));
    }

    /**
     * Adds an argument to the command line
     *
     * @param argument the argument to add (eg "-i"). Quotes and escape codes are not required.
     */
    public void addArgument(String argument) {
        createArgument().setValue(argument);
    }

    /**
     * Sets the input parser, which is the object that handles Accurev's output as its input.
     */
    public void setInputParser(AccurevInputParser inputParser) {
        this.inputParser = inputParser;
    }

    /**
     * Adds an option with an argument (eg. -s my_stream)
     *
     * @param option         the option flag (eg. "-s")
     * @param optionArgument the option argument ("eg. my_stream"). No need for quotes or escape characters.
     */
    public void addOption(String option, String optionArgument) {
        createArgument().setValue(option);
        createArgument().setValue(optionArgument);
    }

    /**
     * Selects all modified files in keep (as in -m)
     */
    public void selectModified() {
        addArgument("-m");
    }

    /**
     * Selects the files to use reading them from the filelist (as in -l filelistName)
     *
     * @param filelistName the path of the file containing the list of files to process
     */
    public void setFileList(String filelistName) {
        addOption("-l", filelistName);
    }

    /**
     * Selects the workspace to use, specifying its path in the local filesystem.
     *
     * @param workspace the workspace path
     * @throws CruiseControlException if the path does not exist
     */
    public void setWorkspaceLocalPath(File workspace) throws CruiseControlException {
        this.setWorkingDirectory(workspace.getAbsolutePath());
    }

    /**
     * Selects the workspace to use, specifying its path in the local filesystem.
     *
     * @param workspace the workspace path
     * @throws CruiseControlException if the path does not exist
     */
    public void setWorkspaceLocalPath(String workspace) throws CruiseControlException {
        this.setWorkingDirectory(workspace);
    }

    /**
     * Runs accurev and returns a reference to this.
     */
    public void run() {
        if (verbose) {
            LOG.info("Accurev: Executing '" + toString() + "'");
        }
        this.syntaxError = runner.execute(inputParser);
        this.returnCode = runner.getReturnCode();
        runCount++;
    }

    /**
     * Runs accurev and parses the output
     *
     * @return true if there are no parsing errors.
     */
    public boolean execute(AccurevInputParser inputParser) {
        Process proc;
        boolean error = false;
        try {
            proc = super.execute();
            Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, proc));
            stderr.start();
            InputStream input = proc.getInputStream();
            try {
                if (inputParser != null) {
                    error = !inputParser.parseStream(input);
                }
                returnCode = proc.waitFor();
                stderr.join();
            } finally {
                IO.close(proc);
            }
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(e.getMessage());
        } catch (CruiseControlException e) {
            LOG.error(e);
            throw new RuntimeException(e.getMessage());
        } catch (InterruptedException e) {
            LOG.error(e);
            throw new RuntimeException(e.getMessage());
        }
        return error;
    }

    protected String[] buildCommandLine() {
        return null;
    }

    /**
     * Default stream parser. It scans Accurev output and detects basic errors.
     *
     * @return true if no errors were found in Accurev's output.
     */
    public boolean parseStream(InputStream iStream) throws CruiseControlException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
        boolean badSyntax = false;
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("AccuRev was unable to understand your command.")) {
                    badSyntax = true;
                }
                if (verbose) {
                    LOG.info(line);
                }
            }
        } catch (IOException ex) {
            throw new CruiseControlException("Error reading input");
        }
        return !badSyntax;
    }

    /**
     * Gets the last "accurev" exec's return code.
     *
     * @return the return code from the command line. Usually 0 is SUCCESS.
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * Returns the accurev subcommand to be run by this command line object
     * (eg. keep, synctime, update). The subcommand can be selected by the
     * {@link #AccurevCommandline(AccurevCommand command)} constructor.
     *
     * @return the command
     */
    public AccurevCommand getCommand() {
        return command;
    }

    /**
     * Enables/disables verbose logging
     *
     * @param verbose if true, verbose logging is enabled.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Returns the verbose flag
     *
     * @return true if verbose logging is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Returns the run status
     *
     * @return true if the last command executed successfully (that is, returnCode == success and the
     *         parser didn't detect errors). It returns false if the command has not been run yet.
     */
    public boolean isSuccess() {
        return (runCount > 0) && (!syntaxError) && (returnCode == SUCCESS);
    }

    /**
     * Throws a CruiseControlException if the last command was not executed successfully.
     *
     * @throws CruiseControlException if the command was not executed successfully
     */
    public void assertSuccess() throws CruiseControlException {
        if (!isSuccess()) {
            throw new CruiseControlException("Error running " + toString());
        }
    }

    /**
     * Sets the runner
     *
     * @param runner
     *          the object that is in charge for provide some input to the parser
     */
    public void setRunner(Runner runner) {
    this.runner = runner;
  }
}
