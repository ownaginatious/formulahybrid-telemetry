package ca.formulahybrid.telemetry.message.throttlecontrol;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.CMessage;
import ca.formulahybrid.telemetry.message.MessageDescriptor;
import ca.formulahybrid.telemetry.message.MessageOrigin;

//FIXME: Replace with actual message ID.
@MessageDescriptor(
		
	id = 0000000000,
	origin = MessageOrigin.THROTTLECONTROL,
	length = 1
)
public class ThrottleControlKeepAlive extends CMessage {

	boolean servoOpen;
	
	public ThrottleControlKeepAlive(Date timeStamp, byte[] payload) throws IOException {
		super(timeStamp, payload);
		
		servoOpen = payloadStream.readBoolean();
	}
	
	public boolean isServoOpen(){
		
		return servoOpen;
	}
}
