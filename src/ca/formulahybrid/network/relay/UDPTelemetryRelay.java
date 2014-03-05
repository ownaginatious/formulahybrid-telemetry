package ca.formulahybrid.network.relay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import ca.formulahybrid.telemetry.exception.DataException;
import ca.formulahybrid.telemetry.exception.TelemetryException;
import ca.formulahybrid.telemetry.message.CMessage;
import ca.formulahybrid.telemetry.message.CMessageFactory;

public class UDPTelemetryRelay implements TelemetryRelay {

	private InetAddress address;
	private DatagramSocket ds;
	
	@Override
	public void connect(InetAddress address, int port) throws TelemetryException, IOException {
		
		if(ds != null)
			throw new TelemetryException("The UDP socket connection to " + this.address
					+ ":" + ds.getPort()+ "needs to be closed before a new one can be opened.");
		
		this.address = address;
		
		ds = new DatagramSocket(port);
	}

	@Override
	public void disconnect() throws TelemetryException {
		
		if(ds == null)
			throw new TelemetryException("A connection must be made before it can be closed.");
	
		ds.close();
		ds = null;
	}
	
	@Override
	public CMessage getMessage() throws IOException, DataException {
		
		while(true){
			
			byte[] buffer = new byte[1000];
			
			DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
			
			ds.receive(dp);
			
			// We only care about datagrams arriving from the origin.
			if(!dp.getAddress().equals(this.address))
				continue;
			
			CMessage cm = CMessageFactory.parseMessage(dp.getData());
			
			if(cm != null)
				return cm;
		}
	}
}
