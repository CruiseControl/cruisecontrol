package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;

public class ProjectConfigTest extends TestCase {

    private ProjectConfig config;

    protected void setUp() {
        config = new ProjectConfig();
    }

    protected void tearDown() {
        config = null;
    }
    
    public void testValidate_ScheduleRequired() throws CruiseControlException {
        try {
            config.validate();
            fail("a schedule should have been required by ProjectConfig");
        } catch (CruiseControlException expected) {
            assertEquals("project requires a schedule", expected.getMessage());
        }
        
        config.add(new MockSchedule());
        config.validate();
    }

}
