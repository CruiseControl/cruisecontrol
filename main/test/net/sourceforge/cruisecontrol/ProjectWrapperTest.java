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

        project.setMockState(ProjectState.QUEUED);
        assertNull(wrapper.getResult());

        project.setMockState(ProjectState.BOOTSTRAPPING);
        assertNull(wrapper.getResult());

        project.setMockState(ProjectState.MODIFICATIONSET);
        assertNull(wrapper.getResult());

        project.setMockState(ProjectState.BUILDING);
        assertNull(wrapper.getResult());

        project.setMockState(ProjectState.MERGING_LOGS);
        assertNull(wrapper.getResult());

        project.setMockState(ProjectState.PUBLISHING);
        assertNull(wrapper.getResult());

        project.setMockState(ProjectState.IDLE);
        assertNotNull(wrapper.getResult());

        project.setMockState(ProjectState.WAITING);
        assertNotNull(wrapper.getResult());

        project.setMockState(ProjectState.PAUSED);
        assertNotNull(wrapper.getResult());

        project.setMockState(ProjectState.STOPPED);
        assertNotNull(wrapper.getResult());
    }
}