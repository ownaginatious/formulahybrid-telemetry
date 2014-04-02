package ca.formulahybrid.telemetry.message.vehicle;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.VehicleMessageDescriptor;
import ca.formulahybrid.telemetry.message.VehicleMessageOrigin;

//FIXME: Replace with actual message ID.
@VehicleMessageDescriptor(
		
	id = 0000000000,
	origin = VehicleMessageOrigin.THROTTLECONTROL,
	length = 1
)
public class ThrottleControlVehicleMessage extends VehicleMessage {

	boolean servoOpen;
	
	public ThrottleControlVehicleMessage(Date timeStamp, int sequenceNumber, byte[] payload) throws IOException {
		
		super(timeStamp, sequenceNumber,  payload);
		
		servoOpen = payloadStream.readBoolean();
	}
	
	public boolean isServoOpen(){
		
		return servoOpen;
	}
}
