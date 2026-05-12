package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DynamicEvidenceEvent {
    public static final String SCHEMA = "microservices-simulator.dynamic-evidence.v1";

    private String eventId = UUID.randomUUID().toString();
    private String eventKind;
    private String functionalityName;
    private String functionalityClassFqn;
    private String functionalityClassSimpleName;
    private String functionalityInvocationId;
    private String stepName;
    private String testClassFqn;
    private String testMethodName;
    private String testDisplayName;
    private String testUniqueId;
    private Long unitOfWorkVersion;
    private Map<String, Object> payload = new LinkedHashMap<>();

    public static DynamicEvidenceEvent of(String eventKind, String functionalityName, String functionalityInvocationId,
                                          String stepName, Long unitOfWorkVersion, Map<String, Object> payload) {
        return of(eventKind, functionalityName, null, null, functionalityInvocationId, stepName, unitOfWorkVersion,
                payload);
    }

    public static DynamicEvidenceEvent of(String eventKind, String functionalityName, String functionalityClassFqn,
                                          String functionalityClassSimpleName, String functionalityInvocationId,
                                          String stepName, Long unitOfWorkVersion, Map<String, Object> payload) {
        DynamicEvidenceEvent event = new DynamicEvidenceEvent();
        event.setEventKind(eventKind);
        event.setFunctionalityName(functionalityName);
        event.setFunctionalityClassFqn(functionalityClassFqn);
        event.setFunctionalityClassSimpleName(functionalityClassSimpleName);
        event.setFunctionalityInvocationId(functionalityInvocationId);
        event.setStepName(stepName);
        event.setUnitOfWorkVersion(unitOfWorkVersion);
        event.setPayload(payload);
        DynamicEvidenceTestContext.current().ifPresent(identity -> {
            event.setTestClassFqn(identity.testClassFqn());
            event.setTestMethodName(identity.testMethodName());
            event.setTestDisplayName(identity.testDisplayName());
            event.setTestUniqueId(identity.testUniqueId());
        });
        return event;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventKind() { return eventKind; }
    public void setEventKind(String eventKind) { this.eventKind = eventKind; }
    public String getFunctionalityName() { return functionalityName; }
    public void setFunctionalityName(String functionalityName) { this.functionalityName = functionalityName; }
    public String getFunctionalityClassFqn() { return functionalityClassFqn; }
    public void setFunctionalityClassFqn(String functionalityClassFqn) { this.functionalityClassFqn = functionalityClassFqn; }
    public String getFunctionalityClassSimpleName() { return functionalityClassSimpleName; }
    public void setFunctionalityClassSimpleName(String functionalityClassSimpleName) { this.functionalityClassSimpleName = functionalityClassSimpleName; }
    public String getFunctionalityInvocationId() { return functionalityInvocationId; }
    public void setFunctionalityInvocationId(String functionalityInvocationId) { this.functionalityInvocationId = functionalityInvocationId; }
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public String getTestClassFqn() { return testClassFqn; }
    public void setTestClassFqn(String testClassFqn) { this.testClassFqn = testClassFqn; }
    public String getTestMethodName() { return testMethodName; }
    public void setTestMethodName(String testMethodName) { this.testMethodName = testMethodName; }
    public String getTestDisplayName() { return testDisplayName; }
    public void setTestDisplayName(String testDisplayName) { this.testDisplayName = testDisplayName; }
    public String getTestUniqueId() { return testUniqueId; }
    public void setTestUniqueId(String testUniqueId) { this.testUniqueId = testUniqueId; }
    public Long getUnitOfWorkVersion() { return unitOfWorkVersion; }
    public void setUnitOfWorkVersion(Long unitOfWorkVersion) { this.unitOfWorkVersion = unitOfWorkVersion; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload); }
}
