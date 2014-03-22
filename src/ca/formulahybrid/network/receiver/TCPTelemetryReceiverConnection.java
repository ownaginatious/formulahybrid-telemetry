package ca.formulahybrid.network.receiver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;

public class TCPTelemetryReceiverConnection implements TelemetryReceiverConnection {

    private Logger logger = Logger.getLogger(TCPTelemetryReceiverConnection.class);
    
    private BlockingQueue<TelemetryMessage> bq;
    private Socket s;

    @Override
    public void connect(InetAddress address, int port, BlockingQueue<TelemetryMessage> bq) throws IOException {
        
        if(this.s != null)
            return;
        
        this.bq = bq;
        this.s = new Socket(address, port);
        
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
            throw new IOException("Failed to start the TCP message listener thread.");
    }

    @Override
    public void disconnect() {
        
        try {
            
            if(this.s != null)
                this.s.close();
            
        } catch(IOException ioe){}
        finally {
            
            this.s = null;
        }
    }

    @Override
    public boolean connected() {
        
        return this.s != null;
    }
    
    @Override
    public String toString(){
        
        StringBuilder sb = new StringBuilder();
        
        return sb.append("[ ").append("TCP @ ")
                .append(s.getInetAddress()).append(":")
                .append(s.getPort()).toString();
    }
    
    private void receiveRoutine(){
        
        while(this.s != null){
            
            try {
                
                // Add the next incoming message to the queue.
                bq.put(TelemetryMessageFactory.buildMessage(this.s.getInputStream()));
                
            } catch (InterruptedException e) {} // This exception cannot happen in this context.
            catch (IOException e) {
                
                // The socket probably broke. Insert a null to indicate that the connection is
                // terminating.
                try {
                    bq.put(null);
                } catch (InterruptedException ie) {} // This exception cannot happen in this context.
                
                this.s = null;
                
                logger.debug("TCP connection failed.");
                
            } catch (DataException e) {
                
                logger.debug("Dropping message : " + e.getMessage());
            }
        }
    }

    /**
     * Returns 0 since TCP sockets are lossless.
     */
    @Override
    public int getLossRate() {
        
        return 0;
    }
}