package net.sourceforge.cruisecontrol.gendoc.html;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.gendoc.PluginInfoParser;
import net.sourceforge.cruisecontrol.jmx.CruiseControlControllerJMXAdaptor;

/**
 * @author Dan Rollo
 *         Date: Aug 8, 2010
 *         Time: 9:46:38 PM
 */
public class ConfigHtmlGeneratorTest extends TestCase {

    private ConfigHtmlGenerator configHtmlGenerator;

    protected void setUp() throws Exception {
        configHtmlGenerator = new ConfigHtmlGenerator();
    }

    public void testGenerateWithNull() throws Exception {
        try  {
            configHtmlGenerator.generate(null);
            fail("should fail with null parser");
        } catch (NullPointerException e) {
            assertNull(e.getMessage());
        }
    }

    public void testGenerate() throws Exception {
        final PluginRegistry registry = PluginRegistry.createRegistry();

        final PluginInfoParser parser = new PluginInfoParser(registry, CruiseControlControllerJMXAdaptor.ROOT_PLUGIN);

        final String html = configHtmlGenerator.generate(parser);
        // @todo Add better assertions (better yet, replace with smaller unit tests...).
        assertTrue("Wrong length: " + html.length(), html.length() >= 234000);
        assertTrue(html.trim().endsWith("</html>"));
    }
}
