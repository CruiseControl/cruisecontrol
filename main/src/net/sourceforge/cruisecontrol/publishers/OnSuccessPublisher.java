/*
 * Created on Mar 24, 2005
 */
package net.sourceforge.cruisecontrol.publishers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

/**
 * @author Jeffrey Fredrick
 */
public class OnSuccessPublisher implements Publisher {
    private List publishers = new ArrayList();

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Publisher#publish(org.jdom.Element)
     */
    public void publish(Element log) throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(log);
        if (helper.isBuildSuccessful()) {
            Iterator iterator = publishers.iterator();
            while (iterator.hasNext()) {
                Publisher publisher = (Publisher) iterator.next();
                publisher.publish(log);
            }            
        }
    }

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Publisher#validate()
     */
    public void validate() throws CruiseControlException {
        Iterator iterator = publishers.iterator();
        while (iterator.hasNext()) {
            Publisher publisher = (Publisher) iterator.next();
            publisher.validate();
        }
    }

    public void add(Publisher publisher) {
        publishers.add(publisher);
    }

}
