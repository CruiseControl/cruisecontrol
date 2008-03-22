package net.sourceforge.cruisecontrol.jmx;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.ModificationSet;
import net.sourceforge.cruisecontrol.Project;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.bootstrappers.AntBootstrapper;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

public class ProjectControllerTest extends TestCase {

    public void testShouldBeAbleToGetCommitMessage() throws Exception {
        Project project = new Project();
        project.setName("TestProject");
        ModificationSet modicationSet = new ModificationSet();
        modicationSet.add(new SVNStub());
        ProjectConfig projectConfig = new ProjectConfig();
        projectConfig.add(new DefaultLabelIncrementer());
        projectConfig.add(modicationSet);

        project.setProjectConfig(projectConfig);

        ProjectMBean controller = new ProjectController(project);
        String[][] message = controller.commitMessages();
        assertEquals(message[0][0], "user1");
        assertEquals(message[0][1], "comment1");
        assertEquals(message[1][0], "user2");
        assertEquals(message[1][1], "comment2");
    }

    public void testShouldRetrieveBuildOutputWhenProjectIsBuilding() throws Exception {
        Project project = new Project();
        project.setName("project1");

        File validFile = new File("project1");
        Util.doMkDirs(validFile);

        AntBootstrapper bootstrapper = new AntBootstrapper();

        bootstrapper.setBuildFile(validFile.getAbsolutePath());
        bootstrapper.setTempFile("notLog.xml");
        bootstrapper.setTarget("init");
        bootstrapper.setAntWorkingDir(validFile.getAbsolutePath());
        bootstrapper.validate();
        try {
            bootstrapper.bootstrap();
        } catch (Exception e) {
            Thread.sleep(2 * 1000);
        } finally {
            IO.delete(validFile);
        }

        ProjectMBean mbean = new ProjectController(project);
        String[] output = mbean.getBuildOutput(new Integer(0));
        assertNotNull(output);
        assertTrue(output.length > 0);
    }

    private class SVNStub implements SourceControl {
        private static final long serialVersionUID = 1L;

        public List getModifications(Date lastBuild, Date now) {
            List modications = new ArrayList();
            Modification modification = new Modification();
            modification.userName = "user1";
            modification.comment = "comment1";
            modications.add(modification);
            modification = new Modification();
            modification.userName = "user2";
            modification.comment = "comment2";
            modications.add(modification);
            return modications;
        }

        public Map getProperties() {
            return null;
        }

        public void validate() throws CruiseControlException {
        }
    }
}
