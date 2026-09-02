package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export.DynamicArtifactWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.CurrentPackageFixture
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification

import java.nio.file.Files

class DynamicRawEvidenceLifecycleSpec extends Specification {
    def 'deletes raw stream only after current package finalizes and retains it on failure'() {
        given:
        def pkg = CurrentPackageFixture.write([plan()], [])
        def app = Files.createTempDirectory('dynamic-app')
        Files.createDirectories(app.resolve('target/surefire-reports'))
        def successRun = Files.createTempDirectory('dynamic-run-success')
        def successRaw = [path: null]
        def runner = rawWritingRunner(successRaw)

        when:
        def result = new DynamicEnrichmentOrchestrator(runner).run(config(), app, 'fixture', successRun,
                ['example.OrderSpec'], pkg.workloads, pkg.manifest, '2026-08-22T10:00:00Z')

        then:
        !Files.exists(successRaw.path)
        Files.exists(result.diagnosticPath())
        Files.exists(result.observationPath())

        when:
        def failedRun = Files.createTempDirectory('dynamic-run-failure')
        def failedRaw = [path: null]
        def failurePackage = CurrentPackageFixture.write([plan()], [])
        def manifestBefore = Files.readAllBytes(failurePackage.manifest).toList()
        def injectedWriter = new DynamicArtifactWriter(new ObjectMapper(), { boundary ->
            if (boundary == DynamicArtifactWriter.Boundary.AFTER_MANIFEST_PROMOTION) {
                throw new IOException('injected finalization failure')
            }
        })
        def orchestrator = new DynamicEnrichmentOrchestrator(rawWritingRunner(failedRaw),
                new DynamicEvidenceReader(), new DynamicEvidenceJoiner(), injectedWriter, new ObjectMapper())
        orchestrator.run(config(), app, 'fixture', failedRun, ['example.OrderSpec'],
                failurePackage.workloads, failurePackage.manifest, '2026-08-22T10:00:00Z')

        then:
        thrown(IOException)
        Files.exists(failedRaw.path)
        Files.readAllBytes(failurePackage.manifest).toList() == manifestBefore
        new ScenarioCatalogPackageReader().readCurrent(failurePackage.manifest).dynamicObservations().isEmpty()
    }

    private static ProcessRunner rawWritingRunner(Map holder) {
        { ProcessRunner.ProcessCommand command ->
            def outputArg = command.arguments().find { it.startsWith('-Dsimulator.dynamic-evidence.output-dir=') }
            def output = java.nio.file.Path.of(outputArg.substring(outputArg.indexOf('=') + 1))
            Files.createDirectories(output)
            holder.path = output.resolve('dynamic-evidence.jsonl')
            Files.writeString(holder.path, '{"eventId":"event-1","eventKind":"STEP_STARTED","testClassFqn":"example.OrderSpec","testMethodName":"createsOrder","testUniqueId":"execution-1","functionalityName":"OrderSaga","functionalityInvocationId":"invocation-1","stepName":"reserveOrder","timestamp":"2026-08-22T10:00:00Z","sequence":1,"threadName":"test-thread","payload":{"stepPhase":"FORWARD"}}\n')
            new ProcessRunner.ProcessResult(0, '', '', false)
        } as ProcessRunner
    }

    private static DynamicEnrichmentConfig config() {
        new DynamicEnrichmentConfig(true, true, 'dynamic-evidence', 'src/test/groovy', [], [], [], 300,
                new DynamicEnrichmentConfig.DynamicEnrichmentMavenConfig('mvn', 'test-sagas'))
    }

    private static WorkloadPlan plan() {
        def input = new InputVariant('input-1', 'example.OrderSaga', 'example.OrderSpec', 'createsOrder', 'orderSaga',
                InputResolutionStatus.RESOLVED, 'source', 'provenance', [], [:], [])
        new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, 'workload-1', ScenarioKind.SINGLE_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, [new SagaInstance('participant-1', 'example.OrderSaga', 'input-1', [])],
                [input], [new ScheduledStep('scheduled-1', 'participant-1', 'example.OrderSaga::reserveOrder#0', 0, 'reserveOrder', [])],
                [], [], [], [])
    }
}
