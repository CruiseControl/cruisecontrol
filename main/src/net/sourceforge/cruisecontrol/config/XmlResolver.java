package net.sourceforge.cruisecontrol.config;

import java.io.File;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom2.Element;

public interface XmlResolver {

    Element getElement(String path) throws CruiseControlException;


    /**
     * Dummy XmlResolver implementation for case when "real" XmlResolver is not available.
     * The implementation simply gets the root element from the given XML file, nothing more.
     */
    public static final class DummyResolver implements XmlResolver {
        private static final Logger LOG = Logger.getLogger(DummyResolver.class);

        /** The implementation of {@link XmlResolver#getElement(String)} returning the
         *  element read from file */
        public Element getElement(final String path) throws CruiseControlException {
            LOG.warn("Using dummy resolver for XML '" + path + "'. Changes in the file will not be reflected" 
                   + "in the project!");
            final File file = new File(path);
            return Util.loadRootElement(file);
        }
    }
}
