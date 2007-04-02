package net.sourceforge.cruisecontrol.gendoc;

import java.util.List;
import java.util.ArrayList;

/**
 * The Plugin Model representation, composed of itself, its {@link Attribute attributes} and {@link Child children}.
 * Each element knows out to turn itself into XML thanks to implementing the {@link ToXml} interface.
 *  
 * @author jerome@coffeebreaks.org
 */
public class PluginModel implements ToXml {

    public String type;
    // public String parent;

    // so far I want to keep these 2 distincts
    public String name; // name for fake plugins
    public String registryName; // name for plugins in registry
    public String description;

    public Attributes attributes = new Attributes();
    public Children children = new Children();

    public boolean isInterface;
//    public Infos infos = new Infos();

    public PluginModel() {
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getRegistryName() {
        return registryName;
    }

    public String getDescription() {
        return description;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public Children getChildren() {
        return children;
    }

    public void toXml(XmlOutputter outputer) {
        outputer.startTag("ccplugin");
        outputer.addChild("modelVersion", getModelVersion());
        outputer.addChild("type", type);
        outputer.addChild("interface", Boolean.valueOf(isInterface).toString());
        /*
        if (parent != null) {
            outputer.addChild("parent", parent);
        }*/
        if (name != null) {
            outputer.addChild("name", name);
        }
        outputer.addChild("description", description);
        outputer.addChild(attributes);
        outputer.addChild(children);
        // outputer.addChild(infos);
        outputer.closeTag();
    }

    // FIXME change this to isNotInstanciable
    public boolean isInterface() {
        return isInterface;
    }

    /**
     *
     */
    public static class Attributes implements ToXml {
        public String description;
        public List attributes = new ArrayList();

        public String getDescription() {
            return description;
        }

        public List getAttributes() {
            return attributes;
        }

        public void toXml(XmlOutputter outputer) {
            if (attributes.size() == 0 && description == null)
                return;
            outputer.startTag("attributes");
            if (description != null) {
                outputer.addChild("description", description);
            }
            for (int i = 0; i < attributes.size(); i++) {
                Attribute attribute = (Attribute) attributes.get(i);
                outputer.addChild(attribute);
            }
            outputer.closeTag();
        }
    }

    public static class Attribute implements ToXml {
        String name;
        String required;
        String defaultValue;
        String description;
        String type;

        public String getName() {
            return name;
        }

        public String getRequired() {
            return required == null ? "" : required;
        }

        public String getDefaultValue() {
            return defaultValue == null ? "" : defaultValue;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public void toXml(XmlOutputter outputer) {
            outputer.startTag("attribute");
            outputer.addChild("name", name);
            outputer.addChild("type", type);
            outputer.addChild("required", required);
            if (defaultValue != null && defaultValue.length() > 0) {
                outputer.addChild("default", defaultValue);                
            }
            outputer.addChild("description", description);
            outputer.closeTag();
        }
    }

    public static class Children implements ToXml {
        public String description;
        public List children = new ArrayList();

        public String getDescription() {
            return description;
        }

        public List getChildren() {
            return children;
        }

        public void toXml(XmlOutputter outputer) {
            if (children.size() == 0 && description == null) {
                return;
            }
            outputer.startTag("children");
            if (description != null) {
                outputer.addChild("description", description);
            }
            for (int i = 0; i < children.size(); i++) {
                Child child = (Child) children.get(i);
                outputer.addChild(child);
            }
            outputer.closeTag();
        }
    }

    public static class Child implements ToXml {
        public String name;
        // public String cardinality;
        public String description;
        public String type;

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public void toXml(XmlOutputter outputer) {
            outputer.startTag("child");
            outputer.addChild("name", name);
            outputer.addChild("type", type);
            // outputer.addChild("cardinality", cardinality);
            outputer.addChild("description", description);
            outputer.closeTag();
        }

        public String toString() {
            return "<Child: name: " + name + " type: " + type + ">";
        }
    }

    public String getModelVersion() {
        return "1.0-SNAPSHOT";
    }

    public String toString() {
        return "<Model type: " + type + " name: " + name + ">";
    }
}
