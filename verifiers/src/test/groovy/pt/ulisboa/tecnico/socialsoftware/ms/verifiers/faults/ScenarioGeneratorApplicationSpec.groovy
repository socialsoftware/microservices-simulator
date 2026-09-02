package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.SpringApplication
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

class ScenarioGeneratorApplicationSpec extends pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.VisitorTestSupport {

    private final ObjectMapper objectMapper = new ObjectMapper()

    @TempDir
    Path tempDir

    def 'application starts when configured application directory exists'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'dummyapp'

        Files.createDirectories(applicationsRoot.resolve(applicationBaseDir))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}"
        )

        then:
        noExceptionThrown()

        cleanup:
        context?.close()
    }

    def 'dynamic enrichment enabled with scenario catalog disabled fails clearly'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'dummyapp'
        Files.createDirectories(applicationsRoot.resolve(applicationBaseDir))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.dynamic-enrichment.enabled=true',
                '--verifiers.scenario-catalog.enabled=false'
        )

        then:
        def ex = thrown(Exception)
        rootCause(ex).message.contains('Dynamic enrichment requires scenario catalog export to be enabled')
    }

    def 'catalog export is disabled by default'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'dummyapp'

        Files.createDirectories(applicationsRoot.resolve(applicationBaseDir))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}"
        )

        then:
        noExceptionThrown()

        and:
        def runDirectory = singleRunDirectory(outputRoot, applicationBaseDir)
        regularArtifactPaths(runDirectory).isEmpty()

        cleanup:
        context?.close()
    }

    def 'configured relative artifact path cannot escape verifier run directory'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'dummyapp'
        Files.createDirectories(applicationsRoot.resolve(applicationBaseDir))
        def outsideArtifact = outputRoot.resolve('outside-workloads.jsonl')

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.scenario-catalog.enabled=true',
                '--verifiers.scenario-catalog.workload-catalog-path=../outside-workloads.jsonl'
        )

        then:
        def ex = thrown(Exception)
        rootCause(ex).message.contains('must stay under verifier run output directory')
        !Files.exists(outsideArtifact)
    }

    def 'configured application base dir cannot escape applications root'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = '../outside-app'
        Files.createDirectories(applicationsRoot)
        Files.createDirectories(tempDir.resolve('outside-app'))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}"
        )

        then:
        def ex = thrown(Exception)
        rootCause(ex).message.contains('must stay under applications root')
    }

    def 'configured absolute artifact paths are rejected'() {
        given:
        def applicationsRoot = tempDir.resolve("applications-${propertyName}")
        def outputRoot = tempDir.resolve("verifier-output-${propertyName}")
        def applicationBaseDir = 'dummyapp'
        Files.createDirectories(applicationsRoot.resolve(applicationBaseDir))
        def absolutePath = tempDir.resolve("outside-${propertyName}.artifact").toAbsolutePath()

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.scenario-catalog.enabled=true',
                "--${propertyName}=${absolutePath}"
        )

        then:
        thrown(Exception)
        !Files.exists(absolutePath)

        where:
        propertyName << [
                'verifiers.scenario-catalog.workload-catalog-path',
                'verifiers.scenario-catalog.fault-scenario-catalog-path',
                'verifiers.scenario-catalog.manifest-path'
        ]
    }

    def 'recovery schedule cap rejects non-positive or malformed values before output mutation'() {
        given:
        def applicationsRoot = tempDir.resolve("applications-recovery-cap-${configuredValue}")
        def outputRoot = tempDir.resolve("verifier-output-recovery-cap-${configuredValue}")
        def applicationBaseDir = 'dummyapp'
        Files.createDirectories(applicationsRoot.resolve(applicationBaseDir))
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                "--verifiers.scenario-catalog.recovery-schedule-cap=${configuredValue}"
        )

        then:
        def error = thrown(Exception)
        rootCause(error).message.contains('recovery schedule cap must be a positive integer')
        !Files.exists(outputRoot)

        where:
        configuredValue << ['0', '-1', 'not-an-integer']
    }

    def 'count only catalog export writes the current static package'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'dummyapp'
        def sourceDummyappRoot = resolveProjectPath('applications', 'dummyapp', 'src')
        def applicationPath = applicationsRoot.resolve(applicationBaseDir)
        copyDirectory(sourceDummyappRoot, applicationPath.resolve('src'))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.scenario-catalog.enabled=true',
                '--verifiers.scenario-catalog.generation-strategy=BRUTE_FORCE',
                '--verifiers.scenario-catalog.catalog-write-mode=COUNT_ONLY',
                '--verifiers.scenario-catalog.max-saga-set-size=2',
                '--verifiers.scenario-catalog.max-catalog-scenarios=1'
        )

        then:
        noExceptionThrown()

        and:
        def runDirectory = singleRunDirectory(outputRoot, applicationBaseDir)
        def manifestPath = runDirectory.resolve('scenario-catalog-manifest.json')
        def accountingPath = runDirectory.resolve('accounting.json')
        def sagaPath = runDirectory.resolve('sagas.jsonl')
        def inputPath = runDirectory.resolve('inputs.jsonl')
        def interactionPath = runDirectory.resolve('interactions.jsonl')
        Files.exists(accountingPath)
        Files.exists(sagaPath)
        Files.exists(inputPath)
        Files.exists(interactionPath)
        Files.exists(manifestPath)
        Files.list(runDirectory).collect { it.fileName.toString() }.sort() ==
                ['accounting.json', 'inputs.jsonl', 'interactions.jsonl', 'sagas.jsonl', 'scenario-catalog-manifest.json']

        and:
        def manifest = objectMapper.readTree(Files.readString(manifestPath))
        manifest.path('formatVersion').isInt()
        manifest.path('files').fieldNames().toList() as Set == ['accounting', 'sagas', 'inputs', 'interactions'] as Set
        manifest.path('files').elements().every { it.fieldNames().toList() as Set == ['path', 'sha256'] as Set }

        and:
        def accounting = objectMapper.readTree(Files.readString(accountingPath))
        accounting.path('configuration').path('catalogWriteMode').asText() == 'count-only'
        accounting.path('workloads').path('written').path('total').asInt() == 0
        !accounting.has('schemaVersion')
        !accounting.has('discovery')
        !accounting.toString().contains('faultScenarioCatalogSpace')

        cleanup:
        context?.close()
    }

    def 'Groovy tracing ignores specifications outside src test groovy'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'main-groovy-only-app'

        writeSource(applicationsRoot, applicationBaseDir,
                'src/main/java/com/example/demo/order/coordination/DemoFunctionalitySagas.java', '''
            package com.example.demo.order.coordination;

            import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
            import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
            import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
            import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
            import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

            public class DemoFunctionalitySagas extends WorkflowFunctionality {
                private final SagaUnitOfWorkService unitOfWorkService;

                public DemoFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              SagaUnitOfWork unitOfWork) {
                    this.unitOfWorkService = unitOfWorkService;
                    buildWorkflow(unitOfWork);
                }

                public void buildWorkflow(SagaUnitOfWork unitOfWork) {
                    this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
                    this.workflow.addStep(new SagaStep("createDemoStep", () -> {}));
                }
            }
        ''')
        writeSource(applicationsRoot, applicationBaseDir,
                'src/main/groovy/demo/MainOnlyTraceSpec.groovy', '''
            package demo

            import com.example.demo.order.coordination.DemoFunctionalitySagas
            import spock.lang.Specification

            class MainOnlyTraceSpec extends Specification {
                def saga = new DemoFunctionalitySagas(null, null)

                def setup() {
                    saga.executeWorkflow(null)
                }
            }
        ''')

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.scenario-catalog.enabled=true'
        )

        then:
        noExceptionThrown()

        and:
        def runDirectory = singleRunDirectory(outputRoot, applicationBaseDir)
        def manifest = objectMapper.readTree(Files.readString(runDirectory.resolve('scenario-catalog-manifest.json')))
        Files.exists(runDirectory.resolve('inputs.jsonl'))
        manifest.path('files').has('inputs')
        regularArtifactContents(runDirectory).every { !it.contains('MainOnlyTraceSpec') }

        cleanup:
        context?.close()
    }

    def 'final catalog cap prioritizes deterministic prerequisite workloads then stable base order'() {
        given:
        def prerequisites = [catalogWorkload('p2'), catalogWorkload('p1')]
        def base = [catalogWorkload('b2'), catalogWorkload('b1'), catalogWorkload('b3')]

        when:
        def selected = ScenarioGeneratorApplication.prioritizeCatalogWorkloads(prerequisites, base, 4)
        def prerequisiteOnly = ScenarioGeneratorApplication.prioritizeCatalogWorkloads(prerequisites, base, 1)

        then:
        selected.workloads()*.deterministicId() == ['p1', 'p2', 'b2', 'b1']
        selected.workloads().size() == 4
        selected.prerequisiteGenerated() == 2
        selected.prerequisiteSelected() == 2
        selected.prerequisiteCapped() == 0
        selected.baseGenerated() == 3
        selected.baseSelected() == 2
        selected.baseCapped() == 1

        and:
        prerequisiteOnly.workloads()*.deterministicId() == ['p1']
        prerequisiteOnly.prerequisiteSelected() == 1
        prerequisiteOnly.prerequisiteCapped() == 1
        prerequisiteOnly.baseSelected() == 0
        prerequisiteOnly.baseCapped() == 3
    }

    def 'enabled catalog export writes exactly the current executable package artifacts'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'dummyapp'
        def sourceDummyappRoot = resolveProjectPath('applications', 'dummyapp', 'src')
        def applicationPath = applicationsRoot.resolve(applicationBaseDir)
        copyDirectory(sourceDummyappRoot, applicationPath.resolve('src'))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.scenario-catalog.enabled=true',
                '--verifiers.scenario-catalog.max-saga-set-size=1'
        )

        then:
        noExceptionThrown()

        and:
        def runDirectory = singleRunDirectory(outputRoot, applicationBaseDir)
        regularArtifactPaths(runDirectory) == [
                'fault-scenarios.jsonl',
                'inputs.jsonl',
                'interactions.jsonl',
                'requests.jsonl',
                'sagas.jsonl',
                'scenario-catalog-manifest.json',
                'setups.jsonl',
                'accounting.json',
                'workloads.jsonl'
        ] as Set

        cleanup:
        context?.close()
    }

    private static WorkloadPlan catalogWorkload(String id) {
        new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, id, null, null,
                [], [], [], [], [], null, [], [], [], [])
    }

    private static Path writeSource(Path applicationsRoot,
                                    String applicationBaseDir,
                                    String relativePath,
                                    String contents) {
        def file = applicationsRoot.resolve(applicationBaseDir).resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, contents.stripIndent().trim() + '\n')
        return file
    }

    private static void copyDirectory(Path sourceRoot, Path targetRoot) {
        Files.walk(sourceRoot).withCloseable { stream ->
            stream.forEach { source ->
                def relativePath = sourceRoot.relativize(source)
                def destination = relativePath.toString().isEmpty() ? targetRoot : targetRoot.resolve(relativePath.toString())
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination)
                } else {
                    Files.createDirectories(destination.parent)
                    Files.copy(source, destination)
                }
            }
        }
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable
        while (current.cause != null) {
            current = current.cause
        }
        current
    }

    private static Path singleRunDirectory(Path outputRoot, String applicationBaseDir) {
        def stream = Files.list(outputRoot)
        try {
            def runDirectories = stream
                    .filter { Files.isDirectory(it) }
                    .filter { it.fileName.toString().startsWith(applicationBaseDir + '-') }
                    .sorted()
                    .toList()
            assert runDirectories.size() == 1
            return runDirectories[0]
        } finally {
            stream.close()
        }
    }

    private static Set<String> regularArtifactPaths(Path runDirectory) {
        Files.walk(runDirectory).withCloseable { stream ->
            stream
                    .filter { Files.isRegularFile(it) }
                    .map { runDirectory.relativize(it).toString() }
                    .collect(java.util.stream.Collectors.toSet())
        }
    }

    private static List<String> regularArtifactContents(Path runDirectory) {
        Files.walk(runDirectory).withCloseable { stream ->
            stream
                    .filter { Files.isRegularFile(it) }
                    .map { Files.readString(it) }
                    .toList()
        }
    }

    private static List<Path> findTimestampedSiblingReports(Path stableReportPath) {
        def fileName = stableReportPath.getFileName().toString()
        int extensionStart = fileName.lastIndexOf('.')
        def baseName = extensionStart >= 0 ? fileName.substring(0, extensionStart) : fileName
        def extension = extensionStart >= 0 ? fileName.substring(extensionStart) : ''
        def archivePattern = Pattern.compile(Pattern.quote(baseName) + '-\\d{8}-\\d{6}-\\d{3}' + Pattern.quote(extension))

        def stream = Files.list(stableReportPath.getParent())
        try {
            return stream
                    .filter { path -> archivePattern.matcher(path.getFileName().toString()).matches() }
                    .sorted()
                    .toList()
        } finally {
            stream.close()
        }
    }
}
