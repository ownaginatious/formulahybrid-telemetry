package ca.formulahybrid.network.communicator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import ca.formulahybrid.network.groupcomm.Beacon;
import ca.formulahybrid.network.groupcomm.GroupCommunicator;
import ca.formulahybrid.network.groupcomm.ListenerBeacon;
import ca.formulahybrid.network.groupcomm.Origin;
import ca.formulahybrid.network.groupcomm.RelayBeacon;
import ca.formulahybrid.network.groupcomm.State;
import ca.formulahybrid.network.receiver.TelemetryReceiver;
import ca.formulahybrid.network.receiver.TelemetryReceiverConnection;
import ca.formulahybrid.telemetry.connector.TelemetryConnection;
import ca.formulahybrid.telemetry.connector.TelemetrySource;

public class GroupManager {

    // -------------------
    // Behaviour constants
    // -------------------
    private static final float GROUPLOSS_THRESHOLD = (float) 1.2;
    private static final float GROUPLOSS_THRESHOLD_HYSTERESIS = (float) 0.1;

    private static final float SELFLOSS_THRESHOLD = (float) 1.2;
    private static final float SELFLOSS_THRESHOLD_HYSTERESIS = (float) 0.1;

    // -------------------
    
    private static Logger logger = Logger.getLogger(GroupManager.class);
    
    private Set<TelemetrySource> relays = new HashSet<TelemetrySource>();
    
    private GroupCommunicator gc = new GroupCommunicator();

    // --------------------
    // Group member data.
    // --------------------
    
    private UUID identifier;
    private State state;
    
    private boolean sourceReacheable = false;
    private float pingTime;
    private float lossRate;
    
    private boolean relayable;
    
    private TelemetryReceiver trc;
    
    // Timeouts
    private int groupLossTimeOut = 0;
    private int selfLossTimeOut = 0;
    private int takeOverTimeout = 0;
    
    // --------------------

    // ------------------
    // Other member data
    // ------------------
    
    protected Map<UUID, Beacon> neighbours = new HashMap<UUID, Beacon>();
    protected Map<UUID, Integer> neighbourTimeouts = new HashMap<UUID, Integer>();

    // ------------------

    public void processBeacons(){
        
        int udpListenerCount = 0, nodesWithReachCount = 0, pingRank = 0;
        float averageLoss = 0, bestPingTime = this.pingTime;
        
        // Increase the age of each neighbour's last communication.
        for(UUID neighbour : this.neighbourTimeouts.keySet()){
            
            int age = this.neighbourTimeouts.get(neighbour) + 1;
            this.neighbourTimeouts.put(neighbour, age);
        }
        
        Beacon b;

        // Store and reset timeouts for all incoming beacons.
        while((b = gc.getBeacon()) != null){

            // We do not want to store our own beacon.
            if(this.identifier.equals(b.getIdentifier()))
                continue;
            
            this.neighbours.put(b.getIdentifier(), b);
            this.neighbourTimeouts.put(b.getIdentifier(), 0); // Cancel out any timeout for this node.
        }
        
        // Remove all beacons who have timed out.
        for(UUID bId : this.neighbourTimeouts.keySet()){
            
            // Remove any beacons that have disappeared for more than 3 beacon processing cycles (9 seconds).
            if(this.neighbourTimeouts.get(bId) > 3)
                this.neighbours.remove(bId);
        }
        
        // Recalculate all node statistics.
        for(UUID bId : this.neighbours.keySet()){
            
            Beacon neighbour = this.neighbours.get(bId);
            
            boolean canReachSource = false;
            
            if(neighbour instanceof RelayBeacon)
                canReachSource = true; // Relays can always reach the source.
            
            else {
                
                ListenerBeacon lb = (ListenerBeacon) neighbour;
                canReachSource = lb.sourceReacheable();
                
                // All listeners on UDP express a loss rate.
                if(lb.getState() == State.LISTENINGUDP){
                    
                    averageLoss += lb.getLossRate();
                    udpListenerCount++;
                }
            }
            
            if(canReachSource){
                
                nodesWithReachCount++;
                
                float neighbourPing = neighbour.getPingTime();
                
                // If this node's source ping time is better than that of the neighbour being processed,
                // then increase this node's rank by 1.
                pingRank += (this.pingTime <= neighbourPing) ? 1 : 0;
                bestPingTime = (neighbourPing > bestPingTime)? neighbourPing : bestPingTime;
            }
        }
        
        // Calculate the loss factor of this node.
        // Over the interval [-1 ... 1 ]; 0 is average, positive is higher loss rate and -1 is
        // lower than the average.
        averageLoss = ((float) averageLoss + this.lossRate)/ (float) (udpListenerCount + 1);
        float selfToGroupLossRatio = this.lossRate / averageLoss - 1;
        
        // Increase timeouts for self loss.
        if(selfToGroupLossRatio > SELFLOSS_THRESHOLD)
            this.selfLossTimeOut++;
        else if(selfToGroupLossRatio < (SELFLOSS_THRESHOLD - SELFLOSS_THRESHOLD_HYSTERESIS))
            this.selfLossTimeOut = (this.selfLossTimeOut == 0) ? 0 : this.selfLossTimeOut - 1;
        
        // Increase timeouts for group loss.
        if(averageLoss > GROUPLOSS_THRESHOLD)
            this.groupLossTimeOut++;
        else if(averageLoss < (GROUPLOSS_THRESHOLD - GROUPLOSS_THRESHOLD_HYSTERESIS))
            this.groupLossTimeOut = (this.groupLossTimeOut == 0) ? 0 : this.groupLossTimeOut - 1;
        
        // If this node is in the top 20% of source ping times, then it should try to become
        // the relay, given the opportunity.
        relayable = (0.8 * nodesWithReachCount) <= pingRank;
        
        // The purpose of this value is to help reduce the number of nodes that attempt to become a relay
        // to reduce stress on the source.
        //
        // If this node's ping is at least 50 milliseconds better than the best and better by a factor
        // of more than 5, then this node is considerably better than the one supplying it. If this
        // node's ping time is the best ping time, then obviously this node is significantly better.
        // This gets ignored when the node is acting as a relay.
        boolean significantlyBetterPing = (Math.abs(this.pingTime - bestPingTime) > 50)
                                                & (this.pingTime / bestPingTime > 5)
                                                | this.pingTime == bestPingTime;
        
        if(significantlyBetterPing)
            this.takeOverTimeout++;
        else
            this.takeOverTimeout--;
            
        // Perform the appropriate behaviour routine depending on the state of this node.
        if(this.state == null){
            
        }
        else if(this.state.origin == Origin.RELAY){
            
            
        } else if(this.state.origin == Origin.LISTENER) {
            
        }
    }
    
    private void performAsDiscoverer(){
        
        
    }
    
    private void performAsRelay(float groupLoss, int pingRank, boolean significantlyBetterPing){
        
        
    }
    
    private void performAsListener(){
        
        
    }
    
    private void initiateTakeOver(){
        
        
    }
    
    private void transformToListener(){
        
    }
}