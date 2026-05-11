package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicEvidenceTestContextTest {

    @AfterEach
    void clearContextAndProperty() {
        DynamicEvidenceTestContext.clear();
        System.clearProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY);
        System.clearProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY);
    }

    @Test
    void holderStoresAndClearsCurrentTestIdentity() {
        DynamicEvidenceTestContext.set(new DynamicEvidenceTestContext.TestIdentity(
                "example.MySpec", "does something", "does something", "unique-id"));

        assertThat(DynamicEvidenceTestContext.current()).hasValueSatisfying(identity -> {
            assertThat(identity.testClassFqn()).isEqualTo("example.MySpec");
            assertThat(identity.testMethodName()).isEqualTo("does something");
            assertThat(identity.testDisplayName()).isEqualTo("does something");
            assertThat(identity.testUniqueId()).isEqualTo("unique-id");
        });

        DynamicEvidenceTestContext.clear();

        assertThat(DynamicEvidenceTestContext.current()).isEmpty();
    }

    @Test
    void listenerSetsAndClearsContextWhenEnabled() {
        System.setProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, "true");
        DynamicEvidenceTestExecutionListener listener = new DynamicEvidenceTestExecutionListener();
        TestIdentifier identifier = testIdentifier();

        listener.executionStarted(identifier);

        assertThat(DynamicEvidenceTestContext.current()).hasValueSatisfying(identity -> {
            assertThat(identity.testClassFqn()).isEqualTo("example.MySpec");
            assertThat(identity.testMethodName()).isEqualTo("featureMethod");
            assertThat(identity.testDisplayName()).isEqualTo("feature display name");
            assertThat(identity.testUniqueId()).isEqualTo("[engine:spock]/[spec:example.MySpec]/[feature:featureMethod]");
        });

        listener.executionFinished(identifier, TestExecutionResult.successful());

        assertThat(DynamicEvidenceTestContext.current()).isEmpty();
    }

    @Test
    void listenerClearsContextEvenIfPropertyIsDisabledBeforeFinish() {
        System.setProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, "true");
        DynamicEvidenceTestExecutionListener listener = new DynamicEvidenceTestExecutionListener();
        TestIdentifier identifier = testIdentifier();

        listener.executionStarted(identifier);
        assertThat(DynamicEvidenceTestContext.current()).isPresent();

        System.clearProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY);
        listener.executionFinished(identifier, TestExecutionResult.successful());

        assertThat(DynamicEvidenceTestContext.current()).isEmpty();
    }

    @Test
    void listenerIsNoopWhenDisabled() {
        DynamicEvidenceTestExecutionListener listener = new DynamicEvidenceTestExecutionListener();
        TestIdentifier identifier = testIdentifier();

        listener.executionStarted(identifier);

        assertThat(DynamicEvidenceTestContext.current()).isEmpty();
    }

    private TestIdentifier testIdentifier() {
        TestIdentifier identifier = mock(TestIdentifier.class);
        when(identifier.isTest()).thenReturn(true);
        when(identifier.getDisplayName()).thenReturn("feature display name");
        when(identifier.getUniqueId()).thenReturn("[engine:spock]/[spec:example.MySpec]/[feature:featureMethod]");
        when(identifier.getSource()).thenReturn(Optional.of(MethodSource.from("example.MySpec", "featureMethod")));
        return identifier;
    }
}
