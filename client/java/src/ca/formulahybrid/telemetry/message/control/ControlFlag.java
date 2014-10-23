package ca.formulahybrid.telemetry.message.control;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.google.common.base.Charsets;

import ca.formulahybrid.telemetry.message.ControlFlagDescriptor;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

public abstract class ControlFlag extends Exception implements TelemetryMessage {
    
    // Identifier for "Telemetry Control Flag"
    public static byte[] PROTOCOL_HEADER = "TCF".getBytes(Charsets.UTF_8);
    
    // We do not care about serialization.
    private static final long serialVersionUID = 1L;
    
    protected byte[] rawPayload;
    protected DataInputStream payloadStream;
    
    public ControlFlag(byte[] payload) {
        
        this.rawPayload = payload;
        
        if(this.rawPayload != null)
            payloadStream = new DataInputStream(new ByteArrayInputStream(rawPayload));
    }
    
    public final byte[] getPayloadBytes(){
    
        return this.rawPayload;
    }
    
    public final byte[] getBytes(){
     
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // Get the message header.
        ControlFlagDescriptor cfd = this.getClass().getAnnotation(ControlFlagDescriptor.class);
        
        try {
            
            dos.write(ControlFlag.PROTOCOL_HEADER);
            
            if(this instanceof AlienControlFlag)
                dos.writeByte(((AlienControlFlag) this).getId());
            else
                dos.writeByte(cfd.id());

            if(this.rawPayload == null){
                
                dos.write(this.rawPayload.length);
                dos.write(this.rawPayload);
            } else
                dos.write(0);
            
        } catch (IOException e) {} // Cannot happen in this context.
        
        return baos.toByteArray();
    }
}