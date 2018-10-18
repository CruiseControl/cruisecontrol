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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.jdom2.Document;
import org.jdom2.Element;

import junit.framework.Assert;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;

public final class TestUtil {
    public static class FilesToDelete {
        private final static File tmpdir;

        /** Static initializer; fills #tmpdir */
        static {
            File t;

            try {
                t = new FilesToDelete().add(FilesToDelete.class).getParentFile();
            } catch (IOException e) {
                // If fails, get the root directory. Most likely the directory will not be writable and the tests
                // will start to fail. But I think it is better than to pretend that everything is OK ...
                t = new File("/");
                // Leave a message
                System.err.println("------------");
                System.err.println("Unable to get the temporary dir, setting it to: [" + t.getAbsolutePath() + "]");
                System.err.println("Reported error:");
                e.printStackTrace(System.err);
                System.err.println("------------");
            }
            // Set the dir
            tmpdir = t;
        }


        private final List<File> files = new Vector<File>();

        /**
         * Returns the path to the (temporary) directory in which the files are created
         * @return the path to tmpdir
         */
        public static File tmpdir() {
            return tmpdir;
        }

        public File add(File file) {
             files.add(file);
             return file;
        }

        public File add(String file) throws IOException {
            return add(file, null);
        }

        /**
         * Creates an empty file in the default temporary-file directory, using the name
         * of the object's class as the prefix. Automatically adds the name into the list of files
         * hold by the class.
         * @param obj the calling class (used to get its name as temporary file pattern)
         * @return a new temp file
         * @throws IOException when the file cannot be created
         */
        public File add(Object obj) throws IOException {
            return add(obj.getClass().getName(), null);
        }
        /**
         * Creates an empty file in the default temporary-file directory, using the given prefix
         * and suffix to generate its name. Automatically adds the name into the list of files
         * hold by the class.
         * @param prefix the prefix string passed to {@link File.createTempFile(String, String)}
         * @param suffix the suffix string passed to {@link File.createTempFile(String, String)}
         * @return a new file
         * @throws IOException when the file cannot be created
         */
        public File add(String prefix, String suffix) throws IOException {
            final File file = File.createTempFile(prefix, suffix);
            return add(file);
        }

        /** The same as {@link #add(String, String)} but creates directory instead of a file */
        public File adddir(String prefix, String suffix) throws IOException {
            final File file = add(prefix, suffix);
            // Delete the file
            IO.delete(file);

            if (! file.mkdirs()) {
                throw new IOException("Unable to create directory ["+file+"]");
            }
            return add(file);
        }

        public void delete() {
            for (File file : files) {
                IO.delete(file);
            }
            files.clear();
        }

        /** "Destructor" */
        @Override
        protected void finalize() {
            delete();
        }
    }

    /**
     * Records and restores properties get by System.get ...
     */
    public static class PropertiesRestorer {

        private final Map<String, String> recordedProps = new HashMap<String, String>();

        /**
         * Stores all properties as they are defined in the time of the call of the method.
         * It is recommended to call this method in @link {@link TestCase#setUp()} or in the constructor
         * of a particular test
         */
        public void record() {
            recordedProps.clear();
            // Cop item-by-item to prevent shallow copy
            for (String name : System.getProperties().stringPropertyNames()) {
                recordedProps.put(name, System.getProperty(name));
            }
        }

        /**
         * Resets properties into the state as they were recorded in @link {@link #record()}.
         * It is recommended to call this method in @link {@link TestCase#tearDown()}.
         */
        public void restore() {
            System.getProperties().clear();
            // Cop item-by-item to prevent shallow copy
            for (Entry<String, String> p : recordedProps.entrySet()) {
                System.setProperty(p.getKey(), p.getValue());
            }
        }
    }


    private static File targetDir;
    public static File getTargetDir() {
        if (targetDir == null) {
            final URL url;
            url = TestUtil.class.getClassLoader().getResource("net/sourceforge/cruisecontrol/BuilderTest.class");
            final File classFile;
            try {
                classFile = new File(URLDecoder.decode(url.getPath(), "utf-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            targetDir = classFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();

            final String absPath = targetDir.getAbsolutePath();
            Assert.assertTrue("Unit test 'target' dir not a dir: " + absPath, targetDir.isDirectory());
            Assert.assertTrue("Unit test 'target' does not exist: " + absPath, targetDir.exists());
            Assert.assertEquals("Unit test 'target' dir has unexpected name: " + absPath,
                    "target", targetDir.getName());
        }
        return targetDir;
    }

    private TestUtil() {

    }

    public static Element createPassingBuild() {
      return createElement(true, true);
    }

    public static Element createFixedBuild() {
        return createElement(true, false);
    }

    public static Element createFailedBuild() {
        return createElement(false, false);
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
        Element info = new Element("info");
        addProperty(info, "projectname", "someproject");
        addProperty(info, "label", label);
        addProperty(info, "lastbuildtime", "");
        addProperty(info, "lastgoodbuildtime", "");
        addProperty(info, "lastbuildsuccessful", lastSuccessful + "");
        addProperty(info, "buildfile", "");
        addProperty(info, "builddate", "11/30/2005 12:07:27");
        addProperty(info, "target", "");
        addProperty(info, "logfile", "log20020313120000.xml");
        addProperty(info, "cctimestamp", "20020313120000");
        return info;
    }

    /**
     * Return true when same.
     * @param msg error message prefix
     * @param refarr first array
     * @param testarr second array
     */
    public static void assertArray(final String msg, final Object[] refarr, final Object[] testarr) {
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

    /**
     * Checks if the given arguments are in the command line. The assert succeeds when all the expected
     * arguments are found in the given command line and no other attributes remain.
     *
     * @param expect the array of expected command line options and their values, grouped according to their
     *      expected meaning. For example, having:
     *      "foo -opt1 val1 -opt2 -opt3 val3.1 val3.2 arg1 arg2"
     *      command line, it is recommended to pass expected arguments as:
     *      ["foo", "-opt1 val1", "-opt2", "-opt3 val3.1 val3.2", "arg1", "arg2"]
     * @param args the array of command line arguments as get e.g. by {@link Commandline#getCommandline()}
     */
    public static void assertCommandLine(final String[] expect, final String[] args) {
        StringBuffer sb = new StringBuffer(256);
        for (String a : args) {
            sb.append(a);
            sb.append(' ');
        }
        final String o = sb.toString();
      //final String o = String.join(" ", args); Works on java-8 and higher ...
        String a = o;

        // Match all the commands
        for (final String e : expect) {
             a = assertCommandLine(e, a, o);
        }
        // Must be empty
        if (!a.trim().isEmpty()) {
            Assert.fail("No full match. [" + a + "] not matched to anything in [" + o + "]");
        }
    }
    // Helper for #assertCommandLine(String[], String[])
    private static String assertCommandLine(final String expect, final String args, final String argsFull) {
        final int beg = args.indexOf(expect);
        final int end = beg + expect.length();

        // Not found
        if (beg < 0) {
            Assert.fail("No [" + expect + "] in [" + argsFull + "]");
            return args;
        }
        // Space must preceed
        if (beg > 0 && args.charAt(beg -1) != ' ') {
            Assert.fail("No [" + expect + "] in [" + argsFull + "] as an argument");
            return args;
        }
        // Space must follow
        if (end < args.length() && args.charAt(end) != ' ') {
            Assert.fail("No [" + expect + "] in [" + argsFull + "] as an argument");
            return args;
        }

        // Remove the part which has been matched
        if (end < args.length()) {
            return args.substring(0, beg) + args.substring(end +1);
        }
        else {
            return args.substring(0, beg);
        }
    }


    public static void addProperty(Element e, String name, String value) {
        Element prop = new Element("property");
        prop.setAttribute("name", name);
        prop.setAttribute("value", value);
        e.addContent(prop);
    }

}
