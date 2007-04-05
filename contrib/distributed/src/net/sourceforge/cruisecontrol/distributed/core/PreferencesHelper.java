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

    /**
     * Implemented by UI classes that wish to persist state about the UI.
     */
    public static interface UIPreferences {
        /**
         * @return  a Preferences node specific to the implementing class.
         */
        public Preferences getPrefsBase();

        /**
         * @return The Windows who's attributes will be persisted or set.
         */
        public Window getWindow();
    }


    // Preferences Node Names, used to access Agent UI and Agent Utility UI preferences
    private static final String PREFS_NODE_SCREEN_POSITION = "screenPosition";
    private static final String PREFS_NODE_X = "x";
    private static final String PREFS_NODE_Y = "y";
    private static final String PREFS_NODE_SCREEN_SIZE = "screenSize";
    private static final String PREFS_NODE_WIDTH = "w";
    private static final String PREFS_NODE_HEIGHT = "h";


    /**
     * @param uiPrefs the UI instance who's preferences we wish to access
     * @return the prefs node in which the screen position settings exist.
     */
    private static Preferences getPrefNodeWindowLocation(final UIPreferences uiPrefs) {
        return uiPrefs.getPrefsBase().node(PREFS_NODE_SCREEN_POSITION);
    }
    /**
     * @param uiPrefs the UI instance who's preferences we wish to access
     * @return the prefs node in which the screen size settings exist.
     */
    private static Preferences getPrefNodeWindowSize(final UIPreferences uiPrefs) {
        return uiPrefs.getPrefsBase().node(PREFS_NODE_SCREEN_SIZE);
    }

    /**
     * @param uiPrefs the UI instance who's preferences we wish to access
     */
    public static void saveWindowInfo(final UIPreferences uiPrefs) {

        // save window info
        final Preferences prfLocation = getPrefNodeWindowLocation(uiPrefs);
        prfLocation.putInt(PREFS_NODE_X, (int) uiPrefs.getWindow().getLocation().getX());
        prfLocation.putInt(PREFS_NODE_Y, (int) uiPrefs.getWindow().getLocation().getY());

        final Preferences prfSize = getPrefNodeWindowSize(uiPrefs);
        prfSize.putInt(PREFS_NODE_WIDTH, uiPrefs.getWindow().getWidth());
        prfSize.putInt(PREFS_NODE_HEIGHT, uiPrefs.getWindow().getHeight());
    }

    /**
     * @param uiPrefs the UI instance who's preferences we wish to access
     */
    public static void applyWindowInfo(final UIPreferences uiPrefs) {

        // apply screen info from last run
        final Preferences prfLocation = getPrefNodeWindowLocation(uiPrefs);
        uiPrefs.getWindow().setLocation(
                prfLocation.getInt(PREFS_NODE_X, 0),
                prfLocation.getInt(PREFS_NODE_Y, 0));

        final Preferences prfSize = getPrefNodeWindowSize(uiPrefs);
        uiPrefs.getWindow().setSize(
                prfSize.getInt(PREFS_NODE_WIDTH, uiPrefs.getWindow().getWidth()),
                prfSize.getInt(PREFS_NODE_HEIGHT, uiPrefs.getWindow().getHeight()));

    }


    private PreferencesHelper () { }
}
