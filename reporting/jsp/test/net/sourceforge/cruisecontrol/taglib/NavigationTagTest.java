package net.sourceforge.cruisecontrol.taglib;

import junit.framework.TestCase;

public class NavigationTagTest extends TestCase {

    public NavigationTagTest(String name) {
        super(name);
    }

    public void testGetUrl() {
        NavigationTag tag = new NavigationTag();
        assertEquals("cruisecontrol/buildresults?log20020222120000", tag.getUrl("log20020222120000.xml", "cruisecontrol/buildresults"));
    }

    public void testGetLinkText() {
        NavigationTag tag = new NavigationTag();
        assertEquals("02/22/2002 12:00:00", tag.getLinkText("log20020222120000.xml"));
        assertEquals("02/22/2002 12:00:00", tag.getLinkText("log200202221200.xml"));
        assertEquals("02/22/2002 12:00:00 (3.11)", tag.getLinkText("log20020222120000L3.11.xml"));


        tag.setDateFormat("dd-MMM-yyyy HH:mm:ss");

        assertEquals("22-Feb-2002 12:00:00", tag.getLinkText("log20020222120000.xml"));
        assertEquals("22-Feb-2002 12:00:00 (3.11)", tag.getLinkText("log20020222120000L3.11.xml"));
    }
}