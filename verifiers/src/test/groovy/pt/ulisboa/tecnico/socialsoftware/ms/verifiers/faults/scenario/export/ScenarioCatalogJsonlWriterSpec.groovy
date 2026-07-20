package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioCatalogReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorOptions
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification

import java.nio.file.Files

class ScenarioCatalogJsonlWriterSpec extends Specification {

    private final ObjectMapper mapper = new ObjectMapper()

    def 'writes the linked v3 package without a v2 catalog artifact'() {
        given:
        def directory = Files.createTempDirectory('v3-workload-package')
        def paths = packagePaths(directory)
        def result = generationResult()

        when:
        def manifest = writePackage(result, paths, '2026-07-20T00:00:00Z')

        then:
        Files.exists(paths.workload)
        Files.exists(paths.faultScenario)
        Files.exists(paths.manifest)
        Files.exists(paths.accounting)
        !Files.exists(directory.resolve('scenario-catalog.jsonl'))
        Files.readAllLines(paths.workload).size() == result.workloadPlans().size()
        Files.readAllLines(paths.faultScenario).isEmpty()

        and:
        manifest.schemaVersion() == ScenarioCatalogManifest.SCHEMA_VERSION
        manifest.workloadCatalog().schemaVersion() == WorkloadPlan.SCHEMA_VERSION
        manifest.workloadCatalog().path() == paths.workload.toString()
        manifest.workloadCatalog().recordCount() == result.workloadPlans().size().toString()
        manifest.faultScenarioCatalog().schemaVersion() == ScenarioCatalogManifest.FAULT_SCENARIO_SCHEMA_VERSION
        manifest.faultScenarioCatalog().path() == paths.faultScenario.toString()
        manifest.faultScenarioCatalog().recordCount() == '0'
        manifest.scenarioSpaceAccounting().path() == paths.accounting.toString()
        manifest.materializabilityPolicy() == 'INPUT_READINESS_AND_STRUCTURAL_ADMISSIBILITY'
        manifest.counts().workloadsExported == result.workloadPlans().size().toString()

        and:
        def workloadJson = mapper.readTree(Files.readAllLines(paths.workload).first())
        workloadJson.path('schemaVersion').asText() == WorkloadPlan.SCHEMA_VERSION
        workloadJson.path('faultSlots').size() == workloadJson.path('forwardSchedule').size()
        workloadJson.path('compensationCheckpoints').first().path('evidenceClass').asText() == 'EXPLICIT_COMPENSATION'
        def accounting = mapper.readTree(Files.readString(paths.accounting))
        accounting.path('schemaVersion').asText() == 'microservices-simulator.scenario-space-accounting.v3'
        accounting.path('workloadCatalogSpace').path('workloadPlansWritten').isTextual()
        accounting.path('workloadCatalogSpace').path('perWorkloadVectorSpace').first().path('possibleBinaryVectors').asText() == '2'
        accounting.path('faultScenarioCatalogSpace').path('faultScenariosWritten').asText() == '0'
    }

    def 'fixed timestamp and semantic inputs produce byte-stable package output'() {
        given:
        def directory = Files.createTempDirectory('v3-byte-stable')
        def paths = packagePaths(directory)
        def firstResult = generationResult()
        def secondResult = generationResult()

        when:
        writePackage(firstResult, paths, '2026-07-20T00:00:00Z')
        def firstBytes = snapshot(paths)
        writePackage(secondResult, paths, '2026-07-20T00:00:00Z')
        def secondBytes = snapshot(paths)

        then:
        firstResult.workloadPlans()*.deterministicId() == secondResult.workloadPlans()*.deterministicId()
        firstBytes == secondBytes
    }

    def 'v3 reader resolves linked artifacts and rejects v2 and dangling records clearly'() {
        given:
        def directory = Files.createTempDirectory('v3-package-reader')
        def paths = packagePaths(directory)
        def result = generationResult()
        writePackage(result, paths, '2026-07-20T00:00:00Z')
        def reader = new ScenarioCatalogPackageReader()

        expect:
        reader.read(paths.manifest).workloadPlans()*.deterministicId() == result.workloadPlans()*.deterministicId()

        when: 'a v2 record is supplied where a v3 package manifest is required'
        def v2 = directory.resolve('scenario-catalog.jsonl')
        Files.writeString(v2, '{"schemaVersion":"microservices-simulator.scenario-catalog.v2"}\n')
        reader.read(v2)

        then:
        def v2Failure = thrown(IllegalArgumentException)
        v2Failure.message.contains('v2 catalogs are not supported')

        when: 'a linked FaultScenario references an absent WorkloadPlan'
        Files.writeString(paths.faultScenario,
                '{"schemaVersion":"microservices-simulator.fault-scenario.v3","deterministicId":"fault-1","workloadPlanId":"missing"}\n')
        def manifestJson = mapper.readTree(Files.readString(paths.manifest))
        manifestJson.withObject('/faultScenarioCatalog').put('recordCount', '1')
        mapper.writerWithDefaultPrettyPrinter().writeValue(paths.manifest.toFile(), manifestJson)
        reader.read(paths.manifest)

        then:
        def danglingFailure = thrown(IllegalArgumentException)
        danglingFailure.message.contains('references missing WorkloadPlan missing')
    }

    def 'v3 writer and reader preserve a high precision decimal input recipe exactly'() {
        given:
        def decimal = new BigDecimal('12345678901234567890.12345678901234567890')
        def directory = Files.createTempDirectory('v3-high-precision-decimal')
        def paths = packagePaths(directory)
        def result = generationResult(decimal, 'decimal')
        writePackage(result, paths, '2026-07-20T00:00:00Z')

        when:
        def loaded = new ScenarioCatalogPackageReader().read(paths.manifest)
        def writtenRecipe = result.workloadPlans().first().acceptedInputs().first().inputRecipe()
        def loadedRecipe = loaded.workloadPlans().first().acceptedInputs().first().inputRecipe()

        then:
        loaded.workloadPlans()*.deterministicId() == result.workloadPlans()*.deterministicId()
        loadedRecipe.recipeFingerprint() == writtenRecipe.recipeFingerprint()
        loadedRecipe.arguments().first().recipe().value() instanceof BigDecimal
        loadedRecipe.arguments().first().recipe().value() == decimal
    }

    def 'v3 reader rejects an input recipe with a stale serialized fingerprint'() {
        given:
        def directory = Files.createTempDirectory('v3-stale-recipe')
        def paths = packagePaths(directory)
        writePackage(generationResult(), paths, '2026-07-20T00:00:00Z')
        def workload = mapper.readTree(Files.readAllLines(paths.workload).first())
        workload.path('acceptedInputs').first()
                .path('inputRecipe').path('arguments').first().path('recipe')
                .put('value', 2)
        Files.writeString(paths.workload, mapper.writeValueAsString(workload) + '\n')

        when:
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def staleFingerprint = thrown(IllegalArgumentException)
        staleFingerprint.message.contains('INPUT_RECIPE_FINGERPRINT_MISMATCH')
    }

    def 'readers reject malformed workload ownership and the legacy executor refuses v3 artifacts'() {
        given:
        def directory = Files.createTempDirectory('v3-reader-boundary')
        def paths = packagePaths(directory)
        def result = generationResult()
        writePackage(result, paths, '2026-07-20T00:00:00Z')
        def workload = mapper.readTree(Files.readAllLines(paths.workload).first())
        workload.path('forwardSchedule').first().put('sagaInstanceId', 'missing-participant')
        Files.writeString(paths.workload, mapper.writeValueAsString(workload) + '\n')

        when:
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def malformed = thrown(IllegalArgumentException)
        malformed.message.contains('Invalid WorkloadPlan')

        when:
        new ScenarioCatalogReader().read(new ScenarioExecutorOptions(null, paths.workload, null, null, true))

        then:
        def legacyBoundary = thrown(IllegalArgumentException)
        legacyBoundary.message.contains('does not support')
        legacyBoundary.message.contains('v3 WorkloadPlan/FaultScenario')
    }

    private static ScenarioCatalogManifest writePackage(def result, Map paths, String generatedAt) {
        new ScenarioCatalogJsonlWriter().write(
                result,
                paths.workload,
                paths.faultScenario,
                paths.manifest,
                paths.rejected,
                paths.accounting,
                null,
                generatedAt)
    }

    private static Map<String, String> snapshot(Map paths) {
        [
                workload    : Files.readString(paths.workload),
                faultScenario: Files.readString(paths.faultScenario),
                manifest    : Files.readString(paths.manifest),
                accounting  : Files.readString(paths.accounting),
                rejected    : Files.readString(paths.rejected)
        ]
    }

    private static Map packagePaths(java.nio.file.Path directory) {
        [
                workload    : directory.resolve('workload-catalog.jsonl'),
                faultScenario: directory.resolve('fault-scenario-catalog.jsonl'),
                manifest    : directory.resolve('scenario-catalog-manifest.json'),
                accounting  : directory.resolve('scenario-space-accounting.json'),
                rejected    : directory.resolve('workload-catalog-rejected-inputs.jsonl')
        ]
    }

    private static def generationResult(Object recipeValue = 1L, String literalKind = 'integer') {
        def footprint = new StepFootprint(
                new AggregateKey('example.Order', 'Order', 'order-1', FootprintConfidence.EXACT),
                AccessMode.WRITE,
                [])
        def step = new StepDefinition(
                'example.OrderSaga::create',
                'example.OrderSaga::create',
                'create',
                0,
                [],
                [footprint],
                [],
                true,
                true,
                true,
                CompensationEvidenceClass.EXPLICIT_COMPENSATION,
                [],
                [])
        def saga = new SagaDefinition('example.OrderSaga', [step], [])
        def recipeType = recipeValue.getClass().name
        def recipeNode = InputRecipeNode.builder('literal')
                .sourceText(recipeValue.toString())
                .provenanceText('constructor argument 0')
                .executorReady(true)
                .literalKind(literalKind)
                .value(recipeValue)
                .targetTypeFqn(recipeType)
                .build()
        def recipeArgument = new InputRecipeArgument(0, recipeType, InputResolutionStatus.RESOLVED,
                true, [], 'argument 0', recipeNode)
        def recipe = new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], [recipeArgument])
        def input = new InputVariant(
                'input-1',
                'example.OrderSaga',
                'example.OrderSagaSpec',
                'creates an order',
                'saga',
                InputResolutionStatus.RESOLVED,
                'source',
                'provenance',
                [],
                [orderId: 'order-1'],
                [],
                recipe)
        ScenarioGenerator.generate([saga], [input], new ScenarioGeneratorConfig())
    }
}
