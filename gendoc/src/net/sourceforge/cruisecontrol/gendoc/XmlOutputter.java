package net.sourceforge.cruisecontrol.gendoc;

/**
 * Interface for XML generator.
 * @author jerome@coffeebreaks.org
 */
public interface XmlOutputter {
    void startTag(String name);
    void addChild(ToXml toXml);
    void addChild(String name, String value);
    void closeTag();
}
