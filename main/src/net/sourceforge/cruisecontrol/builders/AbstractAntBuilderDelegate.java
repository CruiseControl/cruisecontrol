package net.sourceforge.cruisecontrol.builders;

import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Common base class for plugins that use an AntBuilder delegate.
 *
 * @author Dan Rollo
 * Date: Jul 23, 2008
 * Time: 7:36:23 PM
 */
public class AbstractAntBuilderDelegate {

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
    public void setSaveLogDir(String dir) {
        delegate.setSaveLogDir(dir);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntWorkingDir(String)
     */
    public void setAntWorkingDir(String dir) {
        delegate.setAntWorkingDir(dir);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntScript(String)
     */
    public void setAntScript(String antScript) {
        delegate.setAntScript(antScript);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntHome(String)
     */
    public void setAntHome(String antHome) {
        delegate.setAntHome(antHome);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTempFile(String)
     */
    public void setTempFile(String tempFileName) {
        delegate.setTempFile(tempFileName);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTarget(String)
     */
    public void setTarget(String target) {
        delegate.setTarget(target);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setBuildFile(String)
     */
    public void setBuildFile(String buildFile) {
        delegate.setBuildFile(buildFile);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setPropertyfile(String)
     */
    public void setPropertyfile(String propertyfile) {
        delegate.setPropertyfile(propertyfile);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseLogger(boolean)
     */
    public void setUseLogger(boolean useLogger) {
        delegate.setUseLogger(useLogger);
    }
    /**
     * Defaults to false in constructor.
     * @deprecated use {@link #setLiveOutput(boolean)} instead.
     */
    public void setShowAntOutput(final boolean showAntOutput) {
       setLiveOutput(showAntOutput);
    }
    /**
     * Defaults to false in constructor.
     * @see AntBuilder#setLiveOutput(boolean)
     */
    public void setLiveOutput(final boolean showAntOutput) {
        delegate.setLiveOutput(showAntOutput);
    }
    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setProgressLoggerLib(String)
     */
    public void setProgressLoggerLib(String progressLoggerLib) {
        delegate.setProgressLoggerLib(progressLoggerLib);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createJVMArg()
     */
    public AntBuilder.JVMArg createJVMArg() {
        return delegate.createJVMArg();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createLib()
     */
    public AntBuilder.Lib createLib() {
        return delegate.createLib();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createListener()
     */
    public AntBuilder.Listener createListener() {
        return delegate.createListener();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createProperty()
     */
    public Property createProperty() {
        return delegate.createProperty();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseDebug(boolean)
     */
    public void setUseDebug(boolean debug) {
        delegate.setUseDebug(debug);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseQuiet(boolean)
     */
    public void setUseQuiet(boolean quiet) {
        delegate.setUseQuiet(quiet);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setKeepGoing(boolean)
     */
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
    public void setLoggerClassName(String string) {
        delegate.setLoggerClassName(string);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTimeout(long)
     */
    public void setTimeout(long timeout) {
        delegate.setTimeout(timeout);
    }
}
