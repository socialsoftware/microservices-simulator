package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AdvancedException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedException.class);
    private final String advancedErrorMessage;

    public AdvancedException(String advancedErrorMessage) {
        super(advancedErrorMessage);
        logger.info(advancedErrorMessage);
        this.advancedErrorMessage = advancedErrorMessage;
    }

    public AdvancedException(String advancedErrorMessage, String value) {
        super(String.format(advancedErrorMessage, value));
        logger.info(String.format(advancedErrorMessage, value));
        this.advancedErrorMessage = advancedErrorMessage;
    }

    public AdvancedException(String advancedErrorMessage, String value1, String value2) {
        super(String.format(advancedErrorMessage, value1, value2));
        logger.info(String.format(advancedErrorMessage, value1, value2));
        this.advancedErrorMessage = advancedErrorMessage;
    }

    public AdvancedException(String advancedErrorMessage, int value) {
        super(String.format(advancedErrorMessage, value));
        logger.info(String.format(advancedErrorMessage, value));
        this.advancedErrorMessage = advancedErrorMessage;
    }

    public AdvancedException(String advancedErrorMessage, int value1, int value2) {
        super(String.format(advancedErrorMessage, value1, value2));
        logger.info(String.format(advancedErrorMessage, value1, value2));
        this.advancedErrorMessage = advancedErrorMessage;
    }

    public AdvancedException(String advancedErrorMessage, String value1, int value2) {
        super(String.format(advancedErrorMessage, value1, value2));
        logger.info(String.format(advancedErrorMessage, value1, value2));
        this.advancedErrorMessage = advancedErrorMessage;
    }

    public String getErrorMessage() {
        return this.advancedErrorMessage;
    }
}