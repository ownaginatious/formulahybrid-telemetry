package ca.formulahybrid.telemetry.message.control;

import ca.formulahybrid.telemetry.message.ControlFlagDescriptor;

//FIXME: Replace with actual message ID.
@ControlFlagDescriptor(id = 0, length = 0)
public class StreamStoppedControlFlag extends ControlFlag {

    // We do not care about serializing this message.
    private static final long serialVersionUID = 1L;
    
    public StreamStoppedControlFlag(byte[] payload) {
        
        super(payload);
    }

    public StreamStoppedControlFlag() {
        
        super(null);
    }
}
