package ca.formulahybrid.network.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;

public class UDPTelemetryReceiverConnection implements TelemetryReceiverConnection {

    private Logger logger = Logger.getLogger(UDPTelemetryReceiverConnection.class);
    
    private BlockingQueue<TelemetryMessage> bq;
    private DatagramSocket s;

    private int lossCount = 0;
    private int lastId;
    private int offset = 0;
    
    @Override
    public void connect(InetAddress address, int port, BlockingQueue<TelemetryMessage> bq) throws IOException {
        
        if(this.s != null)
            return;
        
        this.bq = bq;
        this.s = new DatagramSocket(port);
        this.s.connect(address, port);
        
        new Thread(new Runnable(){

            @Override
            public void run() {
                
                receiveRoutine();
            }});
        
        // Wait 3 seconds to make sure the thread is running okay.
        try {
            
            Thread.sleep(3);
            
        } catch (InterruptedException e) {}
        
        if(this.s == null)
            throw new IOException("Failed to start the UDP message listener thread.");
    }

    @Override
    public void disconnect(){}

    @Override
    public boolean connected() {
        
        return this.s != null;
    }
    
    @Override
    public String toString(){
        
        StringBuilder sb = new StringBuilder();
        
        return sb.append("[ ").append("UDP @ ")
                .append(s.getInetAddress()).append(":")
                .append(s.getPort()).toString();
    }
    
    private void receiveRoutine(){
        
        byte[] buffer = new byte[1000];
        
        while(this.s != null){
            
            try {
                
                DatagramPacket dg = new DatagramPacket(buffer, 1000);
                this.s.receive(dg);
                
                TelemetryMessage tm = TelemetryMessageFactory.parseMessage(dg.getData());
                
                int messageId = tm.getMessageId();
                
                if(this.lastId != -1){
                    
                    int diff = messageId - this.lastId;
                    
                    if(diff > 1 )
                        this.lossCount += diff;
                    else if(diff < 0)
                        this.lossCount--;
                } else
                    offset = messageId;
                
                this.lastId = messageId;
                
                // Add the next incoming message to the queue.
                bq.put(tm);
                
            } catch (InterruptedException e) {} // This exception cannot happen in this context.
            catch (IOException e) {
                
                // The socket probably broke. Insert a null to indicate that the connection is
                // terminating.
                try {
                    bq.put(null);
                } catch (InterruptedException ie) {}
                
                this.s = null;
                
                logger.debug("UDP connection failed.");
                
            } catch (DataException e) {
                
                logger.debug("Dropping message : " + e.getMessage());
            }
        }
    }

    @Override
    public int getLossRate() {
        
        return (this.lossCount * 100) / (this.lastId - this.offset);
    }
}