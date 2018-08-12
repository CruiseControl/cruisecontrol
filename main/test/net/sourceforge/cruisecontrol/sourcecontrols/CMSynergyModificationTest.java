package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;

import org.jdom2.Element;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.publishers.CMSynergyTaskPublisherTest;

/**
 * @author Dan Rollo
 * Date: Feb 18, 2009
 * Time: 12:42:23 PM
 */
public class CMSynergyModificationTest extends TestCase {

    private static void addCCMObjectToMod(final Element mod,
                                          final String name, final String version, final String type,
                                                     final String instance, final String project,
                                                     final String comment) {

        if (!mod.getName().equals("modification")) {
            throw new IllegalArgumentException("mod parameter must be a modification element, was: "
                    + mod.getContent());
        }

        final Element ccmobject = new Element("ccmobject");

        CMSynergyTaskPublisherTest.addElementWithContent(ccmobject, "name", name);
        CMSynergyTaskPublisherTest.addElementWithContent(ccmobject, "version", version);
        CMSynergyTaskPublisherTest.addElementWithContent(ccmobject, "type", type);
        CMSynergyTaskPublisherTest.addElementWithContent(ccmobject, "instance", instance);
        CMSynergyTaskPublisherTest.addElementWithContent(ccmobject, "project", project);
        CMSynergyTaskPublisherTest.addElementWithContent(ccmobject, "comment", comment);

        mod.addContent(ccmobject);
    }

    public void testToFromElement() throws Exception {

        final Element log = TestUtil.createElement(true, true, "2 minutes 20 seconds", 0, null);

        final Element expectedMods = TestUtil.createModsElement(1);
        final String name = "1) AstNameRenderer.java";
        final String version = "6";
        final String type = "type";
        final String instance = "1";
        final String project = "asteron";
        final String comment = "Version automatically created during work area reconciliation.";
        addCCMObjectToMod((Element) expectedMods.getContent().get(0), name, version, type, instance, project, comment);
        CMSynergyTaskPublisherTest.replaceMods(log, expectedMods);
        final CMSynergyModification cmsModExpected = new CMSynergyModification();
        cmsModExpected.fromElement((Element) expectedMods.getContent().get(0));

        final CMSynergyModification cmsMod = new CMSynergyModification();
        cmsMod.createModifiedObject(name, version, type, instance, project, comment);
        // fake remaining fields to match
        cmsMod.revision = cmsModExpected.revision;
        cmsMod.userName = cmsModExpected.userName;
        cmsMod.modifiedTime = cmsModExpected.modifiedTime;
        cmsMod.comment = cmsModExpected.comment;

        assertEquals(cmsModExpected.toString(), cmsMod.toString());
    }
}
