package net.sourceforge.cruisecontrol.jmx;

import mx4j.MBeanDescriptionAdapter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dan Rollo
 *         Date: Aug 8, 2010
 *         Time: 10:58:43 PM
 */
public class CruiseControlControllerJMXAdaptorMBeanDescription extends MBeanDescriptionAdapter {

    private static final Map<String, String> METHOD_DESCRIPTIONS;

    static {
        METHOD_DESCRIPTIONS = new HashMap<String, String>();

        METHOD_DESCRIPTIONS.put("pause", "Pauses the server.");
        METHOD_DESCRIPTIONS.put("reloadConfigFile", "Re-read the server configuration file.");
        METHOD_DESCRIPTIONS.put("resume", "Resumes the server when it is paused.");
        METHOD_DESCRIPTIONS.put("halt", "Shutdown this server.");

        METHOD_DESCRIPTIONS.put("getPluginInfo",
                "The PluginInfo tree for the give project, or whole server if projectName parameter is null.");

        METHOD_DESCRIPTIONS.put("getPluginHTML",
                "The HTML plugin content for the give project, or whole server if projectName parameter is null.");

    }

    private static final Map<String, String> METHOD_PARAMETER_NAME;

    static {
        METHOD_PARAMETER_NAME = new HashMap<String, String>();

        METHOD_PARAMETER_NAME.put("ConfigFileName-0", "fileName");

        METHOD_PARAMETER_NAME.put("getPluginInfo-0", "projectName");

        METHOD_PARAMETER_NAME.put("getPluginHTML-0", "projectName");

    }

    private static final Map<String, String> METHOD_PARAMETER_DESCRIPTIONS;

    static {
        METHOD_PARAMETER_DESCRIPTIONS = new HashMap<String, String>();

        METHOD_PARAMETER_DESCRIPTIONS.put("ConfigFileName-0", "The config file with settings for this server.");

        METHOD_PARAMETER_DESCRIPTIONS.put("getPluginInfo-0", "Null to fetch entire tree, or a single project name.");

        METHOD_PARAMETER_DESCRIPTIONS.put("getPluginHTML-0", "Null to fetch entire tree, or a single project name.");
    }

    private static final Map<String, String> ATTR_DESCRIPTIONS;

    static {
        ATTR_DESCRIPTIONS = new HashMap<String, String>();
        ATTR_DESCRIPTIONS.put("ConfigFileName",
                              "The name of the config file this server reads its settings from.");

    }

    public String getOperationDescription(final Method method) {
        final String methodName = method.getName();
        if (METHOD_DESCRIPTIONS.containsKey(methodName)) {
            return METHOD_DESCRIPTIONS.get(methodName);
        }
        return super.getOperationDescription(method);
    }


    public String getOperationParameterName(final Method method, final int index) {
        if (method != null) {
            final String methodName = method.getName() + "-" + index;
            if (METHOD_PARAMETER_NAME.containsKey(methodName)) {
                return METHOD_PARAMETER_NAME.get(methodName);
            }
        }
        return super.getOperationParameterName(method, index);
    }

    public String getOperationParameterDescription(final Method method, final int index) {
        if (method != null) {
            final String methodName = method.getName() + "-" + index;
            if (METHOD_PARAMETER_DESCRIPTIONS.containsKey(methodName)) {
                return METHOD_PARAMETER_DESCRIPTIONS.get(methodName);
            }
        }
        return super.getOperationParameterDescription(method, index);
    }



    public String getAttributeDescription(final String attr) {
        if (ATTR_DESCRIPTIONS.containsKey(attr)) {
            return ATTR_DESCRIPTIONS.get(attr);
        }
        return super.getAttributeDescription(attr);
    }

    public String getMBeanDescription() {
        return "Controller for a CruiseControl server instance";
    }
}
