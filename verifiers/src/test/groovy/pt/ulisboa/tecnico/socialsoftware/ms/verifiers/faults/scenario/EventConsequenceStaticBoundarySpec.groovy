package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification

class EventConsequenceStaticBoundarySpec extends Specification {

    def 'recovery keeps a trigger-masked event forced in normal order without adding fault or compensation ownership'() {
        given:
        def workload = workload(2)

        when:
        def allZero = RecoveryScheduleGenerator.generate(workload, '000', 20)
        def triggerFault = RecoveryScheduleGenerator.generate(workload, '010', 20)

        then:
        labels(workload, allZero.faultScenarios().first()) == ['F:a1', 'F:a2', 'E:event', 'F:b1']
        allZero.uncappedScheduleCount() == BigInteger.ONE
        triggerFault.uncappedScheduleCount() == new BigInteger('2')
        triggerFault.faultScenarios().every { scenario ->
            def actionLabels = labels(workload, scenario)
            actionLabels.findAll { it.startsWith('F:') } == ['F:a1', 'F:a2', 'F:b1']
            actionLabels.count('E:event') == 1
            actionLabels.count('C:a1') == 1
            actionLabels.indexOf('E:event') < actionLabels.indexOf('C:a1')
            new FaultScenarioValidator().validate(scenario, workload).valid()
        }
        triggerFault.faultSlotDiagnostics()*.state() == [
                FaultSlotGenerationState.NOT_ASSIGNED,
                FaultSlotGenerationState.REALIZED,
                FaultSlotGenerationState.NOT_ASSIGNED
        ]

        and: 'E owns no fault slot, vector bit, checkpoint, or generated compensation'
        workload.normalSchedule().size() == 4
        workload.faultSlots().size() == 3
        workload.compensationCheckpoints().size() == 1
        workload.faultSlots()*.scheduledStepId() == ['forward-0', 'forward-1', 'forward-2']
        workload.compensationCheckpoints()*.sourceScheduledStepId() == ['forward-0']
        triggerFault.faultScenarios()*.actions().flatten().findAll {
            it.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE
        }.every {
            it.sourceFaultSlotId() == null && it.sourceCompensationCheckpointId() == null &&
                    it.sourceEventConsequenceId() == workload.eventConsequences().first().deterministicId()
        }
        triggerFault.faultScenarios()*.actions().flatten().findAll {
            it.kind() == FaultScenarioActionKind.COMPENSATION
        }.every { it.sourceEventConsequenceId() == null }
    }

    def 'event placement participates in workload and FaultScenario identity'() {
        given:
        def positive = workload(2)
        def control = workload(3)

        when:
        def positiveScenario = RecoveryScheduleGenerator.generate(positive, '000').faultScenarios().first()
        def controlScenario = RecoveryScheduleGenerator.generate(control, '000').faultScenarios().first()

        then:
        positive.deterministicId() != control.deterministicId()
        positiveScenario.deterministicId() != controlScenario.deterministicId()
        labels(positive, positiveScenario) == ['F:a1', 'F:a2', 'E:event', 'F:b1']
        labels(control, controlScenario) == ['F:a1', 'F:a2', 'F:b1', 'E:event']
    }

    def 'v4 workload validation rejects embedded v1 input recipes'() {
        given:
        def valid = workload(2)
        def original = valid.acceptedInputs().first()
        def v1Recipe = new InputRecipe('microservices-simulator.input-recipe.v1', null, true, [], [])
        def v1Input = new InputVariant(original.deterministicId(), original.sagaFqn(),
                original.sourceClassFqn(), original.sourceMethodName(), original.sourceBindingName(),
                original.callContextMethodName(), original.inputRole(), original.fixtureOrigin(),
                original.resolutionStatus(), original.sourceMode(), original.sourceModeConfidence(),
                original.sourceModeEvidence(), original.stableSourceText(), original.provenanceText(),
                original.owners(), original.constructorArgumentSummaries(), original.logicalKeyBindings(),
                original.warnings(), v1Recipe)
        def invalid = new WorkloadPlan(valid.schemaVersion(), valid.deterministicId(), valid.kind(),
                valid.executionShape(), valid.participants(), [v1Input, valid.acceptedInputs()[1]],
                valid.forwardSchedule(), valid.eventConsequences(), valid.normalSchedule(),
                valid.conflictEvidence(), valid.faultSlots(), valid.compensationCheckpoints(), valid.warnings())

        expect:
        new WorkloadPlanValidator().validate(invalid).diagnostics()*.code().contains(
                'UNSUPPORTED_INPUT_RECIPE_SCHEMA')
    }

    def 'workload validator requires every semantic event emission and route field'() {
        given:
        def valid = workload(2)
        def malformed = malformedConsequence(valid.eventConsequences().first(), missingField)
        def invalid = new WorkloadPlan(valid.schemaVersion(), valid.deterministicId(), valid.kind(),
                valid.executionShape(), valid.participants(), valid.acceptedInputs(), valid.forwardSchedule(),
                [malformed], valid.normalSchedule(), valid.prerequisiteBaseline(), valid.conflictEvidence(),
                valid.faultSlots(), valid.compensationCheckpoints(), valid.warnings())

        expect:
        new WorkloadPlanValidator().validate(invalid).diagnostics()*.code().contains(
                'MALFORMED_EVENT_CONSEQUENCE')

        where:
        missingField << [
                'site.sourceServiceClassFqn',
                'site.sourceServiceMethodSignature',
                'site.emissionOrdinal',
                'site.eventTypeFqn',
                'eventTypeFqn',
                'eventHandlingClassFqn',
                'eventHandlingMethodName',
                'eventHandlerClassFqn',
                'eventProcessingClassFqn',
                'eventProcessingMethodName',
                'facadeClassFqn',
                'facadeMethodName',
                'downstreamSagaFqn',
                'deliveryPolicy'
        ]
    }

    def 'validators reject causal normal-order violations and event actions with fault ownership'() {
        given:
        def valid = workload(2)
        def invalidNormal = withId(new WorkloadPlan(
                WorkloadPlan.SCHEMA_VERSION, null, valid.kind(), valid.executionShape(),
                valid.participants(), valid.acceptedInputs(), valid.forwardSchedule(),
                valid.eventConsequences(), [
                NormalActionRef.forward(0, 'forward-0'),
                NormalActionRef.eventConsequence(1, 'event'),
                NormalActionRef.forward(2, 'forward-1'),
                NormalActionRef.forward(3, 'forward-2')
        ], valid.conflictEvidence(), valid.faultSlots(), valid.compensationCheckpoints(), []))
        def event = valid.eventConsequences().first()
        def malformed = new FaultScenarioAction(
                ScenarioIdGenerator.faultScenarioActionId(FaultScenarioActionKind.EVENT_CONSEQUENCE,
                        'a', 'slot-1', null, event.deterministicId(), event.deterministicId()),
                FaultScenarioActionKind.EVENT_CONSEQUENCE, 'a', 'slot-1', null,
                event.deterministicId(), event.deterministicId())
        def validScenario = RecoveryScheduleGenerator.generate(valid, '000').faultScenarios().first()
        def malformedActions = validScenario.actions().collect { action ->
            action.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE ? malformed : action
        }
        def malformedScenarioWithoutId = new FaultScenario(FaultScenario.SCHEMA_VERSION, null,
                valid.deterministicId(), '000', malformedActions)
        def malformedScenario = new FaultScenario(FaultScenario.SCHEMA_VERSION,
                ScenarioIdGenerator.faultScenarioId(malformedScenarioWithoutId), valid.deterministicId(),
                '000', malformedActions)

        expect:
        new WorkloadPlanValidator().validate(invalidNormal).diagnostics()*.code().contains(
                'EVENT_CONSEQUENCE_CAUSAL_ORDER_VIOLATION')
        new FaultScenarioValidator().validate(malformedScenario, valid).diagnostics()*.code().contains(
                'MALFORMED_EVENT_CONSEQUENCE_ACTION')
    }

    private static WorkloadPlan workload(int eventPlacement) {
        def participants = [
                new SagaInstance('a', 'example.ASaga', 'input-a', []),
                new SagaInstance('b', 'example.BSaga', 'input-b', [])
        ]
        def inputs = [input('input-a', 'example.ASaga'), input('input-b', 'example.BSaga')]
        def schedule = [
                new ScheduledStep('forward-0', 'a', 'example.ASaga::a1', 0, 'a1', []),
                new ScheduledStep('forward-1', 'a', 'example.ASaga::a2', 1, 'a2', []),
                new ScheduledStep('forward-2', 'b', 'example.BSaga::b1', 2, 'b1', [])
        ]
        def slots = schedule.withIndex().collect { step, index ->
            new ForwardFaultSlot("slot-${index}".toString(), index, step.deterministicId(),
                    step.sagaInstanceId(), step.stepId(), step.runtimeStepName(), step.deterministicId())
        }
        def checkpoints = [new CompensationCheckpoint('checkpoint-0', 0, 'a', 'forward-0',
                'example.ASaga::a1', 'a1', 'forward-0', CompensationEvidenceClass.EXPLICIT_COMPENSATION,
                [], [], [])]
        def site = new EventEmissionSite(
                ScenarioIdGenerator.eventEmissionSiteId('example.AService', 'a2(example.Command)', 0,
                        'example.Event'),
                'example.AService', 'a2(example.Command)', 0, 'example.Event', ['direct'])
        def consequenceWithoutId = new EventConsequence(null, 'forward-1', site, 'example.Event',
                'example.EventHandling', 'handle', 'example.EventHandler', 'example.EventProcessing',
                'process', 'example.Facade', 'invoke', 'example.DownstreamSaga',
                EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER, [])
        def consequence = new EventConsequence(
                ScenarioIdGenerator.eventConsequenceId('forward-1', site, 'example.EventHandling', 'handle',
                        'example.EventHandler', 'example.EventProcessing', 'process', 'example.Facade',
                        'invoke', 'example.DownstreamSaga', EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER),
                consequenceWithoutId.triggerScheduledStepId(), consequenceWithoutId.emissionSite(),
                consequenceWithoutId.eventTypeFqn(), consequenceWithoutId.eventHandlingClassFqn(),
                consequenceWithoutId.eventHandlingMethodName(), consequenceWithoutId.eventHandlerClassFqn(),
                consequenceWithoutId.eventProcessingClassFqn(), consequenceWithoutId.eventProcessingMethodName(),
                consequenceWithoutId.facadeClassFqn(), consequenceWithoutId.facadeMethodName(),
                consequenceWithoutId.downstreamSagaFqn(), consequenceWithoutId.deliveryPolicy(), [])
        // Use a stable human-readable id in assertions while retaining semantic identity fields.
        consequence = new EventConsequence('event', consequence.triggerScheduledStepId(), consequence.emissionSite(),
                consequence.eventTypeFqn(), consequence.eventHandlingClassFqn(), consequence.eventHandlingMethodName(),
                consequence.eventHandlerClassFqn(), consequence.eventProcessingClassFqn(),
                consequence.eventProcessingMethodName(), consequence.facadeClassFqn(), consequence.facadeMethodName(),
                consequence.downstreamSagaFqn(), consequence.deliveryPolicy(), [])
        def normal = []
        int order = 0
        schedule.eachWithIndex { step, index ->
            if (index == eventPlacement) normal << NormalActionRef.eventConsequence(order++, 'event')
            normal << NormalActionRef.forward(order++, step.deterministicId())
        }
        if (eventPlacement == schedule.size()) normal << NormalActionRef.eventConsequence(order, 'event')
        withId(new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, null, ScenarioKind.MULTI_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, participants, inputs, schedule, [consequence], normal,
                [], slots, checkpoints, []), true)
    }

    private static EventConsequence malformedConsequence(EventConsequence valid, String missingField) {
        def site = valid.emissionSite()
        def malformedSite = new EventEmissionSite(
                site.deterministicId(),
                missingField == 'site.sourceServiceClassFqn' ? null : site.sourceServiceClassFqn(),
                missingField == 'site.sourceServiceMethodSignature' ? null : site.sourceServiceMethodSignature(),
                missingField == 'site.emissionOrdinal' ? -1 : site.emissionOrdinal(),
                missingField == 'site.eventTypeFqn' ? null : site.eventTypeFqn(),
                site.extractionEvidence())
        new EventConsequence(
                valid.deterministicId(), valid.triggerScheduledStepId(), malformedSite,
                missingField == 'eventTypeFqn' ? null : valid.eventTypeFqn(),
                missingField == 'eventHandlingClassFqn' ? null : valid.eventHandlingClassFqn(),
                missingField == 'eventHandlingMethodName' ? null : valid.eventHandlingMethodName(),
                missingField == 'eventHandlerClassFqn' ? null : valid.eventHandlerClassFqn(),
                missingField == 'eventProcessingClassFqn' ? null : valid.eventProcessingClassFqn(),
                missingField == 'eventProcessingMethodName' ? null : valid.eventProcessingMethodName(),
                missingField == 'facadeClassFqn' ? null : valid.facadeClassFqn(),
                missingField == 'facadeMethodName' ? null : valid.facadeMethodName(),
                missingField == 'downstreamSagaFqn' ? null : valid.downstreamSagaFqn(),
                missingField == 'deliveryPolicy' ? null : valid.deliveryPolicy(), valid.diagnostics())
    }

    private static InputVariant input(String id, String sagaFqn) {
        new InputVariant(id, sagaFqn, 'example.Spec', 'feature', id,
                InputResolutionStatus.RESOLVED, 'source', 'provenance', [], [:], [])
    }

    private static WorkloadPlan withId(WorkloadPlan plan, boolean preserveFixtureEventId = false) {
        if (preserveFixtureEventId) {
            // The validator requires the semantic event id; replace the short fixture reference first.
            def consequence = plan.eventConsequences().first()
            def semanticId = ScenarioIdGenerator.eventConsequenceId(consequence.triggerScheduledStepId(),
                    consequence.emissionSite(), consequence.eventHandlingClassFqn(),
                    consequence.eventHandlingMethodName(), consequence.eventHandlerClassFqn(),
                    consequence.eventProcessingClassFqn(), consequence.eventProcessingMethodName(),
                    consequence.facadeClassFqn(), consequence.facadeMethodName(), consequence.downstreamSagaFqn(),
                    consequence.deliveryPolicy())
            def semantic = new EventConsequence(semanticId, consequence.triggerScheduledStepId(), consequence.emissionSite(),
                    consequence.eventTypeFqn(), consequence.eventHandlingClassFqn(), consequence.eventHandlingMethodName(),
                    consequence.eventHandlerClassFqn(), consequence.eventProcessingClassFqn(),
                    consequence.eventProcessingMethodName(), consequence.facadeClassFqn(), consequence.facadeMethodName(),
                    consequence.downstreamSagaFqn(), consequence.deliveryPolicy(), consequence.diagnostics())
            def normal = plan.normalSchedule().collect { action -> action.kind() == NormalActionKind.EVENT_CONSEQUENCE
                    ? NormalActionRef.eventConsequence(action.normalOrder(), semanticId) : action }
            plan = new WorkloadPlan(plan.schemaVersion(), null, plan.kind(), plan.executionShape(), plan.participants(),
                    plan.acceptedInputs(), plan.forwardSchedule(), [semantic], normal, plan.conflictEvidence(),
                    plan.faultSlots(), plan.compensationCheckpoints(), plan.warnings())
        }
        new WorkloadPlan(plan.schemaVersion(), ScenarioIdGenerator.workloadPlanId(plan), plan.kind(),
                plan.executionShape(), plan.participants(), plan.acceptedInputs(), plan.forwardSchedule(),
                plan.eventConsequences(), plan.normalSchedule(), plan.conflictEvidence(), plan.faultSlots(),
                plan.compensationCheckpoints(), plan.warnings())
    }

    private static List<String> labels(WorkloadPlan workload, FaultScenario scenario) {
        def slotNames = workload.faultSlots().collectEntries { [(it.deterministicId()): it.runtimeStepName()] }
        def checkpointNames = workload.compensationCheckpoints().collectEntries { [(it.deterministicId()): it.runtimeStepName()] }
        scenario.actions().collect { action ->
            switch (action.kind()) {
                case FaultScenarioActionKind.FORWARD -> "F:${slotNames[action.sourceFaultSlotId()]}".toString()
                case FaultScenarioActionKind.EVENT_CONSEQUENCE -> 'E:event'
                case FaultScenarioActionKind.COMPENSATION -> "C:${checkpointNames[action.sourceCompensationCheckpointId()]}".toString()
            }
        }
    }
}
