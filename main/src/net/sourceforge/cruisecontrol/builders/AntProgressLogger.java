package net.sourceforge.cruisecontrol.builders;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;

/**
 * Ant Logger impl used to send progress messages back to an AntBuilder.
 * Ideas lifted from contrib/XmlLoggerWithStatus, written by IgorSemenko (igor@semenko.com).
 * @author Dan Rollo
 * Date: Aug 8, 2007
 * Time: 10:21:52 PM
 */
public class AntProgressLogger extends DefaultLogger {
    static final String MSG_PREFIX_PROGRESS_LOG = "msgPrefixProgressLog--";

    public void targetStarted(BuildEvent event) {
        if (Project.MSG_INFO <= msgOutputLevel
                && !event.getTarget().getName().equals("")) {

            final String name = event.getTarget().getName();

//        if (this.targetFilter != null && name.matches(this.targetFilter)){
//            return;
//        }

            final String msg = MSG_PREFIX_PROGRESS_LOG + name;
            printMessage(msg, out, event.getPriority());
            log(msg);
        }
    }


    public void buildFinished(BuildEvent event) {
        // no-op to reduce traffic
    }

    public void messageLogged(BuildEvent event) {
        // no-op to reduce traffic
    }
}
