package pt.ulisboa.tecnico.socialsoftware.ms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SimulatorException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(SimulatorException.class);
    private final SimulatorErrorMessage simulatorErrorMessage;

    public SimulatorException(SimulatorErrorMessage simulatorErrorMessage) {
        super(simulatorErrorMessage.label);
        logger.info(simulatorErrorMessage.label);
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(SimulatorErrorMessage simulatorErrorMessage, String value) {
        super(String.format(simulatorErrorMessage.label, value));
        logger.info(String.format(simulatorErrorMessage.label, value));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(SimulatorErrorMessage simulatorErrorMessage, String value1, String value2) {
        super(String.format(simulatorErrorMessage.label, value1, value2));
        logger.info(String.format(simulatorErrorMessage.label, value1, value2));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(SimulatorErrorMessage simulatorErrorMessage, int value) {
        super(String.format(simulatorErrorMessage.label, value));
        logger.info(String.format(simulatorErrorMessage.label, value));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(SimulatorErrorMessage simulatorErrorMessage, int value1, int value2) {
        super(String.format(simulatorErrorMessage.label, value1, value2));
        logger.info(String.format(simulatorErrorMessage.label, value1, value2));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorException(SimulatorErrorMessage simulatorErrorMessage, String value1, int value2) {
        super(String.format(simulatorErrorMessage.label, value1, value2));
        logger.info(String.format(simulatorErrorMessage.label, value1, value2));
        this.simulatorErrorMessage = simulatorErrorMessage;
    }

    public SimulatorErrorMessage getErrorMessage() {
        return simulatorErrorMessage;
    }
}