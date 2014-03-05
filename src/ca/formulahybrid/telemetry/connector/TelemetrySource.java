package ca.formulahybrid.telemetry.connector;

import java.net.InetAddress;

public class TelemetrySource {

    private String name;
    private InetAddress address;
    
	public TelemetrySource(String name, InetAddress address) {
		
	    this.name = name;
	    this.address = address;
	}
	
	public String getName(){
	    
	    return this.name;
	}
	
	public InetAddress getAddress(){
	    
	    return this.address;
	}
}
