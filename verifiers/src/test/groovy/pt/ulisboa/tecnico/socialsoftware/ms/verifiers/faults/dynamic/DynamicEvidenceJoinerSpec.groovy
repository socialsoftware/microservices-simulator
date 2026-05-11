package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceEvent
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.DynamicEvidenceJoiner
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep
import spock.lang.Specification

import java.nio.file.Path

class DynamicEvidenceJoinerSpec extends Specification {
    private final ObjectMapper mapper = new ObjectMapper()

    def 'assigns exact status when direct input variant correlation is present'() {
        given:
        def plan = plan('scenario-exact', [input('input-1')])
        def events = [event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', stepName: 'reserve', inputVariantId: 'input-1'])]

        when:
        def enriched = new DynamicEvidenceJoiner().join([plan], events).records()[0]

        then:
        enriched.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_EXACT
        enriched.dynamicEvidence().matchedInputVariantIds() == ['input-1']
        enriched.dynamicEvidence().observedSteps()*.stepName() == ['reserve']
    }

    def 'assigns high confidence when one static input matches test identity saga and step'() {
        given:
        def plan = plan('scenario-high', [input('input-1')])
        def events = [
                event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', functionalityInvocationId: 'inv-1', stepName: 'reserve']),
                event('AGGREGATE_ACCESSED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', functionalityInvocationId: 'inv-1', stepName: 'reserve', payload: [aggregateType: 'Order', aggregateId: '42', accessMode: 'READ', sourceMethod: 'aggregateLoadAndRegisterRead']]),
                event('COMMAND_SENT', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', functionalityInvocationId: 'inv-1', stepName: 'reserve', payload: [commandType: 'ReserveOrderCommand', commandFqn: 'com.example.ReserveOrderCommand', serviceName: 'orders', rootAggregateId: '42']])
        ]

        when:
        def enriched = new DynamicEvidenceJoiner().join([plan], events).records()[0]

        then:
        enriched.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE
        enriched.dynamicEvidence().matchedInputVariantIds() == ['input-1']
        enriched.dynamicEvidence().observedAggregateAccesses()[0].aggregateType() == 'Order'
        enriched.dynamicEvidence().observedCommands()[0].commandType() == 'ReserveOrderCommand'
    }

    def 'normalizes expanded static schedule step ids before matching runtime evidence'() {
        given:
        def plan = plan('scenario-normalized', [input('input-1')], 'com.example.OrderSaga', 'com.example.OrderSaga::reserve#0')
        def highConfidenceEvents = [
                event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', functionalityInvocationId: 'inv-1', stepName: 'reserve']),
                event('AGGREGATE_ACCESSED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', functionalityInvocationId: 'inv-1', stepName: 'reserve', payload: [aggregateType: 'Order', aggregateId: '42', accessMode: 'READ', sourceMethod: 'aggregateLoadAndRegisterRead']]),
                event('COMMAND_SENT', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', functionalityInvocationId: 'inv-1', stepName: 'reserve', payload: [commandType: 'ReserveOrderCommand', commandFqn: 'com.example.ReserveOrderCommand', serviceName: 'orders', rootAggregateId: '42']]),
                event('STEP_FINISHED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', functionalityInvocationId: 'inv-1', stepName: 'reserve', payload: [outcome: 'SUCCESS']])
        ]

        when:
        def highConfidence = new DynamicEvidenceJoiner().join([plan], highConfidenceEvents).records()[0]
        def partial = new DynamicEvidenceJoiner().join([plan], [
                event('STEP_STARTED', [functionalityName: 'OrderSaga', stepName: 'reserve'])
        ]).records()[0]

        then:
        highConfidence.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE
        highConfidence.dynamicEvidence().matchedInputVariantIds() == ['input-1']
        highConfidence.dynamicEvidence().observedSteps()[0].stepName() == 'reserve'
        highConfidence.dynamicEvidence().observedAggregateAccesses()[0].aggregateType() == 'Order'
        highConfidence.dynamicEvidence().observedCommands()[0].commandType() == 'ReserveOrderCommand'
        partial.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_PARTIAL
        partial.dynamicEvidence().matchedInputVariantIds().isEmpty()
    }

    def 'does not strip runtime step suffixes when matching dynamic evidence'() {
        given:
        def plan = plan('scenario-runtime-suffix', [input('input-1')], 'com.example.OrderSaga', 'com.example.OrderSaga::reserve')

        when:
        def result = new DynamicEvidenceJoiner().join([plan], [
                event('STEP_STARTED', [functionalityName: 'OrderSaga', stepName: 'reserve#0'])
        ]).records()[0]

        then:
        result.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.UNMATCHED
        result.dynamicEvidence().matchedInputVariantIds().isEmpty()
        result.dynamicEvidence().observedSteps().isEmpty()
        result.dynamicEvidence().observedCommands().isEmpty()
        result.dynamicEvidence().observedAggregateAccesses().isEmpty()
    }

    def 'assigns partial when saga and step match without complete test identity'() {
        expect:
        new DynamicEvidenceJoiner().join([plan('scenario-partial', [input('input-1')])], [
                event('STEP_STARTED', [functionalityName: 'OrderSaga', stepName: 'reserve'])
        ]).records()[0].dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_PARTIAL
    }

    def 'matches lower camel runtime functionality names to FunctionalitySagas static classes'() {
        given:
        def plan = plan(
                'scenario-create-tournament',
                [input('input-1', 'com.example.CreateTournamentFunctionalitySagas')],
                'com.example.CreateTournamentFunctionalitySagas',
                'com.example.CreateTournamentFunctionalitySagas::getCourseExecutionStep')

        when:
        def result = new DynamicEvidenceJoiner().join([plan], [
                event('AGGREGATE_ACCESSED', [functionalityName: 'createTournament', stepName: 'getCourseExecutionStep', payload: [aggregateType: 'SagaExecution', aggregateId: '2', accessMode: 'READ']])
        ]).records()[0]

        then:
        result.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_PARTIAL
        result.dynamicEvidence().observedAggregateAccesses()[0].aggregateType() == 'SagaExecution'
    }

    def 'assigns ambiguous when duplicate saga simple names across catalog do not disambiguate'() {
        given:
        def planA = plan('scenario-a', [input('input-a', 'com.a.OrderSaga')], 'com.a.OrderSaga')
        def planB = plan('scenario-b', [input('input-b', 'com.b.OrderSaga')], 'com.b.OrderSaga')

        when:
        def result = new DynamicEvidenceJoiner().join([planA, planB], [
                event('STEP_STARTED', [functionalityName: 'OrderSaga', stepName: 'reserve'])
        ])

        then:
        result.records()*.dynamicEvidence()*.joinStatus() == [DynamicEvidenceJoinStatus.AMBIGUOUS, DynamicEvidenceJoinStatus.AMBIGUOUS]
        result.records().every { record ->
            record.dynamicEvidence().warnings().any { warning ->
                warning.contains('OrderSaga') && warning.contains('com.a.OrderSaga') && warning.contains('com.b.OrderSaga')
            }
        }
    }

    def 'assigns ambiguous when multiple static inputs match the same dynamic evidence'() {
        expect:
        new DynamicEvidenceJoiner().join([plan('scenario-ambiguous', [input('input-1'), input('input-2')])], [
                event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', stepName: 'reserve'])
        ]).records()[0].dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.AMBIGUOUS
    }

    def 'assigns unmatched when dynamic evidence exists but cannot map to scenario'() {
        expect:
        new DynamicEvidenceJoiner().join([plan('scenario-unmatched', [input('input-1')])], [
                event('STEP_STARTED', [testClassFqn: 'com.example.OtherSpec', testMethodName: 'other', functionalityName: 'PaymentSaga', stepName: 'pay'])
        ]).records()[0].dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.UNMATCHED
    }

    def 'assigns not covered when no dynamic evidence exists'() {
        expect:
        new DynamicEvidenceJoiner().join([plan('scenario-not-covered', [input('input-1')])], []).records()[0].dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.NOT_COVERED
    }

    private ScenarioPlan plan(String id, List<InputVariant> inputs, String sagaFqn = 'com.example.OrderSaga', String stepId = null) {
        def scheduledStepId = stepId == null ? "${sagaFqn}::reserve".toString() : stepId
        new ScenarioPlan(
                ScenarioPlan.SCHEMA_VERSION,
                id,
                ScenarioKind.SINGLE_SAGA,
                [new SagaInstance("${id}-instance".toString(), sagaFqn, inputs[0].deterministicId(), [])],
                inputs,
                [new ScheduledStep("${id}-step".toString(), "${id}-instance".toString(), scheduledStepId, 0, [])],
                null,
                [],
                []
        )
    }

    private static InputVariant input(String id, String sagaFqn = 'com.example.OrderSaga') {
        new InputVariant(id, sagaFqn, 'com.example.OrderSpec', 'creates order', 'orderSaga', InputResolutionStatus.RESOLVED, 'source', 'provenance', [], [:], [])
    }

    private DynamicEvidenceEvent event(String kind, Map values) {
        def json = mapper.valueToTree(([eventId: UUID.randomUUID().toString(), eventKind: kind, sourcePath: 'ignored'] + values))
        DynamicEvidenceEvent.fromJson(json, Path.of('dynamic-evidence/test/dynamic-evidence.jsonl'), 1)
    }
}
