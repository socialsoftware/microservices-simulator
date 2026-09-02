package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class StaticAnalysisArtifactWriterContractSpec extends Specification {

    def 'writer emits the exact shared M0 count-only values and bytes'() {
        given:
        def mapper = new ObjectMapper()
        def fixturePath = Path.of('..', 'issues', '2026-08-30-current-only-verifier-artifacts',
                'examples', 'current-package.fixture.json').toAbsolutePath().normalize()
        def fixture = mapper.readTree(fixturePath.toFile())
        def artifacts = fixture.path('artifacts')
        def expectedManifest = fixture.path('manifests').path('count-only')
        def directory = Files.createTempDirectory('current-writer-contract-')

        when:
        StaticAnalysisArtifactWriter.writeProjection(directory,
                artifacts.path('accounting').path('count-only'),
                artifacts.path('sagas').toList(),
                artifacts.path('inputs').toList(),
                artifacts.path('interactions').toList())

        then:
        Files.readAllBytes(directory.resolve('accounting.json')) ==
                mapper.writeValueAsBytes(artifacts.path('accounting').path('count-only'))
        Files.readAllBytes(directory.resolve('sagas.jsonl')) == jsonLines(mapper, artifacts.path('sagas').toList())
        Files.readAllBytes(directory.resolve('inputs.jsonl')) == jsonLines(mapper, artifacts.path('inputs').toList())
        Files.readAllBytes(directory.resolve('interactions.jsonl')) == jsonLines(mapper, artifacts.path('interactions').toList())
        mapper.readTree(directory.resolve('scenario-catalog-manifest.json').toFile()) == expectedManifest

        and: 'the central reader observes the exact approved values produced by the writer'
        def contents = new ScenarioCatalogPackageReader().readCurrentStatic(
                directory.resolve('scenario-catalog-manifest.json'))
        contents.accounting() == artifacts.path('accounting').path('count-only')
        contents.sagaFacts() == artifacts.path('sagas').toList()
        contents.inputFacts() == artifacts.path('inputs').toList()
        contents.interactionFacts() == artifacts.path('interactions').toList()
    }

    private static byte[] jsonLines(ObjectMapper mapper, List<?> records) {
        (records.collect { mapper.writeValueAsString(it) }.join('\n') + '\n').getBytes('UTF-8')
    }
}
