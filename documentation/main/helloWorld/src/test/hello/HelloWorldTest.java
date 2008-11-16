package test.hello;

import hello.*;
import junit.framework.*;

public class HelloWorldTest extends TestCase {

    public HelloWorldTest(String name) {
        super(name);
    }
    
    public void testSayHello() {
        HelloWorld hello = new HelloWorld();
        assertEquals("Hello world", hello.sayHello());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(HelloWorldTest.class);
    }    
    
}
