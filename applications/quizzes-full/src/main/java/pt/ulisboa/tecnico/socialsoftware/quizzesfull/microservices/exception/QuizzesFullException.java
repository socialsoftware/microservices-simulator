package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuizzesFullException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(QuizzesFullException.class);
    private final String quizzesFullErrorMessage;

    private QuizzesFullException(String quizzesFullErrorMessage, String formattedMessage, boolean alreadyFormatted) {
        super(quizzesFullErrorMessage, formattedMessage, true);
        logger.info(formattedMessage);
        this.quizzesFullErrorMessage = quizzesFullErrorMessage;
    }

    public QuizzesFullException(String quizzesFullErrorMessage) {
        super(quizzesFullErrorMessage);
        logger.info(quizzesFullErrorMessage);
        this.quizzesFullErrorMessage = quizzesFullErrorMessage;
    }

    public QuizzesFullException(String quizzesFullErrorMessage, String value) {
        super(String.format(quizzesFullErrorMessage, value));
        logger.info(String.format(quizzesFullErrorMessage, value));
        this.quizzesFullErrorMessage = quizzesFullErrorMessage;
    }

    public QuizzesFullException(String quizzesFullErrorMessage, String value1, String value2) {
        super(String.format(quizzesFullErrorMessage, value1, value2));
        logger.info(String.format(quizzesFullErrorMessage, value1, value2));
        this.quizzesFullErrorMessage = quizzesFullErrorMessage;
    }

    public QuizzesFullException(String quizzesFullErrorMessage, int value) {
        super(String.format(quizzesFullErrorMessage, value));
        logger.info(String.format(quizzesFullErrorMessage, value));
        this.quizzesFullErrorMessage = quizzesFullErrorMessage;
    }

    public QuizzesFullException(String quizzesFullErrorMessage, int value1, int value2) {
        super(String.format(quizzesFullErrorMessage, value1, value2));
        logger.info(String.format(quizzesFullErrorMessage, value1, value2));
        this.quizzesFullErrorMessage = quizzesFullErrorMessage;
    }

    public QuizzesFullException(String quizzesFullErrorMessage, String value1, int value2) {
        super(String.format(quizzesFullErrorMessage, value1, value2));
        logger.info(String.format(quizzesFullErrorMessage, value1, value2));
        this.quizzesFullErrorMessage = quizzesFullErrorMessage;
    }

    public String getErrorMessage() {
        return this.quizzesFullErrorMessage;
    }

    public static QuizzesFullException fromRemote(String quizzesFullErrorMessage, String formattedMessage) {
        return new QuizzesFullException(quizzesFullErrorMessage, formattedMessage, true);
    }
}
