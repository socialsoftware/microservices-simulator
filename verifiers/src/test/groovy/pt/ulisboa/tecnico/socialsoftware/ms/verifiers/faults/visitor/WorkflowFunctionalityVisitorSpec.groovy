package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import ch.qos.logback.classic.Logger
import com.github.javaparser.StaticJavaParser
import ch.qos.logback.core.read.ListAppender
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicityKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory
import org.slf4j.LoggerFactory
import spock.lang.Shared

class WorkflowFunctionalityVisitorSpec extends VisitorTestSupport {

    @Shared ApplicationAnalysisState state = new ApplicationAnalysisState()
    @Shared ServiceVisitor serviceVisitor = new ServiceVisitor()
    @Shared CommandHandlerVisitor commandHandlerVisitor = new CommandHandlerVisitor()
    @Shared WorkflowFunctionalityVisitor workflowVisitor = new WorkflowFunctionalityVisitor()
    @Shared ListAppender appender = new ListAppender()
    @Shared Logger logger = LoggerFactory.getLogger(WorkflowFunctionalityVisitor) as Logger

    def setupSpec() {
        logger.addAppender(appender)
        appender.start()
        configureParser()
        def cus = parseAllDummyappFiles()
        cus.each { cu -> serviceVisitor.visit(cu, state) }
        cus.each { cu -> commandHandlerVisitor.visit(cu, state) }
        cus.each { cu -> workflowVisitor.visit(cu, state) }
    }

    def "WorkflowFunctionalityVisitor finds CreateItemFunctionalitySagas"() {
        expect:
        state.sagas.any { it.fqn.contains('CreateItemFunctionalitySagas') }
    }

    def "WorkflowFunctionalityVisitor finds field-injected workflows"() {
        expect:
        state.sagas.any { it.fqn.contains('CreateItemFieldInjectionFunctionalitySagas') }
    }

    def "CreateItemFunctionalitySagas has two steps"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemFunctionalitySagas') }

        expect:
        saga != null
        saga.steps.size() == 2
    }

    def "getOrderStep is a READ on Order"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'getOrderStep' }

        expect:
        step != null
        step.dispatches.size() == 1
        step.predecessorStepKeys.isEmpty()
        with(step.dispatches.first()) {
            aggregateName() == 'Order'
            accessPolicy() == AccessPolicy.READ
        }
    }

    def "createItemStep is a WRITE on Item"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'createItemStep' }

        expect:
        step != null
        step.dispatches.size() == 1
        step.predecessorStepKeys == ['CreateItemFunctionalitySagas::getOrderStep'] as Set
        with(step.dispatches.first()) {
            aggregateName() == 'Item'
            accessPolicy() == AccessPolicy.WRITE
        }
    }

    def "compensation step is extracted with compensation phase"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemCompensationFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'createItemStep' }

        expect:
        saga != null
        step != null
        step.dispatches.size() == 2
        step.dispatches.count { it.phase() == DispatchPhase.FORWARD } == 1
        step.dispatches.count { it.phase() == DispatchPhase.COMPENSATION } == 1

        with(step.dispatches.find { it.phase() == DispatchPhase.COMPENSATION }) {
            aggregateName() == 'Item'
            accessPolicy() == AccessPolicy.WRITE
        }
    }

    def "compensation registration is retained without a recognized compensation dispatch"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemCompensationFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'explicitWithoutRecognizedDispatchStep' }

        expect:
        step.compensationRegistered
        step.dispatches.count { it.phase() == DispatchPhase.FORWARD } == 1
        step.dispatches.count { it.phase() == DispatchPhase.COMPENSATION } == 0
        step.isDispatchAnalysisComplete(DispatchPhase.FORWARD)
        step.isDispatchAnalysisComplete(DispatchPhase.COMPENSATION)
    }

    def "ordinary helper call makes mixed read forward analysis incomplete without fabricating helper footprints"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemCompensationFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'mixedReadHelperStep' }
        def forwardDispatches = step.dispatches.findAll { it.phase() == DispatchPhase.FORWARD }

        expect:
        forwardDispatches.size() == 1
        forwardDispatches.first().accessPolicy() == AccessPolicy.READ
        !step.isDispatchAnalysisComplete(DispatchPhase.FORWARD)
        step.analysisDiagnostics.findAll { it.phase() == DispatchPhase.FORWARD }*.code() == ['UNRESOLVED_COMMAND_DISPATCH']
        step.analysisDiagnostics.find { it.code() == 'UNRESOLVED_COMMAND_DISPATCH' }.message() ==
                'cannot resolve command dispatch through call updateItemThroughHelper'
    }

    def "constructor helper and unsupported send shapes remain conservative"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemCompensationFunctionalitySagas') }

        expect:
        ['constructorKeyHelperStep', 'overloadedGatewayStep', 'unrelatedSendStep', 'mismatchedCommandBindingStep'].each { stepName ->
            def step = saga.steps.find { it.name == stepName }
            def forwardDispatches = step.dispatches.findAll { it.phase() == DispatchPhase.FORWARD }
            assert forwardDispatches.size() == 1
            assert forwardDispatches.first().accessPolicy() == AccessPolicy.READ
            assert !step.isDispatchAnalysisComplete(DispatchPhase.FORWARD)
            assert step.analysisDiagnostics.findAll { it.phase() == DispatchPhase.FORWARD }*.code() == [
                    stepName == 'constructorKeyHelperStep'
                            ? 'UNRESOLVED_AGGREGATE_KEY'
                            : 'UNRESOLVED_COMMAND_DISPATCH'
            ]
        }

        and:
        saga.steps.find { it.name == 'constructorKeyHelperStep' }.analysisDiagnostics.first().message() ==
                'cannot resolve aggregate key from call getAndUpdateItemKey'
        ['overloadedGatewayStep', 'unrelatedSendStep', 'mismatchedCommandBindingStep'].each { stepName ->
            assert saga.steps.find { it.name == stepName }.analysisDiagnostics.first().message() ==
                    'cannot resolve command dispatch through call send'
        }
    }

    def "SagaCommand is transparent and generic compensation retains recovery evidence"() {
        given:
        def saga = analyzeSyntheticSaga('WrappedGenericCompensationSaga', '''
            private CommandGateway gateway;

            public WrappedGenericCompensationSaga(SagaUnitOfWorkService service,
                    CommandGateway gateway, ItemDto itemDto, Integer itemAggregateId,
                    SagaUnitOfWork unitOfWork) {
                this.service = service;
                this.gateway = gateway;
                SagaStep step = new SagaStep("wrappedStep", () -> {
                    GetItemCommand payload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    SagaCommand wrapper = new SagaCommand(payload);
                    wrapper.setSemanticLock(null);
                    gateway.send(wrapper);
                });
                step.registerCompensation(() -> {
                    Command payload = new Command(unitOfWork, "Item", itemAggregateId);
                    SagaCommand wrapper = new SagaCommand(payload);
                    wrapper.setForbiddenStates(new ArrayList<>());
                    gateway.send(wrapper);
                }, unitOfWork);
            }
        ''')
        def step = saga.steps.find { it.name == 'wrappedStep' }

        expect: 'the typed payload is recorded once and the bare payload describes compensation'
        step.dispatches.size() == 2
        step.dispatches.count { it.phase() == DispatchPhase.FORWARD } == 1
        step.dispatches.count { it.phase() == DispatchPhase.COMPENSATION } == 1
        with(step.dispatches.find { it.phase() == DispatchPhase.FORWARD }) {
            commandTypeFqn().endsWith('.GetItemCommand')
            aggregateName() == 'Item'
            aggregateKeyText() == 'itemAggregateId'
        }
        with(step.dispatches.find { it.phase() == DispatchPhase.COMPENSATION }) {
            commandTypeFqn() == 'pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command'
            aggregateName() == 'Item'
            accessPolicy() == AccessPolicy.WRITE
            aggregateKeyText() == 'itemAggregateId'
        }

        and: 'wrapper configuration and gateway plumbing add no limitations'
        step.analysisDiagnostics.isEmpty()
        step.compensationRegistered
        step.isDispatchAnalysisComplete(DispatchPhase.COMPENSATION)
    }

    def "one unresolved generic compensation payload produces one useful limitation"() {
        given:
        def saga = analyzeSyntheticSaga('UnresolvedGenericCompensationSaga', '''
            private CommandGateway gateway;

            public UnresolvedGenericCompensationSaga(SagaUnitOfWorkService service,
                    CommandGateway gateway, Integer itemAggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                this.gateway = gateway;
                SagaStep step = new SagaStep("unresolvedCompensationStep", () -> { });
                step.registerCompensation(() -> {
                    Command payload = new Command(unitOfWork, serviceName(), itemAggregateId);
                    SagaCommand wrapper = new SagaCommand(payload);
                    wrapper.setSemanticLock(null);
                    gateway.send(wrapper);
                }, unitOfWork);
            }

            private String serviceName() { return "Item"; }
        ''')
        def step = saga.steps.find { it.name == 'unresolvedCompensationStep' }

        expect:
        step.dispatches.findAll { it.phase() == DispatchPhase.COMPENSATION }.isEmpty()
        step.analysisDiagnostics.findAll { it.phase() == DispatchPhase.COMPENSATION }*.code() ==
                ['UNRESOLVED_COMPENSATION_PAYLOAD']
        step.analysisDiagnostics.first().message() ==
                'cannot resolve compensation command target and aggregate key'
    }

    def "ordinary accessors mutations and collection plumbing do not create limitations"() {
        given:
        def saga = analyzeSyntheticSaga('OrdinaryCallSaga', '''
            private CommandGateway gateway;

            public OrdinaryCallSaga(SagaUnitOfWorkService service, CommandGateway gateway,
                    ItemDto itemDto, Integer itemAggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                this.gateway = gateway;
                SagaStep step = new SagaStep("ordinaryCallsStep", () -> {
                    List<Integer> ids = new ArrayList<>();
                    ids.add(itemAggregateId);
                    ids.get(0);
                    itemDto.setAggregateId(itemDto.getAggregateId());
                    GetItemCommand payload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    gateway.send(payload);
                });
            }
        ''')

        expect:
        saga.steps.find { it.name == 'ordinaryCallsStep' }.analysisDiagnostics.isEmpty()
    }

    def "opaque helper that dispatches a command remains limited once"() {
        given:
        def saga = analyzeSyntheticSaga('OpaqueDispatchHelperSaga', '''
            private CommandGateway gateway;

            public OpaqueDispatchHelperSaga(SagaUnitOfWorkService service, CommandGateway gateway,
                    Integer itemAggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                this.gateway = gateway;
                SagaStep step = new SagaStep("opaqueHelperStep", () -> {
                    dispatchThroughHelper(unitOfWork, itemAggregateId);
                    dispatchThroughHelper(unitOfWork, itemAggregateId);
                });
            }

            private void dispatchThroughHelper(SagaUnitOfWork unitOfWork, Integer itemAggregateId) {
                gateway.send(new GetItemCommand(unitOfWork, "Item", itemAggregateId));
            }
        ''')
        def diagnostics = saga.steps.find { it.name == 'opaqueHelperStep' }.analysisDiagnostics

        expect:
        diagnostics*.code() == ['UNRESOLVED_COMMAND_DISPATCH']
        diagnostics.first().message() == 'cannot resolve command dispatch through call dispatchThroughHelper'
    }

    def "direct read command plumbing and nested key getter remain complete"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemCompensationFunctionalitySagas') }
        def directLocalStep = saga.steps.find { it.name == 'readOnlyStep' }
        def inlineStep = saga.steps.find { it.name == 'inlineReadOnlyStep' }

        expect:
        [directLocalStep, inlineStep].every { step ->
            step.isDispatchAnalysisComplete(DispatchPhase.FORWARD) &&
                    step.analysisDiagnostics.findAll { it.phase() == DispatchPhase.FORWARD }.isEmpty()
        }
    }

    def "exact dispatched SagaCommand semantic locks add status writes while read-only forms stay reads"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemCompensationFunctionalitySagas') }
        def steps = saga.steps.collectEntries { [(it.name): it] }

        expect: 'an arbitrary semantic state and NOT_IN_SAGA both persist state on the payload aggregate and key'
        ['semanticLockReadStep', 'semanticLockClearingStep'].each { stepName ->
            def dispatches = steps[stepName].dispatches.findAll { it.phase() == DispatchPhase.FORWARD }
            assert dispatches*.accessPolicy() == [AccessPolicy.READ, AccessPolicy.WRITE]
            assert dispatches*.aggregateName().toSet() == ['Item'] as Set
            assert dispatches*.aggregateKeyText().toSet() == ['itemDto.getAggregateId()'] as Set
            assert dispatches.last().commandTypeFqn() ==
                    'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand'
            assert steps[stepName].analysisDiagnostics.isEmpty()
        }

        and: 'forbidden-state verification retains the payload read without inventing a write'
        steps.forbiddenStateReadStep.dispatches*.accessPolicy() == [AccessPolicy.READ]
        steps.forbiddenStateReadStep.analysisDiagnostics.isEmpty()

        and: 'a setter after the send does not affect the earlier occurrence'
        steps.postDispatchLockStep.dispatches*.accessPolicy() == [AccessPolicy.READ]
        steps.postDispatchLockStep.analysisDiagnostics.isEmpty()

        and: 'configuration hidden behind a helper cannot become confidently effect-free'
        steps.uncertainWrapperConfigurationStep.dispatches*.accessPolicy() == [AccessPolicy.READ]
        !steps.uncertainWrapperConfigurationStep.isDispatchAnalysisComplete(DispatchPhase.FORWARD)
        steps.uncertainWrapperConfigurationStep.analysisDiagnostics*.code() == ['UNRESOLVED_COMMAND_DISPATCH']
    }

    def "semantic lock on a different undispatched wrapper does not affect the sent occurrence"() {
        given:
        def saga = analyzeSyntheticSaga('ExactWrapperOccurrenceSaga', '''
            private CommandGateway gateway;

            public ExactWrapperOccurrenceSaga(SagaUnitOfWorkService service,
                    CommandGateway gateway, Integer itemAggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                this.gateway = gateway;
                SagaStep step = new SagaStep("exactWrapperStep", () -> {
                    GetItemCommand lockedPayload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    SagaCommand lockedWrapper = new SagaCommand(lockedPayload);
                    lockedWrapper.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                    GetItemCommand sentPayload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    SagaCommand sentWrapper = new SagaCommand(sentPayload);
                    gateway.send(sentWrapper);
                });
            }
        ''')
        def step = saga.steps.find { it.name == 'exactWrapperStep' }

        expect:
        step.dispatches*.accessPolicy() == [AccessPolicy.READ, AccessPolicy.READ]
        step.dispatches.every { it.commandTypeFqn().endsWith('.GetItemCommand') }
        step.analysisDiagnostics.isEmpty()
    }

    def "branch and alias wrapper configuration remain conservative"() {
        given:
        def saga = analyzeSyntheticSaga('UncertainWrapperShapeSaga', '''
            private CommandGateway gateway;

            public UncertainWrapperShapeSaga(SagaUnitOfWorkService service,
                    CommandGateway gateway, Integer itemAggregateId, boolean clear,
                    SagaUnitOfWork unitOfWork) {
                this.service = service;
                this.gateway = gateway;
                SagaStep branchStep = new SagaStep("branchStep", () -> {
                    GetItemCommand payload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    SagaCommand wrapper = new SagaCommand(payload);
                    wrapper.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                    if (clear) {
                        wrapper.setSemanticLock(null);
                    }
                    gateway.send(wrapper);
                });
                SagaStep aliasStep = new SagaStep("aliasStep", () -> {
                    GetItemCommand payload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    SagaCommand wrapper = new SagaCommand(payload);
                    SagaCommand alias = wrapper;
                    alias.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                    gateway.send(wrapper);
                });
                SagaStep assignedAliasStep = new SagaStep("assignedAliasStep", () -> {
                    GetItemCommand payload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    SagaCommand wrapper = new SagaCommand(payload);
                    SagaCommand alias;
                    alias = wrapper;
                    alias.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                    gateway.send(wrapper);
                });
                SagaStep loopReuseStep = new SagaStep("loopReuseStep", () -> {
                    GetItemCommand payload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    SagaCommand wrapper = new SagaCommand(payload);
                    for (int index = 0; index < 2; index++) {
                        gateway.send(wrapper);
                        wrapper.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                    }
                });
            }
        ''')

        expect:
        ['branchStep', 'aliasStep', 'assignedAliasStep', 'loopReuseStep'].each { stepName ->
            def step = saga.steps.find { it.name == stepName }
            assert step.dispatches*.accessPolicy() == [AccessPolicy.READ]
            assert !step.isDispatchAnalysisComplete(DispatchPhase.FORWARD)
            assert step.analysisDiagnostics*.code() == ['UNRESOLVED_SAGA_COMMAND_CONFIGURATION']
        }
    }

    def "SagaCommand subtype and anonymous constructor configuration remain conservative"() {
        given:
        def saga = analyzeSyntheticSaga('ConstructedWrapperConfigurationSaga', '''
            private CommandGateway gateway;

            private static class CustomSagaCommand extends SagaCommand {
                CustomSagaCommand(Command payload) {
                    super(payload);
                    setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                }
            }

            public ConstructedWrapperConfigurationSaga(SagaUnitOfWorkService service,
                    CommandGateway gateway, Integer itemAggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                this.gateway = gateway;
                SagaStep subtypeStep = new SagaStep("subtypeStep", () -> {
                    GetItemCommand payload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    CustomSagaCommand wrapper = new CustomSagaCommand(payload);
                    gateway.send(wrapper);
                });
                SagaStep anonymousStep = new SagaStep("anonymousStep", () -> {
                    GetItemCommand payload = new GetItemCommand(unitOfWork, "Item", itemAggregateId);
                    SagaCommand wrapper = new SagaCommand(payload) {{
                        setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                    }};
                    gateway.send(wrapper);
                });
            }
        ''')

        expect:
        ['subtypeStep', 'anonymousStep'].each { stepName ->
            def step = saga.steps.find { it.name == stepName }
            assert step.dispatches*.accessPolicy() == [AccessPolicy.READ]
            assert !step.isDispatchAnalysisComplete(DispatchPhase.FORWARD)
            assert step.analysisDiagnostics*.code() == ['UNRESOLVED_SAGA_COMMAND_CONFIGURATION']
        }
    }

    def "method-reference forward analysis remains structurally unresolved"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemCompensationFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'conservativeUnresolvedStep' }

        expect:
        !step.isDispatchAnalysisComplete(DispatchPhase.FORWARD)
        step.analysisDiagnostics*.code().contains('UNSUPPORTED_METHOD_REFERENCE')
    }

    def "plain command aggregate parameters map to their saga constructor argument"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('RenameItemFromEventFunctionalitySagas') }
        def dispatch = saga.steps.find { it.name == 'renameItemFromEventStep' }.dispatches.first()

        expect:
        dispatch.aggregateName() == 'Item'
        dispatch.aggregateKeyText() == 'itemAggregateId'
        dispatch.aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.SYMBOLIC
        dispatch.aggregateKeyConstructorArgumentIndex() == 1
    }

    def "semantic command definitions select only their real root key"() {
        given:
        def saga = analyzeSyntheticSaga('SemanticRootKeyFunctionalitySagas', '''
            public SemanticRootKeyFunctionalitySagas(SagaUnitOfWorkService service,
                    ItemDto itemDto, Integer itemAggregateId, Integer relatedAggregateId,
                    SagaUnitOfWork unitOfWork) {
                this.service = service;
                buildWorkflow(itemDto, itemAggregateId, relatedAggregateId, unitOfWork);
            }

            private void buildWorkflow(ItemDto itemDto, Integer itemAggregateId,
                    Integer relatedAggregateId, SagaUnitOfWork unitOfWork) {
                SagaStep reorderedStep = new SagaStep("semanticReorderedStep", () -> {
                    new SemanticRootItemCommand(unitOfWork, relatedAggregateId, "Item", itemAggregateId);
                });
                SagaStep getterStep = new SagaStep("semanticGetterStep", () -> {
                    new SemanticRootItemCommand(unitOfWork, "Item", itemDto);
                });
                SagaStep delegatedStep = new SagaStep("semanticDelegatedStep", () -> {
                    new SemanticRootItemCommand(unitOfWork, "Item", itemAggregateId, true);
                });
                SagaStep literalStep = new SagaStep("semanticLiteralStep", () -> {
                    new SemanticRootItemCommand(unitOfWork, "Item", 1L);
                });
                SagaStep signedDefinitionNegativeStep = new SagaStep("semanticSignedDefinitionNegativeStep", () -> {
                    new SemanticRootItemCommand(unitOfWork, "Item", 1.0f);
                });
                SagaStep signedDefinitionPositiveStep = new SagaStep("semanticSignedDefinitionPositiveStep", () -> {
                    new SemanticRootItemCommand(unitOfWork, "Item", 1.0d);
                });
                SagaStep directLiteralStep = new SagaStep("semanticDirectLiteralStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", 73, null);
                });
                SagaStep signedCreationNegativeStep = new SagaStep("semanticSignedCreationNegativeStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", -73, null);
                });
                SagaStep signedCreationPositiveStep = new SagaStep("semanticSignedCreationPositiveStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", +74, null);
                });
                SagaStep localAliasStep = new SagaStep("semanticLocalAliasStep", () -> {
                    Integer localAlias = itemAggregateId;
                    new UpdateItemCommand(unitOfWork, "Item", localAlias, null);
                });
                SagaStep nullStep = new SagaStep("semanticNullStep", () -> {
                    new SemanticRootItemCommand(unitOfWork, "Item");
                });
                SagaStep unsupportedStep = new SagaStep("semanticUnsupportedStep", () -> {
                    new SemanticRootItemCommand(unitOfWork, "Item", itemAggregateId, "unsupported");
                });
                SagaStep creationTransformStep = new SagaStep("semanticCreationTransformStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", normalize(itemAggregateId), null);
                });
                SagaStep ambiguousStep = new SagaStep("semanticAmbiguousStep", () -> {
                    new SemanticRootItemCommand(unitOfWork, "Item", itemAggregateId, 'a');
                });
            }

            private Integer normalize(Integer itemAggregateId) {
                return itemAggregateId;
            }
        ''')
        def dispatch = { String stepName -> saga.steps.find { it.name == stepName }.dispatches.first() }

        expect: 'the fourth public constructor argument wins over the unrelated second argument'
        with(dispatch('semanticReorderedStep')) {
            aggregateKeyText() == 'itemAggregateId'
            aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.SYMBOLIC
            aggregateKeyConstructorArgumentIndex() == 2
            aggregateKeyPropertyPath().isEmpty()
        }

        and: 'a getter in the command definition remains a canonical source-key suffix'
        with(dispatch('semanticGetterStep')) {
            aggregateKeyText() == 'itemDto.aggregateId'
            aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.SYMBOLIC
            aggregateKeyConstructorArgumentIndex() == 1
            aggregateKeyPropertyPath() == ['aggregateId']
        }

        and: 'one explicit command-constructor delegation is followed'
        with(dispatch('semanticDelegatedStep')) {
            aggregateKeyText() == 'itemAggregateId'
            aggregateKeyConstructorArgumentIndex() == 2
        }

        and: 'literal roots are exact while null and unsupported transforms remain type-only'
        with(dispatch('semanticLiteralStep')) {
            aggregateKeyText() == '41'
            aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.EXACT
            aggregateKeyConstructorArgumentIndex() == null
        }
        with(dispatch('semanticDirectLiteralStep')) {
            aggregateKeyText() == '73'
            aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.EXACT
            aggregateKeyConstructorArgumentIndex() == null
        }
        with(dispatch('semanticSignedDefinitionNegativeStep')) {
            aggregateKeyText() == '-41'
            aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.EXACT
        }
        with(dispatch('semanticSignedDefinitionPositiveStep')) {
            aggregateKeyText() == '+42'
            aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.EXACT
        }
        with(dispatch('semanticSignedCreationNegativeStep')) {
            aggregateKeyText() == '-73'
            aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.EXACT
        }
        with(dispatch('semanticSignedCreationPositiveStep')) {
            aggregateKeyText() == '+74'
            aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.EXACT
        }
        with(dispatch('semanticLocalAliasStep')) {
            aggregateKeyText() == 'localAlias'
            aggregateKeyConfidence() == StepDispatchFootprint.AggregateKeyConfidence.SYMBOLIC
            aggregateKeyConstructorArgumentIndex() == null
        }
        ['semanticNullStep', 'semanticUnsupportedStep', 'semanticCreationTransformStep',
         'semanticAmbiguousStep'].collect { stepName ->
            def unresolved = dispatch(stepName)
            [unresolved.aggregateKeyText(), unresolved.aggregateKeyConfidence(),
             unresolved.aggregateKeyConstructorArgumentIndex(), unresolved.aggregateKeyPropertyPath()]
        } == [[null, null, null, []], [null, null, null, []],
              [null, null, null, []], [null, null, null, []]]
    }

    def "same-named overloads do not create aggregate-key constructor evidence"() {
        given:
        def saga = analyzeSyntheticSaga('SameNamedOverloadSaga', '''
            public SameNamedOverloadSaga(SagaUnitOfWorkService service, SagaUnitOfWork unitOfWork) {
                this.service = service;
                buildWorkflow("not-an-aggregate-key", unitOfWork);
            }

            private void buildWorkflow(String ignored, SagaUnitOfWork unitOfWork) { }

            private void buildWorkflow(Integer aggregateId, SagaUnitOfWork unitOfWork) {
                SagaStep step = new SagaStep("overloadStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", aggregateId, null);
                });
            }
        ''')

        expect:
        saga.steps.find { it.name == 'overloadStep' }.dispatches.first()
                .aggregateKeyConstructorArgumentIndex() == null
    }

    def "differing constructor positions block aggregate-key constructor evidence"() {
        given:
        def saga = analyzeSyntheticSaga('DifferingConstructorPositionsSaga', '''
            public DifferingConstructorPositionsSaga(SagaUnitOfWorkService service,
                    Integer aggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                buildWorkflow(aggregateId, unitOfWork);
            }

            public DifferingConstructorPositionsSaga(Integer aggregateId,
                    SagaUnitOfWorkService service, SagaUnitOfWork unitOfWork, String marker) {
                this.service = service;
                buildWorkflow(aggregateId, unitOfWork);
            }

            private void buildWorkflow(Integer aggregateId, SagaUnitOfWork unitOfWork) {
                SagaStep step = new SagaStep("differingPositionsStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", aggregateId, null);
                });
            }
        ''')

        expect:
        saga.steps.find { it.name == 'differingPositionsStep' }.dispatches.first()
                .aggregateKeyConstructorArgumentIndex() == null
    }

    def "direct constructor-local evidence is blocked when overloaded positions differ"() {
        given:
        def saga = analyzeSyntheticSaga('DifferingDirectConstructorPositionsSaga', '''
            public DifferingDirectConstructorPositionsSaga(SagaUnitOfWorkService service,
                    Integer aggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                SagaStep step = new SagaStep("directPositionsStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", aggregateId, null);
                });
            }

            public DifferingDirectConstructorPositionsSaga(Integer aggregateId,
                    SagaUnitOfWorkService service, SagaUnitOfWork unitOfWork, String marker) {
                this.service = service;
                SagaStep step = new SagaStep("directPositionsStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", aggregateId, null);
                });
            }
        ''')

        expect:
        def dispatches = saga.steps.findAll { it.name == 'directPositionsStep' }*.dispatches.flatten()
        dispatches.size() == 2
        dispatches.every { it.aggregateKeyConstructorArgumentIndex() == null }
    }

    def "method-based constructor delegation with differing positions blocks aggregate-key evidence"() {
        given:
        def saga = analyzeSyntheticSaga('DifferingMethodDelegationSaga', '''
            public DifferingMethodDelegationSaga(SagaUnitOfWorkService service,
                    Integer aggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                buildWorkflow(aggregateId, unitOfWork);
            }

            public DifferingMethodDelegationSaga(Integer aggregateId,
                    SagaUnitOfWorkService service, SagaUnitOfWork unitOfWork, String marker) {
                this(service, aggregateId, unitOfWork);
            }

            private void buildWorkflow(Integer aggregateId, SagaUnitOfWork unitOfWork) {
                SagaStep step = new SagaStep("delegatedMethodStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", aggregateId, null);
                });
            }
        ''')

        expect:
        saga.steps.find { it.name == 'delegatedMethodStep' }.dispatches.first()
                .aggregateKeyConstructorArgumentIndex() == null
    }

    def "direct-command constructor delegation with differing positions blocks aggregate-key evidence"() {
        given:
        def saga = analyzeSyntheticSaga('DifferingDirectDelegationSaga', '''
            public DifferingDirectDelegationSaga(SagaUnitOfWorkService service,
                    Integer aggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                SagaStep step = new SagaStep("delegatedDirectStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", aggregateId, null);
                });
            }

            public DifferingDirectDelegationSaga(Integer aggregateId,
                    SagaUnitOfWorkService service, SagaUnitOfWork unitOfWork, String marker) {
                this(service, aggregateId, unitOfWork);
            }
        ''')

        expect:
        saga.steps.find { it.name == 'delegatedDirectStep' }.dispatches.first()
                .aggregateKeyConstructorArgumentIndex() == null
    }

    def "agreeing constructor delegation preserves method-based aggregate-key evidence"() {
        given:
        def saga = analyzeSyntheticSaga('AgreeingMethodDelegationSaga', '''
            public AgreeingMethodDelegationSaga(SagaUnitOfWorkService service,
                    Integer aggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                buildWorkflow(aggregateId, unitOfWork);
            }

            public AgreeingMethodDelegationSaga(String marker, Integer aggregateId,
                    SagaUnitOfWork unitOfWork, SagaUnitOfWorkService service) {
                this(service, aggregateId, unitOfWork);
            }

            private void buildWorkflow(Integer aggregateId, SagaUnitOfWork unitOfWork) {
                SagaStep step = new SagaStep("agreeingDelegatedMethodStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", aggregateId, null);
                });
            }
        ''')

        expect:
        saga.steps.find { it.name == 'agreeingDelegatedMethodStep' }.dispatches.first()
                .aggregateKeyConstructorArgumentIndex() == 1
    }

    def "agreeing constructor delegation preserves direct-command aggregate-key evidence"() {
        given:
        def saga = analyzeSyntheticSaga('AgreeingDirectDelegationSaga', '''
            public AgreeingDirectDelegationSaga(SagaUnitOfWorkService service,
                    Integer aggregateId, SagaUnitOfWork unitOfWork) {
                this.service = service;
                SagaStep step = new SagaStep("agreeingDelegatedDirectStep", () -> {
                    new UpdateItemCommand(unitOfWork, "Item", aggregateId, null);
                });
            }

            public AgreeingDirectDelegationSaga(String marker, Integer aggregateId,
                    SagaUnitOfWork unitOfWork, SagaUnitOfWorkService service) {
                this(service, aggregateId, unitOfWork);
            }
        ''')

        expect:
        saga.steps.find { it.name == 'agreeingDelegatedDirectStep' }.dispatches.first()
                .aggregateKeyConstructorArgumentIndex() == 1
    }

    def "WorkflowFunctionalityVisitor captures predecessor edges"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemDependencyGraphFunctionalitySagas') }
        assert saga != null
        def rootStep = saga.steps.find { it.name == 'rootStep' }
        def prepareStep = saga.steps.find { it.name == 'prepareStep' }
        def splitStep = saga.steps.find { it.name == 'splitStep' }

        expect:
        saga != null
        rootStep.predecessorStepKeys.isEmpty()
        prepareStep.predecessorStepKeys == ['CreateItemDependencyGraphFunctionalitySagas::rootStep'] as Set
        splitStep.predecessorStepKeys == ['CreateItemDependencyGraphFunctionalitySagas::rootStep'] as Set
    }

    def "WorkflowFunctionalityVisitor captures fan-in dependencies"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemDependencyGraphFunctionalitySagas') }
        assert saga != null
        def mergeStep = saga.steps.find { it.name == 'mergeStep' }

        expect:
        saga != null
        mergeStep.predecessorStepKeys == [
                'CreateItemDependencyGraphFunctionalitySagas::prepareStep',
                'CreateItemDependencyGraphFunctionalitySagas::splitStep'
        ] as Set
    }

    def "WorkflowFunctionalityVisitor captures constructor parameter type signatures"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemFunctionalitySagas') }

        expect:
        saga != null
        saga.constructorSignatures.size() == 1
        saga.constructorSignatures.first().parameterTypeFqns == [
                'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService',
                'com.example.dummyapp.item.aggregate.ItemDto',
                'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork',
                'pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway'
        ]
    }

    def "WorkflowFunctionalityVisitor inventories overloaded constructor parameter types"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateOrderFunctionalitySagas') }

        expect:
        saga != null
        saga.constructorSignatures.size() == 4
        saga.constructorSignatures*.parameterTypeFqns == [
                [
                        'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService',
                        'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork'
                ],
                [
                        'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService',
                        'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork',
                        'java.util.Set<java.lang.Integer>',
                        'java.lang.Integer',
                        'java.lang.Integer'
                ],
                [
                        'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService',
                        'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork',
                        'java.util.List<com.example.dummyapp.item.aggregate.ItemDto>',
                        'java.lang.Integer',
                        'java.lang.Integer',
                        'boolean'
                ],
                [
                        'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService',
                        'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork',
                        'java.lang.Integer',
                        'java.lang.Integer'
                ]
        ]
    }

    def "short recipe constructors default current metadata"() {
        given:
        def recipe = new GroovyValueRecipe(GroovyValueKind.LITERAL, 'hello', [])

        expect:
        recipe.metadata() != null
        recipe.metadata().category() == GroovyValueResolutionCategory.RESOLVED
        new GroovyTraceArgument(0, 'hello', recipe).expectedTypeFqn() == null
    }

    private SagaFunctionalityBuildingBlock analyzeSyntheticSaga(String className, String classBody) {
        def syntheticState = new ApplicationAnalysisState()
        def cus = parseAllDummyappFiles()
        cus.each { cu -> serviceVisitor.visit(cu, syntheticState) }
        cus.each { cu -> commandHandlerVisitor.visit(cu, syntheticState) }

        def source = """
            package com.example.dummyapp.item.coordination;

            import com.example.dummyapp.item.commands.UpdateItemCommand;
            import com.example.dummyapp.item.commands.GetItemCommand;
            import com.example.dummyapp.item.commands.SemanticRootItemCommand;
            import com.example.dummyapp.item.aggregate.ItemDto;
            import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
            import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
            import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
            import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
            import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
            import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
            import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
            import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
            import java.util.ArrayList;
            import java.util.List;

            public class ${className} extends WorkflowFunctionality {
                private SagaUnitOfWorkService service;
                ${classBody}
            }
        """.stripIndent()
        workflowVisitor.visit(StaticJavaParser.parse(source), syntheticState)
        return syntheticState.sagas.find { it.fqn.endsWith(className) }
    }

    def "WorkflowFunctionalityVisitor ignores unresolved dependency references and warns"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemDependencyGraphFunctionalitySagas') }
        assert saga != null
        def conservativeStep = saga.steps.find { it.name == 'conservativeStep' }

        expect:
        conservativeStep.predecessorStepKeys == ['CreateItemDependencyGraphFunctionalitySagas::rootStep'] as Set
        appender.list.any { event ->
            event.level.toString() == 'WARN' &&
                    event.formattedMessage.contains('Unknown dependency reference')
        }
    }

    def cleanupSpec() {
        logger.detachAppender(appender)
    }

    def "fieldInjectedStep is a READ on Order"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemFieldInjectionFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'fieldInjectedStep' }

        expect:
        saga != null
        step != null
        step.dispatches.size() == 1
        step.predecessorStepKeys.isEmpty()
        with(step.dispatches.first()) {
            aggregateName() == 'Order'
            accessPolicy() == AccessPolicy.READ
        }
    }

    def "looped static dispatch is STATIC_REPEAT with count 3"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemLoopedReadsFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'loopedStaticReadStep' }

        expect:
        saga != null
        step != null
        step.dispatches.size() == 1
        with(step.dispatches.first().multiplicity()) {
            kind() == DispatchMultiplicityKind.STATIC_REPEAT
            staticCount() == 3
        }
    }

    def "looped runtime dispatch is PARAMETRIC_REPEAT"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemLoopedReadsFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'loopedRuntimeReadStep' }

        expect:
        saga != null
        step != null
        step.dispatches.size() == 1
        with(step.dispatches.first().multiplicity()) {
            kind() == DispatchMultiplicityKind.PARAMETRIC_REPEAT
        }
    }
}
