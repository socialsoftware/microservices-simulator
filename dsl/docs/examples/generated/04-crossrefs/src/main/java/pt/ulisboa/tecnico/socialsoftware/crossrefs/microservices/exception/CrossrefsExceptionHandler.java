package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ControllerAdvice
public class CrossrefsExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(CrossrefsExceptionHandler.class);

    @ExceptionHandler(CrossrefsException.class)
    public ResponseEntity<String> handleCrossrefsException(CrossrefsException ex) {
        logger.warn("Handled CrossrefsException: {}", ex.getErrorMessage());
        return ResponseEntity.status(statusFor(ex.getMessage())).body(ex.getMessage());
    }

    @ExceptionHandler(SimulatorException.class)
    public ResponseEntity<String> handleSimulatorException(SimulatorException ex) {
        logger.warn("Handled SimulatorException: {}", ex.getMessage());
        return ResponseEntity.status(statusFor(ex.getMessage())).body(ex.getMessage());
    }

    private static HttpStatus statusFor(String message) {
        if (message != null && message.contains("does not exist")) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        logger.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
    }
}