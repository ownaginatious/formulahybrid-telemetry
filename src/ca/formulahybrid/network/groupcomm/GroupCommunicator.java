package ca.formulahybrid.network.groupcomm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import ca.formulahybrid.telemetry.exception.BeaconException;

public class GroupCommunicator {
	
	public static enum Channel { DISCONNECTED, DISCOVER, LISTEN, RELAY }
	
	// Subnet broadcast information.
	public static InetAddress SUBNET_BROADCAST_ADDR;
	public static final int SUBNET_BROADCAST_PORT = 8466;
	
	static {
		
		try {
			
			SUBNET_BROADCAST_ADDR = InetAddress.getByName("255.255.255.255");
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
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
		
		this.channel = Channel.DISCOVER;
		
		if(groupSocket != null){
			
			groupSocket.close();
			groupSocket = null;
		}
		
		// Create a broadcast socket that can only see and send broadcast messages.
		relaySocket = new DatagramSocket(SUBNET_BROADCAST_PORT);
		
		this.outboundSockets = new DatagramSocket[]{};
		this.inboundSocket = this.relaySocket;
	}

	public synchronized void switchToListenerChannel(InetAddress groupAddress, int groupPort) throws IOException{
		
		this.channel = Channel.LISTEN;
		
		if(this.relaySocket != null){
			
			// Get rid of the relay socket.
			this.relaySocket.close();
			this.relaySocket = null;
		}
		
		// Create a group socket that can only see and send group messages.
		groupSocket = new MulticastSocket();
		
		// Receive incoming messages destined for the group.
		groupSocket.joinGroup(groupAddress);
		
		// Restrict communication to the group.
		groupSocket.connect(new InetSocketAddress(groupAddress, groupPort));
		
		this.outboundSockets = new DatagramSocket[]{ this.groupSocket };
		this.inboundSocket = this.groupSocket;
	}
	
	public synchronized void switchToRelayChannel() throws SocketException{
		
		if(this.channel == Channel.RELAY)
			return;
		
		this.channel = Channel.RELAY;
		
		if(this.relaySocket == null)
			this.relaySocket = new DatagramSocket(SUBNET_BROADCAST_PORT);
		
		this.outboundSockets = new DatagramSocket[]{ this.relaySocket, this.groupSocket };
		this.inboundSocket = this.groupSocket;
	}
	
	public InetAddress generateGroupAddress(){

		Random r = new Random();

		byte[] addr = new byte[4];

		addr[0] = (byte) 238;
		
		while(true){
			
			try {

				for(int i = 1; i < 4; i++)
					addr[i] = (byte)(r.nextInt() % 253 + 1);
				
				return InetAddress.getByAddress(addr);

			} catch (UnknownHostException e) {} // We will keep trying until we get something valid.
		}
	}

	public InetAddress generateGroupPort(){

		try {
			new ServerSocket(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
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
	
	/**
	 * Blocks until a new beacon is received from a remote host.
	 * 
	 * @return
	 * @throws BeaconException 
	 * @throws IOException
	 */
	public synchronized Beacon receiveBeacon() throws BeaconException, IOException {
		
		Beacon b;
		
		while(true){
			
			byte[] buffer = new byte[1000];
			DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
			
			this.inboundSocket.receive(dp);
			
			b = BeaconFactory.parseBeacon(dp.getData());
			b.origin = dp.getAddress();
			
			if(b != null)
				return b;
		}
	}
}