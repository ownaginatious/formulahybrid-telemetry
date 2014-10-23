package ca.formulahybrid.network.groupcomm;

public enum State {
    
    // Listener states.
    LISTENINGUDP(Origin.LISTENER),
    LISTENINGTCP(Origin.LISTENER),

    // Relay states.
    RELAYING(Origin.RELAY);
    
    public final Origin origin;
    
    State(Origin origin){
        
        this.origin = origin;
    }
}
