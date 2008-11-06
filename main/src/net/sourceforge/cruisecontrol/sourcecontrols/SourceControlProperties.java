package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SourceControlProperties implements Serializable {

    private static final long serialVersionUID = -8991634210894755397L;
    
    private String property;
    private String propertyOnDelete;
    private Map<String, String> properties = new HashMap<String, String>();

    public Map<String, String> getPropertiesAndReset() {
        final Map<String, String> lvalue = new HashMap<String, String>();
        lvalue.putAll(properties);
        properties.clear();
        return lvalue;
    }

    public void assignPropertyName(String propertyName) {
        property = propertyName;
    }

    public void assignPropertyOnDeleteName(String propertyName) {
        propertyOnDelete = propertyName;
    }

    public void modificationFound() {
        if (property != null) {
            properties.put(property, "true");
        }
    }

    public void deletionFound() {
        if (propertyOnDelete != null) {
            properties.put(propertyOnDelete, "true");
        }
    }

    public void put(String key, String value) {
        properties.put(key, value);
    }

    public void putAll(Map<String, String> moreProperties) {
        properties.putAll(moreProperties);
    }
}
