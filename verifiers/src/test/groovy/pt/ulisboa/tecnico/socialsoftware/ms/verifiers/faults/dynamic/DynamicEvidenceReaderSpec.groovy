package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.DynamicEvidenceReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.DynamicEvidenceJoiner
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep
import spock.lang.Specification
import spock.lang.Timeout

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
        result.evidenceFilesRead() == 2
        result.dynamicEventsRead() == 2
        result.eventsMissingTestContext() == 0
        result.evidenceBytesRead() > 0
        result.events()*.eventId() == ['e1', 'e2']
        result.events()[0].sourcePath() == firstFile
        result.events()[0].lineNumber() == 2
        result.events()[0].functionalityClassFqn() == null
        result.events()[0].functionalityClassSimpleName() == null
        result.events()[1].sourcePath() == secondFile
        result.events()[1].lineNumber() == 1
    }

    def 'reads functionality class identity fields from dynamic evidence'() {
        given:
        def root = Files.createTempDirectory('dynamic-evidence-reader-fqn')
        def dir = root.resolve('dynamic-evidence/test')
        Files.createDirectories(dir)
        def evidence = dir.resolve('dynamic-evidence.jsonl')
        Files.writeString(evidence, '{"eventId":"e1","eventKind":"STEP_STARTED","functionalityName":"RemoveCourseExecutionFunctionalitySagas","functionalityClassFqn":"com.example.tournament.RemoveCourseExecutionFunctionalitySagas","functionalityClassSimpleName":"RemoveCourseExecutionFunctionalitySagas","stepName":"reserve"}\n')

        when:
        def result = new DynamicEvidenceReader().read(root)

        then:
        result.warnings().isEmpty()
        result.events()[0].functionalityName() == 'RemoveCourseExecutionFunctionalitySagas'
        result.events()[0].functionalityClassFqn() == 'com.example.tournament.RemoveCourseExecutionFunctionalitySagas'
        result.events()[0].functionalityClassSimpleName() == 'RemoveCourseExecutionFunctionalitySagas'
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
        result.dynamicEventsRead() == 1
        result.warnings().size() == 1
        result.warnings()[0].contains('dynamic-evidence.jsonl:2')
        result.warnings()[0].contains('Malformed dynamic evidence JSON')
    }

    def 'reader and joiner preserve fixture join behavior through compact events'() {
        given:
        def root = Files.createTempDirectory('dynamic-evidence-reader-joiner')
        def evidenceDir = root.resolve('dynamic-evidence')
        Files.createDirectories(evidenceDir)
        Files.writeString(evidenceDir.resolve('dynamic-evidence.jsonl'), '''
{"eventId":"step-started","eventKind":"STEP_STARTED","testClassFqn":"com.example.OrderSpec","testMethodName":"creates order","functionalityName":"OrderSaga","functionalityInvocationId":"inv-1","stepName":"reserve"}
{"eventId":"command-sent","eventKind":"COMMAND_SENT","testClassFqn":"com.example.OrderSpec","testMethodName":"creates order","functionalityName":"OrderSaga","functionalityInvocationId":"inv-1","stepName":"reserve","payload":{"commandType":"ReserveOrderCommand","commandFqn":"com.example.ReserveOrderCommand","serviceName":"orders","rootAggregateId":"42"}}
{"eventId":"aggregate-read","eventKind":"AGGREGATE_ACCESSED","testClassFqn":"com.example.OrderSpec","testMethodName":"creates order","functionalityName":"OrderSaga","functionalityInvocationId":"inv-1","stepName":"reserve","payload":{"aggregateType":"Order","aggregateId":"42","accessMode":"READ","sourceMethod":"aggregateLoadAndRegisterRead"}}
{"eventId":"step-finished","eventKind":"STEP_FINISHED","testClassFqn":"com.example.OrderSpec","testMethodName":"creates order","functionalityName":"OrderSaga","functionalityInvocationId":"inv-1","stepName":"reserve","payload":{"outcome":"SUCCESS"}}
''')
        def plan = plan('scenario-high', [input('input-1')])

        when:
        def read = new DynamicEvidenceReader().read(root)
        def result = new DynamicEvidenceJoiner().join([plan], read.events(), read.evidenceFilesRead(), read.warnings())
        def enriched = result.records()[0]

        then:
        read.warnings().isEmpty()
        read.dynamicEventsRead() == 4
        read.eventsMissingTestContext() == 0
        result.warnings().isEmpty()
        enriched.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE
        enriched.dynamicEvidence().matchedInputVariantIds() == ['input-1']
        enriched.dynamicEvidence().observedSteps()[0].eventKinds() == ['STEP_STARTED', 'COMMAND_SENT', 'AGGREGATE_ACCESSED', 'STEP_FINISHED']
        enriched.dynamicEvidence().observedSteps()[0].outcomes() == ['SUCCESS']
        enriched.dynamicEvidence().observedAggregateAccesses()[0].aggregateType() == 'Order'
        enriched.dynamicEvidence().observedCommands()[0].commandType() == 'ReserveOrderCommand'
    }

    @Timeout(10)
    def 'reads generated twenty thousand event fixture with byte metrics quickly'() {
        given:
        def root = Files.createTempDirectory('dynamic-evidence-reader-large')
        def evidenceDir = root.resolve('dynamic-evidence')
        Files.createDirectories(evidenceDir)
        def builder = new StringBuilder()
        for (int i = 0; i < 20_000; i++) {
            builder.append('{"eventId":"e').append(i).append('","eventKind":"STEP_STARTED","testClassFqn":"com.example.LoadSpec","testMethodName":"load","functionalityName":"OrderSaga","stepName":"reserve"}\n')
        }
        Files.writeString(evidenceDir.resolve('dynamic-evidence.jsonl'), builder.toString())

        when:
        def started = System.nanoTime()
        def result = new DynamicEvidenceReader().read(root)
        def elapsedMillis = (System.nanoTime() - started) / 1_000_000L

        then:
        result.warnings().isEmpty()
        result.evidenceFilesRead() == 1
        result.dynamicEventsRead() == 20_000
        result.eventsMissingTestContext() == 0
        result.evidenceBytesRead() > 0
        elapsedMillis < 10_000L
    }

    private ScenarioPlan plan(String id, List<InputVariant> inputs, String sagaFqn = 'com.example.OrderSaga') {
        new ScenarioPlan(
                ScenarioPlan.SCHEMA_VERSION,
                id,
                ScenarioKind.SINGLE_SAGA,
                [new SagaInstance("${id}-instance".toString(), sagaFqn, inputs[0].deterministicId(), [])],
                inputs,
                [new ScheduledStep("${id}-step".toString(), "${id}-instance".toString(), "${sagaFqn}::reserve".toString(), 0, [])],
                null,
                [],
                []
        )
    }

    private static InputVariant input(String id, String sagaFqn = 'com.example.OrderSaga') {
        new InputVariant(id, sagaFqn, 'com.example.OrderSpec', 'creates order', 'orderSaga', InputResolutionStatus.RESOLVED, 'source', 'provenance', [], [:], [])
    }
}
