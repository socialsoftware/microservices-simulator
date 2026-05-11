package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

public class DynamicEvidenceNoopRecorder implements DynamicEvidenceRecorder {
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void record(DynamicEvidenceEvent event) {
        // Dynamic evidence is disabled.
    }

    @Override
    public void close() {
        // Dynamic evidence is disabled.
    }
}
