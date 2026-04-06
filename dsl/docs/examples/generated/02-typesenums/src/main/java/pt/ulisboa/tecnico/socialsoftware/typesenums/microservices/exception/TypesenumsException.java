package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TypesenumsException extends SimulatorException {
    private static final Logger logger = LoggerFactory.getLogger(TypesenumsException.class);
    private final String typesenumsErrorMessage;

    public TypesenumsException(String typesenumsErrorMessage) {
        super(typesenumsErrorMessage);
        logger.info(typesenumsErrorMessage);
        this.typesenumsErrorMessage = typesenumsErrorMessage;
    }

    public TypesenumsException(String typesenumsErrorMessage, String value) {
        super(String.format(typesenumsErrorMessage, value));
        logger.info(String.format(typesenumsErrorMessage, value));
        this.typesenumsErrorMessage = typesenumsErrorMessage;
    }

    public TypesenumsException(String typesenumsErrorMessage, String value1, String value2) {
        super(String.format(typesenumsErrorMessage, value1, value2));
        logger.info(String.format(typesenumsErrorMessage, value1, value2));
        this.typesenumsErrorMessage = typesenumsErrorMessage;
    }

    public TypesenumsException(String typesenumsErrorMessage, int value) {
        super(String.format(typesenumsErrorMessage, value));
        logger.info(String.format(typesenumsErrorMessage, value));
        this.typesenumsErrorMessage = typesenumsErrorMessage;
    }

    public TypesenumsException(String typesenumsErrorMessage, int value1, int value2) {
        super(String.format(typesenumsErrorMessage, value1, value2));
        logger.info(String.format(typesenumsErrorMessage, value1, value2));
        this.typesenumsErrorMessage = typesenumsErrorMessage;
    }

    public TypesenumsException(String typesenumsErrorMessage, String value1, int value2) {
        super(String.format(typesenumsErrorMessage, value1, value2));
        logger.info(String.format(typesenumsErrorMessage, value1, value2));
        this.typesenumsErrorMessage = typesenumsErrorMessage;
    }

    public String getErrorMessage() {
        return this.typesenumsErrorMessage;
    }
}