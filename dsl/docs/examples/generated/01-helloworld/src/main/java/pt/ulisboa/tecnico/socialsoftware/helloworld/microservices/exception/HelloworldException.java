package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class HelloworldException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(HelloworldException.class);
    private final String helloworldErrorMessage;

    public HelloworldException(String helloworldErrorMessage) {
        super(helloworldErrorMessage);
        logger.info(helloworldErrorMessage);
        this.helloworldErrorMessage = helloworldErrorMessage;
    }

    public HelloworldException(String helloworldErrorMessage, String value) {
        super(String.format(helloworldErrorMessage, value));
        logger.info(String.format(helloworldErrorMessage, value));
        this.helloworldErrorMessage = helloworldErrorMessage;
    }

    public HelloworldException(String helloworldErrorMessage, String value1, String value2) {
        super(String.format(helloworldErrorMessage, value1, value2));
        logger.info(String.format(helloworldErrorMessage, value1, value2));
        this.helloworldErrorMessage = helloworldErrorMessage;
    }

    public HelloworldException(String helloworldErrorMessage, int value) {
        super(String.format(helloworldErrorMessage, value));
        logger.info(String.format(helloworldErrorMessage, value));
        this.helloworldErrorMessage = helloworldErrorMessage;
    }

    public HelloworldException(String helloworldErrorMessage, int value1, int value2) {
        super(String.format(helloworldErrorMessage, value1, value2));
        logger.info(String.format(helloworldErrorMessage, value1, value2));
        this.helloworldErrorMessage = helloworldErrorMessage;
    }

    public HelloworldException(String helloworldErrorMessage, String value1, int value2) {
        super(String.format(helloworldErrorMessage, value1, value2));
        logger.info(String.format(helloworldErrorMessage, value1, value2));
        this.helloworldErrorMessage = helloworldErrorMessage;
    }

    public String getErrorMessage() {
        return this.helloworldErrorMessage;
    }
}