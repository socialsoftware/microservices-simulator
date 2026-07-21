package pt.ulisboa.tecnico.socialsoftware.ms.exception;

/** A simulator transactional failure that is meaningful as a domain outcome. */
public class SimulatorDomainException extends SimulatorException implements DomainFailure {

    protected SimulatorDomainException(String errorTemplate, String formattedMessage, boolean alreadyFormatted) {
        super(errorTemplate, formattedMessage, alreadyFormatted);
    }

    public SimulatorDomainException(String errorMessage) {
        super(errorMessage);
    }

    public SimulatorDomainException(String errorTemplate, String value) {
        super(errorTemplate, value);
    }

    public SimulatorDomainException(String errorTemplate, String value1, String value2) {
        super(errorTemplate, value1, value2);
    }

    public SimulatorDomainException(String errorTemplate, int value) {
        super(errorTemplate, value);
    }

    public SimulatorDomainException(String errorTemplate, int value1, int value2) {
        super(errorTemplate, value1, value2);
    }

    public SimulatorDomainException(String errorTemplate, int value1, long value2) {
        super(errorTemplate, value1, value2);
    }

    public SimulatorDomainException(String errorTemplate, String value1, int value2) {
        super(errorTemplate, value1, value2);
    }

    public static SimulatorDomainException fromRemote(String errorTemplate, String formattedMessage) {
        return new SimulatorDomainException(errorTemplate, formattedMessage, true);
    }
}
