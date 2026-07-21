package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleCap
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification

import java.nio.file.Files

class ScenarioCatalogJsonlWriterSpec extends Specification {

    private final ObjectMapper mapper = new ObjectMapper()

    def 'writes the linked v3 package without a v2 catalog artifact'() {
        given:
        def directory = Files.createTempDirectory('v3-workload-package')
        def paths = packagePaths(directory)
        def result = eagerGenerationResult()

        when:
        def manifest = writePackage(result, paths, '2026-07-20T00:00:00Z')

        then:
        Files.exists(paths.workload)
        Files.exists(paths.faultScenario)
        Files.exists(paths.manifest)
        Files.exists(paths.accounting)
        !Files.exists(directory.resolve('scenario-catalog.jsonl'))
        Files.readAllLines(paths.workload).size() == result.workloadPlans().size()
        Files.readAllLines(paths.faultScenario).size() == 2

        and:
        manifest.schemaVersion() == ScenarioCatalogManifest.SCHEMA_VERSION
        manifest.workloadCatalog().schemaVersion() == WorkloadPlan.SCHEMA_VERSION
        manifest.workloadCatalog().path() == paths.workload.toString()
        manifest.workloadCatalog().recordCount() == result.workloadPlans().size().toString()
        manifest.faultScenarioCatalog().schemaVersion() == ScenarioCatalogManifest.FAULT_SCENARIO_SCHEMA_VERSION
        manifest.faultScenarioCatalog().path() == paths.faultScenario.toString()
        manifest.faultScenarioCatalog().recordCount() == '2'
        manifest.scenarioSpaceAccounting().path() == paths.accounting.toString()
        manifest.materializabilityPolicy() == 'INPUT_READINESS_AND_STRUCTURAL_ADMISSIBILITY_RUNTIME_MATERIALIZATION_UNPROVEN'
        manifest.recoveryScheduleCap() == 20
        manifest.faultScenarioVectorSource() == 'EAGER_ALL_ZERO_AND_SINGLE_POINT'
        manifest.counts().workloadsExported == result.workloadPlans().size().toString()
        manifest.counts().materializableWorkloadPlans == '1'
        manifest.counts().computedEagerVectors == '2'
        manifest.counts().faultScenariosExported == '2'

        and:
        def workloadJson = mapper.readTree(Files.readAllLines(paths.workload).first())
        workloadJson.path('schemaVersion').asText() == WorkloadPlan.SCHEMA_VERSION
        workloadJson.path('faultSlots').size() == workloadJson.path('forwardSchedule').size()
        workloadJson.path('compensationCheckpoints').first().path('evidenceClass').asText() == 'EXPLICIT_COMPENSATION'
        def accounting = mapper.readTree(Files.readString(paths.accounting))
        accounting.path('schemaVersion').asText() == 'microservices-simulator.scenario-space-accounting.v3'
        accounting.path('workloadCatalogSpace').path('workloadPlansWritten').isTextual()
        accounting.path('workloadCatalogSpace').path('perWorkloadVectorSpace').first().path('possibleBinaryVectors').asText() == '2'
        accounting.path('workloadCatalogSpace').path('perWorkloadVectorSpace').first().path('eagerVectorCount').asText() == '2'
        accounting.path('faultScenarioCatalogSpace').path('faultScenariosWritten').asText() == '2'
        accounting.path('faultScenarioCatalogSpace').path('computedEagerVectorCount').asText() == '2'
        accounting.path('faultScenarioCatalogSpace').path('exactComputedVectorUncappedScheduleSum').asText() == '2'
        accounting.path('faultScenarioCatalogSpace').path('allVectorRecoveryTotalStatus').asText() == 'NOT_COMPUTED'

        and:
        def faultScenarios = Files.readAllLines(paths.faultScenario).collect { mapper.readTree(it) }
        faultScenarios*.path('assignedVector')*.asText() == ['0', '1']
        faultScenarios.find { it.path('assignedVector').asText() == '0' }
                .path('actions').every { it.path('kind').asText() == 'FORWARD' }
    }

    def 'fixed timestamp and semantic inputs produce byte-stable package output'() {
        given:
        def directory = Files.createTempDirectory('v3-byte-stable')
        def paths = packagePaths(directory)
        def firstResult = eagerGenerationResult()
        def secondResult = eagerGenerationResult()

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
        def result = eagerGenerationResult()
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
                .put('sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(paths.faultScenario)))
        mapper.writerWithDefaultPrettyPrinter().writeValue(paths.manifest.toFile(), manifestJson)
        reader.read(paths.manifest)

        then:
        def danglingFailure = thrown(IllegalArgumentException)
        danglingFailure.message.contains('references missing WorkloadPlan missing')
    }

    def 'shared package reader rejects missing and malformed artifact checksums'() {
        given:
        def paths = packagePaths(Files.createTempDirectory('v3-reader-checksum-metadata'))
        writePackage(eagerGenerationResult(), paths, '2026-07-20T00:00:00Z')
        def manifest = mapper.readTree(Files.readString(paths.manifest))
        if (checksum == null) {
            manifest.path(artifactField).remove('sha256')
        } else {
            manifest.path(artifactField).put('sha256', checksum)
        }
        Files.writeString(paths.manifest, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')

        when:
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains(manifest.path(artifactField).path('artifactKind').asText())
        failure.message.contains('valid SHA-256 checksum')

        where:
        artifactField          | checksum
        'workloadCatalog'       | null
        'faultScenarioCatalog'  | 'ABC'
        'scenarioSpaceAccounting' | 'a' * 63
        'rejectedInputsDiagnostic' | 'A' * 64
    }

    def 'shared package reader rejects byte changes to every linked artifact without updated checksums'() {
        given:
        def paths = packagePaths(Files.createTempDirectory("v3-reader-checksum-${artifactField}"))
        writePackage(eagerGenerationResult(), paths, '2026-07-20T00:00:00Z')
        Files.write(paths[artifactKey], '\n'.bytes, java.nio.file.StandardOpenOption.APPEND)

        when:
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains(expectedKind)
        failure.message.contains('checksum mismatch')

        where:
        artifactField                    | artifactKey      | expectedKind
        'workloadCatalog'                 | 'workload'       | 'WORKLOAD_CATALOG'
        'faultScenarioCatalog'            | 'faultScenario'  | 'FAULT_SCENARIO_CATALOG'
        'scenarioSpaceAccounting'         | 'accounting'     | 'SCENARIO_SPACE_ACCOUNTING'
        'rejectedInputsDiagnostic'        | 'rejected'       | 'REJECTED_INPUT_DIAGNOSTIC'
    }

    def 'shared package reader rejects checksum-matching malformed UTF-8 JSONL'() {
        given:
        def paths = packagePaths(Files.createTempDirectory('v3-reader-malformed-utf8'))
        writePackage(eagerGenerationResult(), paths, '2026-07-20T00:00:00Z')
        byte[] original = Files.readAllBytes(paths.workload)
        assert original[-2] == ('}' as char) as byte
        assert original[-1] == ('\n' as char) as byte
        def malformed = new ByteArrayOutputStream()
        malformed.write(original, 0, original.length - 2)
        malformed.write(',"ignored":"'.getBytes('UTF-8'))
        malformed.write(0x80)
        malformed.write('"}\n'.getBytes('UTF-8'))
        Files.write(paths.workload, malformed.toByteArray())
        refreshArtifactHash(paths.manifest, 'workloadCatalog', paths.workload)

        when:
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('Malformed UTF-8')
        failure.message.contains('WORKLOAD_CATALOG')
        failure.message.contains(paths.workload.toString())
        !failure.message.contains('checksum mismatch')
    }

    def 'shared package reader rejects a semantically valid same-count fault catalog replacement'() {
        given:
        def paths = packagePaths(Files.createTempDirectory('v3-reader-valid-replacement'))
        writePackage(eagerGenerationResult(), paths, '2026-07-20T00:00:00Z')
        def replacementLines = Files.readAllLines(paths.faultScenario).collect { line ->
            def record = mapper.readTree(line)
            def schema = record.remove('schemaVersion').asText()
            record.put('schemaVersion', schema)
            mapper.writeValueAsString(record)
        }
        Files.write(paths.faultScenario, replacementLines)

        when:
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('FAULT_SCENARIO_CATALOG')
        failure.message.contains('checksum mismatch')
    }

    def 'writer rejects an invalid eager package before creating any artifact'() {
        given:
        def directory = Files.createTempDirectory('v3-invalid-package')
        def paths = packagePaths(directory.resolve('not-created'))
        def valid = eagerGenerationResult()
        def scenario = valid.faultScenarios().first()
        def invalidScenario = new FaultScenario(
                scenario.schemaVersion(),
                scenario.deterministicId(),
                'missing-workload',
                scenario.assignedVector(),
                scenario.actions())
        def invalid = new EagerFaultScenarioGenerationResult(
                valid.workloadGenerationResult(),
                valid.recoveryScheduleCap(),
                [invalidScenario],
                valid.workloadMaterializability(),
                valid.computedVectors())

        when:
        writePackage(invalid, paths, '2026-07-20T00:00:00Z')

        then:
        thrown(IllegalArgumentException)
        snapshotExisting(paths).isEmpty()
    }

    def 'writer rejects repeated participant runtime step names before package publication'() {
        given:
        def paths = packagePaths(Files.createTempDirectory('v3-repeated-runtime-publication').resolve('not-created'))
        def repeated = repeatedRuntimeStepWorkload(
                EagerFaultScenarioGenerator.generate(generationResult(1L, 'integer', true), RecoveryScheduleCap.defaultCap())
                        .workloadPlans().first())
        def generation = new WorkloadGenerationResult(WorkloadPlan.SCHEMA_VERSION, new ScenarioGeneratorConfig(),
                [repeated], [], [:], [])
        def invalid = EagerFaultScenarioGenerator.generate(generation, RecoveryScheduleCap.defaultCap())

        when:
        writePackage(invalid, paths, '2026-07-20T00:00:00Z')

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME')
        snapshotExisting(paths).isEmpty()
    }

    def 'shared reader rejects a checksum-current workload with repeated participant runtime step names'() {
        given:
        def paths = packagePaths(Files.createTempDirectory('v3-reader-repeated-runtime'))
        writePackage(EagerFaultScenarioGenerator.generate(generationResult(1L, 'integer', true),
                RecoveryScheduleCap.defaultCap()), paths, '2026-07-20T00:00:00Z')
        def workload = mapper.readTree(Files.readAllLines(paths.workload).first())
        def firstStep = workload.path('forwardSchedule').get(0)
        def repeatedStep = workload.path('forwardSchedule').get(1)
        repeatedStep.put('stepId', firstStep.path('stepId').asText())
        repeatedStep.put('runtimeStepName', firstStep.path('runtimeStepName').asText())
        Files.writeString(paths.workload, mapper.writeValueAsString(workload) + '\n')
        refreshArtifactHash(paths.manifest, 'workloadCatalog', paths.workload)

        when:
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME')
        failure.message.contains('first occurrence')
        failure.message.contains('repeated occurrence')
    }

    def 'writer rejects replacement of a required single-point vector with a valid multi-fault vector before artifact creation'() {
        given:
        def directory = Files.createTempDirectory('v3-invalid-eager-vector-set')
        def paths = packagePaths(directory.resolve('not-created'))
        def workloads = generationResult(1L, 'integer', true)
        def valid = EagerFaultScenarioGenerator.generate(workloads, RecoveryScheduleCap.defaultCap())
        def plan = valid.workloadPlans().first()
        def multiFault = RecoveryScheduleGenerator.generate(plan, '11', valid.recoveryScheduleCap())
        def replacement = new ComputedVectorRecovery(
                plan.deterministicId(),
                '11',
                FaultScenarioVectorSource.EAGER_SINGLE_POINT,
                multiFault.uncappedScheduleCount(),
                multiFault.writtenScheduleCount())
        def invalid = new EagerFaultScenarioGenerationResult(
                valid.workloadGenerationResult(),
                valid.recoveryScheduleCap(),
                valid.faultScenarios().findAll { it.assignedVector() != '01' } + multiFault.faultScenarios(),
                valid.workloadMaterializability(),
                valid.computedVectors().findAll { it.assignedVector() != '01' } + replacement)

        when:
        writePackage(invalid, paths, '2026-07-20T00:00:00Z')

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('Eager vector coverage')
        snapshotExisting(paths).isEmpty()
    }

    def 'writer requires exact one-over-one accounting for the eager all-zero vector before artifact creation'() {
        given:
        def directory = Files.createTempDirectory('v3-invalid-all-zero-count')
        def paths = packagePaths(directory.resolve('not-created'))
        def valid = eagerGenerationResult()
        def allZero = valid.computedVectors().find { it.vectorSource() == FaultScenarioVectorSource.EAGER_ALL_ZERO }
        def invalidAllZero = new ComputedVectorRecovery(
                allZero.workloadPlanId(),
                allZero.assignedVector(),
                allZero.vectorSource(),
                BigInteger.TWO,
                1)
        def invalid = new EagerFaultScenarioGenerationResult(
                valid.workloadGenerationResult(),
                valid.recoveryScheduleCap(),
                valid.faultScenarios(),
                valid.workloadMaterializability(),
                valid.computedVectors().collect { it == allZero ? invalidAllZero : it })

        when:
        writePackage(invalid, paths, '2026-07-20T00:00:00Z')

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('all-zero vector must have exact uncapped/written counts 1/1')
        snapshotExisting(paths).isEmpty()
    }

    def 'v3 writer and reader preserve a high precision decimal input recipe exactly'() {
        given:
        def decimal = new BigDecimal('12345678901234567890.12345678901234567890')
        def directory = Files.createTempDirectory('v3-high-precision-decimal')
        def paths = packagePaths(directory)
        def result = eagerGenerationResult(decimal, 'decimal')
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
        writePackage(eagerGenerationResult(), paths, '2026-07-20T00:00:00Z')
        def workload = mapper.readTree(Files.readAllLines(paths.workload).first())
        workload.path('acceptedInputs').first()
                .path('inputRecipe').path('arguments').first().path('recipe')
                .put('value', 2)
        Files.writeString(paths.workload, mapper.writeValueAsString(workload) + '\n')
        refreshArtifactHash(paths.manifest, 'workloadCatalog', paths.workload)

        when:
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def staleFingerprint = thrown(IllegalArgumentException)
        staleFingerprint.message.contains('INPUT_RECIPE_FINGERPRINT_MISMATCH')
    }

    def 'package reader rejects malformed workload ownership'() {
        given:
        def directory = Files.createTempDirectory('v3-reader-boundary')
        def paths = packagePaths(directory)
        def result = eagerGenerationResult()
        writePackage(result, paths, '2026-07-20T00:00:00Z')
        def workload = mapper.readTree(Files.readAllLines(paths.workload).first())
        workload.path('forwardSchedule').first().put('sagaInstanceId', 'missing-participant')
        Files.writeString(paths.workload, mapper.writeValueAsString(workload) + '\n')
        refreshArtifactHash(paths.manifest, 'workloadCatalog', paths.workload)

        when:
        new ScenarioCatalogPackageReader().read(paths.manifest)

        then:
        def malformed = thrown(IllegalArgumentException)
        malformed.message.contains('Invalid WorkloadPlan')
    }

    private void refreshArtifactHash(java.nio.file.Path manifestPath,
                                     String artifactField,
                                     java.nio.file.Path artifactPath) {
        def manifest = mapper.readTree(Files.readString(manifestPath))
        manifest.path(artifactField).put('sha256', ScenarioCatalogJsonlWriter.sha256(Files.readAllBytes(artifactPath)))
        Files.writeString(manifestPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + '\n')
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

    private static Map<String, String> snapshotExisting(Map paths) {
        paths.findAll { key, path -> Files.exists(path) }
                .collectEntries { key, path -> [(key): Files.readString(path)] }
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

    private static WorkloadPlan repeatedRuntimeStepWorkload(WorkloadPlan original) {
        def first = original.forwardSchedule().first()
        def schedule = original.forwardSchedule().withIndex().collect { step, index ->
            index == 1
                    ? new ScheduledStep(step.deterministicId(), step.sagaInstanceId(), first.stepId(),
                    step.scheduleOrder(), first.runtimeStepName(), step.warnings())
                    : step
        }
        def slots = original.faultSlots().withIndex().collect { slot, index ->
            index == 1
                    ? new ForwardFaultSlot(slot.deterministicId(), slot.slotIndex(), slot.scheduledStepId(),
                    slot.sagaInstanceId(), first.stepId(), first.runtimeStepName(), slot.occurrenceId())
                    : slot
        }
        def withoutId = new WorkloadPlan(original.schemaVersion(), null, original.kind(), original.executionShape(),
                original.participants(), original.acceptedInputs(), schedule, original.conflictEvidence(), slots,
                original.compensationCheckpoints(), original.warnings())
        new WorkloadPlan(withoutId.schemaVersion(), ScenarioIdGenerator.workloadPlanId(withoutId), withoutId.kind(),
                withoutId.executionShape(), withoutId.participants(), withoutId.acceptedInputs(),
                withoutId.forwardSchedule(), withoutId.conflictEvidence(), withoutId.faultSlots(),
                withoutId.compensationCheckpoints(), withoutId.warnings())
    }

    private static def eagerGenerationResult(Object recipeValue = 1L, String literalKind = 'integer') {
        EagerFaultScenarioGenerator.generate(generationResult(recipeValue, literalKind), RecoveryScheduleCap.defaultCap())
    }

    private static def generationResult(Object recipeValue = 1L,
                                        String literalKind = 'integer',
                                        boolean twoSlots = false) {
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
        def steps = [step]
        if (twoSlots) {
            steps.add(new StepDefinition(
                    'example.OrderSaga::confirm',
                    'example.OrderSaga::confirm',
                    'confirm',
                    1,
                    [step.stepKey()],
                    [],
                    []))
        }
        def saga = new SagaDefinition('example.OrderSaga', steps, [])
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
