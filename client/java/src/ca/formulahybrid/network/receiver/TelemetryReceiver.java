package ca.formulahybrid.network.receiver;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import ca.formulahybrid.network.telemetry.input.BroadcastRelayTelemetryInput;
import ca.formulahybrid.network.telemetry.input.LocalFileTelemetryInput;
import ca.formulahybrid.network.telemetry.input.ReliableRelayTelemetryInput;
import ca.formulahybrid.network.telemetry.input.SourceTelemetryInput;
import ca.formulahybrid.network.telemetry.input.TelemetryInput;
import ca.formulahybrid.network.telemetry.output.TelemetryOutput;
import ca.formulahybrid.telemetry.connector.TelemetrySource;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.control.ConnectionThrownControlFlag;
import ca.formulahybrid.telemetry.message.control.HeartBeatControlFlag;
import ca.formulahybrid.telemetry.message.control.ProtocolFailureControlFlag;
import ca.formulahybrid.telemetry.message.control.ShutDownControlFlag;
import ca.formulahybrid.telemetry.message.control.ConnectionDropControlFlag;
import ca.formulahybrid.telemetry.message.control.ControlFlag;
import ca.formulahybrid.telemetry.message.control.StreamStopControlFlag;

public class TelemetryReceiver {
    
    public static enum ConnectionType { LOCAL, RELIABLERELAY, BROADCASTRELAY, SOURCE }
    
    private Logger logger = Logger.getLogger(TelemetryReceiver.class);

    private BlockingQueue<TelemetryMessage> localQueue = new LinkedBlockingQueue<TelemetryMessage>();
    private BlockingQueue<TelemetryMessage> relayQueue = new LinkedBlockingQueue<TelemetryMessage>();
    
    private TelemetryInput ti = null;
    
    private ControlFlag failureReason;
    
    private int ignoreFlags = 0;
    
    private Set<TelemetryOutput> outputChannels = new HashSet<TelemetryOutput>();
    
    public TelemetryReceiver(){
        
        // Start the output routine.
        new Thread(new Runnable(){

            @Override
            public void run() {
                messageForwardingRoutine();
            }}).start();
    }
    
    public void addRelayDestination(TelemetryOutput trd){
        
        synchronized(outputChannels){
            outputChannels.add(trd);
        }
    }
    
    public ConnectionType getConnectionType(){
    
        return this.ti.getConnectionType();
    }
    
    public void connectToReliableRelay(InetAddress address, int port, TelemetrySource ts) throws IOException, TelemetryException {
        
        switchConnections(new ReliableRelayTelemetryInput(address, port, ts));
        logger.debug("Successfully switched to the relay TCP connection at " + address + ":" + port + ".");
    }
    
    public void connectToBroadcastRelay(InetAddress address, int port) throws IOException{
        
        switchConnections(new BroadcastRelayTelemetryInput(address, port));
        logger.debug("Successfully switched to the relay UDP connection at " + address + ":" + port + ".");
    }
    
    public void connectToSource(SourceTelemetryInput sti) throws IOException, TelemetryException{
        
        if(!sti.isReceiving())
            throw new TelemetryException("Cannot switch to source as it is not currently receiving a telemetry feed.");
                    
        switchConnections(sti);
        logger.debug("Successfully switched input to telemetry source.");
    }
    
    public void connectLocalFile(String f, boolean simulated) throws IOException, TelemetryException{
        
        switchConnections(new LocalFileTelemetryInput(new File(f), simulated));
        logger.debug("Successfully loaded file record as the telemetry source.");
    }
    
    public void connectLocalFile(File f, boolean simulated) throws IOException, TelemetryException{
        
        switchConnections(new LocalFileTelemetryInput(f, simulated));
        logger.debug("Successfully loaded file record as the telemetry source.");
    }
    
    private void switchConnections(TelemetryInput newConnection) throws IOException{
        
        TelemetryInput oldConnection = this.ti;
        
        synchronized(this.ti){
            
            this.ignoreFlags = this.localQueue.size(); // Ignore flags for the remaining data in the queue.
            
            this.ti = newConnection;
            
            // Reset status variables.
            this.failureReason = null;
            
            if(oldConnection != null)
                oldConnection.close();
            
            // Start a thread to receive data, if not already started.
            new Thread(new Runnable(){

                @Override
                public void run() {
                    messageListeningRoutine();
                }}).start();;
        }
    }
    
    public void shutdown(){
        
        // Tell outputs that this is shutting down.
        this.relayQueue.add(new ShutDownControlFlag());
        this.relayQueue.add(null); // Kill the loop.
        
        if(ti != null)
            ti.close();
    }
    
    public boolean wasThrownOff(){
        
        return this.failureReason != null
                & this.failureReason instanceof ConnectionThrownControlFlag; 
    }
    
    public boolean inputHasShutdown(){
         
        return this.failureReason != null
                & this.failureReason instanceof ShutDownControlFlag;
    }
    
    public boolean timedOutAtSource(){
        
        return this.failureReason != null
                & this.failureReason instanceof ConnectionDropControlFlag;
    }
    
    public boolean protocolFailedAtSource(){
        
        return this.failureReason != null
                & this.failureReason instanceof ProtocolFailureControlFlag;
    }
    
    public boolean sourceTimedOut(){
        
        return this.failureReason == null;
    }
    
    public boolean inputEnded(){
        
        return this.failureReason == null
                | this.failureReason instanceof StreamStopControlFlag;
    }
    
    public boolean connected(){
        
        return this.ti != null && this.ti.isConnected();
    }

    private void messageListeningRoutine(){

        while(true){

            TelemetryMessage relayMessage = null;
            
            try {
                
                try {

                    // Attempt to add the message to the queue.
                    synchronized(this.ti){
                        
                        TelemetryMessage tm = this.ti.getMessage();
                        this.localQueue.add(tm);
                        relayMessage = tm;
                    }

                } catch(ControlFlag cf){ // Only throw the flag exception if we are not ignoring flags.

                    if(this.ignoreFlags > 0){
                        
                        this.ignoreFlags--;
                        logger.debug("Old control flag "
                                + cf.getClass().getName() + " ignored. [" + this.ignoreFlags + "]");
                    }
                    else
                        throw cf;
                }
                
            } catch(HeartBeatControlFlag hbcf){ // This just the input keeping the connection alive.
                continue;
                
            } catch(ControlFlag cf){
                
                this.failureReason = cf;
                this.relayQueue.add(null);
                break;
                
            } catch (IOException ioe) {
                
                this.failureReason = null; // Null signifies that failure was likely because of a timeout.
                
                // Terminate the loop.
                break;
            }

            if(relayMessage != null)
                relayQueue.add(relayMessage);
        }
    }

    private void messageForwardingRoutine(){

        while(true){
            
            try {
                
                TelemetryMessage tm = this.relayQueue.take();
                
                if(tm == null)
                    break;
                
                List<TelemetryOutput> brokenChannels = new ArrayList<TelemetryOutput>();
                
                synchronized(outputChannels){
                    
                    byte[] messageBytes = tm.getBytes();
                    
                    for(TelemetryOutput to : this.outputChannels){
                        
                        try {
                            to.sendBinary(messageBytes);
                        } catch (IOException e) {
                            brokenChannels.add(to);
                        }
                    }
                    
                    // Remove the broken connections.
                    for(TelemetryOutput to : this.outputChannels){
                        
                        to.close();
                        this.outputChannels.remove(to);
                        
                        logger.debug("Telemetry output " + to
                                + " broke during transmission and has been removed.");
                    }
                }
                
            } catch (InterruptedException e) {} // We do not care about this.
        }
        
        // Close all the output channels.
        for(TelemetryOutput to : this.outputChannels)
            to.close();
        
        logger.debug("Message forwarding routine has been shut down.");
    }

    public TelemetryMessage getMessage() throws IOException {
        
        if(ti == null)
            throw new IOException("No connections to receive from have been initialized.");
        
        TelemetryMessage tm = null;
        
        try {
            
            tm = localQueue.take();
            
            // If there is a null, it indicates we have been potentially disconnected.
            // Only throw an exception if it is true.
            if(tm == null & !ti.isConnected())
                throw new IOException("Underlying socket disconnected.");
            
        } catch (InterruptedException e) {} // This cannot help in our situation.
        
        return tm;
    }
}