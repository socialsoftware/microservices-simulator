package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class GroovyConstructorInputTraceVisitorSpec extends VisitorTestSupport {

    private static final String SAGA_FQN = 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
    private static final String NON_SAGA_FQN = 'com.example.dummyapp.DummyAggregate'

    @TempDir
    Path tempDir

    private ApplicationAnalysisState state

    def setup() {
        configureParser()
        state = new ApplicationAnalysisState()

        def workflowVisitor = new WorkflowFunctionalityVisitor()
        parseAllDummyappFiles().each { cu -> workflowVisitor.visit(cu, state) }

        writeSource('demo/GroovyTraceSpec.groovy', '''
            package demo

            import com.example.dummyapp.DummyAggregate
            import com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas
            import spock.lang.Specification

            class RuntimeGateway {
                def loadExternalDto() {
                    externalRuntime.fetchDto()
                }
            }

            class LocalUserDto {
                def aggregateId

                LocalUserDto(aggregateId) {
                    this.aggregateId = aggregateId
                }

                def getAggregateId() {
                    aggregateId
                }
            }

            class LocalDtoWrapper {
                def dto

                LocalDtoWrapper(dto) {
                    this.dto = dto
                }

                def getDto() {
                    dto
                }
            }

            class LocalInputBundle {
                def userDto

                LocalInputBundle(userDto) {
                    this.userDto = userDto
                }

                def getUserDto() {
                    userDto
                }
            }

            class LocalPayload {
                def value

                LocalPayload(value) {
                    this.value = value
                }
            }

            class GroovyTraceSpec extends Specification {
                def sagaInField = new CreateOrderFunctionalitySagas(null, null)
                def plainAggregateInField = new DummyAggregate(999, 'plain')
                def runtimeGateway = new RuntimeGateway()

                def sagaInSetup
                static sagaInSetupSpec

                def setup() {
                    def setupAlias = new CreateOrderFunctionalitySagas(null, null)
                    sagaInSetup = setupAlias
                    setupAlias.executeWorkflow(null)
                }

                def setupSpec() {
                    def setupSpecAlias = new CreateOrderFunctionalitySagas(null, null)
                    sagaInSetupSpec = setupSpecAlias
                    setupSpecAlias.resumeWorkflow(null)
                }

                def 'labels are context only'() {
                    given:
                    def labeledSaga = new CreateOrderFunctionalitySagas(null, null)
                    def labeledAlias = labeledSaga

                    when:
                    labeledAlias.executeUntilStep('step', null)

                    then:
                    true
                }

                def 'helper-method chain resolves local returns'() {
                    given:
                    def helperSaga = buildSagaFromHelpers(buildInputBundle())

                    when:
                    helperSaga.executeWorkflow(null)

                    then:
                    true
                }

                def 'nested constructor arguments are traced'() {
                    given:
                    def nestedSaga = new CreateOrderFunctionalitySagas(new LocalPayload(new LocalPayload('nested-seed')), null)

                    when:
                    nestedSaga.executeUntilStep('step', null)

                    then:
                    true
                }

                def 'accessor chain arguments are traced'() {
                    given:
                    def accessorSaga = new CreateOrderFunctionalitySagas(wrapDto(buildUserDto()).dto.aggregateId, null)

                    when:
                    accessorSaga.resumeWorkflow(null)

                    then:
                    true
                }

                def 'runtime-derived unresolved edge remains conservative'() {
                    given:
                    def runtimeSaga = buildSagaFromRuntime()
                    def unresolvedFromRuntime = runtimeGateway.loadExternalDto()

                    when:
                    runtimeSaga.executeWorkflow(null)
                    unresolvedFromRuntime.executeWorkflow(null)

                    then:
                    true
                }

                def buildSagaFromHelpers(inputBundle) {
                    createSaga(inputBundle.userDto.getAggregateId())
                }

                def createSaga(aggregateId) {
                    new CreateOrderFunctionalitySagas(aggregateId, null)
                }

                def buildInputBundle() {
                    new LocalInputBundle(buildUserDto())
                }

                def buildUserDto() {
                    new LocalUserDto(new LocalPayload(777))
                }

                def wrapDto(userDto) {
                    new LocalDtoWrapper(userDto)
                }

                def buildSagaFromRuntime() {
                    def runtimeDto = runtimeGateway.loadExternalDto()
                    new CreateOrderFunctionalitySagas(runtimeDto.getAggregateId(), null)
                }

                def helperMethod() {
                    def helperSaga = new CreateOrderFunctionalitySagas(null, null)
                    helperSaga.executeWorkflow(null)
                }
            }
        ''')

        def sourceIndex = new GroovySourceIndex()
        sourceIndex.parse(tempDir)

        new GroovyConstructorInputTraceVisitor().visit(sourceIndex, state)
    }

    def 'traces direct saga constructor inputs only for discovered sagas'() {
        expect:
        state.hasSagaFqn(SAGA_FQN)
        state.groovyConstructorInputTraces*.sagaClassFqn as Set == [SAGA_FQN] as Set
        state.groovyConstructorInputTraces*.sourceMethodName as Set == [
            'field:sagaInField',
            'setup',
            'setupSpec',
            'labels are context only',
            'helper-method chain resolves local returns',
            'nested constructor arguments are traced',
            'accessor chain arguments are traced',
            'runtime-derived unresolved edge remains conservative'
        ] as Set
        !state.groovyConstructorInputTraces.any { it.sagaClassFqn == NON_SAGA_FQN }
        !state.groovyConstructorInputTraces.any { it.sourceMethodName == 'helperMethod' }
    }

    def 'records helper chain, nested constructor, and accessor traces conservatively'() {
        expect:
        traceTextFor('setup').contains('setupAlias.executeWorkflow')
        traceTextFor('setupSpec').contains('setupSpecAlias.resumeWorkflow')

        traceArgLineFor('helper-method chain resolves local returns', 0) ==
            'arg[0]: aggregateId <- inputBundle <- buildInputBundle(...) <- new LocalInputBundle(buildUserDto(...) <- new LocalUserDto(new LocalPayload(777))).userDto.aggregateId'
        traceArgLineFor('nested constructor arguments are traced', 0) ==
            'arg[0]: new LocalPayload(new LocalPayload(nested-seed))'
        traceArgLineFor('accessor chain arguments are traced', 0) ==
            'arg[0]: wrapDto(...) <- new LocalDtoWrapper(userDto <- buildUserDto(...) <- new LocalUserDto(new LocalPayload(777))).dto.aggregateId'
        traceArgLineFor('runtime-derived unresolved edge remains conservative', 0) ==
            'arg[0]: runtimeDto <- runtimeGateway.loadExternalDto() [unresolved external/runtime edge].aggregateId'

        traceTextFor('helper-method chain resolves local returns').contains('resolved via helper buildSagaFromHelpers(...)')
        traceTextFor('helper-method chain resolves local returns').contains('resolved via helper createSaga(...)')
        !state.groovyFullTraceResults.any { it.sourceMethodName == 'helperMethod' }
    }

    def 'traces source-backed inherited setup/setupSpec/field/helper members'() {
        given:
        def inheritedState = new ApplicationAnalysisState()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        parseAllDummyappFiles().each { cu -> workflowVisitor.visit(cu, inheritedState) }

        writeSource('demo/BaseTraceSpec.groovy', '''
            package demo

            import com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas
            import spock.lang.Specification

            class BaseTraceSpec extends Specification {
                def inheritedSagaInField = new CreateOrderFunctionalitySagas(null, null)

                def setup() {
                    def setupSaga = inheritedHelper()
                    setupSaga.executeWorkflow(null)
                }

                def setupSpec() {
                    def setupSpecSaga = inheritedHelper()
                    setupSpecSaga.resumeWorkflow(null)
                }

                def inheritedHelper() {
                    new CreateOrderFunctionalitySagas(null, null)
                }
            }
        ''')

        writeSource('demo/ChildTraceSpec.groovy', '''
            package demo

            class ChildTraceSpec extends BaseTraceSpec {
                def 'child uses inherited helper'() {
                    given:
                    def inheritedSaga = inheritedHelper()

                    when:
                    inheritedSaga.executeWorkflow(null)

                    then:
                    true
                }
            }
        ''')

        def inheritedSourceIndex = new GroovySourceIndex()
        inheritedSourceIndex.parse(tempDir)

        when:
        new GroovyConstructorInputTraceVisitor().visit(inheritedSourceIndex, inheritedState)

        then:
        def childConstructorTraces = inheritedState.groovyConstructorInputTraces.findAll { it.sourceClassFqn == 'demo.ChildTraceSpec' }
        childConstructorTraces*.sourceMethodName as Set == [
            'field:inheritedSagaInField',
            'setup',
            'setupSpec',
            'child uses inherited helper'
        ] as Set
        childConstructorTraces.size() == 4

        traceTextFor(inheritedState, 'demo.ChildTraceSpec', 'setup').contains('resolved via helper inheritedHelper(...)')
        traceTextFor(inheritedState, 'demo.ChildTraceSpec', 'setupSpec').contains('resolved via helper inheritedHelper(...)')
        traceTextFor(inheritedState, 'demo.ChildTraceSpec', 'child uses inherited helper').contains('resolved via helper inheritedHelper(...)')
    }

    def 'keeps external or unknown superclass traversal conservative'() {
        given:
        def inheritedState = new ApplicationAnalysisState()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        parseAllDummyappFiles().each { cu -> workflowVisitor.visit(cu, inheritedState) }

        writeSource('demo/UnknownParentChildSpec.groovy', '''
            package demo

            import java.util.ArrayList

            class UnknownParentChildSpec extends ArrayList {
                def 'unknown parent class remains out of scope'() {
                    given:
                    def saga = inheritedHelper()

                    when:
                    saga.executeWorkflow(null)

                    then:
                    true
                }
            }
        ''')

        def inheritedSourceIndex = new GroovySourceIndex()
        inheritedSourceIndex.parse(tempDir)

        when:
        new GroovyConstructorInputTraceVisitor().visit(inheritedSourceIndex, inheritedState)

        then:
        !inheritedState.groovyConstructorInputTraces.any { it.sourceClassFqn == 'demo.UnknownParentChildSpec' }
        !inheritedState.groovyFullTraceResults.any { it.sourceClassFqn == 'demo.UnknownParentChildSpec' }
    }

    def 'keeps shadowed inherited fields layer-aware without duplicate field contexts'() {
        given:
        def inheritedState = new ApplicationAnalysisState()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        parseAllDummyappFiles().each { cu -> workflowVisitor.visit(cu, inheritedState) }

        writeSource('demo/BaseShadowSpec.groovy', '''
            package demo

            import com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas
            import spock.lang.Specification

            class BaseShadowSpec extends Specification {
                def sharedSaga = new CreateOrderFunctionalitySagas(null, null)

                def setup() {
                    sharedSaga.executeWorkflow(null)
                }
            }
        ''')

        writeSource('demo/ChildShadowSpec.groovy', '''
            package demo

            import com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas

            class ChildShadowSpec extends BaseShadowSpec {
                def sharedSaga = new CreateOrderFunctionalitySagas(null, null)

                def 'child executes shadowed field'() {
                    when:
                    sharedSaga.resumeWorkflow(null)

                    then:
                    true
                }
            }
        ''')

        def inheritedSourceIndex = new GroovySourceIndex()
        inheritedSourceIndex.parse(tempDir)

        when:
        new GroovyConstructorInputTraceVisitor().visit(inheritedSourceIndex, inheritedState)

        then:
        def childConstructorTraces = inheritedState.groovyConstructorInputTraces.findAll { it.sourceClassFqn == 'demo.ChildShadowSpec' }
        childConstructorTraces*.sourceMethodName as Set == [
            'field:BaseShadowSpec#sharedSaga',
            'field:ChildShadowSpec#sharedSaga'
        ] as Set
        childConstructorTraces.size() == 2

        traceTextFor(inheritedState, 'demo.ChildShadowSpec', 'field:BaseShadowSpec#sharedSaga').contains('sharedSaga.executeWorkflow(...)')
        traceTextFor(inheritedState, 'demo.ChildShadowSpec', 'field:ChildShadowSpec#sharedSaga').contains('sharedSaga.resumeWorkflow(...)')
    }

    def 'keeps child-vs-parent same-name helper resolution conservative at inheritance boundaries'() {
        given:
        def inheritedState = new ApplicationAnalysisState()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        parseAllDummyappFiles().each { cu -> workflowVisitor.visit(cu, inheritedState) }

        writeSource('demo/BaseHelperBoundarySpec.groovy', '''
            package demo

            import com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas
            import spock.lang.Specification

            class BaseHelperBoundarySpec extends Specification {
                def createSaga(value) {
                    new CreateOrderFunctionalitySagas(value, null)
                }

                def ambiguousSaga(value) {
                    new CreateOrderFunctionalitySagas(value, null)
                }
            }
        ''')

        writeSource('demo/ChildHelperBoundarySpec.groovy', '''
            package demo

            import com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas

            class LocalPayload {
                def value

                LocalPayload(value) {
                    this.value = value
                }
            }

            class ChildHelperBoundarySpec extends BaseHelperBoundarySpec {
                def createSaga(value) {
                    new CreateOrderFunctionalitySagas(new LocalPayload(value), null)
                }

                def ambiguousSaga(Integer value) {
                    new CreateOrderFunctionalitySagas(value, null)
                }

                def 'child override helper is preferred'() {
                    given:
                    def saga = createSaga('seed')

                    when:
                    saga.executeWorkflow(null)

                    then:
                    true
                }

                def 'ambiguous inherited helper remains conservative'() {
                    given:
                    def payload = 'seed'
                    def saga = ambiguousSaga(payload)

                    when:
                    saga.executeWorkflow(null)

                    then:
                    true
                }
            }
        ''')

        def inheritedSourceIndex = new GroovySourceIndex()
        inheritedSourceIndex.parse(tempDir)

        when:
        new GroovyConstructorInputTraceVisitor().visit(inheritedSourceIndex, inheritedState)

        then:
        def childTraces = inheritedState.groovyConstructorInputTraces.findAll { it.sourceClassFqn == 'demo.ChildHelperBoundarySpec' }
        childTraces*.sourceMethodName as Set == ['child override helper is preferred'] as Set

        traceTextFor(inheritedState, 'demo.ChildHelperBoundarySpec', 'child override helper is preferred')
            .contains('resolved via helper createSaga(...)')
        traceArgLineFor(inheritedState, 'demo.ChildHelperBoundarySpec', 'child override helper is preferred', 0)
            .contains('new LocalPayload(')

        !inheritedState.groovyConstructorInputTraces.any {
            it.sourceClassFqn == 'demo.ChildHelperBoundarySpec' && it.sourceMethodName == 'ambiguous inherited helper remains conservative'
        }
    }

    def 'uses layer-aware field visibility for helpers inside constructor argument provenance'() {
        given:
        def inheritedState = new ApplicationAnalysisState()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        parseAllDummyappFiles().each { cu -> workflowVisitor.visit(cu, inheritedState) }

        writeSource('demo/BaseHelperArgShadowSpec.groovy', '''
            package demo

            import spock.lang.Specification

            class BaseHelperArgShadowSpec extends Specification {
                def shadowedSeed = 'base-seed'

                def inheritedArgSeed() {
                    shadowedSeed
                }
            }
        ''')

        writeSource('demo/ChildHelperArgShadowSpec.groovy', '''
            package demo

            import com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas

            class ChildHelperArgShadowSpec extends BaseHelperArgShadowSpec {
                def shadowedSeed = 'child-seed'

                def 'helper argument uses base-layer shadowed field'() {
                    given:
                    def saga = new CreateOrderFunctionalitySagas(inheritedArgSeed(), null)

                    when:
                    saga.executeWorkflow(null)

                    then:
                    true
                }
            }
        ''')

        def inheritedSourceIndex = new GroovySourceIndex()
        inheritedSourceIndex.parse(tempDir)

        when:
        new GroovyConstructorInputTraceVisitor().visit(inheritedSourceIndex, inheritedState)

        then:
        def argLine = traceArgLineFor(inheritedState,
            'demo.ChildHelperArgShadowSpec',
            'helper argument uses base-layer shadowed field',
            0)

        argLine.contains('inheritedArgSeed(...) <- shadowedSeed <- base-seed')
        !argLine.contains('child-seed')
    }

    def 'surfaces label context in trace text and report output'() {
        expect:
        traceTextFor('labels are context only').contains('given:')
        traceTextFor('labels are context only').contains('when:')

        when:
        def report = state.formatHumanReadableReport()

        then:
        report.contains('given:')
        report.contains('when:')
    }

    def 'renders the groovy trace report surface'() {
        when:
        def report = state.formatHumanReadableReport()

        then:
        report.contains('Groovy constructor-input traces (8)')
        report.contains('Groovy full traces (8)')
        report.contains('GroovyTraceSpec.field:sagaInField() -> CreateOrderFunctionalitySagas')
        report.contains('GroovyTraceSpec.setup() -> CreateOrderFunctionalitySagas')
        report.contains('GroovyTraceSpec.setupSpec() -> CreateOrderFunctionalitySagas')
        report.contains('GroovyTraceSpec.labels are context only() -> CreateOrderFunctionalitySagas')
        report.contains('GroovyTraceSpec.helper-method chain resolves local returns() -> CreateOrderFunctionalitySagas')
        report.contains('GroovyTraceSpec.nested constructor arguments are traced() -> CreateOrderFunctionalitySagas')
        report.contains('GroovyTraceSpec.accessor chain arguments are traced() -> CreateOrderFunctionalitySagas')
        report.contains('GroovyTraceSpec.runtime-derived unresolved edge remains conservative() -> CreateOrderFunctionalitySagas')
    }

    private String traceTextFor(String sourceMethodName) {
        return state.groovyFullTraceResults.find { it.sourceMethodName == sourceMethodName }?.traceText() ?: ''
    }

    private static String traceTextFor(ApplicationAnalysisState analysisState, String sourceClassFqn, String sourceMethodName) {
        return analysisState.groovyFullTraceResults.find {
            it.sourceClassFqn == sourceClassFqn && it.sourceMethodName == sourceMethodName
        }?.traceText() ?: ''
    }

    private static String traceArgLineFor(ApplicationAnalysisState analysisState,
                                          String sourceClassFqn,
                                          String sourceMethodName,
                                          int argIndex) {
        return traceTextFor(analysisState, sourceClassFqn, sourceMethodName)
            .readLines()
            .find { it.startsWith("arg[${argIndex}]:") } ?: ''
    }

    private String traceArgLineFor(String sourceMethodName, int argIndex) {
        return traceTextFor(sourceMethodName)
            .readLines()
            .find { it.startsWith("arg[${argIndex}]:") } ?: ''
    }

    private Path writeSource(String relativePath, String contents) {
        def file = tempDir.resolve(relativePath)
        Files.createDirectories(file.parent)
        Files.writeString(file, contents.stripIndent().trim() + '\n')
        return file
    }
}
