package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;
import org.safehaus.uuid.UUID;

public class UniqueBuildloopIdentifierTest extends TestCase {
    public void testShouldGenerateSameIDForRunningBuildloop() throws Exception {
        UUID first = UniqueBuildloopIdentifier.id();
        UUID second = UniqueBuildloopIdentifier.id();
        assertEquals(first, second);
    }
}
