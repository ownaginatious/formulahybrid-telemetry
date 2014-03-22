package ca.formulahybrid.telemetry.connector;

import java.net.InetAddress;

public interface TelemetrySourceClientConnection {

    public void connect(InetAddress address, int port);
    public void disconnect();
    
    
}
