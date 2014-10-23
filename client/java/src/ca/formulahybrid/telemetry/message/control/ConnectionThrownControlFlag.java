package ca.formulahybrid.telemetry.message.control;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import ca.formulahybrid.telemetry.message.ControlFlagDescriptor;

@ControlFlagDescriptor(id = 0, length = 0)
public class ConnectionThrownControlFlag extends ControlFlag {

    // We do not care about serialization.
    private static final long serialVersionUID = 1L;
    
    private UUID terminator;
    private InetAddress terminatorAddress;
    
    public ConnectionThrownControlFlag(byte[] payload) throws IOException {
        
        super(payload);
        
        this.terminator = new UUID(this.payloadStream.readLong(), this.payloadStream.readLong());
        
        byte[] address = new byte[4];
        this.payloadStream.read(address);
        
        this.terminatorAddress = InetAddress.getByAddress(address);
    }
    
    public UUID getTerminator(){
        
        return terminator;
    }
    
    public InetAddress getTerminatorAddress(){
        
        return this.terminatorAddress;
    }
}