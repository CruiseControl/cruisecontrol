package net.sourceforge.cruisecontrol.util;

import org.safehaus.uuid.UUID;
import org.safehaus.uuid.UUIDGenerator;

/**
 * Understands a unique identifier for a running build loop.
 */
public final class UniqueBuildloopIdentifier {
    private static UUID identifier;

    private UniqueBuildloopIdentifier() { }

    public static UUID id() {
        if (identifier == null) {
            identifier = UUIDGenerator.getInstance().generateRandomBasedUUID();
        }
        return identifier;
    }
}
