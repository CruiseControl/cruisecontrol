package net.sourceforge.cruisecontrol.webtest;

import junit.framework.TestCase;
import org.apache.tools.ant.util.FileUtils;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ConfigReloadingTest extends TestCase {
    private MBeanServerConnection server;

    protected void setUp() throws Exception {
        super.setUp();
        JMXServiceURL address = new JMXServiceURL("service:jmx:rmi://localhost:7856/jndi/jrmp");

        Map environment = new HashMap();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
        environment.put(Context.PROVIDER_URL, "rmi://localhost:7856");

        JMXConnector cntor = JMXConnectorFactory.connect(address, environment);
        server = cntor.getMBeanServerConnection();
    }

    public void testChangingConfigFileOnDisk() throws Exception {
        ObjectName ccMgr = ObjectName.getInstance("CruiseControl Manager:id=unique");
        ObjectName ccProj = ObjectName.getInstance("CruiseControl Project:name=connectfour");

        //Look at the current interval value in JMX.
        long currentJMXInterval = ((Long) server.getAttribute(ccProj, "BuildInterval")).longValue() / 1000;

        //Look at the current interval value in the config file on disk
        String pathToConfig = (String) server.getAttribute(ccMgr, "ConfigFileName");
        File configFile = new File(pathToConfig);
        String cruiseConfiguration = FileUtils.readFully(new FileReader(configFile));
        int currentFileInterval = getInterval(cruiseConfiguration);
        assertEquals("Build interval from JMX does not match interval from config file.", currentJMXInterval,
                currentFileInterval);

        //Increment and set the interval value in the config file on disk
        int newInterval = currentFileInterval + 100;
        PrintWriter configWriter = new PrintWriter(new FileOutputStream(configFile));
        configWriter.println(cruiseConfiguration.replaceFirst("interval=\"" + currentFileInterval + "\"",
                "interval=\"" + newInterval + "\""));
        configWriter.flush();
        configWriter.close();

        //Force a build, to trigger CruiseControl to reload the config file
        server.invoke(ccProj, "build", null, null);

        //Look at the current interval value in JMX
        long timeToStopWaiting = System.currentTimeMillis() + 30000;
        long newJMXInterval = ((Long) server.getAttribute(ccProj, "BuildInterval")).longValue() / 1000;
        while (timeToStopWaiting > System.currentTimeMillis()
                && newJMXInterval != newInterval) {
            Thread.sleep(1000);
            newJMXInterval = ((Long) server.getAttribute(ccProj, "BuildInterval")).longValue() / 1000;
        }

        assertEquals(
                "CruiseControl hasn't reloaded the configuration file. The schedule interval is still the OLD value.",
                newInterval, newJMXInterval);
    }


    private int getInterval(String cruiseConfiguration) {
        int startIndex = cruiseConfiguration.indexOf("interval=\"") + 10;
        int endIndex = cruiseConfiguration.substring(startIndex).indexOf("\"") + startIndex;
        return Integer.parseInt(cruiseConfiguration.substring(startIndex, endIndex));
    }
}
