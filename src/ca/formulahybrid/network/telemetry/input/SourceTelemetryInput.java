package ca.formulahybrid.network.telemetry.input;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import ca.formulahybrid.network.receiver.TelemetryReceiver.ConnectionType;
import ca.formulahybrid.telemetry.connector.TelemetrySource;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;
import ca.formulahybrid.telemetry.message.control.ShutDownControlFlag;
import ca.formulahybrid.telemetry.message.control.ConnectionThrownControlFlag;
import ca.formulahybrid.telemetry.message.control.ControlFlag;
import ca.formulahybrid.telemetry.message.control.HeartBeatControlFlag;
import ca.formulahybrid.telemetry.message.control.StreamStoppedControlFlag;

public class SourceTelemetryInput implements TelemetryInput {

    //#FIXME: Think of actual source port name.
    private static final int TELEMETRY_SOURCE_PORT = 8888;
    
    private static final ConnectionType CONNECTION = ConnectionType.SOURCE;
    
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
    
    private TelemetrySource source;
    
    private Logger logger = Logger.getLogger(SourceTelemetryInput.class);
    
    private Timer heartBeat;
    
    private int lastTimeout;
    
    public SourceTelemetryInput(TelemetrySource ts, boolean throwable) throws IOException, TelemetryException {
        
        this.source = ts;
        
        // Connect to the telemetry source.
        this.s = new Socket();
        this.s.connect(new InetSocketAddress(ts.getSourceAddress(), TELEMETRY_SOURCE_PORT), 5000); // Start a new socket with a 5 second time.
        
        this.dis = new DataInputStream(s.getInputStream());
        this.dos = new DataOutputStream(s.getOutputStream());
        
        // *****************************
        // Start identifying the server.
        // *****************************
        
        byte[] protocolHeader = SourceTelemetryInput.protocolIdentifier;
        
        // Ensure that this is a source.
        byte[] header = new byte[protocolHeader.length];
        
        this.dis.read(header);
        
        if(!header.equals(protocolHeader))
            throw new IOException("Expected protocol header '"
                    + new String(protocolHeader, Charsets.UTF_8)
                    + "'. Received " + new String(header, Charsets.UTF_8) + " instead.");
        
        byte[] nameBuffer = new byte[this.dis.read()];
        dis.read(nameBuffer);
        
        String name = new String(nameBuffer, Charsets.UTF_8);
        
        if(!name.equals(this.source.getName()))
            throw new TelemetryException("The connected node identified as a telemetry source, but is relaying for " 
                    + name + ", not the expected " + this.source.getName());

        // ****************************
        
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
                        + command.name() + ".");
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
    
    public byte[] getRemoteLog(String logName) throws IOException, TelemetryException {
        
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
    
    public void startRemoteLogFeed(String logName) throws IOException, TelemetryException {

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
    
    public TelemetrySource getSource() throws TelemetryException {
        
        return this.source;
    }
    
    public void startFeed() throws TelemetryException, IOException {
        
        checkNotReceiving();
        
        attemptCommand(Command.STARTFEED, true);
        this.state = State.RECEIVING;
        
        this.startHeartBeat();
    }
    
    private void startHeartBeat() throws SocketException {
        
        // Begin sending heart beats at an interval of 3 seconds until the feed is stopped.
        this.heartBeat.schedule(new TimerTask(){

            @Override
            public void run() {
                
                try {
                   
                    if(SourceTelemetryInput.this.state == State.STANDBY){
                        
                        this.cancel();
                        return;
                    }
                    
                    Socket s = SourceTelemetryInput.this.s;
                    
                    if(s == null)
                        throw new IOException();
                    else {
                        
                       s.getOutputStream().write(new HeartBeatControlFlag().getBytes());
                       s.getOutputStream().flush();
                    }
                   
                } catch (IOException e) {
                    
                    logger.debug("Telemetry receiver heartbeat generation failed. Thread terminated.");
                    SourceTelemetryInput.this.heartBeat.cancel();
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
    
    @Override
    public ConnectionType getConnectionType() {
        
        return SourceTelemetryInput.CONNECTION;
    }

    public TelemetryMessage getMessage() throws ControlFlag, IOException {
        
        // Throw a stream stopped control flag to indicate nothing is streaming.
        if(this.state == State.STANDBY)
            throw new StreamStoppedControlFlag();
        
        TelemetryMessage tm = null;
        
        while(true){
            
            tm = TelemetryMessageFactory.buildTelemetryMessage(this.dis);
                
            // Check if this message is a control flag.
            if(tm instanceof ControlFlag){
    
                if(tm instanceof ShutDownControlFlag){
                
                    try {
                        this.s.close();
                    } catch(IOException ioe){}
                    
                    throw ((ShutDownControlFlag) tm);
                }
                
                else if(tm instanceof StreamStoppedControlFlag){
                    
                    this.state = State.STANDBY;
                    this.s.setSoTimeout(this.lastTimeout);

                    throw ((StreamStoppedControlFlag) tm);
                }
                    
                else if(tm instanceof ConnectionThrownControlFlag){
                    
                    this.state = State.STANDBY;
                    throw ((ConnectionThrownControlFlag) tm);
                }
                // Used to differentiate slow telemetry and source failure.
                else if(tm instanceof HeartBeatControlFlag)
                    continue;
                
                break;
            }
        }
        
        return tm;
    }
    
    public boolean isConnected(){
    
        return this.s != null;
    }
    
    public boolean isReceiving(){
        
        return this.isConnected()
                && this.state == State.RECEIVING;
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
        this.source = null;
    }

    @Override
    public void close() {
        
        try {
            
            this.stopFeed();
            
        } catch (TelemetryException e) {}
        catch (IOException e) {}
    }
}
