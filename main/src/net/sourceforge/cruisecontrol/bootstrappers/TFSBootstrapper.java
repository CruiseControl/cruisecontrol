/**
 *
 */
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * Bootstrapper for Microsoft Visual Studio Team Foundation Server.
 *
 * The TFS Bootstrapper will perform a get latest for a single ItemSpec before the build process is run.
 *
 * This relies on there being an existing TFS workspace and a working folder mapping existing between the local
 * bootstrap location and the server location for that
 *
 * @author <a href="http://www.woodwardweb.com">Martin Woodward</a>
 */
public class TFSBootstrapper implements Bootstrapper {
    private static final Logger LOG = Logger.getLogger(TFSBootstrapper.class);

    /** Configuration parameters */

    private String itemSpec;
    private String username;
    private String password;
    private String tfPath = "tf";
    private String options;
    private boolean recursive = false;
    private boolean force = false;

    /**
     * @see net.sourceforge.cruisecontrol.Bootstrapper#bootstrap()
     */
    public void bootstrap() throws CruiseControlException {
        buildGetCommand().executeAndWait(LOG);
    }

    /**
     * Generate the tf get command in the format
     *
     * tf get -noprompt c:\cc\projects\connectfour\build.xml -recursive -login:DOMAIN\name,password
     *
     * For more details on get command syntax see
     *
     * <a href="http://msdn2.microsoft.com/en-us/library/fx7sdeyf(VS.80).aspx">
     * http://msdn2.microsoft.com/en-us/library/fx7sdeyf(VS.80).aspx </a>
     *
     * @return a tfs get CommandLine instance
     * @throws CruiseControlException
     */
    protected Commandline buildGetCommand() throws CruiseControlException {
        Commandline command = new Commandline();
        command.setExecutable(tfPath);
        command.createArgument().setValue("get");
        command.createArgument().setValue("-noprompt");
        command.createArgument().setValue(itemSpec);
        if (recursive) {
            command.createArgument().setValue("-recursive");
        }
        if (force) {
            command.createArgument().setValue("-force");
        }
        if (username != null && password != null) {
            command.createArgument().setValue("-login:" + username + "," + password + "");
        }
        if (options != null) {
            command.createArgument().setValue(options);
        }

        LOG.debug("Executing command: " + command);
        return command;
    }

    /**
     * @see net.sourceforge.cruisecontrol.Bootstrapper#validate()
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(itemSpec, "itemSpec", this.getClass());
    }

    // --- Property setters

    /**
     * Mandatory The path to issue a get for
     *
     * @param itemSpec
     */
    public void setItemSpec(String itemSpec) {
        this.itemSpec = itemSpec;
    }

    /**
     * The username to use when talking to TFS. This should be in the format DOMAIN\name or name@DOMAIN if the domain
     * portion is required. Note that name@DOMAIN is the easiest format to use from Unix based systems. If the username
     * contains characters likely to cause problems when passed to the command line then they can be escaped in quotes
     * by passing the following into the config.xml:- <code>&amp;quot;name&amp;quot;</code>
     *
     * If the username or password is not supplied, then none will be passed to the command. On windows system using the
     * Microsoft tf.exe command line client, the credential of that the CruiseControl process is running as will be used
     * for the connection to the server.
     *
     * @param username
     *            the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * If the username or password is not supplied, then none will be passed to the command. On windows system using the
     * Microsoft tf.exe command line client, the credential of that the CruiseControl process is running as will be used
     * for the connection to the server.
     *
     * @param password
     *            the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * The path to the tf command. Either the &quot;tf.exe&quot; command provided by Microsoft in the <a
     * href="http://download.microsoft.com/download/2/a/d/2ad44873-8ccb-4a1b-9c0d-23224b3ba34c/VSTFClient.img"> Team
     * Explorer Client</a> can be used or the &quot;tf&quot; command line client provided by <a
     * href="http://www.teamprise.com">Teamprise</a> can be used. The Teamprise client works cross-platform. Both
     * clients are free to use provided the developers using CruiseControl have a TFS Client Access License (and in the
     * case of Teamprise a license to the Teamprise command line client).
     *
     * If not supplied then the command "tf" will be called and CruiseControl will rely on that command being able to be
     * found in the path.
     *
     * @param tfPath
     *            the path where the tf command resides
     */
    public void setTfPath(String tfPath) {
        this.tfPath = tfPath;
    }

    /**
     * Flag to indicate if the tf get should be performed recursively or not. In the usual bootstrapper scenario, the
     * bootstrapper would be located in a single file (build.xml) or at one level in the bootstrapper directory.
     * Therefore, if not passed, recursive will default to false, i.e. not recursive.
     *
     * @param recursive
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Flag to indicate of the tf get command should be performed using the /force switch. By default, TFS will only
     * download files that the server thinks have changed since the last time you told it you were modifying or geting
     * files into your local TFS workspace. It will also not overwrite locally writable files. Setting the force option
     * will make TFS always download the files and overwrite any that happen to be locally writable - however this has
     * the expense of significantly increasing the network traffice and increaing the time to perform the bootstrap
     * process.
     *
     * @param force
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * An optional argument to add to the end of the tf get command that is generated.
     *
     * @param options
     *            the options to set
     */
    public void setOptions(String options) {
        this.options = options;
    }

}
