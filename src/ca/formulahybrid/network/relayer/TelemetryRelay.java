package ca.formulahybrid.network.relayer;

import java.net.InetAddress;
import java.util.Set;
import java.util.UUID;

public interface TelemetryRelay {

    public void connect(InetAddress address, int port);
    public void disconnect();
    
    public Set<UUID> getConnections();
    
    public boolean connected();
    public boolean wasThrown();
   
    
    public void addDestination(TelemetryDestination td);
}
