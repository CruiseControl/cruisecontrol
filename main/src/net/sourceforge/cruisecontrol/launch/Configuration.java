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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
        new Option(KEY_LIBRARY_DIR,    "lib",                File.class),
        new Option(KEY_PROJECTS,       "projects",           File.class),
        new Option(KEY_ARTIFACTS,      "artifacts",          File.class),
        new Option(KEY_LOG_DIR,        "logs",               File.class),
        new Option(KEY_LOG4J_CONFIG,    null,                URL.class),
        new Option(KEY_NO_USER_LIB,    "false",              Boolean.class),
        new Option(KEY_USER_LIB_DIRS,  "",                   String[].class), // File[] is not supported yet
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

    private static Configuration config = null; //instance
    private final Map<Object, Option> options = new HashMap<Object, Option>(DEFAULT_OPTIONS.length);
    private final Set<Object> optionsSet = new HashSet<Object>(DEFAULT_OPTIONS.length / 2);

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
     * checks, if the given option has been set or it it holds a default value.
     *
     * @return <code>true</code> if the given option has been set, <code>false</code> if not so get
     * methods are going to return the hard-coded default value.
     */
    public boolean wasOptionSet(String key) {
        return optionsSet.contains(key);
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
          final File file = findFile(opt.val);
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
        final File dir = findFile(opt.val);
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
                final File file = findFile(val.getPath());
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
     * Constructor. It is hidden since the class can only be used as singleton, but protected to be
     * overidable for test purposes
     *
     * @param args the array of command line arguments passed to CC launcher
     * @throws LaunchException
     */
    protected Configuration(final String[] args) throws LaunchException {
      final Set<Object> dummy = new HashSet<Object>();

      // Initialize the configuration options with default values
      for (Option o : DEFAULT_OPTIONS) {
        options.put(o.key, o); // WARN: must put string as a key, Map<>.get() does not work for key == Option
      }

      // Override default values with command-line options. This step is used to get the
      // path to launcher XML (ignore marking of set attributes)
      parseArguments(options, dummy, args);
      // But remove the option from the command line arguments now, since
      // - it is the main XML configuration with the configuration of launcher embedded in it; the
      //   path is already stored so overwrite would not
      // - it is raw launcher configuration containing path to the main XML config file; the path to
      //   the main cruisecontrol config will be read from launcher and thus we must prevent its
      //   re-overwrite from args
      for (int i = 0; i < args.length; i++) {
          if (("-" + KEY_CONFIG_FILE).equals(args[i])) {
              args[i] = "-" + KEY_IGNORE;
          }
      }
      // Override the values from config
      parseXmlConfig(options, optionsSet, options.get(KEY_CONFIG_FILE));
      // Override values from properties
      parseProperties(options, optionsSet);
      // Override values from command line (to overwrite values overwritten by the config :-))
      parseArguments(options, optionsSet, args);
    }

    /**
     * Checks, if the given file is accessible and returns absolute path to it. If the file
     * cannot be read (i.e. it is not set by absolute path or not being in the working directory),
     * the home directory of the user is tried.
     *
     * @param fname the name (+ path) to read the file from
     * @return absolute path to the file or <code>null</code> if it cannot be found or read
     */
    private File findFile(final String fname) {
        File file = new File(fname);

        // If the file is not accessible (i.e. set by absolute path or in the current working directory),
        // try home directory of the user
        if (!file.isAbsolute() && !file.exists()) {
            final String home = System.getProperty("user.home");

            log.warn("Unable to find " + file.getAbsolutePath() + ", trying home directory " + home);
            file = new File(home, fname);

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
    private void parseXmlConfig(final Map<Object, Option> opts, final Set<Object> set, final Option xmlPath)
            throws LaunchException {
        // The path is NULL, just leave
        if (xmlPath == null || "".equals(xmlPath.val)) {
            return;
        }

        Element xmlConfig;
        File path = findFile(xmlPath.val);

        // File cannot be found, skip its reading
        if (path == null) {
            log.warn("Skipping the read of config file, using default values!");
            return;
        }

        // Read the config. Use standard Java's XML tools to avoid the dependency ion an external
        // XML handling package (although net.sourceforge.cruisecontrol.util.Util class contains
        // more advanced CML parsers. Could be nice to join them ...)
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
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
            setOption(opts, set, KEY_CONFIG_FILE, path.getAbsolutePath());
            xmlConfig = launch;
        } else {
            // Remove element pointing to the config file - initialize it with the default value, and it
            // optionally will be re-read from the values just going to be parsed
            for (Option opt : DEFAULT_OPTIONS) {
                if (KEY_CONFIG_FILE.equals(opt.key)) {
                    opts.put(opt.key, opt);
                    set.remove(opt.key);
                }
            }
        }

        // Parse options from <launcher>...</launcher> element
        for (final Node child : getChildNodes(xmlConfig)) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final Element elem = (Element) child;
            final String key = elem.getNodeName();
            final String val = elem.getTextContent().trim();

            setOption(opts, set, key, val);
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
    private void parseArguments(final Map<Object, Option> opts, final Set<Object> set, final String[] args)
            throws LaunchException {

      String key = null;

      // Process the command line arguments
      for (String arg : args) {
        // boolean flags process here
        if (arg.startsWith("-")) {
            final String name = arg.substring(1);

            if (KEY_PRINT_HELP1.equals(name)) {
                setOption(opts, set, name, "true");
                // continue with further processing since value can follow
            }
            if (KEY_PRINT_HELP2.equals(name)) {
                setOption(opts, set, name, "true");
            }
            if (KEY_NO_USER_LIB.equals(name)) {
                setOption(opts, set, name, "true");
            }
            if (KEY_DEBUG.equals(name)) {
                setOption(opts, set, name, "true");
            }
            if (KEY_POST_ENABLED.equals(name)) {
                 setOption(opts, set, name, "true");
            }
            if (KEY_JMX_AGENT_UTIL.equals(name)) {
                 setOption(opts, set, name, "true");
            }

            // This is little hack for backward compatibility. If the given option appears, set its
            // current value again to pretend that it was set on the command line; i.e. the call of
            // "-webport 8585 -dashboardurl" must pretend that the dashboardurl was set as well.
            if (KEY_DASHBOARD_URL.equals(name)) {
                log.warn("Using " + arg + " without value. Try to avoid this!");
                setOption(opts, set, name, getOption(opts, name).val);
            }
        }

        // -key value types
        // store the key
        if (arg.startsWith("-")) {
          key = arg.substring(1);
          continue;
        }
        // Store the value
        if (key != null) {
          setOption(opts, set, key, arg);
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
     */
    private void parseProperties(final Map<Object, Option> opts, final Set<Object> set) throws LaunchException {
        final Properties props = System.getProperties();

        for (String name : props.stringPropertyNames()) {
            if (name.startsWith("cc.")) {
                String key = name.substring(3).replace(".", "_");      // cc.library.dir -> library_dir
                setOption(opts, set, key, props.getProperty(name));
            }
        }
    }

    /**
     * Creates new {@link Option} object, stores it into the map of options as well as into the set of
     * options being set. An object with the given key must exist in the map, otherwise {@link LaunchException}
     * is thrown. In this way, default (or previous) values are overwritten and the correctness of the key is
     * checked (i.e. all keys must have default values assigned).
     *
     * @param opts the map with options to be updated
     * @param set the set with options being set
     * @param key the name of the option
     * @param val the value of the option
     */
    private static void setOption(final Map<Object, Option> opts, final Set<Object> set, final String key,
            String val) {
      Option opt = getOption(opts, key);

      // If the option is array and a value has already been set into it, must be treated differently
      if (opt.type.isArray() && set.contains(opt.key)) {
          // CAREFUL - the value must not contain the separator char
          if (val.contains(ITEM_SEPARATOR)) {
              throw new IllegalArgumentException("'" + ITEM_SEPARATOR + "' is not allowed in '"
                      + key + "' = '" + val + "'");
          }
          // Join the original options with the new option
          val = opt.val + ITEM_SEPARATOR + val;
      }

      opt = new Option(opt, val);
      opts.put(opt.key, opt); // WARN: must put string as a key, Map<>.get() does not work for key == Option
      set.add(opt.key);
    }

    /**
     * Finds the option according top the string key.
     *
     * @param opts the map with options to be searched in
     * @param key the name of the option to be found
     * @return value the option; is never <code>null</code>
     */
    private static Option getOption(final Map<Object, Option> opts, final String key) {
      final Option opt = opts.get(key); //new Option(key, "", null));
      // Must already be pre-filled with a default value!
      if (opt == null) {
        throw new IllegalArgumentException("Unknown option '" + key + "'");
      }
      return opt;
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


