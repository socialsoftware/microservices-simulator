package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessrulesException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(BusinessrulesException.class);
    private final String businessrulesErrorMessage;

    public BusinessrulesException(String businessrulesErrorMessage) {
        super(businessrulesErrorMessage);
        logger.info(businessrulesErrorMessage);
        this.businessrulesErrorMessage = businessrulesErrorMessage;
    }

    public BusinessrulesException(String businessrulesErrorMessage, String value) {
        super(String.format(businessrulesErrorMessage, value));
        logger.info(String.format(businessrulesErrorMessage, value));
        this.businessrulesErrorMessage = businessrulesErrorMessage;
    }

    public BusinessrulesException(String businessrulesErrorMessage, String value1, String value2) {
        super(String.format(businessrulesErrorMessage, value1, value2));
        logger.info(String.format(businessrulesErrorMessage, value1, value2));
        this.businessrulesErrorMessage = businessrulesErrorMessage;
    }

    public BusinessrulesException(String businessrulesErrorMessage, int value) {
        super(String.format(businessrulesErrorMessage, value));
        logger.info(String.format(businessrulesErrorMessage, value));
        this.businessrulesErrorMessage = businessrulesErrorMessage;
    }

    public BusinessrulesException(String businessrulesErrorMessage, int value1, int value2) {
        super(String.format(businessrulesErrorMessage, value1, value2));
        logger.info(String.format(businessrulesErrorMessage, value1, value2));
        this.businessrulesErrorMessage = businessrulesErrorMessage;
    }

    public BusinessrulesException(String businessrulesErrorMessage, String value1, int value2) {
        super(String.format(businessrulesErrorMessage, value1, value2));
        logger.info(String.format(businessrulesErrorMessage, value1, value2));
        this.businessrulesErrorMessage = businessrulesErrorMessage;
    }

    public String getErrorMessage() {
        return this.businessrulesErrorMessage;
    }
}