package ca.formulahybrid.network.telemetry.input;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import ca.formulahybrid.network.receiver.TelemetryReceiver.ConnectionType;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;
import ca.formulahybrid.telemetry.message.control.ControlFlag;
import ca.formulahybrid.telemetry.message.control.HeartBeatControlFlag;
import ca.formulahybrid.telemetry.message.control.ShutDownControlFlag;
import ca.formulahybrid.telemetry.message.vehicle.VehicleMessage;

public class BroadcastRelayTelemetryInput implements TelemetryInput {

    public static final ConnectionType CONNECTION = ConnectionType.BROADCASTRELAY;
    
    private MulticastSocket ms;

    private InetAddress address;
    
    private int lossCount = 0;
    private int lastId;
    private int offset = 0;

    public BroadcastRelayTelemetryInput(InetAddress address, int port) throws IOException {

        this.address = address;
        
        this.ms = new MulticastSocket(port);
        this.ms.joinGroup(address);
        this.ms.setSoTimeout(5000); // Set a 5 second time out.
        
        if(this.ms == null)
            throw new IOException("Failed to start the UDP message listener thread.");
    }

    @Override
    public ConnectionType getConnectionType() {
    
        return BroadcastRelayTelemetryInput.CONNECTION;
    }

    public int getLossRate() {
    
        return (this.lossCount * 100) / (this.lastId - this.offset);
    }

    @Override
    public boolean isConnected() {

        return this.ms != null;
    }

    public TelemetryMessage getMessage() throws IOException, ControlFlag{

        while(true){
            
            byte[] buffer = new byte[1000];

            TelemetryMessage tm = null;

            DatagramPacket dg = new DatagramPacket(buffer, buffer.length);
            this.ms.receive(dg);

            tm = TelemetryMessageFactory.parseTelemetryMessage(dg.getData());

            if(tm instanceof VehicleMessage){

                int sequenceNumber = ((VehicleMessage) tm).getSequenceNumber();

                if(this.lastId != -1){

                    int diff = sequenceNumber - this.lastId;

                    if(diff > 1 )
                        this.lossCount += diff - 1;
                    else if(diff < 0)
                        this.lossCount--;
                } else
                    offset = sequenceNumber;

                this.lastId = sequenceNumber;
            } else {

                if(tm instanceof ShutDownControlFlag)
                    this.close();
                else if(tm instanceof HeartBeatControlFlag)
                    continue;

                throw (ControlFlag) tm;
            }

            // Add the next incoming message to the queue.
            return tm;
        }
    }

    @Override
    public void close(){
        
        try {
            
            this.ms.leaveGroup(address);
            this.ms.close();
            
        } catch (IOException e) {} // If this fails, there is nothing we can do anyway.
    }

    @Override
    public String toString(){
    
        StringBuilder sb = new StringBuilder();
    
        return sb.append("[ TINPUT ").append("UDP @ ")
                .append(ms.getInetAddress()).append(":")
                .append(ms.getPort()).toString();
    }
}