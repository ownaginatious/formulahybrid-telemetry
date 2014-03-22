package ca.formulahybrid.telemetry.connector;

import java.net.InetAddress;

public abstract class TelemetrySource {

    protected String name;
    protected InetAddress address;
    
    public TelemetrySource(String name, InetAddress address){
        
        this.name = name;
        this.address = address;
    }
	
	public final String getName(){
	    
	    return this.name;
	}
	
	public final InetAddress getSourceAddress(){
	    
	    return this.address;
	}
	
	@Override
	public boolean equals(Object o){
	    
	    if(!(o instanceof TelemetrySource))
	        return false;
	    
	    TelemetrySource ts = (TelemetrySource) o;
	    
	    return ts.name.equals(this.name);
	}
	
	@Override
	public int hashCode(){
	    
	    return this.name.hashCode();
	}
}
