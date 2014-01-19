package ca.formulahybrid.telemetry.message;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

import ca.formulahybrid.telemetry.exception.DataException;

public class CMessageFactory {
	
	private Map<Short, Constructor<? extends CMessage>> idMap = new HashMap<Short, Constructor<? extends CMessage>>();
	private Map<Short, Integer> sizeMap = new HashMap<Short, Integer>();
	
	public CMessageFactory() throws DataException {
		
		// Find all the classes extending CMessage.
		Reflections reflections = new Reflections("ca.formulahybrid.telemetry.message");
		Set<Class<? extends CMessage>> messageTypes = reflections.getSubTypesOf(CMessage.class);
		
		for(Class<? extends CMessage> type : messageTypes){
			
			// Read the message descriptor for each message.
			MessageDescriptor md = type.getAnnotation(MessageDescriptor.class);
				
			short messageId = md.id();
			
			// Get the constructor for this message.
			try {
				
				Constructor<? extends CMessage> constructor = type.getConstructor(new Class[]{Date.class, byte[].class});
				putWithCheck(messageId, md.length(), constructor);
				
			} catch (SecurityException e) { // Only accessing public constructors.
			} catch (NoSuchMethodException e) {} // Enforced by extension.
			
		}
	}
	
	private void putWithCheck(short id, int length, Constructor<? extends CMessage> constructor) throws DataException {
		
		if(idMap.containsKey(idMap))
			throw new DataException("Attempted to insert duplicate id [" + id + "] for " 
						+ constructor.getName() + ", which was already reserved for " + idMap.get(id).getName() + ".");
		
		idMap.put(id, constructor);
		sizeMap.put(id, length);
		
	}
	
	public CMessage buildMessage(InputStream is) throws IOException, DataException{
		
		// Convert to a data stream.
		DataInputStream dis = new DataInputStream(is);
		
		// Read in the date.
		int unixTime = dis.readInt();
		
		Date date = new Date();
		date.setTime(unixTime * 1000);
				
		//TODO: Find exact message ID length.
		short messageId = dis.readShort();
		
		// Build the message.
		if(!idMap.containsKey(messageId))
			throw new DataException("Unrecognized message id [" + messageId + "]");
		
		Constructor<? extends CMessage> messageConstructor = idMap.get(messageId);
		
		// Get the payload for this message.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		int length = sizeMap.get(messageId);
		
		for(int i = 0; i < length; i++)
			baos.write(dis.readByte());
		
		try {
			
			// Build a new instance of the message.
			return messageConstructor.newInstance(date, is);
			
		} catch (IllegalArgumentException e) { // Impossible given already checked restrictions.
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {}
		
		return null;
	}
}
