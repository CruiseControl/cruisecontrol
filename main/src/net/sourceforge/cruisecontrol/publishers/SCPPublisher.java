package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Commandline.Argument;
import org.jdom.DataConversionException;
import org.jdom.Element;
import java.io.IOException;
import java.io.File;
import org.apache.log4j.Logger;

/**
 * Used to scp a file to a remote location
 *
 * @author <a href="orenmnero@sourceforge.net">Oren Miller</a>
 */
public class SCPPublisher implements Publisher {
    /** enable logging for this class */
    private static Logger log = Logger.getLogger(
                                    SCPPublisher.class);

    private String _sourceuser = null;
    private String _sourcehost = null;
    private String _sourcedir = ".";
    private String _targetuser = null;
    private String _targethost = null;
    private String _targetdir = ".";
    private String _ssh = "ssh";
    private String _options = null;
    private String _file = null;
    private String _targetseparator = File.separator;

    public void setSourceUser(String sourceuser) {
        _sourceuser = sourceuser;
    }
    public void setSourceHost(String sourcehost) {
        _sourcehost = sourcehost;
    }
    public void setSourceDir(String sourcedir) {
        _sourcedir = sourcedir;
    }
    public void setTargetUser(String targetuser) {
        _targetuser = targetuser;
    }
    public void setTargetHost(String targethost) {
        _targethost = targethost;
    }
    public void setTargetDir(String targetdir) {
        _targetdir = targetdir;
    }
    public void setSSH(String ssh) {
        _ssh = ssh;
    }
    public void setOptions(String options) {
        _options = options;
    }
    public void setFile(String file) {
        _file = file;
    }
    public void setTargetSeparator(String targetseparator) {
        _targetseparator = targetseparator;
    }

    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        if(_sourceuser == null) {
            if(_sourcehost != null) {
                throw new CruiseControlException("'sourceuser' not specified in configuration file");
            }
        }
        if(_sourcehost == null) {
            if(_sourceuser != null) {
                throw new CruiseControlException("'sourcehost' not specified in configuration file");
            }
        }
        if(_targetuser == null) {
            if(_targethost != null) {
                throw new CruiseControlException("'targetuser' not specified in configuration file");
            }
        }
        if(_targethost == null) {
            if(_targetuser != null) {
                throw new CruiseControlException("'targethost' not specified in configuration file");
            }
        }
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        if(_file == null) {
            XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
            _file = helper.getLogFileName().substring(1);
        }

        Commandline command = createCommandline(_file);
        log.info("executing command: " + command);
        try {
            Process p = Runtime.getRuntime().exec(command.getCommandline());
        } catch( IOException e ) {
            throw new CruiseControlException(e);
        }
    }

    public Commandline createCommandline(String file) {
        String sourcefile = File.separator + file;
        String targetfile = _targetseparator + file;

        Commandline command = new Commandline();
        command.setExecutable("scp");
        command.createArgument().setLine(_options);
        command.createArgument().setValue("-S");
        command.createArgument().setValue(_ssh);
        createFileArgument(command.createArgument(),_sourceuser, _sourcehost, _sourcedir, sourcefile);
        createFileArgument(command.createArgument(),_targetuser, _targethost, _targetdir, targetfile);
        return command;
    }

    public void createFileArgument(Argument arg, String user, String host, String dir, String file) {
        String argValue = new String();
        if(user != null && host != null) {
            argValue = user + "@" + host + ":";
        }

        argValue += dir;
        argValue += file;
        arg.setValue(argValue);
    }
}
