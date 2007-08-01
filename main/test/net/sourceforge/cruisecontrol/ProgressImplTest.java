package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;

import java.util.Date;

import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

/**
 * @author Dan Rollo
 * Date: Jul 31, 2007
 * Time: 1:38:14 AM
 */
public class ProgressImplTest extends TestCase {

    public void testProgressInitState() throws Exception {
        final Progress progress = new ProgressImpl(null);
        assertEquals("New Progress should support call to getValue.",
                DateFormatFactory.getTimeFormat().format(new Date()) + " null",
                progress.getValue());
    }

    public void testProgressSet() throws Exception {
        final ProjectConfig config = new ProjectConfig();
        config.add(new DefaultLabelIncrementer());

        final Project project = new MockProject();
        project.setProjectConfig(config);
        
        final Progress progress = new ProgressImpl(project);

        final String testValue = "test value";
        progress.setValue(testValue);
        assertEquals(DateFormatFactory.getTimeFormat().format(new Date()) + " " + testValue, progress.getValue());
    }
}
