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

package net.sourceforge.cruisecontrol.builders;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.log4j.*;
import org.jdom.Element;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

public class AntBuilderTest extends TestCase {

    /** enable logging for this class */
    private static Category log = Category.getInstance(AntBuilderTest.class.getName());

    public AntBuilderTest(String name) {
        super(name);
    }

    public void testGetCommandLineArgs() {
        AntBuilder builder = new AntBuilder();
        builder.setTarget("target");
        builder.setBuildFile("buildfile");
        Hashtable properties = new Hashtable();
        properties.put("label", "200.1.23");
        String[] resultDebug = { "java","org.apache.tools.ant.Main","-listener","org.apache.tools.ant.XmlLogger","-listener","net.sourceforge.cruisecontrol.builders.PropertyLogger","-Dlabel=200.1.23","-debug","-verbose","-buildfile","buildfile","target"};
        String[] resultInfo = { "java","org.apache.tools.ant.Main","-listener","org.apache.tools.ant.XmlLogger","-listener","net.sourceforge.cruisecontrol.builders.PropertyLogger","-Dlabel=200.1.23","-buildfile","buildfile","target"};
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%m%n")));

        log.getRoot().setPriority(Priority.INFO);
        Assert.assertTrue(Arrays.equals(resultInfo, builder.getCommandLineArgs(properties)));

        log.getRoot().setPriority(Priority.DEBUG);
        Assert.assertTrue(Arrays.equals(resultDebug, builder.getCommandLineArgs(properties)));
    }

    public void testGetAntLogAsElement() {
        try {
            Element buildLogElement = new Element("build");
            File logFile = new File("_tempAntLog.xml");
            BufferedWriter bw1 = new BufferedWriter(new FileWriter(logFile));
            bw1.write("<?xml:stylesheet type=\"text/xsl\" href=\"log.xsl\"?><build></build>");
            bw1.flush();
            bw1.close();
            AntBuilder builder = new AntBuilder();
            Assert.assertEquals(buildLogElement.toString(), builder.getAntLogAsElement(logFile).toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
