package net.sourceforge.cruisecontrol.servlet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class MainLoopLauncher extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Thread thread;

    public void destroy() {
        super.destroy();
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public void init() throws ServletException {
        super.init();
        Integer jmxport;
        Boolean start;
        try {
            InitialContext ic = new InitialContext();
            Context context = (Context) ic.lookup("java:comp/env");
            jmxport = (Integer) context.lookup("cruisecontrol.jmxport");
            start = (Boolean) context.lookup("cruisecontrol.run.on.start");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
        if (start.booleanValue()) {
            thread = new Thread(new MainLoopRunner(jmxport));
            thread.start();
        }
    }
    
}
