package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.Commandline;
import org.jdom.Element;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Used to execute a custom publishing command
 *
 * @author <a href="orenmnero@sourceforge.net">Oren Miller</a>
 */
public class ExecutePublisher implements Publisher {

    /** enable logging for this class */

    private static Logger log = Logger.getLogger(ExecutePublisher.class);

    private String _commandString;

    public void setCommand(String commandString) {
        _commandString = commandString;
    }

    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        if (_commandString == null) {
            throw new CruiseControlException("'command' not specified in configuration file");
        }
    }

    public void publish(Element cruisecontrolLog)
        throws CruiseControlException {

        Commandline command = new Commandline(_commandString);
        log.info("executing command: " + command);

        try {
            Runtime.getRuntime().exec(command.getCommandline());
        }
        catch (IOException e) {
            throw new CruiseControlException(e);
        }
    }

}
