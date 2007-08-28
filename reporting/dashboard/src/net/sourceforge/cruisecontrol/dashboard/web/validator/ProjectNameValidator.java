package net.sourceforge.cruisecontrol.dashboard.web.validator;

import org.apache.commons.lang.StringUtils;

public class ProjectNameValidator {

    private String error;
    static final char BRITISH_POUND = '\u00A3';

    public ProjectNameValidator(String name) {
        error = checkInvalidChars(name, " is not a valid character in a project name.",
            new char[] {
                '"', '!', BRITISH_POUND, '$', '%', '^', '&', '*', '(', ')',
                '+', '=', '#', '~', '?', '/', '<', '>', '[', ']',
                '{', '}', '@', ':', ';', '\\', '|', '\''
            });
        if (StringUtils.isEmpty(error)) {
            error = checkBlank(name);
        }
    }

    private String checkBlank(String name) {
        if (StringUtils.isBlank(name)) {
            return "Project name cannot be blank.";
        }
        return "";
    }

    private String checkInvalidChars(String name, String message, char[] lookout) {
        for (int i = 0; i < lookout.length; i++) {
            if (StringUtils.contains(name, lookout[i])) {
                return "'" + lookout[i] + "'" + message;
            }
        }

        return "";
    }

    public boolean isNotValid() {
        return StringUtils.isNotEmpty(error);
    }

    public String error() {
        return error;
    }

}
