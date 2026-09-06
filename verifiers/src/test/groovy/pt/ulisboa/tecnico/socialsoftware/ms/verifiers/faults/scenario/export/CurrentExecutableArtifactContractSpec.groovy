package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorReadinessEvaluator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

/**
 * Contract tests for the current role-keyed package.  The records in this
 * spec are materialized directly from the approved M0 fixture so this test
 * protects the wire shape independently of the Java model serializer.
 */
class CurrentExecutableArtifactContractSpec extends Specification {

    private final ObjectMapper mapper = new ObjectMapper()

    def 'the shared M0 executable fixture round-trips every current role exactly'() {
        given:
        def fixture = materializeFixture('initial-catalog')

        when:
        def packageContents = new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)
        def executionContents = new ScenarioCatalogPackageReader().readCurrentForExecution(fixture.manifest)

        then:
        packageContents.manifest().files().keySet() ==
                ['accounting', 'sagas', 'inputs', 'interactions', 'setups', 'workloads', 'faultScenarios', 'requests'] as Set
        packageContents.setupRecords().size() == 2
        packageContents.setupRecords()*.path('kind')*.asText() == ['sourceDerived', 'providerBacked']
        packageContents.setupRecords().first().path('actions')*.path('id')*.asText() ==
                ['setup-action-1', 'setup-action-2']
        packageContents.setupRecords().first().path('bindings').first().path('input').asText() == 'input-1'
        packageContents.setupRecords().last().path('provider').asText() == 'example.fixtureProvider@1'

        and: 'workloads use local participants and one step/event schedule'
        packageContents.workloadRecords().size() == 2
        def firstWorkload = packageContents.workloadRecords().first()
        firstWorkload.path('participants')*.path('id')*.asText() == ['p1', 'p2']
        firstWorkload.path('schedule')*.path('kind')*.asText() == ['step', 'event', 'step']
        firstWorkload.path('schedule').first().path('faultSlot').asInt() == 0
        firstWorkload.path('schedule').last().path('faultSlot').asInt() == 1
        firstWorkload.path('schedule')[1].path('route').asText() == 'reserveOrder#0/event#0'
        firstWorkload.path('schedule')[1].path('triggeringStep').asText() == 's1'

        and: 'faults are compact occurrence references and requests are one shared empty stream'
        packageContents.faultScenarioRecords().size() == 3
        packageContents.faultScenarioRecords().find { it.path('id').asText() == 'fault-3' }
                .path('actions')*.fieldNames()*.next() == ['step', 'event', 'step', 'compensate']
        packageContents.requestRecords().isEmpty()
        executionContents.workloadPlans()*.schemaVersion().every { it == WorkloadPlan.CURRENT_SCHEMA_VERSION }
        executionContents.faultScenarios()*.schemaVersion().every { it == pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario.CURRENT_SCHEMA_VERSION }
    }

    def 'count-only fixture has no executable placeholders and cannot be consumed as executable'() {
        given:
        def fixture = materializeFixture('count-only')

        expect:
        new ScenarioCatalogPackageReader().readCurrentStatic(fixture.manifest).sagaFacts().size() == 2
        new ScenarioCatalogPackageReader().readCurrentStatic(fixture.manifest).inputFacts().size() == 4

        when:
        new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)

        then:
        thrown(IllegalArgumentException)
    }

    def 'source setup bindings do not poison an independent constructor argument after package reading'() {
        given:
        def fixture = materializeFixture('initial-catalog')
        mutateRole(fixture, 'inputs') { inputs ->
            def input = inputs.find { it.path('id').asText() == 'input-1' }
            input.put('materializable', false)
            input.set('blockers', mapper.valueToTree([[
                    argument: 0,
                    reason: 'setupBoundResult',
                    sourceExpression: 'created.aggregateId'
            ]]))
            input.path('arguments').first().set('value', mapper.createObjectNode()
                    .put('kind', 'unresolved').put('reason', 'setupBoundResult'))
            input.path('arguments').add(mapper.valueToTree([
                    index: 1,
                    expectedType: 'java.beans.FeatureDescriptor',
                    sourceExpression: 'dto <- new FeatureDescriptor()',
                    value: [
                            kind: 'constructor',
                            targetType: 'java.beans.FeatureDescriptor',
                            fields: [name: [kind: 'literal', value: 'source-backed-name']]
                    ]
            ]))
        }

        when:
        def contents = new ScenarioCatalogPackageReader().readCurrentForExecution(fixture.manifest)
        def workload = contents.workloadPlans().find { it.deterministicId() == 'workload-1' }
        def input = workload.acceptedInputs().find { it.deterministicId() == 'input-1' }
        def dtoReadiness = new ScenarioExecutorReadinessEvaluator().evaluate(input.inputRecipe().arguments()[1])

        then:
        !input.inputRecipe().executorReady()
        !input.inputRecipe().arguments()[0].executorReady()
        input.inputRecipe().arguments()[1].executorReady()
        input.inputRecipe().arguments()[1].recipe().assignments()*.propertyName() == ['name']
        dtoReadiness.materializable()
        dtoReadiness.blockers().isEmpty()
    }

    def 'ordinary package consumption is current-only and rejects historical manifests'() {
        given:
        def directory = Files.createTempDirectory('current-only-legacy-rejection-')
        def legacy = directory.resolve('legacy.json')
        Files.writeString(legacy, mapper.writeValueAsString([
                schemaVersion: 'microservices-simulator.scenario-catalog-manifest.v5',
                generatedAt: '2026-08-29T00:00:00Z',
                effectiveConfig: [:],
                generationSource: 'STATIC_ANALYSIS',
                materializabilityPolicy: 'fixture',
                recoveryScheduleCap: 1,
                faultScenarioVectorSource: 'EAGER_ALL_ZERO_AND_SINGLE_POINT',
                workloadMaterializability: [], counts: [:], warnings: [],
                workloadCatalog: [:], faultScenarioCatalog: [:], scenarioSpaceAccounting: [:],
                rejectedInputsDiagnostic: [:], inputVariantsBySourceMode: [:],
                inputVariantsAcceptedBySourceMode: [:], inputVariantsRejectedBySourceModeReason: [:]
        ]))

        when:
        new ScenarioCatalogPackageReader().read(legacy)

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('manifest') || error.message.contains('current')
    }

    def 'current reader rejects the complete negative reference matrix'() {
        given:
        def fixture = materializeFixture('initial-catalog')
        def original = Files.readAllBytes(fixture.manifest)

        when:
        mutateRole(fixture, role, mutation)
        new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)

        then:
        thrown(IllegalArgumentException)

        where:
        role             | mutation
        'setups'         | { JsonNode n -> n.first().put('kind', 'not-a-setup-kind') }
        'setups'         | { JsonNode n -> n.first().path('actions').first().put('id', 'duplicate') ; n.first().path('actions').last().put('id', 'duplicate') }
        'workloads'      | { JsonNode n -> n.first().put('setup', 'missing-setup') }
        'setups'         | { JsonNode n -> n.last().path('bindings').first().put('type', '') }
        'workloads'      | { JsonNode n -> n.first().path('participants').first().put('input', 'missing-input') }
        'workloads'      | { JsonNode n -> n.first().path('participants').first().put('saga', 'missing-saga') }
        'workloads'      | { JsonNode n -> n.first().path('schedule').first().put('participant', 'missing-participant') }
        'workloads'      | { JsonNode n -> n.first().path('schedule').last().put('faultSlot', -1) }
        'workloads'      | { JsonNode n -> n.first().path('schedule').last().put('id', 's1') }
        'workloads'      | { JsonNode n -> n.first().path('schedule')[1].put('route', 'missing-route') }
        'workloads'      | { JsonNode n -> ((com.fasterxml.jackson.databind.node.ArrayNode)n.first().path('interactions')).set(0, com.fasterxml.jackson.databind.node.TextNode.valueOf('missing-interaction')) }
        'faultScenarios' | { JsonNode n -> n.first().put('workload', 'missing-workload') }
        'faultScenarios' | { JsonNode n -> n.first().put('faultVector', '0') }
        'faultScenarios' | { JsonNode n -> n.first().path('actions').first().put('event', 's1') }
        'faultScenarios' | { JsonNode n -> n.get(2).path('actions').last().put('compensate', 'e1') }
        'faultScenarios' | { JsonNode n -> n.first().path('actions').first().put('step', 'missing-step') }
        'requests'       | { JsonNode n -> n.add(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put('id', 'forbidden')) }
    }

    def 'current package boundary rejects unsafe paths, stale hashes, and unsupported roles'() {
        given:
        def fixture = materializeFixture('initial-catalog')
        def manifest = mapper.readTree(Files.readString(fixture.manifest))
        mutation.call(manifest)
        Files.write(fixture.manifest, mapper.writeValueAsBytes(manifest))

        when:
        new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)

        then:
        thrown(IllegalArgumentException)

        where:
        mutation << [
                { ObjectNode m -> m.path('files').path('accounting').put('sha256', '0' * 64) },
                { ObjectNode m -> m.path('files').path('workloads').put('path', '../outside.jsonl') },
                { ObjectNode m -> m.path('files').path('workloads').put('path', '/absolute/workloads.jsonl') },
                { ObjectNode m -> m.path('files').path('workloads').put('path', 'sagas.jsonl') },
                { ObjectNode m -> m.path('files').set('unknownRole', m.path('files').path('accounting')) }
        ]
    }

    def 'on-demand uses accounting default, records overrides, deduplicates exact identity, and preserves initial totals'() {
        given:
        def fixture = materializeFixture('initial-catalog')
        def service = new OnDemandFaultScenarioService()
        def before = snapshot(fixture)
        def initialAccounting = mapper.readTree(Files.readString(fixture.directory.resolve('accounting.json')))
        def request = { String cap -> new OnDemandFaultScenarioRequest(
                fixture.manifest, 'workload-2', '1', cap) }

        when:
        def defaultResult = service.request(request(null))
        def afterDefault = snapshot(fixture)
        def repeatResult = service.request(request(null))
        def overrideResult = service.request(request('1'))
        def current = new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)

        then:
        defaultResult.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        defaultResult.writtenScheduleCount() <= 3
        repeatResult.status() == OnDemandFaultScenarioResult.Status.DEDUPLICATED
        overrideResult.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        current.requestRecords()*.path('effectiveRecoveryScheduleCap')*.asInt().toSet() == [3, 1] as Set
        current.accounting().path('faultScenarios').path('initial').path('vectorsComputed').asInt() == 3
        current.accounting().path('faultScenarios').path('current').path('vectorsComputed').asInt() >
                current.accounting().path('faultScenarios').path('initial').path('vectorsComputed').asInt()
        current.accounting().path('faultScenarios').path('initial').path('written').asInt() == 3
        current.accounting().path('faultScenarios').path('current').path('written').asInt() >= 3
        !Arrays.equals(before.faultScenarios, afterDefault.faultScenarios)
        current.requestRecords().size() == 2
        current.accounting().path('faultScenarios').path('initial') ==
                initialAccounting.path('faultScenarios').path('initial')
        current.accounting().path('requests').path('written').asInt() == 2
        ['accounting', 'faultScenarios', 'requests'].every { role ->
            def path = fixture.directory.resolve(current.manifest().files().get(role).path())
            sha256(Files.readAllBytes(path)) == current.manifest().files().get(role).sha256()
        }
    }

    def 'failed request and injected publication failure preserve all current package bytes'() {
        given:
        def fixture = materializeFixture('initial-catalog')
        def before = snapshot(fixture)

        when:
        def rejected = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, 'workload-1', 'not-binary', null))

        then:
        rejected.status() == OnDemandFaultScenarioResult.Status.REJECTED
        snapshot(fixture) == before

        when:
        def failing = new OnDemandFaultScenarioService(
                { WorkloadPlan plan, String vector, int cap -> RecoveryScheduleGenerator.generate(plan, vector, cap) }
                        as OnDemandFaultScenarioService.RecoveryScheduleSource,
                { boundary -> if (boundary == OnDemandFaultScenarioService.Boundary.REQUEST_PROMOTED) throw new IOException('simulated publication failure') }
                        as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover())
        def failed = failing.request(new OnDemandFaultScenarioRequest(fixture.manifest, 'workload-2', '1', null))

        then:
        failed.status() == OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED
        snapshot(fixture) == before
        new ScenarioCatalogPackageReader().readCurrent(fixture.manifest).requestRecords().isEmpty()
    }

    private Map materializeFixture(String manifestName) {
        def fixturePath = Path.of('..', 'issues', '2026-08-30-current-only-verifier-artifacts',
                'examples', 'current-package.fixture.json').toAbsolutePath().normalize()
        def fixture = mapper.readTree(fixturePath.toFile())
        // The service intentionally rejects symlinked path segments.  macOS
        // exposes the default temporary directory through /var; /private/tmp
        // is the canonical non-symlinked test root.
        def directory = Files.createTempDirectory(Path.of('/private/tmp'), "current-fixture-${manifestName}-")
        def manifest = fixture.path('manifests').path(manifestName)
        manifest.path('files').fields().each { entry ->
            def artifactPath = directory.resolve(entry.value.path('path').asText())
            Files.createDirectories(artifactPath.parent)
            def role = entry.key
            if (role == 'accounting') {
                Files.write(artifactPath, mapper.writeValueAsBytes(fixture.path('artifacts').path('accounting').path(manifestName == 'count-only' ? 'count-only' : 'initial-catalog')))
            } else if (['sagas', 'inputs', 'interactions', 'setups', 'workloads'].contains(role)) {
                Files.write(artifactPath, jsonLines(fixture.path('artifacts').path(role).toList()))
            } else if (role == 'faultScenarios') {
                Files.write(artifactPath, jsonLines(fixture.path('artifacts').path(role).path(manifestName).toList()))
            } else if (role == 'requests') {
                Files.write(artifactPath, jsonLines(fixture.path('artifacts').path(role).path(manifestName).toList()))
            }
        }
        def manifestPath = directory.resolve('scenario-catalog-manifest.json')
        Files.write(manifestPath, mapper.writeValueAsBytes(manifest))
        [manifest: manifestPath, directory: directory]
    }

    private byte[] jsonLines(List<JsonNode> records) {
        if (records.isEmpty()) return new byte[0]
        (records.collect { mapper.writeValueAsString(it) }.join('\n') + '\n').getBytes('UTF-8')
    }

    private void mutateRole(Map fixture, String role, Closure mutation) {
        def manifest = mapper.readTree(Files.readString(fixture.manifest))
        def path = fixture.directory.resolve(manifest.path('files').path(role).path('path').asText())
        def records = Files.readAllLines(path).collect { mapper.readTree(it) }
        def array = mapper.createArrayNode()
        records.each { array.add(it) }
        mutation.call(array)
        Files.write(path, jsonLines(array.toList()))
        // The semantic negative cases must pass the hash gate to exercise the
        // reference validator; path/hash negatives remain covered by the
        // shared reader's manifest boundary checks.
        manifest.path('files').path(role).put('sha256', sha256(Files.readAllBytes(path)))
        Files.write(fixture.manifest, mapper.writeValueAsBytes(manifest))
    }

    private Map<String, byte[]> snapshot(Map fixture) {
        def manifest = mapper.readTree(Files.readString(fixture.manifest))
        def files = manifest.path('files')
        [manifest: Files.readAllBytes(fixture.manifest),
         accounting: bytes(fixture, files, 'accounting'),
         faultScenarios: bytes(fixture, files, 'faultScenarios'),
         requests: bytes(fixture, files, 'requests')]
    }

    private byte[] bytes(Map fixture, JsonNode files, String role) {
        Files.readAllBytes(fixture.directory.resolve(files.path(role).path('path').asText()))
    }
    private static String sha256(byte[] bytes) {
        java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance('SHA-256').digest(bytes))
    }
}
