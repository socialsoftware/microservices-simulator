package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception;

import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

/** Indicates invalid Quizzes runtime configuration, not a domain outcome. */
public class QuizzesConfigurationException extends SimulatorException {
    public QuizzesConfigurationException(String message) {
        super(message);
    }
}
