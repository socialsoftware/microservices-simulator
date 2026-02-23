package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CrossrefsException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(CrossrefsException.class);
    private final String crossrefsErrorMessage;

    public CrossrefsException(String crossrefsErrorMessage) {
        super(crossrefsErrorMessage);
        logger.info(crossrefsErrorMessage);
        this.crossrefsErrorMessage = crossrefsErrorMessage;
    }

    public CrossrefsException(String crossrefsErrorMessage, String value) {
        super(String.format(crossrefsErrorMessage, value));
        logger.info(String.format(crossrefsErrorMessage, value));
        this.crossrefsErrorMessage = crossrefsErrorMessage;
    }

    public CrossrefsException(String crossrefsErrorMessage, String value1, String value2) {
        super(String.format(crossrefsErrorMessage, value1, value2));
        logger.info(String.format(crossrefsErrorMessage, value1, value2));
        this.crossrefsErrorMessage = crossrefsErrorMessage;
    }

    public CrossrefsException(String crossrefsErrorMessage, int value) {
        super(String.format(crossrefsErrorMessage, value));
        logger.info(String.format(crossrefsErrorMessage, value));
        this.crossrefsErrorMessage = crossrefsErrorMessage;
    }

    public CrossrefsException(String crossrefsErrorMessage, int value1, int value2) {
        super(String.format(crossrefsErrorMessage, value1, value2));
        logger.info(String.format(crossrefsErrorMessage, value1, value2));
        this.crossrefsErrorMessage = crossrefsErrorMessage;
    }

    public CrossrefsException(String crossrefsErrorMessage, String value1, int value2) {
        super(String.format(crossrefsErrorMessage, value1, value2));
        logger.info(String.format(crossrefsErrorMessage, value1, value2));
        this.crossrefsErrorMessage = crossrefsErrorMessage;
    }

    public String getErrorMessage() {
        return this.crossrefsErrorMessage;
    }
}