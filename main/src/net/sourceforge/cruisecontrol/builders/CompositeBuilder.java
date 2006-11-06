package net.sourceforge.cruisecontrol.builders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

public class CompositeBuilder extends Builder {

    private static final Logger LOG = Logger.getLogger(CompositeBuilder.class);

    private List builders = new ArrayList();

    public void add(Builder builder) {
        try {
            builder.validate();
        } catch (CruiseControlException e) {
            LOG.error("error validating builder");
        }
        builders.add(builder);
    }

    private static boolean processBuildResult(Element buildResult, Element compositeBuildResult) {
        Iterator elements = buildResult.getChildren().iterator();
        while (elements.hasNext()) {
            // combining the outputs
            Element elem = (Element) elements.next();
            elements.remove();
            elem.detach();
            compositeBuildResult.addContent(elem);
        }
        Iterator attributes = buildResult.getAttributes().iterator();
        while (attributes.hasNext()) {
            Attribute attribute = (Attribute) attributes.next();
            attributes.remove();
            attribute.detach();
            compositeBuildResult.setAttribute(attribute);
        }
        // searching for errors (if we found one ore more, we will stop)
        Iterator messageElements = buildResult.getChildren("message").iterator();
        boolean errorOcurred = false;
        while (messageElements.hasNext()) {
            Element messageElement = (Element) messageElements.next();
            if (messageElement.getAttribute("priority").getValue().equals("error")) {
                LOG.debug("CompositeBuilder: errorlement found, stopping)");
                errorOcurred = true;
            }
        }
        return errorOcurred;
    }

    public Element build(Map properties) throws CruiseControlException {
        boolean errorOcurred = false;
        final Element compositeBuildResult = new Element("build");
        final Iterator iter = builders.iterator();
        while (iter.hasNext() & !errorOcurred) {
            final Builder builder = (Builder) iter.next();
            final Element buildResult = builder.build(properties);
            errorOcurred = processBuildResult(buildResult, compositeBuildResult);
        }
        return compositeBuildResult;
    }

    public Element buildWithTarget(Map properties, String target)
            throws CruiseControlException {

        boolean errorOcurred = false;
        final Element compositeBuildResult = new Element("build");
        final Iterator iter = builders.iterator();
        while (iter.hasNext() & !errorOcurred) {
            final Builder builder = (Builder) iter.next();
            final Element buildResult = builder.buildWithTarget(properties, target);
            errorOcurred = processBuildResult(buildResult, compositeBuildResult);
        }
        return compositeBuildResult;
    }

    public void validate() throws CruiseControlException {

        ValidationHelper.assertFalse(builders.isEmpty(), "no builders added");
        super.validate();

        // validate all child builders
        final Iterator iter = builders.iterator();
        while (iter.hasNext()) {
            final Builder builder = (Builder) iter.next();
            builder.validate();
        }
    }
}
