package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicInputMapTest {

    @AfterEach
    void clearHolder() {
        DynamicInputAttributionHolder.clear();
        DynamicEvidenceTestContext.clear();
    }

    @Test
    void resolvesUniqueInputFromTestFunctionalityClassAndStep() {
        DynamicInputAttribution attribution = inputMap(entry("input-1", "example.OrderSaga", "createsOrder", "reserve"))
                .resolve(testIdentity("createsOrder"), "example.OrderSaga", "reserve");

        assertThat(attribution.inputVariantId()).isEqualTo("input-1");
        assertThat(attribution.status()).isEqualTo("MATCHED");
        assertThat(attribution.basis()).isEqualTo("TEST_FUNCTIONALITY_CLASS_STEP");
        assertThat(attribution.candidateInputVariantIds()).containsExactly("input-1");
    }

    @Test
    void doesNotResolveWhenFunctionalityClassFqnDiffers() {
        DynamicInputAttribution attribution = inputMap(entry("input-1", "example.OrderSaga", "createsOrder", "reserve"))
                .resolve(testIdentity("createsOrder"), "example.RenamedOrderSaga", "reserve");

        assertThat(attribution.inputVariantId()).isNull();
        assertThat(attribution.status()).isEqualTo("NO_MATCH");
        assertThat(attribution.candidateInputVariantIds()).isEmpty();
    }

    @Test
    void doesNotResolveFromSimpleFunctionalityNamesOnly() {
        DynamicInputAttribution attribution = inputMap(entry("input-1", "example.OrderSaga", "createsOrder", "reserve"))
                .resolve(testIdentity("createsOrder"), "OrderSaga", "reserve");

        assertThat(attribution.inputVariantId()).isNull();
        assertThat(attribution.status()).isEqualTo("NO_MATCH");
    }

    @Test
    void marksAmbiguousWhenMultipleInputsMatchTheSameRuntimeStep() {
        DynamicInputAttribution attribution = inputMap(
                entry("input-2", "example.OrderSaga", "createsOrder", "reserve"),
                entry("input-1", "example.OrderSaga", "createsOrder", "reserve"))
                .resolve(testIdentity("createsOrder"), "example.OrderSaga", "reserve");

        assertThat(attribution.inputVariantId()).isNull();
        assertThat(attribution.status()).isEqualTo("AMBIGUOUS");
        assertThat(attribution.candidateInputVariantIds()).containsExactly("input-1", "input-2");
    }

    @Test
    void holderUsesCurrentTestIdentityWhenResolvingRuntimeStep() {
        DynamicInputAttributionHolder.setInputMap(
                inputMap(entry("input-1", "example.OrderSaga", "createsOrder", "reserve")),
                true);
        DynamicEvidenceTestContext.set(testIdentity("createsOrder"));

        DynamicInputAttribution attribution = DynamicInputAttributionHolder.resolve("example.OrderSaga", "reserve");

        assertThat(attribution.inputVariantId()).isEqualTo("input-1");
        assertThat(attribution.status()).isEqualTo("MATCHED");
    }

    @Test
    void holderIsDisabledUntilAnActiveMapIsInstalled() {
        DynamicInputAttribution attribution = DynamicInputAttributionHolder.resolve("example.OrderSaga", "reserve");

        assertThat(attribution.inputVariantId()).isNull();
        assertThat(attribution.status()).isNull();
        assertThat(attribution.basis()).isNull();
        assertThat(attribution.candidateInputVariantIds()).isEmpty();
    }

    private DynamicEvidenceTestContext.TestIdentity testIdentity(String methodName) {
        return new DynamicEvidenceTestContext.TestIdentity(
                "example.OrderSpec",
                methodName,
                methodName,
                "example.OrderSpec#" + methodName);
    }

    private DynamicInputMap inputMap(DynamicInputMap.Entry... entries) {
        return new DynamicInputMap(
                "microservices-simulator.dynamic-input-map.v1",
                "2026-05-12T00:00:00Z",
                "example.OrderSpec",
                entries.length,
                List.of(entries));
    }

    private DynamicInputMap.Entry entry(String inputVariantId, String sagaFqn, String sourceMethodName,
                                        String stepNameHint) {
        return new DynamicInputMap.Entry(
                inputVariantId,
                sagaFqn,
                "example.OrderSpec",
                sourceMethodName,
                "order",
                "RESOLVED",
                "CONSTRUCTOR",
                "HIGH",
                List.of(stepNameHint),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of("scenario-1"),
                "new OrderSaga(order)",
                "test constructor",
                List.of());
    }
}
