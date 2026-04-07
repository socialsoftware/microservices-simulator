package pt.ulisboa.tecnico.socialsoftware.ms.monitoring;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TraceService {
    @Value("${spring.application.name:my-app}")
    private String serviceName;

    @PostConstruct
    public void init() {
        TraceManager.init(serviceName);
    }

    public void createMasterRoot() {
        TraceManager.getInstance().createMasterRoot();
    }

    public String getMasterRootTraceId() {
        return TraceManager.getInstance().getMasterRootTraceId();
    }

    public String getMasterRootSpanId() {
        return TraceManager.getInstance().getMasterRootSpanId();
    }

    public void endMasterRootSpan() {
        TraceManager.getInstance().endMasterRootSpan();
    }

    public void startRootSpan() {
        TraceManager.getInstance().startRootSpan();
    }

    public void startRootSpan(String traceId, String spanId) {
        TraceManager.getInstance().startRootSpan(traceId, spanId);
    }

    public String getRootTraceId() {
        return TraceManager.getInstance().getRootTraceId();
    }

    public String getRootSpanId() {
        return TraceManager.getInstance().getRootSpanId();
    }

    public void endRootSpan() {
        TraceManager.getInstance().endRootSpan();
    }

    public void spanFlush() {
        TraceManager.getInstance().forceFlush();
    }
}