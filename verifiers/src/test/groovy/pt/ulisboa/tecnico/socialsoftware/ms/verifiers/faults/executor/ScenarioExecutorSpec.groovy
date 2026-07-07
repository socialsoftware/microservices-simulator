package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import spock.lang.Specification

import java.nio.file.Files

class ScenarioExecutorSpec extends Specification {
    private static final ObjectMapper MAPPER = new ObjectMapper()

    def 'dry run prefers enriched catalog, unwraps scenario plans, validates explicit ids, and derives deterministic steps'() {
        given:
        def runDirectory = Files.createTempDirectory('scenario-executor-dry-run')
        def staticPlan = plan('static-plan', 'pt.example.StaticSaga', 'com.example.StaticSaga::ignored#0')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [staticPlan])
        def enrichedPlan = plan('enriched-plan', 'pt.example.EnrichedSaga', 'com.example.EnrichedSaga::stepName#0')
        writeJsonl(runDirectory.resolve('scenario-catalog-enriched.jsonl'), [enriched(enrichedPlan, DynamicEvidenceJoinStatus.MATCHED_EXACT)])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'enriched-plan', true), runtime())

        then:
        report.catalogKind() == 'ENRICHED'
        report.scenarioPlanId() == 'enriched-plan'
        report.sagaFqn() == 'pt.example.EnrichedSaga'
        report.stepOutcomes()*.runtimeStepName() == ['stepName', 'stepName']
        report.terminalStatus() == 'DRY_RUN'

        when:
        def missing = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'missing-plan', true), runtime())

        then:
        missing.terminalStatus() == 'SELECTION_FAILED'
        missing.blockers()[0].reason() == 'MISSING_SCENARIO_PLAN_ID'
        missing.blockers()[0].message() == 'missing-plan'
    }

    def 'dry run falls back to static catalog and records unsupported shape and step id reasons deterministically'() {
        given:
        def runDirectory = Files.createTempDirectory('scenario-executor-static')
        def multi = multiPlan('multi-plan')
        def invalid = plan('invalid-step', 'pt.example.Saga', 'no-delimiter')
        def valid = plan('valid-plan', 'pt.example.Saga', 'pt.example.Saga::run#12')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [multi, invalid, valid])

        when:
        def first = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, null, true), runtime())
        def second = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, null, true), runtime())

        then:
        first.catalogKind() == 'STATIC'
        first.scenarioPlanId() == 'valid-plan'
        first.stepOutcomes()*.runtimeStepName() == ['run', 'run']
        first.skippedCandidateCounts() == second.skippedCandidateCounts()
        first.skippedCandidateCounts()['UNSUPPORTED_SCENARIO_SHAPE'] == 1
        first.skippedCandidateCounts()['UNSUPPORTED_STEP_ID'] == 2
    }

    def 'fixture execution materializes recipes, calls scheduled steps in order, and never resumes workflow'() {
        given:
        FixtureWorkflow.STEPS.clear()
        FixtureWorkflow.resumeCalls = 0
        def runDirectory = Files.createTempDirectory('scenario-executor-fixture')
        def fixturePlan = plan('fixture-plan', FixtureWorkflow.name, 'fixture::second#0', 'fixture::first#0', literalArg('hello'))
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [fixturePlan])
        def output = runDirectory.resolve('reports/execution-report.json')

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, output, 'fixture-plan', false), runtime())

        then:
        report.terminalStatus() == 'SUCCESS'
        report.selectionMode() == 'EXPLICIT'
        report.stepOutcomes()*.runtimeStepName() == ['first', 'second']
        FixtureWorkflow.STEPS == ['first', 'second']
        FixtureWorkflow.resumeCalls == 0
        Files.exists(output)
    }

    def 'runtime owned SagaUnitOfWorkService CommandGateway and SagaUnitOfWork arguments are allowed narrowly'() {
        given:
        def runDirectory = Files.createTempDirectory('scenario-executor-runtime-owned')
        def args = [
                arg(0, 'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService', InputRecipeNode.builder('placeholder').expectedTypeFqn('pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService').executorReady(false).build()),
                arg(1, 'pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway', InputRecipeNode.builder('placeholder').expectedTypeFqn('pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway').executorReady(false).build()),
                arg(2, 'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork', InputRecipeNode.builder('call_result').executorReady(false).build())
        ]
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [planWithArgs('runtime-owned-plan', FixtureWorkflow.name, args)])

        expect:
        new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'runtime-owned-plan', false), runtime()).terminalStatus() == 'SUCCESS'
    }

    def 'DTO constructor assignments collections helper result property access and blockers are observable through execution reports'() {
        given:
        def runDirectory = Files.createTempDirectory('scenario-executor-materialization')
        def dtoRecipe = constructorNode(FixtureDto.name,
                [arg(0, 'java.lang.String', literalNode('course'))],
                [new InputRecipeAssignment('setter', 'count', null, 0, 'count=3', true, [], literalNode(3)),
                 new InputRecipeAssignment('property', 'tags', null, 1, 'tags=[]', true, [], collectionNode('list', [literalNode('a'), literalNode('b')]))])
        def helperList = InputRecipeNode.builder('helper_result').executorReady(true).resultRecipe(collectionNode('list', [literalNode('a'), literalNode('b')])).build()
        def setRecipe = InputRecipeNode.builder('local_transform').executorReady(true).transformName('toSet').receiver(helperList).build()
        def propertyRecipe = InputRecipeNode.builder('property_access').propertyName('name').receiver(dtoRecipe).executorReady(true).build()
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [plan('dto-plan', FixtureWorkflow.name, 'fixture::first#0', 'fixture::first#0', recipeArg(propertyRecipe))])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'dto-plan', false), runtime())

        then:
        report.terminalStatus() == 'SUCCESS'

        when:
        def badRun = Files.createTempDirectory('scenario-executor-bad-materialization')
        writeJsonl(badRun.resolve('scenario-catalog.jsonl'), [plan('bad-plan', FixtureWorkflow.name, 'fixture::first#0', 'fixture::first#0', recipeArg(InputRecipeNode.builder('constructor').executorReady(true).build()))])
        def bad = new ScenarioExecutor().execute(new ScenarioExecutorOptions(badRun, null, null, 'bad-plan', false), runtime())

        then:
        bad.terminalStatus() == 'MATERIALIZATION_FAILED'
        bad.blockers()*.reason().contains('MISSING_TARGET_TYPE')

        when:
        def setRun = Files.createTempDirectory('scenario-executor-set-materialization')
        writeJsonl(setRun.resolve('scenario-catalog.jsonl'), [plan('set-plan', FixtureWorkflow.name, 'fixture::first#0', 'fixture::first#0', recipeArg(setRecipe))])

        then:
        new ScenarioExecutor().execute(new ScenarioExecutorOptions(setRun, null, null, 'set-plan', false), runtime()).terminalStatus() == 'SUCCESS'
    }

    def 'unsupported call results placeholders transforms property receivers and unresolved values are structured blockers'() {
        expect:
        blockerReason(InputRecipeNode.builder('call_result').executorReady(true).build()) == 'UNSUPPORTED_CALL_RESULT'
        blockerReason(InputRecipeNode.builder('placeholder').executorReady(true).expectedTypeFqn('java.lang.String').build()) == 'UNRESOLVED_PLACEHOLDER'
        blockerReason(InputRecipeNode.builder('local_transform').executorReady(true).transformName('reverse').receiver(collectionNode('list', [literalNode('a')])).build()) == 'UNSUPPORTED_TRANSFORM'
        blockerReason(InputRecipeNode.builder('property_access').executorReady(true).propertyName('name').receiver(InputRecipeNode.builder('unresolved').executorReady(false).build()).build()) == 'UNMATERIALIZABLE_RECEIVER'
        blockerReason(InputRecipeNode.builder('unresolved').executorReady(false).build()) == 'UNRESOLVED_VALUE'
    }

    def 'runtime step failure stops later steps and records exception details'() {
        given:
        FixtureWorkflow.STEPS.clear()
        def runDirectory = Files.createTempDirectory('scenario-executor-step-failure')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [plan('failure-plan', FixtureWorkflow.name, 'fixture::later#0', 'fixture::fail#0', literalArg('hello'))])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'failure-plan', false), runtime())

        then:
        report.terminalStatus() == 'STEP_EXECUTION_FAILED'
        report.stepOutcomes()*.runtimeStepName() == ['fail']
        report.stepOutcomes()[0].exceptionClass() == IllegalStateException.name
        FixtureWorkflow.STEPS == ['fail']
    }

    private static ScenarioRuntimeContext runtime() {
        new ScenarioRuntimeContext() {
            @Override
            Object bean(Class<?> type) { null }
        }
    }

    private static void writeJsonl(java.nio.file.Path path, List<Object> records) {
        Files.createDirectories(path.parent)
        Files.write(path, records.collect { MAPPER.writeValueAsString(it) })
    }

    private static EnrichedScenarioRecord enriched(ScenarioPlan plan, DynamicEvidenceJoinStatus status) {
        new EnrichedScenarioRecord(EnrichedScenarioRecord.SCHEMA_VERSION, plan.deterministicId(), plan,
                new DynamicEvidenceSummary(status, null, [], [], [], [], [], []))
    }

    private static ScenarioPlan plan(String id, String sagaFqn, String stepId) {
        plan(id, sagaFqn, stepId, stepId, literalArg('value'))
    }

    private static ScenarioPlan plan(String id, String sagaFqn, String firstStep, String secondStep, InputRecipeArgument argument) {
        planWithArgs(id, sagaFqn, [argument], firstStep, secondStep)
    }

    private static ScenarioPlan planWithArgs(String id, String sagaFqn, List<InputRecipeArgument> arguments) {
        planWithArgs(id, sagaFqn, arguments, 'fixture::first#0', 'fixture::first#0')
    }

    private static ScenarioPlan planWithArgs(String id, String sagaFqn, List<InputRecipeArgument> arguments, String firstStep, String secondStep) {
        def input = inputVariant("${id}-input".toString(), sagaFqn, arguments)
        def saga = new SagaInstance("${id}-saga".toString(), sagaFqn, input.deterministicId(), [])
        new ScenarioPlan(ScenarioPlan.SCHEMA_VERSION, id, ScenarioKind.SINGLE_SAGA, [saga], [input],
                [new ScheduledStep("${id}-step-2".toString(), saga.deterministicId(), firstStep, 1, []),
                 new ScheduledStep("${id}-step-1".toString(), saga.deterministicId(), secondStep, 0, [])], null, [], [])
    }

    private static ScenarioPlan multiPlan(String id) {
        new ScenarioPlan(ScenarioPlan.SCHEMA_VERSION, id, ScenarioKind.MULTI_SAGA,
                [new SagaInstance('left', 'LeftSaga', 'left-input', []), new SagaInstance('right', 'RightSaga', 'right-input', [])],
                [inputVariant('left-input', 'LeftSaga', literalArg('left')), inputVariant('right-input', 'RightSaga', literalArg('right'))],
                [new ScheduledStep('left-step', 'left', 'LeftSaga::run#0', 0, [])], null, [], [])
    }

    private static InputVariant inputVariant(String id, String sagaFqn, InputRecipeArgument argument) {
        inputVariant(id, sagaFqn, [argument])
    }

    private static InputVariant inputVariant(String id, String sagaFqn, List<InputRecipeArgument> arguments) {
        new InputVariant(id, sagaFqn, 'Fixture', 'build', 'fixture', InputResolutionStatus.RESOLVED,
                SourceMode.UNKNOWN, SourceModeConfidence.UNKNOWN, [], 'source', 'provenance', [], ['arg'], [:], [],
                new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], arguments))
    }

    private static String blockerReason(InputRecipeNode node) {
        def runDirectory = Files.createTempDirectory('scenario-executor-blocker')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [plan('blocker-plan', FixtureWorkflow.name, 'fixture::first#0', 'fixture::first#0', recipeArg(node))])
        new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'blocker-plan', false), runtime()).blockers()[0].reason()
    }

    private static InputRecipeArgument literalArg(Object value) {
        arg(0, 'java.lang.Object', literalNode(value))
    }

    private static InputRecipeArgument recipeArg(InputRecipeNode node) {
        arg(0, 'java.lang.Object', node)
    }

    private static InputRecipeArgument arg(int index, String type, InputRecipeNode node) {
        new InputRecipeArgument(index, type, InputResolutionStatus.RESOLVED, true, [], 'arg', node)
    }

    private static InputRecipeNode literalNode(Object value) {
        InputRecipeNode.builder('literal').executorReady(true).literalKind('value').value(value).build()
    }

    private static InputRecipeNode constructorNode(String type, List<InputRecipeArgument> args, List<InputRecipeAssignment> assignments) {
        InputRecipeNode.builder('constructor').executorReady(true).targetTypeFqn(type).arguments(args).assignments(assignments).build()
    }

    private static InputRecipeNode collectionNode(String kind, List<InputRecipeNode> elements) {
        InputRecipeNode.builder('collection').executorReady(true).collectionKind(kind).elements(elements).build()
    }
}
