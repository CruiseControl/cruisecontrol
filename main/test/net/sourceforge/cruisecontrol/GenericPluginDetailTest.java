package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.sourcecontrols.ConcurrentVersionsSystem;
import junit.framework.TestCase;

public class GenericPluginDetailTest extends TestCase {

    public void testGetName() {
        GenericPluginDetail detail = new GenericPluginDetail("cvs", ConcurrentVersionsSystem.class);
        assertEquals("cvs", detail.getName());
    }

    public void testGetType() {
        GenericPluginDetail detail = new GenericPluginDetail("cvs", ConcurrentVersionsSystem.class);
        assertEquals(PluginType.SOURCE_CONTROL, detail.getType());
    }

    public void testGetRequiredAttributes() {
        Attribute[] attributes = new GenericPluginDetail("cvs", ConcurrentVersionsSystem.class).getRequiredAttributes();
        assertNotNull(attributes);
        assertTrue(0 < attributes.length);
    }

    public void testCompareTo() {
        GenericPluginDetail detail = new GenericPluginDetail("cvs", ConcurrentVersionsSystem.class);
        assertEquals(0, detail.compareTo(detail));
    }

    public void testToString() {
        GenericPluginDetail detail = new GenericPluginDetail("cvs", ConcurrentVersionsSystem.class);
        assertEquals(detail.getType() + ":" + detail.getName(), detail.toString());
    }
}
