package ca.formulahybrid.telemetry.message.dashboard;

import java.io.IOException;

import java.util.Date;

import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.MessageDescriptor;
import ca.formulahybrid.telemetry.message.MessageOrigin;

//FIXME: Replace with actual message ID.
@MessageDescriptor(
		
	id = 0000000000,
	origin = MessageOrigin.DASHBOARD,
	length = 1
)
public class DashBoardGearShift extends TelemetryMessage {

	private boolean shiftUp = false;
	
	public DashBoardGearShift(Date timeStamp, int sequenceNumber, int messageId, byte[] payload) throws IOException {
		
		super(timeStamp, sequenceNumber, messageId,  payload);
		
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
