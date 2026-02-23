package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TutorialException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(TutorialException.class);
    private final String tutorialErrorMessage;

    public TutorialException(String tutorialErrorMessage) {
        super(tutorialErrorMessage);
        logger.info(tutorialErrorMessage);
        this.tutorialErrorMessage = tutorialErrorMessage;
    }

    public TutorialException(String tutorialErrorMessage, String value) {
        super(String.format(tutorialErrorMessage, value));
        logger.info(String.format(tutorialErrorMessage, value));
        this.tutorialErrorMessage = tutorialErrorMessage;
    }

    public TutorialException(String tutorialErrorMessage, String value1, String value2) {
        super(String.format(tutorialErrorMessage, value1, value2));
        logger.info(String.format(tutorialErrorMessage, value1, value2));
        this.tutorialErrorMessage = tutorialErrorMessage;
    }

    public TutorialException(String tutorialErrorMessage, int value) {
        super(String.format(tutorialErrorMessage, value));
        logger.info(String.format(tutorialErrorMessage, value));
        this.tutorialErrorMessage = tutorialErrorMessage;
    }

    public TutorialException(String tutorialErrorMessage, int value1, int value2) {
        super(String.format(tutorialErrorMessage, value1, value2));
        logger.info(String.format(tutorialErrorMessage, value1, value2));
        this.tutorialErrorMessage = tutorialErrorMessage;
    }

    public TutorialException(String tutorialErrorMessage, String value1, int value2) {
        super(String.format(tutorialErrorMessage, value1, value2));
        logger.info(String.format(tutorialErrorMessage, value1, value2));
        this.tutorialErrorMessage = tutorialErrorMessage;
    }

    public String getErrorMessage() {
        return this.tutorialErrorMessage;
    }
}