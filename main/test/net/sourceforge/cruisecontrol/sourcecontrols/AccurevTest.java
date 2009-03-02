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
 * Created on 30-Jun-2005 by norru
 *
 * Copyright (C) Sony Computer Entertainment Europe
 *               Studio Liverpool Server Group
 * Licensed under the CruiseControl BSD license
 *
 * Authors:
 *     Nicola Orru' <Nicola_Orru@scee.net>
 */
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommand;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommandline;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevInputParser;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.DateTimespec;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.Timespec;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.TransactionNumberTimespec;
import net.sourceforge.cruisecontrol.util.Commandline;

/**
 * Basic test cases for the accurev command line utilities
 *
 * AccurevBootstrapper has its own test in
 * net.sourceforge.cruisecontrol.bootstrappers.AccurevBootstrapperTest
 *
 * Accurev sourcecontrol has its own test in
 * net.sourceforge.cruisecontrol.sourcecontrols.AccurevSourcecontrolTest
 *
 * @author <a href="mailto:Nicola_Orru@scee.net">Nicola Orru'</a>
 */
public class AccurevTest extends TestCase implements AccurevInputParser {
  private AccurevMockRunner mockRunner;
  public void setUp() {
    mockRunner = new AccurevMockRunner();
    mockRunner.setScriptRoot("net/sourceforge/cruisecontrol/sourcecontrols");
  }

  protected void tearDown() throws Exception {
    mockRunner = null;
  }

  /**
   * Tests common "accurev hist" commandline configurations
   */
  public void testCommandLineHist() {
    AccurevCommandline hist;
    fake("accurev_hist_now.txt", 0);
    fake("accurev_hist_now_highest.txt", 0);
    fake("accurev_blank.txt", 1);
    fake("accurev_blank.txt", 1);
    fake("accurev_hist_highest.txt", 0);
    fake("accurev_hist_1-now.txt", 0);
    fake("accurev_syntax_error.txt", 0);
    hist = AccurevCommand.HIST.create(getMockRunner());
    hist.setTransactionRange(DateTimespec.NOW);
    hist.run();
    assertTrue(hist.isSuccess());
    hist = AccurevCommand.HIST.create(getMockRunner());
    hist.setTransactionRange(DateTimespec.NOW, TransactionNumberTimespec.HIGHEST);
    hist.run();
    assertTrue(hist.isSuccess());
    hist = AccurevCommand.HIST.create(getMockRunner());
    hist.setTransactionRange(new TransactionNumberTimespec(0), DateTimespec.NOW);
    hist.run();
    assertFalse(hist.isSuccess());
    hist = AccurevCommand.HIST.create(getMockRunner());
    hist.setTransactionRange(new TransactionNumberTimespec(0));
    hist.run();
    assertFalse(hist.isSuccess());
    hist = AccurevCommand.HIST.create(getMockRunner());
    hist.setTransactionRange(TransactionNumberTimespec.HIGHEST);
    hist.run();
    assertTrue(hist.isSuccess());
    hist = AccurevCommand.HIST.create(getMockRunner());
    hist.setTransactionRange(new TransactionNumberTimespec(1), DateTimespec.NOW);
    hist.run();
    assertTrue(hist.isSuccess());
    try {
      AccurevCommand.HIST.create().setWorkspaceLocalPath("ThisDirectoryIsNotSupposedToExist");
      fail("setWorkspace should throw an exception when the workspace is not valid");
    } catch (CruiseControlException e) {
      // An exception must be thrown.
    }
    hist = AccurevCommand.HIST.create(getMockRunner());
    assertFalse(hist.isSuccess());
    AccurevCommand.HIST.create(getMockRunner());
    hist.addArgument("--thisoptiondoesnotexist");
    hist.run();
    assertFalse(hist.isSuccess());
  }
  /**
   * Checks the command line is built as expected
   */
  public void testCommandLineBuild() {
    Timespec d1 = new DateTimespec(-24);
    Timespec d2 = new DateTimespec(0);
    AccurevCommandline cmdKeep = AccurevCommand.KEEP.create();
    cmdKeep.selectModified();
    cmdKeep.setTransactionRange(d1, d2);
    cmdKeep.setComment("Automatic keep");
    assertEquals("accurev keep -m -t \"" + d1.toString() + "-" + d2.toString() + "\" -c \"Automatic keep\"",
        cmdKeep.toString());
    AccurevCommandline cmdHist = AccurevCommand.HIST.create();
    cmdHist.setTransactionRange(d1, d2);
    assertEquals("accurev hist -t \"" + d1.toString() + "-" + d2.toString() + "\"", cmdHist.toString());
    Commandline cmdUpdate = AccurevCommand.UPDATE.create();
    assertEquals("accurev update", cmdUpdate.toString());
    Commandline cmdSynctime = AccurevCommand.SYNCTIME.create();
    assertEquals("accurev synctime", cmdSynctime.toString());
  }
  /**
   * Tests common "accurev keep" commandline configuration
   */
  public void testCommandLineKeep() {
    fake("accurev_keep_nofiles.txt", 0);
    fake("accurev_keep.txt", 0);
    AccurevCommandline keep;
    keep = AccurevCommand.KEEP.create(getMockRunner());
    assertFalse(keep.isSuccess());
    keep = AccurevCommand.KEEP.create(getMockRunner());
    keep.selectModified();
    keep.setComment("Automatic keep");
    keep.setVerbose(true);
    keep.run();
    assertTrue(keep.isSuccess());
    keep = AccurevCommand.KEEP.create(getMockRunner());
    keep.selectModified();
    keep.setComment("Automatic keep");
    keep.setVerbose(true);
    keep.run();
    assertTrue(keep.isSuccess());
  }
  /**
   * Runs "accurev help" and looks for the support@accurev.com string. parseStream is defined as the
   * parsing callback
   */
  public void testCommandLineParse() {
    fake("accurev_help.txt", 0);
    AccurevCommandline help = AccurevCommand.HELP.create(getMockRunner());
    help.setInputParser(this);
    help.run();
    assertTrue(help.isSuccess());
  }
  /**
   * Helper for testCommandLineParse
   */
  public boolean parseStream(InputStream iStream) throws CruiseControlException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
    String line;
    boolean accurevSupportFound = false;
    try {
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("support@accurev.com") >= 0) {
          accurevSupportFound = true;
        }
      }
    } catch (IOException e) {
      throw new CruiseControlException("Error reading Accurev output");
    }
    return accurevSupportFound;
  }
  /**
   * Runs "accurev synctime"
   */
  public void testCommandLineSynctime() {
    fake("accurev_synctime.txt", 0);
    // you only have success after run
    AccurevCommandline synctime;
    synctime = AccurevCommand.SYNCTIME.create(getMockRunner());
    assertFalse(synctime.isSuccess());
    synctime = AccurevCommand.SYNCTIME.create(getMockRunner());
    synctime.run();
    assertTrue(synctime.isSuccess());
  }
  /**
   * Runs "accurev update" in the default workspace
   */
  public void testCommandLineUpdate() {
    fake("accurev_update.txt", 0);
    AccurevCommandline update;
    update = AccurevCommand.UPDATE.create(getMockRunner());
    assertFalse(update.isSuccess());
    update = AccurevCommand.UPDATE.create(getMockRunner());
    update.run();
    assertTrue(update.isSuccess());
  }
  protected AccurevMockRunner getMockRunner() {
    return mockRunner;
  }
  protected void setMockRunner(AccurevMockRunner mockRunner) {
    this.mockRunner = mockRunner;
  }
  public void fake(String path, int returnCode) {
    mockRunner.addScript(path, returnCode);
  }

}
