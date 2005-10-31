package net.sourceforge.cruisecontrol;

import net.sourceforge.jwebunit.WebTestCase;

public class ProjectConfigurationPageWebTest extends WebTestCase {
    private Configuration configuration;
    private String configurationContents;
    
    protected void setUp() throws Exception {
        super.setUp();
        getTestContext().setBaseUrl("http://localhost:7854");
        
        configuration = new Configuration("localhost", 7856);
        configurationContents = configuration.getConfiguration();
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        configuration.setConfiguration(configurationContents);
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
    
    public void testShouldSaveChangesToXMLConfigurationData() {
        beginAt("/cruisecontrol/config/commons-math?tab=config");
        assertFormPresent("commons-math-config");
        assertSubmitButtonPresent("configure");
        setWorkingForm("commons-math-config");
        setFormElement("configuration", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- Empty config --><cruisecontrol />");
        submit();
        assertFormPresent("commons-math-config");
        assertSubmitButtonPresent("configure");
        setWorkingForm("commons-math-config");
        assertFormElementPresent("configuration");
        assertTextPresent("<!-- Empty config -->");
    }
}
