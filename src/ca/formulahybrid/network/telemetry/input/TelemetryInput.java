package ca.formulahybrid.network.telemetry.input;

import java.io.IOException;

import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.exception.TelemetryTimeOutException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

public interface TelemetryInput {
    
    public TelemetryMessage getMessage() throws IOException, TelemetryException;
    
    public TelemetryMessage getMessage(long timeout) throws IOException, TelemetryException, TelemetryTimeOutException;
    
    public boolean connected();
    
    public void close();
}