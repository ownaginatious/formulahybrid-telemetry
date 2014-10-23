package ca.formulahybrid.telemetry.message.control;

import ca.formulahybrid.telemetry.message.ControlFlagDescriptor;

@ControlFlagDescriptor(id = 0, length = 0)
public class ShutDownControlFlag extends ControlFlag {

    // We do not care about serializing this message.
    private static final long serialVersionUID = 1L;

    public ShutDownControlFlag(byte[] payload){
    
        super(payload);
    }
    
    public ShutDownControlFlag(){
        
        super(null);
    }
}
