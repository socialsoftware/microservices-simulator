package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import spock.lang.Specification

import java.nio.file.Files

class ScenarioExecutorSpec extends Specification {
    private static final ObjectMapper MAPPER = new ObjectMapper()

    def setup() {
        FixtureWorkflow.suppressFaultSignal = false
        FixtureWorkflow.injectUnexpectedSignal = false
        FixtureWorkflow.injectWrongSlotSignal = false
        FixtureWorkflow.compensationFails = false
    }

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
        participant(report).sagaFqn() == 'pt.example.EnrichedSaga'
        participant(report).stepOutcomes()*.runtimeStepName() == ['stepName', 'stepName']
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
        participant(first).stepOutcomes()*.runtimeStepName() == ['run', 'run']
        first.skippedCandidateCounts() == second.skippedCandidateCounts()
        first.skippedCandidateCounts()['UNSUPPORTED_SCENARIO_SHAPE'] == 1
        first.skippedCandidateCounts()['UNSUPPORTED_STEP_ID'] == 2
    }

    def 'fixture execution materializes recipes, calls scheduled steps in order, closes lifecycle, and clears provider'() {
        given:
        FixtureWorkflow.STEPS.clear()
        FixtureWorkflow.resumeCalls = 0
        FixtureWorkflow.compensationCalls = 0
        FaultVectorProviderHolder.clear()
        def runDirectory = Files.createTempDirectory('scenario-executor-fixture')
        def fixturePlan = plan('fixture-plan', FixtureWorkflow.name, 'fixture::second#0', 'fixture::first#0', literalArg('hello'))
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [fixturePlan])
        def output = runDirectory.resolve('reports/execution-report.json')

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, output, 'fixture-plan', null, false, 'quizzes', 'quizzes', 'com.example.Application', 'test,sagas,local', 'test-sagas'), runtime())

        then:
        report.schemaVersion() == 'microservices-simulator.scenario-execution-report.v3'
        report.terminalStatus() == 'SUCCESS'
        report.participants().size() == 1
        participant(report).sagaInstanceId() == 'fixture-plan-saga'
        participant(report).sagaFqn() == FixtureWorkflow.name
        participant(report).inputVariantId() == 'fixture-plan-input'
        participant(report).materializationState() == 'MATERIALIZED'
        participant(report).startupState() == 'STARTUP_READY'
        participant(report).lifecycleOutcome() == 'COMMITTED'
        report.providerMode() == 'IN_MEMORY_FAULT_VECTOR'
        report.vectorSource() == 'DEFAULT_VECTOR'
        report.assignedVector() == '00'
        report.selectionMode() == 'EXPLICIT'
        report.runtimeMetadata().applicationBase() == 'quizzes'
        report.runtimeMetadata().applicationId() == 'quizzes'
        report.runtimeMetadata().springApplicationClass() == 'com.example.Application'
        report.runtimeMetadata().springProfiles() == 'test,sagas,local'
        report.runtimeMetadata().mavenProfile() == 'test-sagas'
        report.runtimeMetadata().applicationBase() != participant(report).sagaFqn()
        participant(report).stepOutcomes()*.runtimeStepName() == ['first', 'second']
        participant(report).stepOutcomes()*.status() == ['COMPLETED', 'COMPLETED']
        report.faultSlots()*.realizationState() == ['NOT_ASSIGNED', 'NOT_ASSIGNED']
        FixtureWorkflow.STEPS == ['first', 'second']
        FixtureWorkflow.resumeCalls == 1
        FixtureWorkflow.compensationCalls == 0
        !FaultVectorProviderHolder.active
        Files.exists(output)
        def json = MAPPER.readTree(output.toFile())
        json.get('schemaVersion').asText() == 'microservices-simulator.scenario-execution-report.v3'
        json.get('participants').size() == 1
        json.get('participants').get(0).get('lifecycleOutcome').asText() == 'COMMITTED'
        !json.has('sagaInstanceId')
        !json.has('sagaFqn')
        !json.has('inputVariantId')
        !json.has('lifecycleOutcome')
        !json.has('stepOutcomes')
    }

    def 'assigned fault realizes before target body, compensates, masks later assigned slots, and clears provider'() {
        given:
        FixtureWorkflow.STEPS.clear()
        FixtureWorkflow.resumeCalls = 0
        FixtureWorkflow.compensationCalls = 0
        FaultVectorProviderHolder.clear()
        def runDirectory = Files.createTempDirectory('scenario-executor-realized-fault')
        def sagaId = 'fault-plan-saga'
        def steps = [
                new ScheduledStep('fault-plan-step-1', sagaId, 'fixture::first#0', 0, []),
                new ScheduledStep('fault-plan-step-2', sagaId, 'fixture::second#0', 1, []),
                new ScheduledStep('fault-plan-step-3', sagaId, 'fixture::third#0', 2, [])
        ]
        def scenario = planWithStepsAndFaultSpace('fault-plan', steps,
                new FaultSpace(3, ['fault-plan-step-1', 'fault-plan-step-2', 'fault-plan-step-3'], '010'))
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [scenario])
        def output = runDirectory.resolve('reports/execution-report.json')

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, output, 'fault-plan', false), runtime())

        then:
        report.terminalStatus() == 'COMPENSATED'
        !report.toString().contains('FAULT_COMPENSATED')
        participant(report).lifecycleOutcome() == 'COMPENSATED'
        report.providerMode() == 'IN_MEMORY_FAULT_VECTOR'
        participant(report).stepOutcomes()*.runtimeStepName() == ['first', 'second']
        participant(report).stepOutcomes()*.status() == ['COMPLETED', 'INJECTED_FAULT']
        participant(report).stepOutcomes()[1].exceptionClass() == 'pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException'
        participant(report).stepOutcomes()[1].exceptionMessage().contains('slot 1')
        report.faultSlots()*.realizationState() == ['NOT_ASSIGNED', 'REALIZED', 'NOT_ASSIGNED']
        report.faultSlots()[1].scheduledStepId() == 'fault-plan-step-2'
        FixtureWorkflow.STEPS == ['first']
        FixtureWorkflow.resumeCalls == 0
        FixtureWorkflow.compensationCalls == 1
        !FaultVectorProviderHolder.active
        MAPPER.readTree(output.toFile()).get('terminalStatus').asText() == 'COMPENSATED'
    }

    def 'multiple assigned faults report later assigned slots as masked after first realized fault'() {
        given:
        FixtureWorkflow.STEPS.clear()
        FixtureWorkflow.compensationCalls = 0
        FaultVectorProviderHolder.clear()
        def runDirectory = Files.createTempDirectory('scenario-executor-masked-fault')
        def sagaId = 'masked-plan-saga'
        def steps = [
                new ScheduledStep('masked-plan-step-1', sagaId, 'fixture::first#0', 0, []),
                new ScheduledStep('masked-plan-step-2', sagaId, 'fixture::second#0', 1, []),
                new ScheduledStep('masked-plan-step-3', sagaId, 'fixture::third#0', 2, [])
        ]
        def scenario = planWithStepsAndFaultSpace('masked-plan', steps,
                new FaultSpace(3, ['masked-plan-step-1', 'masked-plan-step-2', 'masked-plan-step-3'], '011'))
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [scenario])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'masked-plan', false), runtime())

        then:
        report.terminalStatus() == 'COMPENSATED'
        report.faultSlots()*.realizationState() == ['NOT_ASSIGNED', 'REALIZED', 'MASKED_BY_SAGA_FAILURE']
        report.faultSlots()[2].maskReason().contains('earlier realized slot 1')
        participant(report).stepOutcomes()*.runtimeStepName() == ['first', 'second']
        FixtureWorkflow.STEPS == ['first']
        FixtureWorkflow.compensationCalls == 1
        !FaultVectorProviderHolder.active
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

    def 'runtime step failure at zero bit reports unexpected execution failure and compensates best effort'() {
        given:
        FixtureWorkflow.STEPS.clear()
        FixtureWorkflow.compensationCalls = 0
        FaultVectorProviderHolder.clear()
        def runDirectory = Files.createTempDirectory('scenario-executor-step-failure')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [plan('failure-plan', FixtureWorkflow.name, 'fixture::later#0', 'fixture::fail#0', literalArg('hello'))])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'failure-plan', false), runtime())

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        participant(report).lifecycleOutcome() == 'COMPENSATED'
        participant(report).stepOutcomes()*.runtimeStepName() == ['fail']
        participant(report).stepOutcomes()[0].exceptionClass() == IllegalStateException.name
        report.blockers()*.reason() == ['UNEXPECTED_EXECUTION_FAILURE']
        report.blockers()[0].message().contains('fixture failure')
        FixtureWorkflow.STEPS == ['fail']
        FixtureWorkflow.compensationCalls == 1
        !FaultVectorProviderHolder.active
    }

    def 'assigned fault that reaches the body without injected signal reports expected fault not injected'() {
        given:
        FixtureWorkflow.STEPS.clear()
        FaultVectorProviderHolder.clear()
        FixtureWorkflow.suppressFaultSignal = true
        def runDirectory = Files.createTempDirectory('scenario-executor-expected-not-injected')
        def scenario = planWithFaultSpace('missing-fault', new FaultSpace(2, ['missing-fault-step-1', 'missing-fault-step-2'], '10'))
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [scenario])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'missing-fault', false), runtime())

        then:
        report.terminalStatus() == 'EXPECTED_FAULT_NOT_INJECTED'
        participant(report).lifecycleOutcome() == 'CLOSURE_SKIPPED'
        report.faultSlots()*.realizationState() == ['EXPECTED_FAULT_NOT_INJECTED', 'NOT_ASSIGNED']
        participant(report).stepOutcomes()*.status() == ['EXPECTED_FAULT_NOT_INJECTED']
        report.blockers()*.reason() == ['EXPECTED_FAULT_NOT_INJECTED']
        FixtureWorkflow.STEPS == ['first']
        !FaultVectorProviderHolder.active
    }

    def 'injected signal at zero bit reports unexpected injected fault'() {
        given:
        FixtureWorkflow.STEPS.clear()
        FaultVectorProviderHolder.clear()
        FixtureWorkflow.injectUnexpectedSignal = true
        def runDirectory = Files.createTempDirectory('scenario-executor-unexpected-injected-fault')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [planWithFaultSpace('unexpected-injected', new FaultSpace(2, ['unexpected-injected-step-1', 'unexpected-injected-step-2'], '00'))])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'unexpected-injected', false), runtime())

        then:
        report.terminalStatus() == 'UNEXPECTED_INJECTED_FAULT'
        participant(report).stepOutcomes()*.status() == ['INJECTED_FAULT']
        report.faultSlots()*.realizationState() == ['NOT_ASSIGNED', 'NOT_ASSIGNED']
        report.blockers()*.reason() == ['UNEXPECTED_INJECTED_FAULT']
        FixtureWorkflow.STEPS.isEmpty()
        !FaultVectorProviderHolder.active
    }

    def 'injected signal with wrong identity reports provider mismatch'() {
        given:
        FixtureWorkflow.STEPS.clear()
        FaultVectorProviderHolder.clear()
        FixtureWorkflow.injectWrongSlotSignal = true
        def runDirectory = Files.createTempDirectory('scenario-executor-provider-mismatch')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [planWithFaultSpace('provider-mismatch', new FaultSpace(2, ['provider-mismatch-step-1', 'provider-mismatch-step-2'], '10'))])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'provider-mismatch', false), runtime())

        then:
        report.terminalStatus() == 'FAULT_PROVIDER_MISMATCH'
        participant(report).stepOutcomes()*.status() == ['INJECTED_FAULT']
        report.blockers()*.reason() == ['FAULT_PROVIDER_MISMATCH']
        FixtureWorkflow.STEPS.isEmpty()
        !FaultVectorProviderHolder.active
    }

    def 'compensation failure preserves forward and compensation exception details and clears provider'() {
        given:
        FixtureWorkflow.STEPS.clear()
        FixtureWorkflow.compensationCalls = 0
        FaultVectorProviderHolder.clear()
        FixtureWorkflow.compensationFails = true
        def runDirectory = Files.createTempDirectory('scenario-executor-compensation-failure')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [planWithFaultSpace('compensation-failed', new FaultSpace(2, ['compensation-failed-step-1', 'compensation-failed-step-2'], '10'))])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'compensation-failed', false), runtime())

        then:
        report.terminalStatus() == 'COMPENSATION_FAILED'
        participant(report).lifecycleOutcome() == 'COMPENSATION_FAILED'
        participant(report).stepOutcomes()*.status() == ['INJECTED_FAULT']
        report.blockers()*.reason() == ['FORWARD_FAILURE', 'COMPENSATION_FAILED']
        report.blockers()[0].message().contains('FaultVectorInjectedFaultException')
        report.blockers()[1].message().contains('fixture compensation failure')
        FixtureWorkflow.compensationCalls == 1
        !FaultVectorProviderHolder.active
    }

    def 'explicit fault vector requires explicit scenario id before execution and writes v3 invalid report'() {
        given:
        FixtureWorkflow.STEPS.clear()
        def runDirectory = Files.createTempDirectory('scenario-executor-explicit-vector-no-id')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [plan('fixture-plan', FixtureWorkflow.name, 'fixture::first#0')])
        def output = runDirectory.resolve('reports/execution-report.json')

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, output, null, '1', true, 'quizzes', 'quizzes', 'com.example.Application', 'test,sagas,local', 'test-sagas'), runtime())

        then:
        report.schemaVersion() == ScenarioExecutionReport.SCHEMA_VERSION
        report.scenarioExecutionId()
        report.terminalStatus() == 'INVALID_FAULT_VECTOR'
        report.participants().isEmpty()
        report.assignedVector() == '1'
        report.vectorSource() == 'EXPLICIT_VECTOR'
        report.providerMode() == 'NONE'
        report.runtimeMetadata().dryRun()
        report.runtimeMetadata().applicationBase() == 'quizzes'
        report.runtimeMetadata().applicationId() == 'quizzes'
        report.runtimeMetadata().springApplicationClass() == 'com.example.Application'
        report.runtimeMetadata().springProfiles() == 'test,sagas,local'
        report.runtimeMetadata().mavenProfile() == 'test-sagas'
        report.faultSlots().isEmpty()
        report.blockers()*.reason() == ['MISSING_EXPLICIT_SCENARIO_ID']
        FixtureWorkflow.STEPS.isEmpty()
        MAPPER.readTree(output.toFile()).get('terminalStatus').asText() == 'INVALID_FAULT_VECTOR'
    }

    def 'fault vector validation rejects malformed vectors and fault spaces before execution'() {
        given:
        FixtureWorkflow.STEPS.clear()
        def runDirectory = Files.createTempDirectory('scenario-executor-invalid-vector')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [scenarioPlan])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, scenarioPlan.deterministicId(), explicitVector, true), runtime())

        then:
        report.terminalStatus() == 'INVALID_FAULT_VECTOR'
        report.providerMode() == 'NONE'
        participant(report).lifecycleOutcome() == 'NOT_STARTED'
        report.assignedVector() == expectedVector
        report.runtimeMetadata().dryRun()
        report.blockers()*.reason().contains(expectedReason)
        FixtureWorkflow.STEPS.isEmpty()

        where:
        scenarioPlan                                                                  | explicitVector || expectedReason                              | expectedVector
        planWithFaultSpace('wrong-length', new FaultSpace(2, ['wrong-length-step-1', 'wrong-length-step-2'], '00')) | '1'            || 'INVALID_EXPLICIT_VECTOR'                    | '1'
        planWithFaultSpace('non-binary', new FaultSpace(2, ['non-binary-step-1', 'non-binary-step-2'], '00'))       | '10x'          || 'INVALID_EXPLICIT_VECTOR'                    | '10x'
        planWithFaultSpace('bad-default', new FaultSpace(2, ['bad-default-step-1', 'bad-default-step-2'], '0x'))    | null           || 'INVALID_DEFAULT_VECTOR'                     | '0x'
        planWithFaultSpace('length-mismatch', new FaultSpace(3, ['length-mismatch-step-1', 'length-mismatch-step-2'], '000')) | null || 'FAULT_SPACE_LENGTH_MISMATCH'       | '000'
        planWithFaultSpace('duplicate-ids', new FaultSpace(2, ['duplicate-ids-step-1', 'duplicate-ids-step-1'], '00')) | null        || 'DUPLICATE_FAULT_SPACE_SCHEDULED_STEP_ID' | '00'
        planWithFaultSpace('unresolved', new FaultSpace(1, ['missing-step'], '0'))                                      | null           || 'UNRESOLVED_FAULT_SPACE_SCHEDULED_STEP_ID' | '0'
        planWithStepsAndFaultSpace('non-unique', [new ScheduledStep('dup-step', 'non-unique-saga', 'fixture::first#0', 0, []), new ScheduledStep('dup-step', 'non-unique-saga', 'fixture::second#0', 1, [])], new FaultSpace(1, ['dup-step'], '0')) | null || 'NON_UNIQUE_FAULT_SLOT_MAPPING' | '0'
        planWithFaultSpace('empty-invalid', new FaultSpace(1, ['empty-invalid-step-1'], '0'))                         | ''             || 'INVALID_EXPLICIT_VECTOR'                    | ''
    }

    def 'zero length fault space accepts empty vector'() {
        given:
        def runDirectory = Files.createTempDirectory('scenario-executor-zero-vector')
        writeJsonl(runDirectory.resolve('scenario-catalog.jsonl'), [planWithStepsAndFaultSpace('zero-plan', [], new FaultSpace(0, [], ''))])

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'zero-plan', '', true), runtime())

        then:
        report.terminalStatus() == 'DRY_RUN'
        participant(report).materializationState() == 'NOT_ATTEMPTED'
        participant(report).startupState() == 'NOT_ATTEMPTED'
        participant(report).lifecycleOutcome() == 'NOT_STARTED'
        report.assignedVector() == ''
        report.faultSlots().isEmpty()
        participant(report).stepOutcomes().isEmpty()
    }

    def 'dry run expands vector mapping and preserves input artifacts without execution'() {
        given:
        FixtureWorkflow.STEPS.clear()
        def runDirectory = Files.createTempDirectory('scenario-executor-vector-dry-run')
        def scenario = planWithFaultSpace('dry-vector', new FaultSpace(2, ['dry-vector-step-1', 'dry-vector-step-2'], '01'))
        def catalog = runDirectory.resolve('scenario-catalog.jsonl')
        writeJsonl(catalog, [scenario])
        def before = Files.readString(catalog)
        def output = runDirectory.resolve('reports/execution-report.json')

        when:
        def explicit = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, output, 'dry-vector', '10', true), runtime())
        def afterExplicit = Files.readString(catalog)
        def defaulted = new ScenarioExecutor().execute(new ScenarioExecutorOptions(runDirectory, null, null, 'dry-vector', true), runtime())

        then:
        explicit.terminalStatus() == 'DRY_RUN'
        explicit.assignedVector() == '10'
        explicit.vectorSource() == 'EXPLICIT_VECTOR'
        explicit.providerMode() == 'NONE'
        explicit.faultSlots()*.slotIndex() == [0, 1]
        explicit.faultSlots()*.scheduledStepId() == ['dry-vector-step-1', 'dry-vector-step-2']
        explicit.faultSlots()*.runtimeStepName() == ['first', 'second']
        explicit.faultSlots()*.assignedBit() == [1, 0]
        explicit.faultSlots()*.realizationState() == ['DRY_RUN', 'NOT_ASSIGNED']
        participant(explicit).stepOutcomes()*.status() == ['DRY_RUN', 'DRY_RUN']
        explicit.runtimeMetadata().scenarioPlanId() == 'dry-vector'
        FixtureWorkflow.STEPS.isEmpty()
        afterExplicit == before
        MAPPER.readTree(output.toFile()).get('faultSlots').size() == 2
        defaulted.assignedVector() == '01'
        defaulted.vectorSource() == 'DEFAULT_VECTOR'
    }

    def 'CLI exit code helper treats dry-run success and compensated faults as zero only'() {
        expect:
        ScenarioExecutorCli.exitCodeFor(status) == code

        where:
        status                    || code
        'SUCCESS'                 || 0
        'COMPENSATED'             || 0
        'PARTIAL_COMPENSATED'     || 0
        'FAULT_COMPENSATED'       || 1
        'DRY_RUN'                 || 0
        'INVALID_FAULT_VECTOR'    || 1
        'UNSUPPORTED_SCENARIO'    || 1
        'COMPENSATION_FAILED'     || 1
    }

    private static ScenarioExecutionReport.Participant participant(ScenarioExecutionReport report) {
        assert report.participants().size() == 1
        report.participants()[0]
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

    private static ScenarioPlan planWithFaultSpace(String id, FaultSpace faultSpace) {
        def sagaId = "${id}-saga".toString()
        planWithStepsAndFaultSpace(id,
                [new ScheduledStep("${id}-step-1".toString(), sagaId, 'fixture::first#0', 0, []),
                 new ScheduledStep("${id}-step-2".toString(), sagaId, 'fixture::second#0', 1, [])],
                faultSpace)
    }

    private static ScenarioPlan planWithStepsAndFaultSpace(String id, List<ScheduledStep> steps, FaultSpace faultSpace) {
        def input = inputVariant("${id}-input".toString(), FixtureWorkflow.name, literalArg('value'))
        def saga = new SagaInstance("${id}-saga".toString(), FixtureWorkflow.name, input.deterministicId(), [])
        def normalizedSteps = steps.collect { new ScheduledStep(it.deterministicId(), saga.deterministicId(), it.stepId(), it.scheduleOrder(), it.warnings()) }
        new ScenarioPlan(ScenarioPlan.SCHEMA_VERSION, id, ScenarioKind.SINGLE_SAGA, [saga], [input], normalizedSteps, faultSpace, [], [])
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
