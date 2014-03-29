package ca.formulahybrid.telemetry.connector;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.google.common.base.Charsets;

import ca.formulahybrid.network.telemetry.input.ReliableRelayTelemetryInput;
import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.exception.TelemetryCloseException;
import ca.formulahybrid.telemetry.exception.TelemetryStopException;
import ca.formulahybrid.telemetry.exception.TelemetryThrownException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;
import ca.formulahybrid.telemetry.message.pseudo.PseudoMessage;
import ca.formulahybrid.telemetry.message.pseudo.TelemetryHeartBeatMessage;
import ca.formulahybrid.telemetry.message.pseudo.TelemetryStopMessage;
import ca.formulahybrid.telemetry.message.pseudo.TelemetryCloseMessage;
import ca.formulahybrid.telemetry.message.pseudo.TelemetryThrownMessage;

public class TelemetrySourceClientConnection {

    // Identifier as "CAN Telemetry Source Device".
    public static final byte[] protocolIdentifier = "CTSD".getBytes(Charsets.UTF_8);
    
    private static enum State { STANDBY, RECEIVING };
    private static enum Command { GETOTHERS, STARTFEED, STOPFEED, DISCONNECT,
                                  GETLOGS, RETRIEVELOG, RETRIEVELOGFEED
                                }
    
    private static enum Response { NOSUCHLOG, INCOMINGLOG }
    
    private State state = State.STANDBY;
    private Socket s;
    private DataInputStream dis;
    private DataOutputStream dos;
    
    private String sourceName;
    private Logger logger = Logger.getLogger(TelemetrySourceClientConnection.class);
    
    private Timer heartBeat;
    
    private int lastTimeout;
    
    public TelemetrySourceClientConnection(InetAddress address, int port, boolean throwable) throws IOException, TelemetryException {
        
        // Connect to the telemetry source.
        this.s = new Socket();
        this.s.connect(new InetSocketAddress(address, port), 5000); // Start a new socket with a 5 second time.
        
        this.dis = new DataInputStream(s.getInputStream());
        this.dos = new DataOutputStream(s.getOutputStream());
        
        DataInputStream dis = new DataInputStream(s.getInputStream());
        
        byte[] header = new byte[4];
        
        dis.read(header);
        
        // Check to ensure we have connected to a telemetry source, throw an exception otherwise.
        if(header.equals(TelemetrySourceClientConnection.protocolIdentifier))
            throw new TelemetryException(
                    String.format("The source at %s:%i does not identify as a telemetry source.",
                            address.toString(), port));
        
        // Inform the server of whether this node's connection is throwable or not.
        this.dos.writeBoolean(throwable);
        
        // Read the name as bytes and convert it from UTF-8.
        byte[] name = new byte[dis.readByte()];
        dis.read(name);
        
        this.sourceName = new String(name, Charsets.UTF_8);
        
        // Set the timeout to 9 seconds to detect if we have been dropped..
        this.s.setSoTimeout(9000);
    }
    
    private void checkNotReceiving() throws TelemetryException{
        
        if(this.state == State.RECEIVING)
            throw new TelemetryException("Must not be receiving telemetry data to run this command.");
    }
    
    private void attemptCommand(Command command, boolean expectMirror) throws IOException, TelemetryException{

        this.dos.writeByte(command.ordinal());
        
        if(expectMirror)
            if(this.dis.readByte() != command.ordinal()){
                
                this.disconnect();
                
                throw new TelemetryException("Unexpected response from server when executing "
                        + command.name() + sourceName + ".");
            }
    }
    
    public Set<UUID> getOtherConnections() throws IOException, TelemetryException {

        checkNotReceiving();
        attemptCommand(Command.GETOTHERS, true);
            
        Set<UUID> otherConnections = new HashSet<UUID>();
        
        int listenerNum = this.dis.readByte();
        
        for(int i = 0; i < listenerNum; i++)
            otherConnections.add(new UUID(this.dis.readLong(), this.dis.readLong()));
        
        return otherConnections;
    }
    
    public Set<String> getLogs() throws IOException, TelemetryException {
        
        checkNotReceiving();
        attemptCommand(Command.GETLOGS, true);
        
        Set<String> logs = new HashSet<String>();
        
        int logNum = this.dis.readByte();
        
        for(int i = 0; i < logNum; i++){
        
            byte length = this.dis.readByte();
            byte[] buffer = new byte[length];
            
            this.dis.read(buffer);
            logs.add(new String(buffer, Charsets.UTF_8));
        }
        
        return logs;
    }
    
    public byte[] retrieveLog(String logName) throws IOException, TelemetryException {
        
        checkNotReceiving();
        attemptCommand(Command.RETRIEVELOG, true);
        
        byte[] log = logName.getBytes(Charsets.UTF_8);
        
        this.dos.write(log.length);
        this.dos.write(log);
        
        int resp = this.dis.read();
        
        if(resp == Response.NOSUCHLOG.ordinal())
            throw new FileNotFoundException("The remote log with the name " + logName + " does not exist.");
        else if(resp != Response.INCOMINGLOG.ordinal()){
            
            this.disconnect();
            throw new TelemetryException("Unexpected response from server. Disconnected.");
        }
        
        int logSize = this.dis.readInt();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        byte[] buffer = new byte[1024]; // Data buffer.
        int totalBytesRead = 0;         // Total bytes read so far.
        
        while(totalBytesRead < logSize){
            
            int bytesIncoming = this.dis.read(buffer);
            baos.write(buffer, 0, bytesIncoming);
            
            totalBytesRead += bytesIncoming;
        }
        
        return baos.toByteArray();
    }
    
    public void startRemoteLogFeed(String logName) throws IOException, TelemetryException{

        checkNotReceiving();
        
        attemptCommand(Command.RETRIEVELOGFEED, true);
        
        byte[] log = logName.getBytes(Charsets.UTF_8);
        
        this.dos.write(log.length);
        this.dos.write(log);
        
        int resp = this.dis.read();
        
        if(resp == Response.NOSUCHLOG.ordinal())
            throw new FileNotFoundException("The remote log with the name " + logName + " does not exist.");
        else if(resp != Response.INCOMINGLOG.ordinal()){
            
            this.disconnect();
            throw new TelemetryException("Unexpected response from server. Disconnected.");
        }
        
        this.state = State.RECEIVING;
    }
    
    public String getSourceName() throws TelemetryException {
        
        return this.sourceName;
    }
    
    public void startFeed() throws TelemetryException, IOException {
        
        checkNotReceiving();
        
        attemptCommand(Command.STARTFEED, true);
        this.state = State.RECEIVING;
        
        this.startHeartBeat();
    }
    
    private void startHeartBeat() throws SocketException{
        
        // Begin sending heart beats at an interval of 3 seconds until the feed is stopped.
        this.heartBeat.schedule(new TimerTask(){

            @Override
            public void run() {
                
                try {
                   
                    if(TelemetrySourceClientConnection.this.state == State.STANDBY){
                        
                        this.cancel();
                        return;
                    }
                    
                    Socket s = TelemetrySourceClientConnection.this.s;
                    
                    if(s == null)
                        throw new IOException();
                    else
                       s.getOutputStream().write(ReliableRelayTelemetryInput.heartBeatFlag);
                   
                } catch (IOException e) {
                    
                    logger.debug("Telemetry receiver heartbeat generation failed. Thread terminated.");
                    TelemetrySourceClientConnection.this.heartBeat.cancel();
                }
                
            }}, 0, 3000);
        
        // Set a timeout of three seconds to detect if we been disconnected while receiving.
        this.lastTimeout = this.s.getSoTimeout();
        this.s.setSoTimeout(3000);
    }
    
    public void stopFeed() throws TelemetryException, IOException {
        
        if(this.state == State.STANDBY)
            return;
        
        attemptCommand(Command.STOPFEED, false);
    }
    
    public TelemetryMessage getMessage() throws TelemetryException, IOException {
        
        TelemetryMessage tm = null;
        
        while(true){
            
            if(this.state == State.RECEIVING)
                throw new TelemetryException("Feed needs to be started before messages can be read.");
            
            try {
                
                tm = TelemetryMessageFactory.buildMessage(this.dis);
                
            } catch (DataException e) {
                
                logger.info("Dropping message from source: " + e.getMessage());
            } 
            
            // Check if this message is one of those marked as a pseudo message.
            if(tm.getClass().getAnnotation(PseudoMessage.class) != null){
    
                if(tm instanceof TelemetryCloseMessage){
                
                    try {
                        this.s.close();
                    } catch(IOException ioe){}
                    
                    throw new TelemetryCloseException("Connection terminated by the source because it is shutting down.");
                }
                
                else if(tm instanceof TelemetryStopMessage){
                    
                    this.state = State.STANDBY;
                    this.s.setSoTimeout(this.lastTimeout);
                    
                    throw new TelemetryStopException("Telemetry from source successfully terminated.");
                }
                    
                else if(tm instanceof TelemetryThrownMessage){
                    
                    UUID terminator = ((TelemetryThrownMessage) tm).getTerminator();
                    
                    throw new TelemetryThrownException(
                            String.format("Node disconnected at the request of a node with the ID %s.",
                                    terminator.toString() ));
                }
                // In case telemetry slows down, we know when to stop listening.
                else if(tm instanceof TelemetryHeartBeatMessage)
                    continue;
                
                break;
            }
        }
        
        return tm;
    }
    
    public boolean connected(){
    
        return this.s != null;
    }
    
    public void disconnect() {
        
        if(this.s == null)
            return;
        
        try {
            
            // Try to tell the server to stop the connection.
            attemptCommand(Command.DISCONNECT, false);
            this.s.close();
            
        } catch (IOException | TelemetryException e) {} // We do not care about exceptions. We are just
                                                        // doing this as a favour to the server.
        
        this.heartBeat.cancel(); // Shutdown the heart beat generator.
        this.s = null;
        this.sourceName = null;
    }
}
