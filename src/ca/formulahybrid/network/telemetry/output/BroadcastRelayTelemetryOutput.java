package ca.formulahybrid.network.telemetry.output;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.control.ShutDownControlFlag;

public class BroadcastRelayTelemetryOutput implements TelemetryOutput {

    private DatagramSocket ds;
    private InetAddress address;
    private int port;
    
    public BroadcastRelayTelemetryOutput(InetAddress address, int port) throws IOException {

        this.address = address;
        this.port = port;
        
        this.ds = new DatagramSocket();
    }
    
    @Override
    public void sendBinary(byte[] message) throws IOException {
        
        DatagramPacket dp = new DatagramPacket(message, message.length, address, port);
        this.ds.send(dp);
    }

    @Override
    public void sendMessage(TelemetryMessage message) throws IOException {
        
        sendBinary(message.getBytes());
    }
    
    @Override
    public void close() {
        
        if(this.ds != null){
            
            try {
                
                // Try to tell connected listeners that this source is closing down.
                sendMessage(new ShutDownControlFlag());
                this.ds.close();
                
            } catch (IOException e) {}
        }
        
        this.ds = null;
    }

    @Override
    public boolean isConnected() {
        
        return this.ds != null;
    }
    
    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();

        return sb.append("[ TOUTPUT ").append("UDP @ ")
                .append(this.address).append(":")
                .append(this.ds.getPort()).toString();
    }
}
