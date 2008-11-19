package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;

public class VetoTest extends TestCase {
    
    public void testValidateFailsWithNoTriggersElement() {
        Veto veto = new Veto();
        String message = "veto requires nested triggers element";
        assertValidateFails(veto, message);
    }
    
    public void testValidateFailsWithEmptryTriggersElement() throws CruiseControlException {
        Veto veto = new Veto();
        veto.createTriggers();
        String message = "Error: there must be at least one source control in a triggers block.";
        assertValidateFails(veto, message);
    }
    
    public void testValidateFailsWithNoBuildStatus() throws CruiseControlException {
        Veto veto = new Veto();
        Triggers t = veto.createTriggers();
        t.add(new MockSourceControl());
        String message = "veto requires a nested buildstatus element";
        assertValidateFails(veto, message);
    }

    public void testValidateFailsWithBuildStatusUnconfigured() throws CruiseControlException {
        Veto veto = new Veto();
        Triggers t = veto.createTriggers();
        t.add(new MockSourceControl());
        veto.createBuildStatus();
        String message = "'logdir' is required for BuildStatus";
        assertValidateFails(veto, message);
    }

    private void assertValidateFails(Veto veto, String message) {
        try {
            veto.validate();
            fail();
        } catch (CruiseControlException expected) {
            assertEquals(message, expected.getMessage());
        }
    }
    
    public void testCreateTriggersCanOnlyBeCalledOnce() throws CruiseControlException {
        Veto veto = new Veto();
        veto.createTriggers();
        try {
            veto.createTriggers();
        } catch (CruiseControlException expected) {
            assertEquals("only one nested triggers allowed", expected.getMessage());
        }
    }

    public void testCreateBuildStatusCanOnlyBeCalledOnce() throws CruiseControlException {
        Veto veto = new Veto();
        veto.createBuildStatus();
        try {
            veto.createBuildStatus();
        } catch (CruiseControlException expected) {
            assertEquals("only one nested buildstatus allowed", expected.getMessage());
        }
    }
    
    public void testGetModificationsShouldReturnEmptyListWhenTriggersHaveNoChanges() throws CruiseControlException {
        MockBuildStatus status = new MockBuildStatus();
        Veto veto = new TestVeto(status);
        veto.createBuildStatus();
        
        SourceControl sc = new MockSourceControl() {
            public List<Modification> getModifications(final Date lastBuild, final Date now) {
                return new ArrayList<Modification>();
            }
        };        
        Triggers triggers = veto.createTriggers();
        triggers.add(sc);

        veto.validate();
        Date notUsedInTest = new Date();
        assertEquals(0, veto.getModifications(notUsedInTest, notUsedInTest).size());
    }

    public void testGetModificationsShouldThrowExceptionWhenBuildStatusShowsNoBuild() throws CruiseControlException {
        MockBuildStatus status = new MockBuildStatus();
        Veto veto = new TestVeto(status);
        veto.createBuildStatus();

        Triggers triggers = veto.createTriggers();
        MockSourceControl sc = new MockSourceControl();
        sc.setType(1);
        triggers.add(sc);
        veto.validate();

        Date notUsedInTest = new Date();
        try {
            veto.getModifications(notUsedInTest, notUsedInTest);
            fail();
        } catch (RuntimeException expected) {
            assertEquals("trigger changes with no buildstatus changes", expected.getMessage());
        }
    }
    
    public void testGetModificationsShouldThrowExceptionWhenBuildStatusOutOfDate() throws CruiseControlException {
        MockBuildStatus status = new MockBuildStatus();
        Veto veto = new TestVeto(status);
        veto.createBuildStatus();

        Triggers triggers = veto.createTriggers();
        MockSourceControl sc = new MockSourceControl();
        sc.setType(1);
        triggers.add(sc);
        veto.validate();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -1);        
        Modification mod = new Modification();
        mod.modifiedTime = cal.getTime();
        status.modifications.add(mod);

        Date notUsedInTest = new Date();
        try {
            veto.getModifications(notUsedInTest, notUsedInTest);
            fail();
        } catch (RuntimeException expected) {
            assertEquals("buildstatus out of date compared to trigger changes", expected.getMessage());
        }
    }
    
    public void testGetModificationsShouldNotThrowExceptionWhenBuildStatusCurrent() throws CruiseControlException {
        MockBuildStatus status = new MockBuildStatus();
        Veto veto = new TestVeto(status);
        veto.createBuildStatus();

        Triggers triggers = veto.createTriggers();
        MockSourceControl sc = new MockSourceControl();
        sc.setType(1);
        triggers.add(sc);
        veto.validate();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, +1);        
        Modification mod = new Modification();
        mod.modifiedTime = cal.getTime();
        status.modifications.add(mod);

        Date notUsedInTest = new Date();
        assertEquals(0, veto.getModifications(notUsedInTest, notUsedInTest).size());
    }
    
     private class MockBuildStatus extends BuildStatus {
        private final ArrayList<Modification> modifications = new ArrayList<Modification>();
        
        public List<Modification> getModifications(final Date lastBuild, final Date unused) {
            return modifications;
        }

        public void validate() throws CruiseControlException {
        }
    }
     
    private final class TestVeto extends Veto {
        private final BuildStatus status;
        
        private TestVeto(final BuildStatus status) {
            this.status = status;
        }

        protected BuildStatus getBuildStatus() {
            return status;
        }
    }

}
