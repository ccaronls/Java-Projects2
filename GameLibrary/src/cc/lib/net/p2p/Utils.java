package cc.lib.net.p2p;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class Utils {

    public static InetAddress getNetworkInterface(String name) throws IOException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)) {
            if (netint.getName().equalsIgnoreCase(name)) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    return inetAddress;
                }
            }
        }
        throw new IOException("Network interface " + name + " not found.  Use scanNetwork to list all available interfaces");
    }

    /**
     * Find a network interface based on display name.  Useful for finding WiFi, Bluetooth etc.
     * @param displayNameLike
     * @return
     * @throws IOException
     */
    public static InetAddress findNetworkInterface(String displayNameLike) throws IOException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        String name = displayNameLike.toLowerCase();
        for (NetworkInterface netint : Collections.list(nets)) {
            if (netint.getDisplayName().toLowerCase().contains(name)) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    return inetAddress;
                }
            }
        }
        throw new IOException("Network interface " + displayNameLike + " not found.  Use scanNetwork to list all available interfaces");
    }

    public static void dumpNetworkInterfaces() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                System.out.printf("Display name: %s\n", netint.getDisplayName());
                System.out.printf("Name: %s\n", netint.getName());
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    System.out.printf("InetAddress: %s\n", inetAddress);
                }
                System.out.printf("\n");            
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
