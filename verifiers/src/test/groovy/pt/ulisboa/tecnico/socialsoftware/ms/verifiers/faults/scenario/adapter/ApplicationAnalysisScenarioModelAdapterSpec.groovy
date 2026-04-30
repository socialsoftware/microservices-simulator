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
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
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
                                'pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork')
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

    private ApplicationAnalysisState buildDummyappAnalysisState() {
        configureParser()

        def state = new ApplicationAnalysisState()
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
}
