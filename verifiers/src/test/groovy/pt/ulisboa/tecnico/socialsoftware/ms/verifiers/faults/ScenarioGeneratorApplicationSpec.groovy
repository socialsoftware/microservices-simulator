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
        Files.exists(runDirectory.resolve('analysis-report.html'))
        !Files.exists(runDirectory.resolve('scenario-catalog.jsonl'))
        !Files.exists(runDirectory.resolve('scenario-catalog-manifest.json'))

        cleanup:
        context?.close()
    }

    def 'configured relative output paths cannot escape verifier run directory'() {
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
                '--verifiers.report-html-path=../outside-analysis-report.html'
        )

        then:
        thrown(Exception)
        !Files.exists(outputRoot.resolve('outside-analysis-report.html'))
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
                'verifiers.report-html-path',
                'verifiers.scenario-catalog.catalog-path',
                'verifiers.scenario-catalog.manifest-path',
                'verifiers.scenario-catalog.rejected-inputs-path'
        ]
    }

    def 'Groovy tracing runs after Java saga discovery and report includes Groovy traces'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'integration-app'

        writeSource(applicationsRoot, applicationBaseDir, 'src/main/java/com/example/demo/order/coordination/DemoFunctionalitySagas.java', '''
            package com.example.demo.order.coordination;

            import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaStep;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaWorkflow;

            public class DemoFunctionalitySagas extends WorkflowFunctionality {

                private final SagaUnitOfWorkService unitOfWorkService;

                public DemoFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              SagaUnitOfWork unitOfWork) {
                    this.unitOfWorkService = unitOfWorkService;
                    buildWorkflow(unitOfWork);
                }

                public void buildWorkflow(SagaUnitOfWork unitOfWork) {
                    this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
                    SagaStep createDemoStep = new SagaStep("createDemoStep", () -> {});
                    workflow.addStep(createDemoStep);
                }
            }
        ''')

        writeSource(applicationsRoot, applicationBaseDir, 'src/main/java/com/example/demo/order/aggregate/DemoAggregate.java', '''
            package com.example.demo.order.aggregate;

            import jakarta.persistence.Entity;
            import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
            import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription;

            import java.util.HashSet;
            import java.util.Set;

            @Entity
            public class DemoAggregate extends Aggregate {

                public DemoAggregate() {}

                public DemoAggregate(Integer aggregateId) {
                    super(aggregateId);
                    setAggregateType(getClass().getSimpleName());
                }

                @Override
                public void verifyInvariants() {}

                @Override
                public Set<EventSubscription> getEventSubscriptions() {
                    return new HashSet<>();
                }
            }
        ''')

        writeSource(applicationsRoot, applicationBaseDir, 'src/main/java/com/example/demo/order/commands/DemoCommand.java', '''
            package com.example.demo.order.commands;

            import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

            public class DemoCommand extends Command {

                private final Integer aggregateId;

                public DemoCommand(UnitOfWork unitOfWork, String serviceName, Integer aggregateId) {
                    super(unitOfWork, serviceName, aggregateId);
                    this.aggregateId = aggregateId;
                }

                public Integer getAggregateId() {
                    return aggregateId;
                }
            }
        ''')

        writeSource(applicationsRoot, applicationBaseDir, 'src/main/java/com/example/demo/order/service/DemoService.java', '''
            package com.example.demo.order.service;

            import org.springframework.stereotype.Service;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWorkService;
            import com.example.demo.order.aggregate.DemoAggregate;

            @Service
            public class DemoService {

                private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

                public DemoService(UnitOfWorkService<UnitOfWork> unitOfWorkService) {
                    this.unitOfWorkService = unitOfWorkService;
                }

                public DemoAggregate touch(Integer aggregateId, UnitOfWork unitOfWork) {
                    DemoAggregate aggregate = new DemoAggregate(aggregateId);
                    unitOfWorkService.registerChanged(aggregate, unitOfWork);
                    return aggregate;
                }
            }
        ''')

        writeSource(applicationsRoot, applicationBaseDir, 'src/main/java/com/example/demo/order/commandHandler/DemoCommandHandler.java', '''
            package com.example.demo.order.commandHandler;

            import org.springframework.stereotype.Component;
            import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
            import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
            import com.example.demo.order.commands.DemoCommand;
            import com.example.demo.order.service.DemoService;

            @Component
            public class DemoCommandHandler extends CommandHandler {

                private final DemoService demoService;

                public DemoCommandHandler(DemoService demoService) {
                    this.demoService = demoService;
                }

                @Override
                protected String getAggregateTypeName() {
                    return "Demo";
                }

                @Override
                protected Object handleDomainCommand(Command command) {
                    return switch (command) {
                        case DemoCommand cmd -> handleDemoCommand(cmd);
                        default -> null;
                    };
                }

                private Object handleDemoCommand(DemoCommand cmd) {
                    return demoService.touch(cmd.getAggregateId(), cmd.getUnitOfWork());
                }
            }
        ''')

        writeSource(applicationsRoot, applicationBaseDir, 'src/test/groovy/demo/DemoTraceSpec.groovy', '''
            package demo

            import com.example.demo.order.coordination.DemoFunctionalitySagas
            import spock.lang.Specification

            class DemoTraceSpec extends Specification {
                def saga = new DemoFunctionalitySagas(null, null)

                def setup() {
                    saga.executeWorkflow(null)
                }
            }
        ''')

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
        runDirectory.fileName.toString() ==~ /integration-app-\d{8}-\d{6}-\d{3}/
        def htmlReportPath = runDirectory.resolve('analysis-report.html')
        Files.exists(htmlReportPath)
        def htmlReport = Files.readString(htmlReportPath)
        htmlReport.contains('Verifier Analysis Report')
        htmlReport.contains('Groovy Trace Explorer')
        htmlReport.contains('Source-mode classification (1 trace(s))')
        htmlReport.contains('source mode: UNKNOWN')
        htmlReport.contains('Services (1)')
        htmlReport.contains('DemoService')
        htmlReport.contains('Command handlers (1)')
        htmlReport.contains('DemoCommand -&gt; DemoCommandHandler')
        htmlReport.contains('Sagas (1)')
        htmlReport.contains('Groovy constructor-input traces (1)')
        htmlReport.contains('Groovy full traces (1)')
        htmlReport.contains('DemoTraceSpec')
        htmlReport.contains('field:saga()')
        htmlReport.contains('arg[0]: null')
        htmlReport.indexOf('Sagas (1)') < htmlReport.indexOf('Groovy constructor-input traces (1)')
        htmlReport.contains('DemoTraceSpec')
        htmlReport.contains('Raw Text Report (verbatim)')

        and:
        findTimestampedSiblingReports(htmlReportPath).isEmpty()

        cleanup:
        context?.close()
    }

    def 'quizzes report exposes nested helper provenance for CreateTournamentFaultTest'() {
        given:
        def applicationsRoot = resolveProjectPath('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'quizzes'
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
        def htmlReport = Files.readString(singleRunDirectory(outputRoot, applicationBaseDir).resolve('analysis-report.html'))
        htmlReport.contains('CreateTournamentFaultTest')

        and:
        def createTournamentFaultTraceMatcher = htmlReport =~
                /(?s)<code>CreateTournamentFaultTest\.<\/code><code>[^<]*\(\)<\/code>.*?<pre class="trace-pre">(.*?)<\/pre>/
        String targetedTraceBlock = null
        while (createTournamentFaultTraceMatcher.find()) {
            def traceBlock = createTournamentFaultTraceMatcher.group(1)
            if (traceBlock.contains('createCourseExecution(...)') && traceBlock.contains('createUser(...)')) {
                targetedTraceBlock = traceBlock
                break
            }
        }

        targetedTraceBlock != null
        targetedTraceBlock.contains('createCourseExecution(...)')
        targetedTraceBlock.contains('createUser(...)')
        !targetedTraceBlock.contains('[unresolved cyclic reference]')

        and:
        def updateCreateUserBlock = findTraceBlock(
                htmlReport,
                'UpdateTournamentTest',
                'createUser',
                'userFunctionalities.createUser(userDto)'
        )
        def updateCreateCourseExecutionBlock = findTraceBlock(
                htmlReport,
                'UpdateTournamentTest',
                'createCourseExecution',
                'courseExecutionFunctionalities.createCourseExecution(courseExecutionDto)'
        )
        def updateCreateTournamentBlock = findTraceBlock(
                htmlReport,
                'UpdateTournamentTest',
                'createTournament',
                'tournamentFunctionalities.createTournament(userCreatorId, courseExecutionId, topicIds, tournamentDto)'
        )

        updateCreateUserBlock != null
        updateCreateUserBlock.contains('arg[1]: userDto &lt;- new UserDto(')
        !updateCreateUserBlock.contains('[unresolved cyclic reference]')
        !updateCreateUserBlock.contains('[unresolved self-reference]')

        updateCreateCourseExecutionBlock != null
        updateCreateCourseExecutionBlock.contains('arg[1]: courseExecutionDto &lt;- new CourseExecutionDto(')
        !updateCreateCourseExecutionBlock.contains('[unresolved cyclic reference]')
        !updateCreateCourseExecutionBlock.contains('[unresolved self-reference]')

        updateCreateTournamentBlock != null
        updateCreateTournamentBlock.contains('arg[4]: tournamentDto &lt;- new TournamentDto(')
        !updateCreateTournamentBlock.contains('[unresolved cyclic reference]')
        !updateCreateTournamentBlock.contains('[unresolved self-reference]')

        and:
        htmlReport.contains('UpdateTournamentTest.update tournament successfully()')
        htmlReport.contains('topicsAggregateIds &lt;- ')
        htmlReport.contains('LOCAL_TRANSFORM: toSet')
        !htmlReport.contains('topicsAggregateIds &lt;- [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet() [unresolved external/runtime edge]')

        and:
        def abortCreateTopicBlock = findTraceBlock(
                htmlReport,
                'AbortUpdateAndRetryTest',
                'createTopic',
                'topicFunctionalities.createTopic(courseExecutionDto.getCourseAggregateId(), topicDto)'
        )

        abortCreateTopicBlock != null
        !abortCreateTopicBlock.contains('[unresolved self-reference]')
        !abortCreateTopicBlock.contains('[unresolved depth-limit]')

        and:
        !htmlReport.contains('courseExecutionDto [unresolved self-reference].courseAggregateId')
        !htmlReport.contains('topicFunctionalities.createTopic(courseExecutionDto.getCourseAggregateId(), topicDto) [unresolved depth-limit]')
        !htmlReport.contains('arg[1]: userDto &lt;- new UserDto() [unresolved depth-limit]')

        cleanup:
        context?.close()
    }

    def 'Groovy tracing scans only src/test/groovy and ignores src/main/groovy specifications'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'integration-app-main-groovy'

        writeSource(applicationsRoot, applicationBaseDir, 'src/main/java/com/example/demo/order/coordination/DemoFunctionalitySagas.java', '''
            package com.example.demo.order.coordination;

            import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaStep;
            import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaWorkflow;

            public class DemoFunctionalitySagas extends WorkflowFunctionality {

                private final SagaUnitOfWorkService unitOfWorkService;

                public DemoFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              SagaUnitOfWork unitOfWork) {
                    this.unitOfWorkService = unitOfWorkService;
                    buildWorkflow(unitOfWork);
                }

                public void buildWorkflow(SagaUnitOfWork unitOfWork) {
                    this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
                    SagaStep createDemoStep = new SagaStep("createDemoStep", () -> {});
                    workflow.addStep(createDemoStep);
                }
            }
        ''')

        writeSource(applicationsRoot, applicationBaseDir, 'src/main/groovy/demo/MainOnlyTraceSpec.groovy', '''
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
                "--verifiers.output-root=${outputRoot}"
        )

        then:
        noExceptionThrown()

        and:
        def htmlReportPath = singleRunDirectory(outputRoot, applicationBaseDir).resolve('analysis-report.html')
        Files.exists(htmlReportPath)
        def htmlReport = Files.readString(htmlReportPath)
        htmlReport.contains('No constructor traces found.')
        !htmlReport.contains('MainOnlyTraceSpec')

        cleanup:
        context?.close()
    }

    def 'application writes html report to configured relative path'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def outputRoot = tempDir.resolve('verifier-output')
        def applicationBaseDir = 'custom-report-app'
        Files.createDirectories(applicationsRoot.resolve(applicationBaseDir))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.report-html-path=reports/custom-analysis.html'
        )

        then:
        noExceptionThrown()

        and:
        def runDirectory = singleRunDirectory(outputRoot, applicationBaseDir)
        def customReport = runDirectory.resolve('reports/custom-analysis.html')
        def defaultReport = runDirectory.resolve('analysis-report.html')
        Files.exists(customReport)
        !Files.exists(defaultReport)
        findTimestampedSiblingReports(customReport).isEmpty()

        cleanup:
        context?.close()
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
        def catalogRelativePath = 'exports/scenario-catalog.jsonl'
        def manifestRelativePath = 'exports/scenario-catalog-manifest.json'
        def rejectedRelativePath = 'exports/scenario-catalog-rejected-inputs.jsonl'
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}",
                "--verifiers.output-root=${outputRoot}",
                '--verifiers.scenario-catalog.enabled=true',
                '--verifiers.scenario-catalog.max-saga-set-size=1',
                "--verifiers.scenario-catalog.catalog-path=${catalogRelativePath}",
                "--verifiers.scenario-catalog.manifest-path=${manifestRelativePath}",
                "--verifiers.scenario-catalog.rejected-inputs-path=${rejectedRelativePath}"
        )

        then:
        noExceptionThrown()

        and:
        def runDirectory = singleRunDirectory(outputRoot, applicationBaseDir)
        def catalogPath = runDirectory.resolve(catalogRelativePath)
        def manifestPath = runDirectory.resolve(manifestRelativePath)
        def rejectedPath = runDirectory.resolve(rejectedRelativePath)
        Files.exists(catalogPath)
        Files.exists(manifestPath)
        Files.exists(rejectedPath)

        and:
        def lines = Files.readAllLines(catalogPath)
        lines.size() > 0
        lines.each { line ->
            def scenario = objectMapper.readTree(line)
            scenario.path('inputs').each { input ->
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
            ['TCC', 'MIXED'].contains(rejected.path('sourceMode').asText()) &&
                    rejected.path('rejectionReason').asText()
        }
        def tccFixtureRejection = rejectedInputs.find {
            it.path('sourceClassFqn').asText() == 'com.example.dummyapp.GroovyTccSourceModeTracingSpec'
        }
        tccFixtureRejection != null
        tccFixtureRejection.path('sourceMode').asText() == 'TCC'
        tccFixtureRejection.path('rejectionReason').asText() == 'SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG'
        def rejectedIds = rejectedInputs.collect { it.path('deterministicId').asText() } as Set
        lines.every { line ->
            def scenario = objectMapper.readTree(line)
            scenario.path('inputs').every { input -> !rejectedIds.contains(input.path('deterministicId').asText()) }
        }

        and:
        def manifest = objectMapper.readTree(Files.readString(manifestPath))
        manifest.path('counts').path('scenariosExported').asInt() == lines.size()
        manifest.path('catalogPath').asText() == catalogPath.toString()
        manifest.path('manifestPath').asText() == manifestPath.toString()
        manifest.path('rejectedInputsPath').asText() == rejectedPath.toString()
        manifest.has('catalogArchivePath') == false
        manifest.has('manifestArchivePath') == false
        manifest.has('rejectedInputsArchivePath') == false
        manifest.path('counts').path('rejectedInputsExported').asInt() == rejectedLines.size()
        manifest.path('inputVariantsBySourceMode').has('SAGAS')
        manifest.path('inputVariantsAcceptedBySourceMode').has('SAGAS')
        manifest.path('inputVariantsRejectedBySourceModeReason').path('SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG').asInt() >= 1
        manifest.path('inputVariantsBySourceMode').path('TCC').asInt() >= 1
        manifest.path('inputVariantsAcceptedBySourceMode').path('TCC').asInt() == 0
        manifest.path('effectiveConfig').path('exportEnabled').asBoolean()
        manifest.path('effectiveConfig').path('maxSagaSetSize').asInt() == 1

        and:
        findTimestampedSiblingReports(catalogPath).isEmpty()
        findTimestampedSiblingReports(manifestPath).isEmpty()
        findTimestampedSiblingReports(rejectedPath).isEmpty()

        cleanup:
        context?.close()
    }

    def 'html report still writes when catalog export is enabled'() {
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
        def htmlReportPath = singleRunDirectory(outputRoot, applicationBaseDir).resolve('analysis-report.html')
        Files.exists(htmlReportPath)
        def htmlReport = Files.readString(htmlReportPath)
        htmlReport.contains('Source-mode classification')
        htmlReport.contains('source mode: TCC')
        htmlReport.contains('source mode: UNKNOWN')
        htmlReport.contains('@ActiveProfiles(tcc)')
        findTimestampedSiblingReports(htmlReportPath).isEmpty()

        cleanup:
        context?.close()
    }

    private Path writeSource(Path applicationsRoot, String applicationBaseDir, String relativePath, String contents) {
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

    private static String findTraceBlock(String htmlReport,
                                          String sourceClassSimpleName,
                                          String sourceMethodName,
                                          String requiredSnippet) {
        def pattern = Pattern.compile(
                '(?s)<code>' + Pattern.quote(sourceClassSimpleName)
                        + '\\.</code><code>'
                        + Pattern.quote(sourceMethodName)
                        + '\\(\\)</code>.*?<pre class="trace-pre">(.*?)</pre>'
        )
        def matcher = pattern.matcher(htmlReport)
        while (matcher.find()) {
            def block = matcher.group(1)
            if (requiredSnippet == null || block.contains(requiredSnippet)) {
                return block
            }
        }
        return null
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
