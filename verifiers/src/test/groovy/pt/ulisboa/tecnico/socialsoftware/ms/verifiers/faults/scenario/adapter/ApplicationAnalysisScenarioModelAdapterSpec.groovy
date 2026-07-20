package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicity
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicityKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationEvidenceClass
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FixtureOrigin
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRole
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceOriginKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueMetadata
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerIndexVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.EventHandlingBridgeVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.GroovyConstructorInputTraceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.ServiceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.VisitorTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityCreationSiteVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityVisitor
import spock.lang.Specification

class ApplicationAnalysisScenarioModelAdapterSpec extends VisitorTestSupport {

    def 'adapter maps saga steps and dispatch footprints'() {
        given:
        def state = new ApplicationAnalysisState()
        state.sagas << saga('com.example.order.coordination.CreateOrderFunctionalitySagas',
                step('com.example.order.coordination.CreateOrderFunctionalitySagas', 'createOrderStep', 0,
                        AccessPolicy.WRITE, 'Order'))

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)

        then:
        result.sagaDefinitions().size() == 1
        def saga = result.sagaDefinitions().first()
        saga.sagaFqn() == 'com.example.order.coordination.CreateOrderFunctionalitySagas'
        saga.steps().size() == 1

        with(saga.steps().first()) {
            deterministicId() == 'com.example.order.coordination.CreateOrderFunctionalitySagas::createOrderStep#0'
            stepKey() == 'com.example.order.coordination.CreateOrderFunctionalitySagas::createOrderStep'
            name() == 'createOrderStep'
            orderIndex() == 0
            footprints().size() == 1
            with(footprints().first()) {
                accessMode() == AccessMode.WRITE
                aggregateKey().aggregateName() == 'Order'
                aggregateKey().confidence() == FootprintConfidence.TYPE_ONLY
            }
        }

        and:
        result.counts().get('typeOnlyFootprints') == 1
        result.diagnostics().any { it.contains('type-only footprint') }
    }

    def 'adapter keeps compensation footprints separate from forward conflict footprints'() {
        given:
        def state = new ApplicationAnalysisState()
        def sagaBlock = saga('com.example.order.coordination.CreateOrderFunctionalitySagas')
        def stepBlock = step('com.example.order.coordination.CreateOrderFunctionalitySagas', 'createOrderStep', 0,
                AccessPolicy.READ, 'Order')
        stepBlock.markCompensationRegistered()
        stepBlock.addDispatch(new StepDispatchFootprint(
                'com.example.order.coordination.CreateOrderFunctionalitySagas::createOrderStep',
                'com.example.DeleteItemCommand',
                'Item',
                AccessPolicy.WRITE,
                DispatchPhase.COMPENSATION,
                new DispatchMultiplicity(DispatchMultiplicityKind.SINGLE, 1)))
        sagaBlock.addStep(stepBlock)
        state.sagas << sagaBlock

        when:
        def adaptedStep = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
                .sagaDefinitions().first().steps().first()

        then:
        adaptedStep.footprints()*.aggregateKey()*.aggregateName() == ['Order']
        adaptedStep.compensationFootprints()*.aggregateKey()*.aggregateName() == ['Item']
        adaptedStep.compensationEvidence() == CompensationEvidenceClass.EXPLICIT_COMPENSATION
    }

    def 'adapter skips missing aggregate names without type-only fallback'() {
        given:
        def state = new ApplicationAnalysisState()
        state.sagas << saga('com.example.order.coordination.CreateOrderFunctionalitySagas',
                step('com.example.order.coordination.CreateOrderFunctionalitySagas', 'createOrderStep', 0,
                        AccessPolicy.WRITE, null))

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
        def footprint = result.sagaDefinitions().first().steps().first().footprints().first()

        then:
        footprint.aggregateKey() == null
        footprint.accessMode() == AccessMode.WRITE
        result.counts().get('typeOnlyFootprints') == 0
        result.diagnostics().any { it.contains('skipped/unknown aggregate footprint because dispatch aggregate name is missing') }
        !result.diagnostics().any { it.contains('type-only footprint for') }
    }

    def 'adapter creates input variants from groovy full traces'() {
        given:
        def state = new ApplicationAnalysisState()
        state.sagas << saga('com.example.order.coordination.CreateOrderFunctionalitySagas',
                step('com.example.order.coordination.CreateOrderFunctionalitySagas', 'createOrderStep', 0,
                        AccessPolicy.WRITE, 'Order'))
        state.groovyFullTraceResults << trace(
                'com.example.order.OrderSpec',
                'build order input',
                'orderSaga',
                'com.example.order.coordination.CreateOrderFunctionalitySagas',
                'createOrder(customerDto)',
                'resolved via direct constructor',
                [
                        resolvedArg(0, 'customerId <- new CustomerDto()', 'new CustomerDto()', 'com.example.CustomerDto'),
                        legacyUnresolvedRuntimeArg(1,
                                'unitOfWork <- sagaUnitOfWorkService.createUnitOfWork("createOrder")',
                                'sagaUnitOfWorkService.createUnitOfWork("createOrder")',
                                'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork')
                ],
                'createOrder(customerDto)',
                ['resolved via direct constructor'],
                'createOrder(customerDto)\narg[0]: customerId <- new CustomerDto()\narg[1]: unitOfWork <- sagaUnitOfWorkService.createUnitOfWork("createOrder") [unresolved external/runtime edge]'
        )

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
        def variant = result.inputVariants().first()

        then:
        result.inputVariants().size() == 1
        variant.sagaFqn() == 'com.example.order.coordination.CreateOrderFunctionalitySagas'
        variant.sourceClassFqn() == 'com.example.order.OrderSpec'
        variant.sourceMethodName() == 'build order input'
        variant.sourceBindingName() == 'orderSaga'
        variant.stableSourceText() == 'createOrder(customerDto)'
        variant.constructorArgumentSummaries().size() == 2
        variant.constructorArgumentSummaries()[0].contains('arg[0]')
        variant.constructorArgumentSummaries()[1].toLowerCase().contains('unresolved') ||
                variant.constructorArgumentSummaries()[1].toLowerCase().contains('partial')
        variant.resolutionStatus() == InputResolutionStatus.PARTIAL
        variant.logicalKeyBindings().isEmpty()
        variant.sourceMode() == SourceMode.UNKNOWN
        variant.sourceModeConfidence() == SourceModeConfidence.UNKNOWN
        variant.sourceModeEvidence().isEmpty()
        variant.warnings().any { it.toLowerCase().contains('partial') || it.toLowerCase().contains('unresolved') }
        variant.inputRecipe() != null
        !variant.inputRecipe().executorReady()
        variant.inputRecipe().arguments()*.index() == [0, 1]
        variant.inputRecipe().arguments()[0].recipe().kind() == 'literal'
        variant.inputRecipe().arguments()[1].recipe().kind() == 'unresolved'
    }

    def 'adapter includes recipe fingerprints in input identity without using owners as identity'() {
        given:
        def state = new ApplicationAnalysisState()
        state.sagas << saga('com.example.order.coordination.CreateOrderFunctionalitySagas',
                step('com.example.order.coordination.CreateOrderFunctionalitySagas', 'createOrderStep', 0,
                        AccessPolicy.WRITE, 'Order'))
        state.groovyFullTraceResults << trace(
                'com.example.order.OrderSpec',
                'same source',
                'orderSaga',
                'com.example.order.coordination.CreateOrderFunctionalitySagas',
                'createOrder(customerDto)',
                'same trace',
                [resolvedArg(0, 'customerId <- value', '1', 'java.lang.Integer')],
                'createOrder(customerDto)',
                [],
                'same provenance')
        state.groovyFullTraceResults << trace(
                'com.example.order.OrderSpec',
                'same source',
                'orderSaga',
                'com.example.order.coordination.CreateOrderFunctionalitySagas',
                'createOrder(customerDto)',
                'same trace',
                [resolvedArg(0, 'customerId <- value', '2', 'java.lang.Integer')],
                'createOrder(customerDto)',
                [],
                'same provenance')

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)

        then:
        result.inputVariants().size() == 2
        result.inputVariants()*.inputRecipe()*.recipeFingerprint().toSet().size() == 2
        result.inputVariants()*.deterministicId().toSet().size() == 2
        result.inputVariants().every { it.owners()*.testMethodName() == ['same source'] }
    }

    def 'adapter copies source-mode metadata into input variants'() {
        given:
        def state = new ApplicationAnalysisState()
        state.sagas << saga('com.example.order.coordination.CreateOrderFunctionalitySagas',
                step('com.example.order.coordination.CreateOrderFunctionalitySagas', 'createOrderStep', 0,
                        AccessPolicy.WRITE, 'Order'))
        state.groovyFullTraceResults << new GroovyFullTraceResult(
                'com.example.order.OrderSpec',
                'build order input',
                'orderSaga',
                GroovyTraceOriginKind.DIRECT_CONSTRUCTOR,
                'createOrder(customerDto)',
                'com.example.order.coordination.CreateOrderFunctionalitySagas',
                SourceMode.SAGAS,
                SourceModeConfidence.TYPE_EVIDENCE,
                ['@Autowired field SagaUnitOfWorkService unitOfWorkService'],
                [resolvedArg(0, 'customerId <- 1', '1', 'java.lang.Integer')],
                [],
                [],
                'createOrder(customerDto)')

        when:
        def variant = new ApplicationAnalysisScenarioModelAdapter().adapt(state).inputVariants().first()

        then:
        variant.sourceMode() == SourceMode.SAGAS
        variant.sourceModeConfidence() == SourceModeConfidence.TYPE_EVIDENCE
        variant.sourceModeEvidence() == ['@Autowired field SagaUnitOfWorkService unitOfWorkService']
    }

    def 'adapter deduplicates equivalent inputs preferring known source mode over unknown'() {
        given:
        def state = new ApplicationAnalysisState()
        state.sagas << saga('com.example.order.coordination.CreateOrderFunctionalitySagas',
                step('com.example.order.coordination.CreateOrderFunctionalitySagas', 'createOrderStep', 0,
                        AccessPolicy.WRITE, 'Order'))
        def args = [resolvedArg(0, 'customerId <- 1', '1', 'java.lang.Integer')]
        state.groovyFullTraceResults << new GroovyFullTraceResult(
                'com.example.order.OrderSpec', 'duplicate source', 'orderSaga', GroovyTraceOriginKind.DIRECT_CONSTRUCTOR,
                'createOrder(customerDto)', 'com.example.order.coordination.CreateOrderFunctionalitySagas',
                SourceMode.UNKNOWN, SourceModeConfidence.UNKNOWN, [], args, [], [], 'same provenance')
        state.groovyFullTraceResults << new GroovyFullTraceResult(
                'com.example.order.OrderSpec', 'duplicate source', 'orderSaga', GroovyTraceOriginKind.DIRECT_CONSTRUCTOR,
                'createOrder(customerDto)', 'com.example.order.coordination.CreateOrderFunctionalitySagas',
                SourceMode.SAGAS, SourceModeConfidence.TYPE_EVIDENCE, ['saga evidence'], args, [], [], 'same provenance')

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
        def variant = result.inputVariants().first()

        then:
        result.inputVariants().size() == 1
        result.counts().get('inputVariantsDeduplicated') == 1
        variant.sourceMode() == SourceMode.SAGAS
        variant.sourceModeConfidence() == SourceModeConfidence.TYPE_EVIDENCE
        variant.sourceModeEvidence() == ['saga evidence']
    }

    def 'adapter deduplicates equivalent input variants'() {
        given:
        def state = new ApplicationAnalysisState()
        state.sagas << saga('com.example.order.coordination.CreateOrderFunctionalitySagas',
                step('com.example.order.coordination.CreateOrderFunctionalitySagas', 'createOrderStep', 0,
                        AccessPolicy.WRITE, 'Order'))

        def firstTrace = trace(
                'com.example.order.OrderSpec',
                'duplicate input source',
                'orderSaga',
                'com.example.order.coordination.CreateOrderFunctionalitySagas',
                'createOrder(customerDto)',
                'resolved via direct constructor',
                [resolvedArg(0, 'customerId <- new CustomerDto()', 'new CustomerDto()', 'com.example.CustomerDto')],
                'createOrder(customerDto)',
                ['resolved via direct constructor'],
                'createOrder(customerDto)\narg[0]: customerId <- new CustomerDto()'
        )

        state.groovyFullTraceResults << firstTrace
        state.groovyFullTraceResults << trace(
                'com.example.order.OrderSpec',
                'duplicate input source',
                'orderSaga',
                'com.example.order.coordination.CreateOrderFunctionalitySagas',
                'createOrder(customerDto)',
                'resolved via direct constructor',
                [resolvedArg(0, 'customerId <- new CustomerDto()', 'new CustomerDto()', 'com.example.CustomerDto')],
                'createOrder(customerDto)',
                ['resolved via direct constructor'],
                'createOrder(customerDto)\narg[0]: customerId <- new CustomerDto()'
        )

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)

        then:
        result.inputVariants().size() == 1
        result.counts().get('inputVariantsDeduplicated') == 1
        result.diagnostics().any { it.contains('deduplicated equivalent input variant') }
    }

    def 'adapter reports saga without usable input'() {
        given:
        def state = new ApplicationAnalysisState()
        state.sagas << saga('com.example.order.coordination.CreateOrderFunctionalitySagas',
                step('com.example.order.coordination.CreateOrderFunctionalitySagas', 'createOrderStep', 0,
                        AccessPolicy.WRITE, 'Order'))

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)

        then:
        result.sagaDefinitions().size() == 1
        result.inputVariants().isEmpty()
        result.counts().get('sagasWithoutUsableInputs') == 1
        result.diagnostics().any { it.contains('has no usable input traces') }
    }

    def 'dummyapp adapter integration exposes saga model'() {
        given:
        def state = buildDummyappAnalysisState()

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)

        then:
        result.sagaDefinitions().any { it.sagaFqn().contains('CreateItemFunctionalitySagas') }
        result.inputVariants().any { it.sagaFqn().contains('CreateItemFunctionalitySagas') }
        result.counts().get('typeOnlyFootprints') > 0
    }

    def 'dummyapp adapter integration classifies compensation checkpoint evidence conservatively'() {
        given:
        def state = buildDummyappAnalysisState()

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
        def saga = result.sagaDefinitions().find { it.sagaFqn().contains('CreateItemCompensationFunctionalitySagas') }
        def steps = saga.steps().collectEntries { [(it.name()): it] }

        then:
        steps.createItemStep.compensationEvidence() == CompensationEvidenceClass.EXPLICIT_COMPENSATION
        steps.createItemStep.footprints().size() == 1
        steps.createItemStep.compensationFootprints().size() == 1

        and:
        steps.explicitWithoutRecognizedDispatchStep.compensationEvidence() == CompensationEvidenceClass.EXPLICIT_COMPENSATION
        steps.explicitWithoutRecognizedDispatchStep.compensationFootprints().isEmpty()

        and:
        steps.implicitWriteStep.compensationEvidence() == CompensationEvidenceClass.IMPLICIT_SAGA_ROLLBACK
        steps.conservativeUnresolvedStep.compensationEvidence() == CompensationEvidenceClass.CONSERVATIVE_UNKNOWN
        steps.conservativeUnresolvedStep.analysisDiagnostics() == steps.conservativeUnresolvedStep.analysisDiagnostics().toSorted()
        steps.readOnlyStep.compensationEvidence() == null
    }

    def 'dummyapp adapter integration produces input variant for event-origin saga'() {
        given:
        def state = buildDummyappAnalysisState()

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
        def variant = result.inputVariants().find {
            it.sagaFqn() == 'com.example.dummyapp.item.coordination.RenameItemFromEventFunctionalitySagas'
        }

        then:
        variant != null
        variant.sourceMethodName() == 'event handling call traces downstream item rename saga'
        variant.resolutionStatus() == InputResolutionStatus.REPLAYABLE
        variant.inputRecipe() != null
        !variant.inputRecipe().executorReady()
        flattenRecipeNodes(variant.inputRecipe())*.kind().contains('event_placeholder')
        flattenRecipeNodes(variant.inputRecipe()).findAll { it.kind() == 'event_placeholder' }*.blockers().flatten().contains('EVENT_PAYLOAD_PLACEHOLDER')
    }

    def 'dummyapp adapter integration classifies setup helper ownership from static extraction'() {
        given:
        def state = buildDummyappAnalysisState()

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
        def setupHelperInput = result.inputVariants().find {
            it.sourceClassFqn() == 'com.example.dummyapp.GroovySetupHelperOwnershipSpec' &&
                    it.sourceMethodName() == 'createSetupItem' &&
                    it.sagaFqn() == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }
        def directFeatureInput = result.inputVariants().find {
            it.sourceClassFqn() == 'com.example.dummyapp.GroovySetupHelperOwnershipSpec' &&
                    it.sourceMethodName() == 'direct feature item creation remains feature under test' &&
                    it.sagaFqn() == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }
        def featureHelperInput = result.inputVariants().find {
            it.sourceClassFqn() == 'com.example.dummyapp.GroovySetupHelperOwnershipSpec' &&
                    it.sourceMethodName() == 'createItemFromFeatureHelper' &&
                    it.sagaFqn() == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }

        then:
        setupHelperInput != null
        setupHelperInput.callContextMethodName() == 'setup'
        setupHelperInput.inputRole() == InputRole.FIXTURE_PREREQUISITE
        setupHelperInput.fixtureOrigin() == FixtureOrigin.SETUP_HELPER
        setupHelperInput.owners()*.testMethodName() as Set == [
                'first feature depends on setup helper item',
                'second feature depends on setup helper item',
                'direct feature item creation remains feature under test',
                'feature calls helper that creates item'
        ] as Set

        and:
        directFeatureInput != null
        directFeatureInput.callContextMethodName() == 'direct feature item creation remains feature under test'
        directFeatureInput.inputRole() == InputRole.FEATURE_UNDER_TEST
        directFeatureInput.fixtureOrigin() == FixtureOrigin.DIRECT_FEATURE
        directFeatureInput.owners()*.testMethodName() == ['direct feature item creation remains feature under test']

        and:
        featureHelperInput != null
        featureHelperInput.callContextMethodName() == 'feature calls helper that creates item'
        featureHelperInput.inputRole() == InputRole.FEATURE_UNDER_TEST
        featureHelperInput.fixtureOrigin() == FixtureOrigin.DIRECT_FEATURE
        featureHelperInput.owners()*.testMethodName() == ['feature calls helper that creates item']
    }

    def 'dummyapp adapter integration exports representative input recipe shapes'() {
        given:
        def state = buildDummyappAnalysisState()

        when:
        def result = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
        def recipes = result.inputVariants()*.inputRecipe().findAll { it != null }
        def allNodes = recipes.collectMany { flattenRecipeNodes(it) }
        def namedSetterInput = result.inputVariants().find { it.sourceMethodName() == 'named args, setters, and toSet provenance feed item saga constructor' }
        def helperInput = result.inputVariants().find { it.sourceMethodName() == 'helper chain and accessor provenance feed item saga constructor' }
        def runtimeInput = result.inputVariants().find { it.sourceMethodName() == 'runtime edge stays conservative for item saga input' }

        then:
        recipes
        recipes.every { it.schemaVersion() == 'microservices-simulator.input-recipe.v1' }
        recipes.every { it.recipeFingerprint() }
        allNodes*.kind().containsAll(['literal', 'constructor', 'collection', 'local_transform', 'helper_result', 'property_access', 'call_result', 'placeholder'])
        !allNodes*.kind().contains('facade_call')

        and:
        def namedSetterNodes = flattenRecipeNodes(namedSetterInput.inputRecipe())
        namedSetterNodes.find { it.kind() == 'constructor' && it.targetTypeFqn() == 'com.example.dummyapp.item.aggregate.ItemDto' }
                .assignments()*.assignmentKind().containsAll(['setter', 'property'])
        namedSetterNodes.find { it.kind() == 'constructor' && it.targetTypeFqn() == 'com.example.dummyapp.item.aggregate.ItemDto' }
                .assignments()*.propertyName().containsAll(['aggregateId', 'orderId', 'name'])
        namedSetterNodes.any { it.kind() == 'local_transform' && it.transformName() == 'toSet' }

        and:
        flattenRecipeNodes(helperInput.inputRecipe())*.kind().containsAll(['helper_result', 'property_access'])
        flattenRecipeNodes(runtimeInput.inputRecipe())*.kind().contains('call_result')
    }

    private ApplicationAnalysisState buildDummyappAnalysisState() {
        configureParser()

        def state = new ApplicationAnalysisState()
        def indexVisitor = new CommandHandlerIndexVisitor()
        def serviceVisitor = new ServiceVisitor()
        def commandHandlerVisitor = new CommandHandlerVisitor()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        def creationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor()
        def eventBridgeVisitor = new EventHandlingBridgeVisitor()
        def cus = parseAllDummyappFiles()
        cus.each { cu -> indexVisitor.visit(cu, state) }
        cus.each { cu -> serviceVisitor.visit(cu, state) }
        cus.each { cu -> commandHandlerVisitor.visit(cu, state) }
        cus.each { cu -> workflowVisitor.visit(cu, state) }
        cus.each { cu -> creationSiteVisitor.visit(cu, state) }
        cus.each { cu -> eventBridgeVisitor.visit(cu, state) }
        eventBridgeVisitor.finish(state)

        def sourceIndex = new GroovySourceIndex()
        sourceIndex.parse(resolveProjectPath('applications', 'dummyapp', 'src', 'test', 'groovy'))

        new GroovyConstructorInputTraceVisitor().visit(sourceIndex, state)
        return state
    }

    private static SagaFunctionalityBuildingBlock saga(String sagaFqn, SagaStepBuildingBlock... steps) {
        def sagaBlock = new SagaFunctionalityBuildingBlock(null, 'com.example', sagaFqn)
        steps.toList().each { sagaBlock.addStep(it) }
        return sagaBlock
    }

    private static SagaStepBuildingBlock step(String sagaFqn,
                                              String stepName,
                                              int orderIndex,
                                              AccessPolicy accessPolicy,
                                              String aggregateName) {
        def stepBlock = new SagaStepBuildingBlock(null, 'com.example', sagaFqn + '::' + stepName, stepName)
        stepBlock.addDispatch(new StepDispatchFootprint(
                sagaFqn + '::' + stepName,
                'com.example.' + stepName.capitalize() + 'Command',
                aggregateName,
                accessPolicy,
                DispatchPhase.FORWARD,
                new DispatchMultiplicity(DispatchMultiplicityKind.SINGLE, 1)
        ))
        return stepBlock
    }

    private static GroovyFullTraceResult trace(String sourceClassFqn,
                                              String sourceMethodName,
                                              String sourceBindingName,
                                              String sagaFqn,
                                              String sourceExpressionText,
                                              String traceText,
                                              List<GroovyTraceArgument> constructorArguments,
                                              String stableSourceText,
                                              List<String> resolutionNotes,
                                              String provenanceText) {
        new GroovyFullTraceResult(
                sourceClassFqn,
                sourceMethodName,
                sourceBindingName,
                GroovyTraceOriginKind.DIRECT_CONSTRUCTOR,
                sourceExpressionText,
                sagaFqn,
                constructorArguments,
                [],
                resolutionNotes,
                provenanceText ?: traceText)
    }

    private static GroovyTraceArgument resolvedArg(int index, String provenance, String text, String expectedTypeFqn) {
        def recipe = new GroovyValueRecipe(
                GroovyValueKind.LITERAL,
                text,
                [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RESOLVED, expectedTypeFqn, null, null))
        new GroovyTraceArgument(index, provenance, recipe, expectedTypeFqn)
    }

    private static GroovyTraceArgument legacyUnresolvedRuntimeArg(int index,
                                                                   String provenance,
                                                                   String text,
                                                                   String expectedTypeFqn) {
        def recipe = new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, text, [])
        new GroovyTraceArgument(index, provenance, recipe, expectedTypeFqn)
    }

    private static List flattenRecipeNodes(recipe) {
        recipe.arguments().collectMany { flattenNode(it.recipe()) }
    }

    private static List flattenNode(node) {
        if (node == null) {
            return []
        }
        def children = []
        children.addAll(node.arguments().collectMany { flattenNode(it.recipe()) })
        children.addAll(node.assignments().collectMany { flattenNode(it.valueRecipe()) })
        children.addAll(node.elements().collectMany { flattenNode(it) })
        children.addAll(node.entries().collectMany { flattenNode(it.keyRecipe()) + flattenNode(it.valueRecipe()) })
        children.addAll(node.callArguments().collectMany { flattenNode(it.recipe()) })
        children.addAll(flattenNode(node.receiver()))
        children.addAll(flattenNode(node.resultRecipe()))
        [node] + children
    }
}
