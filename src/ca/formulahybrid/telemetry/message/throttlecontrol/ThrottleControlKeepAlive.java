package ca.formulahybrid.telemetry.message.throttlecontrol;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.MessageDescriptor;
import ca.formulahybrid.telemetry.message.MessageOrigin;

//FIXME: Replace with actual message ID.
@MessageDescriptor(
		
	id = 0000000000,
	origin = MessageOrigin.THROTTLECONTROL,
	length = 1
)
public class ThrottleControlKeepAlive extends TelemetryMessage {

	boolean servoOpen;
	
	public ThrottleControlKeepAlive(Date timeStamp, int messageId, byte[] payload) throws IOException {
		
		super(timeStamp, messageId,  payload);
		
		servoOpen = payloadStream.readBoolean();
	}
	
	public boolean isServoOpen(){
		
		return servoOpen;
	}
}
