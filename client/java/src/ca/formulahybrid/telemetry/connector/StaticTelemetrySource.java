package ca.formulahybrid.telemetry.connector;

import java.net.InetAddress;

public class StaticTelemetrySource extends TelemetrySource {

    public StaticTelemetrySource(String name, InetAddress address) {
        
        super(name, address);
    }
}
