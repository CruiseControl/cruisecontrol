package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Dan Rollo
 *         Date: Jul 8, 2010
 *         Time: 12:42:53 AM
 */
public class ProjectPropsTest {

    private Project project;
    private ProjectConfig projectConfig;

    @Before
    public void setUp() throws CruiseControlException {
        project = new Project();
        project.setName("TestProject");

        projectConfig = new ProjectConfig();
        projectConfig.add(new DefaultLabelIncrementer());
        project.setProjectConfig(projectConfig);
    }

    @After
    public void tearDown() {
        project.stop();
        project = null;
        projectConfig = null;
    }


    @Test
    public void testGetProjectPropertiesMapClearsAddedProperties() throws Exception {
        assertNull("Additional Props should default to null.", project.getAdditionalProperties());

        final String newKey = "newKey";
        final String newValue = "newValue";
        final Map<String, String> additionalProperties = new HashMap<String, String>();
        additionalProperties.put(newKey, newValue);

        project.forceBuildWithTarget("anyTarget", additionalProperties);

        assertEquals(newValue, project.getProjectPropertiesMap(new Date()).get(newKey));

        assertNull("Additional Props should be cleared.", project.getAdditionalProperties());

        assertEquals("Props arg should be emptied.", 0, additionalProperties.size()); // this may not be so important
    }
}
