/*
 * Created on Nov 19, 2004
 */
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;

/**
 * @author Jeffrey Fredrick
 */
public class ProjectWrapperTest extends TestCase {

    public void testGetResult() {
        MockProject project = new MockProject();
        ProjectWrapper wrapper = new ProjectWrapper(project);

        assertNull(wrapper.getResult());

        wrapper.run();

        assertNotNull(wrapper.getResult());
    }
}