package net.sourceforge.cruisecontrol;

import net.sourceforge.jwebunit.WebTestCase;

public class MetricsTabWebTest extends WebTestCase {
    protected void setUp() throws Exception {
        super.setUp();
        getTestContext().setBaseUrl("http://localhost:7854");
    }

    public void testMetricsTab() {
        beginAt("/cruisecontrol");
        assertTextPresent("CruiseControl Status Page");


        clickLinkWithText("commons-math");
        clickLinkWithText("Metrics");
        assertTextPresent("Number of Build Attempts");
    }
}
