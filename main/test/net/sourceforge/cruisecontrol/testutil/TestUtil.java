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

package net.sourceforge.cruisecontrol.testutil;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import junit.framework.Assert;
import net.sourceforge.cruisecontrol.util.IO;

import org.jdom.Document;
import org.jdom.Element;

public final class TestUtil {

    public static class FilesToDelete {
        private List files = new Vector();
    
        public void add(File file) {
            files.add(file);
        }
    
        public void delete() {
            Iterator fileIterator = files.iterator();
            while (fileIterator.hasNext()) {
                File file = (File) fileIterator.next();
                IO.delete(file);
            }
            files.clear();
        }
    }

    private TestUtil() {

    }

    public static Element createElement(boolean success, boolean lastBuildSuccess) {
        return createElement(success, lastBuildSuccess, "2 minutes 20 seconds", 4, null);
    }

    public static Element createModsElement(int numMods) {
        Element modificationsElement = new Element("modifications");
        for (int i = 1; i <= numMods; i++) {
            Element modificationElement = new Element("modification");

            Element dateElement = new Element("date");
            dateElement.addContent("10/30/2004 13:00:03"); // Put in dynamic value?
            modificationElement.addContent(dateElement);

            Element commentElement = new Element("comment");
            commentElement.addContent("The comment");
            modificationElement.addContent(commentElement);

            Element revisionElement = new Element("revision");
            revisionElement.addContent("1." + i);
            modificationElement.addContent(revisionElement);

            Element fileElement = new Element("file");
            fileElement.setAttribute("action", "modified");

            Element revision2 = new Element("revision");
            revision2.addContent("1." + i);
            fileElement.addContent(revision2);

            Element fileName = new Element("filename");
            fileName.addContent("filename" + i);
            fileElement.addContent(fileName);
            modificationElement.addContent(fileElement);

            if (i != 1) {
                Element projectElement = new Element("project");
                projectElement.addContent("basedir/subdirectory" + i);
                fileElement.addContent(projectElement);
            }

            Element userElement = new Element("user");
            int userNumber = (i > 2) ? i - 1 : i;
            userElement.addContent("user" + userNumber);
            modificationElement.addContent(userElement);

            modificationsElement.addContent(modificationElement);
        }
        return modificationsElement;
    }


    public static Element createElement(
        boolean success,
        boolean lastBuildSuccess,
        String time,
        int modCount,
        String failureReason) {
        Element cruisecontrolElement = new Element("cruisecontrol");
        Element buildElement = new Element("build");
        buildElement.setAttribute("time", time);

        if (!success) {
            buildElement.setAttribute(
                "error",
                (failureReason == null) ? "Compile failed" : failureReason);
        }

        cruisecontrolElement.addContent(createModsElement(modCount));
        cruisecontrolElement.addContent(buildElement);
        cruisecontrolElement.addContent(
            createInfoElement("somelabel", lastBuildSuccess));
        Document doc = new Document();
        doc.setRootElement(cruisecontrolElement);
        return cruisecontrolElement;
    }

    public static Element createInfoElement(String label, boolean lastSuccessful) {
        Element infoElement = new Element("info");

        Hashtable properties = new Hashtable();
        properties.put("projectname", "someproject");
        properties.put("label", label);
        properties.put("lastbuildtime", "");
        properties.put("lastgoodbuildtime", "");
        properties.put("lastbuildsuccessful", lastSuccessful + "");
        properties.put("buildfile", "");
        properties.put("builddate", "11/30/2005 12:07:27");
        properties.put("target", "");
        properties.put("logfile", "log20020313120000.xml");
        properties.put("cctimestamp", "20020313120000");

        Iterator propertyIterator = properties.keySet().iterator();
        while (propertyIterator.hasNext()) {
            String propertyName = (String) propertyIterator.next();
            Element propertyElement = new Element("property");
            propertyElement.setAttribute("name", propertyName);
            propertyElement.setAttribute("value", (String) properties.get(propertyName));
            infoElement.addContent(propertyElement);
        }

        return infoElement;
    }

    /**
     * Return true when same.
     */
    public static void assertArray(String msg, Object[] refarr, Object[] testarr) {
        Assert.assertNotNull(refarr);
        Assert.assertNotNull(testarr);

        Assert.assertNotNull(msg + " Reference array is null and test not", refarr);
        Assert.assertNotNull(msg + " Test array is null and reference not", testarr);
        int shorterLength = Math.min(refarr.length, testarr.length);

        for (int i = 0; i < shorterLength; i++) {
            Assert.assertEquals(msg + " Element " + i + " mismatch.", refarr[i], testarr[i]);
        }
        Assert.assertEquals(msg + " Arrays have different lengths", refarr.length, testarr.length);
        Assert.assertEquals(msg, Arrays.asList(refarr), Arrays.asList(testarr));
    }
}
