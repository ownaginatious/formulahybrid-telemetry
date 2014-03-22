package ca.formulahybrid.network.relayer;

import java.io.IOException;

import ca.formulahybrid.telemetry.message.TelemetryMessage;

public interface TelemetryDestination {

    public void sendBinary(byte[] message) throws IOException;
    public void sendMessage(TelemetryMessage message) throws IOException;
}
