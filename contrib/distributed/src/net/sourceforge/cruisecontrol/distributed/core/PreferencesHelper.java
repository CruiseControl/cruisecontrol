package net.sourceforge.cruisecontrol.distributed.core;

import java.util.prefs.Preferences;
import java.awt.Window;

/**
 * Helper class for access to various Preferences settings.
 *
 * @author Dan Rollo
 * Date: Apr 4, 2007
 * Time: 10:57:09 PM
 */
public final class PreferencesHelper {

    // Preferences Node Names, used to access Agent UI and Agent Utility UI preferences
    private static final String PREFS_NODE_SCREEN_POSITION = "screenPosition";
    private static final String PREFS_NODE_X = "x";
    private static final String PREFS_NODE_Y = "y";
    private static final String PREFS_NODE_SCREEN_SIZE = "screenSize";
    private static final String PREFS_NODE_WIDTH = "w";
    private static final String PREFS_NODE_HEIGHT = "h";


    /**
     * @param prefsBase the base prefs node under which the screen position settings exist.
     * @return the prefs node in which the screen position settings exist.
     */
    private static Preferences getPrefNodeWindowLocation(final Preferences prefsBase) {
        return prefsBase.node(PREFS_NODE_SCREEN_POSITION);
    }
    /**
     * @param prefsBase the base prefs node under which the screen size settings exist.
     * @return the prefs node in which the screen size settings exist.
     */
    private static Preferences getPrefNodeWindowSize(final Preferences prefsBase) {
        return prefsBase.node(PREFS_NODE_SCREEN_SIZE);
    }

    /**
     * @param window the window who's data we will store
     * @param prefsBase the base prefs node under which the window info settings exist.
     */
    public static void saveWindowInfo(final Window window, final Preferences prefsBase) {

        // save window info
        final Preferences prfLocation = getPrefNodeWindowLocation(prefsBase);
        prfLocation.putInt(PREFS_NODE_X, (int) window.getLocation().getX());
        prfLocation.putInt(PREFS_NODE_Y, (int) window.getLocation().getY());

        final Preferences prfSize = getPrefNodeWindowSize(prefsBase);
        prfSize.putInt(PREFS_NODE_WIDTH, window.getWidth());
        prfSize.putInt(PREFS_NODE_HEIGHT, window.getHeight());
    }

    /**
     * @param prefsBase the base prefs node under which the window info settings exist.
     * @param window the window who's attributes will be set to the previously stored values
     */
    public static void applyWindowInfo(final Preferences prefsBase, final Window window) {

        // apply screen info from last run
        final Preferences prfLocation = getPrefNodeWindowLocation(prefsBase);
        window.setLocation(
                prfLocation.getInt(PREFS_NODE_X, 0),
                prfLocation.getInt(PREFS_NODE_Y, 0));

        final Preferences prfSize = getPrefNodeWindowSize(prefsBase);
        window.setSize(
                prfSize.getInt(PREFS_NODE_WIDTH, window.getWidth()),
                prfSize.getInt(PREFS_NODE_HEIGHT, window.getHeight()));

    }


    private PreferencesHelper () { }
}
