/********************************************************************************
 *
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
 *
 ********************************************************************************/
package net.sourceforge.cruisecontrol.util;

import  java.io.BufferedReader;
import  java.io.FileInputStream;
import  java.io.IOException;
import  java.io.InputStream;
import  java.io.InputStreamReader;
import  java.net.URL;
import  java.util.ArrayList;
import  java.util.LinkedList;
import  java.util.List;
import  java.util.Random;

import  junit.framework.TestCase;




/**
 * Class testing the {@link StdoutBuffer} implementation.
 */
public final class StdoutBufferTest extends TestCase {

    /** Random number generator (to randomize texts) */
    private static final Random RANDOM_GENER = new Random();
    /** The maximum sleeping time in msec to make occasional pauses during reading/writing */
    private static final int MAX_SLEEP_TIME = 30;
  
    /** The required number of lines for one test case */
    private static final int NUM_LINES = 500;

  
    /**
     * Setup test environment.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        StringBuilder letters = new StringBuilder();
        try {
            /* Find the file with letters from which to create the texts among resources and read them  */
            URL path = this.getClass().getClassLoader().getResource(
                    "net/sourceforge/cruisecontrol/util/charlist.utf8.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(path.getPath()), "utf-8"));
            String line;

            while ((line = reader.readLine()) != null) {
                letters.append(line);
            }
            reader.close();

        } catch(Exception e) {
            fail("Cannot read letters resource: " + e.getMessage());
        }
      
        /* Lines with texts */
        lines = generateTexts(letters.toString(), NUM_LINES, false);
        /* Create the lists of readers and feeders */
        readers  = new LinkedList<BufferReader>();
        feeder   = new BufferFeeder("Feeder");
    } // setUp    
    /**
     * Clears test environment.
     */
    @Override
    public void tearDown() throws Exception {
        /* Wait until all reader threads end */
        for (Thread tThr : readers) {
            join(tThr);
        }
        /* Wait until all feeder threads end */
        join(feeder);
      
        super.tearDown();
    } // tearDown
    
    /**
     * Tests one feeder to one reader - start feeder (F), wait for finish (W) and start reader (R)
     * @throws IOException if test fails
     */
    public void testOne2One_FWR() throws IOException {
        /* Start feeder  */
        feeder.start();
        /* Wait a second */
        sleep(500);
    
        /* Start the reader */
        readers.add(new BufferReader("Reader_1", feeder.getContent()));
        readers.getLast().start();
        join(readers.getLast());
    } // testOne2One_FWR
  
    /**
     * Tests one feeder to one reader - start feeder (F) and start reader immediately after (R)
     * @throws IOException if test fails
     */
    public void testOne2One_FR() throws IOException {
        /* Start feeder  */
        feeder.start();
        /* Start the reader */
        readers.add(new BufferReader("Reader_1", feeder.getContent()));
        readers.getLast().start();
    
        /* Wait until all lines are filled */
        join(readers.getLast());
    } // testOne2One_FR
  
    /**
     * Tests one feeder to one reader - start reeder (R), wait (W), and start feeder (F)
     * @throws IOException if test fails
     */
    public void testOne2One_RWF() throws IOException {
        /* Start the reader */
        readers.add(new BufferReader("Reader_1", feeder.getContent()));
        readers.getLast().start();
        /* Wait a second */
        sleep(500);
    
        /* Start feeder */
        feeder.start();
    
        /* Wait until all lines are filled */
        join(readers.getLast());
    } // testOne2One_RWF

    /**
     * Tests one feeder to many readers - start readers (R), wait (W), and start some more readers 
     * (R), start feeder immediately after them (F), start readers immediately after it (R), 
     * wait (W) and start even more readers (R).
     * 
     * @throws IOException if test fails
     */
    public void testOne2Many_RWRFRWR() throws IOException {
        /* Start the reader */
        readers.add(new BufferReader("Reader_1", feeder.getContent()));
        readers.getLast().start();
        readers.add(new BufferReader("Reader_2", feeder.getContent()));
        readers.getLast().start();
        /* Wait a second */
        sleep(500);
    
        /* Start one more reader */
        readers.add(new BufferReader("Reader_3", feeder.getContent()));
        readers.getLast().start();
        /* Start feeder  */
        feeder.start();

        /* Start some more readers */
        readers.add(new BufferReader("Reader_4", feeder.getContent()));
        readers.getLast().start();
        readers.add(new BufferReader("Reader_5", feeder.getContent()));
        readers.getLast().start();

        /* Wait a second */
        sleep(500);
        /* Start one more reader */
        readers.add(new BufferReader("Reader_6", feeder.getContent()));
        readers.getLast().start();

        /* Wait 5 seconds */
        sleep(2000);
        /* Start one more readers */
        readers.add(new BufferReader("Reader_7", feeder.getContent()));
        readers.getLast().start();
        readers.add(new BufferReader("Reader_8", feeder.getContent()));
        readers.getLast().start();
    
        /* Wait for feeder to finish */
        join(feeder);

        /* And start one more reader */
        readers.add(new BufferReader("Reader_8", feeder.getContent()));
        readers.getLast().start();
    
        /* Wait until all readers are finished */
        for (Thread tThr : readers) {
            join(tThr);
        }
    } // testOne2Many_RWRFRWR


    /* 
     * ----------- PRIVATE BLOCK ----------- 
     */
  
    /** The list of lines to test */
    private List<String>             lines;

    /** The list to store buffer readers used in a test */
    private LinkedList<BufferReader> readers;
    /** The feeder used in a test */
    private BufferFeeder             feeder;

  
    /* 
     * ----------- INNER CLASSES ----------- 
     */

  
    /**
     * Filler of the buffer.
     */
    private class BufferFeeder extends Thread {
        /** Constructor. Takes input stream from which the output of the command executed is read.
         * @param tName name of feeder. */
        BufferFeeder(String tName) {
            buffer = new StdoutBuffer(null);
            name   = tName;
        } // constructor
    
        /** Loop in which the data are fed into the buffer. */
        @Override
        public void run() {
            int iNextSleep = 0;
        
            try {
	            /* Write lines */
	            for (String line : lines) {
	                buffer.write((line + "\n").getBytes());
	              
	                /* Make occasional pauses ... */
	                if (iNextSleep-- <= 0) {
	                    StdoutBufferTest.sleep(RANDOM_GENER.nextInt(MAX_SLEEP_TIME));
	                    iNextSleep = RANDOM_GENER.nextInt(10);
	                }
	            }
	            /* Closes buffer */
	            StdoutBufferTest.sleep(RANDOM_GENER.nextInt(MAX_SLEEP_TIME * 5));
	            buffer.close();
            } catch (IOException e) {
    			fail("Feeding the StdoutBuffer failed: " + e.getMessage());
    		}
            
        } // run  
    
        /** Gets the stream from which the content can be read (just calls {@link StdoutBuffer#getContent()}).
         *  @return the string representation. 
         * @throws IOException if there is a problem with buffer reading. */
        public final InputStream getContent() throws IOException {
            return buffer.getContent();
        }  // getContent

        /** Gets the string representation of this feeder.
         *  @return the string representation. */
        @Override
        public final String toString() {
            return getClass().getName() + "[" + name + "]";
        } // toString

    
        /** The name of Feeder. */
        private final String       name;
        /** The buffer to feed. */
        private final StdoutBuffer buffer;
    }


    /**
     * Reader and checked of the buffer.
     */
    private class BufferReader extends Thread {
  
        /** Constructor. Takes input stream from which the output of the command executed is read.
         * @param tName name of feeder.
         * @param tInput input stream to read
         */
        BufferReader(String tName, InputStream tInput) {
            dataReader = new BufferedReader(new InputStreamReader(tInput));
            name       = tName;
        } // constuctor

        /** Loop in which the data are read from the stream. */
        @Override
        public void run() {
            try {
                int          iNextSleep = 0;
                int          iLinesRead = 0;
                String       tLine;
    
                /* Read line by line */
                while ((tLine = dataReader.readLine()) != null) {
                    /* Check the line */
                    assertEquals(toString(), lines.get(iLinesRead), tLine);
                    /* Next line */
                    iLinesRead++;
                     
                    /* Make occasional pauses ... */
                    if (iNextSleep-- <= 0) {
                        StdoutBufferTest.sleep(RANDOM_GENER.nextInt(MAX_SLEEP_TIME));
                        iNextSleep = RANDOM_GENER.nextInt(10);
                    }
                }
                /* Everything read, check the size of data */
                assertEquals(toString(), lines.size(), iLinesRead);
           
            } catch (IOException tExc) {
                tExc.printStackTrace();
                fail("Exception when reading data from buffer, " + toString());
            }
        } // run()

        /**
         * Gets the string representation of this command.
         * @return the string representation.
         */
        @Override
        public final String toString() {
            return getClass().getName() + "[" + name + "]";
        } // toString


        /** The name of reader. */
        private final String            name;
        /** The input stream reader. */
        private final BufferedReader    dataReader;

    }

    /* 
     * ----------- PUBLIC STATIC METHODS ----------- 
     */

    /** 
     * Generates the given number of lines filled by the random sequences of characters which
     * are taken from the given list.
     * 
     * @param charsAllowed the list of characters to choose from 
     * @param numlines the number of lines to generate.
     * @param trim {@link String#trim()} be called for each line before put into the array? In 
     *        case that white chars are among the allowed characters but must not start/finish 
     *        the sequence. 
     * @return the array of lines.
     */
    public static List<String> generateTexts(String charsAllowed, int numlines, boolean trim) {
        List<String> lines = new ArrayList<String>(NUM_LINES);
      
        /* Fill the buffer by a sequence of random letters */
        for (int i = 0; i < numlines; i++) {
            StringBuffer line = new StringBuffer();
            /* Create the line */
            for (int j = 0; j < RANDOM_GENER.nextInt(200) + 20; j++) {
                line.append(charsAllowed.charAt(RANDOM_GENER.nextInt(charsAllowed.length())));
            }
           
            lines.add(trim ? line.toString().trim() : line.toString());
        }

        return lines;
    } // generateTexts

    /** 
     * Method waiting the given time. It catches (and ignores) {@link InterruptedException} thrown by 
     * {@link Thread#join()}.. 
     * @param iNumMSecs the number of milliseconds to wait.
     */
    private static void sleep(int iNumMSecs) {
        try {
            Thread.sleep(iNumMSecs);
        } catch (InterruptedException e) {
            /* Ignore */
        }
    } // sleep

    /** 
     * Method joining the given thread. It catches (and ignores) {@link InterruptedException} thrown by 
     * {@link Thread#join()}. 
     * @param tThread the instance of thread to join 
     */
    private static void join(Thread tThread) {
        try {
            tThread.join();
        } catch (InterruptedException e) {
            /* Ignore */
        }
    } // join

}
