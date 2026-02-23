package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

@ControllerAdvice
public class AdvancedExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedExceptionHandler.class);

    @ExceptionHandler(AdvancedException.class)
    public ResponseEntity<String> handleAdvancedException(AdvancedException ex) {
        logger.warn("Handled AdvancedException: {}", ex.getErrorMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(SimulatorException.class)
    public ResponseEntity<String> handleSimulatorException(SimulatorException ex) {
        logger.warn("Handled SimulatorException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        logger.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
    }
}