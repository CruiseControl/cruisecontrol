/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.launch;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LaunchConfiguration implements Config {
    /* All keys used for recognizing settings */
    public static final String KEY_CONFIG_FILE = "configfile";
    public static final String KEY_LIBRARY_DIRS = "lib";
    public static final String KEY_LOG4J_CONFIG = "log4jconfig";
    public static final String KEY_NO_USER_LIB = "nouserlib";
    public static final String KEY_USER_LIB_DIRS = "user_lib";
    public static final String KEY_DIST_DIR = "dist";
    public static final String KEY_PROJ_DIR = "proj";

    private static final String KEY_IGNORE = "XXXX";

    /** Array of default values for all the option keys */
    private static final Option[] DEFAULT_OPTIONS = {
        new Option(KEY_CONFIG_FILE,    "cruisecontrol.xml",  File.class),
        new Option(KEY_LIBRARY_DIRS,   "lib",                File[].class),
        new Option(KEY_LOG4J_CONFIG,    null,                URL.class),
        new Option(KEY_NO_USER_LIB,    "false",              Boolean.class),
        new Option(KEY_USER_LIB_DIRS,   "",                  File[].class),
        new Option(KEY_DIST_DIR,        null,                File.class),
        new Option(KEY_PROJ_DIR,        null,                File.class),
        // Just placeholders
        new Option(KEY_IGNORE,         "",                   String.class),
        // Alternative to keyConfigFile set in Main.setUpSystemPropertiesForDashboard(). Don't know
        // why form from keyConfigFile is not used there ...
        new Option("config_file",      "",                   String.class)
    };

    // The home directory of the user we are running under
    public static final File USER_HOMEDIR = new File(System.getProperty("user.home"));
    // The patters used to match ${xxx} references
    private static final Pattern REF_CHECK = Pattern.compile("[$][{]([^}]+)[}]");

    private final Map<String, Option> options = new HashMap<String, Option>(DEFAULT_OPTIONS.length);
    // Must use "special" logger, since the log4j may not be initialized yet
    private final LogInterface log;
    // The instance which can call #setOption() method
    private final Object confOwner;

    /**
     * Gets the base help message for the given option key. The message consists of the option key, type
     * and its default value formated as a single line string
     * @param key the name of the option.
     * @return the help message
     */
    public static String getBaseHelp(String key) {
        // Find under the known options
        for (final Option o : DEFAULT_OPTIONS) {
            if (o.key.equals(key)) {
                String d = o.val;
                // Default according to the type
                if (d != null) {
                    if (o.type.equals(String.class)) {
                        d = "\"" + d + "\"";
                    }
                }
                // Get the formatted help
                return "-" + o.key + "[" + o.type.getSimpleName() + "]" + (d != null ? ", default=" + d : "");
            }
        }
        // Not found
        return "";
    }

    /**
     * Constructor initializing an instance of {@link LaunchConfiguration}. First it looks for
     * configuration file specified in argument -configfile. It searches the file system in this order:
     * <ol>
     * <li>If the specified path is absolute and file exists then it uses this file.</li>
     * <li>If the specified path is not absolute but file is directly accessible then it uses this path.</li>
     * <li>If the file can not be found, it tries to locate it in home directory.</li>
     * </ol>
     * Configuration options are initialized with default values, config file values, system property values,
     * and command line arguments values. If the same option is specified in more places it uses the one with
     * higher priority. Priorities from the highest:
     * <ol>
     * <li>Program arguments</li>
     * <li>System -D properties</li>
     * <li>Configuration file</li>
     * </ol>
     *
     * @param args the array of command line arguments passed to CC launcher
     * @param log the instance of {@link LogInterface} to write information details through
     * @param confOwner the object "owning" the configuration, i.e. it is allowed to call
     *          {@link #setOption(String, String, Object)} method.
     * @throws LaunchException
     */
    public LaunchConfiguration(final String[] args, LogInterface log, Object confOwner) throws LaunchException {
        final Map<String, Option> temp = new HashMap<String, Option>(DEFAULT_OPTIONS.length);

        // Fills the final attribs
        this.log = log;
        this.confOwner = confOwner;

        // Override default values with command-line options. This step is used to get the
        // path to launcher XML (ignore marking of set attributes)
        parseArguments(temp, args);
        // Store the config file
        final Option configFile = getOption(temp, KEY_CONFIG_FILE);
        final String configOpt = "-" + KEY_CONFIG_FILE;
        // And remove the option from the command line arguments now, since
        // - it is the main XML configuration with the configuration of launcher embedded in it; the
        //   path is already stored so overwrite would not
        // - it is raw launcher configuration containing path to the main XML config file; the path to
        //   the main cruisecontrol config will be read from launcher and thus we must prevent its
        //   re-overwrite from args
        for (int i = 0; i < args.length; i++) {
            if (configOpt.equals(args[i])) {
                args[i] = "-" + KEY_IGNORE;
            }
        }

        // Get values from command line (the highest priority)
        parseArguments(options, args);
        // Get values from properties (lower priority)
        temp.clear();
        parseProperties(temp);
        mergeOptions(options, temp);
        // Get the values from config (the lowest priority)
        temp.clear();
        parseXmlConfig(temp, configFile);
        mergeOptions(options, temp);
    }

    /**
     * Returns <code>true</code> when the option was explicitly set, <code>false</code> when the default
     * value will be used.
     *
     * @param key the key to check
     * @return <code>true</code> if the option has been set, <code>false</code> otherwise.
     */
    public boolean wasOptionSet(String key) {
        return options.containsKey(key);
    }

    /**
     * The implementation of {@link Config#knowsOption(String)}. In contary to {@link #wasOptionSet(String)},
     * this method returns <code>true</code> for both options which has been set and for options having a
     * default value.
     */
    @Override
    public boolean knowsOption(String key) {
        if (wasOptionSet(key)) {
            return true;
        }
        // Not set (yet), try defaults
        for (Option o : DEFAULT_OPTIONS) {
            if (o.key.equals(key)) {
                return true;
            }
        }
        // Unknown
        return false;
    }

    @Override
    public String getOptionRaw(String key) {
        return getOption(options, key).val;
    }

    /**
     *
     */
    @Override
    public Object getOptionType(String key, Class< ? > type) {
        Option opt = options.get(key);
        // Not set
        if (opt == null) {
            return null;
        }
        // Type check
        if (opt.type == File.class && type == opt.type) {
            return getOptionFile(key);
        }
        if (opt.type == URL.class && type == opt.type) {
            return getOptionUrl(key);
        }
        if (opt.type == Integer.class && type == opt.type) {
            return getOptionInt(key);
        }
        if (opt.type == Boolean.class && type == opt.type) {
            return getOptionInt(key);
        }
        return opt.val;
    }

    @Override
    public void setOption(String key, String val, Object owner) throws IllegalAccessError {
        if (owner != this.confOwner) {
            throw new IllegalAccessError("Invalid owner");

        }
        // Set the option
        Option opt = getOption(options, key);
        if (opt.type == File.class) {
           final File f = new File(val);

            if (!f.exists()) {
                throw new IllegalStateException("Trying to set not existing path: " + f.getAbsolutePath());
            }
            setOption(options, key, f.getAbsolutePath());
            return;
        }

        throw new IllegalAccessError("Explicit option setting is not supported for " + key + "=" + val);
    }

    @Override
    public Iterable<String> allOptionKeys() {
        final Set<String> keys = new HashSet<String>(options.size() + DEFAULT_OPTIONS.length);

        // Join all the option sources
        keys.addAll(options.keySet());
        for (Option o : DEFAULT_OPTIONS) {
            keys.add(o.key);
        }
        keys.remove(KEY_IGNORE);

        return keys;
    }

    /**
     * Gets option value as {@link File}. It checks if the file exists, trying it to find in the current working
     * directory first and in the user's home directory after that when not set an an absolute path
     *
     * @param key the required option
     * @return {@link File} instance pointing to an existing file
     * @throws IllegalArgumentException if the option does not represent {@link File}, or the file does not exist
     */
    public File getOptionFile(String key) {
        final Option opt = getOption(options, key);
        // must be file type
        if (File.class.equals(opt.type)) {
            File file;

            file = findFile(opt.val, new File("./"));
            // Must be existing file
            if (file != null && file.isFile()) {
                return file;
            }
            file = findFile(opt.val, USER_HOMEDIR);
            // Must be existing file
            if (file != null && file.isFile()) {
                return file;
            }
        }
        // The option is not file
        throw new IllegalArgumentException(
                "Option '" + key + "' = '" + opt.val + "' does not represent existing file!");
    }

    /**
     * Gets option value as {@link File}. It checks if the file exists, trying it to find under the given
     * directory if not absolute or in accessible path.
     *
     * @param key the name of the option to search for.
     * @param parent the directory to use as the parent when no absolute path is set in the option
     * @return {@link File} instance pointing to an existing file
     * @throws IllegalArgumentException if the option does not represent {@link File}, or the file does not exist
     */
    public File getOptionFile(String key, File parent) {
        final Option opt = getOption(options, key);
        // must be file type
        if (File.class.equals(opt.type)) {
            final File file = findFile(opt.val, parent);
            // Must be existing file
            if (file != null && file.isFile()) {
                return file;
            }
        }
        // The option is not file
        throw new IllegalArgumentException(
                "Option '" + key + "' = '" + opt.val + "' does not represent existing file!");
    }

    /**
     * Gets option value as {@link File}. It checks if the directory exists, trying it to find under the current
     * working directory first and in the user's home directory after that when not set an absolute path..
     *
     * @param key the required option
     * @return {@link File} instance pointing to an existing directory
     * @throws IllegalArgumentException if the option does not represent {@link File}, or the directory does not
     *           exist
     */
    public File getOptionDir(String key) {
        final Option opt = getOption(options, key);
        // must be file type
        if (File.class.equals(opt.type)) {
            File dir;

            dir = findFile(opt.val, new File("./"));
            // Must be existing directory
            if (dir != null && dir.isDirectory()) {
                return dir;
            }
            dir = findFile(opt.val, USER_HOMEDIR);
            // Must be existing directory
            if (dir != null && dir.isDirectory()) {
                return dir;
            }
        }
        // The option is not file
        throw new IllegalArgumentException(
                "Option '" + key + "' = '" + opt.val + "' does not represent existing directory!");
    }

    /**
     * Gets option value as {@link File}. It checks if the directory exists, trying it to find under the given
     * directory if not absolute or in accessible path.

     * @param key the name of the option to search for.
     * @param parent the directory to use as the parent when no absolute path is set in the option
     * @return {@link File} instance pointing to an existing file
     * @throws IllegalArgumentException if the option does not represent {@link File}, or the file does not exist
     */
    public File getOptionDir(String key, File parent) {
        final Option opt = getOption(options, key);
        // must be file type
        if (File.class.equals(opt.type)) {
            final File dir = findFile(opt.val, parent);
            // Must be existing directory
            if (dir != null && dir.isDirectory()) {
                return dir;
            }
        }
        // The option is not file
        throw new IllegalArgumentException(
                "Option '" + key + "' = '" + opt.val + "' does not represent existing directory!");
    }

    /**
     * Gets the list of {@link File} values for options allowing to set a list of directories.
     * It checks if the every of the directories exists, trying it to find under the given
     * directory if not absolute or in accessible path. Directories not found are logger but
     * ignored (not returned).
     *
     * @param key the required option
     * @param parent
     * @return the array of File instances pointing to existing directories
     * @throws IllegalArgumentException when the option does not represent file/directory list
     */
    public File[] getOptionDirArray(final String key, final File parent) {
        final Option opt = getOption(options, key);
        // must be file type
        if (File[].class.equals(opt.type)) {
            final String[] vals = opt.val.split(ITEM_SEPARATOR);
            final List<File> files = new ArrayList<File>(vals.length);

            for (int i = 0; i < vals.length; i++) {
                final File f = findFile(vals[i], parent);
                if (f != null) {
                    files.add(findFile(vals[i], parent));
                }
            }
            return files.toArray(new File[files.size()]);
        }
        // The option is not a boolean
        throw new IllegalArgumentException(
                "Option '" + key + "' = '" + opt.val + "' does not represent array of files!");
    }

    /**
     * Gets option value as {@link String}.
     * @param key the required option
     * @return String instance
     * @throws IllegalArgumentException if the option does not represent {@link String}
     */
    public String getOptionStr(String key) {
        final Option opt = getOption(options, key);
        // must be file type
        if (String.class.equals(opt.type)) {
            return opt.val;
        }
        // The option is not a boolean
        throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val + "' does not represent string!");
    }

    /**
     * Gets option value as {@link Boolean}.
     * @param key the required option
     * @return Integer instance
     * @throws IllegalArgumentException if the option does not represent {@link Boolean}
     */
    public boolean getOptionBool(String key) {
        final Option opt = getOption(options, key);
        // must be file type
        if (Boolean.class.equals(opt.type)) {
            return Boolean.parseBoolean(opt.val);
        }
        // The option is not a boolean
        throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
                + "' does not represent boolean value!");
    }

    /**
     * Gets option value as {@link Integer}.
     * @param key the required option
     * @return Integer instance
     * @throws IllegalArgumentException if the option does not represent {@link Integer}
     */
    public int getOptionInt(String key) {
        final Option opt = getOption(options, key);
        // must be file type
        if (Integer.class.equals(opt.type)) {
            return Integer.parseInt(opt.val);
        }
        // The option is not an integer value
        throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val + "' does not represent int value!");
    }

    /**
     * Gets option value as {@link URL}.
     * @param key the required option
     * @return Integer instance
     * @throws IllegalArgumentException if the option does not represent URL, or the URL is invalid
     */
    public URL getOptionUrl(String key) {
        final Option opt = getOption(options, key);
        // must be file type
        if (URL.class.equals(opt.type)) {
            try {
                final URL val = new URL(opt.val);
                final String protocol = val.getProtocol();
                final String host = val.getHost();

                // If the URL represents a file, check is as if it is a file
                if ("file".equalsIgnoreCase(protocol) && (host == null || host.isEmpty())) {
                    final File file = findFile(val.getPath(), new File("./"));
                    // Must be existing directory
                    if (file == null || !file.isFile()) {
                        throw new IllegalArgumentException(
                                "Option '" + key + "' = '" + opt.val + "' does not represent existing file!");
                    }
                }
                // Return the URL
                return val;
            } catch (MalformedURLException e) {
                // Nothing here, exception will be thrown anyway
            }
        }
        // The option is not an integer value
        throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val + "' does not represent URL value!");
    }

    /**
     * Checks, if the given file is accessible and returns absolute path to it. If the file
     * cannot be read (i.e. it is not set by absolute path or not being in the working directory),
     * it is tried to be found under the given parent directory.
     *
     * @param fname the name (+ path) of file to check
     * @param parent the path to use as parent when the directory is set as relative path and
     *          cannot be found in the current directory. If the parent is <code>null</code>,
     *          the current working directory is tried instead.
     * @return absolute path to the file or <code>null</code> if it cannot be found or read
     */
    private File findFile(final String fname, File parent) {
        if (fname == null) {
            return null;
        }

        File file = new File(fname);

        // If the file is not accessible (i.e. set by absolute path or in the current working directory),
        // try home directory of the user
        if (!file.isAbsolute() && !file.exists()) {
            if (parent == null || !parent.exists() || !parent.isDirectory()) {
                parent = new File("./"); // try current working directory ...
            }

            log.warn("Unable to find " + file.getAbsolutePath() + ", trying directory "
                    + new File(parent, fname).getAbsolutePath());
            file = new File(parent, fname);

            if (!file.exists()) {
                log.warn("Unable to find " + fname);
                return null;
            }
        }

        log.info("Using file: " + file.getAbsolutePath());
        return file;
    }

    /**
     * Parses the given XML file to fill the configuration options, overriding already defined
     * values in <code>opts</code> attribute.
     *
     * @param opts the map to fill options into
     * @param xmlPath the file to read
     * @throws LaunchException
     */
    private void parseXmlConfig(final Map<String, Option> opts, final Option xmlPath) throws LaunchException {

        // The path is NULL, just leave
        if (xmlPath == null || "".equals(xmlPath.val)) {
            return;
        }

        File path = findFile(xmlPath.val, new File("./"));
        // Not in the current
        if (path == null) {
            path = findFile(xmlPath.val, USER_HOMEDIR);
        }
        // File cannot be found, skip its reading
        if (path == null) {
            log.warn("Skipping the read of config file, using default values!");
            return;
        }

        Element xmlConfig;
        // Read the config. Use standard Java's XML tools to avoid the dependency ion an external
        // XML handling package (although net.sourceforge.cruisecontrol.util.Util class contains
        // more advanced CML parsers. Could be nice to join them ...)
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            xmlConfig = builder.parse(path).getDocumentElement();
        } catch (Exception e) {
            throw new LaunchException("Failed to read XML file:" + path.getAbsolutePath(), e);
        }
        // The root element is <cruisecontrol>, find <launcher>...</launcher> section in it
        // and parse recursively
        if ("cruisecontrol".equals(xmlConfig.getNodeName())) {
            final Element launch = getChild(xmlConfig, "launch");
            // Not found!
            if (launch == null) {
                throw new LaunchException("No launcher configuration found in the XML config");
            }

            // Remove the option from the <launch> element since the config is this file
            removeChild(launch, KEY_CONFIG_FILE); // Remove it in case that it will be set
            // Set the path to the file and continue with parsing the launcher element
            setOption(opts, KEY_CONFIG_FILE, path.getAbsolutePath());
            xmlConfig = launch;
        }

        // Parse options from <launcher>...</launcher> element
        for (final Node child : getChildNodes(xmlConfig)) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final Element elem = (Element) child;
            final String key = elem.getNodeName();
            final String val = elem.getTextContent().trim();

            setOption(opts, key, val);
        }
    }
    /*
     * Methods for XML manipulation. They have their equivalents in org.jdom2.Element object,
     * but since "external" org.jdompackage is not used by the launcher (to avoid the need to
     * define path to external jars, we have to re-implement them here.
     */
    private static Element getChild(final Element xmlNode, final String name) {
        for (final Node child : getChildNodes(xmlNode)) {
            if (name.equals(child.getNodeName()) && child.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) child;
            }
        }
        return null;
    }
    private static void removeChild(final Element xmlNode, final String name) {
        for (final Node child : getChildNodes(xmlNode)) {
            if (name.equals(child.getNodeName()) && child.getNodeType() == Node.ELEMENT_NODE) {
                xmlNode.removeChild(child);
            }
        }
    }
    private static Node[] getChildNodes(final Element xmlNode) {
        NodeList list = xmlNode.getChildNodes();
        Node[] nodes = new Node[list.getLength()];

        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = list.item(i);
        }
        return nodes;
    }

    /**
     * Parses the command line arguments to fill the configuration options, overriding already defined
     * values in <code>opts</code> attribute.
     *
     * @param opts the map to fill options into
     * @param args the command line argument to process
     * @throws LaunchException
     */
    private void parseArguments(final Map<String, Option> opts, final String[] args) throws LaunchException {

        String key = null;

        // Process the command line arguments
        for (final String arg : args) {
            // -key value types
            // store the key
            if (arg.startsWith("-")) {
                // If there is a key
                if (key != null) {
                    setOption(opts, key, Boolean.toString(true));
                }
                // Store the current key
                key = arg.substring(1);
                continue;
            }
            // Store the value into temporary since the option may contain array which is merged later
            // on (prepending the option to already defined values)
            if (key != null) {
                setOption(opts, key, arg);

                key = null;
                continue;
            }
        }

        // Args ends with "-option"
        if (key != null) {
            setOption(opts, key, Boolean.toString(true));
        }
    }

    /**
     * Parses properties get by {@link System#getProperties()}, overriding already defined
     * values in <code>opts</code> attribute. Only properties starting with cc. are taken into the
     * consideration.
     *
     * Dots in the properties are replaced by underscores, so <i>cc.library.dir</i> becomes
     * <i>library_dir</i>
     *
     * @param opts the map to fill options into
     */
    private void parseProperties(final Map<String, Option> opts) {
        final Properties props = System.getProperties();

        for (String name : props.stringPropertyNames()) {
            if (name.startsWith("cc.")) {
                String key = name.substring(3).replace(".", "_"); // cc.library.dir -> library_dir
                setOption(opts, key, props.getProperty(name));
            }
        }
    }

    /**
     * Merges options from <code>opt2</code> to <code>opt1</code>, if the <code>opt1</code> does not contain
     * the value (i.e. <code>opt1</code> has higher priority than <code>opt2</code>). In case of array options,
     * the values from <code>opt2</code> are added to the end of <code>opt1</code> options.
     *
     * @param opt1 the resulting options map
     * @param opt2 the options map to merge
     */
    private void mergeOptions(final Map<String, Option> opt1, final Map<String, Option> opt2) {
        // Merge secondary to primary
        for (final String key : opt2.keySet()) {
            Option o1 = opt1.get(key); // Primary
            Option o2 = opt2.get(key); // Secondary

            // Already in the primary and not an array
            if (o1 != null && !o1.type.isArray()) {
                continue;
            }
            // Key not yet in the primary options
            if (o1 == null) {
                o1 = o2;
            }
            // Option type mismatch ??!!
            if (o1.type != o2.type) {
                log.warn("Option type mismatch for " + key + ": " + o1.type + " != " + o2.type);
                continue;
            }
            // Set
            setOption(opt1, o1.key, o2.val);
        }
    }

    /**
     * Creates new {@link Option} object and stores it into the map of options. When the given option exists
     * in the map, it is overwritten. If it exists and is an array option, the new value is added to the end
     * of the options already set.
     *
     * @param opts the map with options to be updated
     * @param key the name of the option
     * @param val the value of the option
     */
    private static void setOption(final Map<String, Option> opts, final String key, String val) {
        Option opt = opts.get(key);

        // Not found, get option pattern (but not the value!) from the default settings
        if (opt == null) {
            opt = getOption(Collections.<String, Option>emptyMap(), key);
            opt = new Option(opt, ""); // Option without value to prevent join in case of array
        }
        // If the option is array, add the value to its end
        if (opt.type.isArray()) {
            val = joinArray(opt.val, val);
        }
        // set
        opt = new Option(opt, val);
        opts.put(opt.key, opt);
    }

    /**
     * Finds the option according to the string key. If the option is not set in the given map,
     * the default value will be get.
     *
     * @param opts the map with options to be searched in
     * @param key the name of the option to be found
     * @return value the option; is never <code>null</code>
     */
    private static Option getOption(final Map<String, Option> opts, final String key) {
        return getOption(opts, key, 0);
    }
    // Method which actually carries out the work of #getOption(Map<String, Option>, String). It also checks,
    // if the recursive call is not too deep ...
    private static Option getOption(final Map<String, Option> opts, final String key, int iRecursCntr) {
        final Option opt = opts.get(key);
        // Should already be pre-filled with a default value ...
        if (opt != null) {
            final Matcher match = REF_CHECK.matcher(opt.val);
            // Does it contain ${...} pattern?
            if (!match.find()) {
                return opt;
            }
            // Check loop (simulated by too deep recursion check)
            if (iRecursCntr > 3) {
                throw new IllegalStateException("Too deep recursion");
            }
            // Get the option and replace the match by its value
            final Option op = getOption(opts, match.group(1), ++iRecursCntr);
            return new Option(opt, match.replaceAll(op.val));
        }
        // No, it wasnt' ... try to find the default value
        for (Option o : DEFAULT_OPTIONS) {
            if (o.key.equals(key)) {
                return o;
            }
        }
        // Option key not found ... Build the new one
        return new Option(key, null, String.class);
    }

    /**
     * Joins the strings, separating them by {@link #ITEM_SEPARATOR}. It correctly handles all the
     * "empty" string cases.
     *
     * @param v1 the first string
     * @param v2 the second string
     * @return the joined options
     */
    private static String joinArray(final String v1, final String v2) {
        if (v1 == null || v1.length() == 0) {
            return v2;
        }
        if (v2 == null || v2.length() == 0) {
            return v1;
        }
        return v1 + ITEM_SEPARATOR + v2;
    }

    /**
     * Single configuration option holder.
     */
    private static final class Option {
        /** Name of the option */
        final String key;
        /** The associated value */
        final String val;
        /** The class the option type belongs to */
        final Class< ? > type;

        Option(final String key, final String val, Class< ? > type) {
            this.key = key;
            this.val = val;
            this.type = type;
        }

        Option(final Option opt, final String val) {
            this.key = opt.key;
            this.val = val;
            this.type = opt.type;
        }
    }
}
