package ca.formulahybrid.network.telemetry.output;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import ca.formulahybrid.telemetry.message.TelemetryMessage;

public class BroadcastRelayTelemetryOutput implements TelemetryOutput {

    private DatagramSocket ds;
    
    public BroadcastRelayTelemetryOutput(InetAddress address, int port) throws SocketException {

        this.ds = new DatagramSocket();
        this.ds.connect(address, port);
    }
    
    @Override
    public void sendBinary(byte[] message) throws IOException {
        
        DatagramPacket dp = new DatagramPacket(message, message.length);
        this.ds.send(dp);
    }

    @Override
    public void sendMessage(TelemetryMessage message) throws IOException {
        
        sendBinary(message.getPayloadBytes());
    }
    
    @Override
    public void close() {
        
        if(this.ds != null)
            this.ds.close();
        
        this.ds = null;
    }
}
