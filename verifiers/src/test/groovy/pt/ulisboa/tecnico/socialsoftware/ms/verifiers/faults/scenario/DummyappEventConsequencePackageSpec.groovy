package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ExecutableArtifactWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.NormalActionKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.*

import java.nio.file.Files
import java.nio.file.Path

class DummyappEventConsequencePackageSpec extends VisitorTestSupport {
    private static final String PRODUCER_SAGA =
            'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'

    def setupSpec() { configureParser() }

    def 'dummyapp extraction writes deterministic current event routes workloads and faults'() {
        given:
        def model = new ApplicationAnalysisScenarioModelAdapter().adapt(dummyappState())
        def producerSaga = model.sagaDefinitions().find { it.sagaFqn() == PRODUCER_SAGA }
        def producerInputs = model.inputVariants().findAll { it.sagaFqn() == PRODUCER_SAGA }
        def producerDefinitions = model.eventConsequenceDefinitions().findAll {
            it.triggerSagaFqn() == PRODUCER_SAGA
        }
        def config = new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                true, 1, 100, 10, 20, false,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 1234L)

        when:
        def firstWorkloads = ScenarioGenerator.generate(
                [producerSaga], producerInputs, producerDefinitions, config)
        def secondWorkloads = ScenarioGenerator.generate(
                [producerSaga], producerInputs, producerDefinitions.reverse(), config)
        def eventPlans = firstWorkloads.workloadPlans().findAll { !it.eventConsequences().isEmpty() }

        then:
        producerDefinitions.size() == 1
        producerSaga != null
        producerInputs
        eventPlans*.deterministicId() == secondWorkloads.workloadPlans()
                .findAll { !it.eventConsequences().isEmpty() }*.deterministicId()
        eventPlans.collect { normalLabels(it) }.toSet() == [
                ['F:getOrderStep', 'E:ItemRenamedEvent', 'F:createItemStep'],
                ['F:getOrderStep', 'F:createItemStep', 'E:ItemRenamedEvent']
        ] as Set
        eventPlans.every { new WorkloadPlanValidator().validate(it).valid() }

        when:
        def eager = EagerFaultScenarioGenerator.generate(firstWorkloads, new RecoveryScheduleCap(20))
        Path firstDirectory = Files.createTempDirectory('dummyapp-current-event-first-')
        Path secondDirectory = Files.createTempDirectory('dummyapp-current-event-second-')
        new ExecutableArtifactWriter().write(model, 'dummyapp', eager, firstDirectory)
        def secondEager = EagerFaultScenarioGenerator.generate(secondWorkloads, new RecoveryScheduleCap(20))
        new ExecutableArtifactWriter().write(model, 'dummyapp', secondEager, secondDirectory)
        def firstManifest = firstDirectory.resolve('scenario-catalog-manifest.json')
        def current = new ScenarioCatalogPackageReader().readCurrent(firstManifest)
        def execution = new ScenarioCatalogPackageReader().readCurrentForExecution(firstManifest)

        then:
        current.manifest().files().keySet() ==
                ['accounting', 'sagas', 'inputs', 'interactions', 'setups', 'workloads',
                 'faultScenarios', 'requests'] as Set
        current.manifest().files().every { role, artifact ->
            Arrays.equals(Files.readAllBytes(firstDirectory.resolve(artifact.path())),
                    Files.readAllBytes(secondDirectory.resolve(
                            current.manifest().files().get(role).path())))
        }
        def eventWorkloads = current.workloadRecords().findAll { record ->
            record.path('schedule').any { it.path('kind').asText() == 'event' }
        }
        eventWorkloads.size() == eventPlans.size()
        eventWorkloads.every { workload ->
            workload.path('schedule').findAll { it.path('kind').asText() == 'event' }.every { event ->
                event.has('route') && event.has('triggeringStep') && event.size() == 4
            }
        }
        execution.faultScenarios().findAll { scenario ->
            execution.workloadPlans().find { it.deterministicId() == scenario.workloadPlanId() }
                    ?.eventConsequences()
        }.every { scenario ->
            scenario.actions().count { it.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE } == 1
        }
        current.accounting().path('events').path('resolvedEventRoutes').asInt() > 0
    }

    private static List<String> normalLabels(def plan) {
        def steps = plan.forwardSchedule().collectEntries { [(it.deterministicId()): it.runtimeStepName()] }
        def consequences = plan.eventConsequences().collectEntries {
            [(it.deterministicId()): it.eventTypeFqn().tokenize('.').last()]
        }
        plan.normalSchedule().collect { action ->
            action.kind() == NormalActionKind.FORWARD
                    ? "F:${steps[action.scheduledStepId()]}".toString()
                    : "E:${consequences[action.eventConsequenceId()]}".toString()
        }
    }

    private static ApplicationAnalysisState dummyappState() {
        def state = new ApplicationAnalysisState()
        def files = parseAllDummyappFiles()
        def visitors = [new CommandHandlerIndexVisitor(), new ServiceVisitor(),
                        new CommandHandlerVisitor(), new WorkflowFunctionalityVisitor(),
                        new WorkflowFunctionalityCreationSiteVisitor()]
        visitors.each { visitor -> files.each { visitor.visit(it, state) } }
        def bridge = new EventHandlingBridgeVisitor()
        files.each { bridge.visit(it, state) }
        bridge.finish(state)
        def consequence = new EventConsequenceVisitor()
        files.each { consequence.visit(it, state) }
        consequence.finish(state)
        def sourceIndex = new GroovySourceIndex()
        sourceIndex.parse(resolveProjectPath('applications', 'dummyapp', 'src', 'test', 'groovy'))
        new GroovyConstructorInputTraceVisitor().visit(sourceIndex, state)
        state
    }
}
