/**
 * 
 */
package net.sourceforge.cruisecontrol;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;

import net.sourceforge.cruisecontrol.util.DateUtil;

public class MockProjectInterface implements ProjectInterface {

    private String name;
    private Foo foo;

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void configureProject() throws CruiseControlException {
    }

    public void execute() {
    }

    public void getStateFromOldProject(ProjectInterface project) throws CruiseControlException {
    }

    public void register(MBeanServer server) throws JMException {
    }

    public void setBuildQueue(BuildQueue buildQueue) {
    }

    public void start() {
    }

    public void stop() {
    }

    public void validate() throws CruiseControlException {
    }

    public Foo createFoo() {
        this.foo = new Foo();
        return foo;
    }
    
    public Foo getFoo() {
        return foo;
    }

    public class Foo {
        
        private String name;
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }

    public Map<String, String> getProperties() {
        return Collections.EMPTY_MAP;
    }

    public List<Modification> modificationsSinceLastBuild() {
        return Collections.EMPTY_LIST;
    }

    public Date successLastBuild() {
        return DateUtil.getMidnight();
    }

    public String getLogDir() {
        return null;
    }

    public List<Modification> modificationsSince(Date since) {
        return Collections.EMPTY_LIST;
    }

    public String successLastLabel() {
        return "";
    }

    public String successLastLog() {
        return "";
    }
}