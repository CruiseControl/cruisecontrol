package net.sourceforge.cruisecontrol.dashboard.utils;

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemUtils;

import junit.framework.TestCase;

public class DashboardUtilsTest extends TestCase {
    public void testShouldBeAbleToDecodeURL() {
        assertEquals("project name with space", DashboardUtils
                .decode("project%20name%20with%20space"));
    }

    public void testShouldSplitURLToArray() {
        String[] params =
                DashboardUtils.urlToParams("/detail/project%20name%20with%20space/whatever");
        assertEquals(3, params.length);
        assertEquals("detail", params[0]);
        assertEquals("project name with space", params[1]);
        assertEquals("whatever", params[2]);
    }

    public void testShouldReturnEmptyIfParamIsEmpty() {
        String[] params = DashboardUtils.urlToParams("");
        assertEquals(0, params.length);
    }

    public void testShouldReturnEmptyIfParamIsNull() {
        String[] params = DashboardUtils.urlToParams(null);
        assertEquals(0, params.length);
    }

    public void testShouldReturnEmptyIfParamOnlyContainsBackSlash() {
        String[] params = DashboardUtils.urlToParams("/");
        assertEquals(0, params.length);
    }

    public void testShouldReturnFileType() {
        assertEquals("file", DashboardUtils.getFileType(new File("abc")));
    }

    public void testShouldReturnDirectoryType() {
        File dir = FilesystemUtils.createDirectory("whatever");
        assertEquals("directory", DashboardUtils.getFileType(dir));
    }

    public void testShouldThrowExceptionWhenFileIsNull() {
        try {
            DashboardUtils.getFileType(null);
            fail("Runtime Exception exptected");
        } catch (RuntimeException e) {

        }
    }
}
