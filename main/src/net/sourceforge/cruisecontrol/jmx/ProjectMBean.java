/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.jmx;

import java.util.List;

import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;

public interface ProjectMBean {

    /**
     * Pauses the controlled project.
     */
    public void pause();

    /**
     * Resumes the controlled project.
     */
    public void resume();

    /**
     * Runs a build now
     */
    public void build();

    /**
     * Runs a build now, overriding the target of the used builder
     * @param target the target to invoke
     */
    public void buildWithTarget(String target);

    /**
     * Runs a build now, overriding the target of the used builder
     * and passing additional properties
     * @param target the target to invoke
     * @param addedProperties the additional properties that will be passed to the build
     */
    public void buildWithTarget(String target, Map<String, String> addedProperties);

    /**
     * Serialize the project
     */
    public void serialize();

    /**
     * Is the project paused?
     *
     * @return Pause state
     */
    public boolean isPaused();

    /**
     *
     * @return start time of the last build, using the format 'yyyyMMddHHmmss'
     */
    public String getBuildStartTime();

    /**
     * Change the Project label
     *
     * @param label a new label; should be valid for the current
     * LabelIncrementer
     */
    public void setLabel(String label);

    public String getLabel();

    /**
     * Change the last built date.  This can be used to manipulate whether
     * builds will be initiated.
     *
     * @param date date string in the form yyyyMMddHHmmss
     * @throws CruiseControlException if an invalid date string is given
     */
    public void setLastBuild(String date) throws CruiseControlException;

    public String getLastBuild();

    public boolean isLastBuildSuccessful();

    /**
     * Change the last built date.  This can be used to manipulate whether
     * builds will be initiated.
     *
     * @param date date string in the form yyyyMMddHHmmss
     * @throws CruiseControlException if an invalid date string is given
     */
    public void setLastSuccessfulBuild(String date) throws CruiseControlException;

    public String getLastSuccessfulBuild();

    /**
     * Change the directory where CruiseControl logs are kept
     *
     * @param logdir Relative or absolute path to the log directory
     */
    public void setLogDir(String logdir);

    public String getLogDir();

    /**
     * @return a list with the names of the available log files
     */
    public List<String> getLogLabels();

    /**
     * @param logLabel a valid build label, must exist in the list returned by {@link #getLogLabels()}.
     * @param firstLine the starting line in the log for the given build label
     * @return lines from the given firstLine up to max lines, or an empty array if no more lines exist.
     */
    public String[] getLogLabelLines(String logLabel, int firstLine);

    /**
     * Change the project name.  May cause problems if configuration file is
     * not also changed.
     * @param name the new project name
     */
    public void setProjectName(String name);

    public String getProjectName();

    /**
     * Change the interval between builds
     *
     * @param buildInterval Build interval in milliseconds
     */
    public void setBuildInterval(long buildInterval);

    public long getBuildInterval();

    /**
     * Gets the human-readable version of the project status.
     * @return the human-readable version of the project status
     */
    public String getStatus();

    /**
     * @return the commit message includes the commiter and message
     */
    public String[][] commitMessages();


    /**
     * @param firstLine The starting line to skip to.
     * @return Output from the live output buffer, after line specified (inclusive).
     */
    public String[] getBuildOutput(Integer firstLine);

    /**
     * @return  A unique (for this VM) identifying string for this logger instance.
     * Intended to allow reporting apps (eg: Dashboard) to check if
     * the "live output" log file has been reset and to start asking for output from the first line
     * of the current output file if the logger has changed.
     *
     * Before the first call to retrieveLines(), the client should call getOutputLoggerID(), and hold that ID value.
     * If a client later calls retrieveLines() with a non-zero 'firstLine' parameter, and receives an empty array
     * as a result, that client should call getOutputLoggerID() again, and if the ID value differs, start reading
     * using a zero 'firstLine' parameter.
     * @see net.sourceforge.cruisecontrol.util.BuildOutputLogger#retrieveLines(int).
     */
    public String getOutputLoggerID();
}
