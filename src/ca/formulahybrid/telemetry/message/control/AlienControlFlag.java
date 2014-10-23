package ca.formulahybrid.telemetry.message.control;

import java.io.IOException;

public class AlienControlFlag extends ControlFlag {
    
    // We do not care about serialization.
    private static final long serialVersionUID = 1L;
    
    private int id;
    
    public AlienControlFlag(int id, byte[] payload) throws IOException {
        
        super(payload);
        this.id = id;
    }
    
    public int getId(){
        
        return this.id;
    }
}