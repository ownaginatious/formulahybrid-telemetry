package ca.formulahybrid.telemetry.message.vehicle;

import java.io.IOException;
import java.util.Date;

import ca.formulahybrid.telemetry.message.VehicleMessageDescriptor;
import ca.formulahybrid.telemetry.message.VehicleMessageOrigin;

@VehicleMessageDescriptor(
        
    id = 0b00000000000,
    length = 0,
    origin = VehicleMessageOrigin.UNKNOWN
)
public class AlienVehicleMessage extends VehicleMessage {

    private int id;
    
    public AlienVehicleMessage(Date timeStamp, int id, int sequenceNumber, byte[] payload) throws IOException {
        
        super(timeStamp, sequenceNumber, payload);
        
        this.id = id;
    }
    
    public int getId(){
        
        return this.id;
    }
}
