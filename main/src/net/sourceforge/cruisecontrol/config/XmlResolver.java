package net.sourceforge.cruisecontrol.config;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.jdom.Element;

public interface XmlResolver {

    Element getElement(String path) throws CruiseControlException;

}
