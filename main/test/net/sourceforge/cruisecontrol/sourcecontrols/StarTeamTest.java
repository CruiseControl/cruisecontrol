package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class StarTeamTest extends TestCase {

    public StarTeamTest(String name) {
        super(name);
    }

    public void testValidate() {

        StarTeam st = new StarTeam();
        try {
            st.validate();
            fail("StarTeam should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        st.setFolder("folder");
        st.setStarteamurl("url");
        st.setUsername("username");
        st.setPassword("password");

        try {
            st.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("StarTeam should not throw exceptions when required fields are set.");
        }
    }
}
