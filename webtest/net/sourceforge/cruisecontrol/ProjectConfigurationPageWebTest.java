package net.sourceforge.cruisecontrol;

import net.sourceforge.jwebunit.WebTestCase;

public class ProjectConfigurationPageWebTest extends WebTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        getTestContext().setBaseUrl("http://localhost:7854");
    }

    public void testShouldLoadRawXMLConfigurationData() {
        beginAt("/cruisecontrol/config/commons-math?tab=config");
        assertFormPresent("commons-math-config");
        assertSubmitButtonPresent("configure");
        setWorkingForm("commons-math-config");
        assertFormElementPresent("configuration");
        assertTextPresent("<cruisecontrol>");
        assertTextPresent("</cruisecontrol>");
    }
}
