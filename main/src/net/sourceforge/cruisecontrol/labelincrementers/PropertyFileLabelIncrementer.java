package net.sourceforge.cruisecontrol.labelincrementers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.sourceforge.cruisecontrol.LabelIncrementer;

import org.apache.log4j.Logger;
import org.jdom.Element;

public class PropertyFileLabelIncrementer implements LabelIncrementer {
    private static final Logger LOG = Logger.getLogger(PropertyFileLabelIncrementer.class);

    private String propertyFile;

    private String propertyName;

    private boolean preBuildIncrementer;

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
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ex) {
            }
        }
    }

}
