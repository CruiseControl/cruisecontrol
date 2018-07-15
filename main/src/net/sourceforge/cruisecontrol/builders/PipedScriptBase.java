package net.sourceforge.cruisecontrol.builders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.GZippedStdoutBuffer;
import net.sourceforge.cruisecontrol.util.StdoutBuffer;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

/**
 * The implementation of {@link PipedScript} handling all the boring cases.
 * @author dtihelka
 */
public abstract class PipedScriptBase implements PipedScript {

    /** The ID of the script set by {@link #setID(String)}. */
    private String id = null;
    /** The ID of script to pipe from, set by {@link #setPipeFrom(String)}. */
    private String pipeFrom = null;
    /** The index of script to wait for, set by {@link #setWaitFor(String)}. */
    private String waitFor = null;
    /** The value set by {@link #setRepipe(String)} */
    private String repipe = null;
    /** The value set by {@link #setDisable(boolean)} */
    private boolean disable = false;
    /** Signalizes wherever the script finished or not */
    private boolean isDone = false;
    /** Keep STDOUT gzipped? Set by {@link #setGZipStdout(boolean)}. */
    private Boolean gzip = null;
    /** Is STDOUT of the script binary? Set by {@link #setBinaryOutput(boolean)} */
    private Boolean binary = null;
    /** The buffer holding the output of the command */
    private transient StdoutBuffer outputBuffer = null;
    /** The stream to read STDIN of the command, set by {@link #setInputProvider(InputStream)}. */
    private transient InputStream inputProvider = null;
    /** The build properties, set by {@link #setBuildProperties(Map)}. */
    private transient Map<String, String> buildProperties = null;
    /** The callback to provide progress updates, set by {@link #setProgress(Progress)}. */
    private transient Progress progress = null;
    /** The parent element into with the build log (created by {@link #build()} method) is stored. */
    private transient Element buildLogParent;


    /**
     * Execute the script and return the results as XML. The script may optionally be fed up
     * by data read from input, and its STDOUT may optionally be stored in for later use.
     *
     * Use the variables/methods of the object to get the required data. The method is called
     * from {@link #run()}.
     *
     * @return the XML element with the script run information.
     * @throws CruiseControlException when the build fails
     */
    protected abstract Element build() throws CruiseControlException;

    /**
     * @return the instance of Logger
     */
    protected abstract Logger log();

    /** The implementation of {@link PipedScript#validate()}, it checks if all the required
     *  items are set.
     *  Do not forget to call this method in the overridden classes!
     *
     *  @throws CruiseControlException if the validation fails */
    @Override
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(id, "ID", getClass());
    }

    /** The implementation of {@link PipedScript#initialize()}. Do not forget to call this method
     *  in the overridden classes!
     */
    @Override
    public void initialize() {
        if (Boolean.TRUE.equals(gzip)) {
            outputBuffer = new GZippedStdoutBuffer(log());
        } else {
            outputBuffer = new StdoutBuffer(log());
        }
        if (buildProperties == null) {
            log().warn("Build properties has not been set, setting empty map");
            buildProperties = new HashMap<String, String>();
        }
        if (buildLogParent == null) {
            log().warn("Build log parent has not been set, setting dummy element");
            buildLogParent = new Element("DummyLogParent");
        }
        // No done from now on
        this.isDone = false;
    }

    /**
     * Main build caller. It calls {@link #build()} implementation and then cleares
     * both pipe streams and sets {@link #isDone} to return <code>true</code>
     */
    @Override
    public final void run() {
        try {
            Element buildLog;

            /* Start and store the build log element created */
            log().info("Script ID '" + this.getID() + "' started");
            buildLog = build();
            /* Add ID to the buid output, if not already there */
            if (buildLog.getAttribute("ID") == null) {
                buildLog.setAttribute("ID", this.getID());
            }
            /* Add the element into the parent */
            synchronized (buildLogParent) {
                this.buildLogParent.addContent(buildLog.detach());
            }
            log().info("Script ID '" + this.getID() + "' finished");

        } catch (Throwable e) {
            log().error("Script ID '" + this.getID() + "' failed", e);
        } finally {
            /* Close the buffer to signalize that all has been written, and clear STDIN
             * provider to signalize to GC that it is not longer needed */
            this.outputBuffer.close();
            this.inputProvider = null;
            this.buildLogParent = null;
            this.isDone = true;
        }
    }

//    /** The implementation of {@link PipedScript#clean()}, it cleans all large-memory consuming
//     *  objects hold. Do not forget to call this method in the overridden classes!
//     */
//    @Override
//    public void clean() {
//        // Clean those since they contain the largest amount of memory
//        this.outputBuffer = null;
//        this.buildLogParent = null;
//    }

    @Override
    public void setID(String value) {
        this.id = value;
    }
    @Override
    public String getID() {
        return this.id;
    }
    @Override
    public void setPipeFrom(String value) {
        this.pipeFrom = value;
    }
    @Override
    public String getPipeFrom() {
        return this.pipeFrom;
    }
    @Override
    public void setWaitFor(String value) {
        this.waitFor = value;
    }
    @Override
    public String getWaitFor() {
        return this.waitFor;
    }
    @Override
    public void setRepipe(String repipe) {
        this.repipe = repipe;
    }
    @Override
    public String getRepipe() {
        return this.repipe;
    }
    @Override
    public void setDisable(boolean disable) {
        this.disable = disable;
    }
    @Override
    public boolean getDisable() {
        return this.disable;
    }
    @Override
    public void setBuildLogParent(Element buildLogParent) {
        this.buildLogParent = buildLogParent;
    }
    @Override
    public void setBuildProperties(Map<String, String> buildProperties) {
        this.buildProperties = buildProperties; /* Shallow copy should be OK */
    }
    /**
     * @return the instance set through {@link #setBuildProperties(Map)} or empty map when no
     *      instance has been set
     */
    protected Map<String, String> getBuildProperties() {
        return this.buildProperties;
    }
    @Override
    public void setProgress(Progress progress) {
        this.progress = progress;
    }
    /**
     * @return the instance set through {@link #setProgress(Progress)} or <code>null</code> if no
     *      instance has been set through {@link #setProgress(Progress)}
     */
    protected Progress getProgress() {
        return this.progress; // null can be returned
    }
    @Override
    public void setInputProvider(InputStream stdinProvider) {
        this.inputProvider = stdinProvider;
    }
    /**
     * @return the instance set through {@link #setInputProvider(InputStream)} or <code>null</code>
     *      if no provider has been set through {@link #setInputProvider(InputStream)}
     */
    protected InputStream getInputProvider() {
        return this.inputProvider; // null can be returned
    }
    /**
     * @return the stream to which the output of the script is supposed to be written.
     * @throws NullPointerException when {@link #initialize()} has not been called.
     */
    protected OutputStream getOutputBuffer() {
        if (this.outputBuffer == null || this.isDone) {
            throw new NullPointerException("Object has not been initialized");
        }
        return this.outputBuffer; // null can be returned
    }
    @Override
    public InputStream getOutputReader() {
        if (this.outputBuffer == null) {
            throw new NullPointerException("Object has not been initialized");
        }

        try {
            return this.outputBuffer.getContent();
        } catch (IOException e) {
            log().error("exec ID=" + getID() + ": unable to create STDOUT reader", e);
            return new ByteArrayInputStream(new byte[0]);
        }
    }
    @Override
    public boolean isDone() {
        /* Does not have to be synchronized, true cannot be returned when the build() is
         * still running ... */
        return this.isDone;
    }

    @Override
    public void setGZipStdout(boolean gzip) {
        this.gzip = Boolean.valueOf(gzip);
    }
    @Override
    public Boolean getGZipStdout() {
        return this.gzip;
    }

    @Override
    public void setBinaryOutput(boolean binary) {
        this.binary = Boolean.valueOf(binary);
    }
    @Override
    public Boolean getBinaryOutput() {
        return this.binary;
    }
}
