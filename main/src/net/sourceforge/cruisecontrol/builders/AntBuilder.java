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

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.util.*;

/**
 *  we often see builds that fail because the previous build is still holding on to some resource.
 *  we can avoid this by just building in a different process which will completely die after every
 *  build.
 */
public class AntBuilder extends Builder {

    /** enable logging for this class */
    private static Category log = Category.getInstance(AntBuilder.class.getName());

    private String _buildFile;
    private String _target;

    /**
     *  build and return the results via xml.  debug status can be determined from log4j category once we
     *  get all the logging in place.
     */
    public Element build(Map buildProperties) {

        try {
            Process p = Runtime.getRuntime().exec(getCommandLineArgs(buildProperties));
            StreamPumper errorPumper = new StreamPumper(p.getErrorStream());
            StreamPumper outPumper = new StreamPumper(p.getInputStream());
            new Thread(errorPumper).start();
            new Thread(outPumper).start();
            InputStream input = p.getInputStream();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //read in log file as element, return it
        File log = new File("log.xml");
        Element buildLogElement = getAntLogAsElement(log);
        log.delete();

        //also read in this file, which has all of the ant properties defined.
        Element propertiesElement = null;
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            propertiesElement = builder.build("propertylogger.xml").getRootElement();
        } catch (Exception ee) {
            ee.printStackTrace();
        }

        buildLogElement.addContent(propertiesElement.detach());
        File propertiesLog = new File("propertylogger.xml");
        propertiesLog.delete();

        return buildLogElement;
    }

    public void setTarget(String target) {
        _target = target;
    }

    public void setBuildFile(String buildFile) {
        _buildFile = buildFile;
    }

    /**
     *  construct the command that we're going to execute.
     *  @param buildProperties Map holding key/value pairs of arguments to the build process
     *  @return String[] holding command to be executed
     */
    protected String[] getCommandLineArgs(Map buildProperties) {
        List al = new ArrayList();
        al.add("java");
        al.add("-classpath");
        al.add(System.getProperty("java.class.path"));
        al.add("org.apache.tools.ant.Main");
        al.add("-listener");
        al.add("org.apache.tools.ant.XmlLogger");
        al.add("-listener");
        al.add("net.sourceforge.cruisecontrol.builders.PropertyLogger");

        Iterator propertiesIterator = buildProperties.keySet().iterator();
        while (propertiesIterator.hasNext()) {
            String key = (String) propertiesIterator.next();
            al.add("-D" + key + "=" + buildProperties.get(key));
        }

        if (log.isDebugEnabled()) {
            al.add("-debug");
            al.add("-verbose");
        }

        al.add("-buildfile");
        al.add(_buildFile);
        al.add(_target);

        StringBuffer sb = new StringBuffer();
        sb.append("Executing Command: ");
        Iterator argIterator = al.iterator();
        while (argIterator.hasNext()) {
            String arg = (String) argIterator.next();
            sb.append(arg);
            sb.append(" ");
        }
        log.debug(sb.toString());

        return (String[]) al.toArray(new String[0]);
    }

    /**
     *  JDOM doesn't like the <?xml:stylesheet ?> tag.  we don't need it, so we'll skip it.
     *  TO DO: make sure that we are only skipping this string and not something else
     */
    protected static Element getAntLogAsElement(File f) {
        try {
            FileReader fr = new FileReader(f);
            StringBuffer sb = new StringBuffer();
            for(int i=0; i<150; i++) {
                sb.append((char) fr.read());
            }
            String beginning = sb.toString();
            int skip = beginning.lastIndexOf("<build");

            BufferedInputStream bufferedStream = new BufferedInputStream(new FileInputStream(f));
            bufferedStream.skip(skip);
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            return builder.build(bufferedStream).getRootElement();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return null;
    }
}