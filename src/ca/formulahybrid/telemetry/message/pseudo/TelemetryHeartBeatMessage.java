package ca.formulahybrid.telemetry.message.pseudo;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.TelemetryMessage;

public class TelemetryHeartBeatMessage extends TelemetryMessage {

    public TelemetryHeartBeatMessage(Date timeStamp, int sequenceNumber, int messageId, byte[] payload) throws IOException {
        super(timeStamp, sequenceNumber, messageId, payload);
    }
}
