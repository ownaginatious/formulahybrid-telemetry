package ca.formulahybrid.network.groupcomm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ListenerBeacon extends Beacon {
    
    protected int lossRate;
    protected boolean sourceReachable;
    
	protected ListenerBeacon() {}
	
	public ListenerBeacon(UUID identifier, State s, boolean sourceReacheable, float pingSpeed, int lossRate){
	
	    this.identifier = identifier;
	    this.state = s;
	    this.sourceReachable = sourceReacheable;
	    this.pingTime = pingSpeed;
	    this.lossRate = lossRate;
	}

	public int getLossRate(){
	    
	    return this.lossRate;
	}
	
	public boolean sourceReacheable(){
	    
	    return this.sourceReachable;
	}
	
	@Override
	protected void writeRemainingToStream(OutputStream os) throws IOException{
	    
	    DataOutputStream daos = new DataOutputStream(os);
	    
        if(this.isRelay())
            daos.writeByte(lossRate);
	}

	@Override
	public void populateFromStream(InputStream is) throws IOException{
	    
	    DataInputStream dis = new DataInputStream(is);
	    
	    this.lossRate = dis.readByte();
	}
}
