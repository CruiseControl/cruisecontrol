package net.sourceforge.cruisecontrol;

/**
 * .
 * User: jfredrick
 * Date: Sep 6, 2004
 * Time: 10:47:31 PM
 */
public interface Listener {

    public void handleEvent(ProjectEvent event) throws CruiseControlException;

    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException;
}
