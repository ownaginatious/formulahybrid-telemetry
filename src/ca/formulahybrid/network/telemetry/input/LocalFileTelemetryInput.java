package ca.formulahybrid.network.telemetry.input;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ca.formulahybrid.network.receiver.TelemetryReceiver.ConnectionType;
import ca.formulahybrid.network.telemetry.output.LocalFileTelemetryOutput;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;
import ca.formulahybrid.telemetry.message.control.ShutDownControlFlag;
import ca.formulahybrid.telemetry.message.vehicle.VehicleMessage;

public class LocalFileTelemetryInput implements TelemetryInput {

    private static final ConnectionType CONNECTION = ConnectionType.LOCAL;
    
    private FileInputStream fs = null;
    private int timeOffset;
    
    private String path;
    
    private boolean readComplete = false;
    private boolean simulate = false;
    
    public LocalFileTelemetryInput(File file, boolean simulate) throws TelemetryException, IOException {
        
        this.simulate = simulate;
        this.fs = new FileInputStream(file);
        this.path = file.getAbsolutePath();
        
        // Let's read the stream header to be sure this is a telemetry broadcast record.
        byte[] header = new byte[3];
        this.fs.read(header);
        
        if(!header.equals(LocalFileTelemetryOutput.PROTOCOL_HEADER))
            throw new TelemetryException("This file is not a telemetry broadcast record.");
        
        // Get the time that this log starts.
        this.timeOffset = new DataInputStream(this.fs).readInt();
    }

    @Override
    public ConnectionType getConnectionType() {
        
        return LocalFileTelemetryInput.CONNECTION;
    }

    public boolean readComplete(){
    
        return this.readComplete;
    }

    @Override
    public boolean isConnected() {
        
        return this.fs != null;
    }
    
    public TelemetryMessage getMessage() throws IOException, ShutDownControlFlag {
        
        TelemetryMessage tm = null;
        
        try {

            tm = TelemetryMessageFactory.buildTelemetryMessage(this.fs);

            if(this.simulate){
                try {

                    if(tm instanceof VehicleMessage){
                        
                        int messageTime = (int) ((VehicleMessage) tm).getTimeStamp().getTime();
                        Thread.sleep(messageTime - this.timeOffset);
                        this.timeOffset = messageTime;
                    }

                } catch (InterruptedException e) {} // This can't happen.
            }

        } catch (EOFException e){

            this.fs = null;
            this.readComplete = true;
            
            throw new ShutDownControlFlag();
        }

        // Add the next incoming message to the queue.
        return tm;
    }
    
    @Override
    public void close() {
        
        try {
            
            this.fs.close();
            
        } catch (IOException e) {} // Just close it.
        finally {
            
            this.fs = null;
        }
    }

    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();

        return sb.append("[ TINPUT ").append("FILE @ ")
                .append(this.path).toString();
    }
}