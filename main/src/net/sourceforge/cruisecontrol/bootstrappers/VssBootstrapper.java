package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import org.apache.log4j.Logger;
import java.io.File;
import java.io.*;


public class VssBootstrapper implements Bootstrapper {

  /** enable logging for this class */
  private static Logger log = Logger.getLogger(VssBootstrapper.class.getName());

  private String vssPath;
  private String localDirectory;
  private String login;

  public void bootstrap() throws CruiseControlException {
    String commandLine = generateCommandLine();

    try {
      Process p = Runtime.getRuntime().exec(commandLine);
      InputStream errorIn = p.getErrorStream();
      PrintWriter errorOut = new PrintWriter(System.err, true);
      StreamPumper errorPumper = new StreamPumper(errorIn, errorOut);
      new Thread(errorPumper).start();
      p.waitFor();
    }
    catch (IOException ex) {
      log.debug("exception trying to exec ss.exe", ex);
      throw new CruiseControlException(ex);
    }
    catch (InterruptedException ex) {
      log.debug("interrupted during get", ex);
      throw new CruiseControlException(ex);
    }
  }

  public void validate() throws CruiseControlException {
    if (vssPath == null || localDirectory == null) throw new CruiseControlException("VssBootstrapper has required attributes vssPath and filePath");
    File localDirForFile = new File(localDirectory);
    boolean dirExists = localDirForFile.exists();
    if (!dirExists) {
      log.debug("local directory [" + localDirectory + "] does not exist");
      throw new CruiseControlException("file path attribute value " + localDirectory + " must specify an existing directory.");
    }
    boolean isDir = localDirForFile.isDirectory();
    if (!isDir) {
      log.debug("local directory [" + localDirectory + "] is not a directory");
      throw new CruiseControlException("file path attribute value " + localDirectory + " must specify an existing directory, not a file.");
    }
    setLocalDirectory(localDirForFile.getAbsolutePath());
  }

  String generateCommandLine() {
    StringBuffer commandLine = new StringBuffer();
    final String QUOTE = "\"";
    commandLine.append("ss.exe get ");
    commandLine.append(QUOTE+this.vssPath+QUOTE);
    commandLine.append(" -GL");
    commandLine.append(QUOTE + this.localDirectory + QUOTE);
    commandLine.append(" -I-N");
    if (login != null) commandLine.append(" -Y" + login);
    return commandLine.toString();
  }

  /**
   * Required.
   * @param vssPath fully qualified VSS path to the file ($/Project/subproject/filename.ext)
   */
  public void setVssPath(String vssPath) {
    this.vssPath = vssPath;
  }

  /**
   * Required.
   * @param localDirectory fully qualified path for the destination directory (c:\directory\subdirectory\)
   */
  public void setLocalDirectory(String localDirectory) {
    this.localDirectory = localDirectory;
  }

  /**
   * Optional.
   * @param login vss login information in the form username,password\
   */
  public void setLogin(String login) {
    this.login = login;
  }
}