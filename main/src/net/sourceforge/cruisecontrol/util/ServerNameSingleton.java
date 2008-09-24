package net.sourceforge.cruisecontrol.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * InetAddress.getLocalHost().getCanonicalHostName() can be slow,
 * on the order of 10 seconds, and the value should rarely change,
 * so worth caching.
 * 
 * @author jfredrick
 */
public final class ServerNameSingleton {
    private static String serverName;
    
    private ServerNameSingleton() {
    }

    public static String getServerName() {
        if (serverName == null) {
            try {
                serverName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                try {
                    // Code from JmxBaseTag
                    // Wouldn't this always cause an exception too?
                    serverName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException again) {
                    serverName = "localhost";
                }
            }
        }
        return serverName;
    }

}
