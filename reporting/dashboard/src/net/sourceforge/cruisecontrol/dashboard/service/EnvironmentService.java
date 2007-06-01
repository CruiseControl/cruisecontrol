package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;

import javax.servlet.ServletContext;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.web.context.ServletContextAware;

public class EnvironmentService implements ServletContextAware {
    public static final String PROPS_CC_CONFIG_FILE = "cc.config.file";

    public static final String PROPS_CC_CONFIG_EDITABLE = "cc.config.editable";

    public static final String PROPS_CC_CONFIG_JMX_PORT = "cc.jmxport";

    public static final String PROPS_CC_CONFIG_RMI_PORT = "cc.rmiport";

    public static final String CONTEXT_CC_CONFIG_FILE = "cruisecontrol.config.file";

    public static final String CONTEXT_CC_CONFIG_EDITABLE = "cruisecontrol.config.editable";

    public static final String CONTEXT_CC_CONFIG_JMX_PORT = "cruisecontrol.jmxport";

    public static final String CONTEXT_CC_CONFIG_RMI_PORT = "cruisecontrol.rmiport";

    private static final Logger LOGGER = Logger.getLogger(EnvironmentService.class);

    private ServletContext servletContext;

    private String getConfigProperty(String props, String context) {
        String propValues = StringUtils.defaultString(System.getProperty(props));
        if (StringUtils.isNotEmpty(propValues)) {
            return propValues;
        } else if (servletContext != null) {
            String initParameter = servletContext.getInitParameter(context);
            LOGGER.info(initParameter + " is loading from the init parameter " + context);
            return StringUtils.defaultString(initParameter);
        } else {
            return "";
        }
    }

    public File getConfigXml() {
        return new File(getConfigProperty(PROPS_CC_CONFIG_FILE, CONTEXT_CC_CONFIG_FILE));
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
        LOGGER.info(port + " is using in dashboard as jmx port");
        return port;
    }

    public int getRmiPort() {
        String rmiPort = getConfigProperty(PROPS_CC_CONFIG_RMI_PORT, CONTEXT_CC_CONFIG_RMI_PORT);
        int port = NumberUtils.toInt(StringUtils.defaultIfEmpty(rmiPort, "1099"));
        LOGGER.info(port + " is using in dashboard as rmi port");
        return port;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
