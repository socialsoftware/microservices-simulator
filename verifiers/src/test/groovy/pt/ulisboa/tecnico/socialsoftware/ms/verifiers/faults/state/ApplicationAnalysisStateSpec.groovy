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
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.ServiceBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicity
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicityKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase
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

    def "source-supported saga pairs require the same producer occurrence and property"() {
        given:
        def state = new ApplicationAnalysisState()
        ['a.Saga', 'b.Saga'].each { sagaFqn ->
            def saga = new SagaFunctionalityBuildingBlock(null, 'demo', sagaFqn)
            def step = new SagaStepBuildingBlock(null, 'demo', "${sagaFqn}::step", 'step')
            step.addDispatch(new StepDispatchFootprint(
                    "${sagaFqn}::step", 'demo.Command', 'Tournament', AccessPolicy.WRITE,
                    DispatchPhase.FORWARD,
                    new DispatchMultiplicity(DispatchMultiplicityKind.SINGLE, 1),
                    null, null, 1))
            saga.addStep(step)
            state.sagas.add(saga)
        }

        def shared = new GroovySourceValueReference('demo.Spec:10:5:createTournament',
                'createTournament', ['aggregateId'])
        def differentOccurrence = new GroovySourceValueReference('demo.Spec:11:5:createTournament',
                'createTournament', ['aggregateId'])
        def differentProperty = new GroovySourceValueReference('demo.Spec:10:5:createTournament',
                'createTournament', ['courseAggregateId'])
        state.groovyFullTraceResults.add(trace('a.Saga', 'left', shared))
        state.groovyFullTraceResults.add(trace('b.Saga', 'different-call', differentOccurrence))
        state.groovyFullTraceResults.add(trace('b.Saga', 'different-property', differentProperty))
        state.groovyFullTraceResults.add(trace('b.Saga', 'exact', shared))

        when:
        def pairs = state.sourceSupportedSagaPairs()

        then:
        pairs.size() == 1
        pairs.first().left().sourceBindingName() == 'left'
        pairs.first().right().sourceBindingName() == 'exact'
        pairs.first().left().producerReference() == pairs.first().right().producerReference()
    }

    private static GroovyFullTraceResult trace(String sagaFqn,
                                               String binding,
                                               GroovySourceValueReference reference) {
        new GroovyFullTraceResult(
                'demo.Spec', 'feature', binding, GroovyTraceOriginKind.DIRECT_CONSTRUCTOR,
                binding, sagaFqn,
                [new GroovyTraceArgument(0, 'runtime', null),
                 new GroovyTraceArgument(1, 'producer.aggregateId', null, reference)],
                [], [], binding)
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
