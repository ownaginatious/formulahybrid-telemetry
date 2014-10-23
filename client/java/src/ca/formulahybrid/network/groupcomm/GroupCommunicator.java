package ca.formulahybrid.network.groupcomm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import ca.formulahybrid.telemetry.exception.BeaconException;

public class GroupCommunicator {

    public static enum Channel { DISCONNECTED, DISCOVER, LISTEN, RELAY }

    // Subnet broadcast information.
    public static InetAddress SUBNET_BROADCAST_ADDR;
    public static final int TELEMETRY_GROUP_COMM_PORT = 8471; // ASCII for TG -> Telemetry Group

    static {

        try {

            SUBNET_BROADCAST_ADDR = InetAddress.getByName("255.255.255.255");

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private Logger logger = Logger.getLogger(GroupCommunicator.class);

    private Thread receiverThread;
    
    private boolean changingChannels = false;

    private BlockingQueue<Beacon> incomingMessages = new LinkedBlockingQueue<Beacon>();

    private MulticastSocket groupSocket;
    private DatagramSocket relaySocket;

    private Channel channel = Channel.DISCONNECTED;

    private DatagramSocket inboundSocket;
    private DatagramSocket[] outboundSockets;

    public synchronized Channel getChannel(){

        return this.channel;
    }

    public synchronized void switchToDiscoveryChannel() throws SocketException{

        if(this.channel == Channel.DISCOVER)
            return;

        this.stopBeaconReceiver();
        
        this.channel = Channel.DISCOVER;

        if(groupSocket != null){

            groupSocket.close();
            groupSocket = null;
        }

        // Create a broadcast socket that can only see and send broadcast messages.
        relaySocket = new DatagramSocket(TELEMETRY_GROUP_COMM_PORT);
        this.relaySocket.connect(SUBNET_BROADCAST_ADDR, TELEMETRY_GROUP_COMM_PORT);
        
        this.outboundSockets = new DatagramSocket[]{};
        this.inboundSocket = this.relaySocket;
        
        this.startBeaconReceiver();
    }

    public synchronized void switchToListenerChannel(InetAddress groupAddress, int port) throws IOException{
            
        this.channel = Channel.LISTEN;

        this.stopBeaconReceiver();
        
        if(this.relaySocket != null){

            // Get rid of the relay socket.
            this.relaySocket.close();
            this.relaySocket = null;
        }
        
        initializeGroupSocket(groupAddress, port);
        
        this.outboundSockets = new DatagramSocket[]{ this.groupSocket };
        this.inboundSocket = this.groupSocket;
        
        this.startBeaconReceiver();
    }
    
    private void initializeGroupSocket(InetAddress groupAddress, int port) throws IOException{
        
        if(this.groupSocket != null)
            if(this.groupSocket.getInetAddress().equals(groupAddress))
                return;
            else
                this.groupSocket.close();
        
        // Create a group socket that can only see and send group messages.
        groupSocket = new MulticastSocket(port);

        // Receive incoming messages destined for the group.
        groupSocket.joinGroup(groupAddress);
        
        // Restrict communication to the group.
        groupSocket.connect(new InetSocketAddress(groupAddress, TELEMETRY_GROUP_COMM_PORT));
    }
    
    public synchronized void switchToRelayChannel(InetAddress groupAddress) throws IOException {
        
        switchToRelayChannel(groupAddress, 0);
    }
    
    public synchronized void switchToRelayChannel(InetAddress groupAddress, int port) throws IOException {

        if(this.channel == Channel.RELAY)
            return;

        this.stopBeaconReceiver();
        
        initializeGroupSocket(groupAddress, 0);
        
        this.channel = Channel.RELAY;
        
        if(this.relaySocket == null){
            
            this.relaySocket = new DatagramSocket(TELEMETRY_GROUP_COMM_PORT);
            this.relaySocket.connect(SUBNET_BROADCAST_ADDR, TELEMETRY_GROUP_COMM_PORT);
        }

        this.outboundSockets = new DatagramSocket[]{ this.relaySocket, this.groupSocket };
        this.inboundSocket = this.groupSocket;
        
        this.startBeaconReceiver();
    }

    public InetAddress generateGroupAddress(){

        Random r = new Random();

        byte[] addr = new byte[4];

        addr[0] = (byte) 239;

        while(true){

            try {

                for(int i = 1; i < 4; i++)
                    addr[i] = (byte)(r.nextInt() % 253 + 1);

                return InetAddress.getByAddress(addr);

            } catch (UnknownHostException e) {} // We will keep trying until we get something valid.
        }
    }

    /**
     * Sends a beacon to members of the multicast group if the communicator is set as a listener,
     * and to the broadcast address if the communicator is set as the relay.
     * 
     * @param b The beacon to be sent.
     * @throws IOException Thrown if there is an issue sending via the out bound socket.
     */
    public synchronized void sendBeacon(Beacon b) throws IOException{

        byte[] beaconBytes = b.getBytes();

        DatagramPacket dp = new DatagramPacket(beaconBytes, beaconBytes.length);

        for(DatagramSocket dgs : this.outboundSockets)
            dgs.send(dp);
    }

    public Beacon getBeacon(){
    
        return this.incomingMessages.poll();
    }
    
    private void stopBeaconReceiver(){
        
        if(this.receiverThread == null)
            return;
        
        this.changingChannels = true;
        this.inboundSocket.close();
        
        try {
            
            this.receiverThread.join();
        
        } catch (InterruptedException e) {}
        
        this.incomingMessages.clear();
    }
    
    private void startBeaconReceiver() {

        if(this.receiverThread != null)
            if(this.receiverThread.isAlive())
                return;
        
        Runnable r = new Runnable(){

            @Override
            public void run() {

                GroupCommunicator.this.changingChannels = false;
                
                try {

                    while(!GroupCommunicator.this.changingChannels){

                        byte[] buffer = new byte[1000];
                        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);

                        GroupCommunicator.this.inboundSocket.receive(dp);

                        try {

                            Beacon b = BeaconFactory.parseBeacon(dp.getData());
                            b.origin = dp.getAddress();

                            incomingMessages.add(b);

                        } catch (BeaconException e) {
                            e.printStackTrace();
                        }
                    }
                } catch(SocketException se){

                    if(!GroupCommunicator.this.changingChannels)
                        se.printStackTrace();

                } catch (IOException ioe) {

                    ioe.printStackTrace();
                }

                logger.info("Beacon receiver terminated.");
            }
        };

        this.receiverThread = new Thread(r);
        this.receiverThread.start();
        
        logger.info("Beacon receiver started on channel [" + GroupCommunicator.this.channel.name() + "]");
    }
}