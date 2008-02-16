package net.sourceforge.cruisecontrol.util;

import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.Modification;

public class BuildInformationHelperTest extends TestCase {

    public void testShouldCreateObjectFromXml() throws Exception {
        String xml =
                "<buildloop>\r\n"
                + "  <uuid>3a373376-44b5-4c22-a782-6c05d1c5c4a5</uuid>"
                + "  <jmx>\r\n"
                + "    <httpurl>http://www.buildloop.com:1234</httpurl>\r\n"
                + "    <rmiurl>rmi://www.buildloop.com:5678</rmiurl>\r\n"
                + "    <username>Chris</username>\r\n"
                + "    <password>123asd</password>\r\n"
                + "  </jmx>\r\n"
                + "  <servername>www.buildloop.com</servername>\r\n"
                + "  <timestamp>2007-09-20T06:53:57</timestamp>\r\n"
                + "  <projects>\r\n"
                + "    <project>\r\n"
                + "      <name>project1</name>\r\n"
                + "      <status>building</status>\r\n"
                + "      <buildstarttime>2003-12-12T07:22:35</buildstarttime>\r\n"
                + "      <modifications>\r\n"
                + "        <modification>\r\n"
                + "          <type>unknown</type>\r\n"
                + "          <username>bob</username>\r\n"
                + "          <revision>123</revision>\r\n"
                + "          <comment>support security check</comment>\r\n"
                + "          <files/>\r\n"
                + "        </modification>\r\n"
                + "      </modifications>\r\n"
                + "    </project>\r\n"
                + "  </projects>\r\n"
                + "</buildloop>\r\n";
        BuildLoopInformation buildInfo = new BuildInformationHelper().toObject(xml);

        assertEquals("3a373376-44b5-4c22-a782-6c05d1c5c4a5", buildInfo.getUuid());

        BuildLoopInformation.JmxInfo jmxInfo = buildInfo.getJmxInfo();
        assertEquals("http://www.buildloop.com:1234", jmxInfo.getHttpAdpatorUrl());
        assertEquals("rmi://www.buildloop.com:5678", jmxInfo.getRmiUrl());
        assertEquals("Chris", jmxInfo.getUserName());
        assertEquals("123asd", jmxInfo.getPassword());

        assertEquals("www.buildloop.com", buildInfo.getServerName());
        assertEquals("2007-09-20T06:53:57", buildInfo.getTimestamp());

        BuildLoopInformation.ProjectInfo[] projects = buildInfo.getProjects();
        assertEquals(1, projects.length);
        assertEquals("project1", projects[0].getName());
        assertEquals("building", projects[0].getStatus());
        assertEquals("2003-12-12T07:22:35", projects[0].getBuildStartTime());

        List modifications = projects[0].getModifications();
        assertEquals(1, modifications.size());
        Modification modification = (Modification) modifications.get(0);
        assertEquals("bob", modification.userName);
        assertEquals("support security check", modification.comment);
        assertEquals("123", modification.revision);
    }

}
