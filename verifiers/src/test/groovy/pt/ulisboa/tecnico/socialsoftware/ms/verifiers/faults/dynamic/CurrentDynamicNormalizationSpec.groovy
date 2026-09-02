package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceEvent
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification

import java.nio.file.Path

class CurrentDynamicNormalizationSpec extends Specification {
    def 'normalizes each observation kind once and exact input wins attribution'() {
        given:
        def plan = plan('workload-1', [input('input-1')])
        def base = [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order',
                    testUniqueId: 'execution-1', functionalityName: 'OrderSaga',
                    functionalityInvocationId: 'invocation-1', stepName: 'reserve', inputVariantId: 'input-1']
        def events = [
                event('STEP_STARTED', base + [payload: [stepPhase: 'FORWARD']], 1),
                event('STEP_FINISHED', base + [payload: [outcome: 'SUCCESS']], 2),
                event('COMMAND_SENT', base + [payload: [commandType: 'ReserveOrderCommand', fields: [orderId: 'o-1']]], 3),
                event('AGGREGATE_ACCESSED', base + [payload: [aggregateType: 'Order', aggregateId: 'o-1', accessMode: 'WRITE']], 4),
                event('INVARIANT_VIOLATION', base + [payload: [exceptionMessage: 'capacity']], 5)
        ]

        when:
        def result = new DynamicEvidenceJoiner().join([plan], events, 1, [], 500L,
                ['com.example.OrderSpec'] as Set, ['com.example.OrderSpec': 'PASSED'])

        then:
        result.observations()*.kind() == ['stepStarted', 'stepFinished', 'commandSent', 'aggregateAccessed', 'invariantViolation']
        result.observations()*.id().toSet().size() == 5
        result.observations()[0].step() == 'reserve#0'
        result.observations()[2].command().fields() == [orderId: 'o-1']
        result.attributions().size() == 1
        result.attributions()[0].status() == 'exactInput'
        result.attributions()[0].input() == 'input-1'
        result.attributions()[0].observationIds().size() == 5
        result.dynamicAccounting().observations.total == 5
        result.dynamicAccounting().observations.byKind.values().every { it == 1 }
        result.dynamicAccounting().sagaInvocations.byStatus.exactInput == 1
        result.dynamicAccounting().testOutcomes == [passed: 1, failed: 0]
    }

    def 'keeps ambiguous and unmatched evidence explicit and exposes input-map mismatch'() {
        given:
        def ambiguousPlan = plan('workload-a', [input('input-a'), input('input-b')])
        def unmatched = plan('workload-u', [input('input-u', 'com.example.OtherSaga')], 'com.example.MissingSaga')
        def events = [
                event('STEP_STARTED', [testUniqueId: 'test-a', functionalityName: 'OrderSaga', functionalityInvocationId: 'inv-a', stepName: 'reserve', payload: [stepPhase: 'FORWARD']], 1),
                event('STEP_STARTED', [testUniqueId: 'test-u', functionalityName: 'MissingSaga', functionalityInvocationId: 'inv-u', stepName: 'missing', payload: [stepPhase: 'FORWARD']], 2)
        ]

        when:
        def result = new DynamicEvidenceJoiner().join([ambiguousPlan, unmatched], events)

        then:
        result.attributions()*.status().toSet() == ['ambiguous', 'unmatched'] as Set
        result.attributions().find { it.status() == 'ambiguous' }.candidateInputs() == ['input-a', 'input-b']
        result.attributions().find { it.status() == 'unmatched' }.reason() == 'no-static-input'
        result.diagnostics().any { it.contains('input-map mismatch') }
    }

    def 'accounts for every workload participant evidence category'() {
        given:
        def workloads = [plan('common', [input('c1'), input('c2')]),
                         plan('across', [input('a1'), input('a2')]),
                         plan('some', [input('s1'), input('s2')]),
                         plan('none', [input('n1'), input('n2')])]
        def events = [
                exact('c1', 'shared-test', 'c-inv-1', 1), exact('c2', 'shared-test', 'c-inv-2', 2),
                exact('a1', 'separate-1', 'a-inv-1', 3), exact('a2', 'separate-2', 'a-inv-2', 4),
                exact('s1', 'some-test', 's-inv-1', 5)
        ]

        expect:
        new DynamicEvidenceJoiner().join(workloads, events).dynamicAccounting().workloadParticipantEvidence == [
                allInputsObservedInOneCommonTest: 1,
                allInputsObservedAcrossSeparateTests: 1,
                someInputsObserved: 1,
                noInputsObserved: 1
        ]
    }

    def 'distinguishes test and shape evidence from shape only evidence'() {
        given:
        def testPlan = plan('test-shape', [input('test-input', 'com.example.OrderSaga')], 'com.example.OrderSaga')
        def shapePlan = plan('shape-only', [input('shape-input', 'com.example.PaymentSaga')], 'com.example.PaymentSaga')
        def events = [
                event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order',
                                       testUniqueId: 'test-execution', functionalityName: 'OrderSaga',
                                       functionalityInvocationId: 'test-invocation', stepName: 'reserve', payload: [stepPhase: 'FORWARD']], 1),
                event('STEP_STARTED', [testClassFqn: 'com.example.UnrelatedSpec', testMethodName: 'unrelated',
                                       testUniqueId: 'shape-execution', functionalityName: 'PaymentSaga',
                                       functionalityInvocationId: 'shape-invocation', stepName: 'reserve', payload: [stepPhase: 'FORWARD']], 2)
        ]

        when:
        def result = new DynamicEvidenceJoiner().join([testPlan, shapePlan], events)

        then:
        result.attributions().collectEntries { [(it.testExecution()): it.status()] } == [
                'shape-execution': 'shapeOnly', 'test-execution': 'testAndShape'
        ]
        result.dynamicAccounting().uniqueInputEvidence.testAndShape == 1
        result.dynamicAccounting().uniqueInputEvidence.shapeOnly == 1
    }

    def 'normalizes dummyapp Saga evidence against its current workload contract'() {
        given:
        def saga = 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        def fixtureInput = new InputVariant('dummy-input', saga,
                'com.example.dummyapp.item.CreateItemFunctionalitySagasTest', 'creates item', 'createItemSaga',
                InputResolutionStatus.RESOLVED, 'source', 'dummyapp', [], [:], [])
        def workload = plan('dummy-workload', [fixtureInput], saga)
        def raw = event('COMMAND_SENT', [testClassFqn: fixtureInput.sourceClassFqn(),
                                         testMethodName: fixtureInput.sourceMethodName(), testUniqueId: 'dummy-execution',
                                         functionalityName: 'CreateItemFunctionalitySagas',
                                         functionalityInvocationId: 'dummy-invocation', stepName: 'reserve',
                                         inputVariantId: 'dummy-input',
                                         payload: [commandType: 'CreateItemCommand', fields: [itemId: 42]]], 1)

        when:
        def result = new DynamicEvidenceJoiner().join([workload], [raw])

        then:
        result.observations()[0].saga() == saga
        result.observations()[0].command().fields() == [itemId: 42]
        result.attributions()[0].status() == 'exactInput'
    }

    def 'runtime step shape distinguishes inputs and refuses a sole Saga input with a mismatching step'() {
        given:
        def reserve = input('reserve-input')
        def charge = input('charge-input')
        def shaped = new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, 'shaped', ScenarioKind.SINGLE_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL,
                [new SagaInstance('reserve-p', 'com.example.OrderSaga', 'reserve-input', []),
                 new SagaInstance('charge-p', 'com.example.OrderSaga', 'charge-input', [])], [reserve, charge],
                [new ScheduledStep('reserve-s', 'reserve-p', 'com.example.OrderSaga::reserve#0', 0, 'reserve', []),
                 new ScheduledStep('charge-s', 'charge-p', 'com.example.OrderSaga::charge#0', 1, 'charge', [])],
                [], [], [], [])
        def chargeEvent = event('STEP_STARTED', [testUniqueId: 'shape', functionalityName: 'OrderSaga',
                functionalityInvocationId: 'shape-inv', stepName: 'charge', payload: [stepPhase: 'FORWARD']], 1)
        def sole = plan('sole', [input('sole-input')])
        def mismatch = event('STEP_STARTED', [testUniqueId: 'mismatch', functionalityName: 'OrderSaga',
                functionalityInvocationId: 'mismatch-inv', stepName: 'unknown', payload: [stepPhase: 'FORWARD']], 2)

        expect:
        new DynamicEvidenceJoiner().join([shaped], [chargeEvent]).attributions()[0].with {
            status() == 'shapeOnly' && input() == 'charge-input'
        }
        new DynamicEvidenceJoiner().join([sole], [mismatch]).attributions()[0].status() == 'unmatched'
    }

    def 'one input observed under multiple attribution strengths counts once at its strongest category'() {
        given:
        def workload = plan('strength', [input('strength-input')])
        def events = [
                event('STEP_STARTED', [testClassFqn: 'other.Spec', testMethodName: 'other', testUniqueId: 'shape',
                    functionalityName: 'OrderSaga', functionalityInvocationId: 'shape-inv', stepName: 'reserve', payload: [stepPhase: 'FORWARD']], 1),
                event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'creates order', testUniqueId: 'test',
                    functionalityName: 'OrderSaga', functionalityInvocationId: 'test-inv', stepName: 'reserve', payload: [stepPhase: 'FORWARD']], 2),
                event('STEP_STARTED', [testUniqueId: 'exact', functionalityName: 'OrderSaga', functionalityInvocationId: 'exact-inv',
                    stepName: 'reserve', inputVariantId: 'strength-input', payload: [stepPhase: 'FORWARD']], 3)
        ]

        when:
        def result = new DynamicEvidenceJoiner().join([workload], events)

        then:
        result.attributions()*.status().toSet() == ['shapeOnly', 'testAndShape', 'exactInput'] as Set
        result.dynamicAccounting().uniqueInputEvidence == [exactInput: 1, testAndShape: 0, shapeOnly: 0]
    }

    def 'test display name preserves source and owner identity parity'() {
        given:
        def workload = plan('display', [input('display-input')])
        def raw = event('STEP_STARTED', [testClassFqn: 'com.example.OrderSpec', testMethodName: 'generatedName',
                testDisplayName: 'creates order', testUniqueId: 'display-test', functionalityName: 'OrderSaga',
                functionalityInvocationId: 'display-inv', stepName: 'reserve', payload: [stepPhase: 'FORWARD']], 1)

        expect:
        new DynamicEvidenceJoiner().join([workload], [raw]).attributions()[0].status() == 'testAndShape'
    }

    private static WorkloadPlan plan(String id, List<InputVariant> inputs, String saga = 'com.example.OrderSaga') {
        new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, id, ScenarioKind.SINGLE_SAGA, WorkloadExecutionShape.SAGA_LOCAL,
                inputs.withIndex().collect { value, index -> new SagaInstance("${id}-instance-${index}", saga, value.deterministicId(), []) }, inputs,
                inputs.withIndex().collect { value, index ->
                    new ScheduledStep("${id}-step-${index}", "${id}-instance-${index}", "${saga}::reserve#0", index, 'reserve', [])
                },
                [], [], [], [])
    }

    private static InputVariant input(String id, String saga = 'com.example.OrderSaga') {
        new InputVariant(id, saga, 'com.example.OrderSpec', 'creates order', 'orderSaga',
                InputResolutionStatus.RESOLVED, 'source', 'provenance', [], [:], [])
    }

    private static DynamicEvidenceEvent event(String kind, Map values, long sequence) {
        new DynamicEvidenceEvent("event-${sequence}", kind, values.testClassFqn as String,
                values.testMethodName as String, values.testDisplayName as String, values.testUniqueId as String,
                values.inputVariantId as String, values.functionalityName as String, null, null,
                values.functionalityInvocationId as String, values.stepName as String,
                "2026-08-22T10:00:0${sequence}Z", sequence, 'test-thread',
                values.payload == null ? [:] : values.payload as Map<String, Object>,
                Path.of('dynamic-evidence.jsonl'), sequence as int)
    }

    private static DynamicEvidenceEvent exact(String inputId, String execution, String invocation, long sequence) {
        event('STEP_STARTED', [testUniqueId: execution, functionalityName: 'OrderSaga',
                               functionalityInvocationId: invocation, stepName: 'reserve', inputVariantId: inputId,
                               payload: [stepPhase: 'FORWARD']], sequence)
    }
}
