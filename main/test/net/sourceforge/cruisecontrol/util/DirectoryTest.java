package net.sourceforge.cruisecontrol.util;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.junit.Test;


public class DirectoryTest {

    @Test(expected = Directory.DirectoryNotSpecifiedException.class)
    public void validateShouldFailIfPathNotSpecified() throws CruiseControlException {
        Directory directory = new Directory();
        directory.validate();
    }
    
    @Test(expected = Directory.DirectoryDoesNotExistException.class)
    public void validateShouldFailIfDirectoryDoesNotExist() throws CruiseControlException {
        Directory directory = new Directory() {
            @Override
            public boolean exists() {
                return false;
            }
            
            @Override
            public boolean isDirectory() {
                return true;
            }
        };
        directory.setPath("mocked");
        directory.validate();
    }
    
    @Test(expected = Directory.FileInsteadOfDirectoryException.class)
    public void validateShouldFailIfDirectoryIsFile() throws CruiseControlException {
        Directory directory = new Directory() {
            @Override
            public boolean exists() {
                return true;
            }
            
            @Override
            public boolean isDirectory() {
                return false;
            }
        };
        directory.setPath("mocked");
        directory.validate();
    }
    
    @Test
    public void validateShouldPassIfDirectoryIsDirectory() throws CruiseControlException {
        Directory directory = new Directory() {
            @Override
            public boolean exists() {
                return true;
            }
            
            @Override
            public boolean isDirectory() {
                return true;
            }
        };
        directory.setPath("mocked");
        directory.validate();
    }

}
