package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import spock.lang.Specification

class ScenarioModelSpec extends Specification {

    def 'checkpoint evidence is semantic but diagnostic warnings are not part of workload identity'() {
        given:
        def explicit = workload(checkpoint(CompensationEvidenceClass.EXPLICIT_COMPENSATION, ['diagnostic-a']))
        def implicit = workload(checkpoint(CompensationEvidenceClass.IMPLICIT_SAGA_ROLLBACK, ['diagnostic-a']))
        def warningOnly = workload(checkpoint(CompensationEvidenceClass.EXPLICIT_COMPENSATION, ['diagnostic-b']), ['different workload warning'])

        expect:
        ScenarioIdGenerator.workloadPlanId(explicit) != ScenarioIdGenerator.workloadPlanId(implicit)
        ScenarioIdGenerator.workloadPlanId(explicit) == ScenarioIdGenerator.workloadPlanId(warningOnly)
    }

    def 'every workload semantic collection participates in deterministic identity'() {
        given:
        def baseline = workload(checkpoint(CompensationEvidenceClass.EXPLICIT_COMPENSATION, []))

        expect:
        ScenarioIdGenerator.workloadPlanId(mutated) != ScenarioIdGenerator.workloadPlanId(baseline)

        where:
        mutated << [
                copyWorkload(participants: [new SagaInstance('other-participant', 'example.Saga', 'input-1', [])]),
                copyWorkload(acceptedInputs: [input('other-input')]),
                copyWorkload(forwardSchedule: [new ScheduledStep('scheduled-1', 'participant-1', 'example.Saga::other', 0, 'other', [])]),
                copyWorkload(conflictEvidence: [new ConflictEvidence('conflict-1', 'scheduled-1', 'scheduled-1', null, null, AccessMode.WRITE, AccessMode.READ, ConflictKind.WRITE_READ, [])]),
                copyWorkload(faultSlots: [new ForwardFaultSlot('slot-1', 0, 'scheduled-1', 'participant-1', 'example.Saga::step', 'step', 'different-occurrence')]),
                copyWorkload(compensationCheckpoints: [checkpoint(CompensationEvidenceClass.CONSERVATIVE_UNKNOWN, [])])
        ]
    }

    def 'every nested workload semantic field participates in deterministic identity'() {
        given:
        def baseline = semanticWorkload()

        expect:
        ScenarioIdGenerator.workloadPlanId(mutated) != ScenarioIdGenerator.workloadPlanId(baseline)

        where:
        field                                      | mutated
        'kind'                                     | semanticWorkload(kind: ScenarioKind.MULTI_SAGA)
        'participant deterministicId'              | semanticWorkload(participants: [semanticParticipant(deterministicId: 'participant-2')])
        'participant sagaFqn'                      | semanticWorkload(participants: [semanticParticipant(sagaFqn: 'example.OtherSaga')])
        'participant inputVariantId'               | semanticWorkload(participants: [semanticParticipant(inputVariantId: 'input-2')])
        'input deterministicId'                    | semanticWorkload(acceptedInputs: [semanticInput(deterministicId: 'input-2')])
        'input sagaFqn'                            | semanticWorkload(acceptedInputs: [semanticInput(sagaFqn: 'example.OtherSaga')])
        'input sourceClassFqn'                     | semanticWorkload(acceptedInputs: [semanticInput(sourceClassFqn: 'example.OtherSpec')])
        'input sourceMethodName'                   | semanticWorkload(acceptedInputs: [semanticInput(sourceMethodName: 'other feature')])
        'input sourceBindingName'                  | semanticWorkload(acceptedInputs: [semanticInput(sourceBindingName: 'otherBinding')])
        'input callContextMethodName'              | semanticWorkload(acceptedInputs: [semanticInput(callContextMethodName: 'otherContext')])
        'input role'                               | semanticWorkload(acceptedInputs: [semanticInput(inputRole: InputRole.FIXTURE_PREREQUISITE)])
        'input fixture origin'                     | semanticWorkload(acceptedInputs: [semanticInput(fixtureOrigin: FixtureOrigin.SETUP)])
        'input resolution status'                  | semanticWorkload(acceptedInputs: [semanticInput(resolutionStatus: InputResolutionStatus.PARTIAL)])
        'input source mode'                        | semanticWorkload(acceptedInputs: [semanticInput(sourceMode: SourceMode.UNKNOWN)])
        'input source mode confidence'             | semanticWorkload(acceptedInputs: [semanticInput(sourceModeConfidence: SourceModeConfidence.UNKNOWN)])
        'input source mode evidence'               | semanticWorkload(acceptedInputs: [semanticInput(sourceModeEvidence: ['other evidence'])])
        'input stable source text'                 | semanticWorkload(acceptedInputs: [semanticInput(stableSourceText: 'other source')])
        'input provenance text'                    | semanticWorkload(acceptedInputs: [semanticInput(provenanceText: 'other provenance')])
        'input owner class'                        | semanticWorkload(acceptedInputs: [semanticInput(owners: [new InputOwner('example.OtherSpec', 'feature')])])
        'input owner method'                       | semanticWorkload(acceptedInputs: [semanticInput(owners: [new InputOwner('example.SagaSpec', 'other feature')])])
        'input constructor argument summaries'     | semanticWorkload(acceptedInputs: [semanticInput(constructorArgumentSummaries: ['other summary'])])
        'input logical key bindings'               | semanticWorkload(acceptedInputs: [semanticInput(logicalKeyBindings: [orderId: '2'])])
        'recipe schema'                            | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(schemaVersion: 'other-recipe-schema'))])
        'recipe executor readiness'                | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(executorReady: false))])
        'recipe blockers'                          | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(blockers: ['recipe blocker']))])
        'recipe argument order/index'              | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(index: 1)]))])
        'recipe argument expected type'            | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(expectedTypeFqn: 'java.lang.Integer')]))])
        'recipe argument resolution status'        | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(resolutionStatus: InputResolutionStatus.PARTIAL)]))])
        'recipe argument executor readiness'       | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(executorReady: false)]))])
        'recipe argument blockers'                 | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(blockers: ['argument blocker'])]))])
        'recipe argument provenance'               | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(provenanceText: 'other argument')]))])
        'recipe node kind'                         | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(recipe: semanticNode(kind: 'placeholder'))]))])
        'recipe node source text'                  | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(recipe: semanticNode(sourceText: '2'))]))])
        'recipe node provenance'                   | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(recipe: semanticNode(provenanceText: 'other node'))]))])
        'recipe node executor readiness'           | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(recipe: semanticNode(executorReady: false))]))])
        'recipe node blockers'                     | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(recipe: semanticNode(blockers: ['node blocker']))]))])
        'recipe node literal kind'                 | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(recipe: semanticNode(literalKind: 'decimal'))]))])
        'recipe node literal value'                | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(recipe: semanticNode(value: 2L))]))])
        'recipe node target type FQN'              | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(recipe: semanticNode(targetTypeFqn: 'java.lang.Integer'))]))])
        'recipe node target type text'             | semanticWorkload(acceptedInputs: [semanticInput(inputRecipe: semanticRecipe(arguments: [semanticArgument(recipe: semanticNode(targetTypeText: 'Long'))]))])
        'scheduled step deterministicId'           | semanticWorkload(forwardSchedule: [semanticStep(deterministicId: 'scheduled-2')])
        'scheduled step owner'                     | semanticWorkload(forwardSchedule: [semanticStep(sagaInstanceId: 'participant-2')])
        'scheduled step definition'                | semanticWorkload(forwardSchedule: [semanticStep(stepId: 'example.Saga::other')])
        'scheduled step order'                     | semanticWorkload(forwardSchedule: [semanticStep(scheduleOrder: 1)])
        'scheduled step runtime name'              | semanticWorkload(forwardSchedule: [semanticStep(runtimeStepName: 'other')])
        'conflict deterministicId'                 | semanticWorkload(conflictEvidence: [semanticConflict(deterministicId: 'conflict-2')])
        'conflict left occurrence'                 | semanticWorkload(conflictEvidence: [semanticConflict(leftScheduledStepId: 'scheduled-2')])
        'conflict right occurrence'                | semanticWorkload(conflictEvidence: [semanticConflict(rightScheduledStepId: 'scheduled-2')])
        'conflict aggregate type name'             | semanticWorkload(conflictEvidence: [semanticConflict(leftAggregateKey: semanticAggregate(aggregateTypeName: 'example.Other'))])
        'conflict aggregate name'                  | semanticWorkload(conflictEvidence: [semanticConflict(leftAggregateKey: semanticAggregate(aggregateName: 'Other'))])
        'conflict aggregate key text'              | semanticWorkload(conflictEvidence: [semanticConflict(leftAggregateKey: semanticAggregate(keyText: '2'))])
        'conflict aggregate confidence'            | semanticWorkload(conflictEvidence: [semanticConflict(leftAggregateKey: semanticAggregate(confidence: FootprintConfidence.TYPE_ONLY))])
        'conflict right aggregate position'        | semanticWorkload(conflictEvidence: [semanticConflict(rightAggregateKey: semanticAggregate(aggregateName: 'Other'))])
        'conflict left access mode'                | semanticWorkload(conflictEvidence: [semanticConflict(leftAccessMode: AccessMode.READ)])
        'conflict right access mode'               | semanticWorkload(conflictEvidence: [semanticConflict(rightAccessMode: AccessMode.WRITE)])
        'conflict kind'                            | semanticWorkload(conflictEvidence: [semanticConflict(kind: ConflictKind.WRITE_WRITE)])
        'fault slot deterministicId'               | semanticWorkload(faultSlots: [semanticSlot(deterministicId: 'slot-2')])
        'fault slot index'                         | semanticWorkload(faultSlots: [semanticSlot(slotIndex: 1)])
        'fault slot scheduled occurrence'          | semanticWorkload(faultSlots: [semanticSlot(scheduledStepId: 'scheduled-2')])
        'fault slot owner'                         | semanticWorkload(faultSlots: [semanticSlot(sagaInstanceId: 'participant-2')])
        'fault slot step definition'               | semanticWorkload(faultSlots: [semanticSlot(stepId: 'example.Saga::other')])
        'fault slot runtime name'                  | semanticWorkload(faultSlots: [semanticSlot(runtimeStepName: 'other')])
        'fault slot occurrence identity'           | semanticWorkload(faultSlots: [semanticSlot(occurrenceId: 'scheduled-2')])
        'checkpoint deterministicId'               | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(deterministicId: 'checkpoint-2')])
        'checkpoint index'                         | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(checkpointIndex: 1)])
        'checkpoint owner'                         | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(sagaInstanceId: 'participant-2')])
        'checkpoint source occurrence'             | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(sourceScheduledStepId: 'scheduled-2')])
        'checkpoint step definition'               | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(stepId: 'example.Saga::other')])
        'checkpoint runtime name'                  | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(runtimeStepName: 'other')])
        'checkpoint occurrence identity'           | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(occurrenceId: 'scheduled-2')])
        'checkpoint evidence class'                | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(evidenceClass: CompensationEvidenceClass.CONSERVATIVE_UNKNOWN)])
        'checkpoint forward footprint'             | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(forwardFootprints: [semanticFootprint(accessMode: AccessMode.READ)])])
        'checkpoint compensation footprint'        | semanticWorkload(compensationCheckpoints: [semanticCheckpoint(compensationFootprints: [semanticFootprint(aggregateKey: semanticAggregate(keyText: '2'))])])

    }

    def 'warnings at every workload layer remain diagnostic-only'() {
        given:
        def baseline = semanticWorkload()
        def warningOnly = semanticWorkload(
                warnings: ['workload warning'],
                participants: [semanticParticipant(warnings: ['participant warning'])],
                acceptedInputs: [semanticInput(warnings: ['input warning'])],
                forwardSchedule: [semanticStep(warnings: ['schedule warning'])],
                conflictEvidence: [semanticConflict(warnings: ['conflict warning'])],
                compensationCheckpoints: [semanticCheckpoint(
                        warnings: ['checkpoint warning'],
                        forwardFootprints: [semanticFootprint(warnings: ['forward footprint warning'])],
                        compensationFootprints: [semanticFootprint(warnings: ['compensation footprint warning'])])])

        expect:
        ScenarioIdGenerator.workloadPlanId(warningOnly) == ScenarioIdGenerator.workloadPlanId(baseline)
    }

    def 'input recipe semantic mutations change workload identity and stale fingerprints are rejected'() {
        given:
        def originalRecipe = recipeWithLiteral(1L)
        def staleRecipe = recipeWithLiteral(2L, originalRecipe.recipeFingerprint())
        def original = copyWorkload(acceptedInputs: [input('input-1', originalRecipe)])
        def changed = copyWorkload(acceptedInputs: [input('input-1', staleRecipe)])
        def changedWithCurrentId = withDeterministicId(changed)

        expect:
        ScenarioIdGenerator.workloadPlanId(original) != ScenarioIdGenerator.workloadPlanId(changed)

        and:
        def validation = new WorkloadPlanValidator().validate(changedWithCurrentId)
        !validation.valid()
        validation.diagnostics()*.code().contains('INPUT_RECIPE_FINGERPRINT_MISMATCH')
    }

    def 'workload records defensively copy ordered collections and derive exact forward slots'() {
        given:
        def participants = [new SagaInstance('participant-1', 'example.Saga', 'input-1', [])]
        def inputs = [input('input-1')]
        def schedule = [new ScheduledStep('scheduled-1', 'participant-1', 'example.Saga::step', 0, 'step', [])]
        def slots = [new ForwardFaultSlot('slot-1', 0, 'scheduled-1', 'participant-1', 'example.Saga::step', 'step', 'scheduled-1')]
        def checkpoints = [checkpoint(CompensationEvidenceClass.EXPLICIT_COMPENSATION, [])]
        def plan = new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, 'workload-1', ScenarioKind.SINGLE_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, participants, inputs, schedule, [], slots, checkpoints, [])

        when:
        participants << new SagaInstance('participant-2', 'example.OtherSaga', 'input-2', [])
        inputs << input('input-2')
        schedule << new ScheduledStep('scheduled-2', 'participant-1', 'example.Saga::other', 1, 'other', [])
        slots.clear()
        checkpoints.clear()

        then:
        plan.participants()*.deterministicId() == ['participant-1']
        plan.acceptedInputs()*.deterministicId() == ['input-1']
        plan.forwardSchedule()*.deterministicId() == ['scheduled-1']
        plan.faultSlots()*.occurrenceId() == ['scheduled-1']
        plan.compensationCheckpoints().size() == 1
    }

    private static WorkloadPlan semanticWorkload(Map overrides = [:]) {
        new WorkloadPlan(
                WorkloadPlan.SCHEMA_VERSION,
                null,
                overrides.get('kind', ScenarioKind.SINGLE_SAGA) as ScenarioKind,
                WorkloadExecutionShape.SAGA_LOCAL,
                overrides.get('participants', [semanticParticipant()]) as List<SagaInstance>,
                overrides.get('acceptedInputs', [semanticInput()]) as List<InputVariant>,
                overrides.get('forwardSchedule', [semanticStep()]) as List<ScheduledStep>,
                overrides.get('conflictEvidence', [semanticConflict()]) as List<ConflictEvidence>,
                overrides.get('faultSlots', [semanticSlot()]) as List<ForwardFaultSlot>,
                overrides.get('compensationCheckpoints', [semanticCheckpoint()]) as List<CompensationCheckpoint>,
                overrides.get('warnings', []) as List<String>)
    }

    private static SagaInstance semanticParticipant(Map overrides = [:]) {
        new SagaInstance(
                overrides.get('deterministicId', 'participant-1') as String,
                overrides.get('sagaFqn', 'example.Saga') as String,
                overrides.get('inputVariantId', 'input-1') as String,
                overrides.get('warnings', []) as List<String>)
    }

    private static InputVariant semanticInput(Map overrides = [:]) {
        new InputVariant(
                overrides.get('deterministicId', 'input-1') as String,
                overrides.get('sagaFqn', 'example.Saga') as String,
                overrides.get('sourceClassFqn', 'example.SagaSpec') as String,
                overrides.get('sourceMethodName', 'feature') as String,
                overrides.get('sourceBindingName', 'field') as String,
                overrides.get('callContextMethodName', 'context') as String,
                overrides.get('inputRole', InputRole.FEATURE_UNDER_TEST) as InputRole,
                overrides.get('fixtureOrigin', FixtureOrigin.DIRECT_FEATURE) as FixtureOrigin,
                overrides.get('resolutionStatus', InputResolutionStatus.RESOLVED) as InputResolutionStatus,
                overrides.get('sourceMode', SourceMode.SAGAS) as SourceMode,
                overrides.get('sourceModeConfidence', SourceModeConfidence.TYPE_EVIDENCE) as SourceModeConfidence,
                overrides.get('sourceModeEvidence', ['saga profile']) as List<String>,
                overrides.get('stableSourceText', 'source') as String,
                overrides.get('provenanceText', 'provenance') as String,
                overrides.get('owners', [new InputOwner('example.SagaSpec', 'feature')]) as List<InputOwner>,
                overrides.get('constructorArgumentSummaries', ['1']) as List<String>,
                overrides.get('logicalKeyBindings', [orderId: '1']) as Map<String, String>,
                overrides.get('warnings', []) as List<String>,
                overrides.get('inputRecipe', semanticRecipe()) as InputRecipe)
    }

    private static InputRecipe semanticRecipe(Map overrides = [:]) {
        new InputRecipe(
                overrides.get('schemaVersion', InputRecipe.SCHEMA_VERSION) as String,
                null,
                overrides.get('executorReady', true) as boolean,
                overrides.get('blockers', []) as List<String>,
                overrides.get('arguments', [semanticArgument()]) as List<InputRecipeArgument>)
    }

    private static InputRecipeArgument semanticArgument(Map overrides = [:]) {
        new InputRecipeArgument(
                overrides.get('index', 0) as int,
                overrides.get('expectedTypeFqn', 'java.lang.Long') as String,
                overrides.get('resolutionStatus', InputResolutionStatus.RESOLVED) as InputResolutionStatus,
                overrides.get('executorReady', true) as boolean,
                overrides.get('blockers', []) as List<String>,
                overrides.get('provenanceText', 'argument 0') as String,
                overrides.get('recipe', semanticNode()) as InputRecipeNode)
    }

    private static InputRecipeNode semanticNode(Map overrides = [:]) {
        InputRecipeNode.builder(overrides.get('kind', 'literal') as String)
                .sourceText(overrides.get('sourceText', '1') as String)
                .provenanceText(overrides.get('provenanceText', 'constructor argument 0') as String)
                .executorReady(overrides.get('executorReady', true) as boolean)
                .blockers(overrides.get('blockers', []) as List<String>)
                .literalKind(overrides.get('literalKind', 'integer') as String)
                .value(overrides.get('value', 1L))
                .targetTypeFqn(overrides.get('targetTypeFqn', 'java.lang.Long') as String)
                .targetTypeText(overrides.get('targetTypeText', 'long') as String)
                .build()
    }

    private static ScheduledStep semanticStep(Map overrides = [:]) {
        new ScheduledStep(
                overrides.get('deterministicId', 'scheduled-1') as String,
                overrides.get('sagaInstanceId', 'participant-1') as String,
                overrides.get('stepId', 'example.Saga::step') as String,
                overrides.get('scheduleOrder', 0) as int,
                overrides.get('runtimeStepName', 'step') as String,
                overrides.get('warnings', []) as List<String>)
    }

    private static ConflictEvidence semanticConflict(Map overrides = [:]) {
        new ConflictEvidence(
                overrides.get('deterministicId', 'conflict-1') as String,
                overrides.get('leftScheduledStepId', 'scheduled-1') as String,
                overrides.get('rightScheduledStepId', 'scheduled-1') as String,
                overrides.get('leftAggregateKey', semanticAggregate()) as AggregateKey,
                overrides.get('rightAggregateKey', semanticAggregate()) as AggregateKey,
                overrides.get('leftAccessMode', AccessMode.WRITE) as AccessMode,
                overrides.get('rightAccessMode', AccessMode.READ) as AccessMode,
                overrides.get('kind', ConflictKind.WRITE_READ) as ConflictKind,
                overrides.get('warnings', []) as List<String>)
    }

    private static AggregateKey semanticAggregate(Map overrides = [:]) {
        new AggregateKey(
                overrides.get('aggregateTypeName', 'example.Order') as String,
                overrides.get('aggregateName', 'Order') as String,
                overrides.get('keyText', '1') as String,
                overrides.get('confidence', FootprintConfidence.EXACT) as FootprintConfidence)
    }

    private static ForwardFaultSlot semanticSlot(Map overrides = [:]) {
        new ForwardFaultSlot(
                overrides.get('deterministicId', 'slot-1') as String,
                overrides.get('slotIndex', 0) as int,
                overrides.get('scheduledStepId', 'scheduled-1') as String,
                overrides.get('sagaInstanceId', 'participant-1') as String,
                overrides.get('stepId', 'example.Saga::step') as String,
                overrides.get('runtimeStepName', 'step') as String,
                overrides.get('occurrenceId', 'scheduled-1') as String)
    }

    private static CompensationCheckpoint semanticCheckpoint(Map overrides = [:]) {
        new CompensationCheckpoint(
                overrides.get('deterministicId', 'checkpoint-1') as String,
                overrides.get('checkpointIndex', 0) as int,
                overrides.get('sagaInstanceId', 'participant-1') as String,
                overrides.get('sourceScheduledStepId', 'scheduled-1') as String,
                overrides.get('stepId', 'example.Saga::step') as String,
                overrides.get('runtimeStepName', 'step') as String,
                overrides.get('occurrenceId', 'scheduled-1') as String,
                overrides.get('evidenceClass', CompensationEvidenceClass.EXPLICIT_COMPENSATION) as CompensationEvidenceClass,
                overrides.get('forwardFootprints', [semanticFootprint()]) as List<StepFootprint>,
                overrides.get('compensationFootprints', [semanticFootprint()]) as List<StepFootprint>,
                overrides.get('warnings', []) as List<String>)
    }

    private static StepFootprint semanticFootprint(Map overrides = [:]) {
        new StepFootprint(
                overrides.get('aggregateKey', semanticAggregate()) as AggregateKey,
                overrides.get('accessMode', AccessMode.WRITE) as AccessMode,
                overrides.get('warnings', []) as List<String>)
    }

    private static WorkloadPlan copyWorkload(Map overrides) {
        def original = workload(checkpoint(CompensationEvidenceClass.EXPLICIT_COMPENSATION, []))
        new WorkloadPlan(
                original.schemaVersion(),
                null,
                overrides.get('kind', original.kind()) as ScenarioKind,
                overrides.get('executionShape', original.executionShape()) as WorkloadExecutionShape,
                overrides.get('participants', original.participants()) as List<SagaInstance>,
                overrides.get('acceptedInputs', original.acceptedInputs()) as List<InputVariant>,
                overrides.get('forwardSchedule', original.forwardSchedule()) as List<ScheduledStep>,
                overrides.get('conflictEvidence', original.conflictEvidence()) as List<ConflictEvidence>,
                overrides.get('faultSlots', original.faultSlots()) as List<ForwardFaultSlot>,
                overrides.get('compensationCheckpoints', original.compensationCheckpoints()) as List<CompensationCheckpoint>,
                original.warnings())
    }

    private static WorkloadPlan workload(CompensationCheckpoint checkpoint, List<String> warnings = []) {
        new WorkloadPlan(
                WorkloadPlan.SCHEMA_VERSION,
                null,
                ScenarioKind.SINGLE_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL,
                [new SagaInstance('participant-1', 'example.Saga', 'input-1', [])],
                [input('input-1')],
                [new ScheduledStep('scheduled-1', 'participant-1', 'example.Saga::step', 0, 'step', [])],
                [],
                [new ForwardFaultSlot('slot-1', 0, 'scheduled-1', 'participant-1', 'example.Saga::step', 'step', 'scheduled-1')],
                [checkpoint],
                warnings)
    }

    private static CompensationCheckpoint checkpoint(CompensationEvidenceClass evidenceClass, List<String> warnings) {
        new CompensationCheckpoint(
                'checkpoint-1',
                0,
                'participant-1',
                'scheduled-1',
                'example.Saga::step',
                'step',
                'scheduled-1',
                evidenceClass,
                [new StepFootprint(new AggregateKey('example.Order', 'Order', '1', FootprintConfidence.EXACT), AccessMode.WRITE, [])],
                [],
                warnings)
    }

    private static InputVariant input(String id, InputRecipe recipe = null) {
        new InputVariant(id, 'example.Saga', 'example.SagaSpec', 'feature', 'field',
                InputResolutionStatus.RESOLVED, SourceMode.SAGAS, SourceModeConfidence.TYPE_EVIDENCE,
                ['saga profile'], 'source', 'provenance', [], [], [:], [], recipe)
    }

    private static InputRecipe recipeWithLiteral(long value, String suppliedFingerprint = null) {
        def node = InputRecipeNode.builder('literal')
                .sourceText(value.toString())
                .provenanceText('constructor argument 0')
                .executorReady(true)
                .literalKind('integer')
                .value(value)
                .targetTypeFqn('java.lang.Long')
                .build()
        def argument = new InputRecipeArgument(0, 'java.lang.Long', InputResolutionStatus.RESOLVED,
                true, [], 'argument 0', node)
        new InputRecipe(InputRecipe.SCHEMA_VERSION, suppliedFingerprint, true, [], [argument])
    }

    private static WorkloadPlan withDeterministicId(WorkloadPlan plan) {
        new WorkloadPlan(
                plan.schemaVersion(),
                ScenarioIdGenerator.workloadPlanId(plan),
                plan.kind(),
                plan.executionShape(),
                plan.participants(),
                plan.acceptedInputs(),
                plan.forwardSchedule(),
                plan.conflictEvidence(),
                plan.faultSlots(),
                plan.compensationCheckpoints(),
                plan.warnings())
    }
}
