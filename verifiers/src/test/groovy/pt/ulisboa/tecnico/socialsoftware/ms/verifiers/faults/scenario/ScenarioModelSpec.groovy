package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultSpace
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioGenerationResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig.InputPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig.ScheduleStrategy
import spock.lang.Specification

import java.util.ArrayList
import java.util.LinkedHashMap

class ScenarioModelSpec extends Specification {

    def 'model records defensively copy mutable collections'() {
        given:
        def footprintWarnings = new ArrayList<>(['footprint-warning'])
        def footprint = new StepFootprint(
                new AggregateKey('com.example.Order', 'Order', 'order-1', FootprintConfidence.EXACT),
                AccessMode.WRITE,
                footprintWarnings
        )

        def stepPredecessors = new ArrayList<>(['com.example.Saga::init'])
        def stepFootprints = new ArrayList<>([footprint])
        def stepWarnings = new ArrayList<>(['step-warning'])
        def stepDefinition = new StepDefinition(
                'step-1',
                'com.example.Saga::placeOrder',
                'placeOrder',
                0,
                stepPredecessors,
                stepFootprints,
                stepWarnings
        )

        def sagaWarnings = new ArrayList<>(['saga-warning'])
        def sagaSteps = new ArrayList<>([stepDefinition])
        def sagaDefinition = new SagaDefinition('com.example.Saga', sagaSteps, sagaWarnings)

        def constructorArgs = new ArrayList<>(['customerId'])
        def logicalBindings = new LinkedHashMap<>([customerId: 'customer-1'])
        def variantWarnings = new ArrayList<>(['variant-warning'])
        def inputVariant = new InputVariant(
                'variant-1',
                'com.example.Saga',
                'com.example.SagaTest',
                'sagaField',
                null,
                InputResolutionStatus.REPLAYABLE,
                'source text',
                'provenance text',
                constructorArgs,
                logicalBindings,
                variantWarnings
        )

        def instanceWarnings = new ArrayList<>(['instance-warning'])
        def sagaInstance = new SagaInstance('instance-1', 'com.example.Saga', 'variant-1', instanceWarnings)

        def scheduledWarnings = new ArrayList<>(['scheduled-warning'])
        def scheduledSteps = new ArrayList<>([
                new ScheduledStep('schedule-1', 'instance-1', 'step-1', 0, scheduledWarnings),
                new ScheduledStep('schedule-2', 'instance-1', 'step-2', 1, new ArrayList<>(['other-warning']))
        ])

        def conflictWarnings = new ArrayList<>(['conflict-warning'])
        def conflictEvidence = new ConflictEvidence(
                'conflict-1',
                'schedule-1',
                'schedule-2',
                new AggregateKey('com.example.Order', 'Order', 'order-1', FootprintConfidence.EXACT),
                new AggregateKey('com.example.Order', 'Order', 'order-1', FootprintConfidence.EXACT),
                AccessMode.WRITE,
                AccessMode.READ,
                ConflictKind.WRITE_READ,
                conflictWarnings
        )

        def planSagaInstances = new ArrayList<>([sagaInstance])
        def planInputs = new ArrayList<>([inputVariant])
        def planSchedule = new ArrayList<>(scheduledSteps)
        def planConflicts = new ArrayList<>([conflictEvidence])
        def planWarnings = new ArrayList<>(['plan-warning'])
        def plan = new ScenarioPlan(
                ScenarioPlan.SCHEMA_VERSION,
                'scenario-1',
                ScenarioKind.SINGLE_SAGA,
                planSagaInstances,
                planInputs,
                planSchedule,
                null,
                planConflicts,
                planWarnings
        )

        def resultCounts = new LinkedHashMap<>([emitted: 1])
        def resultWarnings = new ArrayList<>(['result-warning'])
        def generationResult = new ScenarioGenerationResult(
                ScenarioPlan.SCHEMA_VERSION,
                new ScenarioGeneratorConfig(),
                new ArrayList<>([plan]),
                resultCounts,
                resultWarnings
        )

        def manifestCounts = new LinkedHashMap<>([emitted: 1])
        def manifestWarnings = new ArrayList<>(['manifest-warning'])
        def manifest = new ScenarioCatalogManifest(
                ScenarioPlan.SCHEMA_VERSION,
                '2026-04-27T00:00:00Z',
                new ScenarioGeneratorConfig(),
                manifestCounts,
                manifestWarnings,
                '/tmp/scenario-catalog.jsonl',
                '/tmp/scenario-catalog-manifest.json'
        )

        when:
        footprintWarnings << 'mutated'
        stepPredecessors << 'mutated'
        stepFootprints << new StepFootprint(null, null, [])
        stepWarnings << 'mutated'
        sagaSteps << new StepDefinition('step-2', 'com.example.Saga::cancel', 'cancel', 1, [], [], [])
        sagaWarnings << 'mutated'
        constructorArgs << 'mutated'
        logicalBindings.customerId = 'customer-2'
        variantWarnings << 'mutated'
        instanceWarnings << 'mutated'
        scheduledWarnings << 'mutated'
        scheduledSteps << new ScheduledStep('schedule-3', 'instance-1', 'step-3', 2, [])
        conflictWarnings << 'mutated'
        planSagaInstances << new SagaInstance('instance-2', 'com.example.Saga', 'variant-2', [])
        planInputs << new InputVariant('variant-2', 'com.example.Saga', 'com.example.SagaTest', 'sagaField', null, InputResolutionStatus.RESOLVED, 'source', 'provenance', [], [:], [])
        planSchedule << new ScheduledStep('schedule-4', 'instance-1', 'step-4', 3, [])
        planConflicts << new ConflictEvidence('conflict-2', 'schedule-2', 'schedule-3', null, null, AccessMode.READ, AccessMode.WRITE, ConflictKind.UNKNOWN, [])
        planWarnings << 'mutated'
        resultCounts.emitted = 2
        resultWarnings << 'mutated'
        manifestCounts.emitted = 2
        manifestWarnings << 'mutated'

        then:
        footprint.warnings() == ['footprint-warning']
        stepDefinition.predecessorStepKeys() == ['com.example.Saga::init']
        stepDefinition.footprints().size() == 1
        stepDefinition.warnings() == ['step-warning']
        sagaDefinition.steps().size() == 1
        sagaDefinition.warnings() == ['saga-warning']
        inputVariant.constructorArgumentSummaries() == ['customerId']
        inputVariant.logicalKeyBindings() == [customerId: 'customer-1']
        inputVariant.warnings() == ['variant-warning']
        sagaInstance.warnings() == ['instance-warning']
        conflictEvidence.warnings() == ['conflict-warning']
        plan.sagaInstances().size() == 1
        plan.inputs().size() == 1
        plan.expandedSchedule().size() == 2
        plan.conflictEvidence().size() == 1
        plan.warnings() == ['plan-warning']
        plan.faultSpace().length() == 2
        plan.faultSpace().defaultVector() == '00'
        plan.faultSpace().scheduledStepIds() == ['schedule-1', 'schedule-2']
        generationResult.scenarioPlans().size() == 1
        generationResult.counts() == [emitted: 1]
        generationResult.warnings() == ['result-warning']
        manifest.counts() == [emitted: 1]
        manifest.warnings() == ['manifest-warning']
        manifest.catalogPath() == '/tmp/scenario-catalog.jsonl'
        manifest.manifestPath() == '/tmp/scenario-catalog-manifest.json'
    }

    def 'default scenario generator config is bounded and single-saga first'() {
        when:
        def config = new ScenarioGeneratorConfig()

        then:
        !config.exportEnabled()
        config.includeSingles()
        config.maxSagaSetSize() == 1
        config.maxScenarios() == 100
        config.maxInputVariantsPerSaga() == 3
        config.maxSchedulesPerInputTuple() == 20
        !config.allowTypeOnlyFallback()
        config.inputPolicy() == InputPolicy.RESOLVED_OR_REPLAYABLE
        config.scheduleStrategy() == ScheduleStrategy.SERIAL
        config.deterministicSeed() == 1234L
    }

    def 'scenario fault space maps one binary slot per scheduled step'() {
        when:
        def schedule = new ArrayList<>([
                new ScheduledStep('schedule-1', 'instance-1', 'step-1', 0, []),
                new ScheduledStep('schedule-2', 'instance-1', 'step-2', 1, [])
        ])
        def plan = new ScenarioPlan(
                ScenarioPlan.SCHEMA_VERSION,
                'scenario-1',
                ScenarioKind.SINGLE_SAGA,
                [new SagaInstance('instance-1', 'com.example.Saga', 'variant-1', [])],
                [new InputVariant('variant-1', 'com.example.Saga', 'com.example.SagaTest', 'sagaField', null, InputResolutionStatus.RESOLVED, 'source', 'provenance', [], [:], [])],
                schedule,
                null,
                [],
                []
        )
        schedule << new ScheduledStep('schedule-3', 'instance-1', 'step-3', 2, [])

        then:
        plan.expandedSchedule().size() == 2
        plan.faultSpace().length() == 2
        plan.faultSpace().defaultVector() == '00'
    }
}
