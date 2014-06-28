package ca.formulahybrid.network.telemetry.output;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.common.base.Charsets;

import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;
import ca.formulahybrid.telemetry.message.vehicle.VehicleMessage;

public class LocalFileTelemetryOutput implements TelemetryOutput {

    // TTR = Telemetry Transmission Record.
    public static final byte[] PROTOCOL_HEADER = "TTR".getBytes(Charsets.UTF_8);
    
    private Logger logger = Logger.getLogger(LocalFileTelemetryOutput.class);
    
    private FileOutputStream fos;
    private String path;
    
    private boolean first = true;
    
    public LocalFileTelemetryOutput(File f) throws IOException {
        
        this.fos = new FileOutputStream(f);
        this.path = f.getName();
        
        // Write the header.
        this.fos.write(LocalFileTelemetryOutput.PROTOCOL_HEADER);
    }

    @Override
    public void sendBinary(byte[] message) throws IOException {
        
        // We want to extract the date of the first message to use as the
        // time-offset for our telemetry log.
        if(first){
            
            TelemetryMessage tm = TelemetryMessageFactory.parseTelemetryMessage(message);
            
            // Only vehicle messages contain dates.
            if(tm instanceof VehicleMessage){
                
                new DataOutputStream(this.fos).writeInt((int)(((VehicleMessage) tm).getTimeStamp().getTime() / 1000));
                this.first = false;
            }
        }
        
        this.fos.write(message);
    }

    @Override
    public void sendMessage(TelemetryMessage message) throws IOException {
        
        this.sendBinary(message.getBytes());
    }

    @Override
    public boolean isConnected() {
        
        return this.fos != null;
    }

    @Override
    public void close() {

        try {
            
            this.fos.close();
            logger.debug("Log at " + this.path + " successfully closed.");
            
        } catch (IOException e) {
            logger.debug(e.getMessage());
        }
    }
    
    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();

        return sb.append("[ TOUTPUT ").append("FILE @ ")
                .append(this.path).toString();
    }
}