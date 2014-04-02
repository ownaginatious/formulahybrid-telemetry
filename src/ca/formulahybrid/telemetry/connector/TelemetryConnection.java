package ca.formulahybrid.telemetry.connector;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import ca.formulahybrid.network.receiver.TelemetryReceiver;
import ca.formulahybrid.network.telemetry.input.SourceTelemetryInput;
import ca.formulahybrid.telemetry.exception.ConnectException;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.message.TelemetryMessage;

public class TelemetryConnection {

    private TelemetryReceiver receiver;
    
    private SourceTelemetryInput sourceConnection;
    
	/**
	 * Scans all network interfaces for a specified amount of time for telemetry sources
	 * for which there are active relay networks.
	 * 
	 * @param scanTimeout The amount of time in seconds to scan the connected local networks for.
	 * @return A set of discovered telemetry sources for which relay networks exist on the subnet.
	 */
	public Set<TelemetrySource> scan(int scanTimeout) {
		
		return null;
	}

	/**
	 * Joins the virtual listening network for a telemetry source on the local subnet.
	 * 
	 * This method can only be called if this object is not already connected to a different network.
	 * Joining a group leaves it to the group to determine the underlying transmission type (TCP/UDP).
	 * At any given moment, this node could become the relay point.
	 * 
	 * @param ts The desired telemetry source to listen to.
	 * @param scanTimeout The amount of time to scan the local network before establishing a TCP
	 * 		  connection to the telemetry source. If this node tries to connect while another
	 *        relay on the subnet exists, the telemetry source will inform it to automatically
	 *        reconnect to the source.
	 * 
	 * @throws ConnectException Thrown if the telemetry source cannot be connected to.
	 */
	public void join(TelemetrySource ts, int scanTimeout) throws ConnectException {
		
		
	}

	/**
	 * Connects to a primary telemetry message source via a TCP connection, ignoring all group
	 * communication.
	 * 
	 * @param ts The desired telemetry source to listen to.
     * @throws IOException 
	 * @throws ConnectException Thrown if the telemetry source cannot be connected to or a connection from this
	 *							object already exists.
	 * @throws TelemetryException 
	 * @throws  
	 */
	public void connect(TelemetrySource ts) throws ConnectException, TelemetryException, IOException {
	    
	    if(this.sourceConnection != null)
	        throw new ConnectException("Already connected to a source [" + this.sourceConnection.getSource() + "]");
	    
	    this.sourceConnection = new SourceTelemetryInput(ts, false);
	}

	/**
     * Disconnects from the connected relay network, telemetry source or pseudo-telemetry source (i.e. a
     * previously logged file)
     * 
     * @throws ConnectException Thrown if no connection exists to be closed.
     */
    public void disconnect() {
    	
        if(this.sourceConnection != null)
            this.sourceConnection.disconnect();
    }

    public TelemetryMessage getMessage() throws ConnectException, IOException {
    	
    	return this.receiver.getMessage();
    }

    //#TODO: Requires complete implementation.
    public TelemetryMessage sendMessage(TelemetryMessage cm) throws ConnectException {
    	
    	return null;
    }

    public void connectToLocalReplay(File f, boolean simulated) throws IOException, TelemetryException {
		
        this.receiver.connectLocalFile(f, simulated);
	}

	public void connectToLocalReplay(String path, boolean simulated) throws IOException, TelemetryException {
	    
		this.connectToLocalReplay(new File(path), simulated);
	}

	public Set<String> getRemoteReplays() throws ConnectException, IOException, TelemetryException {

        if(this.sourceConnection != null)
            throw new ConnectException("No source connection to perform the command on.");
        
        return this.sourceConnection.getLogs();
	}

	public void connectToRemoteReplay(String remoteReplayName) throws ConnectException, FileNotFoundException {

        if(this.sourceConnection != null)
            throw new ConnectException("No source connection to perform the command on.");
	    
	}

	public void storeRemoteReplay(String remoteReplayName, String path) throws ConnectException, IOException, TelemetryException{

        if(this.sourceConnection != null)
            throw new ConnectException("No source connection to perform the command on.");
        
        FileOutputStream fos = new FileOutputStream(new File(path));
        
        ByteArrayInputStream bais = new ByteArrayInputStream(this.sourceConnection.getRemoteLog(remoteReplayName));
        
        byte[] buffer = new byte[1024]; // Data buffer.
        int bytesIncoming = 0;
        
        while((bytesIncoming = bais.read(buffer)) != 0)
            fos.write(buffer, 0, bytesIncoming);
        
        fos.close();
	}
}
