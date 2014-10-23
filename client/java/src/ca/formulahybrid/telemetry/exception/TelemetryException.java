package ca.formulahybrid.telemetry.exception;

public class TelemetryException extends Exception {

	private static final long serialVersionUID = -3323159538030382391L;

	public TelemetryException(String message, Exception e){
		
		super(message, e);
	}
	
	public TelemetryException(String message){
		super(message);
	}
}
