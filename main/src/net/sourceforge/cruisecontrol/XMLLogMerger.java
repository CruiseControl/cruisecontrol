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
package net.sourceforge.cruisecontrol;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;

/**
 * Merges ant log and all log files set in auxlogfiles property into one
 * aggregated log.
 *
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class XMLLogMerger {
    
    private String finalLogName;
    private String antLogFile;
    private List logFiles;
    private String label;
    private String currentTime;
    
    /**
     * @param finalLogName Name of final aggregated log file
     * @param antLogFile The value from the Ant project property XmlLogger.file
     * @param logFiles List of file names from auxlogfiles property
     * @param label Current build label
     * @param currentTime Current time
     */
    public XMLLogMerger(String finalLogName, String antLogFile, List logFiles, 
     String label, String currentTime) {
        this.finalLogName = finalLogName;
        this.antLogFile = antLogFile;
        this.logFiles = logFiles;
        this.label = label;
        this.currentTime = currentTime;
    }
    
    /**
     * Perform the XML file merge.
     * @throw IOException
     */
    public void merge() throws IOException {
        System.out.println("Writing " + finalLogName);
        BufferedWriter out = new BufferedWriter(new FileWriter(finalLogName));
        
        mergeAntLog(out);
        
        for (Iterator iter = logFiles.iterator(); iter.hasNext();) {
            mergeLogFile((String) iter.next(), out);
        }
        
        closeMergedLog(out);
        out.flush();
        out.close();
    }
    
    /**
     * Copies ant log to the aggregated log except for the final </build> tag
     * which is appended by closeMergedLog.  NOTE: This means that this method
     * should always be called first before doing the other merges
     *
     * @param writer Should wrap a FileWriter to the merged log file
     */
    private void mergeAntLog(BufferedWriter writer) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(antLogFile));
        
        String currentLine = reader.readLine();
        while (currentLine.indexOf("</build>") == -1) {
            writer.write(currentLine);
            writer.newLine();
            currentLine = reader.readLine();
        }
        reader.close();
    }

    /**
     * Merges a log file after stripping off the initial XML version tag if it
     * exists.  If the file is actually a directory, the method will recursively
     * call itself for all XML files within the directory.
     *
     * @param fileToMerge May be a directory or normal file
     * @param writer Should wrap a FileWriter to the merged log file
     */
    private void mergeLogFile(String fileToMerge, BufferedWriter writer)
    throws IOException {
        File file = new File(fileToMerge);
        if (file.isDirectory()) {
            String[] files = retrieveXMLFiles(file);
            if (files == null) {
                return;
            }
            
            for (int i = 0; i < files.length; i++) {
                mergeLogFile(fileToMerge + File.separator + files[i], writer);
            }
            
            return;
        }
        
        BufferedReader reader = new BufferedReader(new FileReader(fileToMerge));
        String currentLine = reader.readLine();
        // Skip xml version tag which can only be first line if it exists
        if (currentLine.indexOf("<?xml") != -1) {
            currentLine = reader.readLine();
        }
        while (currentLine != null) {
            writer.write(currentLine);
            writer.newLine();
            currentLine = reader.readLine();
        }
        reader.close();
    }
    
    /**
     * @param dir
     * @return Array of all file names that end in *.xml in the directory
     */
    private String[] retrieveXMLFiles(File dir) {
        return dir.list(new FilenameFilter() {
            public boolean accept(File directory, String name) {
                return name.endsWith(".xml");
            }
        });
    }
    
    /**
     * Close merged log file by appending label and current time info and the
     * terminating </build> tag
     * @param writer Should wrap a FileWriter to the merged log file
     */
    private void closeMergedLog(BufferedWriter writer) throws IOException{
        writer.write("<label>" + label + "</label>");
        writer.newLine();
        writer.write("<today>" + currentTime + "</today>");
        writer.newLine();
        writer.write("</build>");
    }
    
}
