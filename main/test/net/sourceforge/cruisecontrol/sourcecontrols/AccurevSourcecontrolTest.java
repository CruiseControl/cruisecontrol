/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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
/*
 * Created on 04-Jul-2005 by norru
 * 
 * Copyright (C) Sony Computer Entertainment Europe
 *               Studio Liverpool Server Group
 * 
 * Authors:
 *     Nicola Orru' <Nicola_Orru@scee.net>
 */
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommand;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommandline;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevInputParser;

public class AccurevSourcecontrolTest extends AccurevTest {
  public AccurevSourcecontrolTest() {
    super();
  }
  public class LineCollector implements AccurevInputParser {
    public List lines;
    public boolean parseStream(InputStream iStream) throws CruiseControlException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
      lines = new ArrayList();
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          lines.add(line);
        }
      } catch (IOException e) {
        throw new CruiseControlException("Error reading Accurev output");
      }
      return true;
    }
  }
  public void testAccurevSourcecontrol() throws CruiseControlException {
    fake("accurev_show_wspaces.txt", 0);
    fake("accurev_hist_lastbuild_now.txt", 0);
    Accurev accurev = new Accurev();
    accurev.setRunner(getMockRunner());
    accurev.setVerbose(true);
    accurev.setStream(getTestStreamName());
    accurev.validate();
    Calendar ago = new GregorianCalendar();
    ago.add(Calendar.MONTH, -1);
    List modifications = accurev.getModifications(ago.getTime(), new GregorianCalendar().getTime());
    accurev.getProperties();
    assertNotNull(modifications);
    assertTrue(modifications.size() == 6);
    assertEquals("Last Modified: 2005/06/22 10:53:10\nRevision: 120208\n"
        + "UserName: norru\nEmailAddress: null\nComment:  Comment\n\n", modifications.get(0).toString());
    assertEquals(
        "Last Modified: 2005/06/22 10:54:44\nRevision: 120209\nUserName: norru\nEmailAddress: null\nComment: \n",
        modifications.get(1).toString());
    assertEquals(
        "Last Modified: 2005/06/22 10:57:23\nRevision: 120210\nUserName: norru\nEmailAddress: null\nComment: \n",
        modifications.get(2).toString());
    assertEquals(
        "Last Modified: 2005/06/22 11:01:07\nRevision: 120214\nUserName: norru\nEmailAddress: null\nComment: \n",
        modifications.get(3).toString());
    assertEquals(
        "Last Modified: 2005/06/22 11:06:55\nRevision: 120216\nUserName: norru\nEmailAddress: null\nComment: \n",
        modifications.get(4).toString());
    assertEquals(
        "Last Modified: 2005/06/22 11:07:27\nRevision: 120217\nUserName: norru\nEmailAddress: null\nComment: \n",
        modifications.get(5).toString());
  }
  /**
   * Picks the last stream name from a list of streams
   */
  private String getTestStreamName() {
    LineCollector collector = new LineCollector();
    AccurevCommandline show = AccurevCommand.SHOW.create(getMockRunner());
    show.addArgument("wspaces");
    show.setInputParser(collector);
    show.run();
    assertNotNull(collector.lines);
    assertTrue(collector.lines.size() > 1);
    return collector.lines.get(collector.lines.size() - 1).toString().split("[ \t]")[0];
  }
  public void testValidate() {
    Accurev accurev = new Accurev();
    try {
      accurev.validate();
      fail("Validate should throw an exception when the stream is not set");
    } catch (CruiseControlException e) {
    }
  }
}
