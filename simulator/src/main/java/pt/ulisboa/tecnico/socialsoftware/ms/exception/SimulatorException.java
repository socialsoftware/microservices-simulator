package pt.ulisboa.tecnico.socialsoftware.ms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SimulatorException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(SimulatorException.class);
    private final String simulatorErrorMessage;

    public SimulatorException(String simulatorErrorMessage) {
        super(simulatorErrorMessage);
        logger.info(simulatorErrorMessage);
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(String simulatorErrorMessage, String value) {
        super(String.format(simulatorErrorMessage, value));
        logger.info(String.format(simulatorErrorMessage, value));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(String simulatorErrorMessage, String value1, String value2) {
        super(String.format(simulatorErrorMessage, value1, value2));
        logger.info(String.format(simulatorErrorMessage, value1, value2));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(String simulatorErrorMessage, int value) {
        super(String.format(simulatorErrorMessage, value));
        logger.info(String.format(simulatorErrorMessage, value));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(String simulatorErrorMessage, int value1, int value2) {
        super(String.format(simulatorErrorMessage, value1, value2));
        logger.info(String.format(simulatorErrorMessage, value1, value2));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(String simulatorErrorMessage, String value1, int value2) {
        super(String.format(simulatorErrorMessage, value1, value2));
        logger.info(String.format(simulatorErrorMessage, value1, value2));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public String getErrorMessage() {
        return simulatorErrorMessage;
    }
}