package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;

import java.util.Date;
import java.text.SimpleDateFormat;

public class ModificationTest extends TestCase {

    public ModificationTest(String name) {
        super(name);
    }

    public void testToXml() {
        Date modifiedTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        Modification mod = new Modification();
        mod.fileName = "File\"Name&";
        mod.folderName = "Folder'Name";
        mod.modifiedTime = modifiedTime;
        mod.userName = "User<>Name";
        mod.comment = "Comment";

        String expected = "<modification type=\"unknown\"><filename>File\"Name&amp;</filename><project>Folder'Name</project><date>" +
        formatter.format(modifiedTime) + "</date><user>User&lt;&gt;Name</user><email></email><comment><![CDATA[Comment]]></comment></modification>";

        assertEquals(expected, mod.toXml(formatter));
    }
}