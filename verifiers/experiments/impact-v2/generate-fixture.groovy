#!/usr/bin/env groovy

import groovy.json.JsonOutput
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputVariantNormalizer
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleCap
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ExecutableArtifactWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence

import java.nio.file.Files
import java.nio.file.Path

if (args.length != 1) {
    throw new IllegalArgumentException('usage: generate-fixture.groovy OUTPUT_DIRECTORY')
}

Path output = Path.of(args[0]).toAbsolutePath().normalize()
Files.createDirectories(output)

final String providerId = 'quizzes-impact-v2-qualification'
final String providerVersion = '1'
final String uowService = 'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService'
final String uow = 'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork'
final String gateway = 'pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway'
final String removeSaga = 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas'
final String updateTournamentSaga = 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas'
final String updateQuestionSaga = 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateQuestionFunctionalitySagas'
final String tournamentDto = 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto'
final String questionDto = 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto'

def runtimeArgument = { int index, String type ->
    new InputRecipeArgument(index, type, InputResolutionStatus.UNRESOLVED, false, [],
            'ScenarioExecutor runtime-owned constructor argument',
            InputRecipeNode.builder('placeholder').executorReady(false).expectedTypeFqn(type).build())
}

def bindingArgument = { int index, String type, String key ->
    new InputRecipeArgument(index, type, InputResolutionStatus.RESOLVED, true, [],
            "qualification provider binding ${key}".toString(),
            InputRecipeNode.builder('baseline_binding').executorReady(true)
                    .bindingKey(key).bindingTypeFqn(type).expectedTypeFqn(type).build())
}

def inputFor = { String sagaFqn, List<InputRecipeArgument> arguments ->
    def recipe = new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], arguments)
    def raw = new InputVariant(null, sagaFqn,
            'qualification.fixture.ImpactV2', 'providerBackedPackage', 'qualificationInput',
            'providerBackedPackage', InputRole.FEATURE_UNDER_TEST, FixtureOrigin.SETUP_SPEC,
            InputResolutionStatus.RESOLVED, SourceMode.UNKNOWN, SourceModeConfidence.UNKNOWN,
            ['Intentionally source-independent qualification fixture'],
            "new ${sagaFqn.substring(sagaFqn.lastIndexOf('.') + 1)}(...)".toString(),
            'Qualification-only provider-backed input; not automatically source-extracted',
            [new InputOwner('qualification.fixture.ImpactV2', 'providerBackedPackage')],
            arguments.sort { it.index() }.collect { it.expectedTypeFqn() }, [:],
            ['QUALIFICATION_FIXTURE_NOT_SOURCE_EXTRACTED'], recipe)
    InputVariantNormalizer.normalizeForArtifact(raw)
}

def stepDefinition = { String sagaFqn, String name, int index, String predecessor,
                       CompensationEvidenceClass compensation ->
    String key = "${sagaFqn}::${name}#0".toString()
    def provisional = new StepDefinition(null, key, name, index,
            predecessor == null ? [] : ["${sagaFqn}::${predecessor}#0".toString()],
            [], [], compensation != null, true, true, compensation, [],
            ['Qualification fixture metadata mirrors the inspected runtime workflow'])
    new StepDefinition(ScenarioIdGenerator.stepDefinitionId(sagaFqn, provisional),
            provisional.stepKey(), provisional.name(), provisional.orderIndex(),
            provisional.predecessorStepKeys(), provisional.footprints(), provisional.compensationFootprints(),
            provisional.compensationRegistered(), provisional.forwardAnalysisComplete(),
            provisional.compensationAnalysisComplete(), provisional.compensationEvidence(),
            provisional.analysisDiagnostics(), provisional.warnings())
}

def createWorkload = { String label, String sagaFqn, InputVariant input,
                       List<Map<String, Object>> stepSpecs, Map<String, Object> eventSpec ->
    String participantId = ScenarioIdGenerator.sagaInstanceId(sagaFqn, input.deterministicId())
    def participant = new SagaInstance(participantId, sagaFqn, input.deterministicId(),
            ['qualification-only provider-backed participant'])
    List<StepDefinition> definitions = stepSpecs.withIndex().collect { spec, index ->
        stepDefinition(sagaFqn, spec.name as String, index, spec.predecessor as String,
                spec.compensation as CompensationEvidenceClass)
    }
    List<ScheduledStep> schedule = definitions.withIndex().collect { definition, index ->
        String occurrence = "${label}:${definition.name()}:0".toString()
        String scheduledId = ScenarioIdGenerator.scheduledStepId(participantId, definition.stepKey(), index)
        new ScheduledStep(scheduledId, participantId, definition.stepKey(), index, definition.name(),
                ["qualification occurrence ${occurrence}".toString()])
    }
    List<ForwardFaultSlot> slots = schedule.withIndex().collect { step, index ->
        new ForwardFaultSlot(ScenarioIdGenerator.forwardFaultSlotId(index, step), index,
                step.deterministicId(), participantId, step.stepId(), step.runtimeStepName(),
                step.deterministicId())
    }
    List<CompensationCheckpoint> checkpoints = []
    definitions.withIndex().each { definition, index ->
        if (definition.compensationEvidence() != null) {
            ScheduledStep step = schedule[index]
            int checkpointIndex = checkpoints.size()
            checkpoints << new CompensationCheckpoint(
                    ScenarioIdGenerator.compensationCheckpointId(checkpointIndex, step, definition),
                    checkpointIndex, participantId, step.deterministicId(), step.stepId(),
                    step.runtimeStepName(), step.deterministicId(),
                    definition.compensationEvidence(), [], [], [])
        }
    }

    List<EventConsequence> consequences = []
    List<NormalActionRef> normal = schedule.withIndex().collect { step, index ->
        NormalActionRef.forward(index, step.deterministicId())
    }
    if (eventSpec != null) {
        ScheduledStep trigger = schedule.find { it.runtimeStepName() == eventSpec.trigger }
        assert trigger != null
        def site = new EventEmissionSite(
                ScenarioIdGenerator.eventEmissionSiteId(eventSpec.service as String,
                        eventSpec.serviceMethod as String, 0, eventSpec.eventType as String),
                eventSpec.service as String, eventSpec.serviceMethod as String, 0,
                eventSpec.eventType as String,
                ['Qualification metadata for the inspected QuestionService registration site'])
        String consequenceId = ScenarioIdGenerator.eventConsequenceId(
                trigger.deterministicId(), site, eventSpec.handling as String,
                eventSpec.handlingMethod as String, eventSpec.handler as String,
                eventSpec.processing as String, eventSpec.processingMethod as String,
                eventSpec.facade as String, eventSpec.facadeMethod as String,
                eventSpec.downstream as String, eventSpec.policy as String)
        consequences << new EventConsequence(consequenceId, trigger.deterministicId(), site,
                eventSpec.eventType as String, eventSpec.handling as String,
                eventSpec.handlingMethod as String, eventSpec.handler as String,
                eventSpec.processing as String, eventSpec.processingMethod as String,
                eventSpec.facade as String, eventSpec.facadeMethod as String,
                eventSpec.downstream as String, eventSpec.policy as String,
                ['Qualification-only exact event route; not source-extracted by this fixture'])
        normal << NormalActionRef.eventConsequence(normal.size(), consequenceId)
    }

    List<BaselineBindingRequirement> requirements = input.inputRecipe().arguments()
            .findAll { it.recipe()?.kind() == 'baseline_binding' }
            .collect { new BaselineBindingRequirement(it.recipe().bindingKey(), it.expectedTypeFqn()) }
    def baseline = new PrerequisiteBaseline(providerId, providerVersion, requirements)
    def provisional = new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, null,
            ScenarioKind.SINGLE_SAGA, WorkloadExecutionShape.SAGA_LOCAL, [participant], [input],
            schedule, consequences, normal, baseline, null, [], slots, checkpoints,
            ['QUALIFICATION_FIXTURE_NOT_SOURCE_EXTRACTED', "qualification-family:${label}".toString()])
    def workload = new WorkloadPlan(provisional.schemaVersion(),
            ScenarioIdGenerator.workloadPlanId(provisional), provisional.kind(), provisional.executionShape(),
            provisional.participants(), provisional.acceptedInputs(), provisional.forwardSchedule(),
            provisional.eventConsequences(), provisional.normalSchedule(), provisional.prerequisiteBaseline(),
            provisional.setupPlan(), provisional.conflictEvidence(), provisional.faultSlots(),
            provisional.compensationCheckpoints(), provisional.warnings())
    [workload: workload, definitions: new SagaDefinition(sagaFqn, definitions,
            ['qualification fixture definition; inspected against Quizzes runtime source'])]
}

InputVariant removeInput = inputFor(removeSaga, [
        runtimeArgument(0, uowService),
        bindingArgument(1, Integer.name, 'tournamentAggregateId'),
        runtimeArgument(2, uow),
        runtimeArgument(3, gateway)
])
InputVariant tournamentInput = inputFor(updateTournamentSaga, [
        runtimeArgument(0, uowService),
        bindingArgument(1, tournamentDto, 'updatedTournament'),
        bindingArgument(2, Set.name, 'updatedTournamentTopicIds'),
        runtimeArgument(3, uow),
        runtimeArgument(4, gateway)
])
InputVariant questionInput = inputFor(updateQuestionSaga, [
        runtimeArgument(0, uowService),
        bindingArgument(1, questionDto, 'updatedQuestion'),
        runtimeArgument(2, uow),
        runtimeArgument(3, gateway)
])

def remove = createWorkload('deleted-dependency', removeSaga, removeInput, [
        [name: 'getTournamentStep', predecessor: null,
         compensation: CompensationEvidenceClass.IMPLICIT_SAGA_ROLLBACK],
        [name: 'removeQuizStep', predecessor: 'getTournamentStep', compensation: null],
        [name: 'removeTournamentStep', predecessor: 'removeQuizStep', compensation: null]
], null)

def updateTournament = createWorkload('failed-update-residual', updateTournamentSaga, tournamentInput, [
        [name: 'getOriginalTournamentStep', predecessor: null,
         compensation: CompensationEvidenceClass.IMPLICIT_SAGA_ROLLBACK],
        [name: 'getTopicsStep', predecessor: 'getOriginalTournamentStep', compensation: null],
        [name: 'updateTournamentStep', predecessor: 'getTopicsStep',
         compensation: CompensationEvidenceClass.EXPLICIT_COMPENSATION],
        [name: 'findQuestionsByTopicIds', predecessor: 'updateTournamentStep', compensation: null],
        [name: 'updateQuizStep', predecessor: 'findQuestionsByTopicIds', compensation: null]
], null)

def event = createWorkload('unresolved-question-event', updateQuestionSaga, questionInput, [
        [name: 'getQuestionStep', predecessor: null,
         compensation: CompensationEvidenceClass.EXPLICIT_COMPENSATION],
        [name: 'updateQuestionStep', predecessor: 'getQuestionStep', compensation: null]
], [
        trigger: 'updateQuestionStep',
        service: 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService',
        serviceMethod: 'updateQuestion(pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto,pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork)',
        eventType: 'pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateQuestionEvent',
        handling: 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling.QuizEventHandling',
        handlingMethod: 'handleUpdateQuestionEvent',
        handler: 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling.handlers.UpdateQuestionEventHandler',
        processing: 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.eventProcessing.QuizEventProcessing',
        processingMethod: 'processUpdateQuestionEvent',
        facade: 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities',
        facadeMethod: 'updateQuestionInQuiz',
        downstream: 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.UpdateQuestionInQuizFunctionalitySagas',
        policy: EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER
])

def fixtures = [remove, updateTournament, event]
def workloads = fixtures*.workload
def inputs = [removeInput, tournamentInput, questionInput]
EventConsequence eventConsequence = event.workload.eventConsequences().first()
def eventDefinitions = [new EventConsequenceDefinition(
        updateQuestionSaga, "${updateQuestionSaga}::updateQuestionStep#0".toString(),
        eventConsequence.emissionSite(), eventConsequence.eventHandlingClassFqn(),
        eventConsequence.eventHandlingMethodName(), eventConsequence.eventHandlerClassFqn(),
        eventConsequence.eventProcessingClassFqn(), eventConsequence.eventProcessingMethodName(),
        eventConsequence.facadeClassFqn(), eventConsequence.facadeMethodName(),
        eventConsequence.downstreamSagaFqn(), eventConsequence.deliveryPolicy(),
        eventConsequence.diagnostics())]
def sagaDefinitions = fixtures*.definitions + [new SagaDefinition(
        eventConsequence.downstreamSagaFqn(), [],
        ['Downstream event Saga is runtime-owned by the selected handler route'])]
def config = new ScenarioGeneratorConfig()
def workloadResult = new WorkloadGenerationResult(WorkloadPlan.SCHEMA_VERSION, config, workloads, [],
        [qualificationWorkloads: workloads.size()],
        ['Qualification-only package; inputs and route metadata are intentionally provider-backed'])
def generated = EagerFaultScenarioGenerator.generate(workloadResult, new RecoveryScheduleCap(20))
def blockedWorkloads = generated.workloadMaterializability().findAll { !it.materializable() }
assert blockedWorkloads.isEmpty(): "Qualification workload materializability failed: ${blockedWorkloads}"
def model = new ScenarioModelAdapterResult(sagaDefinitions, inputs, eventDefinitions, [],
        [qualificationInputs: inputs.size()],
        ['This package is a bounded qualification fixture and was not automatically source-extracted'])

def writer = new ExecutableArtifactWriter()
writer.write(model, 'quizzes-impact-v2-qualification', generated,
        output.resolve('scenario-catalog-manifest.json'),
        output.resolve('accounting.json'), output.resolve('sagas.jsonl'),
        output.resolve('inputs.jsonl'), output.resolve('interactions.jsonl'),
        output.resolve('setups.jsonl'), output.resolve('workloads.jsonl'),
        output.resolve('fault-scenarios.jsonl'), output.resolve('requests.jsonl'),
        '2026-09-06T00:00:00Z')

def select = { WorkloadPlan workload, String vector ->
    def matches = generated.faultScenarios().findAll {
        it.workloadPlanId() == workload.deterministicId() && it.assignedVector() == vector
    }.sort { it.deterministicId() }
    assert matches.size() == 1: "Expected one scenario for ${workload.deterministicId()} vector ${vector}, got ${matches.size()}"
    matches[0]
}

def selection = [
        schemaVersion: 'microservices-simulator.impact-v2-qualification-selection.v1',
        fixtureKind: 'qualification-only-provider-backed-not-source-extracted',
        generatedAt: '2026-09-06T00:00:00Z',
        provider: [id: providerId, version: providerVersion],
        cases: [
                deletedDependencyAssigned: [workloadPlanId: remove.workload.deterministicId(),
                        faultScenarioId: select(remove.workload, '001').deterministicId(), vector: '001'],
                deletedDependencyUnassigned: [workloadPlanId: remove.workload.deterministicId(),
                        faultScenarioId: select(remove.workload, '000').deterministicId(), vector: '000'],
                failedUpdateAssigned: [workloadPlanId: updateTournament.workload.deterministicId(),
                        faultScenarioId: select(updateTournament.workload, '00001').deterministicId(), vector: '00001'],
                failedUpdateUnassigned: [workloadPlanId: updateTournament.workload.deterministicId(),
                        faultScenarioId: select(updateTournament.workload, '00000').deterministicId(), vector: '00000'],
                unresolvedQuestionEvent: [workloadPlanId: event.workload.deterministicId(),
                        faultScenarioId: select(event.workload, '00').deterministicId(), vector: '00']
        ]
]
Files.writeString(output.resolve('selection.json'), JsonOutput.prettyPrint(JsonOutput.toJson(selection)) + '\n')
println "Wrote ImpactV2 qualification package to ${output}"
