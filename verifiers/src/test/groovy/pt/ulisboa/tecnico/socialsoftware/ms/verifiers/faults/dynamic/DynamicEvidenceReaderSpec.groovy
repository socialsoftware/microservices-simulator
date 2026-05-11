package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.DynamicEvidenceReader
import spock.lang.Specification

import java.nio.file.Files

class DynamicEvidenceReaderSpec extends Specification {

    def 'recursively reads dynamic evidence files and captures source location'() {
        given:
        def root = Files.createTempDirectory('dynamic-evidence-reader')
        def nested = root.resolve('dynamic-evidence/com.example.FirstSpec')
        def deeper = root.resolve('dynamic-evidence/com.example.second/NestedSpec')
        Files.createDirectories(nested)
        Files.createDirectories(deeper)
        def firstFile = nested.resolve('dynamic-evidence.jsonl')
        def secondFile = deeper.resolve('dynamic-evidence.jsonl')
        Files.writeString(firstFile, '\n{"eventId":"e1","eventKind":"STEP_STARTED","testClassFqn":"com.example.FirstSpec","functionalityName":"OrderSaga","functionalityInvocationId":"inv-1","stepName":"reserve"}\n')
        Files.writeString(secondFile, '{"eventId":"e2","eventKind":"AGGREGATE_ACCESSED","testClassFqn":"com.example.NestedSpec","payload":{"aggregateType":"Order","aggregateId":"1","accessMode":"READ"}}\n')

        when:
        def result = new DynamicEvidenceReader().read(root)

        then:
        result.warnings().isEmpty()
        result.events()*.eventId() == ['e1', 'e2']
        result.events()[0].sourcePath() == firstFile
        result.events()[0].lineNumber() == 2
        result.events()[1].sourcePath() == secondFile
        result.events()[1].lineNumber() == 1
    }

    def 'malformed lines are reported as warnings without aborting reader'() {
        given:
        def root = Files.createTempDirectory('dynamic-evidence-reader-malformed')
        def dir = root.resolve('dynamic-evidence/test')
        Files.createDirectories(dir)
        def evidence = dir.resolve('dynamic-evidence.jsonl')
        Files.writeString(evidence, '{"eventId":"ok","eventKind":"STEP_FINISHED"}\n{not-json}\n   \n')

        when:
        def result = new DynamicEvidenceReader().read(root)

        then:
        result.events()*.eventId() == ['ok']
        result.warnings().size() == 1
        result.warnings()[0].contains('dynamic-evidence.jsonl:2')
        result.warnings()[0].contains('Malformed dynamic evidence JSON')
    }
}
