package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequenceDefinition
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
        current.sagaFacts().findAll { saga ->
            saga.path('fqn').asText() == PRODUCER_SAGA
        }.collectMany { saga -> saga.path('steps').toList() }
                .collectMany { step -> step.path('eventRoutes').toList() }
                .every { route ->
                    route.path('eventHandlingClass').asText() ==
                            'com.example.dummyapp.item.notification.handling.DummyEventHandling' &&
                            route.path('handler').asText() ==
                            'com.example.dummyapp.item.notification.handling.handlers.ItemRenamedEventHandler'
                }
        execution.workloadPlans().findAll { !it.eventConsequences().isEmpty() }.every { workload ->
            workload.eventConsequences().every { event ->
                event.eventHandlingClassFqn() ==
                        'com.example.dummyapp.item.notification.handling.DummyEventHandling' &&
                        event.eventHandlerClassFqn() ==
                        'com.example.dummyapp.item.notification.handling.handlers.ItemRenamedEventHandler'
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

    def 'selected dummyapp consumer keeps its package route when other routes are unselected or reordered'() {
        given:
        def original = new ApplicationAnalysisScenarioModelAdapter().adapt(dummyappState())
        def first = original.eventConsequenceDefinitions().find { it.triggerSagaFqn() == PRODUCER_SAGA }
        def second = alternateRoute(first, true)
        def producer = original.sagaDefinitions().find { it.sagaFqn() == PRODUCER_SAGA }
        def inputs = original.inputVariants().findAll { it.sagaFqn() == PRODUCER_SAGA }
        def config = routeConfig()
        def generation = ScenarioGenerator.generate([producer], inputs, [second], config)
        def eager = EagerFaultScenarioGenerator.generate(generation, new RecoveryScheduleCap(1))
        def directories = [Files.createTempDirectory('dummyapp-route-subset-'),
                           Files.createTempDirectory('dummyapp-route-reordered-')]

        when:
        [[first, second], [second, first]].eachWithIndex { routes, index ->
            new ExecutableArtifactWriter().write(withRoutes(original, routes), 'dummyapp', eager, directories[index])
        }
        def reader = new ScenarioCatalogPackageReader()
        def current = reader.readCurrent(directories.first().resolve('scenario-catalog-manifest.json'))
        def restored = reader.readCurrentForExecution(directories.first().resolve('scenario-catalog-manifest.json'))
        def events = restored.workloadPlans().collectMany { it.eventConsequences() }

        then:
        !events.isEmpty()
        events.every { it.eventHandlingClassFqn() == second.eventHandlingClassFqn() }
        events.every { it.eventHandlerClassFqn() == second.eventHandlerClassFqn() }
        current.workloadRecords().collectMany { it.path('schedule').toList() }
                .findAll { it.path('kind').asText() == 'event' }
                .every { it.path('route').asText().endsWith('-route#1') }
        current.manifest().files().values().every { artifact ->
            Arrays.equals(Files.readAllBytes(directories[0].resolve(artifact.path())),
                    Files.readAllBytes(directories[1].resolve(artifact.path())))
        }
    }

    def 'dummyapp event export rejects a missing or ambiguous persisted route'() {
        given:
        def original = new ApplicationAnalysisScenarioModelAdapter().adapt(dummyappState())
        def first = original.eventConsequenceDefinitions().find { it.triggerSagaFqn() == PRODUCER_SAGA }
        def second = alternateRoute(first, false)
        def producer = original.sagaDefinitions().find { it.sagaFqn() == PRODUCER_SAGA }
        def inputs = original.inputVariants().findAll { it.sagaFqn() == PRODUCER_SAGA }
        def generation = ScenarioGenerator.generate([producer], inputs, [first], routeConfig())
        def eager = EagerFaultScenarioGenerator.generate(generation, new RecoveryScheduleCap(1))
        def model = withRoutes(original, ambiguous ? [first, second] : [])

        when:
        new ExecutableArtifactWriter().write(model, 'dummyapp', eager,
                Files.createTempDirectory('dummyapp-route-invalid-'))

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('no unique exact Saga route')
        failure.message.contains('matches=' + expectedMatches)

        where:
        ambiguous | expectedMatches
        false     | 0
        true      | 2
    }

    private static ScenarioGeneratorConfig routeConfig() {
        new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                true, 1, 100, 10, 20, false,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 1234L)
    }

    private static ScenarioModelAdapterResult withRoutes(def original, List<EventConsequenceDefinition> routes) {
        new ScenarioModelAdapterResult(original.sagaDefinitions(), original.inputVariants(), routes,
                original.sourceSetupPlanBindings(), original.counts(), original.diagnostics(),
                original.dispatchesBySaga(), original.aggregateKeyInputEvidence())
    }

    private static EventConsequenceDefinition alternateRoute(EventConsequenceDefinition first, boolean distinctHandler) {
        new EventConsequenceDefinition(first.triggerSagaFqn(), first.triggerStepKey(), first.emissionSite(),
                distinctHandler ? 'com.example.dummyapp.item.notification.handling.ZOtherEventHandling' : first.eventHandlingClassFqn(),
                first.eventHandlingMethodName(),
                distinctHandler ? 'com.example.dummyapp.item.notification.handling.handlers.ZOtherEventHandler' : first.eventHandlerClassFqn(),
                first.eventProcessingClassFqn() + 'Other', first.eventProcessingMethodName(),
                first.facadeClassFqn(), first.facadeMethodName(), first.downstreamSagaFqn(),
                first.deliveryPolicy(), [])
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
