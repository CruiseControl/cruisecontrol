package net.sourceforge.cruisecontrol.taglib;

import javax.servlet.jsp.tagext.*;

public class NavigationTagExtraInfo extends TagExtraInfo {

    public VariableInfo[] getVariableInfo(TagData data) {
        return new VariableInfo[] {
            new VariableInfo("url",
                             "String",
                             true,
                             VariableInfo.NESTED),
            new VariableInfo("linktext",
                             "String",
                             true,
                             VariableInfo.NESTED)
        };
    }
}
