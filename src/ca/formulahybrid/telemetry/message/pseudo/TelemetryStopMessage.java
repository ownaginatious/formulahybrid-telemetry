package ca.formulahybrid.telemetry.message.pseudo;

import java.io.IOException;

import java.util.Date;

import ca.formulahybrid.telemetry.message.MessageDescriptor;
import ca.formulahybrid.telemetry.message.MessageOrigin;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

//FIXME: Replace with actual message ID.
@MessageDescriptor(
      
  id = 0000000000,
  origin = MessageOrigin.UNSPECIFIED,
  length = 5
)
@PseudoMessage
public class TelemetryStopMessage extends TelemetryMessage {

    public TelemetryStopMessage(Date timeStamp, int sequenceNumber, int messageId, byte[] payload) throws IOException {
        
        super(timeStamp, sequenceNumber, messageId, payload);
    }

}
