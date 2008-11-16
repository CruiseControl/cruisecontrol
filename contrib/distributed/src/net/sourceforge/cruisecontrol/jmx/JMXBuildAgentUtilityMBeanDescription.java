package net.sourceforge.cruisecontrol.jmx;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import mx4j.MBeanDescriptionAdapter;

/**
 * @author Dan Rollo
 * Date: Sep 27, 2008
 * Time: 12:32:18 PM
 */
public class JMXBuildAgentUtilityMBeanDescription extends MBeanDescriptionAdapter {

    private static final Map<String, String> METHOD_DESCRIPTIONS;

    static {
        METHOD_DESCRIPTIONS = new HashMap<String, String>();

        METHOD_DESCRIPTIONS.put("destroyLUS", "Stop the Lookup Service with the given ServiceId.");
        METHOD_DESCRIPTIONS.put("refresh", "Reload information about Build Agents.");
        METHOD_DESCRIPTIONS.put("kill", "Kill the Build Agent who's ServiceId is specified.");
        METHOD_DESCRIPTIONS.put("killAll", "Kill all Build Agents.");
        METHOD_DESCRIPTIONS.put("restart", "Restart the (webstart) Build Agent who's ServiceId is specified. NOTE: The "
                + "agent specified MUST have been launched via webstart, or this call will fail.");
        METHOD_DESCRIPTIONS.put("restartAll", "Restart all (webstart) Build Agents.");
    }

    private static final Map<String, String> ATTR_DESCRIPTIONS;

    static {
        ATTR_DESCRIPTIONS = new HashMap<String, String>();
        ATTR_DESCRIPTIONS.put("LookupServiceCount", "The number of Lookup Services (Registrars) found.");
        ATTR_DESCRIPTIONS.put("LUSServiceIds", "The ServiceId of Lookup Services (Registrars) found.");
        ATTR_DESCRIPTIONS.put("BuildAgents", "A big knarly string representation of all Build Agents found.");
        ATTR_DESCRIPTIONS.put("BuildAgentServiceIds", "Use the ServiceId (the part after '<hostname>: ') as the "
                + "parameter value to kill() or restart() calls. The ServiceId uniquely identifies a Build Agent.");
        ATTR_DESCRIPTIONS.put("KillOrRestartAfterBuildFinished",
                "If true, any invocation of kill or restart on a busy agent will wait until the currently running "
                + "build finishes. If false, invocation of kill or restart will occur immediately, even if the agent "
                + "is currently busy building a project.");
    }

    public String getOperationDescription(Method method) {
        String methodName = method.getName();
        if (METHOD_DESCRIPTIONS.containsKey(methodName)) {
            return METHOD_DESCRIPTIONS.get(methodName);
        }
        return super.getOperationDescription(method);
    }

    public String getAttributeDescription(String attr) {
        if (ATTR_DESCRIPTIONS.containsKey(attr)) {
            return ATTR_DESCRIPTIONS.get(attr);
        }
        return super.getAttributeDescription(attr);
    }

    public String getMBeanDescription() {
        return "Distributed Build Agent utility";
    }
}


