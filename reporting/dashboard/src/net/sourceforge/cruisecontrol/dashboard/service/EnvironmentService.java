package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import java.lang.reflect.Method;

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

public class EnvironmentService {
    private DashboardConfigService[] services;

    private SystemService systemService;

    public EnvironmentService(SystemService systemService, DashboardConfigService[] serviceArrays) {
        this.systemService = systemService;
        this.services = serviceArrays;
    }

    private String getConfigProperty(String methodName) {
        for (int i = 0; i < services.length; i++) {
            try {
                Method method = DashboardConfigService.class.getMethod(methodName, null);
                DashboardConfigService service = services[i];
                String propValues;
                propValues = StringUtils.defaultString(ObjectUtils.toString(method.invoke(service, null)));
                if (StringUtils.isNotEmpty(propValues)) {
                    return propValues;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    public File getConfigXml() {
        String filename = getConfigProperty("getConfigXml");
        return filename != null ? new File(filename) : null;
    }

    public boolean isConfigFileEditable() {
        return BooleanUtils.toBoolean(getConfigProperty("isConfigFileEditable"));
    }

    public int getJmxPort() {
        return NumberUtils.toInt(getConfigProperty("getJMXPort"));
    }

    public int getRmiPort() {
        return NumberUtils.toInt(getConfigProperty("getRMIPort"));
    }

    public boolean isForceBuildEnabled() {
        String isEnabled = getConfigProperty("isForceBuildEnabled");
        if (StringUtils.isEmpty(isEnabled)) {
            return true;
        } else {
            return isEnabled.equals("enabled");
        }
    }

    public File getLogDir() throws ConfigurationException {
        return getCCHomeSubDir("getLogsDir");
    }

    public File getArtifactsDir() throws ConfigurationException {
        return getCCHomeSubDir("getArtifactsDir");
    }

    public File getProjectsDir() throws ConfigurationException {
        return getCCHomeSubDir("getProjectsDir");
    }

    private File getCCHomeSubDir(String methodName) throws ConfigurationException {
        String subDir = getConfigProperty(methodName);
        if (systemService.isAbsolutePath(subDir)) {
            return new File(subDir);
        } else {
            String cchome = getConfigProperty("getCCHome");
            if (StringUtils.isEmpty(cchome)) {
                throw new ConfigurationException("Failed to invoke " + methodName + "to find " + subDir
                        + " (Have you forgotten to set cc.home?)");
            } else {
                return new File(new File(StringUtils.defaultString(cchome)), subDir);
            }
        }
    }
}
