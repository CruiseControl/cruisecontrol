package net.sourceforge.cruisecontrol;

import java.util.Date;

/**
 * This interface defines the method required to increment
 * the label used in the MasterBuild process. This label
 * is incorporated into the log filename when a successful
 * build occurs.
 * 
 * @author alden almagro (alden@thoughtworks.com), Paul Julius (pdjulius@thoughtworks.com), ThoughtWorks, Inc. 2001
 */
public interface LabelIncrementer {

    /**
     * Increments the label when a successful build occurs.
     * The oldLabel should be transformed and returned as
     * the new label.
     * 
     * @param oldLabel Label from previous successful build.
     * @return Label to use for most recent successful build.
     */
    public String incrementLabel(String oldLabel);
}
