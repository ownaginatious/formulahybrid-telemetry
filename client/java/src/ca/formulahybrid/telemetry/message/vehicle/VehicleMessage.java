package ca.formulahybrid.telemetry.message.vehicle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ca.formulahybrid.telemetry.message.VehicleMessageDescriptor;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

import com.google.common.base.Charsets;

public abstract class VehicleMessage implements TelemetryMessage {

	// Identifier as "Telemetry Vehicle Message".
	public static final byte[] protocolHeader = "TVM".getBytes(Charsets.UTF_8);
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	protected Date timeStamp;
	
	protected byte[] rawPayload;
    
	private int sequenceNumber;
	protected DataInputStream payloadStream;
	
	public VehicleMessage(Date timeStamp, int sequenceNumber, byte[] payload) throws IOException {

        this.timeStamp = timeStamp;
	    this.sequenceNumber = sequenceNumber;
        this.rawPayload = payload;
		
		payloadStream = new DataInputStream(new ByteArrayInputStream(rawPayload));
	}
	
	@Override
	public final byte[] getBytes(){
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		VehicleMessageDescriptor vmd = this.getClass().getAnnotation(VehicleMessageDescriptor.class);
		
		try {
			
		    dos.write(VehicleMessage.protocolHeader);
		    
            if(this instanceof AlienVehicleMessage)
                dos.writeShort(((AlienVehicleMessage) this).getId());
            else
                dos.writeShort(vmd.id());
            
			dos.writeInt( (int)(timeStamp.getTime()/1000) );
			dos.writeInt(this.sequenceNumber);
            dos.writeShort(this.rawPayload.length);
            dos.write(this.rawPayload);
			
		} catch (IOException e) {} // Impossible.
		
		return baos.toByteArray();
	}
	
	public final byte[] getPayloadBytes(){
		
		return rawPayload;
	}
	
	public final Date getTimeStamp(){
		
		return timeStamp;
	}
	
	public final int getSequenceNumber(){
	    
	    return this.sequenceNumber;
	}
	
	public String rawDataString(){
		
		StringBuilder sb = new StringBuilder();
		
		boolean space = true;
		
		for(byte x : rawPayload){
			
		      sb.append(String.format("%02x", x & 0xff));
		      
		      if(space)
		    	  sb.append(" ");
		      
		      space = !space;
		}
		
		return "[" + sdf.format(timeStamp) + "] " + sb.toString().trim();
	}
}
