package ca.formulahybrid.telemetry.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.common.base.Charsets;

public abstract class TelemetryMessage {

	// Identifier as "CAN Telemetry Network Message".
	public static final byte[] protocolIdentifier = "CTNM".getBytes(Charsets.UTF_8);
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	protected Date timeStamp;
	
	protected byte[] rawPayload;
    
	protected int sequenceNumber;
	protected int length;
	protected int messageId;
	protected short id;
	protected MessageOrigin origin;
	protected DataInputStream payloadStream;
	
	public TelemetryMessage(Date timeStamp, int sequenceNumber, int messageId, byte[] payload) throws IOException {

	    this.sequenceNumber = sequenceNumber;
        this.timeStamp = timeStamp;
		this.messageId = messageId;
        this.rawPayload = payload;
		
		MessageDescriptor md = this.getClass().getAnnotation(MessageDescriptor.class);
				
		length = md.length();
		origin = md.origin();
		id = md.id();
		
		payloadStream = new DataInputStream(new ByteArrayInputStream(rawPayload));
	}
	
	public final byte[] getBytes(){
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream daos = new DataOutputStream(baos);
		
		try {
			
		    daos.write(TelemetryMessage.protocolIdentifier);
			daos.writeInt( (int)(timeStamp.getTime()/1000) );
			daos.writeShort(id);
            daos.writeShort(rawPayload.length);
			daos.write(rawPayload);
			
		} catch (IOException e) {} // Impossible.
		
		return baos.toByteArray();
	}
	
	public final byte[] getPayloadBytes(){
		
		return rawPayload;
	}
	
	public final Date getTimeStamp(){
		
		return timeStamp;
	}
	
	public final int getMessageId(){
	
		return messageId;
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
