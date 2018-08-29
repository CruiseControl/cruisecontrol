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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Configuration {
    /* All keys used for recognizing settings */
    public static final String KEY_CONFIG_FILE = "configfile";
    public static final String KEY_LIBRARY_DIR = "library_dir";
    public static final String KEY_PROJECTS = "projects";
    public static final String KEY_ARTIFACTS = "artifacts";
    public static final String KEY_LOG_DIR = "logdir";
    public static final String KEY_LOG4J_CONFIG = "log4jconfig";
    public static final String KEY_NO_USER_LIB = "nouserlib";
    public static final String KEY_USER_LIB_DIRS = "lib";
    public static final String KEY_DIST_DIR = "dist";
    public static final String KEY_HOME_DIR = "home";
    public static final String KEY_PRINT_HELP1 = "help";
    public static final String KEY_PRINT_HELP2 = "?";
    public static final String KEY_DEBUG = "debug";
    public static final String KEY_RMI_PORT = "rmiport";
    public static final String KEY_PORT = "port"; // deprecated, use keyJMXPort
    public static final String KEY_JMX_PORT = "jmxport";
    public static final String KEY_WEB_PORT = "webport";
    public static final String KEY_WEBAPP_PATH = "webapppath";
    public static final String KEY_DASHBOARD = "dashboard";
    public static final String KEY_DASHBOARD_URL = "dashboardurl";
    public static final String KEY_POST_INTERVAL = "postinterval";
    public static final String KEY_POST_ENABLED = "postenabled";
    public static final String KEY_XLS_PATH = "xslpath";
    public static final String KEY_JETTY_XML = "jettyxml";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_USER = "user";
    public static final String KEY_CC_NAME = "ccname";
    public static final String KEY_JMX_AGENT_UTIL = "agentutil";
    private static final String KEY_IGNORE = "XXXX";

    /** Array of default values for all the option keys */
    private static final Option[] DEFAULT_OPTIONS = {
        new Option(KEY_CONFIG_FILE,    "cruisecontrol.xml",  File.class),
        new Option(KEY_LIBRARY_DIR,    "lib",                File[].class),
        new Option(KEY_PROJECTS,       "projects",           File.class),
        new Option(KEY_ARTIFACTS,      "artifacts",          File.class),
        new Option(KEY_LOG_DIR,        "logs",               File.class),
        new Option(KEY_LOG4J_CONFIG,    null,                URL.class),
        new Option(KEY_NO_USER_LIB,    "false",              Boolean.class),
        new Option(KEY_USER_LIB_DIRS,  "",                   File[].class),
        new Option(KEY_DIST_DIR,       "dist",               File.class),
        new Option(KEY_HOME_DIR,       ".",                  File.class),
        new Option(KEY_PRINT_HELP1,    "false",              Boolean.class),
        new Option(KEY_PRINT_HELP2,    "false",              Boolean.class),
        new Option(KEY_DEBUG,          "false",              Boolean.class),
        new Option(KEY_RMI_PORT,       "1099",               Integer.class),
        new Option(KEY_PORT,           "8000",               Integer.class),
        new Option(KEY_JMX_PORT,       "8000",               Integer.class),
        new Option(KEY_WEB_PORT,       "8080",               Integer.class),
        new Option(KEY_WEBAPP_PATH,    "/webapps/cruisecontrol",
                                                             File.class),
        new Option(KEY_DASHBOARD,      "/webapps/dashboard", File.class),
        new Option(KEY_DASHBOARD_URL,  "http://localhost:8080/dashboard",
                                                             URL.class),
        new Option(KEY_XLS_PATH,       ".",                  File.class),
        new Option(KEY_JETTY_XML,      "etc/jetty.xml",      File.class),
        new Option(KEY_POST_INTERVAL,  "5",                  Integer.class),
        new Option(KEY_POST_ENABLED,   "true",               Boolean.class),
        new Option(KEY_PASSWORD,       "",                   String.class),
        new Option(KEY_USER,           "",                   String.class),
        new Option(KEY_CC_NAME,        "",                   String.class),
        new Option(KEY_JMX_AGENT_UTIL, "",                   String.class),
        // Just placeholders
        new Option(KEY_IGNORE,         "",                   String.class),
        // Alternative to keyConfigFile set in Main.setUpSystemPropertiesForDashboard(). Don't know
        // why form from keyConfigFile is not used there ...
        new Option("config_file",      "",                   String.class)
    };

    // String used to separate items in the array-holding options
    public static final String ITEM_SEPARATOR = File.pathSeparator;
    // The home directory of the user we run under
    public static final File USER_HOMEDIR = new File(System.getProperty("user.home"));

    private static Configuration config = null; //instance
    private final Map<String, Option> options = new HashMap<String, Option>(DEFAULT_OPTIONS.length);

    // Must use "special" logger, since the log4j may not be initialized yet
    private static LogInterface log = new LogBuffer();

    /**
     * Initializes a singleton instance of ConfigLoader and returns this instance. First it looks for
     * configuration file specified in argument -configfile. It searches the file system in this order:
     * <ol>
     * <li>If the specified path is absolute and file exists then it uses this file.</li>
     * <li>If the specified path is not absolute but file is directly accessible then it uses this path.</li>
     * <li>If the file can not be found, it tries to locate it in home directory.</li>
     * </ol>
     * Properties are initialized with default values, config file values, system property values, and
     * arguments values. If the same property is specified in more places it uses the one with higher
     * priority. Priorities from the highest:
     * <ol>
     * <li>Program arguments</li>
     * <li>System -D properties</li>
     * <li>Configuration file</li>
     * </ol>
     *
     * @param args the command line arguments to process
     * @return the initialized instance of {@link Configuration}
     * @throws LaunchException
     */
    public static Configuration getInstance(final String[] args) throws LaunchException {
      if (config != null) {
        throw new LaunchException("Config was already initialized. Use getInstance()");
      }
      return new Configuration(args);
    }

    /**
     * Returns a singleton instance of ConfigLoader. Use this when ConfigLoader has been already initialized.
     * If not, initialize it by calling {@link #getInstance(String[])}.
     *
     * @return the instance of {@link Configuration} initialized by {@link #getInstance(String[])}.
     * @throws IllegalStateException when not initialized
     */
    public static Configuration getInstance() {
      if (config == null) {
        throw new IllegalStateException("You must first initialize ConfigLoader. Use getInstance(args)");
      }
      return config;
    }

    /**
     * Gets the current interface through which messages are logged. It is designed for the use when
     * real logger is not defined yet (i.e. not set by #set ], but something is required to be logged.
     *
     * It is not recommended for extensive logging! Also, all the messages logged through this logger
     * appears in the context of this class (and not in the context of the class logging the message).
     * Redesign your algorithm to use a real logged, if "real" logging is required.
     */
    public LogInterface getLogger() {
        return log;
    }

    /** It sets correct instance of "real logger" to log data through. All the data logged into the temporary
     *  logger will be pushed to the logger just being set. Once the real logger is set, it cannot be changed.
     *
     *  @param logger the real logger to log into
     * @throws LaunchException
     */
    public static void setRealLog(LogInterface logger) throws LaunchException {
       // Ignore, if the instance is the same
       if (logger.equals(log)) {
           log.warn("Trying to set the same logger, ignoring");
           return;
       }
       // Set
       log.flush(logger);
       log = logger;
    }

    /**
     * Checks, if the given option has been set or it it holds a default value.
     * @param key the key to check
     * @return <code>true</code> if the given option has been set, <code>false</code> if not so get
     * methods are going to return the hard-coded default value.
     */
    public boolean wasOptionSet(String key) {
        return options.containsKey(key);
    }

    /**
     * @param key the name of the option to search for.
     * @return the value of the option
     * @throws IllegalArgumentException when the option does not exist
     */
    public String getOptionRaw(String key) {
      return getOption(options, key).val;
    }

    /**
     * @param key the name of the option to search for.
     * @return the value of the option; gets <code>null</code> if the file does not exist
     * @throws IllegalArgumentException when the option does not exist or it is not a file
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
        throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
                + "' does not represent existing file!");
    }
    /**
     * @param key the name of the option to search for.
     * @param parent the directory to use as the parent when no absolute path is set in the option
     * @return the value of the option; gets <code>null</code> if the file does not exist
     * @throws IllegalArgumentException when the option does not exist or it is not a file
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
      throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
              + "' does not represent existing file!");
    }
    /**
     * @param key the name of the option to search for.
     * @return the value of the option; gets <code>null</code> if the directory does not exist
     * @throws IllegalArgumentException when the option does not exist or it is not a directory
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
        throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
                + "' does not represent existing directory!");
    }
    /**
     * @param key the name of the option to search for.
     * @param parent the directory to use as the parent when no absolute path is set in the option
     * @return the value of the option; gets <code>null</code> if the directory does not exist
     * @throws IllegalArgumentException when the option does not exist or it is not a directory
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
      throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
              + "' does not represent existing directory!");
    }

    /**
     * @param key the name of the option to search for.
     * @return the value of the option
     * @throws IllegalArgumentException when the option does not exist or it is no a string
     */
    public String getOptionStr(String key) {
        final Option opt = getOption(options, key);
        // must be file type
        if (String.class.equals(opt.type)) {
          return opt.val;
        }
        // The option is not a boolean
        throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
                + "' does not represent string!");
    }

    /**
     * @param key the name of the option to search for.
     * @return the value of the option
     * @throws IllegalArgumentException when the option does not exist or it is not a boolean value
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
     * @param key the name of the option to search for.
     * @return the value of the option
     * @throws IllegalArgumentException when the option does not exist or it is not an integer value
     */
    public int getOptionInt(String key) {
      final Option opt = getOption(options, key);
      // must be file type
      if (Integer.class.equals(opt.type)) {
        return Integer.parseInt(opt.val);
      }
      // The option is not an integer value
      throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
              + "' does not represent int value!");
    }

    /**
     * @param key the name of the option to search for.
     * @return the value of the option
     * @throws IllegalArgumentException when the option does not exist or it is not a valid URL
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
                    throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
                            + "' does not represent existing file!");
                }
            }
            // Return the URL
            return val;
        } catch (MalformedURLException e) {
            // Nothing here, exception will be thrown anyway
        }
      }
      // The option is not an integer value
      throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
              + "' does not represent URL value!");
    }

    /**
     * @param key the name of the option to search for.
     * @return the value of the option
     * @throws IllegalArgumentException when the option does not exist or it is no a string
     */
    public String[] getOptionStrArray(String key) {
        final Option opt = getOption(options, key);
        // must be file type
        if (String[].class.equals(opt.type)) {
          return opt.val.split(ITEM_SEPARATOR);
        }
        // The option is not a boolean
        throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
                + "' does not represent array of string!");
    }
    /**
     * @param key the name of the option to search for.
     * @param parent the directory to use as parent for non-absolute path settings
     * @return the value of the option
     * @throws IllegalArgumentException when the option does not exist or it is no a string
     */
    public File[] getOptionDirArray(final String key, final File parent) {
        final Option opt = getOption(options, key);
        // must be file type
        if (File[].class.equals(opt.type)) {
          final String [] vals = opt.val.split(ITEM_SEPARATOR);
          final File [] files = new File[vals.length];

          for (int i = 0; i < vals.length; i++) {
              files[i] = findFile(vals[i], parent);
          }
          return files;
        }
        // The option is not a boolean
        throw new IllegalArgumentException("Option '" + key + "' = '" + opt.val
                + "' does not represent array of files!");
    }

    /**
     * Constructor. It is hidden since the class can only be used as singleton, but protected to be
     * overridable for test purposes
     *
     * @param args the array of command line arguments passed to CC launcher
     * @throws LaunchException
     */
    protected Configuration(final String[] args) throws LaunchException {
      final Map<String, Option> temp = new HashMap<String, Option>(DEFAULT_OPTIONS.length);

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
     * Checks, if the given file is accessible and returns absolute path to it. If the file
     * cannot be read (i.e. it is not set by absolute path or not being in the working directory),
     * the home directory of the user is tried.
     *
     * @param fname the name (+ path) to read the file from
     * @param parent the path to use as parent when the directory is set as relative path and cannot be found
     *      in the current directory
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

            log.warn("Unable to find " + file.getAbsolutePath() + ", trying directory " + parent.getAbsolutePath());
            file = new File(parent, fname);

            if (!file.exists()) {
                log.warn("Unable to find " + fname);
                return null;
                //throw new LaunchException("Unable to find " + configFile);
            }
        }

        log.info("Using file: " + file.getAbsolutePath());
        return file;
    }

    /**
     * @param opts
     * @param set
     * @param xmlPath
     * @throws LaunchException
     */
    private void parseXmlConfig(final Map<String, Option> opts, final Option xmlPath)
            throws LaunchException {

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
            final Element launch = getChild(xmlConfig, "launcher");
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

    /* Methods for XML manipulation. They have their equivalents in org.jdom2.Element object,
     * but since "external" org.jdompackage is not used by the launcher (to avoid the need to
     * define path to external jars, we have to re-implement them here. */
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
     *
     * @param opts
     * @param args
     * @throws LaunchException
     */
    private static void parseArguments(final Map<String, Option> opts, final String[] args)
            throws LaunchException {

      String key = null;

      // Process the command line arguments
      for (final String arg : args) {

        // boolean flags process here
        if (arg.startsWith("-")) {
            final String name = arg.substring(1);

            if (KEY_PRINT_HELP1.equals(name)) {
                setOption(opts, name, "true");
                // continue with further processing since value can follow
            }
            if (KEY_PRINT_HELP2.equals(name)) {
                setOption(opts, name, "true");
            }
            if (KEY_NO_USER_LIB.equals(name)) {
                setOption(opts, name, "true");
            }
            if (KEY_DEBUG.equals(name)) {
                setOption(opts, name, "true");
            }
            if (KEY_POST_ENABLED.equals(name)) {
                setOption(opts, name, "true");
            }
            if (KEY_JMX_AGENT_UTIL.equals(name)) {
                setOption(opts, name, "true");
            }

            // This is little hack for backward compatibility. If the given option appears, set its
            // current value again to pretend that it was set on the command line; i.e. the call of
            // "-webport 8585 -dashboardurl" must pretend that the dashboardurl was set as well.
            if (KEY_DASHBOARD_URL.equals(name)) {
                log.warn("Using " + arg + " without value. Try to avoid this!");
                setOption(opts, name, "???");
            }
        }

        // -key value types
        // store the key
        if (arg.startsWith("-")) {
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
        // Unknown option
        throw new LaunchException("Unknown option " + arg);
      }
    }

    /**
     * Parses properties get by {@link System#getProperties()}, overriding already defined
     * values in <code>opts</code> attribute. Only properties starting with cc. are taken into the
     * consideration.
     *
     * Dots in the properties are replaced by underscores, so <i>cc.library.dir</i> becomes
     * <i>library_dir</i>
     * @param opts the map to fill options into
     */
    private static void parseProperties(final Map<String, Option> opts) {
        final Properties props = System.getProperties();

        for (String name : props.stringPropertyNames()) {
            if (name.startsWith("cc.")) {
                String key = name.substring(3).replace(".", "_");      // cc.library.dir -> library_dir
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
            if (o1 != null && ! o1.type.isArray()) {
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
          opt = getOption(Collections.EMPTY_MAP, key);
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
     * Finds the option according top the string key. If the option is not set in the given map,
     * the default value will be get.
     *
     * @param opts the map with options to be searched in
     * @param key the name of the option to be found
     * @return value the option; is never <code>null</code>
     * @throws IllegalArgumentException when the option key is unknown
     */
    private static Option getOption(final Map<String, Option> opts, final String key) {
      final Option opt = opts.get(key); //new Option(key, "", null));
      // Should already be pre-filled with a default value ...
      if (opt != null) {
          return opt;
      }
      // No, it wasnt' ... try to find the default value
      for (Option o : DEFAULT_OPTIONS) {
          if (o.key.equals(key)) {
              return o;
          }
      }
      // Option key not found ...
      throw new IllegalArgumentException("Unknown option '" + key + "'");
    }

    /**
     * Joins the strings, separating them by {@link #ITEM_SEPARATOR}. It correctly handles all the
     * "empty" string cases.
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
     * Single configuration option holder. It is hasheable by {@link #key} value
     */
    private static final class Option {
      final String key; /** Name of the option */
      final String val; /** The associated value */
      final Class< ? > type; /** The class the option type belongs to */

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

      /** Compares key to String or Option objects */
      @Override
      public boolean equals(Object obj) {
        if (obj instanceof Option) {
          return key.equals(((Option) obj).key);
        }
        return key.equals(obj);
      }
      /** Gets hash code of the key */
      @Override
      public int hashCode() {
        return this.key.hashCode();
      }
    }
}


