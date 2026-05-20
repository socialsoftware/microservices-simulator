package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export.EnrichedScenarioCatalogWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceEvent
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceReadResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.EnrichedScenarioRecord
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogJsonlWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioGenerationResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerIndexVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.GroovyConstructorInputTraceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.ServiceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.VisitorTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityCreationSiteVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityVisitor
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class DummyappDynamicEnrichmentIntegrationSpec extends VisitorTestSupport {

    private static final String GENERATED_AT = '2026-05-01T00:00:00Z'
    private static final String DYNAMIC_EVIDENCE_SCHEMA = 'microservices-simulator.dynamic-evidence.v1'
    private static final String DUMMYAPP_APPLICATION_NAME = 'dummyapp'
    private static final String ITEM_SAGA_FQN = 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
    private static final Map<String, Object> EFFECTIVE_DYNAMIC_ENRICHMENT_CONFIG = [
            enabled              : true,
            dynamicEvidenceSubdir: 'dynamic-evidence'
    ]

    private final ObjectMapper mapper = new ObjectMapper()

    @TempDir
    Path tempDir

    def 'dummyapp dynamic enrichment keeps matched, missing-context, and not-covered statuses distinct'() {
        given:
        def fixture = buildFixture()
        def currentThreadName = Thread.currentThread().name
        def functionalityName = simpleName(fixture.selectedSaga().fqn)
        def runtimeStepName = fixture.selectedStep().name
        def unitOfWorkVersion = 90L
        def functionalityInvocationId = "${functionalityName}-${unitOfWorkVersion}"
        def runRoot = tempDir.resolve('dummyapp-dynamic-enrichment')
        def staticCatalogPath = runRoot.resolve('scenario-catalog.jsonl')
        def staticManifestPath = runRoot.resolve('scenario-catalog-manifest.json')
        def staticRejectedInputsPath = runRoot.resolve('scenario-catalog-rejected-inputs.jsonl')
        def enrichedCatalogPath = runRoot.resolve('scenario-catalog-enriched.jsonl')
        def enrichedManifestPath = runRoot.resolve('scenario-catalog-enriched-manifest.json')
        def joinReportPath = runRoot.resolve('dynamic-evidence-join-report.json')
        def positiveEvidenceRoot = runRoot.resolve('dynamic-evidence')
        def positiveEvidenceFile = positiveEvidenceRoot
                .resolve(fixture.selectedInput().sourceClassFqn())
                .resolve('dynamic-evidence.jsonl')
        def exactEvidenceRoot = runRoot.resolve('dynamic-evidence-direct-input')
        def exactEvidenceFile = exactEvidenceRoot
                .resolve(fixture.selectedInput().sourceClassFqn())
                .resolve('dynamic-evidence.jsonl')
        def exactEnrichedCatalogPath = runRoot.resolve('direct/scenario-catalog-enriched.jsonl')
        def exactEnrichedManifestPath = runRoot.resolve('direct/scenario-catalog-enriched-manifest.json')
        def exactJoinReportPath = runRoot.resolve('direct/dynamic-evidence-join-report.json')
        def emptyEvidenceRoot = runRoot.resolve('dynamic-evidence-empty')
        def emptyEnrichedCatalogPath = runRoot.resolve('empty/scenario-catalog-enriched.jsonl')
        def emptyEnrichedManifestPath = runRoot.resolve('empty/scenario-catalog-enriched-manifest.json')
        def emptyJoinReportPath = runRoot.resolve('empty/dynamic-evidence-join-report.json')
        def staticWriter = new ScenarioCatalogJsonlWriter()
        def enrichedWriter = new EnrichedScenarioCatalogWriter()

        when:
        staticWriter.write(fixture.scenarioResult(), staticCatalogPath, staticManifestPath, staticRejectedInputsPath, GENERATED_AT)
        Files.createDirectories(positiveEvidenceFile.parent)
        writeJsonl(positiveEvidenceFile, positiveEvidenceEvents(fixture, currentThreadName))
        DynamicEvidenceReadResult positiveRead = new DynamicEvidenceReader().read(positiveEvidenceRoot)
        DynamicEvidenceJoinResult positiveJoin = new DynamicEvidenceJoiner().join(
                fixture.scenarioResult().scenarioPlans(),
                positiveRead.events(),
                positiveRead.evidenceFilesRead(),
                positiveRead.warnings())
        enrichedWriter.write(
                positiveJoin,
                enrichedCatalogPath,
                enrichedManifestPath,
                joinReportPath,
                staticCatalogPath.toString(),
                positiveEvidenceRoot.toString(),
                EFFECTIVE_DYNAMIC_ENRICHMENT_CONFIG,
                [],
                GENERATED_AT)

        then:
        Files.exists(staticCatalogPath)
        Files.exists(staticManifestPath)
        Files.exists(staticRejectedInputsPath)
        Files.readAllLines(staticCatalogPath).size() == fixture.scenarioResult().scenarioPlans().size()
        mapper.readTree(Files.readString(staticManifestPath)).path('counts').path('scenariosExported').asInt() == fixture.scenarioResult().scenarioPlans().size()

        and:
        positiveRead.evidenceFilesRead() == 1
        positiveRead.events().size() == 5
        positiveRead.events()*.eventKind() == ['STEP_STARTED', 'COMMAND_SENT', 'AGGREGATE_ACCESSED', 'STEP_FINISHED', 'STEP_STARTED']
        positiveRead.events().every { event -> assertRealSchemaFields(event, currentThreadName, event.lineNumber(), unitOfWorkVersion, functionalityName, functionalityInvocationId, runtimeStepName) }
        positiveRead.events().first().sourcePath() == positiveEvidenceFile
        positiveRead.events().first().lineNumber() == 1
        positiveRead.events().first().testClassFqn() == fixture.selectedInput().sourceClassFqn()
        positiveRead.events().first().testMethodName() == fixture.selectedInput().sourceMethodName()
        positiveRead.events().first().testDisplayName() == fixture.selectedInput().sourceMethodName()
        positiveRead.events().first().testUniqueId() == "${fixture.selectedInput().sourceClassFqn()}#${fixture.selectedInput().sourceMethodName()}"
        positiveRead.events().first().payloadText('stepPhase') == 'FORWARD'
        positiveRead.events()[1].payloadText('commandType') == simpleName(fixture.selectedDispatch().commandTypeFqn())
        positiveRead.events()[1].payloadText('commandFqn') == fixture.selectedDispatch().commandTypeFqn()
        positiveRead.events()[1].payloadText('serviceName') == fixture.selectedDispatch().aggregateName()
        positiveRead.events()[1].payloadText('rootAggregateId') == '7'
        positiveRead.events()[1].payloadMap('fields').orderAggregateId == 7
        positiveRead.events()[2].payloadText('aggregateType') == fixture.selectedDispatch().aggregateName()
        positiveRead.events()[2].payloadText('aggregateId') == '7'
        positiveRead.events()[2].payloadText('accessMode') == fixture.selectedDispatch().accessPolicy().name()
        positiveRead.events()[2].payloadText('sourceMethod') == aggregateAccessSourceMethod(fixture.selectedDispatch())
        positiveRead.events()[3].payloadText('outcome') == 'SUCCESS'
        (positiveRead.events()[3].payloadValue('durationMillis') as Long) >= 0L
        positiveRead.events().last().testClassFqn() == null
        positiveRead.events().last().testMethodName() == null
        positiveRead.events().last().testDisplayName() == null
        positiveRead.events().last().testUniqueId() == null
        positiveRead.warnings().isEmpty()

        and:
        def positiveRecord = positiveJoin.records().find { record -> record.scenarioPlanId() == fixture.selectedPlan().deterministicId() }
        positiveRecord != null
        positiveRecord.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE
        positiveRecord.dynamicEvidence().matchedInputVariantIds() == [fixture.selectedInput().deterministicId()]
        positiveRecord.dynamicEvidence().observedSteps().size() == 1
        positiveRecord.dynamicEvidence().observedSteps()[0].stepName() == runtimeStepName
        positiveRecord.dynamicEvidence().observedSteps()[0].eventKinds() == ['STEP_STARTED', 'COMMAND_SENT', 'AGGREGATE_ACCESSED', 'STEP_FINISHED']
        positiveRecord.dynamicEvidence().observedSteps()[0].outcomes() == ['SUCCESS']
        positiveRecord.dynamicEvidence().observedAggregateAccesses().size() == 1
        positiveRecord.dynamicEvidence().observedAggregateAccesses()[0].aggregateType() == fixture.selectedDispatch().aggregateName()
        positiveRecord.dynamicEvidence().observedAggregateAccesses()[0].accessMode() == fixture.selectedDispatch().accessPolicy().name()
        positiveRecord.dynamicEvidence().observedAggregateAccesses()[0].sourceMethod() == aggregateAccessSourceMethod(fixture.selectedDispatch())
        positiveRecord.dynamicEvidence().observedAggregateAccesses()[0].sourceEventIds() == ['positive-aggregate-accessed']
        positiveRecord.dynamicEvidence().observedCommands().size() == 1
        positiveRecord.dynamicEvidence().observedCommands()[0].commandType() == simpleName(fixture.selectedDispatch().commandTypeFqn())
        positiveRecord.dynamicEvidence().observedCommands()[0].commandFqn() == fixture.selectedDispatch().commandTypeFqn()
        positiveRecord.dynamicEvidence().observedCommands()[0].serviceName() == fixture.selectedDispatch().aggregateName()
        positiveRecord.dynamicEvidence().observedCommands()[0].rootAggregateId() == '7'
        positiveRecord.dynamicEvidence().observedCommands()[0].sourceEventIds() == ['positive-command-sent']
        positiveRecord.dynamicEvidence().warnings().isEmpty()
        positiveRecord.scenarioPlan() == fixture.selectedPlan()
        positiveRecord.schemaVersion() == EnrichedScenarioRecord.SCHEMA_VERSION

        and:
        def enrichedRecord = readEnrichedRecord(enrichedCatalogPath, fixture.selectedPlan().deterministicId())
        enrichedRecord.path('schemaVersion').asText() == EnrichedScenarioRecord.SCHEMA_VERSION
        enrichedRecord.path('scenarioPlanId').asText() == fixture.selectedPlan().deterministicId()
        def enrichedScenarioPlan = enrichedRecord.path('scenarioPlan')
        enrichedScenarioPlan.path('schemaVersion').asText() == ScenarioPlan.SCHEMA_VERSION
        enrichedScenarioPlan.path('deterministicId').asText() == fixture.selectedPlan().deterministicId()
        enrichedScenarioPlan.path('inputs').get(0).path('deterministicId').asText() == fixture.selectedInput().deterministicId()
        enrichedScenarioPlan.path('inputs').get(0).path('inputRecipe').path('schemaVersion').asText() == fixture.selectedInput().inputRecipe().schemaVersion()
        enrichedScenarioPlan.path('inputs').get(0).path('inputRecipe').path('recipeFingerprint').asText() == fixture.selectedInput().inputRecipe().recipeFingerprint()
        enrichedScenarioPlan.path('inputs').get(0).path('inputRecipe').path('executorReady').asBoolean() == fixture.selectedInput().inputRecipe().executorReady()
        enrichedRecord.path('dynamicEvidence').path('joinStatus').asText() == DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE.name()
        enrichedRecord.path('dynamicEvidence').path('matchedInputVariantIds').collect { it.asText() } == [fixture.selectedInput().deterministicId()]
        enrichedRecord.path('dynamicEvidence').path('observedSteps').get(0).path('eventKinds').collect { it.asText() } == ['STEP_STARTED', 'COMMAND_SENT', 'AGGREGATE_ACCESSED', 'STEP_FINISHED']
        enrichedRecord.path('dynamicEvidence').path('observedSteps').get(0).path('outcomes').collect { it.asText() } == ['SUCCESS']
        enrichedRecord.path('dynamicEvidence').path('observedAggregateAccesses').get(0).path('aggregateType').asText() == fixture.selectedDispatch().aggregateName()
        enrichedRecord.path('dynamicEvidence').path('observedAggregateAccesses').get(0).path('accessMode').asText() == fixture.selectedDispatch().accessPolicy().name()
        enrichedRecord.path('dynamicEvidence').path('observedAggregateAccesses').get(0).path('sourceMethod').asText() == aggregateAccessSourceMethod(fixture.selectedDispatch())
        enrichedRecord.path('dynamicEvidence').path('observedAggregateAccesses').get(0).path('sourceEventIds').collect { it.asText() } == ['positive-aggregate-accessed']
        enrichedRecord.path('dynamicEvidence').path('observedCommands').get(0).path('commandType').asText() == simpleName(fixture.selectedDispatch().commandTypeFqn())
        enrichedRecord.path('dynamicEvidence').path('observedCommands').get(0).path('commandFqn').asText() == fixture.selectedDispatch().commandTypeFqn()
        enrichedRecord.path('dynamicEvidence').path('observedCommands').get(0).path('serviceName').asText() == fixture.selectedDispatch().aggregateName()
        enrichedRecord.path('dynamicEvidence').path('observedCommands').get(0).path('rootAggregateId').asText() == '7'
        enrichedRecord.path('dynamicEvidence').path('observedCommands').get(0).path('sourceEventIds').collect { it.asText() } == ['positive-command-sent']

        and:
        def positiveManifest = mapper.readTree(Files.readString(enrichedManifestPath))
        positiveManifest.path('schema').asText() == EnrichedScenarioCatalogWriter.MANIFEST_SCHEMA
        assertJoinStatusCounts(positiveManifest.path('counts'), positiveJoin)
        positiveManifest.path('counts').path('recordCount').asInt() == fixture.scenarioResult().scenarioPlans().size()
        positiveManifest.path('counts').path('warningCount').asInt() == 0
        positiveManifest.path('counts').path('testRunStatusCounts').isEmpty()
        positiveManifest.path('sourceCatalogPath').asText() == staticCatalogPath.toString()
        positiveManifest.path('dynamicEvidenceRoot').asText() == positiveEvidenceRoot.toString()
        positiveManifest.path('outputCatalogPath').asText() == enrichedCatalogPath.toString()
        positiveManifest.path('warnings').isEmpty()

        and:
        def positiveReport = mapper.readTree(Files.readString(joinReportPath))
        positiveReport.path('schema').asText() == EnrichedScenarioCatalogWriter.JOIN_REPORT_SCHEMA
        positiveReport.path('runStatus').asText() == 'COMPLETE'
        assertJoinStatusCounts(positiveReport.path('counts'), positiveJoin)
        positiveReport.path('counts').path('scenarioPlansRead').asInt() == fixture.scenarioResult().scenarioPlans().size()
        positiveReport.path('counts').path('scenarioPlansEnriched').asInt() == fixture.scenarioResult().scenarioPlans().size()
        positiveReport.path('counts').path('dynamicEventsRead').asInt() == 5
        positiveReport.path('counts').path('eventsMissingTestContext').asInt() == 1
        positiveReport.path('counts').path('evidenceFilesRead').asInt() == 1
        positiveReport.path('warnings').isEmpty()

        when:
        Files.createDirectories(exactEvidenceFile.parent)
        writeJsonl(exactEvidenceFile, positiveEvidenceEvents(fixture, currentThreadName, fixture.selectedInput().deterministicId()))
        DynamicEvidenceReadResult exactRead = new DynamicEvidenceReader().read(exactEvidenceRoot)
        DynamicEvidenceJoinResult exactJoin = new DynamicEvidenceJoiner().join(
                fixture.scenarioResult().scenarioPlans(),
                exactRead.events(),
                exactRead.evidenceFilesRead(),
                exactRead.warnings())
        enrichedWriter.write(
                exactJoin,
                exactEnrichedCatalogPath,
                exactEnrichedManifestPath,
                exactJoinReportPath,
                staticCatalogPath.toString(),
                exactEvidenceRoot.toString(),
                EFFECTIVE_DYNAMIC_ENRICHMENT_CONFIG,
                [],
                GENERATED_AT)

        then:
        exactRead.evidenceFilesRead() == 1
        exactRead.events().size() == 5
        exactRead.events().findAll { it.inputVariantId() == fixture.selectedInput().deterministicId() }.size() == 4
        exactRead.events().last().inputVariantId() == null
        def exactRecord = exactJoin.records().find { record -> record.scenarioPlanId() == fixture.selectedPlan().deterministicId() }
        exactRecord != null
        exactRecord.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_EXACT
        exactRecord.dynamicEvidence().matchedInputVariantIds() == [fixture.selectedInput().deterministicId()]
        exactRecord.dynamicEvidence().observedSteps()[0].eventKinds() == ['STEP_STARTED', 'COMMAND_SENT', 'AGGREGATE_ACCESSED', 'STEP_FINISHED']
        exactRecord.dynamicEvidence().observedAggregateAccesses()[0].aggregateType() == fixture.selectedDispatch().aggregateName()
        exactRecord.dynamicEvidence().observedCommands()[0].commandType() == simpleName(fixture.selectedDispatch().commandTypeFqn())
        exactRecord.dynamicEvidence().warnings().isEmpty()

        and:
        def exactEnrichedRecord = readEnrichedRecord(exactEnrichedCatalogPath, fixture.selectedPlan().deterministicId())
        exactEnrichedRecord.path('dynamicEvidence').path('joinStatus').asText() == DynamicEvidenceJoinStatus.MATCHED_EXACT.name()
        exactEnrichedRecord.path('dynamicEvidence').path('matchedInputVariantIds').collect { it.asText() } == [fixture.selectedInput().deterministicId()]
        def exactManifest = mapper.readTree(Files.readString(exactEnrichedManifestPath))
        assertJoinStatusCounts(exactManifest.path('counts'), exactJoin)
        exactManifest.path('counts').path('recordCount').asInt() == fixture.scenarioResult().scenarioPlans().size()
        def exactReport = mapper.readTree(Files.readString(exactJoinReportPath))
        assertJoinStatusCounts(exactReport.path('counts'), exactJoin)
        exactReport.path('counts').path('dynamicEventsRead').asInt() == 5
        exactReport.path('counts').path('eventsMissingTestContext').asInt() == 1
        exactReport.path('counts').path('evidenceFilesRead').asInt() == 1
        exactReport.path('warnings').isEmpty()

        when:
        DynamicEvidenceReadResult emptyRead = new DynamicEvidenceReader().read(emptyEvidenceRoot)
        DynamicEvidenceJoinResult emptyJoin = new DynamicEvidenceJoiner().join(
                fixture.scenarioResult().scenarioPlans(),
                emptyRead.events(),
                emptyRead.evidenceFilesRead(),
                emptyRead.warnings())
        enrichedWriter.write(
                emptyJoin,
                emptyEnrichedCatalogPath,
                emptyEnrichedManifestPath,
                emptyJoinReportPath,
                staticCatalogPath.toString(),
                emptyEvidenceRoot.toString(),
                EFFECTIVE_DYNAMIC_ENRICHMENT_CONFIG,
                [],
                GENERATED_AT)

        then:
        emptyRead.evidenceFilesRead() == 0
        emptyRead.events().isEmpty()
        emptyRead.warnings().isEmpty()
        Files.exists(emptyEnrichedCatalogPath)
        Files.exists(emptyEnrichedManifestPath)
        Files.exists(emptyJoinReportPath)
        Files.readAllLines(emptyEnrichedCatalogPath).size() == fixture.scenarioResult().scenarioPlans().size()
        def emptyRecord = readEnrichedRecord(emptyEnrichedCatalogPath, fixture.selectedPlan().deterministicId())
        emptyRecord.path('dynamicEvidence').path('joinStatus').asText() == DynamicEvidenceJoinStatus.NOT_COVERED.name()
        emptyRecord.path('dynamicEvidence').path('matchedInputVariantIds').isEmpty()
        emptyRecord.path('dynamicEvidence').path('observedSteps').isEmpty()
        emptyRecord.path('dynamicEvidence').path('observedAggregateAccesses').isEmpty()
        emptyRecord.path('dynamicEvidence').path('observedCommands').isEmpty()
        emptyRecord.path('dynamicEvidence').path('warnings').isEmpty()
        def emptyManifest = mapper.readTree(Files.readString(emptyEnrichedManifestPath))
        emptyManifest.path('counts').path(DynamicEvidenceJoinStatus.NOT_COVERED.name()).asInt() == fixture.scenarioResult().scenarioPlans().size()
        emptyManifest.path('counts').path(DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE.name()).asInt() == 0
        emptyManifest.path('counts').path(DynamicEvidenceJoinStatus.UNMATCHED.name()).asInt() == 0
        emptyManifest.path('counts').path('recordCount').asInt() == fixture.scenarioResult().scenarioPlans().size()
        emptyManifest.path('counts').path('warningCount').asInt() == 0
        emptyManifest.path('counts').path('testRunStatusCounts').isEmpty()
        def emptyReport = mapper.readTree(Files.readString(emptyJoinReportPath))
        emptyReport.path('schema').asText() == EnrichedScenarioCatalogWriter.JOIN_REPORT_SCHEMA
        emptyReport.path('counts').path(DynamicEvidenceJoinStatus.NOT_COVERED.name()).asInt() == fixture.scenarioResult().scenarioPlans().size()
        emptyReport.path('counts').path(DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE.name()).asInt() == 0
        emptyReport.path('counts').path(DynamicEvidenceJoinStatus.UNMATCHED.name()).asInt() == 0
        emptyReport.path('counts').path('dynamicEventsRead').asInt() == 0
        emptyReport.path('counts').path('eventsMissingTestContext').asInt() == 0
        emptyReport.path('counts').path('evidenceFilesRead').asInt() == 0
        emptyReport.path('runStatus').asText() == 'COMPLETE'
    }

    def 'dummyapp dynamic enrichment runs through run scoped batched orchestrator path'() {
        given:
        def fixture = buildFixture()
        def currentThreadName = Thread.currentThread().name
        def runRoot = tempDir.resolve('dummyapp-batched-dynamic-enrichment')
        def applicationPath = tempDir.resolve('applications/dummyapp')
        Files.createDirectories(applicationPath)
        def selectedTestClass = fixture.selectedInput().sourceClassFqn()
        def secondSelectedTestClass = selectedTestClass + 'CompanionSpec'
        def selectedClasses = [selectedTestClass, secondSelectedTestClass]
        def runner = new FakeProcessRunner([new ProcessRunner.ProcessResult(0, 'batched ok', '', false)], { ProcessRunner.ProcessCommand command ->
            def outputDirArg = command.arguments().find { it.startsWith('-Dsimulator.dynamic-evidence.output-dir=') }
            def evidenceRoot = Path.of(outputDirArg.substring(outputDirArg.indexOf('=') + 1))
            writeJsonl(evidenceRoot.resolve('dynamic-evidence.jsonl'), positiveEvidenceEvents(fixture, currentThreadName))
            writeSurefireReport(applicationPath, selectedTestClass, 1, 0, 0, 0)
            writeSurefireReport(applicationPath, secondSelectedTestClass, 1, 0, 0, 0)
        })
        def orchestrator = new DynamicEnrichmentOrchestrator(runner)
        def staticCatalogPath = runRoot.resolve('scenario-catalog.jsonl')

        when:
        def result = orchestrator.run(
                dynamicConfig(),
                applicationPath,
                DUMMYAPP_APPLICATION_NAME,
                runRoot,
                selectedClasses,
                fixture.scenarioResult().scenarioPlans(),
                staticCatalogPath,
                GENERATED_AT)

        then:
        runner.commands.size() == 1
        runner.commands[0].arguments().any { it == "-Dtest=${selectedTestClass},${secondSelectedTestClass}".toString() }
        runner.commands[0].arguments().any { it == "-Dsimulator.dynamic-evidence.output-dir=${runRoot.resolve('dynamic-evidence')}".toString() }
        runner.commands[0].arguments().any { it == "-Dsimulator.dynamic-evidence.input-map-path=${runRoot.resolve('dynamic-evidence').resolve(DynamicInputMapWriter.FILE_NAME)}".toString() }

        and:
        def evidenceRoot = runRoot.resolve('dynamic-evidence')
        Files.exists(evidenceRoot.resolve(DynamicInputMapWriter.FILE_NAME))
        Files.exists(evidenceRoot.resolve('dynamic-evidence.jsonl'))
        Files.exists(evidenceRoot.resolve('maven-output.log'))
        Files.exists(evidenceRoot.resolve('test-run.json'))
        Files.exists(evidenceRoot.resolve('test-runs').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(selectedTestClass) + '.json'))
        Files.exists(evidenceRoot.resolve('test-runs').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(secondSelectedTestClass) + '.json'))
        !Files.exists(evidenceRoot.resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(selectedTestClass)))

        and:
        def inputMap = mapper.readTree(Files.readString(evidenceRoot.resolve(DynamicInputMapWriter.FILE_NAME)))
        inputMap.path('testClassFqn').isMissingNode()
        inputMap.path('selectedTestClassFqns')*.asText() == selectedClasses
        inputMap.path('inputs').find { it.path('inputVariantId').asText() == fixture.selectedInput().deterministicId() } != null

        and:
        def batchRun = mapper.readTree(Files.readString(evidenceRoot.resolve('test-run.json')))
        batchRun.path('status').asText() == 'PASSED'
        batchRun.path('selectedTestClassFqns')*.asText() == selectedClasses
        batchRun.path('commandArguments')*.asText().contains("-Dtest=${selectedTestClass},${secondSelectedTestClass}".toString())
        batchRun.path('statusCounts').path('passed').asInt() == 2
        batchRun.path('staticCatalogPath').asText() == staticCatalogPath.toString()
        batchRun.path('evidenceRoot').asText() == evidenceRoot.toString()
        batchRun.path('testRuns')*.path('status')*.asText() == ['PASSED', 'PASSED']
        mapper.readTree(Files.readString(evidenceRoot.resolve('test-runs').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(selectedTestClass) + '.json'))).path('status').asText() == 'PASSED'

        and:
        result.testRuns()*.status() == ['PASSED', 'PASSED']
        def enrichedRecord = readEnrichedRecord(runRoot.resolve('scenario-catalog-enriched.jsonl'), fixture.selectedPlan().deterministicId())
        enrichedRecord.path('dynamicEvidence').path('joinStatus').asText() == DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE.name()
        enrichedRecord.path('dynamicEvidence').path('observedSteps').get(0).path('eventKinds').collect { it.asText() } == ['STEP_STARTED', 'COMMAND_SENT', 'AGGREGATE_ACCESSED', 'STEP_FINISHED']
        def joinReport = mapper.readTree(Files.readString(runRoot.resolve('dynamic-evidence-join-report.json')))
        joinReport.path('runStatus').asText() == 'COMPLETE'
        joinReport.path('counts').path('testClassesSelected').asInt() == 2
        joinReport.path('counts').path('testClassesPassed').asInt() == 2
        joinReport.path('counts').path('dynamicEventsRead').asInt() == 5
        joinReport.path('counts').path('eventsMissingTestContext').asInt() == 1
        joinReport.path('counts').path(DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE.name()).asInt() == 1
    }

    private DummyappFixture buildFixture() {
        configureParser()

        def analysisState = buildDummyappAnalysisState()
        def adapterResult = new ApplicationAnalysisScenarioModelAdapter().adapt(analysisState)
        def scenarioResult = ScenarioGenerator.generate(adapterResult.sagaDefinitions(), adapterResult.inputVariants(), scenarioGeneratorConfig())

        assert !scenarioResult.scenarioPlans().isEmpty() : 'expected dummyapp scenario generation to emit at least one plan'

        def selectedPlan = selectUniqueItemSagaPlan(scenarioResult.scenarioPlans())
        def selectedInput = selectedPlan.inputs().first()
        def selectedSaga = analysisState.sagas.find { saga -> saga.fqn == selectedInput.sagaFqn() }
        assert selectedSaga != null : "expected to find dummyapp saga ${selectedInput.sagaFqn()} in analysis state"

        def selectedStep = selectedSaga.steps.find { step -> step.name == 'getOrderStep' && !step.dispatches.isEmpty() } ?: selectedSaga.steps.find { step -> !step.dispatches.isEmpty() }
        assert selectedStep != null : "expected ${selectedSaga.fqn} to expose a dispatchable step"
        def selectedDispatch = selectedStep.dispatches.first()

        new DummyappFixture(
                analysisState,
                scenarioResult,
                selectedPlan,
                selectedInput,
                selectedSaga,
                selectedStep,
                selectedDispatch)
    }

    private ApplicationAnalysisState buildDummyappAnalysisState() {
        def state = new ApplicationAnalysisState()
        def indexVisitor = new CommandHandlerIndexVisitor()
        def serviceVisitor = new ServiceVisitor()
        def commandHandlerVisitor = new CommandHandlerVisitor()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        def creationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor()
        def dummyappFiles = parseAllDummyappFiles()

        dummyappFiles.each { cu -> indexVisitor.visit(cu, state) }
        dummyappFiles.each { cu -> serviceVisitor.visit(cu, state) }
        dummyappFiles.each { cu -> commandHandlerVisitor.visit(cu, state) }
        dummyappFiles.each { cu -> workflowVisitor.visit(cu, state) }
        dummyappFiles.each { cu -> creationSiteVisitor.visit(cu, state) }

        def sourceIndex = new GroovySourceIndex()
        sourceIndex.parse(resolveProjectPath('applications', 'dummyapp', 'src', 'test', 'groovy'))
        new GroovyConstructorInputTraceVisitor().visit(sourceIndex, state)
        state
    }

    private ScenarioGeneratorConfig scenarioGeneratorConfig() {
        new ScenarioGeneratorConfig(
                false,
                true,
                1,
                1000,
                100,
                20,
                false,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                1234L)
    }

    private static ScenarioPlan selectUniqueItemSagaPlan(List<ScenarioPlan> plans) {
        def itemPlans = plans.findAll { plan ->
            plan.inputs().size() == 1 && plan.inputs().first().sagaFqn() == ITEM_SAGA_FQN
        }
        assert !itemPlans.isEmpty() : "expected at least one accepted plan for ${ITEM_SAGA_FQN}"

        def keyCounts = plans.collect { plan -> planKey(plan) }.countBy { it }
        def uniqueItemPlan = itemPlans.find { plan -> keyCounts[planKey(plan)] == 1 }
        assert uniqueItemPlan != null : "expected a unique dummyapp plan for ${ITEM_SAGA_FQN}"
        uniqueItemPlan
    }

    private static String planKey(ScenarioPlan plan) {
        def input = plan.inputs().first()
        [input.sagaFqn(), input.sourceClassFqn(), input.sourceMethodName()].join('|')
    }

    private List<Map<String, Object>> positiveEvidenceEvents(DummyappFixture fixture, String threadName,
                                                             String inputVariantId = null) {
        def input = fixture.selectedInput()
        def testClassFqn = input.sourceClassFqn()
        def testMethodName = input.sourceMethodName()
        def testDisplayName = input.sourceMethodName()
        def testUniqueId = "${testClassFqn}#${testMethodName}"
        def functionalityName = simpleName(fixture.selectedSaga().fqn)
        def runtimeStepName = fixture.selectedStep().name
        def commandTypeFqn = fixture.selectedDispatch().commandTypeFqn()
        def commandType = simpleName(commandTypeFqn)
        def aggregateName = fixture.selectedDispatch().aggregateName()
        def accessMode = fixture.selectedDispatch().accessPolicy().name()
        def sourceMethod = aggregateAccessSourceMethod(fixture.selectedDispatch())
        def functionalityInvocationId = "${functionalityName}-90"

        [
                event('positive-step-started', 'STEP_STARTED', testClassFqn, testMethodName, testDisplayName, testUniqueId, functionalityName, functionalityInvocationId, 90L, runtimeStepName, threadName, [stepPhase: 'FORWARD'], inputVariantId),
                event('positive-command-sent', 'COMMAND_SENT', testClassFqn, testMethodName, testDisplayName, testUniqueId, functionalityName, functionalityInvocationId, 90L, runtimeStepName, threadName,
                        [commandType: commandType, commandFqn: commandTypeFqn, serviceName: aggregateName, rootAggregateId: '7', fields: [orderAggregateId: 7]], inputVariantId),
                event('positive-aggregate-accessed', 'AGGREGATE_ACCESSED', testClassFqn, testMethodName, testDisplayName, testUniqueId, functionalityName, functionalityInvocationId, 90L, runtimeStepName, threadName,
                        [aggregateType: aggregateName, aggregateId: '7', accessMode: accessMode, sourceMethod: sourceMethod], inputVariantId),
                event('positive-step-finished', 'STEP_FINISHED', testClassFqn, testMethodName, testDisplayName, testUniqueId, functionalityName, functionalityInvocationId, 90L, runtimeStepName, threadName,
                        [outcome: 'SUCCESS', durationMillis: 12], inputVariantId),
                event('missing-context-step-started', 'STEP_STARTED', null, null, null, null, functionalityName, functionalityInvocationId, 90L, runtimeStepName, threadName, [stepPhase: 'FORWARD'])
        ]
    }

    private static Map<String, Object> event(String eventId,
                                             String eventKind,
                                             String testClassFqn,
                                             String testMethodName,
                                             String testDisplayName,
                                             String testUniqueId,
                                             String functionalityName,
                                             String functionalityInvocationId,
                                             Long unitOfWorkVersion,
                                             String stepName,
                                             String threadName,
                                             Map<String, Object> payload,
                                             String inputVariantId = null) {
        def event = new LinkedHashMap<String, Object>()
        event.schema = DYNAMIC_EVIDENCE_SCHEMA
        event.eventId = eventId
        event.eventKind = eventKind
        event.timestamp = timestampForSequence(eventSequenceFromId(eventId))
        event.sequence = eventSequenceFromId(eventId)
        event.threadName = threadName
        event.applicationName = DUMMYAPP_APPLICATION_NAME
        putIfPresent(event, 'testClassFqn', testClassFqn)
        putIfPresent(event, 'testMethodName', testMethodName)
        putIfPresent(event, 'testDisplayName', testDisplayName)
        putIfPresent(event, 'testUniqueId', testUniqueId)
        putIfPresent(event, 'inputVariantId', inputVariantId)
        putIfPresent(event, 'functionalityName', functionalityName)
        putIfPresent(event, 'functionalityInvocationId', functionalityInvocationId)
        putIfPresent(event, 'unitOfWorkVersion', unitOfWorkVersion)
        putIfPresent(event, 'stepName', stepName)
        if (payload != null) {
            event.payload = payload
        }
        event
    }

    private void writeJsonl(Path file, List<Map<String, Object>> events) {
        Files.createDirectories(file.parent)
        Files.writeString(file, events.collect { event -> mapper.writeValueAsString(event) }.join('\n') + '\n')
    }

    private DynamicEnrichmentConfig dynamicConfig() {
        new DynamicEnrichmentConfig(true, true, 'dynamic-evidence', 'scenario-catalog-enriched.jsonl', 'scenario-catalog-enriched-manifest.json', 'dynamic-evidence-join-report.json', 'src/test/groovy', [], [], [], 300, new DynamicEnrichmentConfig.DynamicEnrichmentMavenConfig('mvn', 'test-sagas'))
    }

    private static void writeSurefireReport(Path applicationPath, String testClassFqn, int tests, int failures, int errors, int skipped) {
        def reportsDir = applicationPath.resolve('target/surefire-reports')
        Files.createDirectories(reportsDir)
        Files.writeString(reportsDir.resolve("TEST-${testClassFqn}.xml"), """
                <testsuite name="${testClassFqn}" classname="${testClassFqn}" tests="${tests}" failures="${failures}" errors="${errors}" skipped="${skipped}">
                </testsuite>
                """)
    }

    private JsonNode readEnrichedRecord(Path catalogPath, String planId) {
        def record = Files.readAllLines(catalogPath)
                .collect { line -> mapper.readTree(line) }
                .find { node -> node.path('scenarioPlanId').asText() == planId }
        assert record != null : "expected enriched catalog to contain plan ${planId}"
        record
    }

    private static boolean assertJoinStatusCounts(JsonNode counts, DynamicEvidenceJoinResult joinResult) {
        DynamicEvidenceJoinStatus.values().each { status ->
            assert counts.path(status.name()).asInt() == joinResult.records().count { record -> record.dynamicEvidence().joinStatus() == status }
        }
        true
    }

    private static String aggregateAccessSourceMethod(StepDispatchFootprint dispatch) {
        dispatch.accessPolicy().name() == 'READ' ? 'aggregateLoadAndRegisterRead' : 'registerChanged'
    }

    private static boolean assertRealSchemaFields(DynamicEvidenceEvent event,
                                                  String expectedThreadName,
                                                  int expectedSequence,
                                                  Long expectedUnitOfWorkVersion,
                                                  String expectedFunctionalityName,
                                                  String expectedFunctionalityInvocationId,
                                                  String expectedStepName) {
        assert event.functionalityName() == expectedFunctionalityName
        assert event.functionalityInvocationId() == expectedFunctionalityInvocationId
        assert event.stepName() == expectedStepName
        assert event.eventId() != null
        assert event.eventKind() != null
        true
    }

    private static String timestampForSequence(Number sequence) {
        String.format('2026-05-01T00:00:00.%03dZ', sequence.intValue())
    }

    private static long eventSequenceFromId(String eventId) {
        switch (eventId) {
            case 'positive-step-started':
                return 1L
            case 'positive-command-sent':
                return 2L
            case 'positive-aggregate-accessed':
                return 3L
            case 'positive-step-finished':
                return 4L
            case 'missing-context-step-started':
                return 5L
            default:
                return 0L
        }
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map[key] = value
        }
    }

    private static String simpleName(String fqn) {
        if (fqn == null) {
            return null
        }
        def index = fqn.lastIndexOf('.')
        index >= 0 ? fqn.substring(index + 1) : fqn
    }

    private static class FakeProcessRunner implements ProcessRunner {
        final List<ProcessRunner.ProcessCommand> commands = []
        private final Queue<ProcessRunner.ProcessResult> results
        private final Closure sideEffect

        FakeProcessRunner(List<ProcessRunner.ProcessResult> results, Closure sideEffect = {}) {
            this.results = new ArrayDeque<>(results)
            this.sideEffect = sideEffect
        }

        @Override
        ProcessRunner.ProcessResult run(ProcessRunner.ProcessCommand command) {
            commands << command
            sideEffect.call(command)
            results.remove()
        }
    }

    private static final class DummyappFixture {
        private final ApplicationAnalysisState analysisState
        private final ScenarioGenerationResult scenarioResult
        private final ScenarioPlan selectedPlan
        private final InputVariant selectedInput
        private final SagaFunctionalityBuildingBlock selectedSaga
        private final SagaStepBuildingBlock selectedStep
        private final StepDispatchFootprint selectedDispatch

        DummyappFixture(ApplicationAnalysisState analysisState,
                        ScenarioGenerationResult scenarioResult,
                        ScenarioPlan selectedPlan,
                        InputVariant selectedInput,
                        SagaFunctionalityBuildingBlock selectedSaga,
                        SagaStepBuildingBlock selectedStep,
                        StepDispatchFootprint selectedDispatch) {
            this.analysisState = analysisState
            this.scenarioResult = scenarioResult
            this.selectedPlan = selectedPlan
            this.selectedInput = selectedInput ?: selectedPlan.inputs().first()
            this.selectedSaga = selectedSaga
            this.selectedStep = selectedStep
            this.selectedDispatch = selectedDispatch
        }

        ApplicationAnalysisState analysisState() {
            analysisState
        }

        ScenarioGenerationResult scenarioResult() {
            scenarioResult
        }

        ScenarioPlan selectedPlan() {
            selectedPlan
        }

        InputVariant selectedInput() {
            selectedInput
        }

        SagaFunctionalityBuildingBlock selectedSaga() {
            selectedSaga
        }

        SagaStepBuildingBlock selectedStep() {
            selectedStep
        }

        StepDispatchFootprint selectedDispatch() {
            selectedDispatch
        }
    }
}
