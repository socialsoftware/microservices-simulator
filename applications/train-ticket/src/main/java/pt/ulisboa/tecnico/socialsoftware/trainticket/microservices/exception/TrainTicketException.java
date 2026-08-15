package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TrainTicketException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(TrainTicketException.class);
    private final String trainTicketErrorMessage;

    private TrainTicketException(String trainTicketErrorMessage, String formattedMessage, boolean alreadyFormatted) {
        super(trainTicketErrorMessage, formattedMessage, true);
        logger.info(formattedMessage);
        this.trainTicketErrorMessage = trainTicketErrorMessage;
    }

    public TrainTicketException(String trainTicketErrorMessage) {
        super(trainTicketErrorMessage);
        logger.info(trainTicketErrorMessage);
        this.trainTicketErrorMessage = trainTicketErrorMessage;
    }

    public TrainTicketException(String trainTicketErrorMessage, String value) {
        super(String.format(trainTicketErrorMessage, value));
        logger.info(String.format(trainTicketErrorMessage, value));
        this.trainTicketErrorMessage = trainTicketErrorMessage;
    }

    public TrainTicketException(String trainTicketErrorMessage, String value1, String value2) {
        super(String.format(trainTicketErrorMessage, value1, value2));
        logger.info(String.format(trainTicketErrorMessage, value1, value2));
        this.trainTicketErrorMessage = trainTicketErrorMessage;
    }

    public TrainTicketException(String trainTicketErrorMessage, int value) {
        super(String.format(trainTicketErrorMessage, value));
        logger.info(String.format(trainTicketErrorMessage, value));
        this.trainTicketErrorMessage = trainTicketErrorMessage;
    }

    public TrainTicketException(String trainTicketErrorMessage, int value1, int value2) {
        super(String.format(trainTicketErrorMessage, value1, value2));
        logger.info(String.format(trainTicketErrorMessage, value1, value2));
        this.trainTicketErrorMessage = trainTicketErrorMessage;
    }

    public TrainTicketException(String trainTicketErrorMessage, String value1, int value2) {
        super(String.format(trainTicketErrorMessage, value1, value2));
        logger.info(String.format(trainTicketErrorMessage, value1, value2));
        this.trainTicketErrorMessage = trainTicketErrorMessage;
    }

    public String getErrorMessage() {
        return this.trainTicketErrorMessage;
    }

    public static TrainTicketException fromRemote(String trainTicketErrorMessage, String formattedMessage) {
        return new TrainTicketException(trainTicketErrorMessage, formattedMessage, true);
    }
}
