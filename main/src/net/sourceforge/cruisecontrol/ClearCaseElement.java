package net.sourceforge.cruisecontrol;

import java.text.*;
import java.util.*;
import java.io.*;

import org.apache.tools.ant.Task;

/**
 * This class implements the SourceControlElement methods for a Clear Case repository.
 *
 * @author  Thomas Leseney
 * @version April 23, 2001
 */

public class ClearCaseElement implements SourceControlElement {

  /**
   * Date format required by commands passed to Clear Case
   */
  public SimpleDateFormat _inDateFormat  = new SimpleDateFormat("dd-MMMM-yyyy.HH:mm:ss");

  /**
   * Date format returned in the output of Clear Case commands.
   */
  public SimpleDateFormat _outDateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss");

  /**
   * Set of the authors that modified files.
   * With Clear Case, it correspond the user names.
   */
  private Set _emailNames = new HashSet();

  /**
   * This date indicates the most recent modification time.
   */
  private Date _lastModified;

  /**
   * The task shoulb be provided by the caller.
   */
  private Task _task;

  /**
   * The path of the clear case view
   */
  private String _viewPath;

  /**
   * The branch to check for modifications
   */
  private String _branch = null;
  private boolean _recursive = true;
  /**
   * Allows the caller to set the task containing this element.
   */
  public void setTask(Task task) {
    _task = task;
  }

  /**
   * Returns a Set of email addresses. since Clear Case doesn't track actual
   * email addresse, we just return the usernames, which may correspond to emails ids.
   */
  public Set getEmails() {
    return _emailNames;
  }

  /**
   * Gets the last modified time for the set of files queried
   * in the {@link #getHistory} method.
   *
   * @return  the lastest revision time.
   */
  public long getLastModified() {
    if (_lastModified == null) {
      return 0;
    }
    return _lastModified.getTime();
  }

  /**
   * Sets the local working copy to use when making queries.
   */
  public void setViewpath(String path) {
    _viewPath = _task.getProject().resolveFile(path).getAbsolutePath();
  }

  /**
   * Sets the branch that we're concerned about checking files into.
   */
  public void setBranch(String branch) {
  	_branch = branch;
  }

  public void setRecursive(boolean b) {
  	_recursive = b;
  }

  /**
   * Logs a message
   */
  private void log(String message) {
    _task.getProject().log(message);
  }

  /**
   * Returns an {@link java.util.ArrayList ArrayList} of {@link Modification} detailing
   * all the changes between now and the last build.
   *
   * @param   lastBuild   the last build time
   * @param   now         time now, or time to check, NOT USED
   * @param   quietPeriod NOT USED
   *
   * @return  the list of modifications, an empty (not null) list if no modifications.
   */
  public ArrayList getHistory(Date lastBuild, Date now, long quietPeriod) {
    ArrayList modifications = null;
    String lastBuildDate = _inDateFormat.format(lastBuild);
    /* let's try a different clearcase command--this one just takes waaaaaaaay too long.
    String command = "cleartool find " + _viewPath +
      " -type f -exec \"cleartool lshistory" +
      " -since " + lastBuildDate;

      if(_branch != null)
         command += " -branch " + _branch;

      command += " -nco" + // exclude check out events
      " -fmt \\\" %u;%Nd;%n;%o \\n \\\" \\\"%CLEARCASE_XPN%\\\" \"";
    */

    String command = "cleartool lshistory";
    if(_branch != null)
       command += " -branch " + _branch;
    if(_recursive == true)
       command += " -r ";
    command += " -nco -since " + lastBuildDate;
    command += " -fmt \"%u;%Nd;%n;%o\\n\" " + _viewPath;

    log("Command to execute : " + command);
    try {
      Process p = Runtime.getRuntime().exec(command);

      StreamPumper errorPumper = new StreamPumper(p.getErrorStream());
      new Thread(errorPumper).start();

      InputStream input = p.getInputStream();
      modifications = parseStream(input);

      p.waitFor();
    } catch (Exception e) {
      log("Error in executing the Clear Case command : " + e);
      e.printStackTrace();
    }
    if (modifications == null) {
      modifications = new ArrayList();
    }
    return modifications;
  }

  /**
   * Parses the input stream to construct the modifications list.
   *
   * @param   input   the stream to parse
   * @return  a list of modification elements
   */
  private ArrayList parseStream(InputStream input)
    throws IOException
  {
    ArrayList modifications = new ArrayList();
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

    String line;

    while ( (line = reader.readLine()) != null) {
      Modification mod = parseEntry(line);
      if (mod != null) {
	modifications.add(mod);
      }
    }
    return modifications;
  }

  /**
   * Parses a single line from the reader.
   * Each line contains a signe revision with the format :
   * <br> username;date_of_revision;element_name;operation_type <br>
   *
   * @param   line    the line to parse
   * @return  a modification element corresponding to the given line
   */
  private Modification parseEntry(String line) {
	System.out.println("parsing entry: " + line);
    StringTokenizer st = new StringTokenizer(line, ";");
    if (st.countTokens() != 4) {
      return null;
    }
    String username = st.nextToken().trim();
    String timeStamp = st.nextToken().trim();
    String elementName = st.nextToken().trim();
    String operationType = st.nextToken().trim();

    /* a branch event shouldn't trigger a build */
    if(operationType.equals("mkbranch")) {
		return null;
	}

    Modification mod = new Modification();

    mod.userName = username;
    _emailNames.add(mod.userName);

    elementName = elementName.substring(elementName.indexOf(":\\") + 1);
    String fileName = elementName.substring(0, elementName.indexOf("@@"));

    mod.fileName = fileName.substring(fileName.lastIndexOf("\\"));
    mod.folderName = fileName.substring(0, fileName.lastIndexOf("\\"));


    try {
      mod.modifiedTime = _outDateFormat.parse(timeStamp);
      updateLastModified(mod.modifiedTime);
    } catch (ParseException e) {
      mod.modifiedTime = null;
    }

    mod.type = operationType;

    mod.comment = "";

    return mod;
  }

  /**
   * Updates the lastModified date if necessary (new date is after the current
   * lastModified date).
   *
   * @param   newPossible   the new possible lastModified date
   */
  private void updateLastModified(Date newPossible) {
    if (_lastModified == null || _lastModified.before(newPossible)) {
      _lastModified = newPossible;
    }
  }

  /**
   * Inner class to pump the error stream during Process's runtime.
   * Copied from the Ant built-in task.
   */
  class StreamPumper implements Runnable {
    private static final int SIZE = 128;
    private static final int SLEEP = 5;

    private InputStream _in;

    public StreamPumper(InputStream in) {
      _in = in;
    }

    public void run() {
      final byte[] buf = new byte[SIZE];
      int length;

      try {
	while ((length = _in.read(buf)) > 0) {
	  System.err.write(buf,0,length);
	  try {
	    Thread.sleep(SLEEP);
	  } catch (InterruptedException e) {}
        }
      } catch(IOException e) {}
    }
  }
}