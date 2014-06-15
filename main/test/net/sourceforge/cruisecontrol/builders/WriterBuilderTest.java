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

package net.sourceforge.cruisecontrol.builders;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginXMLHelper;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.ProjectHelper;
import net.sourceforge.cruisecontrol.config.FileResolver;
import net.sourceforge.cruisecontrol.config.XmlResolver;
import net.sourceforge.cruisecontrol.testutil.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;
import org.junit.Ignore;

/**
 * JUnit tests for {@link WriterBuilder}.
 * @author Tomas Ausberger, Daniel Tihelka 
 *
 */
public class WriterBuilderTest extends TestCase {

	/**Files to delete after test. */
    private final FilesToDelete filesToDelete = new FilesToDelete();
    /** XMLHelper. Instantiates the {@link WriterBuilder} object */
    private final PluginXMLHelper helper = new PluginXMLHelper(new TestHelper());

    /** Map passed to {@link WriterBuilder#build(java.util.Map, net.sourceforge.cruisecontrol.Progress) } */
    private final Map<String,String> buildMap = Collections.<String, String> emptyMap();
    /** Map passed to {@link WriterBuilder#build(java.util.Map, net.sourceforge.cruisecontrol.Progress) } */
    private final Progress buildProgress = null;
    
    
    /**
     * Delete all used files.
     * @throws Exception
     */
    @Override
    public final void tearDown() throws Exception {
        filesToDelete.delete();
    }

    /**
     * Tests the ability to create the configured plugin from WriterBuilder class.
     * @throws CruiseControlException
     * @throws IOException
     */
	public final void testPluginCreate() throws CruiseControlException, IOException {
	    final File incFile = filesToDelete.add(this);
	    final DataBuffer writerXmlNode = new DataBuffer();

        // Build dummy XML parseable by Util.loadRootElement() method
        writerXmlNode.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writerXmlNode.add("<writer file=\"out.file\">");
        writerXmlNode.add("  <msg>first text</msg>");
        writerXmlNode.add("  <msg>second text</msg>");
        writerXmlNode.add("  <file file=\"" + incFile.getAbsolutePath() + "\" encoding=\"UTF-8\"/>");
        writerXmlNode.add("  <msg>4th text</msg>");
        writerXmlNode.add("  <msg>5th text</msg>");
        writerXmlNode.add("</writer>");

        // Let the object be configured
        final Object writerObj = helper.configure(Util.loadRootElement(writerXmlNode.getData()),
                WriterBuilder.class, false);
		assertNotNull(writerObj);
		assertTrue(writerObj instanceof WriterBuilder);
		// Let the object be validated
        ((WriterBuilder) writerObj).validate();
	}

    /**
     * Test for overwrite file while exist.
     * @throws CruiseControlException
     * @throws IOException
     */
    public final void testInvaidAcion() throws CruiseControlException, IOException {
        final WriterBuilder writerObj = new WriterBuilder();

        //Create file with inner text
        try {
            writerObj.setFile("XXX.file");
            writerObj.setAction("");
            writerObj.validate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("action not set properly"));
        }
    }

	
	/**
	 * Test using of GZIP.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	@Ignore
	public final void testMsgAddGzip() throws CruiseControlException, IOException {
	    final File outFile = filesToDelete.add(this);
	    final File inpFile = filesToDelete.add(this);
	    final WriterBuilder writerObj = new WriterBuilder();
	    final DataBuffer buff = new DataBuffer();

        IO.delete(outFile);

        writerObj.setFile(outFile.getAbsolutePath());
        writerObj.setGzip(true);
        newMssg(writerObj).append(buff.add("first text"));
        newMssg(writerObj).append(buff.add("second text"));
        newFile(writerObj).setFile(buff.add("Third text, that is in file.", inpFile));
        newMssg(writerObj).append(buff.add("4th text"));

        writerObj.validate();
		writerObj.build(buildMap, buildProgress);

		final File gzipFile = new File(outFile.getAbsolutePath() + ".gzip"); // just add extension to the name
		assertTrue(gzipFile.exists());
		assertStreams(buff.getData(), new GZIPInputStream(new FileInputStream(gzipFile)));
	}


	/**
	 * Testing of append while using GZIP.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testMsgAddGzipAppend() throws CruiseControlException, IOException {
	    final File outFile = filesToDelete.add("test.out", ".gzip");
	    final File inpFile = filesToDelete.add(this);
	    final WriterBuilder writerObj = new WriterBuilder();
	    final DataBuffer buff = new DataBuffer();

        // Prepend the text
        buff.add("InnerText", new GZIPOutputStream(new FileOutputStream(outFile)));

        writerObj.setFile(outFile.getAbsolutePath());
        writerObj.setGzip(true);
        writerObj.setAction("append");
        newMssg(writerObj).append(buff.add("first text"));
        newMssg(writerObj).append(buff.add("second text"));
        newFile(writerObj).setFile(buff.add("Third text, that is in file.", inpFile));
        newMssg(writerObj).append(buff.add("4th text"));

        writerObj.validate();
        writerObj.build(buildMap, buildProgress);

        assertTrue(outFile.exists());
        assertStreams(buff.getData(), new GZIPInputStream(new FileInputStream(outFile)));
	}

	/**
	 * Test with void tag msg.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testMsgAddWithVoidMsg() throws CruiseControlException, IOException {
	    final File outFile = filesToDelete.add(this);
	    final WriterBuilder writerObj = new WriterBuilder();
	    final DataBuffer buff = new DataBuffer();

        writerObj.setFile(outFile.getAbsolutePath());
        writerObj.setAction("overwrite");
        newMssg(writerObj).append(buff.add("first text"));
        newMssg(writerObj).append(buff.add(""));

        writerObj.validate();
        writerObj.build(buildMap, buildProgress);

        assertTrue(outFile.exists());
        assertStreams(buff.getData(), new FileInputStream(outFile));
	}

	/**
	 * Test for overwrite file while exist, action = overwrite.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testActionOverwrite() throws CruiseControlException, IOException {
	    final File outFile = filesToDelete.add(this);
	    final WriterBuilder writerObj = new WriterBuilder();
	    final DataBuffer buff = new DataBuffer();

        //Create file with inner text
        IO.write(outFile, "InnerText");

        writerObj.setFile(outFile.getAbsolutePath());
        writerObj.setAction("overwrite");
        newMssg(writerObj).append(buff.add("first text"));

        writerObj.validate();
		writerObj.build(buildMap, buildProgress);

		assertTrue(outFile.exists());
		assertStreams(buff.getData(), new FileInputStream(outFile));
	}

	/**
	 * Test for overwrite existing file with action = create.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testMsgActionCreate() throws CruiseControlException, IOException {
	    final File outFile = filesToDelete.add(this);
	    final WriterBuilder writerObj = new WriterBuilder();

        //Create file with inner text
        IO.write(outFile, "InnerText");

        //Set append true.
        writerObj.setFile(outFile.getAbsolutePath());
        writerObj.setAction("create");
        newMssg(writerObj).append("first text");

        // Validation step
        // Setting overwrite false set append also false
        assertFalse(writerObj.getAppend());

        try {
        	writerObj.validate();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().equals("Trying to overwrite file without permition."));
		}

        // Build step. The file was not existing during the validation, but has been created
        // later on ...
        IO.delete(outFile);
        writerObj.validate();
        //Create file with inner text - again
        IO.write(outFile, "InnerText");
        
        // Trigger the build as well
        final Element out = writerObj.build(buildMap, buildProgress);
        assertNotNull(out.getAttribute("error"));
        
	}

	/**
	 * Test for append text to existing file, action = append.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testActionAppend() throws CruiseControlException, IOException {
	    final File outFile = filesToDelete.add(this);
	    final WriterBuilder writerObj = new WriterBuilder();
	    final DataBuffer buff = new DataBuffer();

        //Create file with inner text, that will stay in file after appending
        buff.add("InnerText", new FileOutputStream(outFile));

        //For appending, overwrite have to be set true
        writerObj.setFile(outFile.getAbsolutePath());
        writerObj.setAction("append");
        newMssg(writerObj).append(buff.add("first text"));

        writerObj.validate();
        writerObj.build(buildMap, buildProgress);

        assertTrue(outFile.exists());
        assertStreams(buff.getData(), new FileInputStream(outFile));
	}

	/**
	 * Test for trim.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testTrim() throws CruiseControlException, IOException {
	    final File outFile = filesToDelete.add(this);
	    final WriterBuilder writerObj = new WriterBuilder();
	    final DataBuffer buff = new DataBuffer();

        IO.delete(outFile);

        //Removing white chars from start and end
        writerObj.setFile(outFile.getAbsolutePath());
        writerObj.setTrim(true);
        newMssg(writerObj).append(buff.add("   first text  ", true));
        newMssg(writerObj).append(buff.add("text", true));
        newMssg(writerObj).append(buff.add("  ", true));

        writerObj.validate();
        writerObj.build(buildMap, buildProgress);

        assertTrue(outFile.exists());
        assertStreams(buff.getData(), new FileInputStream(outFile));
	}

	/**
	 * Test for not supported encoding.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testWrongEncodingOutput() throws CruiseControlException, IOException {
	    final WriterBuilder writerObj = new WriterBuilder();

        //Set not supported encoding
        writerObj.setFile("xxx.out");
        writerObj.setEncoding("wrongCoding");
        newMssg(writerObj).append("text");

		try {
			writerObj.validate();
        	fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("Encoding"));
			assertTrue(e.getMessage().contains("not supported on plugin"));
		}
	}

	/**
	 * Test for not supported encoding on the input file.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testWrongEncodingInput() throws CruiseControlException, IOException {
        final File inpFile = filesToDelete.add(this);
	    final WriterBuilder writerObj = new WriterBuilder();

        writerObj.setFile("xxx.out");
        //Set not supported encoding
        final WriterBuilder.File file = newFile(writerObj);
        file.setFile(inpFile.getAbsolutePath());
        file.setEncoding("wrongCoding");

		try {
			writerObj.validate();
        	fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().startsWith("Encoding"));
			assertTrue(e.getMessage().contains("not supported on plugin"));
		}
	}

	/**
	 * Test on missing file path in input file.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testNoInputFileSet() throws CruiseControlException, IOException {
		final WriterBuilder writerObj = new WriterBuilder();

        writerObj.setFile("xxx.out");
        newFile(writerObj);

		try {
			writerObj.validate();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().equals("'file' is required for WriterBuilder$File"));
		}
	}

	/**
	 * Test on missing file path in WriterBuilder.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testNoOutputFileSet() throws CruiseControlException, IOException {
        final WriterBuilder writerObj = new WriterBuilder();

		try {
			writerObj.validate();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().equals("'file' is required for WriterBuilder"));
		}
	}

	/**
	 * Test for invalid path to input file.
	 * @throws CruiseControlException
	 * @throws IOException
	 */
	public final void testInputNotExist() throws CruiseControlException, IOException {
        final WriterBuilder writerObj = new WriterBuilder();

        writerObj.setFile("xxx.out");
        //Set invalid input file path
        WriterBuilder.File file = newFile(writerObj);
        file.setFile("invalidFilePath");

		try {
			writerObj.validate();
        	fail();
		} catch(Exception e) {
			assertTrue(e.getMessage().startsWith("File specified"));
			assertTrue(e.getMessage().contains("for attribute [file] on plugin"));
			assertTrue(e.getMessage().endsWith("doesn't exist."));
		}
	}

    /**
     * Test encoding (latin2, cp1250, Czech language).
     * @throws CruiseControlException
     * @throws IOException
     */
    public final void testEncodingCZ() throws CruiseControlException, IOException {
        final File inpFile = filesToDelete.add(this);
        final File outFile = filesToDelete.add(this);
        final WriterBuilder writerObj = new WriterBuilder();
        final DataBuffer buff = new DataBuffer();

        writerObj.setFile(outFile.getAbsolutePath());
        writerObj.setEncoding("latin2");
        // <msg>...</msg>
        newMssg(writerObj).append(buff.add("Zvlášť zákeřný učeň s ďolíčky běží podél zóny úlů.", true));
        // <file/>
        final WriterBuilder.File f = newFile(writerObj);
        f.setFile(inpFile.getAbsolutePath());
        f.setEncoding("cp1250");
        // Write message to the file
        IO.write(inpFile, buff.add("Příliš žluťoučký kůň úpěl ďábelské ódy"), "cp1250"); 

        writerObj.validate();
        writerObj.build(buildMap, buildProgress);
        // Assert. The output file is in Latin2 encoding
        assertReaders(buff.getChars(), new InputStreamReader(new FileInputStream(outFile), "latin2"));
    }

    /**
     * Test encoding (latin2, cp1250, Czech language).
     * @throws CruiseControlException
     * @throws IOException
     */
    public final void testEncodingRu() throws CruiseControlException, IOException {
        final File inpFile = filesToDelete.add(this);
        final File outFile = filesToDelete.add(this);
        final WriterBuilder writerObj = new WriterBuilder();
        final DataBuffer buff = new DataBuffer();

        // <msg>...</msg>
        writerObj.setFile(outFile.getAbsolutePath());
        writerObj.setEncoding("iso8859-5");
        newMssg(writerObj).append(buff.add("Здесь фабула объять не может всех эмоций - шепелявый "
                + "скороход в юбке тащит горячий мёд.", true));
        // <file/>
        final WriterBuilder.File f = newFile(writerObj);
        f.setFile(inpFile.getAbsolutePath());
        f.setEncoding("cp1251");
        // Write message to the file
        IO.write(inpFile, buff.add("Художник-эксперт с компьютером всего лишь яйца в объёмный низкий "
                + "ящик чохом фасовал."), "cp1251"); 

        writerObj.validate();
        writerObj.build(buildMap, buildProgress);
        // Assert. The output file is in Latin2 encoding
        assertReaders(buff.getChars(), new InputStreamReader(new FileInputStream(outFile), "iso8859-5"));
    }

    /**
     * Tests the setting of workdir value
     * @throws CruiseControlException
     * @throws IOException
     */
    public final void testWorkingDir() throws CruiseControlException, IOException {
        final File inpFile = filesToDelete.add(this);
        final File outFile = filesToDelete.add(this);
        final WriterBuilder writerObj = new WriterBuilder();
        final DataBuffer buff = new DataBuffer();

        // Must be both in the same directory
        assertEquals(outFile.getParent(), inpFile.getParent());
        
        // <msg>...</msg>
        writerObj.setFile(outFile.getAbsolutePath());
        newMssg(writerObj).append(buff.add("text1"));
        newFile(writerObj).setFile(new File(buff.add("text2", inpFile)).getName()); // take just name

        writerObj.setWorkingDir(outFile.getParent());
        writerObj.validate();
        writerObj.build(buildMap, buildProgress);
        // Assert. The output file is in Latin2 encoding
        assertReaders(buff.getChars(), new InputStreamReader(new FileInputStream(outFile), "latin2"));
    }

	
	/**
	 * Adds {@link WriterBuilder.File} object to the builder object and returns it
     * @param builder the instance to add file to
     * @return not configured instance of file
	 */
	public static WriterBuilder.File newFile(final WriterBuilder builder) {
	    return (WriterBuilder.File) builder.createFile();
	}

	/**
     * Adds {@link WriterBuilder.Msg} object to the builder object and returns it
	 * @param builder the instance to add message to
	 * @return not configured instance of message
	 */
    public static WriterBuilder.Msg newMssg(final WriterBuilder builder) {
        return (WriterBuilder.Msg) builder.createMsg();
    }

    /**
     * String buffer from which data can be read as stream.
     */
    public static class DataBuffer {
        /** Constructor */
        public DataBuffer() {
        }
        
        /**
         * Adds the given string into the buffer and returns the input string back.
         * @param s 
         * @return string
         */
        public String add(final String s) {
            buff.append(s + System.lineSeparator());
            return s;
        }

        /**
         * Adds the given string into the buffer and returns the input string back.
         * @param s 
         * @param eatwhites
         * @return outputString
         */
        public String add(String s, final boolean eatwhites) {
        	if (eatwhites) {
        		s = s.trim();
        	}
            buff.append(s + System.lineSeparator());
            return s;
        }

        /**
         * Adds the given string into the buffer, stores in to the given file and returns back
         * the <b>absolute path to file</b> into which the text has been stored.
         * @param s the string to be added to the buffer and stored to the file
         * @param f the file to write the string into
         * @throws CruiseControlException
         * @return filePath
         */
        public String add(final String s, final File f) throws CruiseControlException {
            buff.append(s + System.lineSeparator());
            IO.write(f, s + System.lineSeparator());
            return f.getAbsolutePath();
        }
        /**
         * Adds the given string into the buffer, stores in to the stream and returns the input
         * string back.
         * @param s the string to be added to the buffer and stored to the stream
         * @param o the output to write the string into
         * @throws CruiseControlException
         * @return inputString
         */
        public String add(final String s, final OutputStream o) throws CruiseControlException {
            buff.append(s + System.lineSeparator());
            IO.write(o, s + System.lineSeparator());
            return s;
        }

        /**
         * Returns the content of the buffer readable through {@link InputStream}.
         * @return InputStream
         */
        public InputStream getData() {
            return new ByteArrayInputStream(buff.toString().getBytes());
        }
        /**
         * Returns the content of the buffer readable through {@link Reader}.
         * @return Reader
         */
        public Reader getChars() {
            return new CharArrayReader(buff.toString().toCharArray());
        }


        /**
         * The holder of the buffer.
         */
        private final StringBuffer buff = new StringBuffer(256);
    }

    /**
     * Helping test class allowing to create {@link WriterBuilder} instance from an XML
     * configuration.
     */
	public static class TestHelper implements ProjectHelper {

	    @Override
		public Object configurePlugin(final Element pluginElement,
				final boolean skipChildElements) throws CruiseControlException {
			
		    assertEquals("writer", pluginElement.getName());
		    return new WriterBuilder();
		}

        @Override
		public FileResolver getFileResolver() {
			return null;
		}

        @Override
		public XmlResolver getXmlResolver() {
			return null;
		}

        @Override
        public Element resolveProperties(Element objectElement) {
            return null;
        }
	}
}
