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

    private static void addTaskElement(final Element mod) {
        if (!mod.getName().equals("modification")) {
            throw new IllegalArgumentException("mod parameter must be a modification element, was: " + mod.getContent());
        }

        // set type attrib on parent mod element
        mod.setAttribute("type", "ccmtask");

        final Element task = new Element("task");
        task.addContent(22491 + "");

        mod.addContent(task);
    }

    private static void replaceModsWithTaskMod(final Element log) {
        final Element mods = TestUtil.createModsElement(1);
        addTaskElement((Element) mods.getContent().get(0));
        log.removeChild("modifications");
        log.addContent(0, mods);
    }


    public void testShouldPublishSuccess() throws Exception {
        final CMSynergyTaskPublisher publisher = new CMSynergyTaskPublisher();

        final Element log = TestUtil.createElement(true, true, "2 minutes 20 seconds", 0, null);
        assertFalse("No task mods, should not publish", publisher.shouldPublish(log));

        replaceModsWithTaskMod(log);
        assertTrue("Build Success with task mod, should publish", publisher.shouldPublish(log));
    }

    public void testShouldNotPublishFail() throws Exception {
        final CMSynergyTaskPublisher publisher = new CMSynergyTaskPublisher();

        final Element log = TestUtil.createElement(false, true, "2 minutes 20 seconds", 0, "it broke");
        assertFalse("Build Fail, should not publish", publisher.shouldPublish(log));

        replaceModsWithTaskMod(log);
        assertFalse("Build Fail with task mod, should not publish", publisher.shouldPublish(log));
    }

}
