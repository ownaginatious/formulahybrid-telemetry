package ca.formulahybrid.telemetry.exception;

public class UnreachableException extends Exception {

    // We do not care about serializing this exception.
    private static final long serialVersionUID = 1L;

    public UnreachableException(String message, Exception e){
		
		super(message, e);
	}
	
	public UnreachableException(String message){
		super(message);
	}
}
