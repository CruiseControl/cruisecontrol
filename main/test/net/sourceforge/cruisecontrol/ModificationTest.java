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

        String base = "<modification type=\"unknown\"><filename>File\"Name&amp;</filename><project>Folder'Name</project><date>" +
        formatter.format(modifiedTime) + "</date><user>User&lt;&gt;Name</user><comment><![CDATA[Comment]]></comment>";
        String closingTag = "</modification>";
        String expected = base + closingTag;
        assertEquals(expected, mod.toXml(formatter));

        String expectedWithEmail = base + "<email>foo.bar@quuuux.quuux.quux.qux</email>" + closingTag;
        mod.emailAddress = "foo.bar@quuuux.quuux.quux.qux";
        assertEquals(expectedWithEmail, mod.toXml(formatter));
    }
}