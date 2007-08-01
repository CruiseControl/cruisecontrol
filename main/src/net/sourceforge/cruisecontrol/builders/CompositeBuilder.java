package net.sourceforge.cruisecontrol.builders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

public class CompositeBuilder extends Builder {

    private static final Logger LOG = Logger.getLogger(CompositeBuilder.class);

    private final List builders = new ArrayList();

    private long startTime = 0;
    
    public void add(Builder builder) {
        builders.add(builder);
    }

    private void startBuild() {
        startTime = System.currentTimeMillis();
    }

    private void endBuild(Element buildResult) {
        long endTime = System.currentTimeMillis();
        buildResult.setAttribute("time", DateUtil.getDurationAsString((endTime - startTime)));
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
            if (attribute.getName().equalsIgnoreCase("error")) {
                attributes.remove();
                attribute.detach();
                compositeBuildResult.setAttribute(attribute);
                return true;
            }
            attributes.remove();
            attribute.detach();
            compositeBuildResult.setAttribute(attribute);

        }
        // searching for errors (if we found one ore more, we will stop)
        Iterator messageElements = buildResult.getChildren("message").iterator();
        while (messageElements.hasNext()) {
            Element messageElement = (Element) messageElements.next();
            if (messageElement.getAttribute("priority").getValue().equals("error")) {
                LOG.debug("CompositeBuilder: errorlement found, stopping)");
                return true; // stop looking, since we found an error
            }
        }
        return false; // if we made it this far, no errors were found
    }

    public Element build(final Map properties, final Progress progressIn) throws CruiseControlException {

        final Progress progress = getShowProgress() ? progressIn : null;

        boolean errorOcurred = false;
        final Element compositeBuildResult = new Element("build");
        final Iterator iter = builders.iterator();

        int i = 0;
        final int totalBuilders = builders.size();

        startBuild();
        while (iter.hasNext() & !errorOcurred) {

            if (progress != null) {
                i++;
                progress.setValue("composite build " + i + " of " + totalBuilders);
            }

            final Builder builder = (Builder) iter.next();
            final Element buildResult = builder.build(properties, progress);
            errorOcurred = processBuildResult(buildResult, compositeBuildResult);
        }
        endBuild(compositeBuildResult);

        return compositeBuildResult;
    }

    public Element buildWithTarget(final Map properties, final String target, final Progress progressIn)
            throws CruiseControlException {

        final Progress progress = getShowProgress() ? progressIn : null;

        boolean errorOcurred = false;
        final Element compositeBuildResult = new Element("build");
        final Iterator iter = builders.iterator();

        int i = 0;
        final int totalBuilders = builders.size();

        startBuild();
        while (iter.hasNext() & !errorOcurred) {

            if (progress != null) {
                i++;
                progress.setValue("composite build " + i + " of " + totalBuilders);
            }

            final Builder builder = (Builder) iter.next();
            final Element buildResult = builder.buildWithTarget(properties, target, progress);
            errorOcurred = processBuildResult(buildResult, compositeBuildResult);
        }
        endBuild(compositeBuildResult);

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
