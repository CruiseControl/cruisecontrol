package net.sourceforge.cruisecontrol;

/**
 * This class provides a default label incrementation.
 * This class expects the label format to be "x.y",
 * where x is any String and y is an integer.
 *
 * @author alden almagro (alden@thoughtworks.com), Paul Julius (pdjulius@thoughtworks.com), ThoughtWorks, Inc. 2001
 */
public class DefaultLabelIncrementer implements LabelIncrementer {
    
    /**
     * Increments the label when a successful build occurs.
     * Assumes that the label will be in
     * the format of "x.y", where x can be anything, and y is an integer.
     * The y value will be incremented by one, the rest will remain the same.
     * 
     * @param oldLabel Label from previous successful build.
     * @return Label to use for most recent successful build.
     */
    public String incrementLabel(String oldLabel) {
        
        String prefix = oldLabel.substring(0, oldLabel.lastIndexOf(".") + 1);
        String suffix = oldLabel.substring(oldLabel.lastIndexOf(".") + 1, oldLabel.length());
        int i = Integer.parseInt(suffix);
        return prefix + ++i;
    }
}
