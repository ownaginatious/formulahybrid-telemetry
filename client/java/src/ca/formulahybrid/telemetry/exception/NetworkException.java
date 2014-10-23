package ca.formulahybrid.telemetry.exception;

public class NetworkException extends Exception {

	private static final long serialVersionUID = 3767938627154982929L;

	public NetworkException(String message, Exception e){
	
		super(message, e);
	}
	
	public NetworkException(String message){
		super(message);
	}
}
