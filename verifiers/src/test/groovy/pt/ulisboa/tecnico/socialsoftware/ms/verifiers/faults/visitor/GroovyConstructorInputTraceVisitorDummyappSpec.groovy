package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceOriginKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe

class GroovyConstructorInputTraceVisitorDummyappSpec extends VisitorTestSupport {

    private ApplicationAnalysisState state

    def setup() {
        configureParser()
        state = new ApplicationAnalysisState()

        def indexVisitor = new CommandHandlerIndexVisitor()
        def serviceVisitor = new ServiceVisitor()
        def commandHandlerVisitor = new CommandHandlerVisitor()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        def creationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor()
        def cus = parseAllDummyappFiles()
        cus.each { cu -> indexVisitor.visit(cu, state) }
        cus.each { cu -> serviceVisitor.visit(cu, state) }
        cus.each { cu -> commandHandlerVisitor.visit(cu, state) }
        cus.each { cu -> workflowVisitor.visit(cu, state) }
        cus.each { cu -> creationSiteVisitor.visit(cu, state) }

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
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'helper chain and accessor provenance feed item saga constructor' &&
                    it.sourceBindingName == 'helperSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }

        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'helper chain and accessor provenance feed item saga constructor',
                'helperSaga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        then:
        trace != null
        trace.originKind() == GroovyTraceOriginKind.DIRECT_CONSTRUCTOR
        recipeContainsKind(trace.constructorArguments()[1].recipe(), GroovyValueKind.HELPER_CALL_RESULT)
        recipeContainsKind(trace.constructorArguments()[1].recipe(), GroovyValueKind.PROPERTY_ACCESS)
        !trace.constructorArguments()[1].provenance().contains('[unresolved cyclic reference]')
        traceText.contains('resolved via helper buildItemSagaFromBundle(...)')
        traceText.contains('resolved via helper createItemSaga(...)')
        traceText.contains('arg[1]:')
        traceText.contains('buildItemBundle(...)')
        traceText.contains('helperSaga.executeWorkflow(...)')
    }

    def 'captures item facade assignment, bare call, and helper-return traces from dummyapp fixture'() {
        given:
        def assignmentTrace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'assignment from facade call traces item saga recipe' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }
        def bareTrace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'bare facade call traces item saga recipe' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }
        def helperTrace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'helper returning facade result feeds item saga constructor' &&
                    it.sourceBindingName == 'helperSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }

        when:
        def assignmentTraceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'assignment from facade call traces item saga recipe',
                null,
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )
        def bareTraceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'bare facade call traces item saga recipe',
                null,
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )
        def helperTraceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'helper returning facade result feeds item saga constructor',
                'helperSaga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        then:
        assignmentTrace != null
        assignmentTrace.originKind() == GroovyTraceOriginKind.FACADE_CALL
        assignmentTrace.sourceBindingName() == null
        assignmentTrace.sourceExpressionText() == 'itemFunctionalities.createItem(dto)'
        assignmentTrace.workflowCalls().isEmpty()
        assignmentTrace.constructorArguments()[1].provenance().contains('dto <- new ItemDto(')
        recipeContainsKind(assignmentTrace.constructorArguments()[1].recipe(), GroovyValueKind.CONSTRUCTOR)
        assignmentTraceText.contains('resolved via facade ItemFunctionalitiesFacade.createItem(...)')
        assignmentTraceText.contains('arg[1]: dto <- new ItemDto(')

        and:
        bareTrace != null
        bareTrace.originKind() == GroovyTraceOriginKind.FACADE_CALL
        bareTrace.sourceBindingName() == null
        bareTrace.sourceExpressionText() == 'itemFunctionalities.createItem(dto)'
        bareTrace.workflowCalls().isEmpty()
        bareTrace.constructorArguments()[1].provenance().contains('dto <- new ItemDto(')
        recipeContainsKind(bareTrace.constructorArguments()[1].recipe(), GroovyValueKind.CONSTRUCTOR)
        bareTraceText.contains('resolved via facade ItemFunctionalitiesFacade.createItem(...)')
        bareTraceText.contains('arg[1]: dto <- new ItemDto(')

        and:
        helperTrace != null
        helperTrace.originKind() == GroovyTraceOriginKind.DIRECT_CONSTRUCTOR
        helperTrace.sourceBindingName() == 'helperSaga'
        helperTrace.constructorArguments()[1].provenance().contains('buildItemDtoViaFacade(...)')
        recipeContainsKind(helperTrace.constructorArguments()[1].recipe(), GroovyValueKind.HELPER_CALL_RESULT)
        !helperTrace.constructorArguments()[1].provenance().contains('[unresolved cyclic reference]')
        helperTraceText.contains('resolved via helper createItemSaga(...)')
        helperTraceText.contains('buildItemDtoViaFacade(...) <- itemDto <- itemFunctionalities.createItem(itemDto)')
        !helperTraceText.contains('[unresolved cyclic reference]')
    }

    def 'nested helper facade traces are emitted once'() {
        given:
        def helperFacadeTraces = state.groovyFullTraceResults.findAll {
            it.sourceClassFqn == 'com.example.dummyapp.GroovyNestedFacadeTracingSpec' &&
                    it.sourceMethodName == 'buildItemDtoViaFacade' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }
        def parentTraceText = traceTextFor(
                'com.example.dummyapp.GroovyNestedFacadeTracingSpec',
                'nested helper facade result feeds item saga constructor',
                'helperSaga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        expect:
        helperFacadeTraces.size() == 1
        helperFacadeTraces[0].originKind() == GroovyTraceOriginKind.FACADE_CALL
        helperFacadeTraces[0].sourceBindingName() == null
        helperFacadeTraces[0].sourceExpressionText() == 'itemFunctionalities.createItem(itemDto)'
        parentTraceText.contains('buildItemDtoViaFacade(...) <- itemDto <- itemFunctionalities.createItem(itemDto)')
        parentTraceText.contains('resolved via helper createItemSaga(...)')
    }

    def 'same-name caller/helper locals no longer produce a cyclic marker'() {
        given:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovyNestedFacadeTracingSpec',
                'shadowed helper field/local itemDto remains acyclic',
                'shadowSaga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        expect:
        traceText.contains('buildItemDtoWithShadowing(...) <- itemDto <- itemFunctionalities.createItem(itemDto)')
        !traceText.contains('[unresolved cyclic reference]')
    }

    def 'captures named-args and setter-based dto constructor provenance from dummyapp fixture'() {
        given:
        def toSetTrace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'named args, setters, and toSet provenance feed item saga constructor' &&
                    it.sourceBindingName() == 'saga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }

        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'named args, setters, and toSet provenance feed item saga constructor',
                'saga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        then:
        toSetTrace != null
        traceText.contains('arg[1]: setterDto <- new ItemDto()')
        traceText.contains('saga.resumeWorkflow(...)')
    }

    def 'recognizes local toSet collection transforms from dummyapp fixture'() {
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'local toSet literal feeds order saga constructor' &&
                    it.sourceBindingName == 'toSetSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }

        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'local toSet literal feeds order saga constructor',
                'toSetSaga',
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )

        then:
        trace != null
        recipeContainsKind(trace.constructorArguments()[2].recipe(), GroovyValueKind.LOCAL_TRANSFORM)
        recipeContainsKind(trace.constructorArguments()[2].recipe(), GroovyValueKind.COLLECTION_LITERAL)
        traceText.contains('arg[2]: aggregateCount <- [1, 2, 3].toSet().size()')
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

    def 'captures facade assignment and bare call traces from dummyapp fixture'() {
        given:
        def assignmentTrace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'facade assignment traces order saga recipe' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }
        def bareTrace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'bare facade call traces order saga recipe' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }

        when:
        def assignmentTraceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'facade assignment traces order saga recipe',
                null,
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )
        def bareTraceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'bare facade call traces order saga recipe',
                null,
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )

        then:
        assignmentTrace != null
        assignmentTrace.originKind() == GroovyTraceOriginKind.FACADE_CALL
        assignmentTrace.sourceBindingName() == null
        assignmentTrace.sourceExpressionText() == 'orderFunctionalities.createOrder(null)'
        assignmentTrace.workflowCalls().isEmpty()
        assignmentTrace.constructorArguments()*.recipe()*.kind() == [
                GroovyValueKind.UNRESOLVED_VARIABLE,
                GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                GroovyValueKind.LITERAL,
                GroovyValueKind.UNRESOLVED_VARIABLE
        ]
        assignmentTraceText.contains('orderFunctionalities.createOrder(null)')
        assignmentTraceText.contains('resolved via facade OrderFunctionalitiesFacade.createOrder(...)')
        assignmentTraceText.contains('arg[2]: null')

        and:
        bareTrace != null
        bareTrace.originKind() == GroovyTraceOriginKind.FACADE_CALL
        bareTrace.sourceBindingName() == null
        bareTrace.sourceExpressionText() == 'orderFunctionalities.createOrder(null)'
        bareTrace.workflowCalls().isEmpty()
        bareTrace.constructorArguments()*.recipe()*.kind() == [
                GroovyValueKind.UNRESOLVED_VARIABLE,
                GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                GroovyValueKind.LITERAL,
                GroovyValueKind.UNRESOLVED_VARIABLE
        ]
        bareTraceText.contains('orderFunctionalities.createOrder(null)')
        bareTraceText.contains('resolved via facade OrderFunctionalitiesFacade.createOrder(...)')
        bareTraceText.contains('arg[2]: null')
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
        trace.originKind() == GroovyTraceOriginKind.DIRECT_CONSTRUCTOR
        trace.constructorArguments*.index == [0, 1]
        trace.constructorArguments*.provenance == ['null', 'null']
        trace.constructorArguments*.recipe*.kind == [GroovyValueKind.LITERAL, GroovyValueKind.LITERAL]
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

    private static boolean recipeContainsKind(GroovyValueRecipe recipe, GroovyValueKind kind) {
        if (recipe == null) {
            return false
        }

        if (recipe.kind() == kind) {
            return true
        }

        return recipe.children().any { child -> recipeContainsKind(child, kind) }
    }

}
