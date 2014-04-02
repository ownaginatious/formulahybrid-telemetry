package ca.formulahybrid.network.telemetry.output;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.google.common.base.Charsets;

import ca.formulahybrid.telemetry.connector.TelemetrySource;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;
import ca.formulahybrid.telemetry.message.control.ShutDownControlFlag;
import ca.formulahybrid.telemetry.message.control.ConnectionDropControlFlag;
import ca.formulahybrid.telemetry.message.control.HeartBeatControlFlag;

public class ReliableRelayTelemetryOutput implements TelemetryOutput {

    // Identifier as "CAN Telemetry Network Relay".
    public static final byte[] protocolIdentifier = "CTNR".getBytes(Charsets.UTF_8);

    private static Logger logger = Logger.getLogger(ReliableRelayTelemetryOutput.class);

    private Set<Socket> connections = new HashSet<Socket>();

    private int relayPort;
    private ServerSocket ss;
    
    private Timer sweepTimer = new Timer();
    private Timer heartBeat = new Timer();
    
    private TelemetrySource sourceBeingRelayed;
    
    public ReliableRelayTelemetryOutput(InetAddress address, TelemetrySource sourceBeingRelayed) throws IOException {
        
        this.sourceBeingRelayed = sourceBeingRelayed;
        
        // Create the TCP server.
        this.ss = new ServerSocket(0); // Setting a port of zero means select any available port.
        this.relayPort = ss.getLocalPort();

        // Start the TCP server thread for accepting new listeners.
        new Thread(new Runnable(){

            @Override
            public void run() {

                serverRoutine();
                logger.debug("Server socket terminated.");
            }

        }).start();
        
        // Initialize the TCP connection sweeper to run at an interval of 5 seconds.
        this.sweepTimer.schedule(new TimerTask(){

            @Override
            public void run() {
                reliableConnectionSweepRoutine();
            }}, 0, 5000);
        
        // Sending heart beats at an interval of 3 seconds to all clients until the connection dies.
        // This is to maintain the connection at times of general radio silence.
        this.heartBeat.schedule(new TimerTask(){

            @Override
            public void run() {
                ReliableRelayTelemetryOutput.this.sendMessage(new HeartBeatControlFlag());
            }}, 0, 3000);
    }

    /**
     * Returns the randomly selected port that the underlying socket server is listening on.
     * 
     * @return The port number of the listening socket server.
     */
    public int getRelayPort(){
    
        return this.relayPort;
    }

    /**
     * This routine checks for the keep-alive heart beat signal on each connected socket,
     * flushing all incoming data from the socket's buffer and checking for a heart beat.
     * If no heart beat is detected, the connection is dropped.
     */
    private void reliableConnectionSweepRoutine(){
    
        logger.debug("Beginning connection sweep...");
    
        List<Socket> deadDestinations = new ArrayList<Socket>();
    
        // Collect the dead destinations and increase timeouts.
        for(Socket s : this.connections){
    
            boolean heartBeatDetected = false;
            
            try {
                
                // Keep reading in data from the buffer until it is clear for heart beat messages. If anything in
                // there is not a heartbeat, kill the connection.
                while(true)
                    if(!(TelemetryMessageFactory.buildTelemetryMessage(s.getInputStream()) instanceof HeartBeatControlFlag))
                        throw new IOException();
                    else
                        heartBeatDetected = true;
    
            } catch (SocketTimeoutException ste){
                
                // If no heart beat was detected after clearing the buffer, the socket should be dropped.
                if(!heartBeatDetected)
                    deadDestinations.add(s);
                
            } catch (IOException e) {
    
                // Connections throwing other IO exceptions are also considered dead.
                deadDestinations.add(s);
            }
        }
    
        // Remove the dead connections.
        synchronized(this.connections){
    
            for(Socket s : deadDestinations){
    
                this.connections.remove(s);
                
                // Attempt to close the connection properly if it is still open.
                try {
                    
                    // If the other end is somehow still listening, send a drop message to
                    // let it know to give up.
                    s.getOutputStream().write(new ConnectionDropControlFlag().getBytes());
                    s.close();
                    
                } catch (IOException e) {}
    
                logger.debug("Reliable connection to " + s.getInetAddress().toString()
                        + " has timed out and was dropped.");
            }
        }
    
        logger.debug("Sweep completed.");
    }

    private void serverRoutine(){
    
        try {
            
            while(this.ss != null){
                
                Socket s = this.ss.accept();
                
                logger.debug("Accepting new client [" + s.getInetAddress() + "] on to reliable connection.");
                
                try {
    
                    DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                    
                    // State information to the new socket.
                    dos.write(ReliableRelayTelemetryOutput.protocolIdentifier);
                    
                    byte[] sourceName = this.sourceBeingRelayed.getName().getBytes(Charsets.UTF_8);
                    
                    dos.write(sourceName.length);
                    dos.write(sourceName); // Connected network.
                    
                    // We only want checking for incoming heart beats to be as non-blocking as possible.
                    s.setSoTimeout(1);
                    
                    // Add the socket to the list of connections between a sweep or message push.
                   synchronized(this.connections){
                       this.connections.add(s);
                   }
                    
                } catch (IOException e){
    
                    logger.debug("Problem accepting the new client. Dropping.");
                }
            }
            
        } catch (IOException e) {
            
            logger.debug("Reliable connection server failure. Shutting down.");
        }
    }

    public TelemetrySource getRelayedSource(){
    
        return this.sourceBeingRelayed;
    }
    
    @Override
    public void sendBinary(byte[] message) {
        
        // Wait until the sweep has completed before attempting to 
        // send the incoming message to listening clients.
        synchronized(this.connections){

            List<Socket> droppedSockets = new ArrayList<Socket>();
            
            for(Socket s : this.connections){

                try {

                    s.getOutputStream().write(message);

                } catch (IOException e) {
                    
                    logger.debug("Reliable connection to " + s.getInetAddress().toString()
                        + " has broken and will be dropped.");
                    
                    droppedSockets.add(s);
                }
            }
            
            for(Socket s : droppedSockets)
                this.connections.remove(s);
        }
    }

    @Override
    public void sendMessage(TelemetryMessage message) {

        sendBinary(message.getBytes());
    }

    @Override
    public boolean isConnected() {
        
        return this.ss != null;
    }

    @Override
    public void close() {

        try {
            
            // Attempt to close the server connection, if possible.
            if(this.ss != null)
                this.ss.close();
            
            this.ss = null;
            
        } catch (IOException e) {}
        finally {

            try{
                
                // Attempt to close each individual connection.
                for(Socket s : this.connections){
                    
                    // Tell the other end the connection is closing.
                    s.getOutputStream().write(new ShutDownControlFlag().getBytes());
                    s.close();
                }
                
            } catch(IOException e){}
            
            // Stop the sweeper.
            this.sweepTimer.cancel();
        }
    }
    
    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();

        return sb.append("[ TOUTPUT ").append("TCP @ ")
                .append(this.ss.getInetAddress()).append(":")
                .append(this.relayPort).toString();
    }
}
