package ca.formulahybrid.telemetry.message.rearslave;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.CMessage;
import ca.formulahybrid.telemetry.message.MessageDescriptor;
import ca.formulahybrid.telemetry.message.MessageOrigin;

//FIXME: Replace with actual message ID.
@MessageDescriptor(
		
	id = 0000000000,
	origin = MessageOrigin.REARSLAVE,
	length = 5
)
public class RearSlaveKeepAlive extends CMessage {

	//FIXME: Get actual byte-length of this message.
	
	//FIXME: Get the actual meaning of this value.
	private short engineStatus;
	private int currentGearLevel;
	private int targetGearLevel;
	private int backLeftWheelSpeed;
	private int backRightWheelSpeed;
	
	public RearSlaveKeepAlive(Date timeStamp, int messageId, byte[] payload) throws IOException {
		
		super(timeStamp, messageId,  payload);
		
		engineStatus = payloadStream.readShort();
		
		byte gearLevels = payloadStream.readByte();
		
		currentGearLevel = (int)(gearLevels >> 4);
		targetGearLevel = (int)(gearLevels & 0x0f);
		
		backLeftWheelSpeed = payloadStream.readByte();
		backRightWheelSpeed = payloadStream.readByte();
	}

	public short getEngineStatus() {
		
		return engineStatus;
	}

	public int getCurrentGearLevel() {
		
		return currentGearLevel;
	}

	public int getTargetGearLevel() {
		
		return targetGearLevel;
	}

	public int getBackLeftWheelSpeed() {
		
		return backLeftWheelSpeed;
	}

	public int getBackRightWheelSpeed() {
		
		return backRightWheelSpeed;
	}
}