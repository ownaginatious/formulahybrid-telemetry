package ca.formulahybrid.network.telemetry.input;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.google.common.base.Charsets;

import ca.formulahybrid.network.receiver.TelemetryReceiver.ConnectionType;
import ca.formulahybrid.network.telemetry.output.ReliableRelayTelemetryOutput;
import ca.formulahybrid.telemetry.connector.TelemetrySource;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;
import ca.formulahybrid.telemetry.message.control.ControlFlag;
import ca.formulahybrid.telemetry.message.control.HeartBeatControlFlag;
import ca.formulahybrid.telemetry.message.control.ShutDownControlFlag;

public class ReliableRelayTelemetryInput implements TelemetryInput {

    private static final ConnectionType CONNECTION = ConnectionType.RELIABLERELAY;
    
    private Logger logger = Logger.getLogger(ReliableRelayTelemetryInput.class);
    
    private Socket s;
    private Timer heartBeat = new Timer();
    
    public ReliableRelayTelemetryInput(InetAddress address, int port, TelemetrySource desiredSource) throws IOException, TelemetryException {
        
        this.s = new Socket(address, port);
        this.s.setSoTimeout(5000); // We will drop a server connection after 5 seconds of silence.

        InputStream is = this.s.getInputStream();

        // *****************************
        // Start identifying the server.
        // *****************************
        
        byte[] protocolHeader = ReliableRelayTelemetryOutput.protocolIdentifier;
        
        // Ensure that this is a relay.
        byte[] header = new byte[protocolHeader.length];
        
        is.read(header);
        
        if(!header.equals(protocolHeader))
            throw new IOException("Expected protocol header '"
                    + new String(protocolHeader, Charsets.UTF_8)
                    + "'. Received " + new String(header, Charsets.UTF_8) + " instead.");
        
        byte[] nameBuffer = new byte[is.read()];
        is.read(nameBuffer);
        
        String name = new String(nameBuffer, Charsets.UTF_8);
        
        if(!name.equals(desiredSource.getName()))
            throw new TelemetryException("The connected node identified as a telemetry source, but is relaying for " 
                    + name + ", not the expected " + desiredSource.getName());

        // ****************************
        
        // Begin sending heart beats at an interval of 3 seconds until the connection dies.
        this.heartBeat.schedule(new TimerTask(){

            @Override
            public void run() {
                
                try {
                   
                    Socket s = ReliableRelayTelemetryInput.this.s;
                    
                    if(s == null)
                        throw new IOException();
                    else {
                        
                       s.getOutputStream().write(new HeartBeatControlFlag().getBytes());
                       s.getOutputStream().flush();
                    }
                   
                } catch (IOException e) {
                    
                    logger.debug("Telemetry receiver heartbeat generation failed. Thread terminated.");
                    ReliableRelayTelemetryInput.this.heartBeat.cancel();
                }
                
            }}, 0, 3000);
    }

    @Override
    public void close() {
        
        try {
            
            if(this.s != null)
                this.s.close();
            
        } catch(IOException ioe){}
        finally {
            
            this.s = null;
        }
    }

    @Override
    public boolean isConnected() {
        
        return this.s != null;
    }
    
    @Override
    public TelemetryMessage getMessage() throws IOException, ControlFlag {
        
        while(true){
            
            TelemetryMessage tm = TelemetryMessageFactory.buildTelemetryMessage(this.s.getInputStream());
            
            // Check if this message is a control flag.
            if(tm instanceof ControlFlag){
    
                if(tm instanceof ShutDownControlFlag){
                
                    try {
                        this.s.close();
                    } catch(IOException ioe){}
                    
                    throw ((ShutDownControlFlag) tm);
                }
                // Used to differentiate slow telemetry and source failure.
                else if(tm instanceof HeartBeatControlFlag)
                    continue;
                else
                    throw (ControlFlag) tm;
            }
            
            return tm;
        }
    }

    @Override
    public String toString(){
        
        StringBuilder sb = new StringBuilder();
        
        return sb.append("[ ").append("TCP @ ")
                .append(s.getInetAddress()).append(":")
                .append(s.getPort()).toString();
    }

    @Override
    public ConnectionType getConnectionType() {
        
        return ReliableRelayTelemetryInput.CONNECTION;
    }
}