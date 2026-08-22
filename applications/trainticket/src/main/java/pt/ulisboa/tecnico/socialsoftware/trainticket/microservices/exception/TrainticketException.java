package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TrainticketException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(TrainticketException.class);
    private final String trainticketErrorMessage;

    private TrainticketException(String trainticketErrorMessage, String formattedMessage, boolean alreadyFormatted) {
        super(trainticketErrorMessage, formattedMessage, true);
        logger.info(formattedMessage);
        this.trainticketErrorMessage = trainticketErrorMessage;
    }

    public TrainticketException(String trainticketErrorMessage) {
        super(trainticketErrorMessage);
        logger.info(trainticketErrorMessage);
        this.trainticketErrorMessage = trainticketErrorMessage;
    }

    public TrainticketException(String trainticketErrorMessage, String value) {
        super(String.format(trainticketErrorMessage, value));
        logger.info(String.format(trainticketErrorMessage, value));
        this.trainticketErrorMessage = trainticketErrorMessage;
    }

    public TrainticketException(String trainticketErrorMessage, String value1, String value2) {
        super(String.format(trainticketErrorMessage, value1, value2));
        logger.info(String.format(trainticketErrorMessage, value1, value2));
        this.trainticketErrorMessage = trainticketErrorMessage;
    }

    public TrainticketException(String trainticketErrorMessage, int value) {
        super(String.format(trainticketErrorMessage, value));
        logger.info(String.format(trainticketErrorMessage, value));
        this.trainticketErrorMessage = trainticketErrorMessage;
    }

    public TrainticketException(String trainticketErrorMessage, int value1, int value2) {
        super(String.format(trainticketErrorMessage, value1, value2));
        logger.info(String.format(trainticketErrorMessage, value1, value2));
        this.trainticketErrorMessage = trainticketErrorMessage;
    }

    public TrainticketException(String trainticketErrorMessage, String value1, int value2) {
        super(String.format(trainticketErrorMessage, value1, value2));
        logger.info(String.format(trainticketErrorMessage, value1, value2));
        this.trainticketErrorMessage = trainticketErrorMessage;
    }

    public String getErrorMessage() {
        return this.trainticketErrorMessage;
    }

    public static TrainticketException fromRemote(String trainticketErrorMessage, String formattedMessage) {
        return new TrainticketException(trainticketErrorMessage, formattedMessage, true);
    }
}
