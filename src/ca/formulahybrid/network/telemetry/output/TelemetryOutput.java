package ca.formulahybrid.network.telemetry.output;

import java.io.IOException;

import ca.formulahybrid.telemetry.message.TelemetryMessage;

public interface TelemetryOutput {

    public void sendBinary(byte[] message) throws IOException;
    public void sendMessage(TelemetryMessage message) throws IOException;
    
    public void close();
}
