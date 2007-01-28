/**
 * 
 */
package net.sourceforge.cruisecontrol;

import javax.management.JMException;
import javax.management.MBeanServer;

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
    
}