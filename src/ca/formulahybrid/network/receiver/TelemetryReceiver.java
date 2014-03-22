package ca.formulahybrid.network.receiver;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import ca.formulahybrid.network.relayer.TelemetryRelay;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

public class TelemetryReceiver{
    
    private Logger logger = Logger.getLogger(TelemetryReceiver.class);

    private BlockingQueue<TelemetryMessage> bq = new LinkedBlockingQueue<TelemetryMessage>();
    private TelemetryReceiverConnection trc = null;
    
    public void connectTCP(InetAddress address, int port) throws IOException{
        
        switchConnections(new TCPTelemetryReceiverConnection(), address, port);
        logger.debug("Successfully switched to the relay's TCP connection.");
    }
    
    public void connectUDP(InetAddress address, int port) throws IOException{
        
        switchConnections(new UDPTelemetryReceiverConnection(), address, port);
        logger.debug("Successfully switched to the relay's UDP connection.");
    }
    
    public void connectLocal(TelemetryRelay tr){
        
    }
    
    private void switchConnections(TelemetryReceiverConnection newConnection, InetAddress address, int port) throws IOException{
        
        newConnection.connect(address, port, bq);
        TelemetryReceiverConnection oldConnection = trc;
        
        // To ensure the null message is hit only after the new connection is fully setup in the 
        // getMessage() method.
        this.trc = newConnection;
        
        if(oldConnection != null)
            oldConnection.disconnect();
        
    }
    
    public void disconnect(){
        
        if(trc != null)
            trc.disconnect();
    }
    
    public TelemetryMessage getMessage() throws IOException {
        
        if(trc == null)
            throw new IOException("No connections to receive from have been initialized.");
        
        TelemetryMessage cm = null;
        
        try {
            
            cm = bq.take();
            
            // If there is a null, it indicates we have been potentially disconnected.
            // Only throw an exception if it is true.
            if(cm == null & !trc.connected())
                throw new IOException("Underlying socket disconnected.");
            
        } catch (InterruptedException e) {} // This cannot help in our situation.
        
        return cm;
    }
    
    public int getLossRate(){
        
        return this.trc.getLossRate();
    }

}