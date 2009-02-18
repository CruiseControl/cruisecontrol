package net.sourceforge.cruisecontrol.publishers;

import org.jdom.Element;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import junit.framework.TestCase;

/**
 * @author Dan Rollo
 * Date: Feb 18, 2009
 * Time: 11:31:37 AM
 */
public class CMSynergyTaskPublisherTest extends TestCase {

    public static void addElementWithContent(final Element parent,
                                             final String elementName, final String elementContent) {

        final Element elem = new Element(elementName);
        elem.addContent(elementContent);

        parent.addContent(elem);
    }

    private static void addTaskElement(final Element mod) {
        if (!mod.getName().equals("modification")) {
            throw new IllegalArgumentException("mod parameter must be a modification element, was: " + mod.getContent());
        }

        // set type attrib on parent mod element
        mod.setAttribute("type", "ccmtask");

        addElementWithContent(mod, "task", 22491 + "");
    }

    public static void replaceMods(final Element log, final Element mods) {
        log.removeChild("modifications");
        log.addContent(0, mods);
    }



    public void testShouldPublishSuccess() throws Exception {
        final CMSynergyTaskPublisher publisher = new CMSynergyTaskPublisher();

        assertFalse("No mods, should not publish", publisher.shouldPublish(
                TestUtil.createElement(true, true, "2 minutes 20 seconds", 0, null)));

        final Element log = TestUtil.createElement(true, true, "2 minutes 20 seconds", 1, null);
        assertFalse("No task mods, should not publish", publisher.shouldPublish(log));

        final Element mods = TestUtil.createModsElement(1);
        addTaskElement((Element) mods.getContent().get(0));
        replaceMods(log, mods);
        assertTrue("Build Success with task mod, should publish", publisher.shouldPublish(log));
    }

    public void testShouldNotPublishFail() throws Exception {
        final CMSynergyTaskPublisher publisher = new CMSynergyTaskPublisher();

        assertFalse("Build Fail no mods, should not publish", publisher.shouldPublish(
                TestUtil.createElement(false, true, "2 minutes 20 seconds", 0, "it broke")));

        final Element log = TestUtil.createElement(false, true, "2 minutes 20 seconds", 1, "it broke");
        assertFalse("Build Fail with no task mod, should not publish", publisher.shouldPublish(log));

        final Element mods = TestUtil.createModsElement(1);
        addTaskElement((Element) mods.getContent().get(0));
        replaceMods(log, mods);
        assertFalse("Build Fail with task mod, should not publish", publisher.shouldPublish(log));
    }

}
