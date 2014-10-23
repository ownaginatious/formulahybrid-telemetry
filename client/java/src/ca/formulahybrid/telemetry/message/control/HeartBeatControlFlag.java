package ca.formulahybrid.telemetry.message.control;

import ca.formulahybrid.telemetry.message.ControlFlagDescriptor;

@ControlFlagDescriptor(id = 0, length = 0)
public class HeartBeatControlFlag extends ControlFlag {
   
    // We do not care about serializing this message.
    private static final long serialVersionUID = 1L;
    
    public HeartBeatControlFlag() {
        super(null);
    }
    
    public HeartBeatControlFlag(byte[] payload) {
        
        super(payload);
    }
}
