import net.sourceforge.cruisecontrol.*;

/**
 * This class wraps the MasterBuild process to 
 * snazzify the command line. 
 * (instead of "javac net.sourceforge.cruisecontrol.MasterBuild"
 *  the command line is snazzier, in the form of "java CruiseControl")
 * 
 * * @author alden almagro (alden@thoughtworks.com), Paul Julius (pdjulius@thoughtworks.com), ThoughtWorks, Inc. 2001
 */
public class CruiseControl {
    public static void main(String[] args) {
        MasterBuild.main(args);
    }
}
