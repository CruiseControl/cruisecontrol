package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.HashMap;
import java.util.Map;

public class SourceControlProperties {

    private String property;
    private String propertyOnDelete;
    private Map properties = new HashMap();
    
    public Map getPropertiesAndReset() {
        Map lvalue = new HashMap();
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

    public void putAll(Map moreProperties) {
        properties.putAll(moreProperties);
    }
}
