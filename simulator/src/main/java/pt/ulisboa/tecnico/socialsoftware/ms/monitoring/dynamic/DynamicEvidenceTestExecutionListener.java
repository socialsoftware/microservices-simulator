package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.Optional;

public final class DynamicEvidenceTestExecutionListener implements TestExecutionListener {
    public static final String TEST_CONTEXT_ENABLED_PROPERTY = "simulator.dynamic-evidence.test-context.enabled";
    public static final String JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY =
            "junit.platform.listeners.autodetection.enabled";
    private static final String DYNAMIC_EVIDENCE_ENABLED_PROPERTY = "simulator.dynamic-evidence.enabled";
    private static final String DYNAMIC_EVIDENCE_OUTPUT_DIR_PROPERTY = "simulator.dynamic-evidence.output-dir";
    private static final String DYNAMIC_EVIDENCE_APPLICATION_NAME_PROPERTY = "simulator.dynamic-evidence.application-name";
    private static final String DYNAMIC_EVIDENCE_INCLUDE_COMMAND_FIELDS_PROPERTY = "simulator.dynamic-evidence.include-command-fields";
    private static final String DYNAMIC_EVIDENCE_MAX_FIELD_DEPTH_PROPERTY = "simulator.dynamic-evidence.max-field-depth";
    private static final String DYNAMIC_EVIDENCE_MAX_FIELD_VALUE_LENGTH_PROPERTY = "simulator.dynamic-evidence.max-field-value-length";

    private DynamicEvidenceRecorder ownedRecorder;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        initializeRecorderFromSystemPropertiesIfNecessary();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        DynamicEvidenceRecorder recorderToClose = ownedRecorder;
        ownedRecorder = null;
        if (recorderToClose == null) {
            return;
        }
        try {
            recorderToClose.close();
        } catch (Exception ignored) {
            // Dynamic evidence must never break the test run.
        } finally {
            if (DynamicEvidenceRecorderHolder.getRecorder() == recorderToClose) {
                DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
            }
        }
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (!isEnabled() || !testIdentifier.isTest()) {
            return;
        }
        DynamicEvidenceTestContext.set(toTestIdentity(testIdentifier));
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (!testIdentifier.isTest()) {
            return;
        }
        DynamicEvidenceTestContext.clear();
    }

    private boolean isEnabled() {
        return Boolean.getBoolean(TEST_CONTEXT_ENABLED_PROPERTY);
    }

    private void initializeRecorderFromSystemPropertiesIfNecessary() {
        if (!Boolean.getBoolean(DYNAMIC_EVIDENCE_ENABLED_PROPERTY)
                || !(DynamicEvidenceRecorderHolder.getRecorder() instanceof DynamicEvidenceNoopRecorder)) {
            return;
        }

        DynamicEvidenceProperties properties = new DynamicEvidenceProperties();
        properties.setEnabled(true);
        properties.setTestContextEnabled(Boolean.getBoolean(TEST_CONTEXT_ENABLED_PROPERTY));
        setStringProperty(DYNAMIC_EVIDENCE_OUTPUT_DIR_PROPERTY, properties::setOutputDir);
        setStringProperty(DYNAMIC_EVIDENCE_APPLICATION_NAME_PROPERTY, properties::setApplicationName);
        setBooleanProperty(DYNAMIC_EVIDENCE_INCLUDE_COMMAND_FIELDS_PROPERTY, properties::setIncludeCommandFields);
        setIntegerProperty(DYNAMIC_EVIDENCE_MAX_FIELD_DEPTH_PROPERTY, properties::setMaxFieldDepth);
        setIntegerProperty(DYNAMIC_EVIDENCE_MAX_FIELD_VALUE_LENGTH_PROPERTY, properties::setMaxFieldValueLength);

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, new ObjectMapper().findAndRegisterModules());
        ownedRecorder = recorder;
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
    }

    private void setStringProperty(String propertyName, java.util.function.Consumer<String> setter) {
        String value = System.getProperty(propertyName);
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }

    private void setBooleanProperty(String propertyName, java.util.function.Consumer<Boolean> setter) {
        String value = System.getProperty(propertyName);
        if (value != null && !value.isBlank()) {
            setter.accept(Boolean.parseBoolean(value));
        }
    }

    private void setIntegerProperty(String propertyName, java.util.function.IntConsumer setter) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            setter.accept(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            // Keep defaults if a non-Spring listener property is malformed.
        }
    }

    private DynamicEvidenceTestContext.TestIdentity toTestIdentity(TestIdentifier testIdentifier) {
        String testClassFqn = null;
        String testMethodName = null;
        Optional<TestSource> source = testIdentifier.getSource();
        if (source.isPresent()) {
            TestSource testSource = source.get();
            if (testSource instanceof MethodSource methodSource) {
                testClassFqn = methodSource.getClassName();
                testMethodName = methodSource.getMethodName();
            } else if (testSource instanceof ClassSource classSource) {
                testClassFqn = classSource.getClassName();
            }
        }

        return new DynamicEvidenceTestContext.TestIdentity(
                testClassFqn,
                testMethodName,
                testIdentifier.getDisplayName(),
                testIdentifier.getUniqueId());
    }
}
