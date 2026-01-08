package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TeastoreException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(TeastoreException.class);
    private final String teastoreErrorMessage;

    public TeastoreException(String teastoreErrorMessage) {
        super(teastoreErrorMessage);
        logger.info(teastoreErrorMessage);
        this.teastoreErrorMessage = teastoreErrorMessage;
    }

    public TeastoreException(String teastoreErrorMessage, String value) {
        super(String.format(teastoreErrorMessage, value));
        logger.info(String.format(teastoreErrorMessage, value));
        this.teastoreErrorMessage = teastoreErrorMessage;
    }

    public TeastoreException(String teastoreErrorMessage, String value1, String value2) {
        super(String.format(teastoreErrorMessage, value1, value2));
        logger.info(String.format(teastoreErrorMessage, value1, value2));
        this.teastoreErrorMessage = teastoreErrorMessage;
    }

    public TeastoreException(String teastoreErrorMessage, int value) {
        super(String.format(teastoreErrorMessage, value));
        logger.info(String.format(teastoreErrorMessage, value));
        this.teastoreErrorMessage = teastoreErrorMessage;
    }

    public TeastoreException(String teastoreErrorMessage, int value1, int value2) {
        super(String.format(teastoreErrorMessage, value1, value2));
        logger.info(String.format(teastoreErrorMessage, value1, value2));
        this.teastoreErrorMessage = teastoreErrorMessage;
    }

    public TeastoreException(String teastoreErrorMessage, String value1, int value2) {
        super(String.format(teastoreErrorMessage, value1, value2));
        logger.info(String.format(teastoreErrorMessage, value1, value2));
        this.teastoreErrorMessage = teastoreErrorMessage;
    }

    public String getErrorMessage() {
        return this.teastoreErrorMessage;
    }
}