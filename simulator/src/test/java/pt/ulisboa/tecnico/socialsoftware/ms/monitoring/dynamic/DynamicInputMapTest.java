package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicInputMapTest {

    @TempDir
    Path tempDir;

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
    void resolvesHelperInputForAnyDeclaredOwnerOnly() {
        DynamicInputMap.Entry helperEntry = entry(
                "input-helper",
                "example.OrderSaga",
                "createHelperSaga",
                "reserve",
                List.of(
                        new DynamicInputMap.InputOwner("example.OrderSpec", "createsOrder"),
                        new DynamicInputMap.InputOwner("example.OrderSpec", "retriesOrder")));

        assertThat(inputMap(helperEntry).resolve(testIdentity("createsOrder"), "example.OrderSaga", "reserve").inputVariantId())
                .isEqualTo("input-helper");
        assertThat(inputMap(helperEntry).resolve(testIdentity("retriesOrder"), "example.OrderSaga", "reserve").inputVariantId())
                .isEqualTo("input-helper");
        assertThat(inputMap(helperEntry).resolve(testIdentity("undeclared"), "example.OrderSaga", "reserve").status())
                .isEqualTo("NO_MATCH");
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
    void resolvesInputsOwnedByDifferentTestClassesFromTheSameMap() {
        DynamicInputMap map = inputMap(
                entry("input-order", "example.OrderSpec", "example.OrderSaga", "createsOrder", "reserve"),
                entry("input-payment", "example.PaymentSpec", "example.PaymentSaga", "createsPayment", "charge"));

        DynamicInputAttribution order = map.resolve(testIdentity("example.OrderSpec", "createsOrder"), "example.OrderSaga", "reserve");
        DynamicInputAttribution payment = map.resolve(testIdentity("example.PaymentSpec", "createsPayment"), "example.PaymentSaga", "charge");

        assertThat(order.status()).isEqualTo("MATCHED");
        assertThat(order.inputVariantId()).isEqualTo("input-order");
        assertThat(payment.status()).isEqualTo("MATCHED");
        assertThat(payment.inputVariantId()).isEqualTo("input-payment");
    }

    @Test
    void rejectsNonOwnerWhenFunctionalityClassAndStepMatch() {
        DynamicInputMap map = inputMap(entry("input-1", "example.OrderSpec", "example.OrderSaga", "createsOrder", "reserve"));

        DynamicInputAttribution attribution = map.resolve(testIdentity("example.OtherSpec", "createsOrder"), "example.OrderSaga", "reserve");

        assertThat(attribution.status()).isEqualTo("NO_MATCH");
        assertThat(attribution.inputVariantId()).isNull();
    }

    @Test
    void loadsRunScopedInputMapWithoutRootTestClassGate() throws Exception {
        Path path = tempDir.resolve("dynamic-input-map.json");
        Files.writeString(path, """
                {
                  "schemaVersion": "microservices-simulator.dynamic-input-map.v1",
                  "generatedAt": "2026-05-12T00:00:00Z",
                  "selectedTestClassFqns": ["example.OrderSpec", "example.PaymentSpec"],
                  "inputCount": 2,
                  "inputs": [
                    {
                      "inputVariantId": "input-order",
                      "sagaFqn": "example.OrderSaga",
                      "sourceClassFqn": "example.OrderSpec",
                      "sourceMethodName": "createsOrder",
                      "owners": [],
                      "stepNameHints": ["reserve"]
                    },
                    {
                      "inputVariantId": "input-payment",
                      "sagaFqn": "example.PaymentSaga",
                      "sourceClassFqn": "example.PaymentSpec",
                      "sourceMethodName": "createsPayment",
                      "owners": [],
                      "stepNameHints": ["charge"]
                    }
                  ]
                }
                """);

        DynamicInputMapLoader.LoadResult result = new DynamicInputMapLoader(new ObjectMapper()).load(path.toString());

        assertThat(result.active()).isTrue();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.inputMap().selectedTestClassFqns()).containsExactly("example.OrderSpec", "example.PaymentSpec");
        assertThat(result.inputMap().resolve(testIdentity("example.OrderSpec", "createsOrder"), "example.OrderSaga", "reserve").inputVariantId())
                .isEqualTo("input-order");
        assertThat(result.inputMap().resolve(testIdentity("example.PaymentSpec", "createsPayment"), "example.PaymentSaga", "charge").inputVariantId())
                .isEqualTo("input-payment");
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
        return testIdentity("example.OrderSpec", methodName);
    }

    private DynamicEvidenceTestContext.TestIdentity testIdentity(String testClassFqn, String methodName) {
        return new DynamicEvidenceTestContext.TestIdentity(
                testClassFqn,
                methodName,
                methodName,
                testClassFqn + "#" + methodName);
    }

    private DynamicInputMap inputMap(DynamicInputMap.Entry... entries) {
        return new DynamicInputMap(
                "microservices-simulator.dynamic-input-map.v1",
                "2026-05-12T00:00:00Z",
                List.of("example.OrderSpec"),
                entries.length,
                List.of(entries));
    }

    private DynamicInputMap.Entry entry(String inputVariantId, String sagaFqn, String sourceMethodName,
                                        String stepNameHint) {
        return entry(inputVariantId, "example.OrderSpec", sagaFqn, sourceMethodName, stepNameHint);
    }

    private DynamicInputMap.Entry entry(String inputVariantId, String sourceClassFqn, String sagaFqn,
                                        String sourceMethodName, String stepNameHint) {
        return new DynamicInputMap.Entry(
                inputVariantId,
                sagaFqn,
                sourceClassFqn,
                sourceMethodName,
                "order",
                List.of(),
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

    private DynamicInputMap.Entry entry(String inputVariantId, String sagaFqn, String sourceMethodName,
                                        String stepNameHint, List<DynamicInputMap.InputOwner> owners) {
        return new DynamicInputMap.Entry(
                inputVariantId,
                sagaFqn,
                "example.OrderSpec",
                sourceMethodName,
                "order",
                owners,
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
