package net.sourceforge.cruisecontrol;

public interface Manipulator {

    public void execute(String logDir);
    
    public void validate() throws CruiseControlException;
    
}
