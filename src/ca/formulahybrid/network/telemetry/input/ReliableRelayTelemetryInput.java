package ca.formulahybrid.network.telemetry.input;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.exception.TelemetryTimeOutException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;

public class ReliableRelayTelemetryInput implements TelemetryInput {

    public static final byte heartBeatFlag = 'H';
    
    private Logger logger = Logger.getLogger(ReliableRelayTelemetryInput.class);
    
    private Socket s;
    private Timer heartBeat = new Timer();
    
    public ReliableRelayTelemetryInput(InetAddress address, int port) throws IOException {
        
        if(this.s != null)
            return;
        
        this.s = new Socket(address, port);
        
        // Begin sending heart beats at an interval of 3 seconds until the connection dies.
        this.heartBeat.schedule(new TimerTask(){

            @Override
            public void run() {
                
                try {
                   
                    Socket s = ReliableRelayTelemetryInput.this.s;
                    
                    if(s == null)
                        throw new IOException();
                    else
                       s.getOutputStream().write(ReliableRelayTelemetryInput.heartBeatFlag);
                   
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
    
    //#TODO: Add support for telemetry pseudo message flags (close, heart beat)
    @Override
    public TelemetryMessage getMessage() throws IOException {
        
        TelemetryMessage tm = null;
        
        try {
            
            tm = TelemetryMessageFactory.buildMessage(this.s.getInputStream());
            
        } catch (DataException e) {
            
            logger.debug("Dropping message : " + e.getMessage());
        }
        
        return tm;
    }

    @Override
    public TelemetryMessage getMessage(long timeout) throws IOException, TelemetryException, TelemetryTimeOutException {
        
        return null;
    }
}