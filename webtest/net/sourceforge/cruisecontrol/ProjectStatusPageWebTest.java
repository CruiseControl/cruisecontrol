package net.sourceforge.cruisecontrol;

import net.sourceforge.jwebunit.WebTestCase;

public class ProjectStatusPageWebTest extends WebTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        getTestContext().setBaseUrl("http://localhost:7854");
    }

    public void testForceBuild() {
        beginAt("/cruisecontrol");
        assertTextPresent("CruiseControl Status Page");
        setWorkingForm("force_commons-math");
        submit();
        assertTextPresent("CruiseControl Status Page");

        // Make sure the build actually started running.
         clickLinkWithText("commons-math");
         clickLinkWithText("Control Panel");
         assertTextNotPresent("waiting for next time to build");
    }
}
