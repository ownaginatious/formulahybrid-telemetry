package ca.formulahybrid.telemetry.exception;

public class DataException extends Exception {

	private static final long serialVersionUID = 3767938627154982929L;
	
	public DataException(String message, Exception e){

		super(message, e);
	}
	
	public DataException(String message){
		
		super(message);
	}
}