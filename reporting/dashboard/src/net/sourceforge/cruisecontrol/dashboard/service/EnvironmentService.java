package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;

import javax.servlet.ServletContext;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.web.context.ServletContextAware;

public class EnvironmentService implements ServletContextAware {
    public static final String PROPS_CC_HOME = "cc.home";

    public static final String PROPS_CC_CONFIG_FILE = "cc.config.file";

    public static final String PROPS_CC_CONFIG_EDITABLE = "cc.config.editable";

    public static final String PROPS_CC_CONFIG_JMX_PORT = "cc.jmxport";

    public static final String PROPS_CC_CONFIG_RMI_PORT = "cc.rmiport";

    public static final String PROPS_CC_CONFIG_FORCEBUILD_ENABLED = "cc.config.forcebuild";

    public static final String PROPS_CC_CONFIG_LOG_DIR = "cc.logdir";

    public static final String PROPS_CC_CONFIG_ARTIFACTS_DIR = "cc.artifacts";

    public static final String PROPS_CC_CONFIG_PROJECTS_DIR = "cc.projects";

    public static final String CONTEXT_CC_CONFIG_FILE = "cruisecontrol.config.file";

    public static final String CONTEXT_CC_CONFIG_EDITABLE = "cruisecontrol.config.editable";

    public static final String CONTEXT_CC_CONFIG_JMX_PORT = "cruisecontrol.jmxport";

    public static final String CONTEXT_CC_CONFIG_RMI_PORT = "cruisecontrol.rmiport";

    public static final String CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED =
            "cruisecontrol.config.forcebuild";

    public static final String CONTEXT_CC_CONFIG_LOG_DIR = "cruisecontrol.logdir";

    private static final Logger LOGGER = Logger.getLogger(EnvironmentService.class);

    public static final String CONTEXT_CC_CONFIG_ARTIFACTS_DIR = "cruisecontrol.artifacts";

    public static final String CONTEXT_CC_CONFIG_PROJECTS_DIR = "cruisecontrol.projects";

    private ServletContext servletContext;

    private final SystemService systemService;

    public EnvironmentService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private String getConfigProperty(String props, String context) {
        String propValues = StringUtils.defaultString(systemService.getProperty(props));
        if (StringUtils.isNotEmpty(propValues)) {
            return propValues;
        } else if (servletContext != null) {
            String initParameter = servletContext.getInitParameter(context);
            LOGGER.debug("Using value '" + initParameter + "' for init parameter " + context);
            return StringUtils.defaultString(initParameter);
        } else {
            return null;
        }
    }

    public File getConfigXml() {
        String filename = getConfigProperty(PROPS_CC_CONFIG_FILE, CONTEXT_CC_CONFIG_FILE);
        return filename != null ? new File(filename) : null;
    }

    public boolean isConfigFileEditable() {
        String isEditable = getConfigProperty(PROPS_CC_CONFIG_EDITABLE, CONTEXT_CC_CONFIG_EDITABLE);
        if (StringUtils.isEmpty(isEditable)) {
            return true;
        } else {
            return BooleanUtils.toBoolean(isEditable);
        }
    }

    public int getJmxPort() {
        String jmxPort = getConfigProperty(PROPS_CC_CONFIG_JMX_PORT, CONTEXT_CC_CONFIG_JMX_PORT);
        int port = NumberUtils.toInt(StringUtils.defaultIfEmpty(jmxPort, "8000"));
        LOGGER.debug("Using " + port + " as jmx port in dashboard");
        return port;
    }

    public int getRmiPort() {
        String rmiPort = getConfigProperty(PROPS_CC_CONFIG_RMI_PORT, CONTEXT_CC_CONFIG_RMI_PORT);
        int port = NumberUtils.toInt(StringUtils.defaultIfEmpty(rmiPort, "1099"));
        LOGGER.debug("Using " + port + " as rmi port in dashboard");
        return port;
    }

    public boolean isForceBuildEnabled() {
        String isEnabled =
                getConfigProperty(PROPS_CC_CONFIG_FORCEBUILD_ENABLED,
                        CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED);
        if (StringUtils.isEmpty(isEnabled)) {
            return true;
        } else {
            return isEnabled.equals("enabled");
        }
    }

    public File getLogDir() throws ConfigurationException {
        return getCCHomeSubDir(PROPS_CC_CONFIG_LOG_DIR, CONTEXT_CC_CONFIG_LOG_DIR, "logs");
    }

    public File getArtifactsDir() throws ConfigurationException {
        return getCCHomeSubDir(PROPS_CC_CONFIG_ARTIFACTS_DIR, CONTEXT_CC_CONFIG_ARTIFACTS_DIR,
                "artifacts");
    }

    private File getCCHomeSubDir(String prop, String initParam, String defaultStr)
            throws ConfigurationException {
        String subDir = getConfigProperty(prop, initParam);
        subDir = StringUtils.isEmpty(subDir) ? defaultStr : subDir;
        if (systemService.isAbsolutePath(subDir)) {
            return new File(subDir);
        } else {
            String ccHome = StringUtils.defaultString(systemService.getProperty(PROPS_CC_HOME));
            if (StringUtils.isEmpty(ccHome)) {
                throw new ConfigurationException("Failed to locate " + defaultStr + " dir "
                        + subDir);
            } else {
                return new File(new File(ccHome), subDir);
            }
        }
    }

    public File getProjectsDir() throws ConfigurationException {
        return getCCHomeSubDir(PROPS_CC_CONFIG_PROJECTS_DIR, CONTEXT_CC_CONFIG_PROJECTS_DIR,
                "projects");
    }
}
