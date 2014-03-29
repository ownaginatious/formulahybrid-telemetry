package ca.formulahybrid.network.telemetry.output;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.google.common.base.Charsets;

import ca.formulahybrid.network.telemetry.input.ReliableRelayTelemetryInput;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.pseudo.TelemetryCloseMessage;

public class ReliableRelayTelemetryOutput implements TelemetryOutput {

    // Identifier as "CAN Telemetry Network Relay".
    public static final byte[] protocolIdentifier = "CTNR".getBytes(Charsets.UTF_8);

    private static Logger logger = Logger.getLogger(ReliableRelayTelemetryOutput.class);
    
    private DataInputStream dis;
    

    private boolean running;

    private Map<Socket, Integer> connectionsWithTimeOut = new HashMap<Socket, Integer>();
    private List<Socket> destinationsToAdd = new ArrayList<Socket>();

    private int relayPort;
    private ServerSocket ss;
    
    private Timer sweepTimer = new Timer();
    
    private String sourceBeingRelayed;
    
    public ReliableRelayTelemetryOutput(InetAddress address, int port, String sourceName) throws IOException {
        
        this.sourceBeingRelayed = sourceName;
        
        // Create the TCP server.
        this.ss = new ServerSocket(0);
        this.relayPort = ss.getLocalPort();

        // Start the TCP server.
        new Thread(new Runnable(){

            @Override
            public void run() {

                serverRoutine();
                logger.debug("Server socket terminated.");
            }

        }).start();
        
        // Initialize the TCP connection sweeper to run at an interval of 3 seconds.
        this.sweepTimer.schedule(new TimerTask(){

            @Override
            public void run() {
                reliableConnectionSweepRoutine();
            }}, 0, 3000);
    }

    @Override
    public void sendBinary(byte[] message) throws IOException {
        
        synchronized(this.connectionsWithTimeOut){

            List<Socket> droppedSockets = new ArrayList<Socket>();
            
            for(Socket s : this.connectionsWithTimeOut.keySet()){

                try {

                    s.getOutputStream().write(message);

                } catch (IOException e) {
                    
                    logger.debug("Reliable connection to " + s.getInetAddress().toString()
                        + " has broken and will be dropped.");
                    
                    droppedSockets.add(s);
                }
            }
            
            for(Socket s : droppedSockets)
                this.connectionsWithTimeOut.remove(s);
        }
    }

    @Override
    public void sendMessage(TelemetryMessage message) throws IOException {

        sendBinary(message.getPayloadBytes());
    }

    @Override
    public void close() {

        try {
            
            if(this.ss != null)
                this.ss.close();

            try{
                
                // Attempt to close each connection.
                for(Socket s : this.connectionsWithTimeOut.keySet()){
                    
                    // Tell the other end the connection is closing.
                    s.getOutputStream().write(new TelemetryCloseMessage().getBytes());
                    s.close();
                }
                
            } catch(IOException e){}
        } catch (IOException e) {}
    }

    private boolean detectHeartBeat(Socket s) throws IOException{
        
        int timeout = s.getSoTimeout();
        s.setSoTimeout(1);
        
        boolean detected = (ReliableRelayTelemetryInput.heartBeatFlag == this.dis.readByte());
        
        s.setSoTimeout(timeout);
        
        return detected;
    }
    
    public int getRelayPort(){
    
        return this.relayPort;
    }
    
    private void reliableConnectionSweepRoutine(){

        logger.debug("Beginning connection sweep...");

        List<Socket> deadDestinations = new ArrayList<Socket>();

        // Collect the dead destinations and increase timeouts.
        for(Socket s : this.connectionsWithTimeOut.keySet()){

            int timeout = this.connectionsWithTimeOut.get(s);

            try {

                if(timeout > 3)
                    deadDestinations.add(s);
                else {
                    
                    timeout = (this.detectHeartBeat(s))? 0 : timeout + 1;
                    this.connectionsWithTimeOut.put(s, timeout);
                }

            } catch (IOException e) {

                // Connections throwing IO exceptions are also considered dead.
                deadDestinations.add(s);
            }
        }

        // Remove the dead connections.
        synchronized(this.connectionsWithTimeOut){

            for(Socket s : deadDestinations){

                this.connectionsWithTimeOut.remove(s);
                
                // Attempt to close the connection properly if it is still open.
                try {
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
            
            while(this.running){
                
                Socket s = this.ss.accept();
                
                logger.debug("Accepting new client [" + s.getInetAddress() + "] on to reliable connection.");
                
                try {

                    DataInputStream dis = new DataInputStream(s.getInputStream());
                    DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                    
                    // State information to the new socket.
                    dos.write(ReliableRelayTelemetryOutput.protocolIdentifier);
                    dos.write(this.sourceBeingRelayed.getBytes(Charsets.UTF_8)); // Connected network.
                    
                    // Lets wait 3 seconds for the first heart beat.
                    s.setSoTimeout(3000);
                    
                    // This will either timeout or return the wrong heart beat value, both of
                    // which invalidate and kill the socket.
                    if(ReliableRelayTelemetryInput.heartBeatFlag != dis.readByte())
                        throw new IOException(); // The client on the other end is broken.
                    
                    // If the code has not thrown an exception by here, then the socket is ready to be added.
                    connectionsWithTimeOut.put(s, 0);
                    destinationsToAdd.add(s);
                    
                } catch (IOException e){

                    logger.debug("Problem accepting the new client. Dropping.");
                }
            }
            
        } catch (IOException e) {
            
            logger.debug("Server socket failure. Reliable connection server. Shutting down");
        }
    }
}
