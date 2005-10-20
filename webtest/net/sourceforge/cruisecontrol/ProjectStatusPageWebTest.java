package net.sourceforge.cruisecontrol;

import net.sourceforge.jwebunit.WebTestCase;
import com.meterware.httpunit.HttpUnitOptions;

public class ProjectStatusPageWebTest extends WebTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        getTestContext().setBaseUrl("http://localhost:7854");
    }

    public void testForceBuild() {
        beginAt("/cruisecontrol");
        assertTextPresent("CruiseControl Status Page");
        clickLinkWithImage("play_white_bkg.png");
        assertTextPresent("CruiseControl Status Page");

        //Make sure the build actually started running.
        clickLinkWithText("commons-math");
        clickLinkWithText("Control Panel");
        assertTextNotPresent("waiting for next time to build");

    }
}
