package net.sourceforge.cruisecontrol.builders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import net.sourceforge.cruisecontrol.BuildOutputLoggerManager;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Directory;

import org.apache.log4j.Logger;
import org.jdom.Element;

public class XcodeBuilder extends Builder implements Script {

    private static final Logger LOG = Logger.getLogger(XcodeBuilder.class);
    private static final String DEFAULT_OUTFILE_NAME = "xcodebuild.cc.output";

    Directory directory = new Directory();
    private int exitCode = -1;
    private boolean hitBuildFailedMessage;
    
    @Override
    public Element build(Map<String, String> properties, Progress progress) throws CruiseControlException {
        OutputFile file = createOutputFile(directory, DEFAULT_OUTFILE_NAME);
        runScript(file);
        return elementFromFile(file);
    }

    private void runScript(OutputFile file) throws CruiseControlException {
        LOG.info("starting build");
        new ScriptRunner().runScript(this, -1, file.getBuildOutputLogger());
        LOG.info("build finished");
    }

    OutputFile createOutputFile(Directory d, String filename) {
        return new OutputFile(d, filename);
    }

    private Element elementFromFile(OutputFile file) {
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
        }
        
        return build;
    }

    @Override
    public Element buildWithTarget(Map<String, String> properties, String target, Progress progress)
            throws CruiseControlException {
        // TODO: add target support
        return build(properties, progress);
    }

    @Override
    public void validate() throws CruiseControlException {
        LOG.debug("super.validate()");
        super.validate();
        
        LOG.debug("validate directory");
        directory.validate();
    }

    public Commandline buildCommandline() throws CruiseControlException {
        Commandline cmdLine = new Commandline();
        cmdLine.setWorkingDir(directory);
        cmdLine.setExecutable("xcodebuild");
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

    class OutputFile {
        private File file;
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

        private BuildOutputLogger getBuildOutputLogger() {
            final BuildOutputLogger logger = BuildOutputLoggerManager.INSTANCE.lookupOrCreate(file);
            logger.clear();
            return logger;
        }
    }
}
