package net.sourceforge.cruisecontrol.builders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.gendoc.annotations.Cardinality;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.CDATA;

@Description(
        "<p>The CompositeBuilder executes a list of builders (any builder, except <a "
        + "href=\"#pause\">&lt;pause&gt;</a>). This is necessary for builds in an "
        + "empty directory (see keyword CRISP-builds in Pragmatic Project Automation "
        + "from Mike Clark) </p>")
public class CompositeBuilder extends Builder {

    private static final Logger LOG = Logger.getLogger(CompositeBuilder.class);

    private static final long serialVersionUID = -3819555247003945186L;

    private final List<Builder> builders = new ArrayList<Builder>();

    private long startTime = 0;
    private long timeoutSeconds = ScriptRunner.NO_TIMEOUT;
    private boolean isTimedOut;


    private long childStartTime = 0;

    @Description("Adds a builder to this composite builder.")
    @Cardinality(min = 0, max = -1)
    public void add(Builder builder) {
        builders.add(builder);
    }

    private void startBuild() {
        startTime = System.currentTimeMillis();
    }

    private void endBuild(Element buildResult) {
        if (isTimedOut) {
            LOG.warn("Composite Build timeout timer of " + timeoutSeconds + " seconds has expired");
            buildResult.setAttribute("error", "build timeout");
        }

        long endTime = System.currentTimeMillis();
        buildResult.setAttribute("time", DateUtil.getDurationAsString((endTime - startTime)));
    }

    private void checkTimedOut() {
        if (timeoutSeconds != ScriptRunner.NO_TIMEOUT
                && (System.currentTimeMillis() - startTime) > (timeoutSeconds * 1000L)) {
            isTimedOut = true;
        }
    }

    private void startChild() {
        childStartTime = System.currentTimeMillis();
    }


    private static boolean processBuildResult(final Element buildResult, final Element compositeBuildResult,
                                              final String buildlogMsgPrefix, final Builder builder,
                                              final long childStartTime) {

        // add child builder info to build log
        insertBuildLogHeader(buildResult, buildlogMsgPrefix + " - " + builder.getClass().getName() + "; child",
                childStartTime, "composite", "composite-childbuilder");

        compositeBuildResult.addContent(buildResult.removeContent());

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
     * @param attribNameTarget value of name attribute of 'target' element.
     * @param attribNameTask value of name attribute of 'task' element.
     */
    @SuppressWarnings("unchecked")    
    public static void insertBuildLogHeader(final Element buildResult,
                                             final String buildLogMsg, final long childStartTime,
                                             final String attribNameTarget, final String attribNameTask) {

        // add info from attributes of "build" tag from child build
        String buildMsgWithAttibs = buildLogMsg + " build attributes: ";
        final Iterator<Attribute> attributes = (Iterator<Attribute>) buildResult.getAttributes().iterator();
        while (attributes.hasNext()) {
            final Attribute attribute = attributes.next();
            buildMsgWithAttibs += attribute.getName() + "=" + attribute.getValue() + "; ";
        }

        // @todo Rearrange these elements (even nesting childLog elements?), might display this info in reporting apps

        final Element target = new Element("target");
        target.setAttribute("name", attribNameTarget);
        target.setAttribute("time", DateUtil.getDurationAsString((System.currentTimeMillis() - childStartTime)));

        final Element task = new Element("task");
        task.setAttribute("name", attribNameTask);

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

    public Element build(final Map<String, String> properties, final Progress progressIn)
            throws CruiseControlException {

        final Progress progress = getShowProgress() ? progressIn : null;

        boolean errorOcurred = false;
        final Element compositeBuildResult = new Element("build");
        final Iterator<Builder> iter = builders.iterator();

        int i = 0;
        final int totalBuilders = builders.size();

        startBuild();
        while (iter.hasNext() & !errorOcurred & !isTimedOut) {

            i++;
            final String buildlogMsgPrefix = "composite build " + i + " of " + totalBuilders;
            if (progress != null) {
                progress.setValue(buildlogMsgPrefix);
            }

            final Builder builder = iter.next();
            startChild();
            final Element buildResult = builder.build(properties, progress);
            errorOcurred = processBuildResult(buildResult, compositeBuildResult,
                    buildlogMsgPrefix, builder, childStartTime);
            checkTimedOut();
        }
        endBuild(compositeBuildResult);

        return compositeBuildResult;
    }

    public Element buildWithTarget(final Map<String, String> properties, final String target, final Progress progressIn)
            throws CruiseControlException {

        final Progress progress = getShowProgress() ? progressIn : null;

        boolean errorOcurred = false;
        final Element compositeBuildResult = new Element("build");
        final Iterator<Builder> iter = builders.iterator();

        int i = 0;
        final int totalBuilders = builders.size();

        startBuild();
        while (iter.hasNext() & !errorOcurred & !isTimedOut) {

            i++;
            final String buildlogMsgPrefix = "composite build " + i + " of " + totalBuilders;
            if (progress != null) {
                progress.setValue(buildlogMsgPrefix);
            }

            final Builder builder = iter.next();
            startChild();
            final Element buildResult = builder.buildWithTarget(properties, target, progress);
            errorOcurred = processBuildResult(buildResult, compositeBuildResult,
                    buildlogMsgPrefix, builder, childStartTime);
            checkTimedOut();
        }
        endBuild(compositeBuildResult);

        return compositeBuildResult;
    }

    public void validate() throws CruiseControlException {

        ValidationHelper.assertFalse(builders.isEmpty(), "no builders added");
        super.validate();

        // validate all child builders
        for (final Builder builder : builders) {
            builder.validate();
        }
    }

    /** @return array of the builders in this composite. */
    public Builder[] getBuilders() {
        return builders.toArray(new Builder[builders.size()]);
    }


    /**
     * @param timeout The timeout (in seconds) to set.
     */
    @Description(
            "Composite Build will be halted if it continues longer than the specified "
            + "timeout. Value in seconds.")
    @Optional
    public void setTimeout(long timeout) {
        this.timeoutSeconds = timeout;
    }
    
    /** Method override to allow different @Description annotations. */
    @Description(
            "If true or omitted, the composite builder will provide progress messages, "
            + "as will any child builders that support this feature (assuming the child "
            + "builder's own showProgress setting is true). If false, no progress "
            + "messages will be shown by the composite builder or child builders - "
            + "regardless of child builder showProgress settings. If any parent "
            + "showProgress is false, then no progress will be shown, regardless of the "
            + "composite or child builder settings.")
    @Optional
    @Default("true")
    @Override
    public void setShowProgress(boolean show) {
        super.setShowProgress(show);
    }

    /** Method override to allow different @Description annotations. */
    @Description("Currently, the liveOutput setting has no effect on composite builders.")
    @Optional
    @Default("true")
    @Override
    public void setLiveOutput(boolean live) {
        super.setLiveOutput(live);
    }
}
