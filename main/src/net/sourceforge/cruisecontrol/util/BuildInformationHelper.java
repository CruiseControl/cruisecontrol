package net.sourceforge.cruisecontrol.util;

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.BuildLoopInformation.ProjectInfo;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppDriver;

public class BuildInformationHelper {
    private XStream xStream;

    public void init() {
        if (xStream == null) {
            xStream = new XStream(new XppDriver());
            xStream.alias("buildloop", BuildLoopInformation.class);
            xStream.alias("project", ProjectInfo.class);
            xStream.alias("modification", Modification.class);
            xStream.aliasField("username", Modification.class, "userName");
            xStream.omitField(BuildLoopInformation.class, "controller");
            xStream.omitField(BuildLoopInformation.class, "xstream");
        }
    }

    public String toXml(BuildLoopInformation buildinfo) {
        init();
        return xStream.toXML(buildinfo);
    }

    public BuildLoopInformation toObject(String xml) {
        init();
        return (BuildLoopInformation) xStream.fromXML(xml);
    }
}
