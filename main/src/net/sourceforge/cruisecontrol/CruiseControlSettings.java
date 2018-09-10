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
package net.sourceforge.cruisecontrol;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;

import net.sourceforge.cruisecontrol.launch.Config;

public class CruiseControlSettings implements Config {

    /* All keys used for recognizing settings */
    public static final String KEY_CONFIG_FILE = "configfile";
    public static final String KEY_DIST_DIR = "dist";
    public static final String KEY_PROJ_DIR = "proj";
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

    private static CruiseControlSettings config = null; // instance
    private static final Logger LOG = Logger.getLogger(CruiseControlSettings.class);

    /** Array of default values for all the option keys */
    private static final Option[] DEFAULT_OPTIONS = {
        new Option(KEY_CONFIG_FILE,   new File("config.xml"),  File.class,
                 "configuration file; default config.xml"),
        new Option(KEY_DIST_DIR,          null,                File.class,
                 "The root directory under which the CriseControl was installed. Required to be set."),
        new Option(KEY_PROJ_DIR,          null,                File.class,
                 "The root directory where CruiseControl projects are located. Required to be set."),
        new Option(KEY_PRINT_HELP1,       Boolean.FALSE,              Boolean.class,
                 "Prints this help message"),
        new Option(KEY_PRINT_HELP2,       Boolean.FALSE,              Boolean.class,
                 "Prints this help message"),
        new Option(KEY_DEBUG,             Boolean.FALSE,              Boolean.class,
                 "Set logging level to DEBUG"),
        new Option(KEY_RMI_PORT,      new Integer(1099),               Integer.class,
                 "RMI port of the Controller; default 1099"),
        new Option(KEY_PORT,          new Integer(8000),               Integer.class,
                 "Deprecated. Use -" + KEY_JMX_PORT + " instead"),
        new Option(KEY_JMX_PORT,      new Integer(8000),               Integer.class,
                 "Port of the JMX HttpAdapter; default 8000"),
        new Option(KEY_WEB_PORT,      new Integer(8080),               Integer.class,
                 "Port for the Reporting website; default 8080, removing this propery will make "
               + "cruisecontrol start without Jetty"),
        new Option(KEY_JETTY_XML,     new File("etc/jetty.xml"),      File.class,
                 "Jetty configuration xml. Defaults to jetty.xml"),
        new Option(KEY_WEBAPP_PATH,   new File("/webapps/cruisecontrol"),
                                                             File.class,
                 ""),
        new Option(KEY_DASHBOARD,     new File("/webapps/dashboard"), File.class,
                 ""),
        new Option(KEY_DASHBOARD_URL,      "http://localhost:8080/dashboard",
                                                             URL.class, // TODO: URL!!!
                 "the url for dashboard (used for posting build information),"
               + "default is http://localhost:8080/dashboard\","),
        new Option(KEY_XLS_PATH,      new File("."),                  File.class,
                 "location of jmx xsl files; default files in package,"
               + "CruiseControlSettings.KEY_JMX_AGENT_UTIL"
               + "[true/false] load JMX Build Agent utility; default is true."),
        new Option(KEY_POST_INTERVAL, new Integer(5),                  Integer.class,
                 "how frequently build information will be posted to dashboard,,"
               + "default is 5 (in seconds)."),
        new Option(KEY_POST_ENABLED,      Boolean.TRUE,               Boolean.class,
                 "TODO"),
        new Option(KEY_PASSWORD,          null,                 String.class,
                 "password for HttpAdapter; default no login required"),
        new Option(KEY_USER,              null,                   String.class,
                 "username for HttpAdapter; default no login required"),
        new Option(KEY_CC_NAME,           "",                   String.class,
                 "A logical name which will be displayed in the "
               + "reporting Application's status page."),
        new Option(KEY_JMX_AGENT_UTIL,    "",                   String.class,
                 "TODO"),
    };

    /** The holder of options */
    private final Map<String, Option> options = new HashMap<String, Option>();
    /** Object allowing to call {@link #setOption(String, String, Object)} */
    private final Object owner;

    /**
     * Initializes a singleton instance of {@link CruiseControlSettings} and returns this instance.
     *
     * @param owner the object allowing to change the configuration through {@link #setOption(String, String, Object)}
     * @return the initialized instance of {@link CruiseControlSettings}
     * @throws CruiseControlException
     */
    public static CruiseControlSettings getInstance(final Object owner) throws CruiseControlException {
        if (config != null) {
            throw new CruiseControlException("Settings was already initialized. Use getInstance()");
        }
        config = new CruiseControlSettings(owner);
        return config;
    }

    /**
     * Returns a singleton instance of {@link CruiseControlSettings}, if initiaized by
     * {@link #getInstance(Object)}.
     *
     * @return the instance of {@link CruiseControlSettings}
     * @throws CruiseControlException when not initialized
     */
    public static CruiseControlSettings getInstance() throws CruiseControlException {
        if (config == null) {
            throw new CruiseControlException("You must first initialize Settings. Use getInstance(args)");
        }
        return config;
    }

    @Override
    public Iterable<String> allOptionKeys() {
        return options.keySet();
    }

    @Override
    public boolean knowsOption(String key) {
        for (Option o : DEFAULT_OPTIONS) {
            if (o.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean wasOptionSet(String key) {
        return options.containsKey(key);

        // TODO: pokud ji nezna tak vrati default
    }

    @Override
    public String getOptionRaw(String key) {
        return getOption(key).val.toString();
    }

    @Override
    public Object getOptionType(String key, Class< ? > type) {
        final Option opt = getOption(key);
        if (opt.val == null) {
            return null;
        }
        // Does the type match?
        if (type == opt.type) {
            return opt.val;
        }
        Log.warn("Option type mismatch for '" + key + "': " + opt.type.getName() + " != " + type.getName());
        return null;
    }

    @Override
    public void setOption(String key, String val, Object owner) throws IllegalAccessError {
        // Allowed?
        if (owner != this.owner) {
            throw new IllegalAccessError("Wrong owner ...");
        }
        // null passed. Will use the default value
        if (val == null) {
            options.remove(key);
            return;
        }

        // Try to find the option
        Option opt = getOption(key);
        Object v = null;
        // Convert the value o the given type
        if (opt.type == File.class) {
            File f = new File(val);
            if (!f.exists()) {
                throw new IllegalArgumentException(key + "=" + val + ": file does not exist");
            }
            v = f;
        }
        if (opt.type == String.class) {
            v = val;
        }
        if (opt.type == URL.class) {
            try {
                v = new URL(val);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(key + "=" + val + ": invalid value", e);
            }
        }
        if (opt.type == Integer.class) {
            v = Integer.valueOf(val);
        }
        if (opt.type == Boolean.class) {
            v = Boolean.valueOf(val);
        }
        // Unknown
        if (v == null) {
            throw new IllegalArgumentException(key + "=" + val + ": unknown option type");
        }

        // And put it with changed value
        options.put(key, new Option(opt, v));
    }

    /**
     * Helper around {@link #getOptionType(String, Class)} with Class being a {@link File}. Gets the file
     * and checks, if exist.
     *
     * @param key the name of the option
     * @return absolute path to existing file
     * @throws CruiseControlException if the file does not exist
     */
    public File getOptionFile(String key) throws CruiseControlException {
        final Option opt = getOption(key);
        // must be file type
        if (opt.type == File.class && ((File) opt.val).isFile()) {
            return (File) opt.val;
        }
        // The option is not file
        throw new CruiseControlException("Option '" + key + "' = '" + ((File) opt.val).getAbsolutePath()
                    + "' does not represent existing file!");
    }

    /**
     * Helper around {@link #getOptionType(String, Class)}} with Class being a {@link File}. Gets the file
     * and checks, if exist and is directory.
     *
     * @param key the name of the option
     * @return absolute path to  existing directory
     * @throws CruiseControlException if the directory does not exist
     */
    public File getOptionDir(String key) throws CruiseControlException {
        final File file = (File) getOptionType(key, File.class);
        // must be file type
        if (file != null && file.isDirectory()) {
            return file;
        }
        // The option is not file
        throw new CruiseControlException(
                "Option '" + key + "' = '" + file.getAbsolutePath() + "' does not represent existing directory!");
    }

    /**
     * Helper around {@link #getOptionType(String, Class)}} with Class being a {@link String}.
     * @param key the name of the option.
     * @return the value
     * @throws CruiseControlException if the option is not found or not string
     */
    public String getOptionStr(String key) throws CruiseControlException {
        final Option opt = getOption(key);
        // must be file type
        if (opt.type == String.class) {
            return (String) opt.val;
        }
        // The option is not a boolean
        throw new CruiseControlException("Option '" + key + "' = '" + opt.val + "' does not represent string!");
    }

    /**
     * Helper around {@link #getOptionType(String, Class)}} with Class being a {@link Boolean}.
     * @param key the name of the option.
     * @return the value
     * @throws CruiseControlException if the option is not found or not bool
     */
    public boolean getOptionBool(String key) throws CruiseControlException {
        final Option opt = getOption(key);
        // must be file type
        if (opt.type == Boolean.class) {
            return ((Boolean) opt.val).booleanValue();
        }
        // The option is not a boolean
        throw new CruiseControlException("Option '" + key + "' = '" + opt.val + "' does not represent boolean!");
    }

    /**
     * Helper around {@link #getOptionType(String, Class)}} with Class being a {@link Integer}.
     * @param key the name of the option.
     * @return the value
     * @throws CruiseControlException if the option is not found or not int value
     */
    public int getOptionInt(String key) throws CruiseControlException {
        final Option opt = getOption(key);
        // must be file type
        if (opt.type == Integer.class) {
            return ((Integer) opt.val).intValue();
        }
        // The option is not a boolean
        throw new CruiseControlException("Option '" + key + "' = '" + opt.val + "' does not represent string!");
    }

    /**
     * Helper around {@link #getOptionType(String, Class)}} with Class being a {@link URL}.
     * @param key the name of the option.
     * @return the value
     * @throws CruiseControlException if the option is not found or not URL
     */
    public URL getOptionUrl(String key) throws CruiseControlException {
        final Option opt = getOption(key);
        // must be file type
        if (opt.type == URL.class) {
            return (URL) opt.val;
        }
        // The option is not a boolean
        throw new CruiseControlException("Option '" + key + "' = '" + opt.val + "' does not represent URL!");
    }

    /**
     * Constructor. It is hidden since the class can only be used as singleton, but protected to be
     * overridable for test purposes
     *
     * @param owner the object allows to change the configuration through {@link #setOption(String, String, Object)}
     * @throws CruiseControlException
     */
    protected CruiseControlSettings(final Object owner) throws CruiseControlException {
        this.owner = owner;
    }

    /**
     * Finds the option according top the string key. If the option is not set in the given map,
     * the default value will be get.
     *
     * @param key the name of the option to be found
     * @return value the option; is never <code>null</code>
     * @throws IllegalArgumentException when the option key is unknown
     */
    private Option getOption(final String key) {
        final Option opt = options.get(key);
        // Filled
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

    // For testing purposes only! The owner can remove the instance, ant thus getIstance(Object) can be
    // called again to create new (clear) instance of the config
    @SuppressWarnings("javadoc")
    public static void delInstance(Object owner) {
        if (config == null) {
            return;
        }
        if (config.owner != owner) {
            throw new IllegalAccessError("Only owner can remove the config");
        }
        // Clear it
        config = null;
    }

    /**
     * Unchangeable option holder "structure". This is similar to LaunchConfiguration#Option, except
     * it holds object instead of string.
     */
    private static final class Option {
        /** Name of the option */
        final String key;
        /** The associated value */
        final Object val;
        /** The class the option type belongs to */
        final Class< ? > type;
        /* The help message */
        final String help;

        Option(final String key, Object val, final Class< ? > type, final String help) {
            // Special hack for URL type
            if (type == URL.class && val instanceof String) {
                try {
                    val = new URL((String) val);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid option " + key + "=" + val + " - invalid URL", e);
                }
            }
            // Check the instance
            if (val != null && val.getClass() != type) {
                throw new IllegalArgumentException("Invalid option " + key + "=" + val + " - " + val.getClass()
                            + "!=" + type);
            }
            this.key = key;
            this.val = val;
            this.type = type;
            this.help = help;
        }

        Option(final Option opt, final Object val) {
            // Check the instance
            if (val != null && val.getClass() != opt.type) {
                throw new IllegalArgumentException("Invalid option " + opt.key + "=" + val + " - " + val.getClass()
                + "!=" + opt.type);
            }
            this.key = opt.key;
            this.val = val;
            this.type = opt.type;
            this.help = opt.help;
        }
    }
}
