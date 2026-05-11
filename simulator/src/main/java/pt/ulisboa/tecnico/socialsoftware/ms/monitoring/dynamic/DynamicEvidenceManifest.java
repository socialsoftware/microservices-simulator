package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicEvidenceManifest {
    public static final String SCHEMA = "microservices-simulator.dynamic-evidence-manifest.v1";

    private String schema = SCHEMA;
    private String runId;
    private String generatedAt;
    private String startedAt;
    private String finishedAt;
    private String applicationName;
    private boolean enabled;
    private String evidencePath;
    private String manifestPath;
    private Map<String, Object> effectiveConfig = new LinkedHashMap<>();
    private Map<String, Object> counts = new LinkedHashMap<>();
    private List<String> warnings = new ArrayList<>();

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getFinishedAt() { return finishedAt; }
    public void setFinishedAt(String finishedAt) { this.finishedAt = finishedAt; }
    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEvidencePath() { return evidencePath; }
    public void setEvidencePath(String evidencePath) { this.evidencePath = evidencePath; }
    public String getManifestPath() { return manifestPath; }
    public void setManifestPath(String manifestPath) { this.manifestPath = manifestPath; }
    public Map<String, Object> getEffectiveConfig() { return effectiveConfig; }
    public void setEffectiveConfig(Map<String, Object> effectiveConfig) { this.effectiveConfig = effectiveConfig; }
    public Map<String, Object> getCounts() { return counts; }
    public void setCounts(Map<String, Object> counts) { this.counts = counts; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
