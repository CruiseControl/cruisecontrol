package net.sourceforge.cruisecontrol.dashboard.service;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

public class EnvironmentService {
    private DashboardConfigService[] services;

    public EnvironmentService(DashboardConfigService[] serviceArrays) {
        this.services = serviceArrays;
    }

    private String getConfigProperty(DashboardConfigServiceMethod method) {
        for (int i = 0; i < services.length; i++) {
            try {
                String propValues = StringUtils.defaultString(method.execute(services[i]));
                if (StringUtils.isNotEmpty(propValues)) {
                    return propValues;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    public boolean isForceBuildEnabled() {
        return isEnabled(getConfigProperty(OF_FORCE_BUILD_ENABLED));
    }

    public File getLogDir() {
        return getDir(OF_LOGS);
    }

    public File getArtifactsDir() {
        return getDir(OF_ARTIFACTS);
    }

    private File getDir(DashboardConfigServiceMethod method) {
        return new File(getConfigProperty(method));
    }

    private boolean isEnabled(String isEnabled) {
        return "enabled".equalsIgnoreCase(isEnabled) || BooleanUtils.toBoolean(isEnabled);
    }

    private interface DashboardConfigServiceMethod {
        String execute(DashboardConfigService service) throws ConfigurationException;
    }

    private static final DashboardConfigServiceMethod OF_ARTIFACTS = new DashboardConfigServiceMethod() {
        public String execute(DashboardConfigService service) throws ConfigurationException {
            return service.getArtifactsDir();
        }
    };
    private static final DashboardConfigServiceMethod OF_LOGS = new DashboardConfigServiceMethod() {
        public String execute(DashboardConfigService service) throws ConfigurationException {
            return service.getLogsDir();
        }
    };
    private static final DashboardConfigServiceMethod OF_FORCE_BUILD_ENABLED = new DashboardConfigServiceMethod() {
        public String execute(DashboardConfigService service) {
            return service.isForceBuildEnabled();
        }
    };
}
