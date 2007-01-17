/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
/*
 * Created on 04-Aug-2005 by norru
 *
 * Copyright (C) Sony Computer Entertainment Europe
 *               Studio Liverpool Server Group
 *
 * Authors:
 *     Nicola Orru' <Nicola_Orru@scee.net>
 */
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevInputParser;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.Runner;
import org.apache.log4j.Logger;

class Script {
  private String path;
  private int    returnCode;
  public Script(String path, int returnCode) {
    this.path = path;
    this.returnCode = returnCode;
  }
  public String getPath() {
    return path;
  }
  public void setPath(String path) {
    this.path = path;
  }
  public int getReturnCode() {
    return returnCode;
  }
  public void setReturnCode(int returnCode) {
    this.returnCode = returnCode;
  }
}

public class AccurevMockRunner implements Runner {
  public static final Logger LOG = Logger.getLogger(AccurevMockRunner.class);
  private int                returnCode;
  private List               scriptList;
  private Iterator           scriptIterator;
  private String             scriptRoot;
  public AccurevMockRunner() {
    scriptList = new LinkedList();
  }
  public void setScriptRoot(String scriptRoot) {
    this.scriptRoot = scriptRoot;
  }
  public void addScript(String path, int rCode) {
    if (scriptRoot != null) {
      path = scriptRoot + "/" + path;
    }
    Script script = new Script(path, rCode);
    scriptList.add(script);
  }
  private Script nextScript() {
    if (scriptList.size() == 0) { return null; }
    if (scriptIterator == null || !scriptIterator.hasNext()) {
      scriptIterator = scriptList.iterator();
    }
    return (Script) scriptIterator.next();
  }
  public boolean execute(AccurevInputParser inputParser) {
    boolean syntaxError = false;
    try {
      Script script = nextScript();
      if (script == null) { throw new IOException("Script list empty"); }
      LOG.info("Scripted execution, reading input from resource " + script.getPath());
      InputStream fakeFile = getClass().getClassLoader().getResourceAsStream(script.getPath());
      if (fakeFile == null) { throw new IOException("Script not found: " + script.getPath()); }
      try {
        if (inputParser != null) {
          syntaxError = !inputParser.parseStream(fakeFile);
        }
        this.returnCode = script.getReturnCode();
      } finally {
        fakeFile.close();
      }
    } catch (IOException e) {
      LOG.error(e);
      throw new RuntimeException(e.getMessage());
    } catch (CruiseControlException e) {
      LOG.error(e);
      throw new RuntimeException(e.getMessage());
    }
    return syntaxError;
  }
  public int getReturnCode() {
    return returnCode;
  }
}
