package net.sourceforge.cruisecontrol.servlet;

import net.sourceforge.cruisecontrol.Main;

public class MainLoopRunner implements Runnable {

    private Integer jmxport;

    public MainLoopRunner(Integer jmxport) {
        this.jmxport = jmxport;
    }

    public void run() {
        String[] args = {"-jmxport", jmxport.toString()};
        new Main().start(args);
    }

}
