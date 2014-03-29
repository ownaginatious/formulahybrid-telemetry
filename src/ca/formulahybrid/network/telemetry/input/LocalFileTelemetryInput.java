package ca.formulahybrid.network.telemetry.input;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.common.base.Charsets;

import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.exception.TelemetryTimeOutException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;

public class LocalFileTelemetryInput implements TelemetryInput {

    // TTR = Telemetry Transmission Record.
    private static final byte[] protocolHeader = "TTR".getBytes(Charsets.UTF_8);
    private static Logger logger = Logger.getLogger(LocalFileTelemetryInput.class);
    
    private FileInputStream fs;
    private int timeOffset;
    
    private boolean readComplete = false;
    private boolean simulate = false;
    
    public LocalFileTelemetryInput(File file, boolean simulate) throws IOException {
        
        this.simulate = simulate;
        this.fs = new FileInputStream(file);
        
        // Let's read the stream header to be sure this is a telemetry broadcast record.
        byte[] header = new byte[3];
        this.fs.read(header);
        
        if(!header.equals(LocalFileTelemetryInput.protocolHeader))
            throw new IOException("This file is not a telemetry broadcast record.");
        
        // Get the time that this log starts.
        this.timeOffset = new DataInputStream(this.fs).readInt();
    }

    @Override
    public void close() {
        
        try {
            
            this.fs.close();
            
        } catch (IOException e) {} // You have to wonder why this exception exists here.
        finally {
            
            this.fs = null;
        }
    }

    @Override
    public boolean connected() {
        
        return this.fs != null;
    }
    
    public boolean readComplete(){
    
        return this.readComplete;
    }
    
    public TelemetryMessage getMessage() throws IOException {
        
        TelemetryMessage tm = null;
        
        try {

            tm = TelemetryMessageFactory.buildMessage(this.fs);

            if(this.simulate){
                try {

                    int messageTime = (int) tm.getTimeStamp().getTime();
                    Thread.sleep(messageTime - this.timeOffset);
                    this.timeOffset = messageTime;

                } catch (InterruptedException e) {} // This can't happen.
            }

        } catch (EOFException e){

            this.fs = null;
            this.readComplete = true;
            
            throw new EOFException("End of telemetry record.");

        } catch (DataException e) {

            logger.debug("Dropping message : " + e.getMessage());
        }

        // Add the next incoming message to the queue.
        return tm;
    }

    @Override
    public TelemetryMessage getMessage(long timeout) throws IOException, TelemetryException, TelemetryTimeOutException {
        
        // TODO Auto-generated method stub
        return null;
    }
}
