package net.sourceforge.cruisecontrol.launch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.testutil.TestUtil.PropertiesRestorer;


public class LaunchConfigurationTest extends TestCase {

    private final FilesToDelete filesToDelete = new FilesToDelete();
    private final PropertiesRestorer propRestorer = new PropertiesRestorer();
    private final LogInterface log = new StdErrLogger();

    @Override
    protected void setUp() throws IOException {
        propRestorer.record();
    }

    @Override
    protected void tearDown() throws Exception {
        filesToDelete.delete();
        propRestorer.restore();
//        // Must also release all properties added here
//        for (String name : System.getProperties().stringPropertyNames()) {
//            if (name.startsWith("cc.")) {
//                System.clearProperty(name);
//            }
//        }
//        final Properties props = System.getProperties();
//        for (String name : props.stringPropertyNames()) {
//            if (name.startsWith("cc.")) {
//                props.remove(name);
//            }
//        }
    }

    /** Tests the default values of the options, when not overridden by anything else
     * @throws LaunchException */
    public void testDefaultVals() throws CruiseControlException, LaunchException {
        final LaunchConfiguration config = new LaunchConfiguration(new String[0], log, null);

        assertEquals("lib", config.getOptionRaw(LaunchConfiguration.KEY_LIBRARY_DIRS));
        assertEquals("cruisecontrol.xml", config.getOptionRaw(LaunchConfiguration.KEY_CONFIG_FILE));
        assertEquals(null, config.getOptionRaw(LaunchConfiguration.KEY_LOG4J_CONFIG)); // no config by default
        assertEquals(false, config.getOptionBool(LaunchConfiguration.KEY_NO_USER_LIB));
        // libs
        // distDir
        // homeDir

        // None was set
        assertFalse(config.wasOptionSet(LaunchConfiguration.KEY_LIBRARY_DIRS));
        assertFalse(config.wasOptionSet(LaunchConfiguration.KEY_CONFIG_FILE));
        assertFalse(config.wasOptionSet(LaunchConfiguration.KEY_LOG4J_CONFIG));
        assertFalse(config.wasOptionSet(LaunchConfiguration.KEY_NO_USER_LIB));
    }

    /** Tests the overwrite of default values through command line arguments */
    public void testArgumentVals() throws LaunchException, CruiseControlException {
        final String[] args = new String[] {
                "-artefacts",  "/tmp/artifacts",
                "-some_bool_option",
                "-another_bool_option",
                "-"+LaunchConfiguration.KEY_LIBRARY_DIRS, "/usr/share/cruisecontrol/lib",
                "-"+LaunchConfiguration.KEY_LOG4J_CONFIG,"/var/spool/cruisecontrol/log4j.config",
                };
        final LaunchConfiguration config = new LaunchConfiguration(args, log, null);

        // test changed
        assertEquals("/tmp/artifacts", config.getOptionRaw("artefacts"));
        assertEquals("true", config.getOptionRaw("some_bool_option"));
        assertEquals("true", config.getOptionRaw("another_bool_option"));
        assertEquals("/usr/share/cruisecontrol/lib", config.getOptionRaw(LaunchConfiguration.KEY_LIBRARY_DIRS));
        assertEquals("/var/spool/cruisecontrol/log4j.config", config.getOptionRaw(LaunchConfiguration.KEY_LOG4J_CONFIG));
        // Those has been set
        assertTrue(config.wasOptionSet(LaunchConfiguration.KEY_LIBRARY_DIRS));
        assertTrue(config.wasOptionSet(LaunchConfiguration.KEY_LOG4J_CONFIG));
        // Others must remain
        assertEquals("cruisecontrol.xml", config.getOptionRaw(LaunchConfiguration.KEY_CONFIG_FILE));
        // So they have not been set
        assertFalse(config.wasOptionSet(LaunchConfiguration.KEY_CONFIG_FILE));
    }

    /** Tests the overwrite of default values through properties */
    public void testPropertiesVals() throws LaunchException, CruiseControlException {
        System.setProperty("cc.artefacts",  "/tmp/cruise/artifacts");
        System.setProperty("cc."+LaunchConfiguration.KEY_LIBRARY_DIRS, "/usr/share/cruise/lib");
        System.setProperty("cc."+LaunchConfiguration.KEY_LOG4J_CONFIG,"/var/spool/cruise/log4j.conf");

        final LaunchConfiguration config = new LaunchConfiguration(new String[0], log, null);

        // test changed
        assertEquals("/tmp/cruise/artifacts", config.getOptionRaw("artefacts"));
        assertEquals("/usr/share/cruise/lib", config.getOptionRaw(LaunchConfiguration.KEY_LIBRARY_DIRS));
        assertEquals("/var/spool/cruise/log4j.conf", config.getOptionRaw(LaunchConfiguration.KEY_LOG4J_CONFIG));
        // Those has been set
        assertTrue(config.wasOptionSet("artefacts"));
        assertTrue(config.wasOptionSet(LaunchConfiguration.KEY_LIBRARY_DIRS));
        assertTrue(config.wasOptionSet(LaunchConfiguration.KEY_LOG4J_CONFIG));
        // Others must remain
        assertEquals("cruisecontrol.xml", config.getOptionRaw(LaunchConfiguration.KEY_CONFIG_FILE));
        // So they have not been set
        assertFalse(config.wasOptionSet(LaunchConfiguration.KEY_CONFIG_FILE));
    }

    public void testConfigVals() throws LaunchException, CruiseControlException, IOException {
        final Map<String, String> opts = new HashMap<String, String>();
        opts.put("artefacts",  "/home/CC/artifacts");
        opts.put(LaunchConfiguration.KEY_LIBRARY_DIRS, "/usr/share/CC/lib");
        opts.put(LaunchConfiguration.KEY_LOG4J_CONFIG,"/var/spool/CC/log4j.conf");

        final Element data = makeLauchXML(opts);
        final File xml = storeXML(data, filesToDelete.add("config.xml"));
        final LaunchConfiguration config = new LaunchConfiguration(
                new String[] {"-"+LaunchConfiguration.KEY_CONFIG_FILE, xml.getAbsolutePath()}, log, null);

        // test changed
        assertEquals("/home/CC/artifacts", config.getOptionRaw("artefacts"));
        assertEquals("/usr/share/CC/lib", config.getOptionRaw(LaunchConfiguration.KEY_LIBRARY_DIRS));
        assertEquals("/var/spool/CC/log4j.conf", config.getOptionRaw(LaunchConfiguration.KEY_LOG4J_CONFIG));
        // Those has been set
        assertTrue(config.wasOptionSet("artefacts"));
        assertTrue(config.wasOptionSet(LaunchConfiguration.KEY_LIBRARY_DIRS));
        assertTrue(config.wasOptionSet(LaunchConfiguration.KEY_LOG4J_CONFIG));
        // Others must remain
        assertEquals("cruisecontrol.xml", config.getOptionRaw(LaunchConfiguration.KEY_CONFIG_FILE)); // must be default since no other has been specified
        // So they have not been set
        assertFalse(config.wasOptionSet(LaunchConfiguration.KEY_CONFIG_FILE));
    }

    /** Tests various levels of data overriding */
    public void testConfigOverride() throws LaunchException, CruiseControlException, IOException {
        // Configuration file, the lowest priority
        final Map<String, String> opts = new HashMap<String, String>();
        opts.put("artefacts",  "/home/CC/artifacts");
        opts.put(LaunchConfiguration.KEY_LIBRARY_DIRS, "/usr/share/CC/lib");
        final Element data = makeLauchXML(opts);
        final File xml = storeXML(data, filesToDelete.add("config.xml"));
        // Properties - the highest priority, overrides config file
        System.setProperty("cc.artefacts",  "/tmp/cruise/artifacts");
        System.setProperty("cc."+LaunchConfiguration.KEY_LIBRARY_DIRS, "/usr/share/cruisecontrol/lib");
        // command line options - overrides options from config and from properties
        final String[] args = new String[] {
                "-artefacts",  "/tmp/artifacts",
                "-"+LaunchConfiguration.KEY_CONFIG_FILE, xml.getAbsolutePath()
                };

        // Create the object
        final LaunchConfiguration config = new LaunchConfiguration(args, log, null);

        // Single overrides
        assertEquals("/tmp/artifacts", config.getOptionRaw("artefacts"));
        // Multiple appends (higher priority first)
        assertEquals("/usr/share/cruisecontrol/lib" + config.ITEM_SEPARATOR
                + "/usr/share/CC/lib", config.getOptionRaw(LaunchConfiguration.KEY_LIBRARY_DIRS));
    }


    /** Tests if correct path to main config file is returned when the <launcher>...</launcher>
     *  configuration stands on its own and points to an "external" main
     *  <cruisecontrol>...</cruisecontrol> configuration.
     *
     *  @throws Exception
     */
    public void testLaunchSeparate() throws Exception {
        // Configuration file, referenced to an external file
        final Map<String, String> opts = new HashMap<String, String>();
        opts.put(LaunchConfiguration.KEY_CONFIG_FILE,  "/home/CC/mainconfig.xml");
        final Element launch = makeLauchXML(opts);
        final File xml = storeXML(launch, filesToDelete.add("launch.xml"));
        // command line options - overrides options from config
        final String[] args = new String[] {
                "-"+LaunchConfiguration.KEY_CONFIG_FILE, xml.getAbsolutePath()
                };
        // Create the object
        final LaunchConfiguration config = new LaunchConfiguration(args, log, null);

        // Must return path to the main configuration file!
        assertEquals("/home/CC/mainconfig.xml", config.getOptionRaw(LaunchConfiguration.KEY_CONFIG_FILE));
    }
    /** Tests if correct path to main config file is returned when the <launcher>...</launcher> configuration
     *  is embedded in the main <cruisecontrol>...</cruisecontrol> configuration.
     *
     *  @throws Exception
     */
    public void testLaunchEmbedded() throws Exception {
        // Configuration file, referenced to an external file
        final Map<String, String> opts = new HashMap<String, String>();
        opts.put(LaunchConfiguration.KEY_CONFIG_FILE,  "/home/CC/mainconfig.xml");  // should be ignored, even if presented!
        final Element launch = makeLauchXML(opts);
        final Element main = makeConfigXML(launch); // embeds <launcher> to the main config
        final File xml = storeXML(main, filesToDelete.add("cruisecontrol.xml"));
        // command line options - overrides options from config
        final String[] args = new String[] {
                "-"+LaunchConfiguration.KEY_CONFIG_FILE, xml.getAbsolutePath()
                };
        // Create the object
        final LaunchConfiguration config = new LaunchConfiguration(args, log, null);

        // Must return path to the main configuration file!
        assertEquals(xml.getAbsolutePath(), config.getOptionRaw(LaunchConfiguration.KEY_CONFIG_FILE));
    }

    /** Tests if default path to main config file is returned when the <launcher>...</launcher>
     *  configuration stands on its own and DOES NOT point to an "external" main
     *  <cruisecontrol>...</cruisecontrol> configuration.
     *
     *  @throws Exception
     */
    public void testConfigNotSet() throws Exception {
        // Configuration file, referenced to an external file
        final Element launch = makeLauchXML(new HashMap<String, String>());
        final File xml = storeXML(launch, filesToDelete.add("launch.xml"));
        // command line options - overrides options from config
        final String[] args = new String[] {
                "-"+LaunchConfiguration.KEY_CONFIG_FILE, xml.getAbsolutePath()
                };
        // Create the object
        final LaunchConfiguration config = new LaunchConfiguration(args, log, null);

        // Must return default path to the main configuration file!
        assertEquals("cruisecontrol.xml", config.getOptionRaw(LaunchConfiguration.KEY_CONFIG_FILE));
    }

    public void testBoolArgs() throws Exception {
        LaunchConfiguration config;

        config = new LaunchConfiguration(new String[] {"-" + LaunchConfiguration.KEY_NO_USER_LIB, "true"}, log, null);
        assertTrue(config.getOptionBool(LaunchConfiguration.KEY_NO_USER_LIB));

        config = new LaunchConfiguration(new String[] {"-" + LaunchConfiguration.KEY_NO_USER_LIB, "false"}, log, null);
        assertFalse(config.getOptionBool(LaunchConfiguration.KEY_NO_USER_LIB));

        // No true/false value set, must be true
        config = new LaunchConfiguration(new String[] {"-" + LaunchConfiguration.KEY_NO_USER_LIB}, log, null);
        assertTrue(config.getOptionBool(LaunchConfiguration.KEY_NO_USER_LIB));

        // Default is false to make the previous test meaningful
        config = new LaunchConfiguration(new String[] {}, log, null);
        assertFalse(config.getOptionBool(LaunchConfiguration.KEY_NO_USER_LIB));
    }

    /** Tests the case where an option can be set multiple times in the <launcher>...</launcher>
     *  XML element
     */
    public void testMultiOpts() throws Exception {
        LaunchConfiguration config;
        List<Map.Entry<String,String>> opts = new ArrayList<Map.Entry<String,String>>();

        // Fill with values. Paths does not have to exist since Configuration.getOptionRaw() method
        // is used to ge the value
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "path/1"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "path/1/with/subpath/"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "path/2"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "path/3/with/even/more"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "path_4_with_nonsence"));
        // Make XML config
        File confFile = storeXML(makeLauchXML(opts), filesToDelete.add("launch", ".conf"));

        config = new LaunchConfiguration(new String[] {"-"+LaunchConfiguration.KEY_CONFIG_FILE, confFile.getAbsolutePath()}, log, null);
        assertEquals("path/1" + LaunchConfiguration.ITEM_SEPARATOR + "path/1/with/subpath/"  + LaunchConfiguration.ITEM_SEPARATOR +
                     "path/2" + LaunchConfiguration.ITEM_SEPARATOR + "path/3/with/even/more" + LaunchConfiguration.ITEM_SEPARATOR +
                     "path_4_with_nonsence",
                     config.getOptionRaw(LaunchConfiguration.KEY_USER_LIB_DIRS));
    }

    /** Tests the case where an option can be set multiple times on the command line, i.e.
     *  <code>-lib path/to/lib/1 -lib path/to/lib/3 -lib path/to/lib/2 ...</code>
     */
    public void testMultiArgs() throws Exception {
        String args[] = {          // Paths does not have to exist since Configuration.getOptionRaw()
                "-user_lib", "path/1/", //  method is used to get the value
                "-user_lib", "path/1/with/subpath",
                "-user_lib", "path/2/",
                "-user_lib", "path/3/"};
        LaunchConfiguration config;

        config = new LaunchConfiguration(args, log, null);
        assertEquals("path/1/" + LaunchConfiguration.ITEM_SEPARATOR + "path/1/with/subpath" + LaunchConfiguration.ITEM_SEPARATOR +
                     "path/2/" + LaunchConfiguration.ITEM_SEPARATOR + "path/3/",
                     config.getOptionRaw(LaunchConfiguration.KEY_USER_LIB_DIRS));
    }

    /** Tests the case where an option sequence can be set multiple times - through the command line,
     *  properties and config file. Check the correct priority
     */
    public void testMultiArgSequence() throws Exception {
        final LaunchConfiguration config;

        // Options in file
        // Paths does not have to exist since Configuration.getOptionRaw() method is used to get the value
        final List<Map.Entry<String,String>> opts = new ArrayList<Map.Entry<String,String>>();
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "path/c1/"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "path/c2/"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "path/c3/with/even/more"));
        // Make XML config
        final File confFile = storeXML(makeLauchXML(opts), filesToDelete.add("launch", ".conf"));

        // Options through command line
        final String args[] = {
                "-user_lib",  "path/a1/",
                "-user_lib",  "path/a2/",
                "-user_lib",  "path/a3/",
                "-configfile", confFile.getAbsolutePath()};

        // Options in properties
        System.setProperty("cc.user_lib", "path/p1/" + LaunchConfiguration.ITEM_SEPARATOR + "path/p2/");

        // Test the sequence. It must be command-line first, properties second and the config the last
        config = new LaunchConfiguration(args, log, null);
        assertEquals("path/a1/" + LaunchConfiguration.ITEM_SEPARATOR + "path/a2/" + LaunchConfiguration.ITEM_SEPARATOR +
                     "path/a3/" + LaunchConfiguration.ITEM_SEPARATOR +
                     "path/p1/" + LaunchConfiguration.ITEM_SEPARATOR + "path/p2/" + LaunchConfiguration.ITEM_SEPARATOR +
                     "path/c1/" + LaunchConfiguration.ITEM_SEPARATOR + "path/c2/" + LaunchConfiguration.ITEM_SEPARATOR +
                     "path/c3/with/even/more",  config.getOptionRaw("user_lib"));
    }

    /** Tests the case where an option conains reference to another option, for example <code>${proj}/a/path</code>
     */
    public void testReference() throws Exception {
        // Options in file
        // Paths does not have to exist since Configuration.getOptionRaw() method is used to get the value
        final List<Map.Entry<String,String>> opts = new ArrayList<Map.Entry<String,String>>();
        opts.add(new AbstractMap.SimpleEntry<String, String>("dist", "/install/path"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("proj", "/project/path"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "${proj}/custom/lib1/"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("user_lib", "${proj}/custom/lib2/"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("log4jconfig", "${dist}/log4j/log4j.cfg"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("configfile", "${proj}/conf/projects.cfg"));
        // Make XML config
        final File confFile = storeXML(makeLauchXML(opts), filesToDelete.add("launch", ".conf"));

        // Options through command line
        final String args[] = {
                "-lib",  "${dist}/spec/path/lib/", // refers to launch config file
                "-configfile", confFile.getAbsolutePath()};

        // Options in properties
        System.setProperty("cc.user_lib", "${proj}/spec/lib/");

        // Test the sequence. It must be command-line first, properties second and the config the last
        final LaunchConfiguration config = new LaunchConfiguration(args, log, null);
        assertEquals("/project/path/spec/lib/" + LaunchConfiguration.ITEM_SEPARATOR +
                     "/project/path/custom/lib1/" + LaunchConfiguration.ITEM_SEPARATOR +
                     "/project/path/custom/lib2/", config.getOptionRaw(LaunchConfiguration.KEY_USER_LIB_DIRS));
        assertEquals("/install/path/spec/path/lib/", config.getOptionRaw(LaunchConfiguration.KEY_LIBRARY_DIRS));
        assertEquals("/install/path/log4j/log4j.cfg", config.getOptionRaw(LaunchConfiguration.KEY_LOG4J_CONFIG));
        assertEquals("/project/path/conf/projects.cfg", config.getOptionRaw(LaunchConfiguration.KEY_CONFIG_FILE));
    }

    /** Tests the loop detection in references
     */
    public void testReferenceLoop() throws Exception {
        // Options in file
        // Paths does not have to exist since Configuration.getOptionRaw() method is used to get the value
        final List<Map.Entry<String,String>> opts = new ArrayList<Map.Entry<String,String>>();
        opts.add(new AbstractMap.SimpleEntry<String, String>("dist", "${proj}/install/path"));
        // Make XML config
        final File confFile = storeXML(makeLauchXML(opts), filesToDelete.add("launch", ".conf"));
        // Options through command line
        final String args[] = {"-configfile", confFile.getAbsolutePath()};
        // Options in properties
        System.setProperty("cc.proj", "${dist}/project/path/");

        // Test the sequence. It must be command-line first, properties second and the config the last
        final LaunchConfiguration config = new LaunchConfiguration(args, log, null);
        try {
            config.getOptionRaw("dist");
            fail("Loop was not detected!");
        } catch (IllegalStateException e) {
            assertEquals("Too deep recursion", e.getMessage());
        }
        try {
            config.getOptionRaw("proj");
            fail("Loop was not detected!");
        } catch (IllegalStateException e) {
            assertEquals("Too deep recursion", e.getMessage());
        }
    }

    /** Tests too complex reference embeddings (2 levels)
     */
    public void testReferenceEmbedded() throws Exception {
        // Options in file
        // Paths does not have to exist since Configuration.getOptionRaw() method is used to get the value
        final List<Map.Entry<String,String>> opts = new ArrayList<Map.Entry<String,String>>();
        opts.add(new AbstractMap.SimpleEntry<String, String>("dist", "${proj}/install/path"));
        opts.add(new AbstractMap.SimpleEntry<String, String>("proj", "${dist}"));
        // Make XML config
        final File confFile = storeXML(makeLauchXML(opts), filesToDelete.add("launch", ".conf"));
        // Options through command line
        final String args[] = {"-configfile", confFile.getAbsolutePath()};
        // Options in properties
        System.setProperty("cc.logdir", "${proj}/project/path/");

        // Test the sequence. It must be command-line first, properties second and the config the last
        final LaunchConfiguration config = new LaunchConfiguration(args, log, null);
        try {
            config.getOptionRaw("logdir");
            fail("Loop was not detected!");
        } catch (IllegalStateException e) {
            assertEquals("Too deep recursion", e.getMessage());
        }
    }

    public void testFindFile() throws Exception {
        // Various files
        final File inAbsolutePath = filesToDelete.add("file1.xml");
        final File inWorkingDir = filesToDelete.add(new File("file2.txt"));
        final File inHomeDir = filesToDelete.add(new File(new File(System.getProperty("user.home")).getAbsoluteFile(), "file3.txt"));
        LaunchConfiguration config;
        String file;

        // absolute
        file = inAbsolutePath.getAbsolutePath();
        makeLaunchConfig(inAbsolutePath, file);

        config = new LaunchConfiguration(new String[] {"-"+LaunchConfiguration.KEY_CONFIG_FILE, file}, log, null);

        assertTrue(new File(file).isAbsolute());
        assertEquals(inAbsolutePath, config.getOptionFile(LaunchConfiguration.KEY_CONFIG_FILE));

        // in working dir
        file = inWorkingDir.getName();
        makeLaunchConfig(inWorkingDir, file);

        config = new LaunchConfiguration(new String[] {"-"+LaunchConfiguration.KEY_CONFIG_FILE, file}, log, null);

        assertFalse(new File(file).isAbsolute());
        assertEquals(inWorkingDir, config.getOptionFile(LaunchConfiguration.KEY_CONFIG_FILE));

        // in home dir
        file = inHomeDir.getName();
        makeLaunchConfig(inHomeDir, file);

        config = new LaunchConfiguration(new String[] {"-"+LaunchConfiguration.KEY_CONFIG_FILE, file}, log, null);

        assertFalse(new File(file).isAbsolute());
        assertEquals(inHomeDir, config.getOptionFile(LaunchConfiguration.KEY_CONFIG_FILE));
    }


    /** From the set of entries creates string with <launch> ... </launch> XML fragment with values
     *  filled according to the items in the set */
    public static Element makeLauchXML(final Map<String,String> opts) {
        return makeLauchXML(opts.entrySet());
    }

    /** From the map, where keys are names of options, creates string with <launch> ... </launch> XML
     *  fragment with values filled according to the map */
    public static Element makeLauchXML(final Collection<Map.Entry<String,String>> opts) {
       Element root = new Element("launcher");

       for (Map.Entry<String, String> item : opts) {
           Element conf = new Element(item.getKey());
           conf.setText(item.getValue());

           root.addContent(conf);
       }
       return root;
    }

    /** The given <launch> ... </launch> XML element embedds into the <cruisecontrol>...</cruisecontrol>
     *  element */
    public static Element makeConfigXML(final Element launchConf) {
       Element root = new Element("cruisecontrol");
       root.addContent(launchConf.clone());

       return root;
    }

    /** Stores the given element to the given file */
    public static File storeXML(final Element xml, final File file) throws IOException {
       final XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());

       out.output(new Document(xml), new FileOutputStream(file));
       return file;
    }

    /**
     * Creates the XML file with the following format:
     * <pre>
     *      <launcher>
     *          <configfile>cruiseconfigFname</configfile>
     *      <launcher>
     * </pre>
     * and stores it to launchConfigFname
     *
     * @param launchConfigFname the file to be created
     * @param cruiseConfigFname the content of <configfile>...</configfile> element
     * @throws IOException if the file cannot be created
     */
    static
    private void makeLaunchConfig(final File launchConfigFname, final String cruiseConfigFname) throws IOException  {
        Map<String, String> opts = new HashMap<String, String>();
        Element xml;

        opts.put(LaunchConfiguration.KEY_CONFIG_FILE, cruiseConfigFname);
        xml = makeLauchXML(opts);

        storeXML(xml, launchConfigFname);
    }
}
