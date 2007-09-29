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
import org.jdom.CDATA;

public class CompositeBuilder extends Builder {

    private static final Logger LOG = Logger.getLogger(CompositeBuilder.class);

    private final List builders = new ArrayList();

    private long startTime = 0;

    private long childStartTime = 0;

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


    private void startChild() {
        childStartTime = System.currentTimeMillis();
    }


    private static boolean processBuildResult(final Element buildResult, final Element compositeBuildResult,
                                              final String buildlogMsgPrefix, final Builder builder,
                                              final long childStartTime) {

        // add child builder info to build log
        insertBuildLogHeader(buildResult, buildlogMsgPrefix + " - " + builder.getClass().getName(), childStartTime);

        final Iterator elements = buildResult.getChildren().iterator();
        while (elements.hasNext()) {
            // combining the outputs
            final Element elem = (Element) elements.next();
            elements.remove();
            elem.detach();
            compositeBuildResult.addContent(elem);
        }

        // check for error (if we found one, we will stop)
        if (!isBuildSuccessful(buildResult)) {
            LOG.debug("CompositeBuilder: error element found, stopping)");

            final Attribute attribute = buildResult.getAttribute("error");
            attribute.detach();
            compositeBuildResult.setAttribute(attribute);

            return true; // stop, since we found an error in the last build
        }

        return false; // if we made it this far, no errors were found
    }

    /**
     * set the "header" for this part of the build log. turns it into an Ant target/task style element for reporting
     * purposes
     *
     * @param buildResult the element of the build log for the current child builder
     * @param buildLogMsg child builder info to add to the build log
     * @param childStartTime the time this child builder started building
     */
    private static void insertBuildLogHeader(final Element buildResult,
                                             final String buildLogMsg, final long childStartTime) {

        // add info from attributes of "build" tag from child build
        String buildMsgWithAttibs = buildLogMsg + "; child build attributes: ";
        Iterator attributes = buildResult.getAttributes().iterator();
        while (attributes.hasNext()) {
            Attribute attribute = (Attribute) attributes.next();
            buildMsgWithAttibs += attribute.getName() + "=" + attribute.getValue() + "; ";
        }

        // @todo Rearrange these elements (even nesting childLog elements?), might display this info in reporting apps

        final Element target = new Element("target");
        target.setAttribute("name", "composite");
        target.setAttribute("time", DateUtil.getDurationAsString((System.currentTimeMillis() - childStartTime)));

        final Element task = new Element("task");
        task.setAttribute("name", "composite-childbuilder");

        final Element msg = new Element("message");
        msg.addContent(new CDATA(buildMsgWithAttibs));
        msg.setAttribute("priority", "warn");
        task.addContent(msg);

        target.addContent(task);

        buildResult.addContent(0, target);


        final Element msgBuild = new Element("message");
        msgBuild.addContent(new CDATA(buildMsgWithAttibs));
        msgBuild.setAttribute("priority", "warn");

        buildResult.addContent(1, msgBuild);

    } // insertBuildLogHeader

    
    private static boolean isBuildSuccessful(final Element buildResult) {
        return (buildResult.getAttribute("error") == null);
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

            i++;
            final String buildlogMsgPrefix = "composite build " + i + " of " + totalBuilders;
            if (progress != null) {
                progress.setValue(buildlogMsgPrefix);
            }

            final Builder builder = (Builder) iter.next();
            startChild();
            final Element buildResult = builder.build(properties, progress);
            errorOcurred = processBuildResult(buildResult, compositeBuildResult,
                    buildlogMsgPrefix, builder, childStartTime);
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

            i++;
            final String buildlogMsgPrefix = "composite build " + i + " of " + totalBuilders;
            if (progress != null) {
                progress.setValue(buildlogMsgPrefix);
            }

            final Builder builder = (Builder) iter.next();
            startChild();
            final Element buildResult = builder.buildWithTarget(properties, target, progress);
            errorOcurred = processBuildResult(buildResult, compositeBuildResult,
                    buildlogMsgPrefix, builder, childStartTime);
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

    /** @return array of the builders in this composite. */
    public Builder[] getBuilders() {
        return (Builder[]) builders.toArray(new Builder[builders.size()]);
    }
}
