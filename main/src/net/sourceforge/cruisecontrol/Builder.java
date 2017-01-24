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
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.cruisecontrol.gendoc.annotations.Cardinality;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
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
    private final LinkedList<EnvConf> env = new LinkedList<EnvConf>();

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
                final String outputSuff = ".tmp";
                final String outputPref = "ccLiveOutput-" + getFileSystemSafeProjectName(projectName)
                                + "-" + getClass().getSimpleName() + "-";
                try {
                    outputFile = File.createTempFile(outputPref, outputSuff, workingDir);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to create temporary file in workingdir="
                            + workingDir == null ? "<null>" : workingDir.getAbsolutePath(), e);
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
    public boolean isValidDay(final Date now) {
        if (getDay() < 0) {
            return true;
        }

        final Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        return cal.get(Calendar.DAY_OF_WEEK) == getDay();
    }

    /**
     *  used to sort builders.  we're only going to care about sorting builders based on build number,
     *  so we'll sort based on the multiple attribute.
     */
    public int compareTo(final Object o) {
        final Builder builder = (Builder) o;
        final Integer integer = multiple;
        final Integer integer2 = builder.getMultiple();
        return integer2.compareTo(integer); //descending order
    }

    public boolean isTimeBuilder() {
        return time != NOT_SET;
    }

    /**
     * @return new {@link EnvConf} object to configure.
     */
    @Description("Used to define environment variables for the builder. The element has two "
            + "required attributes: \"name\" and either \"value\" or \"delete\".")
    @Cardinality(min = 0, max = -1)
    public EnvConf createEnv() {
        env.add(new EnvConf());
        return env.getLast();
    } // createEnv

    /**
     * Merges the environment settings configures through {@link EnvConf} classes with the
     * given environment values.
     *
     * Call this method in {@link #build(Map, Progress)} implementation, if the builder
     * supports the environment configuration.
     *
     * @param env the environment holder
     */
    protected void mergeEnv(final OSEnvironment env) {
        for (final EnvConf e : this.env) {
            e.merge(env);
        }
    } // merge

    /**
     * Class for the environment variables configuration. They are configured from XML
     * configuration in form:
     * <pre>
     * {@code
     *   <a_builder ...>
     *      <env name="ENV1" value="" />
     *      <env name="ENV2" delete="true" />
     *   </a_builder>
     * }
     * </pre>
     *
     * The configured class merges the environment changes with the actual environment
     * configuration using method {@link #merge(OSEnvironment)}.
     */
    @Description("Provides environment variable configuration.")
    public static final class EnvConf {

        private String name;
        private String value;
        // pattern used to find the ${*} strings to replace by ENV values
        private final Pattern prop = Pattern.compile("\\$\\{([^}]+)\\}");


        /** Constructor */
        public EnvConf() {
            name = "";
            value = null;
        }

        /**
         * Sets the name of the environment variable. Avoid explicit calls of the method
         * as it is supposed to be set when configuring the builder from CC XML configuration
         * only.
         * @param name the name of the variable
         */
        @Description("The name of the environment variable.")
        @Required
        public void setName(final String name) {
            this.name = name;
        } // setName
        /**
         * @return the name of the environment variable set by {@link #setName(String)}.
         */
        public String getName() {
            return this.name;
        } // setName

        /**
         * Sets the the environment variable to the new value. Avoid explicit calls of the
         * method as it is supposed to be set when configuring the builder from CC XML
         * configuration only.
         * @param val the new value to set
         */
        @Description("The (new) value of the environment variable.")
        @Required("Either a 'value' or 'delete' attribute must be set.")
        public void setValue(final String val) {
            this.value = val;
        } // setValue
        /**
         * @return the value of the environment variable set by {@link #setValue(String)},
         *         or <code>null</code> if no environment variable was defined yet.
         */
        public String getValue() {
            return this.value;
        } // setName
        /**
         * Mark the environment variable to delete. Avoid explicit calls of the method
         * as it is supposed to be set when configuring the builder from CC XML configuration
         * only.
         * @param thisParameterIsIgnored typically should be "true".
         */
        @Description("Marks the environment variable for removal.")
        @Required("Either a 'value' or 'delete' attribute must be set.")
        @SuppressWarnings({"UnusedParameters" })
        public void setDelete(final boolean thisParameterIsIgnored) {
            this.value = null;
        } // setDelete
        /**
         * @return the <code>true</code> if the given environment variable (named as get by
         *         {@link #getValue()} is supposed to be removed from the environment.
         */
        public boolean toDelete() {
            return this.value == null;
        } // setName

        /**
         * Merges the current configuration to the given environment variables.
         *
         * Although properties defined in CC project should already be resolved when passed
         * to {@link #setValue(String)}, it tries to replace remaining ${NAME} strings in
         * the variable value by the value of NAME environment variable (if defined).
         *
         * @param env the environment holder
         */
        void merge(final OSEnvironment env) {
            if (this.value == null) {
                env.del(this.name);
            } else {
                StringBuffer sb = new StringBuffer();
                Matcher m = prop.matcher(this.value);
                // resolve the ${*} properties remaining in the config using the environment
                // variables.
                while (m.find()) {
                     m.appendReplacement(sb, Matcher.quoteReplacement(env.getVariable(m.group(1), m.group(0))));
                }
                m.appendTail(sb);
                // Set the new value
                env.add(name, sb.toString());
            }
        }

        /** Copy the content of EnvConf to this class */
        public void copy(final EnvConf env) {
          this.name = env.name;
          this.value = env.value; // NULL is toDelete() was set
        }

    } // EnvConf

}
