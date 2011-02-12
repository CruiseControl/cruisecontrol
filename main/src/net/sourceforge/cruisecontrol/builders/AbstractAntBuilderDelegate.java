package net.sourceforge.cruisecontrol.builders;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.gendoc.annotations.Cardinality;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;

/**
 * Common base class for plugins that use an AntBuilder delegate.
 *
 * @author Dan Rollo
 * Date: Jul 23, 2008
 * Time: 7:36:23 PM
 */
public class AbstractAntBuilderDelegate {

    /*
     * FIXME: Using this delegate pattern requires us to duplicate all the annotations
     * for document construction (Like @Description). Could we instead inherit from AntBuilder,
     * as this would let us pull in all the annotations automatically?
     */
    private final AntBuilder delegate = new AntBuilder();

    /**
     * Constructor overrides default AntBuilder.showAntOutput value in delegate.
     * Required if showAntOutput defaults to true.
     */
    public AbstractAntBuilderDelegate() {
        delegate.setLiveOutput(false);
    }

    /** @return delegate AntBuilder instance. */
    protected AntBuilder getDelegate() {
        return delegate;
    }

    public void validate() throws CruiseControlException {
        delegate.validate();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setSaveLogDir(String)
     */
    @Description(
            "If supplied, a copy of the ant log will be saved in the specified "
            + "local directory. Example: saveLogDir=\"/usr/local/dev/projects/cc/logs\".")
    @Optional
    public void setSaveLogDir(String dir) {
        delegate.setSaveLogDir(dir);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntWorkingDir(String)
     */
    @Description(
            "Will invoke ANT in the specified directory. This directory can be "
            + "absolute or relative to the cruisecontrol working directory.")
    @Optional
    public void setAntWorkingDir(String dir) {
        delegate.setAntWorkingDir(dir);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntScript(String)
     */
    @Description(
            "Absolute filename of script (shell script or bat file) used to start Ant. "
            + "You can use this to make CruiseControl use your own Ant installation. "
            + "If this is not specified, the AntBuilder uses the Ant distribution that "
            + "ships with CruiseControl. See below for <a href=\"#ant-examples\">examples"
            + "</a>.")
    @Optional(
            "Recommended, however. Cannot be specified if anthome attribute "
            + "is also specified")
    public void setAntScript(String antScript) {
        delegate.setAntScript(antScript);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntHome(String)
     */
    @Description(
            "Directory in which Ant is installed. CruiseControl will attempt to use the "
            + "standard Ant execution scripts (i.e. ant.bat or ant). See below for "
            + "<a href=\"#ant-examples\">examples</a>.")
    @Optional("Cannot be specified if antscript attribute is also specified.")
    public void setAntHome(String antHome) {
        delegate.setAntHome(antHome);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTempFile(String)
     */
    @Description("Name of temp file used to capture output.")
    @Optional
    @Default("log.xml")
    public void setTempFile(String tempFileName) {
        delegate.setTempFile(tempFileName);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTarget(String)
     */
    @Description(
            "Ant target(s) to run. Default is \"\", or the default target for "
            + "the build file.")
    @Optional
    public void setTarget(String target) {
        delegate.setTarget(target);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setBuildFile(String)
     */
    @Description("Path to Ant build file.")
    @Optional
    @Default("build.xml")
    public void setBuildFile(String buildFile) {
        delegate.setBuildFile(buildFile);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setPropertyfile(String)
     */
    @Description(
            "Load all properties from file with -D properties (like child <code><a href=\""
            + "#antbuilderchildprop\">&lt;property&gt;</a></code> elements) taking "
            + "precedence. Useful when the propertyfile content can change for every build.")
    @Optional
    public void setPropertyfile(String propertyfile) {
        delegate.setPropertyfile(propertyfile);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseLogger(boolean)
     */
    @Description(
            "'true' if CruiseControl should call Ant using -logger; 'false' to call Ant "
            + "using '-listener', thus using the loggerclass as a Listener. uselogger="
            + "\"true\" will make Ant log its messages using the class specified by "
            + "loggerclassname as an Ant Logger, which can make for smaller log files since "
            + "it doesn't log DEBUG messages (see useDebug and useQuiet attributes below, "
            + "and the <a href=\"http://ant.apache.org/manual/listeners.html\">Ant manual</a>). "
            + "Set to false to have Ant echo ant messages to console "
            + "using its DefaultLogger, which is useful when debugging your ant build. "
            + "Defaults to 'false' to make initial setup easier but setting it to 'true' is "
            + "recommended for production situations."
            + "<br/><br/>"
            + "RE: liveOutput: If liveOutput=true AND uselogger=true, this builder will write "
            + "the ant output to a file (antBuilderOutput.log) that can be read by the "
            + "Dashboard reporting application. The liveOutput setting has no effect if "
            + "uselogger=false. <a href=\"#antbootstrapper\">AntBootstrapper</a> and "
            + "<a href=\"#antpublisher\">AntPublisher</a> do not provide access to "
            + "liveOutput, and operate as if liveOutput=false. NOTE: In order to show ant "
            + "output while uselogger=true, the AntBuilder uses a custom Build Listener. If "
            + "this interferes with your Ant build, set liveOutput=false (and please report "
            + "the problem).")
    @Optional
    public void setUseLogger(boolean useLogger) {
        delegate.setUseLogger(useLogger);
    }
    
    /**
     * Defaults to false in constructor.
     * @deprecated use {@link #setLiveOutput(boolean)} instead.
     */
    @SkipDoc
    public void setShowAntOutput(final boolean showAntOutput) {
       setLiveOutput(showAntOutput);
    }
    
    /**
     * Defaults to false in constructor.
     * @see AntBuilder#setLiveOutput(boolean)
     */
    @Description("If true, the builder will write all output to a file that can be read by the "
            + "Dashboard reporting application while the builder is executing.")
    @Optional
    @Default("false")
    public void setLiveOutput(final boolean showAntOutput) {
        delegate.setLiveOutput(showAntOutput);
    }
    
    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setProgressLoggerLib(String)
     */
    @Description(
            "Overrides the default -lib search path used to add support for showProgress "
            + "features in the ant builder. This search path ensures customized ant "
            + "Loggers/Listeners are available on the classpath of the ant builder VM. You "
            + "should not normally set this value. If you do set this value, you should "
            + "use the full path (including filename) to cruisecontrol-antprogresslogger.jar. "
            + "This setting has no effect if showProgress=false.")
    @Optional
    public void setProgressLoggerLib(String progressLoggerLib) {
        delegate.setProgressLoggerLib(progressLoggerLib);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createJVMArg()
     */
    @Description("Pass specified argument to the jvm used to invoke ant."
            + "Ignored if using anthome or antscript. The element has a single required"
            + "attribute: \"arg\".<br />"
            + "<strong>Example:</strong> <code>&lt;jvmarg arg=\"-Xmx120m\"/&gt;</code>")
    @Cardinality(min = 0, max = -1)
    public AntBuilder.JVMArg createJVMArg() {
        return delegate.createJVMArg();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createLib()
     */
    @Description("Used to define additional <a "
            + "href=\"http://ant.apache.org/manual/running.html#libs\">library directories</a> "
            + "for the ant build. The element has one required attribute: \"searchPath\".<br /> "
            + "<strong>Example:</strong> <code>&lt;lib searchPath=\"/home/me/myantextensions\"/"
            + "&gt;</code>")
    @Cardinality(min = 0, max = -1)
    public AntBuilder.Lib createLib() {
        return delegate.createLib();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createListener()
     */
    @Description("Used to define additional <a "
            + "href=\"http://ant.apache.org/manual/listeners.html\">listeners</a> for the "
            + "ant build. The element has one required attribute: \"classname\".<br />"
            + "<strong>Example:</strong> <code>&lt;listener classname=\"org.apache.tools."
            + "ant.listener.Log4jListener\"/&gt;</code>")
    @Cardinality(min = 0, max = -1)
    public AntBuilder.Listener createListener() {
        return delegate.createListener();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createProperty()
     */
    @Description("Used to define properties for the ant build. The element has two "
            + "required attributes: \"name\" and \"value\". These will be passed on the "
            + "ant command-line as \"-Dname=value\"<br />"
            + "<strong>Example:</strong> <code>&lt;property name=\"foo\" value=\"bar\"/"
            + "&gt;</code>")
    @Cardinality(min = 0, max = -1)
    public Property createProperty() {
        return delegate.createProperty();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseDebug(boolean)
     */
    @Description(
            "If true will invoke ant with -debug, which can be useful for debugging your "
            + "ant build. Defaults to 'false', cannot be set to 'true' if usequiet is "
            + "also set to 'true'. When used in combination with uselogger=\"true\", "
            + "this will result in bigger XML log files; otherwise, it will cause more "
            + "output to be written to the console by Ant's DefaultLogger.")
    @Optional
    public void setUseDebug(boolean debug) {
        delegate.setUseDebug(debug);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseQuiet(boolean)
     */
    @Description(
            "If true will invoke ant with -quiet, which can be useful for creating smaller "
            + "log files since messages with a priority of INFO will not be logged. Defaults "
            + "to 'false', cannot be set to 'true' if usedebug is also set to 'true'. "
            + "Smaller logfiles are only achieved when used in combination with uselogger="
            + "\"true\", otherwise there will just be less output echoed to the console by "
            + "Ant's DefaultLogger."
            + "<br/><br/>"
            + "RE: showProgress: useQuiet=\"true\" will prevent any progress messages from "
            + "being displayed. NOTE: In order to show progress, the AntBuilder uses custom "
            + "Build Loggers and Listeners. If these interfere with your Ant build, set "
            + "showProgress=false (and please report the problem).")
    @Optional
    public void setUseQuiet(boolean quiet) {
        delegate.setUseQuiet(quiet);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setKeepGoing(boolean)
     */
    @Description(
            "If true will invoke ant with -keep-going, which can be useful for performing "
            + "build steps after an optional step fails. Defaults to 'false'.")
    @Optional
    public void setKeepGoing(boolean keepGoing) {
        delegate.setKeepGoing(keepGoing);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#getLoggerClassName()
     */
    public String getLoggerClassName() {
        return delegate.getLoggerClassName();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setLoggerClassName(String)
     */
    @Description(
            "If you want to use another logger (or listener, when uselogger=\"false\") than "
            + "Ant's XmlLogger, you can specify the classname of the logger here. The logger "
            + "needs to output compatible XML, and the class needs to be available on the "
            + "classpath at buildtime.")
    @Optional
    @Default("org.apache.tools.ant.XmlLogger")
    public void setLoggerClassName(String string) {
        delegate.setLoggerClassName(string);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTimeout(long)
     */
    @Description(
            "Ant build will be halted if it continues longer than the specified timeout. "
            + "Value in seconds.")
    @Optional
    public void setTimeout(long timeout) {
        delegate.setTimeout(timeout);
    }
}
