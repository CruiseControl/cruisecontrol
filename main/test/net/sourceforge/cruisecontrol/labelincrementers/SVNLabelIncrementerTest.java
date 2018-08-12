package net.sourceforge.cruisecontrol.labelincrementers;

import java.io.IOException;

import junit.framework.TestCase;

import org.jdom2.Element;

/**
 * @author Ketan Padegaonkar &lt; KetanPadegaonkar gmail &gt;
 */
public class SVNLabelIncrementerTest extends TestCase {

    private SVNLabelIncrementer incrementer;

    protected void setUp() {
        incrementer = new SVNLabelIncrementer();
    }

    public void testIsPreBuildLabelIncrementer() throws Exception {
        assertTrue(incrementer.isPreBuildIncrementer());
    }

    public void testGetsDefaultLabel() throws Exception {
        assertEquals("svn.0", incrementer.getDefaultLabel());
        incrementer.setSeparator("-");
        assertEquals("svn-0", incrementer.getDefaultLabel());
        incrementer.setLabelPrefix("test");
        assertEquals("test-0", incrementer.getDefaultLabel());
    }

    public void testGetsLabelPrefix() throws Exception {
        assertEquals("svn", incrementer.getLabelPrefix());
    }

    public void testGetsSeparator() throws Exception {
        assertEquals(".", incrementer.getSeparator());

        incrementer.setSeparator("-");
        assertEquals("-", incrementer.getSeparator());
    }

    public void testIncrementsLabelWhenSVNRevisionIsSame() throws Exception {
        incrementer = new SVNLabelIncrementer() {
            protected String getSvnRevision() throws IOException {
                return "10";
            }
        };
        assertEquals("svn.10.1", incrementer.incrementLabel("svn.10", new Element("nothing")));
        assertEquals("svn.10.3", incrementer.incrementLabel("svn.10.2", new Element("nothing")));
    }

    public void testIncrementsLabelWithProperSeparatorWhenSVNRevisionIsSame() {
        incrementer = new SVNLabelIncrementer() {
            protected String getSvnRevision() throws IOException {
                return "10";
            }

            public String getSeparator() {
                return "-";
            }
        };
        assertEquals("svn-10-1", incrementer.incrementLabel("svn-10", new Element("nothing")));
        assertEquals("svn-10-3", incrementer.incrementLabel("svn-10-2", new Element("nothing")));
    }

    public void testValidatesLabel() throws Exception {
        // all labels should be considered valid!
        assertTrue(incrementer.isValidLabel("svn10"));
        assertTrue(incrementer.isValidLabel("svn.10"));
        assertTrue(incrementer.isValidLabel("svn.10.2"));

        incrementer.setSeparator("-");
        assertTrue(incrementer.isValidLabel("svn10"));
        assertTrue(incrementer.isValidLabel("svn-10"));
        assertTrue(incrementer.isValidLabel("svn-10-2"));
        assertTrue(incrementer.isValidLabel("svn.10:11"));
        assertTrue(incrementer.isValidLabel("svn.10:11.2"));        
        assertTrue(incrementer.isValidLabel("svn.10M"));
        assertTrue(incrementer.isValidLabel("svn.10M.2"));
        assertTrue(incrementer.isValidLabel("svn.10S"));
        assertTrue(incrementer.isValidLabel("svn.10S.2"));
        assertTrue(incrementer.isValidLabel("svn.10:11MS"));
        assertTrue(incrementer.isValidLabel("svn.10:11MS.2"));
 
        incrementer.setSeparator("-");
        assertTrue(incrementer.isValidLabel("svn.10:11MS.2"));
        assertTrue(incrementer.isValidLabel("svn-10:11MS.2"));
        assertTrue(incrementer.isValidLabel("svn.10:11MS-2"));
        assertTrue(incrementer.isValidLabel("svn-10:11MS-2"));
    }

    
}