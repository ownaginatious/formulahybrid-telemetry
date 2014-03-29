package ca.formulahybrid.telemetry.exception;

public class TelemetryThrownException extends TelemetryException {

    private static final long serialVersionUID = 7872437467605727159L;

    public TelemetryThrownException(String message, Exception e) {
        
        super(message, e);
    }

    public TelemetryThrownException(String message) {
        
        super(message);
    }

}
