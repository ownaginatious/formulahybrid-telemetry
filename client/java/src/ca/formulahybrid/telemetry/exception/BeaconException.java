package ca.formulahybrid.telemetry.exception;

public class BeaconException extends Exception {

	private static final long serialVersionUID = 9139359540941146422L;

	public BeaconException(String message, Exception e){
		
		super(message, e);
	}
	
	public BeaconException(String message){
		super(message);
	}
}
