package ca.formulahybrid.network.relayer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import ca.formulahybrid.network.receiver.TelemetryReceiverConnection;
import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;
import ca.formulahybrid.telemetry.message.TelemetryMessageFactory;

public class LocalTelemetryDestination implements TelemetryDestination, TelemetryReceiverConnection {

    private static Logger logger = Logger.getLogger(LocalTelemetryDestination.class);
    
    private BlockingQueue<TelemetryMessage> bq;
    
    public LocalTelemetryDestination(BlockingQueue<TelemetryMessage> bq) {
        
        this.bq = bq;
    }

    @Override
    public void sendBinary(byte[] message) throws IOException {
        
        try {
            this.sendMessage(TelemetryMessageFactory.buildMessage(new ByteArrayInputStream(message)));
        } catch (DataException e) {
            logger.info(String.format("Dropped broken message [%s].", e.getMessage()));
        }
    }

    @Override
    public void sendMessage(TelemetryMessage message) {
        
        this.bq.add(message);
    }

    @Override
    public void connect(InetAddress address, int port, BlockingQueue<TelemetryMessage> bq) {
        
        return;
    }

    @Override
    public void disconnect() {
        
        return;
    }

    @Override
    public boolean connected() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getLossRate() {
        
        return 0;
    }

}
