package ca.formulahybrid.telemetry.message.vehicle;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.VehicleMessageDescriptor;
import ca.formulahybrid.telemetry.message.VehicleMessageOrigin;

@VehicleMessageDescriptor(
		
	id = 0b00000000100,
	origin = VehicleMessageOrigin.FRONTSLAVE,
	length = 0
)
public class OverTravelVehicleMessage extends VehicleMessage {

	public OverTravelVehicleMessage(Date timeStamp, int sequenceNumber, byte[] payload) throws IOException {
		
		super(timeStamp, sequenceNumber,  payload);
	}
}
