package ca.formulahybrid.telemetry.message.pseudo;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import ca.formulahybrid.telemetry.message.MessageDescriptor;
import ca.formulahybrid.telemetry.message.MessageOrigin;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

@MessageDescriptor(

        id = 0000000000,
        origin = MessageOrigin.UNSPECIFIED,
        length = 5
       )
@PseudoMessage
public class TelemetryThrownMessage extends TelemetryMessage {

    private UUID terminator;
    
    public TelemetryThrownMessage(Date timeStamp, int sequenceNumber, int messageId, byte[] payload) throws IOException {
        
        super(timeStamp, sequenceNumber, messageId, payload);
        this.terminator = new UUID(this.payloadStream.readLong(), this.payloadStream.readLong());
    }
    
    public UUID getTerminator(){
        
        return terminator;
    }
}