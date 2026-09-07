package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

/**
 * Audited application contract for one outer aggregate in one exact command/result pair.
 * Implementations extract only the returned object's identity and revision; they must not
 * query persistence, use the requested ID as a substitute, or infer nested provenance.
 */
public interface ReadResponseAdapter<C extends Command, R> {
    Class<C> commandType();
    Class<R> responseType();
    String contractId();
    String contractVersion();
    String aggregateType();
    String runtimeType();
    Integer aggregateId(R response);
    Long version(R response);
}
