package net.sourceforge.cruisecontrol.gendoc;

/**
 * Visitor for converting a component into its XML representation.
 * @author jerome@coffeebreaks.org
 */
public interface ToXml {
    void toXml(XmlOutputter outputer);
}
