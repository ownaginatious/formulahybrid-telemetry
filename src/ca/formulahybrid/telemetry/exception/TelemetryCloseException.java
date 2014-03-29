package ca.formulahybrid.telemetry.exception;

public class TelemetryCloseException extends TelemetryException {

    private static final long serialVersionUID = 3629167480563599212L;

    public TelemetryCloseException(String message, Exception e) {
        super(message, e);
    }

    public TelemetryCloseException(String message) {
        super(message);
    }

}
