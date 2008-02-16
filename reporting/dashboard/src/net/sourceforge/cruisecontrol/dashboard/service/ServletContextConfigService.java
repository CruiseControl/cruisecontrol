package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;

public class ServletContextConfigService implements ServletContextAware, DashboardConfigService {
    private static final String WARNING_MESSAGE =
            "Configure dashboard via web.xml is deprecated. Use system properties or dashboard-config.xml instead.";

    private static final Logger LOGGER = Logger.getLogger(ServletContextConfigService.class);

    public static final String CONTEXT_CC_CONFIG_FILE = "cruisecontrol.config.file";

    public static final String CONTEXT_CC_CONFIG_EDITABLE = "cruisecontrol.config.editable";

    public static final String CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED = "cruisecontrol.config.forcebuild";

    public static final String CONTEXT_CC_CONFIG_LOG_DIR = "cruisecontrol.logdir";

    public static final String CONTEXT_CC_CONFIG_ARTIFACTS_DIR = "cruisecontrol.artifacts";

    public static final String CONTEXT_CC_CONFIG_PROJECTS_DIR = "cruisecontrol.projects";

    private ServletContext servletContext;

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public String getArtifactsDir() throws ConfigurationException {
        return getValueAndwarnDeprecated(CONTEXT_CC_CONFIG_ARTIFACTS_DIR);
    }

    public String getLogsDir() throws ConfigurationException {
        return getValueAndwarnDeprecated(CONTEXT_CC_CONFIG_LOG_DIR);
    }

    public String isForceBuildEnabled() {
        return getValueAndwarnDeprecated(CONTEXT_CC_CONFIG_FORCEBUILD_ENABLED);
    }

    private String getValueAndwarnDeprecated(final String parameter) {
        String value = StringUtils.defaultString(servletContext.getInitParameter(parameter));
        if (!StringUtils.isEmpty(value)) {
            LOGGER.warn(WARNING_MESSAGE);
        }
        return value;
    }
}
