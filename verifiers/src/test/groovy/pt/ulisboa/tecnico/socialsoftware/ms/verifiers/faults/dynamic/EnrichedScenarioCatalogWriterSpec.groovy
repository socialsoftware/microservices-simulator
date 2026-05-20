package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export.EnrichedScenarioCatalogWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.EnrichedScenarioRecord
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep
import spock.lang.Specification

import java.nio.file.Files

class EnrichedScenarioCatalogWriterSpec extends Specification {
    private final ObjectMapper mapper = new ObjectMapper()

    def 'writes enriched jsonl records that embed original scenario plan unchanged'() {
        given:
        def root = Files.createTempDirectory('enriched-catalog-writer')
        def plan = scenarioPlan('scenario-1')
        def joinResult = new DynamicEvidenceJoiner().join([plan], [])
        def writer = new EnrichedScenarioCatalogWriter()
        def catalogPath = root.resolve('scenario-catalog-enriched.jsonl')
        def manifestPath = root.resolve('scenario-catalog-enriched-manifest.json')
        def reportPath = root.resolve('dynamic-evidence-join-report.json')

        when:
        writer.write(joinResult, catalogPath, manifestPath, reportPath, 'scenario-catalog.jsonl', 'dynamic-evidence', [:], [], '2026-05-01T00:00:00Z')

        then:
        def lines = Files.readAllLines(catalogPath)
        lines.size() == 1
        def enriched = mapper.readTree(lines[0])
        enriched.path('schemaVersion').asText() == EnrichedScenarioRecord.SCHEMA_VERSION
        enriched.path('scenarioPlanId').asText() == 'scenario-1'
        enriched.path('scenarioPlan').path('schemaVersion').asText() == ScenarioPlan.SCHEMA_VERSION
        enriched.path('scenarioPlan').path('deterministicId').asText() == plan.deterministicId()
        enriched.path('scenarioPlan').path('inputs')[0].path('deterministicId').asText() == plan.inputs()[0].deterministicId()
        enriched.path('scenarioPlan').path('inputs')[0].path('inputRecipe').path('recipeFingerprint').asText() == plan.inputs()[0].inputRecipe().recipeFingerprint()
        enriched.path('scenarioPlan').path('inputs')[0].path('inputRecipe').path('executorReady').asBoolean()
        enriched.path('dynamicEvidence').path('joinStatus').asText() == DynamicEvidenceJoinStatus.NOT_COVERED.name()
    }

    def 'writes manifest and join report counts'() {
        given:
        def root = Files.createTempDirectory('enriched-catalog-writer-counts')
        def joinResult = new DynamicEvidenceJoiner().join([
                scenarioPlan('covered'),
                scenarioPlan('not-covered')
        ], [])
        def writer = new EnrichedScenarioCatalogWriter()
        def catalogPath = root.resolve('scenario-catalog-enriched.jsonl')
        def manifestPath = root.resolve('scenario-catalog-enriched-manifest.json')
        def reportPath = root.resolve('dynamic-evidence-join-report.json')
        def testRuns = [[testClassFqn: 'com.example.OrderSpec', status: 'PASSED', exitCode: 0, evidencePath: 'dynamic-evidence/com.example.OrderSpec/dynamic-evidence.jsonl']]

        when:
        writer.write(joinResult, catalogPath, manifestPath, reportPath, 'scenario-catalog.jsonl', 'dynamic-evidence', [enabled: true], testRuns, '2026-05-01T00:00:00Z')

        then:
        def manifest = mapper.readTree(Files.readString(manifestPath))
        manifest.path('schema').asText() == EnrichedScenarioCatalogWriter.MANIFEST_SCHEMA
        manifest.path('counts').path(DynamicEvidenceJoinStatus.NOT_COVERED.name()).asInt() == 2
        manifest.path('counts').path('warningCount').asInt() == 0
        manifest.path('sourceCatalogPath').asText() == 'scenario-catalog.jsonl'
        manifest.path('dynamicEvidenceRoot').asText() == 'dynamic-evidence'

        and:
        def report = mapper.readTree(Files.readString(reportPath))
        report.path('schema').asText() == 'microservices-simulator.dynamic-evidence-join-report.v1'
        report.path('runStatus').asText() == 'COMPLETE'
        report.path('counts').path('testClassesSelected').asInt() == 1
        report.path('counts').path('testClassesPassed').asInt() == 1
        report.path('counts').path('scenarioPlansRead').asInt() == 2
        report.path('counts').path('scenarioPlansEnriched').asInt() == 2
        report.path('counts').path(DynamicEvidenceJoinStatus.NOT_COVERED.name()).asInt() == 2
    }

    private static ScenarioPlan scenarioPlan(String id) {
        def input = inputWithRecipe("${id}-input".toString())
        new ScenarioPlan(
                ScenarioPlan.SCHEMA_VERSION,
                id,
                ScenarioKind.SINGLE_SAGA,
                [new SagaInstance("${id}-instance".toString(), 'com.example.OrderSaga', input.deterministicId(), [])],
                [input],
                [new ScheduledStep("${id}-step".toString(), "${id}-instance".toString(), 'com.example.OrderSaga::reserve', 0, [])],
                null,
                [],
                []
        )
    }

    private static InputVariant inputWithRecipe(String id) {
        def node = InputRecipeNode.builder('literal')
                .sourceText('17')
                .executorReady(true)
                .literalKind('integer')
                .value(17L)
                .expectedTypeFqn('java.lang.Integer')
                .build()
        def argument = new InputRecipeArgument(0,
                'java.lang.Integer',
                InputResolutionStatus.RESOLVED,
                true,
                [],
                'customerId <- 17',
                node)
        def recipe = new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], [argument])
        new InputVariant(id,
                'com.example.OrderSaga',
                'com.example.OrderSpec',
                'creates order',
                'orderSaga',
                InputResolutionStatus.RESOLVED,
                'source',
                'provenance',
                ['arg[0]: customerId <- 17'],
                [:],
                [],
                recipe)
    }
}
