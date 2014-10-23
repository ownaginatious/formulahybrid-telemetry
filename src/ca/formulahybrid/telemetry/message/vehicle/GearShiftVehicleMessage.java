package ca.formulahybrid.telemetry.message.vehicle;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.VehicleMessageDescriptor;
import ca.formulahybrid.telemetry.message.VehicleMessageOrigin;

//FIXME: Replace with actual message ID.
@VehicleMessageDescriptor(
		
	id = 0b00110010000,
	origin = VehicleMessageOrigin.DASHBOARD,
	length = 1
)
public class GearShiftVehicleMessage extends VehicleMessage {

	private boolean shiftUp = false;
	
	public GearShiftVehicleMessage(Date timeStamp, int sequenceNumber, byte[] payload) throws IOException {
		
		super(timeStamp, sequenceNumber,  payload);
		
		if(payloadStream.readBoolean())
			shiftUp = true;
	}

	public boolean shiftingUp(){
		
		return shiftUp;
	}

	public boolean shiftingDown(){
		
		return !shiftUp;
	}
}
