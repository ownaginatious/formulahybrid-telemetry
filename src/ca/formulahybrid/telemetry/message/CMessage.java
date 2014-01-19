package ca.formulahybrid.telemetry.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class CMessage {

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	protected Date timeStamp;
	protected byte[] rawData;
	protected int length;
	protected short id;
	protected MessageOrigin origin;
	protected DataInputStream payloadStream;
	
	public CMessage(Date timeStamp, byte[] payload) throws IOException {
		
		this.rawData = payload;
		this.timeStamp = timeStamp;
		
		MessageDescriptor md = this.getClass().getAnnotation(MessageDescriptor.class);
				
		length = md.length();
		origin = md.origin();
		id = md.id();
		
		payloadStream = new DataInputStream(new ByteArrayInputStream(rawData));
	}
	
	public final byte[] getFullBytes(){
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream daos = new DataOutputStream(baos);
		
		try {
			
			daos.writeInt( (int)(timeStamp.getTime()/1000) );
			daos.writeShort(id);
			daos.write(rawData);
			
		} catch (IOException e) {} // Impossible.
		
		return baos.toByteArray();
	}
	
	public final byte[] getBytes(){
		
		return rawData;
	}
	
	public final Date getTimeStamp(){
		
		return timeStamp;
	}
	
	public String rawDataString(){
		
		StringBuilder sb = new StringBuilder();
		
		boolean space = true;
		
		for(byte x : rawData){
			
		      sb.append(String.format("%02x", x & 0xff));
		      
		      if(space)
		    	  sb.append(" ");
		      
		      space = !space;
		}
		
		return "[" + sdf.format(timeStamp) + "] " + sb.toString().trim();
	}
}
