package ca.formulahybrid.network.telemetry.input;

import java.io.IOException;

import ca.formulahybrid.network.receiver.TelemetryReceiver.ConnectionType;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.control.ControlFlag;

public interface TelemetryInput {
    
    public TelemetryMessage getMessage() throws IOException, ControlFlag;
    
    public boolean isConnected();
    
    public ConnectionType getConnectionType();
    
    public void close();
}