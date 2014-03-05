package ca.formulahybrid.network.groupcomm;

import java.util.HashSet;
import java.util.Set;

public enum State {
    
    // Listener states.
    LISTENINGUDP(Origin.LISTENER),
    LISTENINGTCP(Origin.LISTENER),
    REQUESTINGTCP(Origin.LISTENER),
    AWAITINGDATA(Origin.LISTENER), 

    // Relay states.
    UDPONLY(Origin.RELAY),
    UDPANDTCP(Origin.RELAY),
    TCPONLY(Origin.RELAY);
    
    private static Set<State> relayStates = new HashSet<State>();
    private static Set<State> listenerStates = new HashSet<State>();
    
    static {
        
        for(State s : State.values())
            if(s.getParent() == Origin.LISTENER)
                listenerStates.add(s);
            else
                relayStates.add(s);
    }
    
    private Origin origin;
    
    State(Origin origin){
        
        this.origin = origin;
    }
    
    public Origin getParent(){
        
        return origin;
    }
}
