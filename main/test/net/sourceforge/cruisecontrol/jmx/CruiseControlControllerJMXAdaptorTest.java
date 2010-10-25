/*
 * Created on Oct 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.jmx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.ProjectInterface;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

/**
 * @author alwick
 */
public class CruiseControlControllerJMXAdaptorTest extends TestCase {

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    private CruiseControlControllerJMXAdaptor adaptor;

    protected void setUp() throws Exception {
        super.setUp();
        adaptor = new CruiseControlControllerJMXAdaptor(new CruiseControlController());
    }

    protected void tearDown() throws Exception {
        filesToDelete.delete();
    }


    public void testShouldReturnAllProjectWithStatus() throws Exception {
        final ProjectConfig projectConfig = new ProjectConfig() {
            private static final long serialVersionUID = 1L;

            public String getName() {
                return "test";
            }

            public String getStatus() {
                return "now building";
            }
            
            public String getBuildStartTime() {
                return "20070420061700";
            }
            
            public boolean isPaused() {
                return false;
            }
        };
        adaptor = new CruiseControlControllerJMXAdaptor(new CruiseControlController() {
            public List<ProjectInterface> getProjects() {
                final List<ProjectInterface> list = new ArrayList<ProjectInterface>();
                list.add(projectConfig);
                return list;
            }
        });
        Map projectsStatus = adaptor.getAllProjectsStatus();
        assertEquals("now building since 20070420061700", projectsStatus.get("test"));
    }

    public void testShouldReturnPausedAsStatus() throws Exception {
        final ProjectConfig projectConfig = new ProjectConfig() {
            private static final long serialVersionUID = 1L;

            public String getName() {
                return "test";
            }

            public String getStatus() {
                return "waiting for next time to build";
            }
            
            public String getBuildStartTime() {
                return "20070420061700";
            }
            
            public boolean isPaused() {
                return true;
            }
        };
        adaptor = new CruiseControlControllerJMXAdaptor(new CruiseControlController() {
            public List<ProjectInterface> getProjects() {
                final List<ProjectInterface> list = new ArrayList<ProjectInterface>();
                list.add(projectConfig);
                return list;
            }
        });
        Map projectsStatus = adaptor.getAllProjectsStatus();
        assertEquals("paused", projectsStatus.get("test"));        
    }
    
    public void testShouldReturnBuildingAsStatusWhenProjectIsBuildingAndPaused() throws Exception {
        final ProjectConfig projectConfig = new ProjectConfig() {
            private static final long serialVersionUID = 1L;

            public String getName() {
                return "test";
            }

            public String getStatus() {
                return "now building";
            }
            
            public String getBuildStartTime() {
                return "20070420061700";
            }
            
            public boolean isPaused() {
                return true;
            }
        };
        adaptor = new CruiseControlControllerJMXAdaptor(new CruiseControlController() {
            public List<ProjectInterface> getProjects() {
                final List<ProjectInterface> list = new ArrayList<ProjectInterface>();
                list.add(projectConfig);
                return list;
            }
        });
        Map projectsStatus = adaptor.getAllProjectsStatus();
        assertEquals("now building since 20070420061700", projectsStatus.get("test"));        
    }


    public void testShouldNotIncludeBuildStartTimeInWaitingStatus() throws Exception {
        final ProjectConfig projectConfig = new ProjectConfig() {
            private static final long serialVersionUID = 1L;
            
            public String getName() {
                return "test";
            }
            
            public String getStatus() {
                return "waiting for next time to build";
            }
            
            public String getBuildStartTime() {
                return "20070420061700";
            }
           
            public boolean isPaused() {
                return false;
            }
        };
        adaptor = new CruiseControlControllerJMXAdaptor(new CruiseControlController() {
            public List<ProjectInterface> getProjects() {
                final List<ProjectInterface> list = new ArrayList<ProjectInterface>();
                list.add(projectConfig);
                return list;
            }
        });
        Map projectsStatus = adaptor.getAllProjectsStatus();
        assertEquals(projectConfig.getStatus(), projectsStatus.get("test"));
    }

    public void testInvalid() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("<cruisecontrol>");
            sb.append("<project name=\"test\" foo=\"foo\"></project>");
            sb.append("</cruisecontrol>");

            adaptor.validateConfig(sb.toString());
            fail("No exception found");
        } catch (CruiseControlException cce) {
            // expected
        }
    }

    public void testValid() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("<cruisecontrol>");
            sb.append("<project name=\"test\">");
            sb.append("<modificationset><cvs localworkingcopy=\".\"/></modificationset>");
            sb.append("<schedule><ant/></schedule>");
            sb.append("</project>");
            sb.append("</cruisecontrol>");

            filesToDelete.add(new File(TestUtil.getTargetDir(), "logs"));

            adaptor.validateConfig(sb.toString());

        } catch (CruiseControlException cce) {
            fail("Validation failed on valid config, reason: " + cce.getMessage());
        }
    }

    public void testReadConfigFileContents() throws Exception {
        final String configContents = "<cruisecontrol>\n"
            + "<project name=\"test\"\n>"
            + "\t<modificationset><cvs localworkingcopy=\".\"/></modificationset>\n"
            + "    <schedule><ant/></schedule>\n"
            + "</project>\n"
            + "</cruisecontrol>";
        final BufferedReader theConfigFileReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(
                configContents.getBytes())));
        final StringBuffer actualContents = new StringBuffer();
        try {
            CruiseControlControllerJMXAdaptor.readConfigFileContents(actualContents, theConfigFileReader);
        } finally {
            theConfigFileReader.close();
        }
        assertEquals(configContents + "\n", // trailing newline always added 
                actualContents.toString());
    }
    
    public void testGetPluginCSS() {
        String css = adaptor.getPluginCSS().trim();
        assertTrue("CSS not loaded", css.length() > 1000);
    }
    
}
