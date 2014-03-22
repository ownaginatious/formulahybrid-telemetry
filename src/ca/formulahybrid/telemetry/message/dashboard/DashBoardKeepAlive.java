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
public class DashBoardKeepAlive extends TelemetryMessage {

	private boolean declutchInitialized;
	private boolean dragReduceEnabled;
	private boolean torqueVectoringEnabled;
	private boolean regenBreaksEnabled;
	private boolean throttleEnabled;
	private boolean combustionEngineActive;
	
	public DashBoardKeepAlive(Date timeStamp, int messageId, byte[] payload) throws IOException {
		
		super(timeStamp, messageId,  payload);
		
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
