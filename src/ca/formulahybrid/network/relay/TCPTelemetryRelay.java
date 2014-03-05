package ca.formulahybrid.network.relay;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.message.CMessage;
import ca.formulahybrid.telemetry.message.CMessageFactory;

public class TCPTelemetryRelay implements TelemetryRelay {

	private Socket s;

	@Override
	public void connect(InetAddress address, int port) throws IOException, TelemetryException {
		
		if(s != null)
			throw new TelemetryException("The TCP socket connection to " + s.getInetAddress().toString()
					+ ":" + s.getPort()+ "needs to be closed before a new one can be opened.");
		
		s = new Socket(address, port);
	}

	@Override
	public void disconnect() throws TelemetryException, IOException {
		
		if(s == null)
			throw new TelemetryException("A connection must be made before it can be closed.");
		
		s.close();
		s = null;
	}
	
	@Override
	public CMessage getMessage() throws IOException, DataException {
		
		return CMessageFactory.buildMessage(s.getInputStream());
	}
}
