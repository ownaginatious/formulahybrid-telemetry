package ca.formulahybrid.network.communicator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.formulahybrid.telemetry.exception.UnreachableException;

public abstract class NetworkStatistics {

    private static Logger logger = Logger.getLogger(NetworkStatistics.class);
    private static long MILLISECOND_IN_NANOSECONDS = 1000000;

    public static float speedToHost(InetAddress address) throws UnreachableException{

        float speedAssessment = 0;
        
        for(int i = 0; i < 10; i++){
            
            try {

                long duration = System.nanoTime();

                boolean reacheable = address.isReachable(5000);

                duration = System.nanoTime() - duration;
                
                if(!reacheable)
                        throw new UnreachableException("Destination "
                                + address.getHostAddress() + " unreachable.");
                else
                    speedAssessment += duration;

            } catch (IOException e) {
                e.printStackTrace();
                // We failed to reach the host; add the maximum timeout;
                speedAssessment += 5000 * MILLISECOND_IN_NANOSECONDS;
            }
        }

        speedAssessment /= 10 * MILLISECOND_IN_NANOSECONDS;

        return speedAssessment;
    }
    
    public static Set<InetAddress> getBroadcastInterfaces(){

        // Let's make sure we do not start getting IPv6 broadcast addresses.
        Properties p = System.getProperties();

        String preferIPv4StackValue = p.getProperty("java.net.preferIPv4Stack");
        p.setProperty("java.net.preferIPv4Stack", "true");

        Set<InetAddress> broadcastAddresses = new HashSet<InetAddress>();

        try {
            
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {

                NetworkInterface networkInterface = interfaces.nextElement();

                try {
                    
                    // Let's skip the loopback interface.
                    if (networkInterface.isLoopback())
                        continue;

                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {

                        InetAddress broadcast = interfaceAddress.getBroadcast();

                        if (broadcast != null)
                            broadcastAddresses.add(broadcast);
                    }
                    
                } catch(SocketException se){
                    
                    logger.debug(
                            String.format("Crashed while attempting to access data about network interface %s. Ignoring.",
                                    networkInterface));
                    continue;
                }
            }
        } catch(SocketException se){
            
            logger.debug("No access to network interfaces. Cannot detect broadcast addresses.");
            return broadcastAddresses;
            
        } finally {

            // Revert the state of the property, if possible.
            p.setProperty("java.net.preferIPv4Stack", preferIPv4StackValue == null ? "false" : preferIPv4StackValue);
        }

        return broadcastAddresses;
    }
}
