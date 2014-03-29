package ca.formulahybrid.telemetry.exception;

public class TelemetryStopException extends TelemetryException {

    private static final long serialVersionUID = 3629167480563599212L;

    public TelemetryStopException(String message, Exception e) {
        super(message, e);
        // TODO Auto-generated constructor stub
    }

    public TelemetryStopException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
