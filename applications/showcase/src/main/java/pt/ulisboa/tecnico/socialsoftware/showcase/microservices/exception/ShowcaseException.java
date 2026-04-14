package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ShowcaseException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(ShowcaseException.class);
    private final String showcaseErrorMessage;

    public ShowcaseException(String showcaseErrorMessage) {
        super(showcaseErrorMessage);
        logger.info(showcaseErrorMessage);
        this.showcaseErrorMessage = showcaseErrorMessage;
    }

    public ShowcaseException(String showcaseErrorMessage, String value) {
        super(String.format(showcaseErrorMessage, value));
        logger.info(String.format(showcaseErrorMessage, value));
        this.showcaseErrorMessage = showcaseErrorMessage;
    }

    public ShowcaseException(String showcaseErrorMessage, String value1, String value2) {
        super(String.format(showcaseErrorMessage, value1, value2));
        logger.info(String.format(showcaseErrorMessage, value1, value2));
        this.showcaseErrorMessage = showcaseErrorMessage;
    }

    public ShowcaseException(String showcaseErrorMessage, int value) {
        super(String.format(showcaseErrorMessage, value));
        logger.info(String.format(showcaseErrorMessage, value));
        this.showcaseErrorMessage = showcaseErrorMessage;
    }

    public ShowcaseException(String showcaseErrorMessage, int value1, int value2) {
        super(String.format(showcaseErrorMessage, value1, value2));
        logger.info(String.format(showcaseErrorMessage, value1, value2));
        this.showcaseErrorMessage = showcaseErrorMessage;
    }

    public ShowcaseException(String showcaseErrorMessage, String value1, int value2) {
        super(String.format(showcaseErrorMessage, value1, value2));
        logger.info(String.format(showcaseErrorMessage, value1, value2));
        this.showcaseErrorMessage = showcaseErrorMessage;
    }

    public String getErrorMessage() {
        return this.showcaseErrorMessage;
    }
}