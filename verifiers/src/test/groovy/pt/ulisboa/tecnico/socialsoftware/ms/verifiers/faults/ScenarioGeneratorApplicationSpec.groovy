package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.SpringApplication
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
        def outsideArtifact = outputRoot.resolve('outside-workload-catalog.jsonl')

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.scenario-catalog.enabled=true',
                '--verifiers.scenario-catalog.workload-catalog-path=../outside-workload-catalog.jsonl'
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
                'verifiers.scenario-catalog.manifest-path',
                'verifiers.scenario-catalog.rejected-inputs-path'
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

    def 'enabled catalog export writes jsonl and manifest'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'dummyapp'
        def sourceDummyappRoot = resolveProjectPath('applications', 'dummyapp', 'src')
        def applicationPath = applicationsRoot.resolve(applicationBaseDir)
        copyDirectory(sourceDummyappRoot, applicationPath.resolve('src'))

        and:
        def workloadRelativePath = 'exports/workload-catalog.jsonl'
        def faultScenarioRelativePath = 'exports/fault-scenario-catalog.jsonl'
        def manifestRelativePath = 'exports/scenario-catalog-manifest.json'
        def rejectedRelativePath = 'exports/workload-catalog-rejected-inputs.jsonl'
        def accountingRelativePath = 'exports/scenario-space-accounting.json'
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.scenario-catalog.enabled=true',
                '--verifiers.scenario-catalog.max-saga-set-size=1',
                "--verifiers.scenario-catalog.workload-catalog-path=${workloadRelativePath}",
                "--verifiers.scenario-catalog.fault-scenario-catalog-path=${faultScenarioRelativePath}",
                "--verifiers.scenario-catalog.manifest-path=${manifestRelativePath}",
                "--verifiers.scenario-catalog.rejected-inputs-path=${rejectedRelativePath}",
                "--verifiers.scenario-catalog.accounting-path=${accountingRelativePath}",
                '--verifiers.scenario-catalog.generation-strategy=BRUTE_FORCE',
                '--verifiers.scenario-catalog.catalog-write-mode=WRITE_WORKLOADS',
                '--verifiers.scenario-catalog.recovery-schedule-cap=3'
        )

        then:
        noExceptionThrown()

        and:
        def runDirectory = singleRunDirectory(outputRoot, applicationBaseDir)
        def workloadPath = runDirectory.resolve(workloadRelativePath)
        def faultScenarioPath = runDirectory.resolve(faultScenarioRelativePath)
        def manifestPath = runDirectory.resolve(manifestRelativePath)
        def rejectedPath = runDirectory.resolve(rejectedRelativePath)
        def accountingPath = runDirectory.resolve(accountingRelativePath)
        Files.exists(workloadPath)
        Files.exists(faultScenarioPath)
        def faultScenarioLines = Files.readAllLines(faultScenarioPath)
        def faultScenarios = faultScenarioLines.collect { objectMapper.readTree(it) }
        !Files.exists(runDirectory.resolve('scenario-catalog.jsonl'))
        Files.exists(manifestPath)
        Files.exists(rejectedPath)
        Files.exists(accountingPath)

        and:
        def lines = Files.readAllLines(workloadPath)
        def workloads = lines.collect { objectMapper.readTree(it) }
        lines.size() > 0
        workloads.each { workload ->
            workload.path('acceptedInputs').each { input ->
                assert input.path('sourceMode').asText()
                assert input.path('sourceModeConfidence').asText()
                assert input.has('sourceModeEvidence')
                assert !['TCC', 'MIXED'].contains(input.path('sourceMode').asText())
            }
        }

        and:
        def rejectedLines = Files.readAllLines(rejectedPath)
        rejectedLines.size() > 0
        def rejectedInputs = rejectedLines.collect { objectMapper.readTree(it) }
        rejectedInputs.every { rejected ->
            ['TCC', 'MIXED'].contains(rejected.path('input').path('sourceMode').asText()) &&
                    rejected.path('rejectionReason').asText()
        }
        def tccFixtureRejection = rejectedInputs.find {
            it.path('input').path('sourceClassFqn').asText() == 'com.example.dummyapp.GroovyTccSourceModeTracingSpec'
        }
        tccFixtureRejection != null
        tccFixtureRejection.path('schemaVersion').asText() == 'microservices-simulator.workload-catalog-rejected-input.v3'
        tccFixtureRejection.path('input').path('sourceMode').asText() == 'TCC'
        tccFixtureRejection.path('rejectionReason').asText() == 'SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG'
        def rejectedIds = rejectedInputs.collect { it.path('input').path('deterministicId').asText() } as Set
        workloads.every { workload ->
            workload.path('acceptedInputs').every { input -> !rejectedIds.contains(input.path('deterministicId').asText()) }
        }

        and:
        def manifest = objectMapper.readTree(Files.readString(manifestPath))
        manifest.path('schemaVersion').asText() == 'microservices-simulator.scenario-catalog-manifest.v3'
        manifest.path('counts').path('workloadsExported').asText() == lines.size().toString()
        manifest.path('workloadCatalog').path('path').asText() == workloadPath.toString()
        manifest.path('workloadCatalog').path('schemaVersion').asText() == 'microservices-simulator.workload-plan.v3'
        manifest.path('faultScenarioCatalog').path('path').asText() == faultScenarioPath.toString()
        manifest.path('faultScenarioCatalog').path('recordCount').asText() == faultScenarioLines.size().toString()
        manifest.path('recoveryScheduleCap').asInt() == 3
        manifest.path('faultScenarioVectorSource').asText() == 'EAGER_ALL_ZERO_AND_SINGLE_POINT'
        manifest.path('materializabilityPolicy').asText().contains('RUNTIME_MATERIALIZATION_UNPROVEN')
        def materializableWorkloadIds = manifest.path('workloadMaterializability')
                .findAll { it.path('materializable').asBoolean() }
                .collect { it.path('workloadPlanId').asText() } as Set
        def expectedEagerVectorCount = workloads
                .findAll { materializableWorkloadIds.contains(it.path('deterministicId').asText()) }
                .sum { it.path('faultSlots').size() + 1 }
        materializableWorkloadIds.size() == 7
        manifest.path('counts').path('materializableWorkloadPlans').asInt() == materializableWorkloadIds.size()
        manifest.path('counts').path('nonMaterializableWorkloadPlans').asInt() == workloads.size() - materializableWorkloadIds.size()
        expectedEagerVectorCount == 17
        manifest.path('counts').path('computedEagerVectors').asInt() == expectedEagerVectorCount
        faultScenarios.size() == expectedEagerVectorCount
        faultScenarios.collect { it.path('workloadPlanId').asText() }.toSet() == materializableWorkloadIds
        manifest.path('counts').path('faultScenariosExported').asText() == faultScenarioLines.size().toString()
        manifest.path('rejectedInputsDiagnostic').path('path').asText() == rejectedPath.toString()
        manifest.path('scenarioSpaceAccounting').path('path').asText() == accountingPath.toString()
        manifest.has('catalogArchivePath') == false
        manifest.has('manifestArchivePath') == false
        manifest.has('rejectedInputsArchivePath') == false
        manifest.path('counts').path('rejectedInputsExported').asText() == rejectedLines.size().toString()
        manifest.path('inputVariantsBySourceMode').has('SAGAS')
        manifest.path('inputVariantsAcceptedBySourceMode').has('SAGAS')
        manifest.path('inputVariantsRejectedBySourceModeReason').path('SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG').asInt() >= 1
        manifest.path('inputVariantsBySourceMode').path('TCC').asInt() >= 1
        manifest.path('inputVariantsAcceptedBySourceMode').path('TCC').asInt() == 0
        manifest.path('effectiveConfig').path('exportEnabled').asBoolean()
        manifest.path('effectiveConfig').path('generationStrategy').asText() == 'BRUTE_FORCE'
        manifest.path('effectiveConfig').path('catalogWriteMode').asText() == 'WRITE_WORKLOADS'
        manifest.path('effectiveConfig').path('maxSagaSetSize').asInt() == 1

        and:
        def accounting = objectMapper.readTree(Files.readString(accountingPath))
        accounting.path('schemaVersion').asText() == 'microservices-simulator.scenario-space-accounting.v3'
        accounting.path('runConfig').path('targetApplication').asText() == applicationBaseDir
        accounting.path('runConfig').path('generationStrategy').asText() == 'BRUTE_FORCE'
        accounting.path('runConfig').path('catalogWriteMode').asText() == 'WRITE_WORKLOADS'
        accounting.path('runConfig').path('includeSingles').asBoolean()
        accounting.path('runConfig').path('maxSagaSetSize').asInt() == 1
        accounting.path('runConfig').path('maxInputVariantsPerSaga').asInt() == 3
        accounting.path('runConfig').path('maxSchedulesPerInputTuple').asInt() == 20
        accounting.path('runConfig').path('maxCatalogScenarios').asInt() == 100
        accounting.path('runConfig').path('scheduleStrategy').asText() == 'SERIAL'
        accounting.path('runConfig').path('allowTypeOnlyFallback').asBoolean() == false
        accounting.path('runConfig').path('inputPolicy').asText() == 'RESOLVED_OR_REPLAYABLE'
        accounting.path('runConfig').path('sourceModeHandling').asText().contains('TCC and MIXED rejected')
        accounting.path('inputBoundScenarioSpace').path('allInputBound').path('total').isTextual()
        accounting.path('inputBoundScenarioSpace').path('catalogWritten').path('total').asText() == lines.size().toString()
        accounting.path('workloadCatalogSpace').path('materializableWorkloadPlans').asInt() == materializableWorkloadIds.size()
        accounting.path('workloadCatalogSpace').path('nonMaterializableWorkloadPlans').asInt() == workloads.size() - materializableWorkloadIds.size()
        accounting.path('faultScenarioCatalogSpace').path('computedEagerVectorCount').asInt() == expectedEagerVectorCount
        accounting.path('faultScenarioCatalogSpace').path('faultScenariosWritten').asText() == faultScenarioLines.size().toString()
        accounting.path('faultScenarioCatalogSpace').path('allVectorRecoveryTotalStatus').asText() == 'NOT_COMPUTED'

        and:
        findTimestampedSiblingReports(workloadPath).isEmpty()
        findTimestampedSiblingReports(faultScenarioPath).isEmpty()
        findTimestampedSiblingReports(manifestPath).isEmpty()
        findTimestampedSiblingReports(rejectedPath).isEmpty()
        findTimestampedSiblingReports(accountingPath).isEmpty()

        cleanup:
        context?.close()
    }

    def 'count only catalog export writes empty catalog with complete accounting'() {
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
        def workloadPath = runDirectory.resolve('workload-catalog.jsonl')
        def faultScenarioPath = runDirectory.resolve('fault-scenario-catalog.jsonl')
        def manifestPath = runDirectory.resolve('scenario-catalog-manifest.json')
        def rejectedPath = runDirectory.resolve('workload-catalog-rejected-inputs.jsonl')
        def accountingPath = runDirectory.resolve('scenario-space-accounting.json')
        Files.exists(workloadPath)
        Files.exists(faultScenarioPath)
        Files.exists(manifestPath)
        Files.exists(rejectedPath)
        Files.exists(accountingPath)
        Files.readAllLines(workloadPath).isEmpty()
        Files.readAllLines(faultScenarioPath).isEmpty()

        and:
        def manifest = objectMapper.readTree(Files.readString(manifestPath))
        manifest.path('effectiveConfig').path('catalogWriteMode').asText() == 'COUNT_ONLY'
        manifest.path('counts').path('workloadsExported').asText() == '0'

        and:
        def accounting = objectMapper.readTree(Files.readString(accountingPath))
        accounting.path('runConfig').path('generationStrategy').asText() == 'BRUTE_FORCE'
        accounting.path('runConfig').path('catalogWriteMode').asText() == 'COUNT_ONLY'
        accounting.path('inputBoundScenarioSpace').path('catalogWritten').path('total').asText() == '0'
        accounting.path('inputBoundScenarioSpace').path('allInputBound').path('total').asText() != '0'
        accounting.path('inputBoundScenarioSpace').path('selectedByGenerator').path('total').asText() ==
                accounting.path('inputBoundScenarioSpace').path('allInputBound').path('total').asText()
        accounting.path('workloadCatalogSpace').path('workloadPlansWritten').asText() == '0'
        accounting.path('faultScenarioCatalogSpace').path('computedEagerVectorCount').asText() == '0'
        accounting.path('faultScenarioCatalogSpace').path('faultScenariosWritten').asText() == '0'
        accounting.path('faultScenarioCatalogSpace').path('allVectorRecoveryTotalStatus').asText() == 'NOT_COMPUTED'

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
        def workloadPath = runDirectory.resolve('workload-catalog.jsonl')
        def faultScenarioPath = runDirectory.resolve('fault-scenario-catalog.jsonl')
        def rejectedInputsPath = runDirectory.resolve('workload-catalog-rejected-inputs.jsonl')
        def manifest = objectMapper.readTree(Files.readString(runDirectory.resolve('scenario-catalog-manifest.json')))
        Files.readAllLines(workloadPath).isEmpty()
        Files.readAllLines(faultScenarioPath).isEmpty()
        Files.readAllLines(rejectedInputsPath).isEmpty()
        manifest.path('counts').path('inputTracesSeen').asInt() == 0
        manifest.path('counts').path('inputVariantsAdapted').asInt() == 0
        manifest.path('counts').path('workloadsExported').asInt() == 0
        regularArtifactContents(runDirectory).every { !it.contains('MainOnlyTraceSpec') }

        cleanup:
        context?.close()
    }

    def 'enabled catalog export writes exactly the five v3 package artifacts'() {
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
                'fault-scenario-catalog.jsonl',
                'scenario-catalog-manifest.json',
                'scenario-space-accounting.json',
                'workload-catalog-rejected-inputs.jsonl',
                'workload-catalog.jsonl'
        ] as Set

        cleanup:
        context?.close()
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
