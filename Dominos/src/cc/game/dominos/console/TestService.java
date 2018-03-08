package cc.game.dominos.console;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

/**
 * Created by chriscaron on 3/6/18.
 */

public class TestService {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Listenering on 1234");

        try {
            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Register a service
            ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", "example", 1234, "path=index.html");
            jmdns.registerService(serviceInfo);

            // Wait a bit
            Thread.sleep(60*1000);

            // Unregister all services
            jmdns.unregisterAllServices();

        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        System.out.println("All done");
        System.exit(0);
    }

}
