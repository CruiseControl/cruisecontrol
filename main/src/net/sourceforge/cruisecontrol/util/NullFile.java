package net.sourceforge.cruisecontrol.util;

public class NullFile extends java.io.File {

    public NullFile() {
        this(" ");
    }
    
    public NullFile(String pathName) {
        super(pathName);
    }
    
    public String getPath() {
        return " ";
    }
    
}
