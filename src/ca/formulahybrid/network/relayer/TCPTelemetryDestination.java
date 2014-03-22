package ca.formulahybrid.network.relayer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import ca.formulahybrid.telemetry.message.TelemetryMessage;

public class TCPTelemetryDestination implements TelemetryDestination {

    private OutputStream os;
    
    public TCPTelemetryDestination(Socket s) throws IOException {
        
        this.os = s.getOutputStream();
    }

    @Override
    public void sendBinary(byte[] message) throws IOException {
        
        this.os.write(message);
    }

    @Override
    public void sendMessage(TelemetryMessage message) {

        message.getBytes();
    }
}
