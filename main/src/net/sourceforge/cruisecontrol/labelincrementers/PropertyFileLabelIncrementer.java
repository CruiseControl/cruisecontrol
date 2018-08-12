package net.sourceforge.cruisecontrol.labelincrementers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.sourceforge.cruisecontrol.LabelIncrementer;
import net.sourceforge.cruisecontrol.util.IO;

import org.apache.log4j.Logger;
import org.jdom2.Element;

public class PropertyFileLabelIncrementer implements LabelIncrementer {
    private static final Logger LOG = Logger.getLogger(PropertyFileLabelIncrementer.class);

    private String propertyFile;

    private String propertyName;

    private boolean preBuildIncrementer;

    private String defaultLabel;

    public String incrementLabel(String oldLabel, Element buildLog) {
        return getLabel();
    }

    public boolean isValidLabel(String label) {
        return true;
    }

    public boolean isPreBuildIncrementer() {
        return this.preBuildIncrementer;
    }

    public String getDefaultLabel() {
        return getLabel();
    }

    public void setPreBuildIncrementer(boolean preBuildIncrementer) {
        this.preBuildIncrementer = preBuildIncrementer;
    }

    public void setPropertyFile(String propertyFile) {
        this.propertyFile = propertyFile;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    private String getLabel() {
        if (propertyFile == null) {
            throw new IllegalStateException("property file not specified");
        }
        if (!new File(propertyFile).isFile()) {
            String message = "property file does not exist: " + propertyFile;
            if (defaultLabel == null) {
                throw new IllegalStateException(message);
            } else {
                LOG.info(message);
                LOG.info("using specified default label");
                return defaultLabel;
            }
        }
        
        InputStream is = null;
        try {
            is = new FileInputStream(propertyFile);
            Properties p = new Properties();
            p.load(is);

            String label = p.getProperty(propertyName);
            LOG.info("Retrieved label " + label);

            return label;
        } catch (IOException ex) {
            String msg = "Unable to retrieve label " + ex.getMessage();
            LOG.error(msg, ex);
            throw new RuntimeException(msg);
        } finally {
            IO.close(is);
        }
    }

    public void setDefaultLabel(String defaultLabel) {
        if (defaultLabel == null) {
            throw new IllegalArgumentException("null is not valid as the default label");
        }
        if ("".equals(defaultLabel)) {
            throw new IllegalArgumentException("empty string is not valid as the default label");
        }
        this.defaultLabel = defaultLabel;
    }

}
