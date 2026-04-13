package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandDispatchInfo
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandHandlerBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicity
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicityKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.ServiceBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowCreationArgumentSource
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowCreationArgumentSourceKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowFunctionalityCreationSite
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceOriginKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyWorkflowCall
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class ApplicationAnalysisStateSpec extends Specification {

    @TempDir
    Path tempDir

    def "getCommandDispatchInfo resolves same simple name commands by fully qualified type"() {
        given:
        writeSource('a/one/GetUserCommand.java', '''
            package a.one;
            public class GetUserCommand {}
        ''')
        writeSource('b/two/GetUserCommand.java', '''
            package b.two;
            public class GetUserCommand {}
        ''')
        writeSource('demo/UseA.java', '''
            package demo;
            import a.one.GetUserCommand;
            class UseA { GetUserCommand command; }
        ''')
        writeSource('demo/UseB.java', '''
            package demo;
            import b.two.GetUserCommand;
            class UseB { GetUserCommand command; }
        ''')
        configureParser(tempDir)

        def state = new ApplicationAnalysisState()

        def serviceA = new ServiceBuildingBlock(null, 'a.one', 'a.one.UserService')
        serviceA.addMethod('handle(a.one.GetUserCommand)', AccessPolicy.READ)
        def serviceB = new ServiceBuildingBlock(null, 'b.two', 'b.two.UserService')
        serviceB.addMethod('handle(b.two.GetUserCommand)', AccessPolicy.WRITE)

        def handlerA = new CommandHandlerBuildingBlock(null, 'a.one', 'a.one.UserHandler', 'User')
        handlerA.addCommandDispatch('a.one.GetUserCommand', new CommandDispatchInfo(serviceA, 'handle(a.one.GetUserCommand)', 'User'))
        def handlerB = new CommandHandlerBuildingBlock(null, 'b.two', 'b.two.UserHandler', 'User')
        handlerB.addCommandDispatch('b.two.GetUserCommand', new CommandDispatchInfo(serviceB, 'handle(b.two.GetUserCommand)', 'User'))

        state.commandHandlers.add(handlerB)
        state.commandHandlers.add(handlerA)

        and:
        def typeA = parseFieldType(tempDir.resolve('demo/UseA.java'))
        def typeB = parseFieldType(tempDir.resolve('demo/UseB.java'))

        expect:
        state.getCommandDispatchInfo(typeA).present
        state.getCommandDispatchInfo(typeA).get().serviceClassName() == 'a.one.UserService'
        state.getCommandDispatchInfo(typeA).get().accessPolicy() == AccessPolicy.READ
        state.getCommandDispatchInfo(typeB).present
        state.getCommandDispatchInfo(typeB).get().serviceClassName() == 'b.two.UserService'
        state.getCommandDispatchInfo(typeB).get().accessPolicy() == AccessPolicy.WRITE
    }

    def "findSagaByFqn resolves saga entries by fully qualified name"() {
        given:
        def state = new ApplicationAnalysisState()
        def saga = new SagaFunctionalityBuildingBlock(null, 'com.example.app.order.coordination', 'com.example.app.order.coordination.CreateOrderFunctionalitySagas')
        state.sagas.add(saga)

        expect:
        state.findSagaByFqn('com.example.app.order.coordination.CreateOrderFunctionalitySagas').present
        state.findSagaByFqn('com.example.app.order.coordination.CreateOrderFunctionalitySagas').get() == saga
        state.hasSagaFqn('com.example.app.order.coordination.CreateOrderFunctionalitySagas')
        !state.hasSagaFqn('com.example.app.order.coordination.MissingSaga')
    }

    def "formatHumanReadableReport renders the collected analysis state"() {
        given:
        def state = new ApplicationAnalysisState()
        state.dispatchTargetFqns.add('com.example.app.order.service.OrderService')

        def service = new ServiceBuildingBlock(null, 'com.example.app.order.service', 'com.example.app.order.service.OrderService')
        service.addMethod('getOrder(com.example.app.order.commands.GetOrderCommand)', AccessPolicy.READ)
        service.addMethod('placeOrder(com.example.app.order.commands.PlaceOrderCommand)', AccessPolicy.WRITE)
        state.services.add(service)

        def handler = new CommandHandlerBuildingBlock(null, 'com.example.app.order.commandHandler', 'com.example.app.order.commandHandler.OrderCommandHandler', 'Order')
        handler.addCommandDispatch(
                'com.example.app.order.commands.GetOrderCommand',
                new CommandDispatchInfo(service, 'getOrder(com.example.app.order.commands.GetOrderCommand)', 'Order')
        )
        state.commandHandlers.add(handler)

        def saga = new SagaFunctionalityBuildingBlock(null, 'com.example.app.order.coordination', 'com.example.app.order.coordination.CreateOrderFunctionalitySagas')
        def step = new SagaStepBuildingBlock(null, 'com.example.app.order.coordination', 'CreateOrderFunctionalitySagas::placeOrderStep', 'placeOrderStep')
        step.addDispatch(new StepDispatchFootprint(
                'CreateOrderFunctionalitySagas::placeOrderStep',
                'com.example.app.order.commands.PlaceOrderCommand',
                'Order',
                AccessPolicy.WRITE,
                DispatchPhase.FORWARD,
                new DispatchMultiplicity(DispatchMultiplicityKind.SINGLE, 1)
        ))
        saga.addStep(step)
        state.sagas.add(saga)
        state.sagaCreationSites.add(new WorkflowFunctionalityCreationSite(
                'com.example.app.order.coordination.OrderFunctionalitiesFacade',
                'createOrder',
                'com.example.app.order.coordination.CreateOrderFunctionalitySagas',
                [
                        new WorkflowCreationArgumentSource(0, WorkflowCreationArgumentSourceKind.FIELD_REFERENCE, null,
                                'sagaUnitOfWorkService', null),
                        new WorkflowCreationArgumentSource(1, WorkflowCreationArgumentSourceKind.LOCAL_VARIABLE, null,
                                'unitOfWork', 'sagaUnitOfWorkService.createUnitOfWork("createOrder")'),
                        new WorkflowCreationArgumentSource(2, WorkflowCreationArgumentSourceKind.METHOD_PARAMETER, 0,
                                'customerId', null),
                        new WorkflowCreationArgumentSource(3, WorkflowCreationArgumentSourceKind.INLINE_EXPRESSION, null,
                                null, 'customerId + 1')
                ]
        ))

        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                'com.example.app.order.CreateOrderSpec',
                'setup',
                'setupSaga',
                'com.example.app.order.coordination.CreateOrderFunctionalitySagas'
        ))
        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                'com.example.app.order.CreateOrderSpec',
                'setup',
                'setupSaga',
                GroovyTraceOriginKind.DIRECT_CONSTRUCTOR,
                'new CreateOrderFunctionalitySagas(null, null)',
                'com.example.app.order.coordination.CreateOrderFunctionalitySagas',
                [new GroovyTraceArgument(0, 'null', new GroovyValueRecipe(GroovyValueKind.LITERAL, 'null', []))],
                [new GroovyWorkflowCall('setupSaga.executeWorkflow(...)', 'when')],
                [],
                'setup -> new CreateOrderFunctionalitySagas(...)'
        ))

        when:
        def report = state.formatHumanReadableReport()

        then:
        report.contains('Analysis Summary')
        report.contains('Dispatch targets (1)')
        report.contains('com.example.app.order.service.OrderService')
        report.contains('Services (1)')
        report.contains('placeOrder(com.example.app.order.commands.PlaceOrderCommand) [WRITE]')
        report.contains('Command handlers (1)')
        report.contains('GetOrderCommand -> OrderCommandHandler')
        report.contains('Sagas (1)')
        report.contains('placeOrderStep')
        report.contains('PlaceOrderCommand -> Order [WRITE, FORWARD, SINGLE x1]')
        report.contains('Saga creation sites (1)')
        report.contains('OrderFunctionalitiesFacade.createOrder() -> CreateOrderFunctionalitySagas')
        report.contains('arg[2]: parameter #0 customerId')
        report.contains('Groovy constructor-input traces (1)')
        report.contains('CreateOrderSpec.setup() [binding=setupSaga] -> CreateOrderFunctionalitySagas')
        report.contains('Groovy full traces (1)')
        report.contains('setup -> new CreateOrderFunctionalitySagas(...)')
    }

    private static void configureParser(Path sourceRoot) {
        def solver = new CombinedTypeSolver(
                new ReflectionTypeSolver(false),
                new JavaParserTypeSolver(sourceRoot.toFile())
        )
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setSymbolResolver(new JavaSymbolSolver(solver))
    }

    private Path writeSource(String relativePath, String contents) {
        def file = tempDir.resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, contents.stripIndent().trim() + '\n')
        return file
    }

    private static com.github.javaparser.ast.type.Type parseFieldType(Path file) {
        StaticJavaParser.parse(file.toFile())
                .findFirst(FieldDeclaration)
                .orElseThrow()
                .commonType
    }
}
