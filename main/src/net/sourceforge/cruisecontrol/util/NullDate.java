package net.sourceforge.cruisecontrol.util;

public class NullDate extends java.util.Date {

    public NullDate() {
    }

    public long getTime() {
        return 0;
    }
    
}
