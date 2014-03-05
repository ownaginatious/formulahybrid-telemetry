package ca.formulahybrid.network.groupcomm;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import ca.formulahybrid.telemetry.exception.BeaconException;

public final class BeaconFactory {
	
	private BeaconFactory(){};
	
	/**
	 * Parses a single beacon message from a byte array.
	 * 
	 * @param b The byte array to parse the beacon message from.
	 * @return A beacon if that is the datagram type that arrived, and null otherwise.
	 * @throws BeaconException If the incoming data was detected as a beacon, but was malformed.
	 */
	public static Beacon parseBeacon(byte[] b) throws BeaconException {
	
		return BeaconFactory.readBeacon(new ByteArrayInputStream(b));
	}
	
	/**
	 * Reads a single beacon message from an input stream.
	 * 
	 * @param is The input stream the beacon is arriving from.
	 * @return A beacon if that is the datagram type that arrived, and null otherwise.
	 * @throws BeaconException If the incoming data was detected as a beacon, but was malformed.
	 */
	public static Beacon readBeacon(InputStream is) throws BeaconException {
		
		DataInputStream dis = new DataInputStream(is);

		Beacon b;
		
		byte[] header = new byte[3];
		
		try {
			
			is.read(header);

			// The received message is probably not intended for this device; ignore it.
			if(!header.equals(Beacon.protocolIdentifier))
				return null;

			// Read the client ID.
			UUID uuid =  new UUID(dis.readLong(), dis.readLong());

			State s = null;
			
			try {
			
			    s = State.values()[dis.readByte()];
			    
			} catch(ArrayIndexOutOfBoundsException e){
			    
			    throw new BeaconException("Invalid beacon state; dropping message.");
			}
			
			// True if a relay beacon, false otherwise.
			if(s.getParent() == Origin.RELAY)
				b = new RelayBeacon();
			else
				b = new ListenerBeacon();
			
			b.identifier = uuid;
			b.state = s;
			
			boolean sourceReacheable = true;
			
			if(b instanceof ListenerBeacon){
			    
			    sourceReacheable = dis.readBoolean();
                ((ListenerBeacon) b).sourceReachable = sourceReacheable;
			}
			
			if(sourceReacheable)
			    b.pingSpeed = dis.readFloat();
			
			b.populateFromStream(is);
			
		} catch(IOException ioe){
			
			throw new BeaconException("The incoming beacon is malformed.");
		}
		
		return b;
	}
}
