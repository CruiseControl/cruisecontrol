package net.sourceforge.cruisecontrol;

import java.io.*;
import java.util.*;
import junit.framework.*;

public class XMLLogMergerTest extends TestCase {

    private final String FINAL_LOG = "final.xml";
    private final String ANT_LOG = "log.xml";
    private final String ANT_LOG_STUFF = "<stuff>Blah, blah, blah</stuff>";
    
    private final String LABEL = "label.0";
    private final String TODAY = "today";
    
    private final String LOG_ONE = "logFileOne.xml";
    private final String LOG_TWO = "logFileTwo.xml";
    
    private final String LOG_ONE_STUFF = "<stuff> One's stuff </stuff>";
    private final String LOG_TWO_STUFF = "<stuff> Two's stuff </stuff>";
    
    private final String XML_VERSION_LINE = "<?xml version=\"1.0\"?>";
    
    public XMLLogMergerTest(String name) {
        super(name);
    }

    protected void setUp() {
        new File(FINAL_LOG).delete();
        new File(ANT_LOG).delete();
        new File(LOG_ONE).delete();
        new File(LOG_TWO).delete();
        
        List logFiles = new ArrayList();
        logFiles.add(LOG_ONE);
        logFiles.add(LOG_TWO);
        
        try {
            createLogFile(LOG_ONE, LOG_ONE_STUFF);
            createLogFile(LOG_TWO, LOG_TWO_STUFF);
            
            writeAntLog();
        
            XMLLogMerger merger = 
                new XMLLogMerger(FINAL_LOG, ANT_LOG, logFiles, 
                LABEL, TODAY);
            merger.merge();
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }
    
    public void testMerge() {
        assertTrue("Final log file was not created", new File("final.xml").exists());
        try {
            assertTrue("No start build tag found", mergedLogContains("<build>"));
            assertTrue("No end build tag found", mergedLogContains("</build>"));
            assertTrue("No ant log stuff found", mergedLogContains(ANT_LOG_STUFF));
            assertTrue("Log one stuff not found", mergedLogContains(LOG_ONE_STUFF));
            assertTrue("Log two stuff not found", mergedLogContains(LOG_TWO_STUFF));
            assertTrue("No label found", mergedLogContains(LABEL));
            assertTrue("No today found", mergedLogContains(TODAY));
            assertTrue("No XML version info found", 
                mergedLogContains(XML_VERSION_LINE));
            assertTrue("More than one XML version line", !tooManyXMLVersionLines());
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }

    private boolean tooManyXMLVersionLines() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(FINAL_LOG));
        int count = 0;
        String currentLine = reader.readLine();
        while(currentLine != null) {
            if (currentLine.indexOf(XML_VERSION_LINE) != -1) {
                count++;
                if (count > 1) {
                    return true;
                }
            }
            currentLine = reader.readLine();
        }
        
        return false;
    }
    
    private void createLogFile(String logFileName, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName));
        writer.write(XML_VERSION_LINE);
        writer.newLine();
        writer.write(content);
        
        writer.flush();
        writer.close();
    }
    
    private boolean mergedLogContains(String target) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(FINAL_LOG));
        String currentLine = reader.readLine();
        while(currentLine != null) {
            if (currentLine.indexOf(target) != -1) {
                return true;
            }
            currentLine = reader.readLine();
        }
        
        return false;
    }
    
    private void writeAntLog() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(ANT_LOG));
        writer.write(XML_VERSION_LINE);
        writer.write("<build>");
        writer.newLine();
        writer.write(ANT_LOG_STUFF);
        writer.newLine();
        writer.write("</build>");
        
        writer.flush();
        writer.close();
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(XMLLogMergerTest.class);
    }    
    
}
