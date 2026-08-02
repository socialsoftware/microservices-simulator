package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuizzesFull2Exception extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(QuizzesFull2Exception.class);
    private final String quizzesFull2ErrorMessage;

    private QuizzesFull2Exception(String quizzesFull2ErrorMessage, String formattedMessage, boolean alreadyFormatted) {
        super(quizzesFull2ErrorMessage, formattedMessage, true);
        logger.info(formattedMessage);
        this.quizzesFull2ErrorMessage = quizzesFull2ErrorMessage;
    }

    public QuizzesFull2Exception(String quizzesFull2ErrorMessage) {
        super(quizzesFull2ErrorMessage);
        logger.info(quizzesFull2ErrorMessage);
        this.quizzesFull2ErrorMessage = quizzesFull2ErrorMessage;
    }

    public QuizzesFull2Exception(String quizzesFull2ErrorMessage, String value) {
        super(String.format(quizzesFull2ErrorMessage, value));
        logger.info(String.format(quizzesFull2ErrorMessage, value));
        this.quizzesFull2ErrorMessage = quizzesFull2ErrorMessage;
    }

    public QuizzesFull2Exception(String quizzesFull2ErrorMessage, String value1, String value2) {
        super(String.format(quizzesFull2ErrorMessage, value1, value2));
        logger.info(String.format(quizzesFull2ErrorMessage, value1, value2));
        this.quizzesFull2ErrorMessage = quizzesFull2ErrorMessage;
    }

    public QuizzesFull2Exception(String quizzesFull2ErrorMessage, int value) {
        super(String.format(quizzesFull2ErrorMessage, value));
        logger.info(String.format(quizzesFull2ErrorMessage, value));
        this.quizzesFull2ErrorMessage = quizzesFull2ErrorMessage;
    }

    public QuizzesFull2Exception(String quizzesFull2ErrorMessage, int value1, int value2) {
        super(String.format(quizzesFull2ErrorMessage, value1, value2));
        logger.info(String.format(quizzesFull2ErrorMessage, value1, value2));
        this.quizzesFull2ErrorMessage = quizzesFull2ErrorMessage;
    }

    public QuizzesFull2Exception(String quizzesFull2ErrorMessage, String value1, int value2) {
        super(String.format(quizzesFull2ErrorMessage, value1, value2));
        logger.info(String.format(quizzesFull2ErrorMessage, value1, value2));
        this.quizzesFull2ErrorMessage = quizzesFull2ErrorMessage;
    }

    public String getErrorMessage() {
        return this.quizzesFull2ErrorMessage;
    }

    public static QuizzesFull2Exception fromRemote(String quizzesFull2ErrorMessage, String formattedMessage) {
        return new QuizzesFull2Exception(quizzesFull2ErrorMessage, formattedMessage, true);
    }
}
