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

package net.sourceforge.cruisecontrol;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import net.sourceforge.cruisecontrol.util.PerDayScheduleItem;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.jdom.Element;

public abstract class Builder extends PerDayScheduleItem implements Comparable {

    private int time = NOT_SET;
    private int multiple = 1;
    private boolean multipleSet = false;
    private boolean showProgress = true;
    private boolean isLiveOutput = true;
    private BuildOutputLogger buildOutputLogger;

    /** Build property name of property that is pass to all builders. */
    public static final String BUILD_PROP_PROJECTNAME = "projectname";

    /**
     * Execute a build.
     * @param properties build properties
     * @param progress callback to provide progress updates
     * @return the log resulting from executing the build
     * @throws CruiseControlException if something breaks
     */
    public abstract Element build(Map<String, String> properties, Progress progress) throws CruiseControlException;
    /**
     * Execute a build with the given target.
     * @param properties build properties
     * @param target the build target to call, overrides target defined in config
     * @param progress callback to provide progress updates
     * @return the log resulting from executing the build
     * @throws CruiseControlException if something breaks
     */
    public abstract Element buildWithTarget(Map<String, String> properties, String target, Progress progress)
            throws CruiseControlException;

    public void validate() throws CruiseControlException {
        boolean timeSet = time != NOT_SET;
        
        if (timeSet) {
          ValidationHelper.assertFalse(time < 0, "negative values for time are not allowed");
        }

        ValidationHelper.assertFalse(timeSet && multipleSet,
            "Only one of 'time' or 'multiple' are allowed on builders.");
    }

    public int getTime() {
        return time;
    }

    /**
     * can use ScheduleItem.NOT_SET to reset.
     * @param timeString new time integer
     */
    @Description("Time in the form HHmm. Can't be set if multiple is set.")
    @Optional
    public void setTime(String timeString) {
        time = Integer.parseInt(timeString);
    }

    /**
     * can use Builder.NOT_SET to reset.
     * @param multiple new multiple
     */
    @Description("Build index used to run different builders. For example, if this is set to 3, "
            + "the builder will be run every 3 builds. Default value is 1. Can't be set if time "
            + "is set.")
    @Optional
    public void setMultiple(int multiple) {
        multipleSet = multiple != NOT_SET;
        this.multiple = multiple;
    }

    public int getMultiple() {
        boolean timeSet = time != NOT_SET;
        if (timeSet && !multipleSet) {
            return NOT_SET;
        }
        return multiple;
    }

    @Description("If true, the builder will provide short progress messages, visible in the JSP "
            + "reporting application. If parent builders exist (eg: composite), and any parent's "
            + "showProgress=false, then no progress messages will be shown, regardless of this "
            + "builder's showProgress setting.")
    @Optional
    @Default("true")
    public void setShowProgress(final boolean showProgress) {
        this.showProgress = showProgress;
    }
    public boolean getShowProgress() {
        return showProgress;
    }

    @Description("If true, the builder will write all output to a file that can be read by the "
            + "Dashboard reporting application while the builder is executing.")
    @Optional
    @Default("true")
    public void setLiveOutput(final boolean isLiveOutputEnabled) {
        isLiveOutput = isLiveOutputEnabled;
    }
    public boolean isLiveOutput() {
        return isLiveOutput;
    }

    protected BuildOutputLogger getBuildOutputConsumer(final String projectName,
                                                       final File workingDir, final String logFilename) {

        if (isLiveOutput && buildOutputLogger == null) {

            final File outputFile;
            if (logFilename != null) {
                outputFile = new File(workingDir, logFilename);
            } else {
                try {
                    outputFile = File.createTempFile(
                            "ccLiveOutput-" + getFileSystemSafeProjectName(projectName)
                                    + "-" + getClass().getSimpleName() + "-",
                            ".tmp",
                            workingDir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            outputFile.deleteOnExit();

            final BuildOutputLogger buildOutputConsumer
                = BuildOutputLoggerManager.INSTANCE.lookupOrCreate(projectName, outputFile);
            buildOutputConsumer.clear();

            buildOutputLogger = buildOutputConsumer;
        }

        return buildOutputLogger;
    }

    /**
     * @param projectName the actual project name from the config file.
     * @return a string that is safe to using in a file system path (eg: with slashes replaced by underscores).
     */
    public static String getFileSystemSafeProjectName(final String projectName) {
        final String safeProjectName;
        if (projectName != null) {
            safeProjectName = projectName.replaceAll("/", "_"); // replace prevents error if name has slash
        } else {
            safeProjectName = null;
        }
        return safeProjectName;
    }

    /**
     * Is this the correct day to be running this builder?
     * @param now the current date
     * @return true if this this the correct day to be running this builder 
     */
    public boolean isValidDay(Date now) {
        if (getDay() < 0) {
            return true;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        return cal.get(Calendar.DAY_OF_WEEK) == getDay();
    }

    /**
     *  used to sort builders.  we're only going to care about sorting builders based on build number,
     *  so we'll sort based on the multiple attribute.
     */
    public int compareTo(Object o) {
        Builder builder = (Builder) o;
        Integer integer = new Integer(multiple);
        Integer integer2 = new Integer(builder.getMultiple());
        return integer2.compareTo(integer); //descending order
    }

    public boolean isTimeBuilder() {
        return time != NOT_SET;
    }

}
