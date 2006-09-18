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
        ProjectConfig config = new ProjectConfig() {
            public void execute() {
            }
        };
        ProjectWrapper wrapper = new ProjectWrapper(config);

        assertNull(wrapper.getResult());

        wrapper.run();

        assertNotNull(wrapper.getResult());
    }
}