package ca.formulahybrid.network.communicator;

import java.io.IOException;
import java.net.InetAddress;

import ca.formulahybrid.telemetry.exception.UnreachableException;

public abstract class ConnectionStatistics {

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
}
