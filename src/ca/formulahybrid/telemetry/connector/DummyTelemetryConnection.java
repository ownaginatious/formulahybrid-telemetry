package ca.formulahybrid.telemetry.connector;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.Map;

import ca.formulahybrid.telemetry.exception.ConnectException;
import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.message.CMessage;

public class DummyTelemetryConnection implements TelemetryConnection {

	public DummyTelemetryConnection() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Map<String, InetAddress> discover() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void connect(InetAddress address, int timeout)
			throws ConnectException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnect() throws ConnectException {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectToLocalReplay(File f) throws DataException {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectToLocalReplay(String path) throws FileNotFoundException,
			DataException {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, String> getRemoteReplays() throws ConnectException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void openRemoteReplay(String remoteReplayName)
			throws ConnectException, FileNotFoundException, DataException {
		// TODO Auto-generated method stub

	}

	@Override
	public void storeRemoteReplay(String remoteReplayName, String path,
			boolean overwrite) {
		// TODO Auto-generated method stub

	}

	@Override
	public CMessage nextMessage() throws ConnectException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CMessage nextMessage(long timeout) throws ConnectException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CMessage sendMessage(CMessage cm) throws ConnectException {
		// TODO Auto-generated method stub
		return null;
	}

}
