package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.RejectedInputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioGenerationResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeRejectionReason
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class ScenarioCatalogJsonlWriterSpec extends Specification {

    private final ObjectMapper mapper = new ObjectMapper()

    def 'writes one valid json object per scenario line'() {
        given:
        def tempRoot = Files.createTempDirectory('scenario-catalog-writer-lines')
        def catalogPath = tempRoot.resolve('nested/catalog/scenario-catalog.jsonl')
        def manifestPath = tempRoot.resolve('nested/manifest/scenario-catalog-manifest.json')
        def writer = new ScenarioCatalogJsonlWriter()
        def result = generationResult([
                singleScenarioPlan('scenario-1'),
                multiScenarioPlan('scenario-2')
        ])

        when:
        writer.write(result, catalogPath, manifestPath, '2026-04-27T00:00:00Z')

        then:
        Files.exists(catalogPath)
        Files.exists(manifestPath)
        def lines = Files.readAllLines(catalogPath)
        lines.size() == 2

        and:
        def first = mapper.readTree(lines[0])
        def second = mapper.readTree(lines[1])
        first.path('schemaVersion').asText() == ScenarioPlan.SCHEMA_VERSION
        first.path('deterministicId').asText() == 'scenario-1'
        second.path('schemaVersion').asText() == ScenarioPlan.SCHEMA_VERSION
        second.path('deterministicId').asText() == 'scenario-2'
        first.path('kind').asText() == ScenarioKind.SINGLE_SAGA.name()
        second.path('kind').asText() == ScenarioKind.MULTI_SAGA.name()
        first.path('inputs').get(0).path('sourceMode').asText() == SourceMode.SAGAS.name()
        first.path('inputs').get(0).path('sourceModeConfidence').asText() == SourceModeConfidence.TYPE_EVIDENCE.name()
        first.path('inputs').get(0).path('sourceModeEvidence').collect { it.asText() } == ['evidence-scenario-1-input']
    }

    def 'writes manifest with counters and config'() {
        given:
        def tempRoot = Files.createTempDirectory('scenario-catalog-writer-manifest')
        def catalogPath = tempRoot.resolve('exports/catalog.jsonl')
        def manifestPath = tempRoot.resolve('exports/manifest.json')
        def writer = new ScenarioCatalogJsonlWriter()
        def result = generationResult(
                [singleScenarioPlan('scenario-1'), singleScenarioPlan('scenario-2')],
                config(),
                [scenariosEmitted: 2, scenariosCapped: 1],
                ['reached maxScenarios=7; remaining scenarios were not emitted', 'schedule cap reached']
        )

        when:
        writer.write(result, catalogPath, manifestPath, '2026-04-27T00:00:00Z')

        then:
        def manifest = mapper.readTree(Files.readString(manifestPath))
        manifest.path('schemaVersion').asText() == ScenarioPlan.SCHEMA_VERSION
        manifest.path('generatedAt').asText() == '2026-04-27T00:00:00Z'
        manifest.path('effectiveConfig').path('maxSagaSetSize').asInt() == 4
        manifest.path('effectiveConfig').path('maxScenarios').asInt() == 7
        manifest.path('effectiveConfig').path('maxInputVariantsPerSaga').asInt() == 8
        manifest.path('counts').path('scenariosEmitted').asInt() == 2
        manifest.path('counts').path('scenariosCapped').asInt() == 1
        manifest.path('counts').path('scenariosExported').asInt() == 2
        manifest.path('counts').path('rejectedInputsExported').asInt() == 0
        manifest.path('inputVariantsAcceptedBySourceMode').path(SourceMode.SAGAS.name()).asInt() == 2
        manifest.path('inputVariantsAcceptedBySourceMode').path(SourceMode.TCC.name()).asInt() == 0
        manifest.path('warnings').collect { it.asText() } == [
                'reached maxScenarios=7; remaining scenarios were not emitted',
                'schedule cap reached'
        ]
        manifest.path('catalogPath').asText() == catalogPath.toString()
        manifest.path('manifestPath').asText() == manifestPath.toString()
    }

    def 'empty catalog still writes manifest'() {
        given:
        def tempRoot = Files.createTempDirectory('scenario-catalog-writer-empty')
        def catalogPath = tempRoot.resolve('empty/catalog.jsonl')
        def manifestPath = tempRoot.resolve('empty/manifest.json')
        def writer = new ScenarioCatalogJsonlWriter()
        def result = generationResult([])

        when:
        writer.write(result, catalogPath, manifestPath, '2026-04-27T00:00:00Z')

        then:
        Files.exists(catalogPath)
        Files.exists(manifestPath)
        Files.exists(tempRoot.resolve('empty/scenario-catalog-rejected-inputs.jsonl'))
        Files.readAllLines(catalogPath).isEmpty()
        Files.readAllLines(tempRoot.resolve('empty/scenario-catalog-rejected-inputs.jsonl')).isEmpty()
        mapper.readTree(Files.readString(manifestPath)).path('counts').path('scenariosExported').asInt() == 0
    }

    def 'writes rejected inputs with manifest paths and counters without archive siblings'() {
        given:
        def tempRoot = Files.createTempDirectory('scenario-catalog-writer-rejected')
        def catalogPath = tempRoot.resolve('exports/scenario-catalog.jsonl')
        def manifestPath = tempRoot.resolve('exports/scenario-catalog-manifest.json')
        def rejectedPath = tempRoot.resolve('exports/scenario-catalog-rejected-inputs.jsonl')
        def catalogArchivePath = tempRoot.resolve('exports/scenario-catalog-20260427-000000-000.jsonl')
        def manifestArchivePath = tempRoot.resolve('exports/scenario-catalog-manifest-20260427-000000-000.json')
        def rejectedArchivePath = tempRoot.resolve('exports/scenario-catalog-rejected-inputs-20260427-000000-000.jsonl')
        def writer = new ScenarioCatalogJsonlWriter()
        def rejectedInput = inputVariant('rejected-input', 'com.example.OrderSaga', SourceMode.TCC)
        def result = new ScenarioGenerationResult(
                ScenarioPlan.SCHEMA_VERSION,
                config(),
                [singleScenarioPlan('scenario-1')],
                [new RejectedInputVariant(rejectedInput, SourceModeRejectionReason.SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG, ['rejected because tcc'])],
                [inputVariantsRejectedBySourceMode: 1],
                []
        )

        when:
        writer.write(result,
                catalogPath,
                manifestPath,
                rejectedPath,
                '2026-04-27T00:00:00Z')

        then:
        !Files.exists(catalogArchivePath)
        !Files.exists(rejectedArchivePath)
        !Files.exists(manifestArchivePath)

        and:
        def rejectedLines = Files.readAllLines(rejectedPath)
        rejectedLines.size() == 1
        def rejectedJson = mapper.readTree(rejectedLines[0])
        rejectedJson.path('deterministicId').asText() == 'rejected-input'
        rejectedJson.path('sourceMode').asText() == SourceMode.TCC.name()
        rejectedJson.path('sourceModeConfidence').asText() == SourceModeConfidence.TYPE_EVIDENCE.name()
        rejectedJson.path('sourceModeEvidence').collect { it.asText() } == ['evidence-rejected-input']
        rejectedJson.path('rejectionReason').asText() == SourceModeRejectionReason.SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG.name()
        rejectedJson.path('rejectionWarnings').collect { it.asText() } == ['rejected because tcc']

        and:
        def acceptedJson = mapper.readTree(Files.readAllLines(catalogPath)[0])
        acceptedJson.path('inputs').collect { it.path('deterministicId').asText() } == ['scenario-1-input']
        !acceptedJson.path('inputs').collect { it.path('deterministicId').asText() }.contains('rejected-input')

        and:
        def manifest = mapper.readTree(Files.readString(manifestPath))
        manifest.path('catalogPath').asText() == catalogPath.toString()
        manifest.path('manifestPath').asText() == manifestPath.toString()
        manifest.path('rejectedInputsPath').asText() == rejectedPath.toString()
        manifest.has('catalogArchivePath') == false
        manifest.has('manifestArchivePath') == false
        manifest.has('rejectedInputsArchivePath') == false
        manifest.path('counts').path('rejectedInputsExported').asInt() == 1
        manifest.path('inputVariantsBySourceMode').path(SourceMode.SAGAS.name()).asInt() == 1
        manifest.path('inputVariantsBySourceMode').path(SourceMode.TCC.name()).asInt() == 1
        manifest.path('inputVariantsAcceptedBySourceMode').path(SourceMode.SAGAS.name()).asInt() == 1
        manifest.path('inputVariantsAcceptedBySourceMode').path(SourceMode.TCC.name()).asInt() == 0
        manifest.path('inputVariantsRejectedBySourceModeReason').path(SourceModeRejectionReason.SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG.name()).asInt() == 1
    }

    def 'writer creates parent directories'() {
        given:
        def tempRoot = Files.createTempDirectory('scenario-catalog-writer-dirs')
        def catalogPath = tempRoot.resolve('deep/output/catalog/scenario-catalog.jsonl')
        def manifestPath = tempRoot.resolve('deep/output/manifest/scenario-catalog-manifest.json')
        def writer = new ScenarioCatalogJsonlWriter()
        def result = generationResult([singleScenarioPlan('scenario-1')])

        when:
        writer.write(result, catalogPath, manifestPath, '2026-04-27T00:00:00Z')

        then:
        Files.isDirectory(catalogPath.parent)
        Files.isDirectory(manifestPath.parent)
        Files.exists(catalogPath)
        Files.exists(manifestPath)
    }

    private static ScenarioGenerationResult generationResult(List<ScenarioPlan> plans,
                                                             ScenarioGeneratorConfig config = config(),
                                                             Map<String, Integer> counts = [:],
                                                             List<String> warnings = []) {
        new ScenarioGenerationResult(ScenarioPlan.SCHEMA_VERSION, config, plans, counts, warnings)
    }

    private static ScenarioGeneratorConfig config() {
        new ScenarioGeneratorConfig(
                true,
                false,
                4,
                7,
                8,
                9,
                true,
                ScenarioGeneratorConfig.InputPolicy.ALLOW_PARTIAL,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                99L
        )
    }

    private static ScenarioPlan singleScenarioPlan(String scenarioId) {
        def sagaFqn = 'com.example.OrderSaga'
        def sagaInstanceId = "${scenarioId}-instance"
        def inputVariantId = "${scenarioId}-input"
        def scheduledStepId = "${scenarioId}-scheduled-step"

        new ScenarioPlan(
                ScenarioPlan.SCHEMA_VERSION,
                scenarioId,
                ScenarioKind.SINGLE_SAGA,
                [new SagaInstance(sagaInstanceId, sagaFqn, inputVariantId, ['instance-warning'])],
                [inputVariant(inputVariantId, sagaFqn)],
                [new ScheduledStep(scheduledStepId, sagaInstanceId, "${sagaFqn}::step-1", 0, ['scheduled-warning'])],
                null,
                [],
                ['plan-warning']
        )
    }

    private static ScenarioPlan multiScenarioPlan(String scenarioId) {
        def leftSagaFqn = 'com.example.OrderSaga'
        def rightSagaFqn = 'com.example.PaymentSaga'
        def leftInstanceId = "${scenarioId}-left-instance"
        def rightInstanceId = "${scenarioId}-right-instance"
        def leftInputId = "${scenarioId}-left-input"
        def rightInputId = "${scenarioId}-right-input"
        def leftScheduledStep = new ScheduledStep("${scenarioId}-left-scheduled", leftInstanceId, "${leftSagaFqn}::step-1", 0, [])
        def rightScheduledStep = new ScheduledStep("${scenarioId}-right-scheduled", rightInstanceId, "${rightSagaFqn}::step-1", 1, [])

        new ScenarioPlan(
                ScenarioPlan.SCHEMA_VERSION,
                scenarioId,
                ScenarioKind.MULTI_SAGA,
                [
                        new SagaInstance(leftInstanceId, leftSagaFqn, leftInputId, []),
                        new SagaInstance(rightInstanceId, rightSagaFqn, rightInputId, [])
                ],
                [inputVariant(leftInputId, leftSagaFqn), inputVariant(rightInputId, rightSagaFqn)],
                [leftScheduledStep, rightScheduledStep],
                null,
                [new ConflictEvidence(
                        "${scenarioId}-conflict",
                        leftScheduledStep.deterministicId(),
                        rightScheduledStep.deterministicId(),
                        new AggregateKey('com.example.Order', 'Order', 'order-1', FootprintConfidence.EXACT),
                        new AggregateKey('com.example.Order', 'Order', 'order-1', FootprintConfidence.EXACT),
                        AccessMode.WRITE,
                        AccessMode.READ,
                        ConflictKind.WRITE_READ,
                        ['conflict-warning'])],
                ['plan-warning']
        )
    }

    private static InputVariant inputVariant(String deterministicId, String sagaFqn) {
        inputVariant(deterministicId, sagaFqn, SourceMode.SAGAS)
    }

    private static InputVariant inputVariant(String deterministicId, String sagaFqn, SourceMode sourceMode) {
        new InputVariant(
                deterministicId,
                sagaFqn,
                'com.example.TestInput',
                'build',
                'sagaField',
                InputResolutionStatus.RESOLVED,
                sourceMode,
                SourceModeConfidence.TYPE_EVIDENCE,
                ["evidence-${deterministicId}".toString()],
                "source-${deterministicId}",
                "provenance-${deterministicId}",
                ['arg'],
                [orderId: 'order-1'],
                ['input-warning']
        )
    }
}
