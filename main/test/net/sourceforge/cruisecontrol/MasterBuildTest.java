package net.sourceforge.cruisecontrol;

import java.io.*;
import junit.framework.*;

public class MasterBuildTest extends TestCase {

    private File _logXML;
    
    public MasterBuildTest(String name) {
        super(name);
    }
    
    public void setUp() {
        _logXML = new File("log.xml");
        _logXML.delete();
    }

    public void tearDown() {
        _logXML.delete();
    }
    
    public void testMock() {}
    
/*    
    public void testLogFileReadOnly() {
        MasterBuild mb = new MasterBuild();
        try {
            assertTrue("Could not create file", _logXML.createNewFile());
            assertTrue("Could not set read only", _logXML.setReadOnly());
        } catch (IOException ioe) {
            fail("Problem with file");
        } 
        
        assertTrue("Should have been read only", !mb.canWriteXMLLoggerFile());
    }
    
    public void testLogFileNotReadOnly() {
        MasterBuild mb = new MasterBuild();
        try {
            assertTrue("Could not create file", _logXML.createNewFile());
        } catch (IOException ioe) {
            fail("Problem with file");
        } 
        
        assertTrue("Should have been writable", mb.canWriteXMLLoggerFile());
    }
    
    public void testLogFileNotPresent() {
        MasterBuild mb = new MasterBuild();
        assertTrue("Should have been writable", mb.canWriteXMLLoggerFile());
    }
  */  
    public static void main(String[] args) {
        junit.textui.TestRunner.run(MasterBuildTest.class);
    }    
    
}
