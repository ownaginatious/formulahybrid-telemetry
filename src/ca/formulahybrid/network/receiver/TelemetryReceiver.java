package ca.formulahybrid.network.receiver;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import ca.formulahybrid.network.telemetry.input.BroadcastRelayTelemetryInput;
import ca.formulahybrid.network.telemetry.input.LocalFileTelemetryInput;
import ca.formulahybrid.network.telemetry.input.ReliableRelayTelemetryInput;
import ca.formulahybrid.network.telemetry.input.TelemetryInput;
import ca.formulahybrid.network.telemetry.output.TelemetryOutput;
import ca.formulahybrid.telemetry.exception.TelemetryCloseException;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.exception.TelemetryStopException;
import ca.formulahybrid.telemetry.exception.TelemetryThrownException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

public class TelemetryReceiver {
    
    private Logger logger = Logger.getLogger(TelemetryReceiver.class);

    private BlockingQueue<TelemetryMessage> bq = new LinkedBlockingQueue<TelemetryMessage>();
    private TelemetryInput ti = null;
    
    private boolean thrownOff = false;
    private boolean running = false;
    private boolean sourceDisabled = true;
    
    public void addRelayDestination(TelemetryOutput trd){
        
    }
    
    public void connectToReliableRelay(InetAddress address, int port) throws IOException {
        
        switchConnections(new ReliableRelayTelemetryInput(address, port));
        logger.debug("Successfully switched to the relay TCP connection at " + address + ":" + port + ".");
    }
    
    public void connectToBroadcastRelay(InetAddress address, int port) throws IOException{
        
        switchConnections(new BroadcastRelayTelemetryInput(address, port));
        logger.debug("Successfully switched to the relay UDP connection at " + address + ":" + port + ".");
    }
    
    public void connectToSource(InetAddress address, int port){
        
    }
    
    public void connectToRemoteReplay(InetAddress address, int port, String filaName){
        
    }
    
    public void connectLocalFile(String f, boolean simulated) throws IOException{
        
        switchConnections(new LocalFileTelemetryInput(new File(f), simulated));
        logger.debug("Successfully loaded file record as the telemetry source.");
    }
    
    public void connectLocalFile(File f, boolean simulated) throws IOException{
        
        switchConnections(new LocalFileTelemetryInput(f, simulated));
        logger.debug("Successfully loaded file record as the telemetry source.");
    }
    
    private void switchConnections(TelemetryInput newConnection) throws IOException{
        
        TelemetryInput oldConnection = this.ti;
        
        // To ensure the null message is hit only after the new connection is fully setup in the 
        // getMessage() method.
        
        synchronized(this.ti){
            
            this.ti = newConnection;
            
            // Reset status variables.
            this.thrownOff = false;
            this.running = true;
            
            if(oldConnection != null)
                oldConnection.close();
            
            // Start a thread to receive data, if not already started.
            new Thread(new Runnable(){

                @Override
                public void run() {
                    
                    messageListeningRoutine();
                }});
        }
    }
    
    public void shutdown(){
        
        if(ti != null)
            ti.close();
    }
    
    public boolean wasThrownOff(){
        
        return this.thrownOff;
    }
    
    public boolean sourceHasShutdown(){
        
        return this.sourceDisabled;
    }
    
    public TelemetryMessage getMessage() throws IOException {
        
        if(ti == null)
            throw new IOException("No connections to receive from have been initialized.");
        
        TelemetryMessage cm = null;
        
        try {
            
            cm = bq.take();
            
            // If there is a null, it indicates we have been potentially disconnected.
            // Only throw an exception if it is true.
            if(cm == null & !ti.connected())
                throw new IOException("Underlying socket disconnected.");
            
        } catch (InterruptedException e) {} // This cannot help in our situation.
        
        return cm;
    }
    
    public boolean connected(){
        
        return this.running;
    }
    
    private void messageListeningRoutine(){
        
       while(this.running){
            
            try {
                
                synchronized(this.ti){
                    
                    this.bq.add(this.ti.getMessage());
                }

            } catch (TelemetryCloseException e) {
                
                this.sourceDisabled = true;
                this.running = false;
                
                break;

            } catch (TelemetryStopException e) {
                break;

            } catch (TelemetryThrownException e) {
                this.thrownOff = true;

            } catch (TelemetryException e) {

            } catch (IOException e) { // On socket failure

                this.running = false;
                logger.debug("Underlying input stream failure. Message receiver terminated.");
                break;
            }
        }
    }
}