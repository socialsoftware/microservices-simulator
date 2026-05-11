package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

public interface DynamicEvidenceRecorder extends AutoCloseable {
    default boolean isEnabled() {
        return true;
    }

    void record(DynamicEvidenceEvent event);

    @Override
    void close();
}
