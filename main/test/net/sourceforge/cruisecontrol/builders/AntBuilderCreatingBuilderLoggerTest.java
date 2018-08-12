package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.BuildOutputLogger;

import org.jdom2.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AntBuilderCreatingBuilderLoggerTest {
    private final FilesToDelete filesToDelete = new FilesToDelete();

    private TestAntBuilder builder;
    private BuildOutputLogger buildOutputLogger;

    private Map<String, String> buildProperties;
    
    @Before
    public void setUp() throws Exception {
        builder = new TestAntBuilder();

        File buildFile = File.createTempFile("build", ".xml", TestUtil.getTargetDir());
        filesToDelete.add(buildFile);
        builder.setBuildFile(buildFile.getAbsolutePath());

        buildProperties = new HashMap<String, String>();
        buildProperties.put(Builder.BUILD_PROP_PROJECTNAME, "testproject");
    }

    @After
    public void tearDown() throws Exception {
        builder = null;
        buildOutputLogger = null;
        filesToDelete.delete();
    }

    @Test
    public void testShouldCreateBuildOutputLoggerByDefault() throws CruiseControlException {
        builder.validate();
        builder.build(buildProperties, null);
        Assert.assertNotNull(buildOutputLogger);
    }

    @Test
    public void testShouldCreateBuildOutputLoggerWithShowAntOutputTrue() throws CruiseControlException {
        builder.setLiveOutput(true);
        builder.validate();
        builder.build(buildProperties, null);
        Assert.assertNotNull(buildOutputLogger);
    }

    @Test
    public void testShouldNotCreateBuildOutputLoggerWithShowAntOutputFalse() throws CruiseControlException {
        builder.setLiveOutput(false);
        builder.validate();
        builder.build(buildProperties, null);
        Assert.assertNull(buildOutputLogger);
    }

    private class TestAntBuilder extends AntBuilder {
        @Override
        boolean runScript(AntScript script, File workingDir, BuildOutputLogger outputLogger)
                throws CruiseControlException {
            buildOutputLogger = outputLogger;
            return true;
        }

        @Override
        protected Element getAntLogAsElement(File file) throws CruiseControlException {
            return null;
        }
    }
}
