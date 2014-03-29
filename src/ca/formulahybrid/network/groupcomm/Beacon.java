package ca.formulahybrid.network.groupcomm;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.UUID;

import com.google.common.base.Charsets;

import ca.formulahybrid.telemetry.exception.BeaconException;

public abstract class Beacon {

	// Identifier as "Listener Network Participation Beacon"
	public static final byte[] protocolIdentifier = "LNPB".getBytes(Charsets.UTF_8);
	
	protected InetAddress origin;
	protected UUID identifier;
	protected float pingTime;
	protected State state;
	
	public InetAddress getOrigin(){
	
	    return this.origin;
	}
	
	public UUID getIdentifier() {

		return identifier;
	}

	public float getPingTime() {

		return pingTime;
	}

	public State getState() {

		return state;
	}

	public boolean isRelay() {

		return this instanceof RelayBeacon;
	}

	public byte[] getBytes(){
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try {
			this.writeToStream(baos);
		} catch (IOException e) {} // Impossible.
		
		return baos.toByteArray();
	}
	
	public abstract void populateFromStream(InputStream is) throws IOException, BeaconException;
	
	protected abstract void writeRemainingToStream(OutputStream os) throws IOException;
	
	public void writeToStream(OutputStream os) throws IOException {

		DataOutputStream daos = new DataOutputStream(os);

		// Protocol identifier.
		daos.write(protocolIdentifier);
		
		// Client identifier.
		daos.writeLong(identifier.getMostSignificantBits());
		daos.writeLong(identifier.getLeastSignificantBits());

		// Client type.
		daos.write((this.isRelay()) ? 255 : 0);
		
		daos.write(state.ordinal());
		
		//#FIXME Commenting out to maintain compilation for commit.
		/*daos.write(this.sourceReachable ? 255 : 0);
		
		if(this.sourceReachable)
			daos.writeFloat(pingSpeed);*/
		
		writeRemainingToStream(os);
	}
}