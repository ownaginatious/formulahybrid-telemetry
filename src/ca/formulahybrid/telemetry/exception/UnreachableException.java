package ca.formulahybrid.telemetry.exception;

public class UnreachableException extends Exception {

    private static final long serialVersionUID = 8803353375064793306L;

    public UnreachableException(String message, Exception e){
		
		super(message, e);
	}
	
	public UnreachableException(String message){
		super(message);
	}
}
