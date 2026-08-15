package pt.ulisboa.tecnico.socialsoftware.ms.notification;

public class EventReplayException extends RuntimeException {
    private final String reason;

    public EventReplayException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public EventReplayException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
