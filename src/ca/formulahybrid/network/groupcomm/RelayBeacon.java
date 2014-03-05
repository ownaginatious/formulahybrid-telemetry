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
	
    protected TelemetrySource telemetrySource;
    protected String sourceName;
	protected InetAddress sourceAddress;
	protected InetAddress groupAddress;
	protected UUID takeover;
	protected int udpPort = -1;
	protected int tcpPort = -1;
	protected boolean requestHandover = false;
	
	protected RelayBeacon(){}
	
	public RelayBeacon(UUID identifier, State state, float pingSpeed, TelemetrySource ts) {
	    
	    this.identifier = identifier;
	    this.state = state;
	    this.pingSpeed = pingSpeed;
	    this.telemetrySource = ts;
	}
	
   public RelayBeacon setReliableChannel(int tcpPort) throws BeaconException{

       if(tcpPort <= 0 | tcpPort > 65535)
           throw new BeaconException("Attempted to set a TCP value of '"
                   + tcpPort + "'. Valid numbers are between 1 and 65535.");
       
        this.tcpPort = tcpPort;
        
        return this;
    }
	   
	public RelayBeacon setBroadcasting(InetAddress groupAddress, int udpPort) throws BeaconException{
	    
	    if(tcpPort <= 0 | tcpPort > 65535 | udpPort <= 0 | udpPort > 65535)
            throw new BeaconException("Attempted to set a UDP or TCP value out of range. Valid numbers are "
                    + "between 1 and 65535.");
	    
	    this.groupAddress = groupAddress;
	    this.udpPort = udpPort;
	    
	    return this;
	}
	
	public RelayBeacon requestHandOver(UUID takeover){
		
		this.requestHandover = true;
		this.takeover = takeover;
		
		return this;
	}
	
	public String getSourceName(){
	    
	    return sourceName;
	}
	
   public InetAddress getSourceAddress(){
        
        return sourceAddress;
    }
	   
	public InetAddress getGroupAddress() {
		
		return groupAddress;
	}

	public UUID getTakeover() {
		
		return takeover;
	}

	public int getTcpPort() {
		
		return tcpPort;
	}

	@Override
	protected void writeRemainingToStream(OutputStream os) throws IOException {
		
		DataOutputStream dos = new DataOutputStream(os);
		
		byte[] name = this.sourceName.getBytes(Charsets.UTF_8);
		dos.write(name.length);
		dos.write(name);
		
		dos.write(this.sourceAddress.getAddress());
		dos.write(this.groupAddress.getAddress());
		
		if(this.state == State.UDPONLY || this.state == State.UDPANDTCP)
			dos.writeShort((short) this.udpPort);
		
		if(this.state == State.TCPONLY || this.state == State.UDPANDTCP)
			dos.writeShort((short) this.tcpPort);
		
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
		this.sourceName = new String(nameBuffer, Charsets.UTF_8);
		
		byte[] addressBuffer = new byte[4];
		
		try {

			dis.read(addressBuffer);
			this.sourceAddress = InetAddress.getByAddress(addressBuffer);

			dis.read(addressBuffer);
			this.groupAddress = InetAddress.getByAddress(addressBuffer);
			
		} catch (UnknownHostException e) {
			throw new BeaconException("Malformed source or group address detected from relay beacon.");
		}
		
		if(this.state == State.UDPONLY || this.state == State.UDPANDTCP)
			this.udpPort = (int) dis.readShort() & 0xFFFF;
			
		if(this.state == State.UDPONLY || this.state == State.UDPANDTCP)
			this.tcpPort = (int) dis.readShort() & 0xFFFF;
						
		// If requesting hand over.
		if(dis.readBoolean())
			this.takeover = new UUID(dis.readLong(), dis.readLong());
	}
}