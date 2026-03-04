package pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AnswersException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(AnswersException.class);
    private final String answersErrorMessage;

    public AnswersException(String answersErrorMessage) {
        super(answersErrorMessage);
        logger.info(answersErrorMessage);
        this.answersErrorMessage = answersErrorMessage;
    }

    public AnswersException(String answersErrorMessage, String value) {
        super(String.format(answersErrorMessage, value));
        logger.info(String.format(answersErrorMessage, value));
        this.answersErrorMessage = answersErrorMessage;
    }

    public AnswersException(String answersErrorMessage, String value1, String value2) {
        super(String.format(answersErrorMessage, value1, value2));
        logger.info(String.format(answersErrorMessage, value1, value2));
        this.answersErrorMessage = answersErrorMessage;
    }

    public AnswersException(String answersErrorMessage, int value) {
        super(String.format(answersErrorMessage, value));
        logger.info(String.format(answersErrorMessage, value));
        this.answersErrorMessage = answersErrorMessage;
    }

    public AnswersException(String answersErrorMessage, int value1, int value2) {
        super(String.format(answersErrorMessage, value1, value2));
        logger.info(String.format(answersErrorMessage, value1, value2));
        this.answersErrorMessage = answersErrorMessage;
    }

    public AnswersException(String answersErrorMessage, String value1, int value2) {
        super(String.format(answersErrorMessage, value1, value2));
        logger.info(String.format(answersErrorMessage, value1, value2));
        this.answersErrorMessage = answersErrorMessage;
    }

    public String getErrorMessage() {
        return this.answersErrorMessage;
    }
}