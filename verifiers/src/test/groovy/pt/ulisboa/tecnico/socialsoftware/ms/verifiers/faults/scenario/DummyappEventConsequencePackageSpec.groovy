package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingCalculator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogJsonlWriter
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

    private final Path temporaryDirectory = Files.createTempDirectory('dummyapp-event-package-')

    def setupSpec() {
        configureParser()
    }

    def 'dummyapp extraction generates deterministic positive and control latest package records'() {
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
        eventPlans.size() == firstWorkloads.workloadPlans().count { it.eventConsequences().isEmpty() } * 2
        eventPlans*.deterministicId() == secondWorkloads.workloadPlans()
                .findAll { !it.eventConsequences().isEmpty() }*.deterministicId()
        eventPlans.collect { normalLabels(it) }.toSet() == [
                ['F:getOrderStep', 'E:ItemRenamedEvent', 'F:createItemStep'],
                ['F:getOrderStep', 'F:createItemStep', 'E:ItemRenamedEvent']
        ] as Set
        eventPlans.every { new WorkloadPlanValidator().validate(it).valid() }
        eventPlans.every { plan ->
            plan.schemaVersion() == 'microservices-simulator.workload-plan.v5' &&
                    plan.faultSlots().size() == plan.forwardSchedule().size() &&
                    plan.normalSchedule().size() == plan.forwardSchedule().size() + 1 &&
                    plan.eventConsequences().every { consequence ->
                        !plan.faultSlots()*.scheduledStepId().contains(consequence.deterministicId()) &&
                                !plan.compensationCheckpoints()*.sourceScheduledStepId().contains(consequence.deterministicId())
                    }
        }

        when:
        def eager = EagerFaultScenarioGenerator.generate(firstWorkloads, new RecoveryScheduleCap(20))
        def accounting = new ScenarioSpaceAccountingCalculator().calculate(
                'dummyapp', [producerSaga], producerInputs, config, firstWorkloads.workloadPlans().size())
        def paths = packagePaths(temporaryDirectory)
        def firstManifest = new ScenarioCatalogJsonlWriter().write(
                eager, paths.workload, paths.fault, paths.manifest, paths.rejected, paths.accounting,
                accounting, '2026-07-31T00:00:00Z')
        def firstBytes = paths.collectEntries { key, path -> [(key): Files.readAllBytes(path)] }
        def secondEager = EagerFaultScenarioGenerator.generate(secondWorkloads, new RecoveryScheduleCap(20))
        new ScenarioCatalogJsonlWriter().write(
                secondEager, paths.workload, paths.fault, paths.manifest, paths.rejected, paths.accounting,
                accounting, '2026-07-31T00:00:00Z')
        def contents = new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        paths.every { key, path -> Arrays.equals(firstBytes[key], Files.readAllBytes(path)) }
        firstManifest.schemaVersion() == 'microservices-simulator.scenario-catalog-manifest.v5'
        firstManifest.workloadCatalog().schemaVersion() == 'microservices-simulator.workload-plan.v5'
        firstManifest.faultScenarioCatalog().schemaVersion() == 'microservices-simulator.fault-scenario.v4'
        firstManifest.scenarioSpaceAccounting().schemaVersion() == 'microservices-simulator.scenario-space-accounting.v4'
        firstManifest.counts().eventConsequencesExported == eventPlans.size().toString()
        firstManifest.counts().normalActionsExported == firstWorkloads.workloadPlans()*.normalSchedule()*.size().sum().toString()
        contents.workloadPlans()*.deterministicId() == firstWorkloads.workloadPlans()*.deterministicId()
        contents.faultScenarios().findAll { scenario ->
            eventPlans*.deterministicId().contains(scenario.workloadPlanId()) && !scenario.assignedVector().contains('1')
        }.every { scenario ->
            scenario.schemaVersion() == 'microservices-simulator.fault-scenario.v4' &&
                    scenario.actions().count { it.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE } == 1 &&
                    scenario.actions().find { it.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE }.with {
                        sourceFaultSlotId() == null && sourceCompensationCheckpointId() == null &&
                                sourceEventConsequenceId() != null
                    }
        }
        contents.accounting().path('workloadCatalogSpace').path('eventConsequencesWritten').asText() ==
                eventPlans.size().toString()

        when: 'a linked v3 WorkloadPlan is substituted into the otherwise valid v4 package'
        def mapper = new ObjectMapper()
        def v3WorkloadText = new String(firstBytes.workload)
                .replaceFirst('microservices-simulator.workload-plan.v5', 'microservices-simulator.workload-plan.v3')
        Files.writeString(paths.workload, v3WorkloadText)
        def v3WorkloadManifest = mapper.readTree(firstBytes.manifest)
        v3WorkloadManifest.withObject('/workloadCatalog').put(
                'sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(paths.workload)))
        mapper.writerWithDefaultPrettyPrinter().writeValue(paths.manifest.toFile(), v3WorkloadManifest)
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def workloadSchemaFailure = thrown(IllegalArgumentException)
        workloadSchemaFailure.message.contains('latest v5 or explicit valid v4 packages are required')
        workloadSchemaFailure.message.contains('v3 catalogs are not supported')

        when: 'a linked v3 FaultScenario is substituted into the valid v4 package'
        Files.write(paths.workload, firstBytes.workload)
        def v3FaultText = new String(firstBytes.fault)
                .replaceFirst('microservices-simulator.fault-scenario.v4', 'microservices-simulator.fault-scenario.v3')
        Files.writeString(paths.fault, v3FaultText)
        def v3FaultManifest = mapper.readTree(firstBytes.manifest)
        v3FaultManifest.withObject('/faultScenarioCatalog').put(
                'sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(paths.fault)))
        mapper.writerWithDefaultPrettyPrinter().writeValue(paths.manifest.toFile(), v3FaultManifest)
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def faultSchemaFailure = thrown(IllegalArgumentException)
        faultSchemaFailure.message.contains('latest v5 or explicit valid v4 packages are required')
        faultSchemaFailure.message.contains('v3 catalogs are not supported')

        when: 'a checksum-valid v4 package contains an incomplete semantic event route'
        Files.write(paths.fault, firstBytes.fault)
        def malformedWorkloads = Files.readAllLines(paths.workload).collect { mapper.readTree(it) }
        def malformedEventWorkload = malformedWorkloads.find { !it.path('eventConsequences').isEmpty() }
        malformedEventWorkload.path('eventConsequences').get(0).path('emissionSite')
                .putNull('sourceServiceClassFqn')
        Files.write(paths.workload, malformedWorkloads.collect { mapper.writeValueAsString(it) })
        def malformedManifest = mapper.readTree(firstBytes.manifest)
        malformedManifest.withObject('/workloadCatalog').put(
                'sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(paths.workload)))
        mapper.writerWithDefaultPrettyPrinter().writeValue(paths.manifest.toFile(), malformedManifest)
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def malformedRouteFailure = thrown(IllegalArgumentException)
        malformedRouteFailure.message.contains('MALFORMED_EVENT_CONSEQUENCE')
    }

    def 'latest package reader rejects v3 manifest before reading linked artifacts'() {
        given:
        def manifest = temporaryDirectory.resolve('v3-manifest.json')
        Files.writeString(manifest, '{"schemaVersion":"microservices-simulator.scenario-catalog-manifest.v3"}')

        when:
        new ScenarioCatalogPackageReader().read(manifest)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('latest v5 or explicit valid v4 packages are required')
        failure.message.contains('v3 catalogs are not supported')
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

    private static Map<String, Path> packagePaths(Path directory) {
        [
                workload: directory.resolve('workload-catalog.jsonl'),
                fault: directory.resolve('fault-scenario-catalog.jsonl'),
                manifest: directory.resolve('scenario-catalog-manifest.json'),
                rejected: directory.resolve('workload-catalog-rejected-inputs.jsonl'),
                accounting: directory.resolve('scenario-space-accounting.json')
        ]
    }

    private static ApplicationAnalysisState dummyappState() {
        def state = new ApplicationAnalysisState()
        def files = parseAllDummyappFiles()
        def indexVisitor = new CommandHandlerIndexVisitor()
        def serviceVisitor = new ServiceVisitor()
        def commandHandlerVisitor = new CommandHandlerVisitor()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        def creationVisitor = new WorkflowFunctionalityCreationSiteVisitor()
        def bridgeVisitor = new EventHandlingBridgeVisitor()
        def consequenceVisitor = new EventConsequenceVisitor()
        files.each { indexVisitor.visit(it, state) }
        files.each { serviceVisitor.visit(it, state) }
        files.each { commandHandlerVisitor.visit(it, state) }
        files.each { workflowVisitor.visit(it, state) }
        files.each { creationVisitor.visit(it, state) }
        files.each { bridgeVisitor.visit(it, state) }
        bridgeVisitor.finish(state)
        files.each { consequenceVisitor.visit(it, state) }
        consequenceVisitor.finish(state)
        def sourceIndex = new GroovySourceIndex()
        sourceIndex.parse(resolveProjectPath('applications', 'dummyapp', 'src', 'test', 'groovy'))
        new GroovyConstructorInputTraceVisitor().visit(sourceIndex, state)
        state
    }
}
