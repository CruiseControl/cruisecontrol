package net.sourceforge.cruisecontrol.testutil;

public class MockTask extends org.apache.tools.ant.Task {

    private String _logMessage;
    
    public MockTask() {
    }
    
    public void log(String message, int msgLevel) {
        _logMessage = message;
    }
    
    public String getSentLog() {
        return _logMessage;
    }

}
