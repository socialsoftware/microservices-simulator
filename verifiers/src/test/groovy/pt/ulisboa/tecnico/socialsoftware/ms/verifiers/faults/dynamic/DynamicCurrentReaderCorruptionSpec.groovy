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
import java.security.MessageDigest

class DynamicCurrentReaderCorruptionSpec extends Specification {
    private final ObjectMapper mapper = new ObjectMapper()

    def 'rejects a manifest-linked empty dynamic role'() {
        given:
        def pkg = enrichedPackage()
        def manifest = mapper.readTree(pkg.manifest.toFile())
        def path = pkg.manifest.parent.resolve(manifest.files.dynamicObservations.path.asText())
        Files.write(path, new byte[0])
        refresh(pkg.manifest, 'dynamicObservations', path)

        when:
        new ScenarioCatalogPackageReader().readCurrent(pkg.manifest)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('must not be empty')
    }

    @Unroll
    def 'rejects #kind observation with incompatible #field field'() {
        given:
        def pkg = enrichedPackage()
        mutateObservation(pkg.manifest, index) { it.putObject(field) }

        when:
        new ScenarioCatalogPackageReader().readCurrent(pkg.manifest)

        then:
        thrown(IllegalArgumentException)

        where:
        index | kind                 | field
        0     | 'stepStarted'        | 'command'
        1     | 'stepFinished'       | 'phase'
        2     | 'commandSent'        | 'outcome'
        3     | 'aggregateAccessed'  | 'violation'
        4     | 'invariantViolation' | 'access'
    }

    @Unroll
    def 'rejects #kind observation without required #field field'() {
        given:
        def pkg = enrichedPackage()
        mutateObservation(pkg.manifest, index) { it.remove(field) }

        when:
        new ScenarioCatalogPackageReader().readCurrent(pkg.manifest)

        then:
        thrown(IllegalArgumentException)

        where:
        index | kind                 | field
        0     | 'stepStarted'        | 'phase'
        1     | 'stepFinished'       | 'outcome'
        2     | 'commandSent'        | 'command'
        3     | 'aggregateAccessed'  | 'access'
        4     | 'invariantViolation' | 'violation'
    }

    @Unroll
    def 'rejects unreconciled dynamic accounting #label'() {
        given:
        def pkg = enrichedPackage()
        def manifest = mapper.readTree(pkg.manifest.toFile())
        def accountingPath = pkg.manifest.parent.resolve(manifest.files.accounting.path.asText())
        def accounting = mapper.readTree(accountingPath.toFile())
        def target = path.inject(accounting.dynamicEvidence) { node, segment -> node.path(segment) }
        target.put(field, target.path(field).asInt() + 1)
        mapper.writeValue(accountingPath.toFile(), accounting)
        refresh(pkg.manifest, 'accounting', accountingPath)

        when:
        new ScenarioCatalogPackageReader().readCurrent(pkg.manifest)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('reconcile')

        where:
        label                  | path                                  | field
        'observation total'    | ['observations']                      | 'total'
        'observation kind'     | ['observations', 'byKind']            | 'commandSent'
        'without test context' | ['observations']                      | 'withoutTestContext'
        'attribution status'   | ['sagaInvocations', 'byStatus']       | 'exactInput'
        'strongest input'      | ['uniqueInputEvidence']               | 'exactInput'
        'participant category' | ['workloadParticipantEvidence']       | 'allInputsObservedInOneCommonTest'
    }

    private Map enrichedPackage() {
        def pkg = CurrentPackageFixture.write([plan()], [])
        new DynamicArtifactWriter().write(fullResult(pkg.workloads[0]), pkg.manifest)
        pkg
    }

    private void mutateObservation(java.nio.file.Path manifestPath, int index, Closure mutation) {
        def manifest = mapper.readTree(manifestPath.toFile())
        def path = manifestPath.parent.resolve(manifest.files.dynamicObservations.path.asText())
        def lines = Files.readAllLines(path).collect { mapper.readTree(it) }
        mutation(lines[index])
        Files.writeString(path, lines.collect { mapper.writeValueAsString(it) }.join('\n') + '\n')
        refresh(manifestPath, 'dynamicObservations', path)
    }

    private void refresh(java.nio.file.Path manifestPath, String role, java.nio.file.Path artifactPath) {
        def manifest = mapper.readTree(manifestPath.toFile())
        manifest.files.path(role).put('sha256', sha256(Files.readAllBytes(artifactPath)))
        mapper.writeValue(manifestPath.toFile(), manifest)
    }

    private static String sha256(byte[] bytes) {
        java.util.HexFormat.of().formatHex(MessageDigest.getInstance('SHA-256').digest(bytes))
    }

    private static DynamicEvidenceJoinResult fullResult(WorkloadPlan workload) {
        def saga = workload.participants()[0].sagaFqn()
        def input = workload.participants()[0].inputVariantId()
        def step = workload.forwardSchedule()[0].stepId().split('::')[-1]
        def test = new DynamicObservation.TestIdentity('execution-1', 'example.OrderSpec', 'createsOrder')
        def common = { String id, String kind, long sequence, String phase, String outcome, error, command, access, violation ->
            new DynamicObservation(id, kind, sequence, "2026-08-22T10:00:0${sequence}Z", 'thread', test,
                    saga, 'invocation-1', step, input, phase, outcome, error, command, access, violation)
        }
        def observations = [
                common('o1', 'stepStarted', 1, 'forward', null, null, null, null, null),
                common('o2', 'stepFinished', 2, null, 'failed', new DynamicObservation.ErrorDetail(null, 'boom'), null, null, null),
                common('o3', 'commandSent', 3, null, null, null, new DynamicObservation.CommandDetail('Command', [id: 1]), null, null),
                common('o4', 'aggregateAccessed', 4, null, null, null, null, new DynamicObservation.AccessDetail('order', null, 'read'), null),
                common('o5', 'invariantViolation', 5, null, null, null, null, null, new DynamicObservation.ViolationDetail('aggregateInvariant', null))
        ]
        def link = new DynamicAttributionLink('execution-1', saga, 'invocation-1', 'exactInput', observations*.id(), input, [], null)
        def accounting = [testOutcomes: [passed: 1, failed: 0],
                observations: [total: 5, byKind: [stepStarted: 1, stepFinished: 1, commandSent: 1, aggregateAccessed: 1, invariantViolation: 1], withoutTestContext: 0],
                sagaInvocations: [total: 1, byStatus: [exactInput: 1, testAndShape: 0, shapeOnly: 0, ambiguous: 0, unmatched: 0]],
                uniqueInputEvidence: [exactInput: 1, testAndShape: 0, shapeOnly: 0],
                workloadParticipantEvidence: [allInputsObservedInOneCommonTest: 1, allInputsObservedAcrossSeparateTests: 0, someInputsObserved: 0, noInputsObserved: 0]]
        new DynamicEvidenceJoinResult(observations, [link], accounting, [], 1, 1L)
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
