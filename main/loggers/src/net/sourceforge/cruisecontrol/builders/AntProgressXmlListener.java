package net.sourceforge.cruisecontrol.builders;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.XmlLogger;

import java.util.Vector;


/**
 * Ant XmlLogger impl (for use only as a Listener) that creates log.xml file for CC build results.
 * Sets its log level to match that of the logger {@link AntProgressXmlLogger}.
 * @author Dan Rollo
 * Date: Aug 8, 2007
 * Time: 10:21:52 PM
 */
public class AntProgressXmlListener extends XmlLogger {

    /**
     * Fired when the build starts, this builds the top-level element for the
     * document and remembers the time of the start of the build.
     *
     * @param event Ignored.
     */
    public void buildStarted(BuildEvent event) {
        super.buildStarted(event);

        final Vector buildListeners = event.getProject().getBuildListeners();
        BuildListener buildListener;
        for (int i = 0; i < buildListeners.size(); i++) {
            buildListener = (BuildListener) buildListeners.get(i);

            if (buildListener instanceof AntProgressXmlLogger) {
                setMessageOutputLevel(((AntProgressXmlLogger) buildListener).getMessageOutputLevel());
            }
        }
    }

}
