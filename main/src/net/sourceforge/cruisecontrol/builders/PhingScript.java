package net.sourceforge.cruisecontrol.builders;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;

public class PhingScript implements Script {
    private Map buildProperties;

    private boolean isWindows;
    private String phingScript;
    private String loggerClassName;
    private String tempFileName = "log.xml";
    private boolean useLogger;
    private boolean useQuiet;
    private boolean useDebug;
    private String buildFile = "build.xml";
    private List properties;
    private String target = "";
    private int exitCode;


    /**
     * construct the command that we're going to execute.
     *
     * @return Commandline holding command to be executed
     * @throws CruiseControlException on unquotable attributes
     */
    public Commandline buildCommandline() throws CruiseControlException {
        
        Commandline cmdLine = new Commandline();

        cmdLine.setExecutable(phingScript);
        
        if (useLogger) {
            cmdLine.createArguments("-logger", getLoggerClassName());
            cmdLine.createArguments("-logfile", tempFileName);
        } else {
            cmdLine.createArguments("-listener", getLoggerClassName());
            cmdLine.createArgument("-DXmlLogger.file=" + tempFileName);
        }

        // -debug and -quiet only affect loggers, not listeners: when we use the loggerClassName as
        // a listener, they will affect the default logger that writes to the console
        if (useDebug) {
            cmdLine.createArgument("-debug");
        } else if (useQuiet) {
            cmdLine.createArgument("-quiet");
        }

        for (Iterator propertiesIter = buildProperties.entrySet().iterator(); propertiesIter.hasNext(); ) {
            Map.Entry property = (Map.Entry) propertiesIter.next();
            String value = (String) property.getValue();
            if (!"".equals(value)) {
                cmdLine.createArgument("-D" + property.getKey() + "=" + value);
            }
        }

        for (Iterator antPropertiesIterator = properties.iterator(); antPropertiesIterator.hasNext(); ) {
            Property property = (Property) antPropertiesIterator.next();
            cmdLine.createArgument("-D" + property.getName() + "=" + property.getValue());
        }

        cmdLine.createArguments("-buildfile", buildFile);

        StringTokenizer targets = new StringTokenizer(target);
        while (targets.hasMoreTokens()) {
            cmdLine.createArgument(targets.nextToken());
        }
        
        System.out.println(cmdLine.toString());
        
        return cmdLine;
    } 
    
    /**
     * @param buildProperties The buildProperties to set.
     */
    public void setBuildProperties(Map buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     * @return Returns the loggerClassName.
     */
    public String getLoggerClassName() {
        return loggerClassName;
    }
    /**
     * @param loggerClassName The loggerClassName to set.
     */
    public void setLoggerClassName(String loggerClassName) {
        this.loggerClassName = loggerClassName;
    }
    /**
     * @param antScript The antScript to set.
     */
    public void setPhingScript(String antScript) {
        this.phingScript = antScript;
    }
    
    /**
     * @param isWindows The isWindows to set.
     */
    public void setWindows(boolean isWindows) {
        this.isWindows = isWindows;
    }
    /**
     * @param buildFile The buildFile to set.
     */
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }
    /**
     * @param tempFileName The tempFileName to set.
     */
    public void setTempFileName(String tempFileName) {
        this.tempFileName = tempFileName;
    }
    /**
     * @param useDebug The useDebug to set.
     */
    public void setUseDebug(boolean useDebug) {
        this.useDebug = useDebug;
    }
    /**
     * @param useLogger The useLogger to set.
     */
    public void setUseLogger(boolean useLogger) {
        this.useLogger = useLogger;
    }
    /**
     * @param useQuiet The useQuiet to set.
     */
    public void setUseQuiet(boolean useQuiet) {
        this.useQuiet = useQuiet;
    }

    /**
     * @param properties The properties to set.
     */
    public void setProperties(List properties) {
        this.properties = properties;
    }
    /**
     * @param target The target to set.
     */
    public void setTarget(String target) {
        this.target = target;
    }
    /**
     * @return Returns the exitCode.
     */
    public int getExitCode() {
        return exitCode;
    }
    /**
     * @param exitCode The exitCode to set.
     */
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
}
