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

import junit.framework.TestCase;
import org.apache.log4j.*;
import org.jdom.Element;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

public class AntBuilderTest extends TestCase {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(AntBuilderTest.class);

    public AntBuilderTest(String name) {
        super(name);
    }

    public void testGetCommandLineArgs() {
        AntBuilder builder = new AntBuilder();
        builder.setTarget("target");
        builder.setBuildFile("buildfile");
        Hashtable properties = new Hashtable();
        properties.put("label", "200.1.23");
        String classpath = System.getProperty("java.class.path");
        String[] resultDebug = {"java", "-classpath", classpath, "org.apache.tools.ant.Main", "-listener", "org.apache.tools.ant.XmlLogger", "-listener", "net.sourceforge.cruisecontrol.builders.PropertyLogger", "-Dlabel=200.1.23", "-debug", "-verbose", "-buildfile", "buildfile", "target"};
        String[] resultInfo = {"java", "-classpath", classpath, "org.apache.tools.ant.Main", "-listener", "org.apache.tools.ant.XmlLogger", "-listener", "net.sourceforge.cruisecontrol.builders.PropertyLogger", "-Dlabel=200.1.23", "-buildfile", "buildfile", "target"};
        String[] resultDebugWithMaxMemory = {"java", "-Xmx256m", "-classpath", classpath, "org.apache.tools.ant.Main", "-listener", "org.apache.tools.ant.XmlLogger", "-listener", "net.sourceforge.cruisecontrol.builders.PropertyLogger", "-Dlabel=200.1.23", "-debug", "-verbose", "-buildfile", "buildfile", "target"};
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%m%n")));

        log.getRoot().setPriority(Priority.INFO);
        assertTrue(Arrays.equals(resultInfo, builder.getCommandLineArgs(properties)));

        log.getRoot().setPriority(Priority.DEBUG);
        assertTrue(Arrays.equals(resultDebug, builder.getCommandLineArgs(properties)));

        AntBuilder.JVMArg arg = (AntBuilder.JVMArg) builder.createJVMArg();
        arg.setArg("-Xmx256m");
        assertTrue(Arrays.equals(resultDebugWithMaxMemory, builder.getCommandLineArgs(properties)));
    }

    public void testGetAntLogAsElement() throws Exception {
        try {
            Element buildLogElement = new Element("build");
            File logFile = new File("_tempAntLog14.xml");
            BufferedWriter bw1 = new BufferedWriter(new FileWriter(logFile));
            bw1.write("<?xml:stylesheet type=\"text/xsl\" href=\"log.xsl\"?><build></build>");
            bw1.flush();
            bw1.close();
            File logFile2 = new File("_tempAntLog141.xml");
            BufferedWriter bw2 = new BufferedWriter(new FileWriter(logFile2));
            bw2.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><?xml:stylesheet type=\"text/xsl\" href=\"log.xsl\"?><build></build>");
            bw2.flush();
            bw2.close();

            AntBuilder builder = new AntBuilder();
            assertEquals(buildLogElement.toString(), builder.getAntLogAsElement(logFile).toString());
            assertEquals(buildLogElement.toString(), builder.getAntLogAsElement(logFile2).toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void testBuild() throws Exception {
        AntBuilder builder = new AntBuilder();
        builder.setBuildFile("build.xml");
        builder.setTarget("init");
        HashMap buildProperties = new HashMap();
        Element buildElement = builder.build(buildProperties);

        Iterator targetIterator = buildElement.getChildren("target").iterator();
        while (targetIterator.hasNext()) {
            Element targetElement = (Element) targetIterator.next();
            if (targetElement.getAttributeValue("name").equals("init")) {
                assertTrue(true);
            }
        }

        Element propertiesElement = buildElement.getChild("properties");
        Iterator propertyIterator = propertiesElement.getChildren("property").iterator();
        while (propertyIterator.hasNext()) {
            Element propertyElement = (Element) propertyIterator.next();
            if (propertyElement.getAttributeValue("name").equals("src")) {
                assertEquals("src", propertyElement.getAttributeValue("value"));
            }
        }
    }
}
