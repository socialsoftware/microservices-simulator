package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "simulator.dynamic-evidence")
public class DynamicEvidenceProperties {
    private boolean enabled = false;
    private String outputRoot = "../verifiers/output";
    private String outputDir = "";
    private String applicationName = "application";
    private String evidenceFileName = "dynamic-evidence.jsonl";
    private String manifestFileName = "dynamic-evidence-manifest.json";
    private String inputMapPath = "";
    private boolean includeCommandFields = true;
    @NestedConfigurationProperty
    private final TestContext testContext = new TestContext();
    private int maxFieldDepth = 2;
    private int maxFieldValueLength = 500;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getOutputRoot() { return outputRoot; }
    public void setOutputRoot(String outputRoot) { this.outputRoot = outputRoot; }
    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public String getEvidenceFileName() { return evidenceFileName; }
    public void setEvidenceFileName(String evidenceFileName) { this.evidenceFileName = evidenceFileName; }
    public String getManifestFileName() { return manifestFileName; }
    public void setManifestFileName(String manifestFileName) { this.manifestFileName = manifestFileName; }
    public String getInputMapPath() { return inputMapPath; }
    public void setInputMapPath(String inputMapPath) { this.inputMapPath = inputMapPath; }
    public boolean isIncludeCommandFields() { return includeCommandFields; }
    public void setIncludeCommandFields(boolean includeCommandFields) { this.includeCommandFields = includeCommandFields; }
    public TestContext getTestContext() { return testContext; }
    public void setTestContext(TestContext testContext) {
        if (testContext == null) {
            this.testContext.setEnabled(false);
            return;
        }
        this.testContext.setEnabled(testContext.isEnabled());
    }
    public boolean isTestContextEnabled() { return testContext.isEnabled(); }
    public void setTestContextEnabled(boolean testContextEnabled) { this.testContext.setEnabled(testContextEnabled); }
    public int getMaxFieldDepth() { return maxFieldDepth; }
    public void setMaxFieldDepth(int maxFieldDepth) { this.maxFieldDepth = maxFieldDepth; }
    public int getMaxFieldValueLength() { return maxFieldValueLength; }
    public void setMaxFieldValueLength(int maxFieldValueLength) { this.maxFieldValueLength = maxFieldValueLength; }

    public static class TestContext {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
