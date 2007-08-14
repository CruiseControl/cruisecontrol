package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LongTextLeakOutTest extends SeleniumTestCase {

    public void testShouldInsertWbrTagInToTheContentOfWbrSensitiveElement() throws Exception {
        selenium.open("/dashboard/dashboard");
        String source = selenium.getHtmlSource();
        Document htmlDom = getHtmlDom(source);
        System.out.println(source);
        assertTrue(StringUtils.containsIgnoreCase(source, "wbr"));
        Element div = htmlDom.getElementById("toolkit_projectWithoutPublishers");
        NodeList elements = div.getElementsByTagName("span");
        Node item = null;
        for (int i = 0; i < elements.getLength(); i++) {
            String cssClass = elements.item(i).getAttributes().getNamedItem("class").getNodeValue();
            if (StringUtils.containsIgnoreCase(cssClass, "title")) {
                if (item != null) { throw new RuntimeException("Already found a title div"); }
                item = elements.item(i);
            }
        }
        NodeList elementsByTagName = ((Element) item).getElementsByTagName("WBR");
        assertEquals(4, elementsByTagName.getLength());
    }

}
