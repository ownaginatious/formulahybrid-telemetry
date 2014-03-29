package ca.formulahybrid.telemetry.message.pseudo;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.MessageDescriptor;
import ca.formulahybrid.telemetry.message.MessageOrigin;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

@PseudoMessage
@MessageDescriptor(
        
        id = 0000000000,
        origin = MessageOrigin.UNSPECIFIED,
        length = 5
      )
public class TelemetryCloseMessage extends TelemetryMessage {

    public TelemetryCloseMessage() throws IOException{
        super(null, 0, 0, null);
    }
    
    public TelemetryCloseMessage(Date timeStamp, int sequenceNumber, int messageId, byte[] payload) throws IOException {
        
        super(timeStamp, sequenceNumber, messageId, payload);
    }
}
