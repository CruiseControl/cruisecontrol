package net.sourceforge.cruisecontrol.gendoc;

import java.util.Stack;

// todo we could track empty tag and output them as <tag/> or just not output them at all
/**
 * Something that allows us to produce XML representations of the {@link PluginModel}.
 * @author jerome@coffeebreaks.org
 */
public class XmlOutputerImpl implements XmlOutputter {

    private StringBuffer buffer = new StringBuffer();
    private Stack nodes = new Stack();
    private static final String EOL = System.getProperty("line.separator");

    public XmlOutputerImpl() {
    }

    public void startTag(String name) {
        buffer.append(getIndentation());
        buffer.append("<").append(name).append(">");
        buffer.append(getEndOfLine());
        nodes.add(name); 
    }

    private String getIndentation() {
        StringBuffer indent = new StringBuffer(nodes.size() * 2);
        for (int i = 0; i < nodes.size(); i++) {
           indent.append("  ");
        }
        return indent.toString();
    }

    public void addChild(ToXml toXml) {
        toXml.toXml(this);
    }

    public void addChild(String name, String value) {
        buffer.append(getIndentation());
        String safeXMLValue = value; // FIXME might contain HTML... make sure we output valid XML code 
        buffer.append("<").append(name).append(">").append(safeXMLValue).append("</").append(name).append(">");
        buffer.append(getEndOfLine());
    }

    public void closeTag() {
        String name = (String) nodes.pop();
        buffer.append(getIndentation());
        buffer.append("</").append(name).append(">");
        buffer.append(getEndOfLine());
    }

    public String toString() {
        return buffer.toString();
    }

    public String getEndOfLine() {
        return EOL;
    }
}
