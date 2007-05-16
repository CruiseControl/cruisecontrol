package net.sourceforge.cruisecontrol.servlet;

import net.sourceforge.cruisecontrol.Main;

public class MainLoopRunner implements Runnable {

    private Integer jmxport;
    private Integer rmiport;
    private Main main;

    public MainLoopRunner(Integer jmxport, Integer rmiport) {
        this.jmxport = jmxport;
        this.rmiport = rmiport;
    }

    public void run() {
        String[] args = {"-jmxport", jmxport.toString(), "-rmiport", rmiport.toString()};
        main = new Main();
        main.start(args);
    }

    public void stop() {
        main.stop();
    }

}
