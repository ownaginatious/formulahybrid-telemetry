package ca.formulahybrid.telemetry.message.highvoltage;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.CMessage;
import ca.formulahybrid.telemetry.message.MessageDescriptor;
import ca.formulahybrid.telemetry.message.MessageOrigin;

//FIXME: Replace with actual message ID.
@MessageDescriptor(
		
	id = 0000000000,
	origin = MessageOrigin.HIGHVOLTAGE,
	length = 6
)
public class HighVoltageKeepAlive extends CMessage {

	private int batteryLevel;
	private boolean dragWingEnabled;
	
	//FIXME: Find exact meaning of value.
	private byte electricMotorStatus;
	
	public HighVoltageKeepAlive(Date timeStamp, byte[] payload) throws IOException {
		
		super(timeStamp, payload);
		
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
