package ca.formulahybrid.telemetry.exception;

public class DataValidityException extends DataException {

	private int code;
	
	public DataValidityException(int code, String message, Exception e){

		super(message + "(code: " + code + ")", e);
		this.code = code;
	}
	
	public DataValidityException(int code, String message){
		
		super(message + "(code: " + code + ")");
		this.code = code;
	}
	
	public DataValidityException(String message, Exception e){

		this(0, message, e);
	}
	
	public DataValidityException(String message){
		
		this(0, message);
	}
	
	public int getCode(){
		
		return code;
	}
}
