package ca.formulahybrid.telemetry.message.vehicle;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.VehicleMessageDescriptor;
import ca.formulahybrid.telemetry.message.VehicleMessageOrigin;

@VehicleMessageDescriptor(
		
	id = 0b01000000101,
	origin = VehicleMessageOrigin.REARSLAVE,
	length = 5
)
public class RearSlaveVehicleMessage extends VehicleMessage {

	//FIXME: Get actual byte-length of this message.
	
	//FIXME: Get the actual meaning of this value.
	private short engineStatus;
	private int currentGearLevel;
	private int targetGearLevel;
	private int backLeftWheelSpeed;
	private int backRightWheelSpeed;
	
	public RearSlaveVehicleMessage(Date timeStamp, int sequenceNumber, byte[] payload) throws IOException {
		
		super(timeStamp, sequenceNumber,  payload);
		
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