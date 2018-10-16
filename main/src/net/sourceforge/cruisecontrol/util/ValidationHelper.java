package net.sourceforge.cruisecontrol.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Reusable assertion like facility for handling configuration mistakes (e.g. unsupported/required attributes).
 *
 * @author <a href="mailto:jerome@coffeebreaks.org">Jerome Lacoste</a>
 */
public final class ValidationHelper {
    private ValidationHelper() {
    }

    /**
     * Handle required plugin attributes.
     */
    public static void assertIsSet(final Object attribute, final String attributeName, final Class plugin)
            throws CruiseControlException {
        assertIsSet(attribute, attributeName, getShortClassName(plugin));
    }

    /**
     * Handle required plugin attributes.
     */
    public static void assertIsSet(final Object attribute, final String attributeName, final String pluginName)
            throws CruiseControlException {
        if (attribute == null) {
            fail("'" + attributeName + "' is required for " + pluginName);
        }
    }

    /**
     * 
     * @param masterAttribute
     * @param childAttribute
     * @param plugin
     * @throws CruiseControlException
     */
    public static void assertIsDependentSet(String masterAttribute, String masterAttributeName, String childAttribute,
            String childAttributeName, Class plugin) throws CruiseControlException {
        if (masterAttribute != null) {
            if (childAttribute == null) {
                fail("'" + childAttributeName + "' is required for " + getShortClassName(plugin) + " if '"
                        + masterAttributeName + "' is set");
            }
        }
    }

    /**
     * Handle required plugin attributes.
     * 
     * @throws CruiseControlException
     *             if empty (null OK)
     */
    public static void assertNotEmpty(final String attribute, final String attributeName, final Class plugin)
            throws CruiseControlException {
        assertTrue(attribute == null || !"".equals(attribute), attributeName
                + " must be meaningful or not provided on " + getShortClassName(plugin));
    }

    /**
     * Handle required plugin child elements.
     */
    public static void assertHasChild(final Object child, final Class childType,
        final String usualChildNodeName, final Class plugin) throws CruiseControlException {
        if (child == null) {
            fail("child <" + usualChildNodeName + "> (or type " + getShortClassName(childType) 
                 + ") is required for plugin " + getShortClassName(plugin));
        }
    }

    /**
     * Handle required plugin child elements.
     */
    public static void assertHasChild(final Object child,
        final String usualChildNodeName, final Class plugin) throws CruiseControlException {
        if (child == null) {
            fail("child <" + usualChildNodeName + "> is required for plugin " + getShortClassName(plugin));
        }
    }

    public static void assertFalse(boolean condition, String message, final Class plugin)
        throws CruiseControlException {
        if (condition) {
            fail(message + " for plugin " + getShortClassName(plugin));
        }
    }
    public static void assertTrue(boolean condition, String message, final Class plugin)
        throws CruiseControlException {
        if (!condition) {
            fail(message + " for plugin " + getShortClassName(plugin));
        }
    }

    public static void assertTrue(boolean condition, String message) throws CruiseControlException {
        if (!condition) {
            fail(message);
        }
    }

    public static void fail(String message) throws CruiseControlException {
        throw new CruiseControlException(message);
    }

    public static void fail(String message, Exception e) throws CruiseControlException {
        throw new CruiseControlException(message, e);
    }

    public static void assertFalse(boolean condition, String message) throws CruiseControlException {
        if (condition) {
            fail(message);
        }
    }

    /**
     * The short class name of an object.
     * @return The short class name
     * @throws NullPointerException if object is null
     */
    private static String getShortClassName(final Class plugin) {
        final String fullClassName = plugin.getName();
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    public static void assertExists(File file, String attributeName, Class plugin) throws CruiseControlException {
        if (file == null || attributeName == null || plugin == null) {
            throw new IllegalArgumentException("All parameters are required.");
        }

        if (!file.exists()) {
            fail("File specified [" + file.getAbsolutePath() + "] for attribute [" + attributeName + "] on plugin ["
                    + plugin.getName() + "] doesn't exist.");
        }
    }

    public static void assertNotExists(File file, String attributeName, Class plugin) throws CruiseControlException {
        if (file == null || attributeName == null || plugin == null) {
            throw new IllegalArgumentException("All parameters are required.");
        }

        if (file.exists()) {
            fail("File specified [" + file.getAbsolutePath() + "] for attribute [" + attributeName + "] on plugin ["
                    + plugin.getName() + "] must not exist.");
        }
    }

    public static void assertIsNotDirectory(File file, String attributeName, Class plugin)
            throws CruiseControlException {
        if (file == null || attributeName == null || plugin == null) {
            throw new IllegalArgumentException("All parameters are required.");
        }

        if (file.isDirectory()) {
            fail("File specified [" + file.getAbsolutePath() + "] for attribute [" + attributeName + "] on plugin ["
                    + plugin.getName() + "] is really a directory where a file was expected.");
        }
    }

    public static void assertIsReadable(File file, String attributeName, Class plugin) throws CruiseControlException {
        if (file == null || attributeName == null || plugin == null) {
            throw new IllegalArgumentException("All parameters are required.");
        }

        if (!file.canRead()) {
            fail("File specified [" + file.getAbsolutePath() + "] for attribute [" + attributeName + "] on plugin ["
                    + plugin.getName() + "] is not readable.");
        }
    }

    public static void assertIntegerInRange(String candidate, int start, int end, String message)
            throws CruiseControlException {
        try {
            int asInt = Integer.parseInt(candidate);
            if (asInt < start || asInt > end) {
                fail(message);
            }
        } catch (NumberFormatException e) {
            fail(message);
        }
    }

    /**
     * Assertion for encoding string (must be recognised by {@link InputStreamReader} constructor)
     * @param encoding
     * @param plugin
     * @throws CruiseControlException if encoding is invalid
     */
    public static void assertEncoding(String encoding, Class plugin)
            throws CruiseControlException {
        if (encoding == null || plugin == null) {
            throw new IllegalArgumentException("All parameters are required.");
        }

        try {
            new InputStreamReader(new ByteArrayInputStream(new byte[10]), encoding).close();
        } catch (UnsupportedEncodingException e) {
            fail("Encoding " + encoding + " not supported on plugin [" + plugin.getName() + "]", e);
        } catch (IOException e) {
            fail("Failed", e);
        }
    }
}
