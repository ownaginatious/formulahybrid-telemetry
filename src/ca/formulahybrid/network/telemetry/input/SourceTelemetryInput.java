package ca.formulahybrid.network.telemetry.input;

import ca.formulahybrid.telemetry.connector.TelemetrySourceClientConnection;
import ca.formulahybrid.telemetry.exception.TelemetryTimeOutException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

public class SourceTelemetryInput implements TelemetryInput {

    public SourceTelemetryInput(TelemetrySourceClientConnection tscc) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean connected() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public TelemetryMessage getMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TelemetryMessage getMessage(long timeout) throws TelemetryTimeOutException {
        // TODO Auto-generated method stub
        return null;
    }
}
