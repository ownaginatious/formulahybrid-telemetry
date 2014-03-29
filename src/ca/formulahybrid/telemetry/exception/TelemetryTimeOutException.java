package ca.formulahybrid.telemetry.exception;

public class TelemetryTimeOutException extends TelemetryException {

    private static final long serialVersionUID = -5692433483573887324L;

    public TelemetryTimeOutException(String message, Exception e) {
        
        super(message, e);
    }

    public TelemetryTimeOutException(String message) {
        
        super(message);
    }
}
