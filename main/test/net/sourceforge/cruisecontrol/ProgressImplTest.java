package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;

import java.util.Date;

import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.util.DateUtil;

/**
 * @author Dan Rollo
 * Date: Jul 31, 2007
 * Time: 1:38:14 AM
 */
public class ProgressImplTest extends TestCase {

    public static class MockProgress implements Progress {
        private String value;
        private Date lastUpdated;

        public void setValue(String value) {
            this.value = value;
            lastUpdated = new Date();
        }

        /** @return current progress value represented as a String, prefixed with last update date. */
        public String getValue() {
            return DateUtil.getFormattedTime(lastUpdated) + " " + value;
        }

        /** @return the date when current progress value was set. */
        public Date getLastUpdated() {
            return lastUpdated;
        }

        /**
         * @return the current progress value (not prefixed by last updated date).
         */
        public String getText() {
            return value;
        }
    }

    public void testProgressInitState() throws Exception {
        final Progress progress = new ProgressImpl(null);
        final Date lastUpdate = progress.getLastUpdated();

        assertEquals("New Progress should support call to getValue.",
                DateUtil.getFormattedTime(lastUpdate) + " null",
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
        assertEquals(DateUtil.getFormattedTime(new Date()) + " " + testValue, progress.getValue());
    }
}
