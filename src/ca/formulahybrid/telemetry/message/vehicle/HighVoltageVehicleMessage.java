package ca.formulahybrid.telemetry.message.vehicle;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.VehicleMessageDescriptor;
import ca.formulahybrid.telemetry.message.VehicleMessageOrigin;

@VehicleMessageDescriptor(
		
	id = 0b10000000101,
	origin = VehicleMessageOrigin.HIGHVOLTAGE,
	length = 6
)
public class HighVoltageVehicleMessage extends VehicleMessage {

	private int batteryLevel;
	private boolean dragWingEnabled;
	
	//FIXME: Find exact meaning of value.
	private byte electricMotorStatus;
	
	public HighVoltageVehicleMessage(Date timeStamp, int sequenceNumber, byte[] payload) throws IOException {
		
		super(timeStamp, sequenceNumber,  payload);
		
		batteryLevel = payloadStream.readByte();
		dragWingEnabled = payloadStream.readBoolean();
		electricMotorStatus = payloadStream.readByte();
	}
	
	public int getBatteryLevel(){
		
		return batteryLevel;
	}
	
	public boolean dragWingEnabled(){
		
		return dragWingEnabled;
	}

	public byte getElectricMotorStatus(){
		
		return electricMotorStatus;
	}
}
