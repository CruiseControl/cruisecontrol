/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.*;

import java.text.*;
import java.util.*;
import net.sourceforge.cruisecontrol.Modification;

import org.apache.tools.ant.Task;
import org.apache.log4j.Category;

/**
 *  This class implements the SourceControlElement methods for a Clear Case
 *  repository.
 *
 * @author Thomas Leseney
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Eric Lefevre
 * @author Ralf Krakowski
 */
public class ClearCase extends SourceControlElement {

    /** enable logging for this class */
    private static Category log = Category.getInstance(ClearCase.class.getName());

  /**
   * Set of the authors that modified files. With Clear Case, it corresponds
     * to the user names.
   */
  private Set _emailNames = new HashSet();

  /**
   *  This date indicates the most recent modification time.
   */
  private Date _lastModified;

  /**
   *  The path of the clear case view
   */
  private String _viewPath;

  /**
   *  The branch to check for modifications
   */
  private String _branch = null;
  private boolean _recursive = true;

  /**
   *  Date format required by commands passed to Clear Case
   */
  final static SimpleDateFormat IN_DATE_FORMAT =
     new SimpleDateFormat("dd-MMMM-yyyy.HH:mm:ss");

  /**
   *  Date format returned in the output of Clear Case commands.
   */
  final static SimpleDateFormat OUT_DATE_FORMAT =
     new SimpleDateFormat("yyyyMMdd.HHmmss");

  /**
   *  Unlikely combinaison of characters to separate fields in a ClearCase query
   */
  final static String DELIMITER = "£~£";

  /**
   *  Even more unlikely combinaison of characters to indicate end of one line in query.
   * Carriage return (\n) can be used in comments and so is not available to us.
   */
  final static String END_OF_STRING_DELIMITER = "@#@#@#@#@#@#@#@#@#@#@#@";

  /**
   *  Sets the local working copy to use when making queries.
   *
   *@param  path
   */
  public void setViewpath(String path) {
    _viewPath = getAntTask().getProject().resolveFile(path).getAbsolutePath();
  }

  /**
   *  Sets the branch that we're concerned about checking files into.
   *
   *@param  branch
   */
  public void setBranch(String branch) {
    _branch = branch;
  }

  public void setRecursive(boolean b) {
    _recursive = b;
  }

  /**
   *  Returns a Set of email addresses. since Clear Case doesn't track actual
   *  email addresse, we just return the usernames, which may correspond to
   *  emails ids.
   *
   *@return
   */
  public Set getEmails() {
    return _emailNames;
  }

  /**
   *  Gets the last modified time for the set of files queried in the {@link
   *  #getHistory} method.
   *
   *@return  the lastest revision time.
   */
  public long getLastModified() {
    if (_lastModified == null) {
      return 0;
    }
    return _lastModified.getTime();
  }

  /**
   *  Returns an {@link java.util.List List} of {@link Modification}
   *  detailing all the changes between now and the last build.
   *
   *@param  lastBuild the last build time
   *@param  now time now, or time to check, NOT USED
   *@param  quietPeriod NOT USED
   *@return  the list of modifications, an empty (not null) list if no
   *      modifications.
   */
  public List getHistory(Date lastBuild, Date now, long quietPeriod) {
    String lastBuildDate = IN_DATE_FORMAT.format(lastBuild);
    /*
     * let's try a different clearcase command--this one just takes
         * waaaaaaaay too long.
     * String command = "cleartool find " + _viewPath +
     * " -type f -exec \"cleartool lshistory" +
     * " -since " + lastBuildDate;
     * if(_branch != null)
     * command += " -branch " + _branch;
     * command += " -nco" + // exclude check out events
     * " -fmt \\\" %u;%Nd;%n;%o \\n \\\" \\\"%CLEARCASE_XPN%\\\" \"";
     */
    String command = "cleartool lshistory";

        if (_branch != null) {
      command += " -branch " + _branch;
    }

        if (_recursive == true) {
      command += " -r ";
    }

        command += " -nco -since " + lastBuildDate;
    command += " -fmt \"%u"+DELIMITER+"%Nd"+DELIMITER+"%n"+DELIMITER+"%o"+DELIMITER+"%Nc"+END_OF_STRING_DELIMITER+"\\n\" " + _viewPath;

    log.debug("Command to execute : " + command);
        List modifications = null;
    try {
      Process p = Runtime.getRuntime().exec(command);

      StreamPumper errorPumper = new StreamPumper(p.getErrorStream());
      new Thread(errorPumper).start();

      InputStream input = p.getInputStream();
      modifications = parseStream(input);

      p.waitFor();
    } catch (Exception e) {
      log.error("Error in executing the Clear Case command : ", e);
    }

    if (modifications == null) {
      modifications = new ArrayList();
    }

    return modifications;
  }

  /**
   *  Parses the input stream to construct the modifications list.
   * Package-private to make it available to the unit test.
   *
   *@param  input the stream to parse
   *@return  a list of modification elements
   *@exception  IOException
   */
  List parseStream(InputStream input) throws IOException {
    ArrayList modifications = new ArrayList();
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

    String line;
    String lines = "";

    while ((line = reader.readLine()) != null) {
      if ( !lines.equals("") && !lines.endsWith(" ") && !line.startsWith(" ")) {
        lines += " ";
      }
      lines += line;
      Modification mod = null;
      if ( lines.indexOf(END_OF_STRING_DELIMITER)>-1 )
      {
        mod = parseEntry(lines.substring(0, lines.indexOf(END_OF_STRING_DELIMITER)));
        lines = "";
      }
      if (mod != null) {
        modifications.add(mod);
      }
    }
    return modifications;
  }

  /**
   *  Parses a single line from the reader. Each line contains a signe revision
   *  with the format : <br>
   *  username££date_of_revision££element_name££operation_type££comments <br>
   *
   *
   *@param  line the line to parse
   *@return  a modification element corresponding to the given line
   */
  private Modification parseEntry(String line) {
    System.out.println("parsing entry: " + line);
    StringTokenizer st = new StringTokenizer(line, DELIMITER);

    // we should get either 4 (w/o comments) or 5 tokens (w/ comments)
    if ( (st.countTokens()<4) || (st.countTokens()>5) ) {
      return null;
    }
    String username = st.nextToken().trim();
    String timeStamp = st.nextToken().trim();
    String elementName = st.nextToken().trim();
    String operationType = st.nextToken().trim();

    String comment;
    if ( st.countTokens() > 0 ) {
      comment = st.nextToken().trim();
    }
    else {
      comment = "";
    }
    /*
     *  a branch event shouldn't trigger a build
     */
    if (operationType.equals("mkbranch")) {
      return null;
    }

    Modification mod = new Modification();

    mod.userName = username;
    _emailNames.add(mod.userName);

    elementName = elementName.substring(elementName.indexOf(File.separator));
    String fileName = elementName.substring(0, elementName.indexOf("@@"));

    mod.fileName = fileName.substring(fileName.lastIndexOf(File.separator));
    mod.folderName = fileName.substring(0, fileName.lastIndexOf(File.separator));

    try {
      mod.modifiedTime = OUT_DATE_FORMAT.parse(timeStamp);
      updateLastModified(mod.modifiedTime);
    } catch (ParseException e) {
      mod.modifiedTime = null;
    }

    mod.type = operationType;

    mod.comment = comment;

    return mod;
  }

  /**
   *  Updates the lastModified date if necessary (new date is after the current
   *  lastModified date).
   *
   *@param  newPossible the new possible lastModified date
   */
  private void updateLastModified(Date newPossible) {
    if (_lastModified == null || _lastModified.before(newPossible)) {
      _lastModified = newPossible;
    }
  }

  /**
   *  Inner class to pump the error stream during Process's runtime. Copied from
   *  the Ant built-in task.
   */
  class StreamPumper implements Runnable {

    private InputStream _in;
    private final static int SIZE = 128;
    private final static int SLEEP = 5;

    public StreamPumper(InputStream in) {
      _in = in;
    }

    public void run() {
      final byte[] buf = new byte[SIZE];
      int length;

      try {
        while ((length = _in.read(buf)) > 0) {
          System.err.write(buf, 0, length);
          try {
            Thread.sleep(SLEEP);
          }
          catch (InterruptedException ignoredInterrupted) { }
        }
      } catch (IOException ignoredIOE) { }
    }
  }
}
