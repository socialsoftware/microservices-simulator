package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EcommerceException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(EcommerceException.class);
    private final String ecommerceErrorMessage;

    public EcommerceException(String ecommerceErrorMessage) {
        super(ecommerceErrorMessage);
        logger.info(ecommerceErrorMessage);
        this.ecommerceErrorMessage = ecommerceErrorMessage;
    }

    public EcommerceException(String ecommerceErrorMessage, String value) {
        super(String.format(ecommerceErrorMessage, value));
        logger.info(String.format(ecommerceErrorMessage, value));
        this.ecommerceErrorMessage = ecommerceErrorMessage;
    }

    public EcommerceException(String ecommerceErrorMessage, String value1, String value2) {
        super(String.format(ecommerceErrorMessage, value1, value2));
        logger.info(String.format(ecommerceErrorMessage, value1, value2));
        this.ecommerceErrorMessage = ecommerceErrorMessage;
    }

    public EcommerceException(String ecommerceErrorMessage, int value) {
        super(String.format(ecommerceErrorMessage, value));
        logger.info(String.format(ecommerceErrorMessage, value));
        this.ecommerceErrorMessage = ecommerceErrorMessage;
    }

    public EcommerceException(String ecommerceErrorMessage, int value1, int value2) {
        super(String.format(ecommerceErrorMessage, value1, value2));
        logger.info(String.format(ecommerceErrorMessage, value1, value2));
        this.ecommerceErrorMessage = ecommerceErrorMessage;
    }

    public EcommerceException(String ecommerceErrorMessage, String value1, int value2) {
        super(String.format(ecommerceErrorMessage, value1, value2));
        logger.info(String.format(ecommerceErrorMessage, value1, value2));
        this.ecommerceErrorMessage = ecommerceErrorMessage;
    }

    public String getErrorMessage() {
        return this.ecommerceErrorMessage;
    }
}