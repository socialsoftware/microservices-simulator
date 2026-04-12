package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex

class GroovyConstructorInputTraceVisitorDummyappSpec extends VisitorTestSupport {

    private ApplicationAnalysisState state

    def setup() {
        configureParser()
        state = new ApplicationAnalysisState()

        def indexVisitor = new CommandHandlerIndexVisitor()
        def serviceVisitor = new ServiceVisitor()
        def commandHandlerVisitor = new CommandHandlerVisitor()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        def cus = parseAllDummyappFiles()
        cus.each { cu -> indexVisitor.visit(cu, state) }
        cus.each { cu -> serviceVisitor.visit(cu, state) }
        cus.each { cu -> commandHandlerVisitor.visit(cu, state) }
        cus.each { cu -> workflowVisitor.visit(cu, state) }

        def sourceIndex = new GroovySourceIndex()
        sourceIndex.parse(resolveProjectPath('applications', 'dummyapp', 'src', 'test', 'groovy'))

        new GroovyConstructorInputTraceVisitor().visit(sourceIndex, state)
    }

    def 'captures separate constructor traces for same method by variable binding'() {
        given:
        def constructorTraces = state.groovyConstructorInputTraces.findAll {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'same method tracks two order saga instances' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }

        expect:
        constructorTraces.size() == 2
        constructorTraces*.sourceBindingName as Set == ['firstOrderSaga', 'secondOrderSaga'] as Set

        and:
        def fullTraces = state.groovyFullTraceResults.findAll {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'same method tracks two order saga instances' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }
        fullTraces.size() == 2
        fullTraces*.sourceBindingName as Set == ['firstOrderSaga', 'secondOrderSaga'] as Set
    }

    def 'traces setup, setupSpec, and field constructor contexts from dummyapp fixture'() {
        given:
        def traceMethods = state.groovyConstructorInputTraces.findAll {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }*.sourceMethodName as Set

        expect:
        traceMethods.contains('field:orderSagaInField')
        traceMethods.contains('setup')
        traceMethods.contains('setupSpec')
    }

    def 'captures helper chain and accessor provenance for dummyapp item saga fixture'() {
        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'helper chain and accessor provenance feed item saga constructor',
                'helperSaga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        then:
        traceText.contains('resolved via helper buildItemSagaFromBundle(...)')
        traceText.contains('resolved via helper createItemSaga(...)')
        traceText.contains('arg[1]:')
        traceText.contains('buildItemBundle(...)')
        traceText.contains('helperSaga.executeWorkflow(...)')
    }

    def 'captures named-args and setter-based dto constructor provenance from dummyapp fixture'() {
        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'named args, setters, and toSet provenance feed item saga constructor',
                'saga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        then:
        traceText.contains('arg[1]: setterDto <- new ItemDto()')
        traceText.contains('saga.resumeWorkflow(...)')
    }

    def 'keeps unresolved runtime edges conservative for dummyapp item saga input'() {
        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'runtime edge stays conservative for item saga input',
                'runtimeSaga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        then:
        traceText.contains('arg[1]: runtimeDto <- runtimeGateway.loadExternalDto() [unresolved external/runtime edge]')
    }

    def 'traces inherited setup and shadowed field contexts from dummyapp fixture classes'() {
        given:
        def childMethods = state.groovyConstructorInputTraces.findAll {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingChildSpec'
        }*.sourceMethodName as Set
        def shadowMethods = state.groovyConstructorInputTraces.findAll {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingShadowChildSpec'
        }*.sourceMethodName as Set

        expect:
        childMethods == ['field:inheritedSagaInField', 'setup', 'setupSpec', 'child uses inherited helper saga'] as Set
        shadowMethods.contains('field:GroovySagaTracingBaseSpec#inheritedSagaInField')
        shadowMethods.contains('field:GroovySagaTracingShadowChildSpec#inheritedSagaInField')
        shadowMethods.contains('setup')
        shadowMethods.contains('setupSpec')
    }

    def 'captures workflow calls inside try-catch and retry loop blocks'() {
        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'try catch and retry loop include workflow calls',
                'retrySaga',
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )

        then:
        traceText.contains('retrySaga.resumeWorkflow(...)')
        traceText.contains('retrySaga.executeWorkflow(...)')
    }

    def 'captures workflow calls inside for and do-while blocks'() {
        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'for and do-while blocks include workflow calls',
                'loopSaga',
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )

        then:
        traceText.contains('loopSaga.executeUntilStep(...)')
        traceText.contains('loopSaga.resumeWorkflow(...)')
    }

    def 'captures workflow calls inside if switch and finally blocks'() {
        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'if switch and finally blocks include workflow calls',
                'branchSaga',
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )

        then:
        traceText.contains('branchSaga.executeUntilStep(...)')
        traceText.contains('branchSaga.resumeWorkflow(...)')
        traceText.contains('branchSaga.executeWorkflow(...)')
    }

    def 'emits structured trace payload for replay-oriented consumers'() {
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'same method tracks two order saga instances' &&
                    it.sourceBindingName == 'firstOrderSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }

        expect:
        trace != null
        trace.constructorArguments*.index == [0, 1]
        trace.constructorArguments*.provenance == ['null', 'null']
        trace.workflowCalls*.callText.contains('firstOrderSaga.executeWorkflow(...)')
    }

    private String traceTextFor(String sourceClassFqn,
                                String sourceMethodName,
                                String sourceBindingName,
                                String sagaClassFqn) {
        return state.groovyFullTraceResults.find {
            it.sourceClassFqn == sourceClassFqn &&
                    it.sourceMethodName == sourceMethodName &&
                    it.sourceBindingName == sourceBindingName &&
                    it.sagaClassFqn == sagaClassFqn
        }?.traceText() ?: ''
    }
}
