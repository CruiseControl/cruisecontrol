package net.sourceforge.cruisecontrol.util;

import java.io.File;

import net.sourceforge.cruisecontrol.CruiseControlException;

public class Directory {
    private File directory;
    
    public void validate() throws CruiseControlException {
        if (directory == null) {
            throw new DirectoryNotSpecifiedException("directory");
        }
        
        if (!exists()) {
            throw new DirectoryDoesNotExistException();
        }
        
        if (!isDirectory()) {
            throw new FileInsteadOfDirectoryException();
        }
    }

    public boolean exists() {
        return directory.exists();
    }
    
    public boolean isDirectory() {
        return directory.exists();
    }
    
    public class DirectoryDoesNotExistException extends CruiseControlException {
        DirectoryDoesNotExistException() {
            super(directory.getPath() + " does not exist");
        }
    }

    public class FileInsteadOfDirectoryException extends CruiseControlException {
        FileInsteadOfDirectoryException() {
            super(directory.getPath() + " is a file instead of a directory");
        }
    }

    public static class DirectoryNotSpecifiedException extends CruiseControlException {
        public DirectoryNotSpecifiedException(String attributeName) {
            super(attributeName + " must be specified");
        }
    }

    public void setPath(String path) {
        directory = new File(path);
    }

    public File toFile() {
        return directory;
    }
}