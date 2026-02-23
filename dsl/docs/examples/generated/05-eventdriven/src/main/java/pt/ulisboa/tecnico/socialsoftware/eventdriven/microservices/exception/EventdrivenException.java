package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EventdrivenException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(EventdrivenException.class);
    private final String eventdrivenErrorMessage;

    public EventdrivenException(String eventdrivenErrorMessage) {
        super(eventdrivenErrorMessage);
        logger.info(eventdrivenErrorMessage);
        this.eventdrivenErrorMessage = eventdrivenErrorMessage;
    }

    public EventdrivenException(String eventdrivenErrorMessage, String value) {
        super(String.format(eventdrivenErrorMessage, value));
        logger.info(String.format(eventdrivenErrorMessage, value));
        this.eventdrivenErrorMessage = eventdrivenErrorMessage;
    }

    public EventdrivenException(String eventdrivenErrorMessage, String value1, String value2) {
        super(String.format(eventdrivenErrorMessage, value1, value2));
        logger.info(String.format(eventdrivenErrorMessage, value1, value2));
        this.eventdrivenErrorMessage = eventdrivenErrorMessage;
    }

    public EventdrivenException(String eventdrivenErrorMessage, int value) {
        super(String.format(eventdrivenErrorMessage, value));
        logger.info(String.format(eventdrivenErrorMessage, value));
        this.eventdrivenErrorMessage = eventdrivenErrorMessage;
    }

    public EventdrivenException(String eventdrivenErrorMessage, int value1, int value2) {
        super(String.format(eventdrivenErrorMessage, value1, value2));
        logger.info(String.format(eventdrivenErrorMessage, value1, value2));
        this.eventdrivenErrorMessage = eventdrivenErrorMessage;
    }

    public EventdrivenException(String eventdrivenErrorMessage, String value1, int value2) {
        super(String.format(eventdrivenErrorMessage, value1, value2));
        logger.info(String.format(eventdrivenErrorMessage, value1, value2));
        this.eventdrivenErrorMessage = eventdrivenErrorMessage;
    }

    public String getErrorMessage() {
        return this.eventdrivenErrorMessage;
    }
}