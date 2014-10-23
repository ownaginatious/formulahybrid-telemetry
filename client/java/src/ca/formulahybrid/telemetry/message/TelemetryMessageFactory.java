package ca.formulahybrid.telemetry.message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.reflections.Reflections;

import com.google.common.base.Charsets;

import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.message.control.AlienControlFlag;
import ca.formulahybrid.telemetry.message.control.ControlFlag;
import ca.formulahybrid.telemetry.message.vehicle.AlienVehicleMessage;
import ca.formulahybrid.telemetry.message.vehicle.VehicleMessage;

public class TelemetryMessageFactory {

    private static Map<Short, Constructor<? extends VehicleMessage>> vehicleMessageMap = new HashMap<Short, Constructor<? extends VehicleMessage>>();
    private static Map<Short, Integer> vehicleMessageSizeMap = new HashMap<Short, Integer>();

    private static Map<Short, Constructor<? extends ControlFlag>> controlFlagMap = new HashMap<Short, Constructor<? extends ControlFlag>>();
    private static Map<Short, Integer> controlFlagSizeMap = new HashMap<Short, Integer>();

    private static Logger logger = Logger.getLogger(TelemetryMessageFactory.class);
    
	static {
		
		// Find all the classes extending VehicleMessage.
		Reflections reflections = new Reflections("ca.formulahybrid.telemetry.message.vehicle");
		Set<Class<? extends VehicleMessage>> vehicleMessages = reflections.getSubTypesOf(VehicleMessage.class);
		
		for(Class<? extends VehicleMessage> type : vehicleMessages){
			
		    // Ignore the alien class.
		    if(type.getClass().equals(AlienVehicleMessage.class))
		        continue;
		        
			// Read the message descriptor for each message.
			VehicleMessageDescriptor vmd = type.getAnnotation(VehicleMessageDescriptor.class);
				
			short messageId = vmd.id();
			
			// Get the constructor for this message.
			try {
				
			    Constructor<? extends VehicleMessage> constructor = type.getConstructor(new Class[]{Date.class, Integer.class, byte[].class});
			    
				if(vehicleMessageMap.containsKey(messageId))
		            throw new DataException("Attempted to insert duplicate id [" + messageId + "] for " 
		                        + constructor.getName() + ", which was already reserved for "
		                        + vehicleMessageMap.get(messageId).getName() + ".");
		        
				vehicleMessageMap.put(messageId, constructor);
				vehicleMessageSizeMap.put(messageId, vmd.length());
				
			} catch (SecurityException e) { // Only accessing public constructors.
			} catch (NoSuchMethodException e) {} // Enforced by extension.
			catch (DataException e) {
				
				e.printStackTrace();
				System.exit(1);
			}
		}
		
	    // Find all the classes extending ControlFlag.
        reflections = new Reflections("ca.formulahybrid.telemetry.message.control");
        Set<Class<? extends ControlFlag>> controlFlags = reflections.getSubTypesOf(ControlFlag.class);
        
        for(Class<? extends ControlFlag> type : controlFlags){
            
            // Ignore the alien class.
            if(type.getClass().equals(AlienControlFlag.class))
                continue;
            
            // Read the message descriptor for each message.
            ControlFlagDescriptor cfd = type.getAnnotation(ControlFlagDescriptor.class);
                
            short messageId = cfd.id();
            
            // Get the constructor for this message.
            try {
                
                Constructor<? extends ControlFlag> constructor = type.getConstructor(new Class[]{Date.class, Integer.class, byte[].class});
                
                if(controlFlagMap.containsKey(messageId))
                    throw new DataException("Attempted to insert duplicate id [" + messageId + "] for " 
                                + constructor.getName() + ", which was already reserved for "
                                + vehicleMessageMap.get(messageId).getName() + ".");
                
                controlFlagMap.put(messageId, constructor);
                controlFlagSizeMap.put(messageId, cfd.length());
                
            } catch (SecurityException e) { // Only accessing public constructors.
            } catch (NoSuchMethodException e) {} // Enforced by extension.
            catch (DataException e) {
                
                e.printStackTrace();
                System.exit(1);
            }
        }
	}
	
	public static TelemetryMessage parseTelemetryMessage(byte[] b) throws IOException {
	
		return buildTelemetryMessage(new ByteArrayInputStream(b));
	}
	
	public static TelemetryMessage buildTelemetryMessage(InputStream is) throws IOException {
		
		// Convert to a data stream.
		DataInputStream dis = new DataInputStream(is);
		
		byte[] header = new byte[3];
		
		boolean isVehicleMessage = false;

        // Check the headers to ensure this is a control flag or a vehicle message.
		if(!header.equals(VehicleMessage.protocolHeader)) // Check if it is a vehicle message.
			isVehicleMessage = true;
		else if(!header.equals(ControlFlag.PROTOCOL_HEADER)) // Check if it is a control flag.
		    isVehicleMessage = false;
		else
		    throw new IOException(new StringBuilder().append("Sync error. Expected either the protocol headers : ")
		            .append(new String(VehicleMessage.protocolHeader, Charsets.UTF_8)).append(" or ")
		            .append(new String(ControlFlag.PROTOCOL_HEADER, Charsets.UTF_8)).append(". Received ")
		            .append(new String(header, Charsets.UTF_8)).append(".").toString());
        
		if(isVehicleMessage)
		    return buildVehicleMessage(dis);
		else
		    return buildControlFlag(dis);
	}
	
	private static ControlFlag buildControlFlag(DataInputStream dis) throws IOException{
	    
        // Read the message ID.
        int messageId = (int) dis.readShort();
        
        int length = (int) dis.readShort();
        
        // Retrieve the payload.
        byte[] payload = new byte[length];
        dis.read(payload);
        
        // If we do not recognize the ID, return an alien message.
        if(!vehicleMessageMap.containsKey(messageId))
            return new AlienControlFlag(messageId, payload);
        
        Constructor<? extends ControlFlag> controlFlagConstructor = controlFlagMap.get(messageId);
        
        int expectedLength = controlFlagSizeMap.get(messageId);
        
        if(expectedLength != length){
            
            logger.debug("Message with recognized ID [" + messageId + "] ("
                    + controlFlagConstructor.getName() + ") arrived of length "
                    + length + ", however a length of " + expectedLength + " was expected. Returning alien.");
            
            return new AlienControlFlag(messageId, payload);
        }
        
        try {
            
            // Build a new instance of the message.
            return controlFlagConstructor.newInstance(dis);
            
        } catch (IllegalArgumentException e) { // Impossible given already checked restrictions.
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {}
        
        return null;
	}
	
	private static VehicleMessage buildVehicleMessage(DataInputStream dis) throws IOException {
	    
        // Read the message ID.
        int messageId = (int) dis.readShort();
        
        // Read in the date.
        int unixTime = dis.readInt();
        
        // Convert the date from UNIX time.
        Date date = new Date();
        date.setTime(unixTime * 1000);
        
        // Read the sequencing number of the message.
        int sequenceNumber = dis.readInt();
        int length = (int) dis.readShort();
        
        // Buffer the payload.
        byte[] payload = new byte[length];
        dis.read(payload);
        
        // If we do not recognize this message, generate an alien message.
        if(!vehicleMessageMap.containsKey(messageId))
            return new AlienVehicleMessage(date, messageId, sequenceNumber, payload);
        
        // Retrieve the constructor for this message.
        Constructor<? extends VehicleMessage> vehicleMessageConstructor = vehicleMessageMap.get(messageId);
        
        int expectedLength = vehicleMessageSizeMap.get(messageId);
        
        if(expectedLength != length){
            
            logger.debug("Message with recognized ID [" + messageId + "] ("
                    + vehicleMessageConstructor.getName() + ") arrived of length "
                    + length + ", however a length of " + expectedLength + " was expected. Returning alien.");
            
            return new AlienVehicleMessage(date, messageId, sequenceNumber, payload);
        }
        
        try {
            
            // Build a new instance of the message.
            return vehicleMessageConstructor.newInstance(date, sequenceNumber, payload);
            
        } catch (IllegalArgumentException e) { // Impossible given already checked restrictions.
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {}
        
        return null;
	}
}
