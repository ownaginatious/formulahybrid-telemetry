package ca.formulahybrid.network.communicator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.formulahybrid.network.groupcomm.Beacon;
import ca.formulahybrid.network.groupcomm.GroupCommunicator;
import ca.formulahybrid.network.groupcomm.GroupCommunicator.Channel;
import ca.formulahybrid.network.groupcomm.BeaconFactory;
import ca.formulahybrid.network.groupcomm.ListenerBeacon;
import ca.formulahybrid.network.groupcomm.RelayBeacon;
import ca.formulahybrid.network.groupcomm.State;
import ca.formulahybrid.telemetry.connector.TelemetrySource;
import ca.formulahybrid.telemetry.exception.BeaconException;
import ca.formulahybrid.telemetry.message.CMessage;

public class GroupManager {

    private GroupCommunicator groupComm;

    private Set<TelemetrySource> sources = new HashSet<TelemetrySource>();

    private Map<UUID, Beacon> neighbours = new HashMap<UUID, Beacon>();
    private BlockingQueue<Beacon> incomingBeacons = new LinkedBlockingQueue<Beacon>();
    
    private float pingTime = -1;
    private byte lossRate;
    private boolean sourceReacheable = false;

    private static Pattern unixPingTimePattern = Pattern.compile(".+ time=(?<time>[0-9.]+) ms$");
    private static Pattern windowsPingTimePattern = Pattern.compile(".+ Average = (?<time>[0-9.]+)ms$");

    private Object pingUpdateLock = new Object();
    private Object sourceUpdateLock = new Object();
    
    private boolean tryPing = true;
    private State s;
    
    private InetAddress presentRelay;
    
    private UUID identifier;
    
    public GroupManager() throws IOException {
        
        // Generate a unique ID for this device. The chances of a randomly generated clash
        // in a listener network is basically zero.
        this.identifier = UUID.randomUUID();
        
        //#FIXME Commenting out for commit.
       // new Thread(this).start();
    }

    public Set<TelemetrySource> discoverRelay() throws IOException {
        
        return null;
    }

    private void sendBeaconRoutine(){

        Channel currentChannel = this.groupComm.getChannel();
        
        // We only want to send beacons while listening or relaying.
        if(currentChannel == Channel.DISCONNECTED | currentChannel == Channel.DISCOVER)
            return;
        
        Beacon b = null;

        //#FIXME Commenting out to maintain compilation for commit.
        /*
        if(currentChannel = Channel.RELAY){
            
            b = new RelayBeacon(UUID );
        } else*/
    }

    private void processBeaconsRoutine(){

        Beacon b = null;

        // Process all incoming beacons into the neighbours map.
        while((b = this.incomingBeacons.poll()) != null){

            UUID identifier = b.getIdentifier();

            // We want to maintain stability if another node with the same UUID shows up.
            // We will only listen to the one that appeared first.
            if(this.neighbours.keySet().contains(identifier)){
                if(this.neighbours.get(identifier).getOrigin().equals(b.getOrigin()))
                    this.neighbours.put(identifier, b);
            } else
                this.neighbours.put(identifier, b);
        }
        
        // Determine what information should be analyzed based on the current channel.
        switch(this.groupComm.getChannel()){
        
            case DISCOVER:
                
                // Here we only want to analyze only relay beacons.
                
                for(UUID node : this.neighbours.keySet()){
                    
                    Beacon neighbour = this.neighbours.get(node);
                    
                    if(neighbour instanceof RelayBeacon){
                        
                        String sourceName = ((RelayBeacon) neighbour).getSourceName();
                        InetAddress sourceAddress = ((RelayBeacon) neighbour).getSourceAddress();
                    }
                }
                break;
                
            case LISTEN:
                
                break;
                
            case RELAY:
                
                break;
                
        default:
            break;
        }
    }

    private void updateDistanceVectorRoutine(){

        Pattern pingTimePattern = null;

        if(!tryPing)
            return;
        
        try {
            
            String cmd = "ping";

            if(System.getProperty("os.name").startsWith("Windows")) {

                // For Windows.
                cmd += " -n 10 " + presentRelay.getHostAddress();
                pingTimePattern = windowsPingTimePattern;

            } else {
                
                // For Linux, BSD and Mac OS X.
                cmd += " -c 10 " + presentRelay.getHostAddress();
                pingTimePattern = unixPingTimePattern;
            }

            Process myProcess = Runtime.getRuntime().exec(cmd);

            if(myProcess.waitFor() != 0){
                
                synchronized(pingUpdateLock) {
                    
                    this.tryPing = false;
                }
                
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(myProcess.getInputStream()));

            String line = null, tempLine;

            // Skip to the end of the output.
            while ((tempLine = br.readLine()) != null)
                line = tempLine;
           
            Matcher m = pingTimePattern.matcher(line);

            synchronized(pingUpdateLock){
                
                if(m.find()){
    
                    this.pingTime = Float.parseFloat(m.group("time"));
                    this.tryPing = true;
                }
                else
                    this.tryPing = false;
            }
        } catch (IOException | InterruptedException | NumberFormatException e) {
            
            e.printStackTrace();
        }
    }

    private void beaconReceiveThreadMethodRoutine() {

        // This thread is the "listening" component of the group manager, setting
        // flags appropriately.
        try {

            // Create a new communicator for this group.
            this.groupComm = new GroupCommunicator();
            this.groupComm.switchToDiscoveryChannel();
            
            //TODO: Implement kill flagging.
            while(true){

                Beacon b = null;
                
                try {
                    
                    b = this.groupComm.receiveBeacon();
                    
                } catch (BeaconException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                incomingBeacons.add(b);
            }

            // Add the beacon to the incoming queue.
        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }
}