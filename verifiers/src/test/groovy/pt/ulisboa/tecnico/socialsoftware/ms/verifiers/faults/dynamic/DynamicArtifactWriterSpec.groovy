package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export.DynamicArtifactWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.CurrentPackageFixture
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

class DynamicArtifactWriterSpec extends Specification {
    private final ObjectMapper mapper = new ObjectMapper()

    def 'writes normalized roles into current manifest with the M0 exact field shapes'() {
        given:
        def pkg = CurrentPackageFixture.write([plan()], [])
        def currentWorkload = pkg.workloads[0]
        def saga = currentWorkload.participants()[0].sagaFqn()
        def inputId = currentWorkload.participants()[0].inputVariantId()
        def step = currentWorkload.forwardSchedule()[0].stepId().split('::')[-1]
        def observation = new DynamicObservation('event-1', 'stepStarted', 1L, '2026-08-22T10:00:00Z',
                'test-thread', new DynamicObservation.TestIdentity('execution-1', 'example.OrderSpec', 'createsOrder'),
                saga, 'invocation-1', step, inputId, 'forward', null, null, null, null, null)
        def link = new DynamicAttributionLink('execution-1', saga, 'invocation-1', 'exactInput',
                ['event-1'], inputId, [], null)
        def result = new DynamicEvidenceJoinResult([observation], [link], accounting(), [], 1, 100L)

        when:
        new DynamicArtifactWriter(mapper).write(result, pkg.manifest)
        def read = new ScenarioCatalogPackageReader().readCurrent(pkg.manifest)
        def manifest = mapper.readTree(pkg.manifest.toFile())
        def m0 = mapper.readTree(new File('../issues/2026-08-30-current-only-verifier-artifacts/examples/current-package.fixture.json'))

        then:
        read.dynamicObservations().size() == 1
        read.dynamicAttributionLinks().size() == 1
        manifest.files.has('dynamicObservations')
        manifest.files.has('dynamicAttributionLinks')
        read.accounting().dynamicEvidence.observations.total.asInt() == 1
        read.dynamicObservations()[0].fieldNames().toSet() == m0.artifacts.dynamicObservations[0].fieldNames().toSet()
        read.dynamicAttributionLinks()[0].fieldNames().toSet() == m0.artifacts.dynamicAttributionLinks[0].fieldNames().toSet()
    }

    def 'omits absent optional dynamic files while recording zero accounting'() {
        given:
        def pkg = CurrentPackageFixture.write([plan()], [])

        when:
        def written = new DynamicArtifactWriter().write(new DynamicEvidenceJoinResult([], [], accounting(0), [], 0, 0L), pkg.manifest)
        def manifest = mapper.readTree(pkg.manifest.toFile())
        def read = new ScenarioCatalogPackageReader().readCurrent(pkg.manifest)

        then:
        written.observationPath() == null
        written.attributionPath() == null
        !manifest.files.has('dynamicObservations')
        !manifest.files.has('dynamicAttributionLinks')
        read.accounting().dynamicEvidence.observations.total.asInt() == 0
    }

    def 'production models serialize to every exact M0 dynamic observation and attribution shape'() {
        given:
        def fixture = mapper.readTree(new File('../issues/2026-08-30-current-only-verifier-artifacts/examples/current-package.fixture.json'))
        def observations = [
                new DynamicObservation('observation-1', 'stepStarted', 1L, '2026-08-29T00:00:01Z', 'worker-1',
                        test('execution-1', 'example.OrderSpec', 'case1'), 'example.OrderSaga', 'invocation-1',
                        'reserveOrder#0', 'input-1', 'forward', null, null, null, null, null),
                new DynamicObservation('observation-2', 'stepFinished', 2L, '2026-08-29T00:00:02Z', 'worker-2',
                        test('execution-2', 'example.OrderSpec', 'case2'), 'example.OrderSaga', 'invocation-2',
                        'reserveOrder#0', 'input-1', null, 'completed', null, null, null, null),
                new DynamicObservation('observation-3', 'commandSent', 3L, '2026-08-29T00:00:03Z', 'worker-3',
                        test('execution-3', 'example.PaymentSpec', 'case3'), 'example.PaymentSaga', 'invocation-3',
                        'chargePayment#0', 'input-4', null, null, null,
                        new DynamicObservation.CommandDetail('ObserveOrderCommand', [orderId: 'order-1', userId: 'user-1']), null, null),
                new DynamicObservation('observation-4', 'aggregateAccessed', 4L, '2026-08-29T00:00:04Z', 'worker-4',
                        test('execution-4', 'example.OrderSpec', 'case4'), 'example.OrderSaga', 'invocation-4',
                        'reserveOrder#0', null, null, null, null, null,
                        new DynamicObservation.AccessDetail('order', 'order-1', 'write'), null),
                new DynamicObservation('observation-5', 'invariantViolation', 5L, '2026-08-29T00:00:05Z', 'worker-5',
                        test('execution-5', 'example.PaymentSpec', 'case5'), 'example.PaymentSaga', 'invocation-5',
                        'chargePayment#0', null, null, null, null, null, null,
                        new DynamicObservation.ViolationDetail('aggregateInvariant', 'order is locked'))
        ]
        def links = [
                new DynamicAttributionLink('execution-1', 'example.OrderSaga', 'invocation-1', 'exactInput', ['observation-1'], 'input-1', [], null),
                new DynamicAttributionLink('execution-2', 'example.OrderSaga', 'invocation-2', 'testAndShape', ['observation-2'], 'input-1', [], null),
                new DynamicAttributionLink('execution-3', 'example.PaymentSaga', 'invocation-3', 'shapeOnly', ['observation-3'], 'input-4', [], null),
                new DynamicAttributionLink('execution-4', 'example.OrderSaga', 'invocation-4', 'ambiguous', ['observation-4'], null, ['input-1', 'input-3'], null),
                new DynamicAttributionLink('execution-5', 'example.PaymentSaga', 'invocation-5', 'unmatched', ['observation-5'], null, [], 'no-static-input')
        ]

        expect:
        mapper.writeValueAsString(observations) == mapper.writeValueAsString(fixture.artifacts.dynamicObservations)
        mapper.writeValueAsString(links) == mapper.writeValueAsString(fixture.artifacts.dynamicAttributionLinks)
    }

    @Unroll
    def 'publication failure at #boundary restores every package artifact'() {
        given:
        def pkg = CurrentPackageFixture.write([plan()], [])
        def before = packageBytes(pkg.manifest)
        def current = pkg.workloads[0]
        def result = oneObservationResult(current)
        def writer = new DynamicArtifactWriter(mapper, { hit ->
            if (hit == boundary) throw new IOException("injected ${boundary}")
        })

        when:
        writer.write(result, pkg.manifest)

        then:
        thrown(IOException)
        packageBytes(pkg.manifest) == before
        new ScenarioCatalogPackageReader().readCurrent(pkg.manifest).dynamicObservations().isEmpty()

        where:
        boundary << DynamicArtifactWriter.Boundary.values()
    }

    private static DynamicObservation.TestIdentity test(String execution, String clazz, String method) {
        new DynamicObservation.TestIdentity(execution, clazz, method)
    }

    private static Map packageBytes(java.nio.file.Path manifest) {
        def node = new ObjectMapper().readTree(manifest.toFile())
        def result = [(manifest.fileName.toString()): Files.readAllBytes(manifest).toList()]
        node.files.fields().each { entry ->
            def path = manifest.parent.resolve(entry.value.path.asText())
            result[entry.key] = Files.readAllBytes(path).toList()
        }
        result
    }

    private static DynamicEvidenceJoinResult oneObservationResult(WorkloadPlan workload) {
        def input = workload.participants()[0].inputVariantId()
        def saga = workload.participants()[0].sagaFqn()
        def step = workload.forwardSchedule()[0].stepId().split('::')[-1]
        def observation = new DynamicObservation('event-1', 'stepStarted', 1L, '2026-08-22T10:00:00Z', 'thread',
                test('execution-1', 'example.OrderSpec', 'createsOrder'), saga, 'invocation-1', step, input,
                'forward', null, null, null, null, null)
        def link = new DynamicAttributionLink('execution-1', saga, 'invocation-1', 'exactInput', ['event-1'], input, [], null)
        new DynamicEvidenceJoinResult([observation], [link], accounting(), [], 1, 100L)
    }

    private static Map accounting(int total = 1) {
        [testOutcomes: [passed: total ? 1 : 0, failed: 0],
         observations: [total: total, byKind: [stepStarted: total, stepFinished: 0, commandSent: 0,
                                               aggregateAccessed: 0, invariantViolation: 0], withoutTestContext: 0],
         sagaInvocations: [total: total, byStatus: [exactInput: total, testAndShape: 0, shapeOnly: 0, ambiguous: 0, unmatched: 0]],
         uniqueInputEvidence: [exactInput: total, testAndShape: 0, shapeOnly: 0],
         workloadParticipantEvidence: [allInputsObservedInOneCommonTest: total, allInputsObservedAcrossSeparateTests: 0,
                                       someInputsObserved: 0, noInputsObserved: total ? 0 : 1]]
    }

    private static WorkloadPlan plan() {
        def input = new InputVariant('input-1', 'example.OrderSaga', 'example.OrderSpec', 'createsOrder', 'orderSaga',
                InputResolutionStatus.RESOLVED, 'source', 'provenance', [], [:], [])
        new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, 'workload-1', ScenarioKind.SINGLE_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, [new SagaInstance('participant-1', 'example.OrderSaga', 'input-1', [])],
                [input], [new ScheduledStep('scheduled-1', 'participant-1', 'example.OrderSaga::reserveOrder#0', 0, 'reserveOrder', [])],
                [], [], [], [])
    }
}
