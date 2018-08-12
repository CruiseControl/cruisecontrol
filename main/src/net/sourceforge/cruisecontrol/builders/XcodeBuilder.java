package net.sourceforge.cruisecontrol.builders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Directory;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom2.Element;

public class XcodeBuilder extends Builder implements Script {

    private static final Logger LOG = Logger.getLogger(XcodeBuilder.class);
    private static final String DEFAULT_OUTFILE_NAME = "xcodebuild.cc.output";

    Directory directory = new Directory();
    private int exitCode = -1;
    private boolean hitBuildFailedMessage;
    private long timeout = ScriptRunner.NO_TIMEOUT;
    private boolean buildTimedOut;
    private final Arguments arguments = new Arguments();
    private Map<String, String> buildProperties;
    
    @Override
    public Element build(Map<String, String> properties, Progress progressIn) throws CruiseControlException {

        final Progress progress = getShowProgress() ? progressIn : null;
        // @todo To support progress, determine text pattern indicating progress messages in output,
        // see AntScript.consumeLine() as an example

        setProperties(properties);
        OutputFile file = createOutputFile(directory, DEFAULT_OUTFILE_NAME);
        runScript(file);
        return elementFromFile(file);
    }

    void setProperties(Map<String, String> properties) {
        buildProperties = properties;
    }

    private void runScript(OutputFile file) throws CruiseControlException {
        LOG.info("starting build");
        String projectName = buildProperties.get(Builder.BUILD_PROP_PROJECTNAME);
        boolean finished = createScriptRunner().runScript(this, timeout,
                getBuildOutputConsumer(projectName, file.file, file.file.getName()));
        buildTimedOut = !finished;
        LOG.info("build finished with exit code " + exitCode);
    }

    ScriptRunner createScriptRunner() {
        return new ScriptRunner();
    }

    OutputFile createOutputFile(Directory d, String filename) {
        return new OutputFile(d, filename);
    }

    Element elementFromFile(OutputFile file) {
        hitBuildFailedMessage = false;
        
        Element build = new Element("build");
        while (file.hasMoreLines()) {
            String line = file.nextLine();
            Element message = getElementFromLine(line);
            if (message != null) {
                build.addContent(message);
            }
        }

        if (hitBuildFailedMessage) {
            build.setAttribute("error", "** BUILD FAILED **");
        } else if (timeout != ScriptRunner.NO_TIMEOUT && buildTimedOut) {
            build.setAttribute("error", "build timed out");
        }
        
        return build;
    }

    @Override
    public Element buildWithTarget(Map<String, String> properties, String target, Progress progress)
            throws CruiseControlException {
        arguments.overrideTarget(target);
        try {
            return build(properties, progress);
        } finally {
            arguments.resetTarget();
        }
    }

    @Override
    public void validate() throws CruiseControlException {
        LOG.debug("super.validate()");
        super.validate();
        
        LOG.debug("validate directory");
        directory.validate();
        
        LOG.debug("validate args");
        arguments.validate();
    }

    public Commandline buildCommandline() throws CruiseControlException {
        Commandline cmdLine = new Commandline();
        cmdLine.setWorkingDir(directory);
        cmdLine.setExecutable("xcodebuild");
        arguments.addArguments(cmdLine, buildProperties);
        return cmdLine;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int result) {
        LOG.debug("exit code set to " + result);
        exitCode = result;
    }

    public void setDirectory(String path) {
        directory.setPath(path);
    }

    public Element getElementFromLine(String line) {
        if (hitBuildFailedMessage) {
            return messageAtLevel(line, "error");
        }
        
        Element e = messageAtLevelIfContains(line, "warn", " warning: ");
        if (e != null) {
            return e;
        }
        
        e = messageAtLevelIfContains(line, "error", " error: ");
        if (e != null) {
            return e;
        }
        
        e = messageAtLevelIfContains(line, "error", "** BUILD FAILED **");
        if (e != null) {
            hitBuildFailedMessage = true;
            return e;
        }

        return null;
    }

    private Element messageAtLevelIfContains(String line, String messageLevel, String semaphore) {
        if (line.contains(semaphore)) {
            return messageAtLevel(line, messageLevel);
        }
        
        return null;
    }

    private Element messageAtLevel(String line, String messageLevel) {
        Element target = new Element("target");
        Element task = new Element("task");
        target.addContent(task);
        Element message = new Element("message");
        task.addContent(message);
        message.setAttribute("priority", messageLevel);
        message.setText(line);
        return target;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    static class OutputFile {
        private final File file;
        private BufferedReader reader;
        private String nextLine;

        OutputFile(Directory dir, String filename) {
            file = new File(dir.toFile(), filename);
        }
        
        public String nextLine() {
            return nextLine;
        }

        public boolean hasMoreLines() {
            if (reader == null) {
                createReader();
            }
            try {
                nextLine = reader.readLine();
            } catch (IOException e) {
                LOG.error("error reading file " + file.getAbsolutePath(), e);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                    }
                }
                throw new RuntimeException(e);
            }
            
            if (nextLine != null) {
                return true;
            }
            
            LOG.debug("reached end of build output");
            try {
                reader.close();
                reader = null;
            } catch (IOException ex) {
            }
            return false;
        }

        private void createReader() {
            LOG.debug("creating reader for file " + file.getAbsolutePath());
            try {
                reader = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                LOG.error("error creating reader for file " + file.getAbsolutePath(), e);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                    }
                }
                throw new RuntimeException(e);
            }
        }
    }

    @Description("Pass specified argument to xcodebuild. The element has the required attribute: value.")
    public Arg createArg() {
        return arguments.createArg();
    }
    
    class Arguments implements Serializable {

        private static final long serialVersionUID = -6252774653884713089L;

        private final List<Arg> args = new ArrayList<Arg>();
        private Arg overrideTarget;
        private Arg originalTarget;

        public Arg createArg() {
            Arg arg = new Arg();
            args.add(arg);
            return arg;
        }

        public void resetTarget() {
            args.remove(overrideTarget);
            if (originalTarget != null) {
              args.add(originalTarget);
              originalTarget = null;
            }
        }

        public void overrideTarget(String string) {
            saveOriginalTarget();
            overrideTarget = createArg();
            overrideTarget.setValue("-target " + string);
        }

        private void saveOriginalTarget() {
            for (final Arg arg : args) {
                if (arg.value.startsWith("-target ")) {
                    originalTarget = arg;
                    args.remove(arg);
                    break;
                }
            }
        }

        public void validate() throws CruiseControlException {
            for (final Arg arg : args) {
                arg.validate();
            }
        }

        public void addArguments(Commandline cmdLine, Map<String, String> buildProperties) {
            for (final Arg arg : args) {
                String value = substituteProperties(buildProperties, arg.value);
                cmdLine.createArgument().setValue(value);
            }
        }
        
        private String substituteProperties(Map<String, String> properties, String string) {
            String value = string;
            try {
                value = Util.parsePropertiesInString(properties, string, false);
            } catch (CruiseControlException e) {
                LOG.error("exception substituting properties into arguments: " + string, e);
            }
            return value;
        }

    }
    
    @Description("Passes an argument to xcodebuild. Example: <code>&lt;arg value=\"-project "
            + "${projectname}\"&gt;</code>.")
    public class Arg implements Serializable {

        private static final long serialVersionUID = 832468395631809962L;

        String value;

        @Description("Argument to be passed on the command line. Any of the <a href=\""
                + "#buildproperties\">properties passed to builders</a> can "
                + "be substituted into the value.")
        @Required
        public void setValue(String value) {
            this.value = value.trim();
        }

        public void validate() throws InvalidValueException {
            if (value.length() == 0) {
                throw new InvalidValueException();
            }
        }
        
        class InvalidValueException extends CruiseControlException {

            private static final long serialVersionUID = -1464414088701915032L;

            InvalidValueException() {
                super("value of arg can't be an empty string");
            }
        }
    }
}
