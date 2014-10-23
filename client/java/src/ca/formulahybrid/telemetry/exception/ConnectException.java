package ca.formulahybrid.telemetry.exception;

public class ConnectException extends NetworkException {

	private static final long serialVersionUID = 3767938627154982929L;

	public ConnectException(String message, Exception e){
	
		super(message, e);
	}
	
	public ConnectException(String message){
		super(message);
	}
}
