package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuizzesException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(QuizzesException.class);
    private final String quizzesErrorMessage;

    public QuizzesException(String quizzesErrorMessage) {
        super(quizzesErrorMessage);
        logger.info(quizzesErrorMessage);
        this.quizzesErrorMessage = quizzesErrorMessage;
    }

    public QuizzesException(String quizzesErrorMessage, String value) {
        super(String.format(quizzesErrorMessage, value));
        logger.info(String.format(quizzesErrorMessage, value));
        this.quizzesErrorMessage = quizzesErrorMessage;
    }

    public QuizzesException(String quizzesErrorMessage, String value1, String value2) {
        super(String.format(quizzesErrorMessage, value1, value2));
        logger.info(String.format(quizzesErrorMessage, value1, value2));
        this.quizzesErrorMessage = quizzesErrorMessage;
    }

    public QuizzesException(String quizzesErrorMessage, int value) {
        super(String.format(quizzesErrorMessage, value));
        logger.info(String.format(quizzesErrorMessage, value));
        this.quizzesErrorMessage = quizzesErrorMessage;
    }

    public QuizzesException(String quizzesErrorMessage, int value1, int value2) {
        super(String.format(quizzesErrorMessage, value1, value2));
        logger.info(String.format(quizzesErrorMessage, value1, value2));
        this.quizzesErrorMessage = quizzesErrorMessage;
    }

    public QuizzesException(String quizzesErrorMessage, String value1, int value2) {
        super(String.format(quizzesErrorMessage, value1, value2));
        logger.info(String.format(quizzesErrorMessage, value1, value2));
        this.quizzesErrorMessage = quizzesErrorMessage;
    }

    public String getErrorMessage() {
        return this.quizzesErrorMessage;
    }
}