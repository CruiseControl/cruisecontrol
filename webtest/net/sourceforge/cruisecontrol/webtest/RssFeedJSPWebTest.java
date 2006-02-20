package net.sourceforge.cruisecontrol.webtest;

import net.sourceforge.jwebunit.WebTestCase;
import net.sourceforge.cruisecontrol.Configuration;

public class RssFeedJSPWebTest extends WebTestCase {
    protected void setUp() throws Exception {
        super.setUp();

        getTestContext().setBaseUrl("http://localhost:7854");
    }

    public void testNavigatingToProjectRssFeed() {
        beginAt("/");
        clickLinkWithText("connectfour");
        clickLinkWithImage("rss.png");
        assertTextPresent("<title>CruiseControl Results - connectfour</title>");
    }

    public void testNavigatingToOveralltRssFeed() {
        beginAt("/");
        clickLinkWithImage("rss.png");
        assertTextPresent("<title>CruiseControl Results</title>");
    }
}
