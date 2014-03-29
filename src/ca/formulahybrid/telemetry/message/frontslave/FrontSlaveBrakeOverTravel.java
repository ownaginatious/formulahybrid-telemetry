package ca.formulahybrid.telemetry.message.frontslave;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.MessageDescriptor;
import ca.formulahybrid.telemetry.message.MessageOrigin;

@MessageDescriptor(
		
	id = 0000000000,
	origin = MessageOrigin.FRONTSLAVE,
	length = 0
)
public class FrontSlaveBrakeOverTravel extends TelemetryMessage {

	public FrontSlaveBrakeOverTravel(Date timeStamp, int sequenceNumber, int messageId, byte[] payload) throws IOException {
		
		super(timeStamp, sequenceNumber, messageId,  payload);
	}

}
