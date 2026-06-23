package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceEvent
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.DynamicEvidenceJoiner
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputOwner
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep
import spock.lang.Specification
import spock.lang.Timeout

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

    def 'functionality class fqn disambiguates duplicate simple saga names'() {
        given:
        def executionFqn = 'com.example.execution.RemoveCourseExecutionFunctionalitySagas'
        def tournamentFqn = 'com.example.tournament.RemoveCourseExecutionFunctionalitySagas'
        def executionPlan = plan('scenario-execution', [input('input-execution', executionFqn, [new InputOwner('com.example.RemoveSpec', 'removes tournament course execution')])], executionFqn)
        def tournamentPlan = plan('scenario-tournament', [input('input-tournament', tournamentFqn, [new InputOwner('com.example.RemoveSpec', 'removes tournament course execution')])], tournamentFqn)

        when:
        def result = new DynamicEvidenceJoiner().join([executionPlan, tournamentPlan], [
                event('STEP_STARTED', [
                        testClassFqn                 : 'com.example.RemoveSpec',
                        testMethodName               : 'removes tournament course execution',
                        functionalityName            : 'RemoveCourseExecutionFunctionalitySagas',
                        functionalityClassFqn        : tournamentFqn,
                        functionalityClassSimpleName : 'RemoveCourseExecutionFunctionalitySagas',
                        stepName                     : 'reserve'])
        ])

        then:
        result.records()*.scenarioPlanId() == ['scenario-execution', 'scenario-tournament']
        result.records()*.dynamicEvidence()*.joinStatus() == [DynamicEvidenceJoinStatus.UNMATCHED, DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE]
        result.records()[1].dynamicEvidence().observedSteps()[0].sagaFqn() == tournamentFqn
    }

    def 'simple-name-only duplicate saga evidence remains ambiguous'() {
        given:
        def executionFqn = 'com.example.execution.RemoveCourseExecutionFunctionalitySagas'
        def tournamentFqn = 'com.example.tournament.RemoveCourseExecutionFunctionalitySagas'
        def executionPlan = plan('scenario-execution', [input('input-execution', executionFqn)], executionFqn)
        def tournamentPlan = plan('scenario-tournament', [input('input-tournament', tournamentFqn)], tournamentFqn)

        when:
        def result = new DynamicEvidenceJoiner().join([executionPlan, tournamentPlan], [
                event('STEP_STARTED', [
                        testClassFqn    : 'com.example.RemoveSpec',
                        testMethodName  : 'removes tournament course execution',
                        functionalityName: 'RemoveCourseExecutionFunctionalitySagas',
                        stepName         : 'reserve'])
        ])

        then:
        result.records()*.dynamicEvidence()*.joinStatus() == [DynamicEvidenceJoinStatus.AMBIGUOUS, DynamicEvidenceJoinStatus.AMBIGUOUS]
        result.records().every { record ->
            record.dynamicEvidence().warnings().any { warning ->
                warning.contains('RemoveCourseExecutionFunctionalitySagas') && warning.contains(executionFqn) && warning.contains(tournamentFqn)
            }
        }
    }

    def 'unambiguous legacy simple-name evidence still matches'() {
        given:
        def uniqueFqn = 'com.example.UniqueFunctionalitySagas'
        def uniquePlan = plan('scenario-unique', [input('input-unique', uniqueFqn, [new InputOwner('com.example.UniqueSpec', 'runs unique')])], uniqueFqn)

        when:
        def result = new DynamicEvidenceJoiner().join([uniquePlan], [
                event('STEP_STARTED', [
                        testClassFqn    : 'com.example.UniqueSpec',
                        testMethodName  : 'runs unique',
                        functionalityName: 'UniqueFunctionalitySagas',
                        stepName         : 'reserve'])
        ])

        then:
        result.records()[0].dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE
    }

    def 'assigns ambiguous when multiple static inputs match the same dynamic evidence'() {
        expect:
        new DynamicEvidenceJoiner().join([plan('scenario-ambiguous', [input('input-1'), input('input-2')])], [
                event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', stepName: 'reserve'])
        ]).records()[0].dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.AMBIGUOUS
    }

    def 'direct input variant id resolves otherwise ambiguous static identity evidence'() {
        given:
        def ambiguousPlan = plan('scenario-ambiguous-direct', [input('input-1'), input('input-2')])

        when:
        def result = new DynamicEvidenceJoiner().join([ambiguousPlan], [
                event('STEP_STARTED', [
                        testClassFqn  : 'com.example.OrderSpec',
                        testMethodName: 'creates order',
                        functionalityName: 'OrderSaga',
                        stepName      : 'reserve',
                        inputVariantId: 'input-2'])
        ]).records()[0]

        then:
        result.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_EXACT
        result.dynamicEvidence().matchedInputVariantIds() == ['input-2']
        result.dynamicEvidence().warnings().isEmpty()
    }

    def 'foreign direct input variant id does not promote neighboring plan through fallback'() {
        given:
        def planA = plan('scenario-a', [input('input-a')])
        def planB = plan('scenario-b', [input('input-b')])

        when:
        def result = new DynamicEvidenceJoiner().join([planA, planB], [
                event('STEP_STARTED', [
                        testClassFqn  : 'com.example.OrderSpec',
                        testMethodName: 'creates order',
                        functionalityName: 'OrderSaga',
                        stepName      : 'reserve',
                        inputVariantId: 'input-a'])
        ])

        then:
        result.records()*.dynamicEvidence()*.joinStatus() == [DynamicEvidenceJoinStatus.MATCHED_EXACT, DynamicEvidenceJoinStatus.UNMATCHED]
    }

    def 'ambiguity is limited to plan inputs that participate in the ambiguous identity set'() {
        given:
        def ambiguousPlan = plan('scenario-ambiguous', [input('input-1'), input('input-2')])
        def neighboringPlan = plan('scenario-neighbor', [input('input-3')])

        when:
        def result = new DynamicEvidenceJoiner().join([ambiguousPlan, neighboringPlan], [
                event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', functionalityName: 'OrderSaga', stepName: 'reserve'])
        ])

        then:
        result.records()*.dynamicEvidence()*.joinStatus() == [DynamicEvidenceJoinStatus.AMBIGUOUS, DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE]
        result.records()[0].dynamicEvidence().matchedInputVariantIds() == ['input-1', 'input-2']
        result.records()[1].dynamicEvidence().matchedInputVariantIds() == ['input-3']
    }

    def 'fallback uses declared owners before class and method provenance'() {
        given:
        def helperInput = input('input-helper', 'com.example.OrderSaga', [
                new InputOwner('com.example.OrderSpec', 'owner one'),
                new InputOwner('com.example.OrderSpec', 'owner two')])
        def plan = plan('scenario-owned-helper', [helperInput])

        expect:
        new DynamicEvidenceJoiner().join([plan], [
                event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: methodName, functionalityName: 'OrderSaga', stepName: 'reserve'])
        ]).records()[0].dynamicEvidence().joinStatus() == status

        where:
        methodName      || status
        'owner one'     || DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE
        'owner two'     || DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE
        'creates order' || DynamicEvidenceJoinStatus.UNMATCHED
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

    @Timeout(10)
    def 'joins generated full baseline sized fixture through public outputs quickly'() {
        given:
        def plans = (1..66).collect { index ->
            plan("scenario-${index}".toString(), [input("input-${index}".toString())])
        }
        def events = (1..20_000).collect { index ->
            event(index % 3 == 0 ? 'COMMAND_SENT' : 'STEP_STARTED', [
                    testClassFqn    : 'com.example.OrderSpec',
                    testMethodName  : 'creates order',
                    functionalityName: 'OrderSaga',
                    stepName         : 'reserve',
                    inputVariantId   : 'input-1',
                    payload          : [commandType: 'ReserveOrderCommand', commandFqn: 'com.example.ReserveOrderCommand', serviceName: 'orders', rootAggregateId: '42']
            ], index)
        }

        when:
        def started = System.nanoTime()
        def result = new DynamicEvidenceJoiner().join(plans, events, 1, [])
        def elapsedMillis = (System.nanoTime() - started) / 1_000_000L

        then:
        result.dynamicEventsRead() == 20_000
        result.evidenceFilesRead() == 1
        result.eventsMissingTestContext() == 0
        result.warnings().isEmpty()
        result.records().count { it.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_EXACT } == 1
        result.records().count { it.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.UNMATCHED } == 65
        def exact = result.records().find { it.scenarioPlanId() == 'scenario-1' }
        exact.dynamicEvidence().matchedInputVariantIds() == ['input-1']
        exact.dynamicEvidence().observedSteps()[0].stepName() == 'reserve'
        exact.dynamicEvidence().observedSteps()[0].eventKinds() == ['STEP_STARTED', 'COMMAND_SENT']
        exact.dynamicEvidence().observedCommands()[0].commandType() == 'ReserveOrderCommand'
        elapsedMillis < 10_000L
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

    private static InputVariant input(String id, String sagaFqn, List<InputOwner> owners) {
        new InputVariant(id, sagaFqn, 'com.example.OrderSpec', 'createHelperSaga', 'orderSaga', InputResolutionStatus.RESOLVED,
                pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode.UNKNOWN,
                pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence.UNKNOWN,
                [], 'source', 'provenance', owners, [], [:], [])
    }

    private DynamicEvidenceEvent event(String kind, Map values, int lineNumber = 1) {
        def event = [eventId: UUID.randomUUID().toString(), eventKind: kind] + values
        new DynamicEvidenceEvent(
                event.eventId as String,
                event.eventKind as String,
                event.testClassFqn as String,
                event.testMethodName as String,
                event.testDisplayName as String,
                event.testUniqueId as String,
                event.inputVariantId as String,
                event.functionalityName as String,
                event.functionalityClassFqn as String,
                event.functionalityClassSimpleName as String,
                event.functionalityInvocationId as String,
                event.stepName as String,
                event.payload == null ? [:] : event.payload as Map<String, Object>,
                Path.of('dynamic-evidence/test/dynamic-evidence.jsonl'),
                lineNumber)
    }
}
