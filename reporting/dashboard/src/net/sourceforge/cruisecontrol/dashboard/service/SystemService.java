package net.sourceforge.cruisecontrol.dashboard.service;

import org.apache.tools.ant.util.FileUtils;

public class SystemService {
    public String getProperty(String prop) {
        return System.getProperty(prop);
    }

    public boolean isAbsolutePath(String artifactsDir) {
        return FileUtils.isAbsolutePath(artifactsDir);
    }
}
