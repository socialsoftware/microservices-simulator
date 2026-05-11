package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.atomic.AtomicBoolean;

public class DynamicEvidenceRecorderRegistration implements DisposableBean, AutoCloseable {
    private final DynamicEvidenceRecorder registeredRecorder;
    private final boolean closeRecorderOnDestroy;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public DynamicEvidenceRecorderRegistration(DynamicEvidenceRecorder recorder) {
        this(recorder, true);
    }

    public DynamicEvidenceRecorderRegistration(DynamicEvidenceRecorder recorder, boolean closeRecorderOnDestroy) {
        this.registeredRecorder = recorder;
        this.closeRecorderOnDestroy = closeRecorderOnDestroy;
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
    }

    @Override
    public void destroy() {
        close();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true) || !closeRecorderOnDestroy) {
            return;
        }

        try {
            registeredRecorder.close();
        } catch (Exception ignored) {
            // Dynamic evidence must never break the test run.
        } finally {
            if (DynamicEvidenceRecorderHolder.getRecorder() == registeredRecorder) {
                DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
            }
        }
    }
}
