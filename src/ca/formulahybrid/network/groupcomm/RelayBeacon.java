package ca.formulahybrid.network.groupcomm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import com.google.common.base.Charsets;

import ca.formulahybrid.telemetry.connector.TelemetrySource;
import ca.formulahybrid.telemetry.exception.BeaconException;

public class RelayBeacon extends Beacon {
	
    public final class NetworkTelemetrySource extends TelemetrySource {
        
        private int tcpPort = 0;
        private int udpPort = 0;
        
        private InetAddress groupAddress = null;
        
        protected NetworkTelemetrySource(String name, InetAddress address) {
            
            super(name, address);
        }
        
        public InetAddress getGroupAddress(){
            
            return this.groupAddress;
        }
        
        public int getUdpPort(){
            
            return this.udpPort;
        }
        
        public int getTcpPort(){
            
            return this.tcpPort;
        }
        
        public boolean hasTcp(){
            
            return this.tcpPort > 0;
        }
        
        public boolean hasUdp(){
            
            return this.udpPort > 0;
        }
    }
    
    private NetworkTelemetrySource telemetrySource;
    private InetAddress groupAddress;
	
    private UUID takeover;
    private boolean requestHandover = false;
	
	protected RelayBeacon(){}
	
	public RelayBeacon(UUID identifier, State state, float pingSpeed, TelemetrySource ts) {
	    
	    this.identifier = identifier;
	    this.state = state;
	    this.pingTime = pingSpeed;
	    
	    this.telemetrySource = new NetworkTelemetrySource(ts.getName(), ts.getSourceAddress());
	}
	
   public RelayBeacon setReliableChannel(int tcpPort) throws BeaconException{

       if(tcpPort <= 0 | tcpPort > 65535)
           throw new BeaconException("Attempted to set a TCP value of '"
                   + tcpPort + "'. Valid numbers are between 1 and 65535.");
       
        this.telemetrySource.tcpPort = tcpPort;
        
        return this;
    }
	   
	public RelayBeacon setBroadcasting(InetAddress groupAddress, int udpPort) throws BeaconException{
	    
	    if(udpPort <= 0 | udpPort > 65535)
	           throw new BeaconException("Attempted to set a UDP value of '"
	                   + udpPort + "'. Valid numbers are between 1 and 65535.");
	    
	    this.telemetrySource.groupAddress = groupAddress;
	    this.telemetrySource.udpPort = udpPort;
	    
	    return this;
	}
	
	public InetAddress getGroupAddress(){
	    
	    return this.groupAddress;
	}
	
	public NetworkTelemetrySource getTelemetrySource(){
	
	    return this.telemetrySource;
	}
	
	public RelayBeacon requestTakeOver(UUID takeover){
		
		this.requestHandover = true;
		this.takeover = takeover;
		
		return this;
	}
	
	public boolean requestingTakeOver(){
	
	    return this.takeover != null;
	}
	
	public UUID getTakeover() {
		
		return takeover;
	}

	@Override
	protected void writeRemainingToStream(OutputStream os) throws IOException {
		
		DataOutputStream dos = new DataOutputStream(os);
		
		byte[] name = this.telemetrySource.getName().getBytes(Charsets.UTF_8);
		dos.write(name.length);
		dos.write(name);
		
		dos.write(this.telemetrySource.getSourceAddress().getAddress());
		dos.write(this.groupAddress.getAddress());
		
		dos.writeShort((short) this.telemetrySource.udpPort);
		dos.writeShort((short) this.telemetrySource.tcpPort);
		
		if(this.requestHandover){
			
			dos.write(255);
			dos.writeLong(this.takeover.getMostSignificantBits());
			dos.writeLong(this.takeover.getLeastSignificantBits());
		}
		else
			dos.write(0);
	}
	
	@Override
	public void populateFromStream(InputStream is) throws IOException, BeaconException {
		
		DataInputStream dis = new DataInputStream(is);
		
		byte nameLength = dis.readByte();
		
		byte[] nameBuffer = new byte[nameLength];
		
		dis.read(nameBuffer);
		String sourceName = new String(nameBuffer, Charsets.UTF_8);
		
		byte[] addressBuffer = new byte[4];
		
		try {

			dis.read(addressBuffer);
			InetAddress sourceAddress = InetAddress.getByAddress(addressBuffer);
			
			dis.read(addressBuffer);
			
            NetworkTelemetrySource nts = new NetworkTelemetrySource(sourceName, sourceAddress);
            nts.groupAddress = InetAddress.getByAddress(addressBuffer);
                    
			nts.udpPort = (int) dis.readShort() & 0xFFFF;
	        nts.tcpPort = (int) dis.readShort() & 0xFFFF;
	        
            this.telemetrySource = nts;
			
		} catch (UnknownHostException e) {
			throw new BeaconException("Malformed source or group address detected from relay beacon.");
		}
						
		// If requesting hand over.
		if(dis.readBoolean())
			this.takeover = new UUID(dis.readLong(), dis.readLong());
	}
}