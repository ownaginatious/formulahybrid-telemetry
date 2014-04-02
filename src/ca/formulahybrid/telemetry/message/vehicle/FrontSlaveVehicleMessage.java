package ca.formulahybrid.telemetry.message.vehicle;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.exception.DataValidityException;
import ca.formulahybrid.telemetry.message.VehicleMessageDescriptor;
import ca.formulahybrid.telemetry.message.VehicleMessageOrigin;

@VehicleMessageDescriptor(
		
	id = 0b00100001111,
	origin = VehicleMessageOrigin.FRONTSLAVE,
	length = 6
)
public class FrontSlaveVehicleMessage extends VehicleMessage {

	private int frontLeftWheelSpeed;
	private int frontRightWheelSpeed;
	private int steeringWheelPosition;
	private int throttleLevel;
	private int breakLevel;
	
	byte checksum;
	
	public FrontSlaveVehicleMessage(Date timeStamp, int sequenceNumber,  byte[] payload) throws IOException {
		
		super(timeStamp, sequenceNumber,  payload);
		
		frontLeftWheelSpeed = payloadStream.readByte();
		frontRightWheelSpeed = payloadStream.readByte();
		steeringWheelPosition = payloadStream.readByte();
		throttleLevel = payloadStream.readByte();
		breakLevel = payloadStream.readByte();
		
		checksum = payloadStream.readByte();
	}

	public int getFrontLeftWheelSpeed() {
		
		return frontLeftWheelSpeed;
	}

	public int getFrontRightWheelSpeed() {
		
		return frontRightWheelSpeed;
	}

	public int getSteeringWheelPosition() throws DataValidityException {
		
		if((0b00000001 & checksum) == 0)
			throw new DataValidityException("This message indicates that the steering position data is not valid.");
		
		return steeringWheelPosition;
	}

	public int getThrottleLevel() throws DataValidityException {
		
		if((0b00001110 & ~checksum) != 0)
			throw new DataValidityException(0b00000111 & (checksum >> 1), "This message indicates that the throttle sensor values are not valid.");
		
		return throttleLevel;
	}

	public int getBreakLevel() throws DataValidityException {
		
		if((0b00001110 & ~checksum) != 0)
			throw new DataValidityException(0b00000111 & (checksum >> 1), "This message indicates that the throttle sensor values are not valid.");
		
		return breakLevel;
	}

	public byte getChecksum() {
		
		return checksum;
	}
}
