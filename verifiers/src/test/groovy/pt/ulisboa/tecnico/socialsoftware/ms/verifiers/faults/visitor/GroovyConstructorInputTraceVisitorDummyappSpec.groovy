package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceOriginKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory
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
        fullTraces.every { trace ->
            trace.constructorArguments()*.expectedTypeFqn() == [
                    'pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService',
                    'pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork'
            ]
        }
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
        assignmentTrace.constructorArguments()[2].recipe().kind() == GroovyValueKind.UNRESOLVED_RUNTIME_EDGE
        assignmentTrace.constructorArguments()[2].recipe().metadata().category() == GroovyValueResolutionCategory.RUNTIME_CALL
        assignmentTrace.constructorArguments()[2].recipe().metadata().runtimeCall() != null
        assignmentTrace.constructorArguments()[2].recipe().metadata().runtimeCall().methodName() == 'createUnitOfWork'
        assignmentTrace.constructorArguments()[2].recipe().metadata().runtimeCall().receiverText().contains('sagaUnitOfWorkService')
        assignmentTrace.constructorArguments()[2].recipe().metadata().runtimeCall().sourceText() == 'sagaUnitOfWorkService.createUnitOfWork("createItem")'
        assignmentTrace.constructorArguments()[2].recipe().metadata().runtimeCall().arguments().size() == 1
        assignmentTrace.constructorArguments()[2].recipe().metadata().runtimeCall().arguments()[0].recipe().kind() == GroovyValueKind.LITERAL
        assignmentTrace.constructorArguments()[2].expectedTypeFqn() == 'pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork'
        assignmentTraceText.contains('resolved via facade ItemFunctionalitiesFacade.createItem(...)')
        assignmentTraceText.contains('arg[1]: dto <- new ItemDto(')
        assignmentTraceText.contains('arg[2]: unitOfWork <- sagaUnitOfWorkService.createUnitOfWork(...) [unresolved external/runtime edge]')

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

    def 'injectable placeholders carry identity and expected type'() {
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'assignment from facade call traces item saga recipe' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }

        expect:
        trace != null
        trace.constructorArguments()[0].recipe().metadata().category() == GroovyValueResolutionCategory.INJECTABLE_PLACEHOLDER
        trace.constructorArguments()[0].recipe().metadata().placeholderId()?.trim()
        trace.constructorArguments()[0].expectedTypeFqn() == 'pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService'
        trace.constructorArguments()[0].recipe().metadata().expectedTypeFqn() == 'pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService'
        trace.constructorArguments()[2].recipe().kind() == GroovyValueKind.UNRESOLVED_RUNTIME_EDGE
        trace.constructorArguments()[2].recipe().metadata().category() == GroovyValueResolutionCategory.RUNTIME_CALL
        trace.constructorArguments()[2].recipe().metadata().runtimeCall() != null
        trace.constructorArguments()[2].recipe().metadata().runtimeCall().methodName() == 'createUnitOfWork'
        trace.constructorArguments()[2].recipe().metadata().runtimeCall().receiverText().contains('sagaUnitOfWorkService')
        trace.constructorArguments()[2].recipe().metadata().runtimeCall().sourceText() == 'sagaUnitOfWorkService.createUnitOfWork("createItem")'
        trace.constructorArguments()[2].recipe().metadata().runtimeCall().arguments().size() == 1
        trace.constructorArguments()[2].recipe().metadata().runtimeCall().arguments()[0].recipe().kind() == GroovyValueKind.LITERAL
        trace.constructorArguments()[2].expectedTypeFqn() == 'pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork'
        trace.constructorArguments()[2].recipe().metadata().expectedTypeFqn() == 'pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork'
        trace.constructorArguments()[3].recipe().metadata().category() == GroovyValueResolutionCategory.INJECTABLE_PLACEHOLDER
        trace.constructorArguments()[3].recipe().metadata().placeholderId()?.trim()
        trace.constructorArguments()[3].expectedTypeFqn() == 'pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway'
        trace.constructorArguments()[3].recipe().metadata().expectedTypeFqn() == 'pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway'
        trace.constructorArguments()[0].recipe().metadata().placeholderId() != trace.constructorArguments()[3].recipe().metadata().placeholderId()
    }

    def 'nested helper facade traces are emitted once'() {
        given:
        def helperFacadeTraces = state.groovyFullTraceResults.findAll {
            it.sourceClassFqn == 'com.example.dummyapp.GroovyNestedFacadeTracingSpec' &&
                    it.sourceMethodName == 'buildItemDtoViaFacade' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }
        def helperFacadeTrace = helperFacadeTraces ? helperFacadeTraces[0] : null
        def parentTraceText = traceTextFor(
                'com.example.dummyapp.GroovyNestedFacadeTracingSpec',
                'nested helper facade result feeds item saga constructor',
                'helperSaga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        expect:
        helperFacadeTraces.size() == 1
        helperFacadeTrace.originKind() == GroovyTraceOriginKind.FACADE_CALL
        helperFacadeTrace.sourceBindingName() == null
        helperFacadeTrace.sourceExpressionText() == 'itemFunctionalities.createItem(itemDto)'
        helperFacadeTrace.constructorArguments()[1].provenance().contains('itemDto <- buildItemDto(...)')
        !helperFacadeTrace.constructorArguments()[1].provenance().contains('[unresolved cyclic reference]')
        recipeContainsKind(helperFacadeTrace.constructorArguments()[1].recipe(), GroovyValueKind.HELPER_CALL_RESULT)
        parentTraceText.contains('buildItemDtoViaFacade(...) <- itemDto <- itemFunctionalities.createItem(itemDto)')
        parentTraceText.contains('resolved via helper createItemSaga(...)')
    }

    def 'functionality runtime edge retains call arguments'() {
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'helper returning facade result feeds item saga constructor' &&
                    it.sourceBindingName == 'helperSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }

        expect:
        trace != null
        trace.constructorArguments()[1].recipe().kind() == GroovyValueKind.HELPER_CALL_RESULT
        trace.constructorArguments()[1].recipe().children().size() == 1

        and:
        def runtimeEdge = trace.constructorArguments()[1].recipe().children()[0]
        runtimeEdge.metadata().category() == GroovyValueResolutionCategory.RUNTIME_CALL
        runtimeEdge.metadata().runtimeCall() != null
        runtimeEdge.metadata().runtimeCall().methodName() == 'createItem'
        runtimeEdge.metadata().runtimeCall().receiverText().contains('itemFunctionalities')
        runtimeEdge.metadata().runtimeCall().sourceText() == 'itemFunctionalities.createItem(itemDto)'
        runtimeEdge.metadata().runtimeCall().arguments().size() == 1
        runtimeEdge.metadata().runtimeCall().arguments()[0].index() == 0
        runtimeEdge.metadata().runtimeCall().arguments()[0].recipe() != null
    }

    def 'same-name caller/helper locals no longer produce a cyclic marker'() {
        given:
        def helperFacadeTrace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovyNestedFacadeTracingSpec' &&
                    it.sourceMethodName == 'buildItemDtoWithShadowing' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovyNestedFacadeTracingSpec',
                'shadowed helper field/local itemDto remains acyclic',
                'shadowSaga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        expect:
        helperFacadeTrace != null
        helperFacadeTrace.constructorArguments()[1].provenance().contains('itemDto <- buildItemDto(...)')
        !helperFacadeTrace.constructorArguments()[1].provenance().contains('[unresolved cyclic reference]')
        traceText.contains('buildItemDtoWithShadowing(...) <- itemDto <- itemFunctionalities.createItem(itemDto)')
        !traceText.contains('[unresolved cyclic reference]')
    }

    def 'helper parameter accessor keeps caller provenance without self-reference markers'() {
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovyNestedFacadeTracingSpec' &&
                    it.sourceMethodName == 'helper parameter property access remains acyclic' &&
                    it.sourceBindingName == 'aggregateSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovyNestedFacadeTracingSpec',
                'helper parameter property access remains acyclic',
                'aggregateSaga',
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )

        expect:
        trace != null
        recipeContainsKind(trace.constructorArguments()[2].recipe(), GroovyValueKind.HELPER_CALL_RESULT)
        recipeContainsKind(trace.constructorArguments()[2].recipe(), GroovyValueKind.PROPERTY_ACCESS)
        !trace.constructorArguments()[2].provenance().contains('[unresolved self-reference]')
        !trace.constructorArguments()[2].provenance().contains('[unresolved depth-limit]')
        traceText.contains('aggregateIdFromHelper(...)')
        !traceText.contains('[unresolved self-reference]')
        !traceText.contains('[unresolved depth-limit]')
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

    def 'keeps Groovy as Set coercion as local transform for dummyapp literal'() {
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'empty list as Set feeds order saga constructor' &&
                    it.sourceBindingName == 'asSetSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }

        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'empty list as Set feeds order saga constructor',
                'asSetSaga',
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )

        then:
        trace != null
        trace.constructorArguments()[2].recipe().kind() == GroovyValueKind.LOCAL_TRANSFORM
        trace.constructorArguments()[2].recipe().text() == 'as Set'
        trace.constructorArguments()[2].recipe().metadata().category() == GroovyValueResolutionCategory.RESOLVED
        trace.constructorArguments()[2].recipe().children()[0].kind() == GroovyValueKind.COLLECTION_LITERAL
        trace.constructorArguments()[2].expectedTypeFqn() == 'java.util.Set<java.lang.Integer>'
        !recipeContainsKind(trace.constructorArguments()[2].recipe(), GroovyValueKind.UNRESOLVED_RUNTIME_EDGE)
        traceText.contains('arg[2]: [] as Set')
        !traceText.contains('arg[2]: [] as Set [unresolved external/runtime edge]')
    }

    def 'keeps toSet as local transform when dummyapp collection leaves stay unresolved'() {
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'local toSet with unresolved child leaves feeds order saga constructor' &&
                    it.sourceBindingName == 'runtimeAwareToSetSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }

        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'local toSet with unresolved child leaves feeds order saga constructor',
                'runtimeAwareToSetSaga',
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )

        then:
        trace != null
        trace.constructorArguments()[0].recipe().kind() == GroovyValueKind.LOCAL_TRANSFORM
        trace.constructorArguments()[0].recipe().text() == 'toSet'
        trace.constructorArguments()[0].recipe().children()[0].kind() == GroovyValueKind.COLLECTION_LITERAL
        recipeContainsKind(trace.constructorArguments()[0].recipe(), GroovyValueKind.UNRESOLVED_RUNTIME_EDGE)
        !trace.constructorArguments()[0].provenance().contains('.toSet() [unresolved external/runtime edge]')
        traceText.contains('arg[0]: runtimeAwareIds <- [runtimeGateway.loadExternalDto() [unresolved external/runtime edge].aggregateId, 9].toSet()')
    }

    def 'keeps runtime toSet receivers conservative in dummyapp fixture'() {
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'runtime toSet receiver remains unresolved for order saga constructor' &&
                    it.sourceBindingName == 'runtimeToSetSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }

        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'runtime toSet receiver remains unresolved for order saga constructor',
                'runtimeToSetSaga',
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        )

        then:
        trace != null
        trace.constructorArguments()[0].recipe().kind() == GroovyValueKind.UNRESOLVED_RUNTIME_EDGE
        traceText.contains('arg[0]: runtimeSet <- runtimeGateway.loadExternalDto().toSet() [unresolved external/runtime edge]')
    }

    def 'keeps unresolved runtime edges conservative for dummyapp item saga input'() {
        given:
        def trace = state.groovyFullTraceResults.find {
            it.sourceClassFqn == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName == 'runtime edge stays conservative for item saga input' &&
                    it.sourceBindingName == 'runtimeSaga' &&
                    it.sagaClassFqn == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }

        when:
        def traceText = traceTextFor(
                'com.example.dummyapp.GroovySagaTracingSpec',
                'runtime edge stays conservative for item saga input',
                'runtimeSaga',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        )

        then:
        trace != null
        trace.constructorArguments()[1].recipe().metadata().category() == GroovyValueResolutionCategory.RUNTIME_CALL
        trace.constructorArguments()[1].recipe().metadata().runtimeCall() != null
        trace.constructorArguments()[1].recipe().metadata().runtimeCall().methodName() == 'loadExternalDto'
        trace.constructorArguments()[1].recipe().metadata().runtimeCall().receiverText().contains('runtimeGateway')
        trace.constructorArguments()[1].recipe().metadata().runtimeCall().sourceText() == 'runtimeGateway.loadExternalDto()'
        trace.constructorArguments()[1].recipe().metadata().runtimeCall().arguments().isEmpty()
        trace.constructorArguments()[1].expectedTypeFqn() == 'com.example.dummyapp.item.aggregate.ItemDto'
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
        assignmentTrace.constructorArguments()[1].recipe().metadata().category() == GroovyValueResolutionCategory.RUNTIME_CALL
        assignmentTrace.constructorArguments()[1].recipe().metadata().runtimeCall() != null
        assignmentTrace.constructorArguments()[1].recipe().metadata().runtimeCall().methodName() == 'createUnitOfWork'
        assignmentTrace.constructorArguments()[1].recipe().metadata().runtimeCall().receiverText().contains('sagaUnitOfWorkService')
        assignmentTrace.constructorArguments()[1].recipe().metadata().runtimeCall().sourceText() == 'sagaUnitOfWorkService.createUnitOfWork("createOrder")'
        assignmentTrace.constructorArguments()[1].recipe().metadata().runtimeCall().arguments().size() == 1
        assignmentTrace.constructorArguments()[1].recipe().metadata().runtimeCall().arguments()[0].recipe().kind() == GroovyValueKind.LITERAL
        assignmentTraceText.contains('orderFunctionalities.createOrder(null)')
        assignmentTraceText.contains('resolved via facade OrderFunctionalitiesFacade.createOrder(...)')
        assignmentTraceText.contains('arg[1]: unitOfWork <- sagaUnitOfWorkService.createUnitOfWork(...) [unresolved external/runtime edge]')

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
        bareTrace.constructorArguments()[1].recipe().metadata().category() == GroovyValueResolutionCategory.RUNTIME_CALL
        bareTrace.constructorArguments()[1].recipe().metadata().runtimeCall() != null
        bareTrace.constructorArguments()[1].recipe().metadata().runtimeCall().methodName() == 'createUnitOfWork'
        bareTrace.constructorArguments()[1].recipe().metadata().runtimeCall().receiverText().contains('sagaUnitOfWorkService')
        bareTrace.constructorArguments()[1].recipe().metadata().runtimeCall().sourceText() == 'sagaUnitOfWorkService.createUnitOfWork("createOrder")'
        bareTrace.constructorArguments()[1].recipe().metadata().runtimeCall().arguments().size() == 1
        bareTrace.constructorArguments()[1].recipe().metadata().runtimeCall().arguments()[0].recipe().kind() == GroovyValueKind.LITERAL
        bareTraceText.contains('orderFunctionalities.createOrder(null)')
        bareTraceText.contains('resolved via facade OrderFunctionalitiesFacade.createOrder(...)')
        bareTraceText.contains('arg[1]: unitOfWork <- sagaUnitOfWorkService.createUnitOfWork(...) [unresolved external/runtime edge]')
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
