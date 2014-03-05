package ca.formulahybrid.network.relay;

import java.io.IOException;
import java.net.InetAddress;

import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.message.CMessage;

public interface TelemetryRelay {

	public void connect(InetAddress address, int port) throws IOException, TelemetryException;
	public void disconnect() throws IOException, TelemetryException;
	
	public CMessage getMessage() throws IOException, DataException;
}
