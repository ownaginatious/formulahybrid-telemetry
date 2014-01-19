package ca.formulahybrid.telemetry.connector;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.Map;

import ca.formulahybrid.telemetry.exception.ConnectException;
import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.message.CMessage;

public interface TelemetryConnection {

	public Map<String, InetAddress> discover();
	
	public void connect(InetAddress address, int timeout) throws ConnectException;
	
	public void disconnect() throws ConnectException;
	
	public void connectToLocalReplay(File f) throws DataException;
	
	public void connectToLocalReplay(String path) throws FileNotFoundException, DataException;
	
	public Map<String, String> getRemoteReplays() throws ConnectException;
	
	public void openRemoteReplay(String remoteReplayName) throws ConnectException, FileNotFoundException, DataException;
	
	public void storeRemoteReplay(String remoteReplayName, String path, boolean overwrite) ;
	
	public CMessage nextMessage() throws ConnectException;
	
	public CMessage nextMessage(long timeout) throws ConnectException;
	
	public CMessage sendMessage(CMessage cm) throws ConnectException;
}
