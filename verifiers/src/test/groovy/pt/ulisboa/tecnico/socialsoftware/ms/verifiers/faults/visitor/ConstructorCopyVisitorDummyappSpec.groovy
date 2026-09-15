package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.StaticAnalysisArtifactWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState

import java.nio.file.Files
import java.security.MessageDigest

class ConstructorCopyVisitorDummyappSpec extends VisitorTestSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper()

    def setupSpec() {
        configureParser()
    }

    def 'infers unrelated direct field names and excludes computed constructor values'() {
        given:
        def state = infer(parseAllDummyappFiles())

        expect:
        state.copyContractArtifact.schema() == 'copy-contracts.v1'
        state.copyContractArtifact.support().status() == 'bounded'
        state.copyContractArtifact.limitations().any { it.contains('Computed') }
        state.copyContractArtifact.contracts()*.targetType() == [
            'com.example.dummyapp.inferredcopy.StoredCard'
        ]

        and:
        with(state.copyContractArtifact.contracts().first()) {
            sourceType() == 'com.example.dummyapp.inferredcopy.CardInput'
            sourceKey() == 'aggregateId'
            targetKey() == 'reference'
            fields() == [aggregateId: 'reference', caption: 'heading']
            proof()*.sourceField() == ['aggregateId', 'caption']
            sourceFiles()*.path() == [
                'src/main/java/com/example/dummyapp/inferredcopy/CardInput.java',
                'src/main/java/com/example/dummyapp/inferredcopy/StoredCard.java'
            ]
            sourceFiles().every { it.sha256() == sha256(resolveDummyappSource(it.path())) }
        }
    }

    def 'inference and JSON projection are deterministic across source traversal order'() {
        given:
        def units = parseAllDummyappFiles()

        expect:
        MAPPER.writeValueAsBytes(infer(units).copyContractArtifact) as List ==
            MAPPER.writeValueAsBytes(infer(units.reverse()).copyContractArtifact) as List
    }

    def 'ordinary static package writes and reads optional copy contracts while legacy package stays valid'() {
        given:
        def state = infer(parseAllDummyappFiles())
        def model = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
        def output = Files.createTempDirectory('copy-contract-package')

        when:
        def manifest = new StaticAnalysisArtifactWriter().write(
            model, 'dummyapp', new ScenarioGeneratorConfig(), output, '2026-09-15T00:00:00Z')
        def contents = new ScenarioCatalogPackageReader().readCurrentStatic(
            output.resolve('scenario-catalog-manifest.json'))

        then:
        manifest.files().keySet().contains('copy-contracts')
        manifest.files().get('copy-contracts').path() == 'copy-contracts.json'
        contents.copyContractPath() == output.resolve('copy-contracts.json')
        contents.copyContracts().path('schema').asText() == 'copy-contracts.v1'
        contents.copyContracts().path('contracts').size() == 1

        cleanup:
        output?.toFile()?.deleteDir()
    }

    def 'ordinary package describes bounded support when inference finds zero contracts'() {
        given:
        def model = new ScenarioModelAdapterResult([], [], [], [], [:], [])
        def output = Files.createTempDirectory('empty-copy-contract-package')

        when:
        new StaticAnalysisArtifactWriter().write(
            model, 'empty', new ScenarioGeneratorConfig(), output, '2026-09-15T00:00:00Z')
        def artifact = new ScenarioCatalogPackageReader().readCurrentStatic(
            output.resolve('scenario-catalog-manifest.json')).copyContracts()

        then:
        artifact.path('support').path('status').asText() == 'bounded'
        artifact.path('contracts').isEmpty()
        artifact.path('limitations').any { it.asText().contains('empty contracts array') }

        cleanup:
        output?.toFile()?.deleteDir()
    }

    private static ApplicationAnalysisState infer(List units) {
        def visitor = new ConstructorCopyVisitor()
        def state = new ApplicationAnalysisState()
        units.each { visitor.visit(it, state) }
        visitor.finish(state)
        return state
    }

    private static java.nio.file.Path resolveDummyappSource(String stablePath) {
        resolveProjectPath('applications', 'dummyapp').resolve(stablePath)
    }

    private static String sha256(java.nio.file.Path path) {
        HexFormat.of().formatHex(MessageDigest.getInstance('SHA-256').digest(Files.readAllBytes(path)))
    }
}
