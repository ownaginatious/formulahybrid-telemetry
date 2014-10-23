package ca.formulahybrid.telemetry.message.vehicle;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.VehicleMessageDescriptor;
import ca.formulahybrid.telemetry.message.VehicleMessageOrigin;

//FIXME: Replace with actual message ID.
@VehicleMessageDescriptor(
		
	id = 0000000000,
	origin = VehicleMessageOrigin.DASHBOARD,
	length = 1
)
public class DashBoardVehicleMessage extends VehicleMessage {

	private boolean declutchInitialized;
	private boolean dragReduceEnabled;
	private boolean torqueVectoringEnabled;
	private boolean regenBreaksEnabled;
	private boolean throttleEnabled;
	private boolean combustionEngineActive;
	
	public DashBoardVehicleMessage(Date timeStamp, int sequenceNumber, byte[] payload) throws IOException {
		
		super(timeStamp, sequenceNumber,  payload);
		
		byte status = payloadStream.readByte();
		
		declutchInitialized = (status & 0b00100000) != 0;
		dragReduceEnabled = (status & 0b00010000) != 0;
		torqueVectoringEnabled = (status & 0b00001000) != 0;
		regenBreaksEnabled = (status & 0b00000100) != 0;
		throttleEnabled = (status & 0b00000010) != 0;
		combustionEngineActive = (status & 0b00000001) != 0;
	}

	public boolean combustionEngineDeclutchInitialized() {
		
		return declutchInitialized;
	}

	public boolean dragReductionSystemEnabled() {
		
		return dragReduceEnabled;
	}

	public boolean torqueVectoringEnabled() {
		
		return torqueVectoringEnabled;
	}

	public boolean regenerativeBreakingEnabled() {
		
		return regenBreaksEnabled;
	}

	public boolean electricMotorThrottleEnabled() {
		
		return throttleEnabled;
	}

	public boolean combustionEngineActive() {
		
		return combustionEngineActive;
	}
}
