package ca.formulahybrid.network.receiver;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

import ca.formulahybrid.telemetry.message.TelemetryMessage;

public interface TelemetryReceiverConnection {
    
    public void connect(InetAddress address, int port, BlockingQueue<TelemetryMessage> bq) throws IOException;
    
    public void disconnect();
    
    public boolean connected();
    
    public int getLossRate();
}