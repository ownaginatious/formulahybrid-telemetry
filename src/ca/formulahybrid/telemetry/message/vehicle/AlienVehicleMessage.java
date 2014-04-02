package ca.formulahybrid.telemetry.message.vehicle;

import java.io.IOException;
import java.util.Date;

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
