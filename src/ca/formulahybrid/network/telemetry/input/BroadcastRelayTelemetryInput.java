package ca.formulahybrid.network.telemetry.input;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;

public class BroadcastRelayTelemetryInput implements TelemetryInput {

    private Logger logger = Logger.getLogger(BroadcastRelayTelemetryInput.class);

    private DatagramSocket ds;

    private int lossCount = 0;
    private int lastId;
    private int offset = 0;

    public BroadcastRelayTelemetryInput(InetAddress address, int port) throws IOException {

        if(this.ds != null)
            return;

        this.ds = new DatagramSocket(port);
        this.ds.connect(address, port);

        if(this.ds == null)
            throw new IOException("Failed to start the UDP message listener thread.");
    }

    @Override
    public void close(){}

    @Override
    public boolean connected() {

        return this.ds != null;
    }

    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();

        return sb.append("[ ").append("UDP @ ")
                .append(ds.getInetAddress()).append(":")
                .append(ds.getPort()).toString();
    }

    public TelemetryMessage getMessage() throws IOException{

        byte[] buffer = new byte[1000];

        TelemetryMessage tm = null;
        
        try {

            DatagramPacket dg = new DatagramPacket(buffer, 1000);
            this.ds.receive(dg);

            tm = TelemetryMessageFactory.parseMessage(dg.getData());

            int sequenceNumber = tm.getSequenceNumber();

            if(this.lastId != -1){

                int diff = sequenceNumber - this.lastId;

                if(diff > 1 )
                    this.lossCount += diff;
                else if(diff < 0)
                    this.lossCount--;
            } else
                offset = sequenceNumber;

            this.lastId = sequenceNumber;

        } catch (DataException e) {

            logger.debug("Dropping message : " + e.getMessage());
        }
        
        // Add the next incoming message to the queue.
        return tm;
    }

    public int getLossRate() {

        return (this.lossCount * 100) / (this.lastId - this.offset);
    }

    @Override
    public TelemetryMessage getMessage(long timeout) {
        // TODO Auto-generated method stub
        return null;
    }
}