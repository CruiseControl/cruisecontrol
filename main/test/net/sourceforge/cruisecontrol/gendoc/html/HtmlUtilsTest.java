package net.sourceforge.cruisecontrol.gendoc.html;

import junit.framework.TestCase;

/**
 * @author Dan Rollo
 *         Date: Aug 8, 2010
 *         Time: 11:45:48 PM
 */
public class HtmlUtilsTest extends TestCase {

    private HtmlUtils htmlUtils;

    protected void setUp() throws Exception {
        htmlUtils = new HtmlUtils();
    }


    public void testGetVersion() {
        assertNotNull(htmlUtils.getRelease());
    }
}
