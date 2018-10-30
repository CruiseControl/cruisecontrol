package net.sourceforge.cruisecontrol.builders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
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
    private String[] pipeFrom  = new String[0];
    /** The index of script to wait for, set by {@link #setWaitFor(String)}. */
    private String[] waitFor = new String[0];
    /** Signalizes wherever the script finished or not */
    private boolean isDone = false;
    /** Keep STDOUT gzipped? Set by {@link #setGZipStdout(boolean)}. */
    private Boolean gzip = Boolean.FALSE;
    /** Is STDOUT of the script binary? Set by {@link #setBinaryOutput(boolean)} */
    private Boolean binary = Boolean.FALSE;
    /** The buffer holding the output of the command */
    private transient StdoutBuffer outputBuffer = null;
    /** The stream to read STDIN of the command, set by {@link #setInputProvider(InputStream)}. */
    private transient InputStream[] inputProvider = null;
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
        ValidationHelper.assertIsSet(getID(), "ID", getClass());
    }

    /** The implementation of {@link PipedScript#initialize()}. Do not forget to call this method
     *  in the overridden classes!
     */
    @Override
    public void initialize() {
        if (Boolean.TRUE.equals(getGZipStdout())) {
            outputBuffer = new GZippedStdoutBuffer(log());
        } else {
            outputBuffer = new StdoutBuffer(log());
        }
        if (getBuildProperties() == null) {
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
    /** The implementation of {@link PipedScript#finish()}.
     */
    @Override
    public void finish() throws CruiseControlException {
        if (!isDone()) {
            throw new CruiseControlException("Calling finish() when not marked as done??!!");
        }
        if (outputBuffer != null) {
            outputBuffer.close();
            outputBuffer = null;
        }
        inputProvider = null;
    }

    /**
     * Main build caller. It calls {@link #build()} implementation and then clears
     * both pipe streams and sets {@link #isDone} to return <code>true</code>
     */
    @Override
    public final void run() {
        Element buildLog = new Element("PipedScript");
        buildLog.setAttribute("ID", this.getID());

        try {

            /* Start and store the build log element created */
            log().info("Script ID '" + this.getID() + "' started");
            buildLog = build();
            /* Add ID to the buid output, if not already there */
            if (buildLog.getAttribute("ID") == null) {
                buildLog.setAttribute("ID", this.getID());
            }
            log().info("Script ID '" + this.getID() + "' finished");

        } catch (Throwable e) {
            log().error("Script ID '" + this.getID() + "' failed", e);
            /* If no error attribute is set, set it now ... */
            if (buildLog.getAttribute("error") == null) {
                buildLog.setAttribute("error", e.getMessage());
            }
        } finally {
            /* Add the element into the parent */
            synchronized (buildLogParent) {
                this.buildLogParent.addContent(buildLog.detach());
            }
            /* Close the buffer to signalize that all has been written, and clear STDIN
             * provider to signalize to GC that it is not longer needed */
            this.outputBuffer.close();
            this.inputProvider = null;
            this.buildLogParent = null;
            this.isDone = true;
        }
    }

    @Description("The unique identifier of the command or script (any string). The value is referenced by "
            + "<code>pipefrom</code> and <code>waitfor</code> attributes.")
    @Required
    @Override
    public void setID(String value) {
        this.id = value;
    }
    @Override
    public String getID() {
        return this.id;
    }
    @Description("The comma-separated list of <i>id</i>s of scripts to connect to - the STDIO of this command "
            + "or script is fed up by  the STDOUT of scripts with the given <i>id</i>.")
    @Override
    public void setPipeFrom(String value) {
        this.pipeFrom = Helpers.split(value);
    }
    @Override
    public String[] getPipeFrom() {
        return this.pipeFrom;
    }
    @Description("The comma-separated list of <i>id</i>s of scripts to wait for. The execution of this "
            + "command or script will be hold until all the scripts with the given <i>id</i>s finish. It may be "
            + "used to prevent memory exhausting/swapping, or to wait for data generated by another script, which "
            + "are not passed through STDOUT-STDIN pipe.")
    @Override
    public void setWaitFor(String value) {
        if (value != null && value.length() == 0) {
            value = null;
        }
        this.waitFor = Helpers.split(value);
    }
    @Override
    public String[] getWaitFor() {
        return this.waitFor;
    }

    @SkipDoc
    @Override
    public void setBuildLogParent(Element buildLogParent) {
        this.buildLogParent = buildLogParent;
    }
    @SkipDoc
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

    @SkipDoc
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

    @SkipDoc
    @Override
    public void setInputProvider(InputStream stdinProvider, String id) throws CruiseControlException {
        final String[] pipeFr = getPipeFrom();

        if (inputProvider == null) {
            inputProvider = new InputStream[pipeFr.length];
        }
        for (int i = 0; i < Math.min(pipeFr.length, inputProvider.length); i++) {
            if (pipeFr[i].equals(id)) {
                inputProvider[i] = stdinProvider;
                return;
            }
        }
        throw new CruiseControlException("Script ID '" + this.getID() + "': unexpected pipe from ID=" + id);
    }
    /**
     * @return the instance set through {@link #setInputProvider(InputStream, String)} or empty stream reader
     *      if no provider has been set through {@link #setInputProvider(InputStream, String)}
     */
    protected InputStream getInputProvider(final String id) throws CruiseControlException {
        final String[] pipeFr = getPipeFrom();

        if (inputProvider == null) {
            return new ByteArrayInputStream(new byte[0]); // Nothing to read
        }
        for (int i = 0; i < Math.min(pipeFr.length, inputProvider.length); i++) {
            if (pipeFr[i].equals(id)) {
                return inputProvider[i];
            }
        }
        throw new CruiseControlException("Script ID '" + this.getID() + "': unexpected pipe from ID=" + id);
    }

    /**
     * @return the stream to which the output of the script is supposed to be written.
     * @throws NullPointerException when {@link #initialize()} has not been called.
     */
    protected OutputStream getOutputBuffer() {
        if (this.outputBuffer == null || isDone()) {
            throw new NullPointerException("Script ID '" + this.getID() + "': object has not been initialized");
        }
        return this.outputBuffer; // null can be returned
    }
    @Override
    public InputStream getOutputReader() {
        if (this.outputBuffer == null) {
            throw new NullPointerException("Script ID '" + this.getID() + "': object has not been initialized");
        }

        try {
            return this.outputBuffer.getContent();
        } catch (IOException e) {
            log().error("Script ID '" + this.getID() + "': unable to create STDOUT reader", e);
            return new ByteArrayInputStream(new byte[0]);
        }
    }
    @Override
    public boolean isDone() {
        /* Does not have to be synchronized, true cannot be returned when the build() is
         * still running ... */
        return this.isDone;
    }

    @Description("When there is an expectation that the STDOUT of the script is too large, setting this to "
            + "<code>true</code> will compresses the data stored within the internal buffer (and transparently "
            + "decompresses them transparently as passed to a script piped from this). This allows to save "
            + "a bit of memory.")
    @Default("false")
    @Override
    public void setGZipStdout(boolean gzip) {
        this.gzip = Boolean.valueOf(gzip);
    }
    @Override
    public Boolean getGZipStdout() {
        return this.gzip;
    }

    @Description("When the STDOUT of the script conains binary data (as opposed to text data), it is recommended "
            + "to set this to <code>true</code>. It will adjust the handling of the internal buffers.")
    @Default("false")
    @Override
    public void setBinaryOutput(boolean binary) {
        this.binary = Boolean.valueOf(binary);
    }
    @Override
    public Boolean getBinaryOutput() {
        return this.binary;
    }

    /**
     * The implementation of {@link PipedScript#setEnvGlue(EnvGlue)}. <b>Does nothing!</b>.
     */
    @SkipDoc
    public void setEnvGlue(final EnvGlue env) {
        // Does nothing
    }
}
