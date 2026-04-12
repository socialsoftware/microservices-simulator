package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults

import org.springframework.boot.SpringApplication
import spock.lang.Specification
import spock.lang.TempDir

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path

class ScenarioGeneratorApplicationSpec extends Specification {

    @TempDir
    Path tempDir

    def 'application starts when configured application directory exists'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def applicationBaseDir = 'dummyapp'

        Files.createDirectories(applicationsRoot.resolve(applicationBaseDir))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}"
        )

        then:
        noExceptionThrown()

        cleanup:
        context?.close()
    }

    def 'Groovy tracing runs after Java saga discovery and report includes Groovy traces'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
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

        def originalOut = System.out
        def originalErr = System.err
        def outBuffer = new ByteArrayOutputStream()
        def errBuffer = new ByteArrayOutputStream()
        System.setOut(new PrintStream(outBuffer, true))
        System.setErr(new PrintStream(errBuffer, true))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}"
        )

        then:
        noExceptionThrown()

        and:
        def capturedOutput = outBuffer.toString() + errBuffer.toString()
        def reportIndex = capturedOutput.indexOf('Analysis report:\n')
        reportIndex >= 0
        def report = capturedOutput.substring(reportIndex + 'Analysis report:\n'.length())
        report.contains('Services (1)')
        report.contains('DemoService')
        report.contains('Command handlers (1)')
        report.contains('DemoCommand -> DemoCommandHandler')
        report.contains('Sagas (1)')
        report.contains('Groovy constructor-input traces (1)')
        report.contains('Groovy full traces (1)')
        report.contains('DemoTraceSpec.field:saga() -> DemoFunctionalitySagas')
        report.contains('arg[0]: null')
        report.indexOf('Sagas (1)') < report.indexOf('Groovy constructor-input traces (1)')

        and:
        def htmlReportPath = applicationsRoot.resolve(applicationBaseDir).resolve('analysis-report.html')
        Files.exists(htmlReportPath)
        def htmlReport = Files.readString(htmlReportPath)
        htmlReport.contains('Verifier Analysis Report')
        htmlReport.contains('Groovy Trace Explorer')
        htmlReport.contains('DemoTraceSpec')
        htmlReport.contains('Raw Text Report (verbatim)')

        cleanup:
        System.setOut(originalOut)
        System.setErr(originalErr)
        context?.close()
    }

    private Path writeSource(Path applicationsRoot, String applicationBaseDir, String relativePath, String contents) {
        def file = applicationsRoot.resolve(applicationBaseDir).resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, contents.stripIndent().trim() + '\n')
        return file
    }
}
