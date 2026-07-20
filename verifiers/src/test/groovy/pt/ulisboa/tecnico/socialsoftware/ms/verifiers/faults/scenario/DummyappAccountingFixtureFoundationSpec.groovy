package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingCalculator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationEvidenceClass
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerIndexVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.GroovyConstructorInputTraceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.ServiceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.VisitorTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityCreationSiteVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityVisitor
import spock.lang.Shared

class DummyappAccountingFixtureFoundationSpec extends VisitorTestSupport {

    private static final String ITEM_SAGA = 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
    private static final String CANCEL_ORDER_SAGA = 'com.example.dummyapp.order.coordination.CancelOrderFromItemFunctionalitySagas'
    private static final String CREATE_ORDER_SAGA = 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
    private static final String DEPENDENCY_GRAPH_SAGA = 'com.example.dummyapp.item.coordination.CreateItemDependencyGraphFunctionalitySagas'
    private static final String LOOPED_READS_SAGA = 'com.example.dummyapp.item.coordination.CreateItemLoopedReadsFunctionalitySagas'
    private static final String COMPENSATION_SAGA = 'com.example.dummyapp.item.coordination.CreateItemCompensationFunctionalitySagas'

    @Shared ApplicationAnalysisState state
    @Shared ScenarioModelAdapterResult model

    def setupSpec() {
        configureParser()
        state = buildDummyappAnalysisState()
        model = new ApplicationAnalysisScenarioModelAdapter().adapt(state)
    }

    def 'dummyapp exposes unrelated sagas with accepted inputs'() {
        expect:
        acceptedInputsFor(ITEM_SAGA)
        acceptedInputsFor(CREATE_ORDER_SAGA)

        and: 'the create-order fixture is structurally unrelated because it has no aggregate footprint'
        !ConflictGraphBuilder.build([saga(ITEM_SAGA), saga(CREATE_ORDER_SAGA)], strictConfig()).adjacency().containsKey(ITEM_SAGA)
    }

    def 'dummyapp exposes strict symbolic interaction evidence'() {
        when:
        def strictGraph = ConflictGraphBuilder.build([saga(ITEM_SAGA), saga(CANCEL_ORDER_SAGA)], strictConfig())

        then:
        acceptedInputsFor(ITEM_SAGA)
        acceptedInputsFor(CANCEL_ORDER_SAGA)
        strictGraph.conflictCandidates().any { candidate ->
            candidate.kind() == ConflictKind.SYMBOLIC &&
                    candidate.leftFootprint().aggregateKey().confidence() == FootprintConfidence.SYMBOLIC &&
                    candidate.rightFootprint().aggregateKey().confidence() == FootprintConfidence.SYMBOLIC &&
                    candidate.leftFootprint().aggregateKey().keyText() == 'itemDto.getOrderId()' &&
                    candidate.rightFootprint().aggregateKey().keyText() == 'itemDto.getOrderId()'
        }
    }

    def 'dummyapp exposes broad type-only interaction evidence'() {
        when:
        def broadGraph = ConflictGraphBuilder.build([saga(ITEM_SAGA), saga(DEPENDENCY_GRAPH_SAGA)], broadConfig())

        then:
        broadGraph.conflictCandidates().any { candidate ->
            candidate.kind() == ConflictKind.TYPE_ONLY &&
                    (candidate.leftFootprint().aggregateKey().confidence() == FootprintConfidence.TYPE_ONLY ||
                            candidate.rightFootprint().aggregateKey().confidence() == FootprintConfidence.TYPE_ONLY)
        }
    }

    def 'dummyapp exposes an interacting saga that lacks accepted input coverage'() {
        expect:
        saga(DEPENDENCY_GRAPH_SAGA).steps().any { step -> step.footprints().any { it.accessMode() == AccessMode.WRITE } }
        acceptedInputsFor(DEPENDENCY_GRAPH_SAGA).isEmpty()

        and:
        ConflictGraphBuilder.build([saga(ITEM_SAGA), saga(DEPENDENCY_GRAPH_SAGA)], broadConfig()).conflictCandidates()
    }

    def 'dummyapp exposes a connected chain of at least three sagas'() {
        when:
        def graph = ConflictGraphBuilder.build([saga(CANCEL_ORDER_SAGA), saga(ITEM_SAGA), saga(COMPENSATION_SAGA)], broadConfig())
        def connected = ConnectedSagaSetEnumerator.enumerate([CANCEL_ORDER_SAGA, ITEM_SAGA, COMPENSATION_SAGA], graph.adjacency(), 3)

        then:
        adjacent(graph, CANCEL_ORDER_SAGA, ITEM_SAGA)
        adjacent(graph, ITEM_SAGA, COMPENSATION_SAGA)
        !adjacent(graph, CANCEL_ORDER_SAGA, COMPENSATION_SAGA)
        connected.connectedSagaSets().any { it.size() == 3 && it.containsAll([CANCEL_ORDER_SAGA, ITEM_SAGA, COMPENSATION_SAGA]) }
    }

    def 'dummyapp exposes multi-step sagas for order-preserving schedule counts'() {
        given:
        def dependencyGraphSaga = saga(DEPENDENCY_GRAPH_SAGA)
        def loopedReadsSaga = saga(LOOPED_READS_SAGA)

        expect:
        dependencyGraphSaga.steps()*.name().containsAll(['rootStep', 'prepareStep', 'splitStep', 'mergeStep', 'conservativeStep'])
        dependencyGraphSaga.steps().find { it.name() == 'mergeStep' }.predecessorStepKeys().size() == 2
        loopedReadsSaga.steps()*.name().containsAll(['loopedStaticReadStep', 'loopedRuntimeReadStep'])
    }

    def 'dummyapp exposes the complete compensation evidence fixture matrix'() {
        given:
        def steps = saga(COMPENSATION_SAGA).steps().collectEntries { [(it.name()): it] }

        expect:
        steps.createItemStep.compensationEvidence() == CompensationEvidenceClass.EXPLICIT_COMPENSATION
        steps.createItemStep.compensationFootprints()
        steps.explicitWithoutRecognizedDispatchStep.compensationEvidence() == CompensationEvidenceClass.EXPLICIT_COMPENSATION
        steps.explicitWithoutRecognizedDispatchStep.compensationFootprints().isEmpty()
        steps.implicitWriteStep.compensationEvidence() == CompensationEvidenceClass.IMPLICIT_SAGA_ROLLBACK
        steps.conservativeUnresolvedStep.compensationEvidence() == CompensationEvidenceClass.CONSERVATIVE_UNKNOWN
        steps.readOnlyStep.compensationEvidence() == null
    }

    def 'dummyapp exposes key-bearing input variants for compatible and incompatible tuple tests'() {
        given:
        def itemOrder13 = model.inputVariants().find { it.sagaFqn() == ITEM_SAGA && it.logicalKeyBindings().orderId == '13' }
        def itemOrder23 = model.inputVariants().find { it.sagaFqn() == ITEM_SAGA && it.sourceMethodName() == 'item saga input shares order id with cancellation fixture' }
        def cancelOrder23 = model.inputVariants().find { it.sagaFqn() == CANCEL_ORDER_SAGA && it.sourceMethodName() == 'item-derived order cancellation shares symbolic order key' }

        expect:
        itemOrder13 != null
        itemOrder23 != null
        cancelOrder23 != null

        and:
        InputTupleJoiner.join(sagaOrder(), inputsBySaga(itemOrder23, cancelOrder23))
                .counts().inputTuplesEmitted == 1
        InputTupleJoiner.join(sagaOrder(), inputsBySaga(itemOrder13, cancelOrder23))
                .counts().inputTuplesEmitted == 0
    }

    def 'dummyapp brute force full-write singles match exact accounting counts'() {
        given:
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                true,
                1,
                false,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL)

        when:
        def result = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), config)
        def accounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(), model.inputVariants(), config, result.workloadPlans().size())

        then:
        result.workloadPlans().size() == 7
        accounting.inputBoundScenarioSpace().allInputBound().total() == '7'
        accounting.inputBoundScenarioSpace().selectedByGenerator().total() == '7'
        accounting.inputBoundScenarioSpace().catalogWritten().total() == '7'
        accounting.groupedSagaSets()*.sagaSetSize().unique() == [1]
    }

    def 'dummyapp interaction-pruned full write matches selected accounting and prunes unrelated rows'() {
        given:
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false,
                2,
                false,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING)

        when:
        def result = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), config)
        def accounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(), model.inputVariants(), config, result.workloadPlans().size())
        def unrelated = row(accounting, [ITEM_SAGA, CREATE_ORDER_SAGA])
        def strict = row(accounting, [ITEM_SAGA, CANCEL_ORDER_SAGA])

        then:
        result.workloadPlans().size() == 0
        accounting.inputBoundScenarioSpace().selectedByGenerator().total() == '0'
        accounting.inputBoundScenarioSpace().catalogWritten().total() == '0'
        result.workloadPlans().size().toString() == accounting.inputBoundScenarioSpace().catalogWritten().total()
        accounting.inputBoundScenarioSpace().catalogWritten().total() == accounting.inputBoundScenarioSpace().selectedByGenerator().total()
        new BigInteger(accounting.inputBoundScenarioSpace().selectedByGenerator().total()) < new BigInteger(accounting.inputBoundScenarioSpace().allInputBound().total())

        and:
        unrelated != null
        !unrelated.selectedByConfiguredGenerator()
        !unrelated.strictInteractionSummary().connected()

        and:
        strict != null
        strict.selectedByConfiguredGenerator()
        strict.strictInteractionSummary().connected()
        strict.scheduleCountPerTuple() == '3'
        new BigInteger(strict.compatibleInputTupleCount()) == InputTupleJoiner.join([ITEM_SAGA, CANCEL_ORDER_SAGA], groupedInputs([ITEM_SAGA, CANCEL_ORDER_SAGA], config)).tuples().size()
        new BigInteger(strict.compatibleInputTupleCount()) < inputProduct(strict)
    }

    def 'dummyapp segment compressed full write matches accounting and preserves expanded schedules'() {
        given:
        def config = fullDummyappConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false,
                2,
                false,
                ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED)
        def opiConfig = fullDummyappConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY,
                false,
                2,
                false,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING)

        when:
        def result = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), config)
        def accounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(), model.inputVariants(), config, result.workloadPlans().size())
        def opiAccounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(), model.inputVariants(), opiConfig, 0)
        def compressedRow = row(accounting, [ITEM_SAGA, CANCEL_ORDER_SAGA])
        def opiRow = row(opiAccounting, [ITEM_SAGA, CANCEL_ORDER_SAGA])
        def interactingPlans = result.workloadPlans().findAll { plan ->
            plan.conflictEvidence() && plan.participants()*.sagaFqn().toSet() == [ITEM_SAGA, CANCEL_ORDER_SAGA] as Set
        }

        then:
        result.workloadPlans().size() > 0
        result.effectiveConfig().scheduleStrategy() == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED
        result.workloadPlans().size().toString() == accounting.inputBoundScenarioSpace().catalogWritten().total()
        accounting.inputBoundScenarioSpace().catalogWritten().total() == accounting.inputBoundScenarioSpace().selectedByGenerator().total()

        and:
        !interactingPlans.isEmpty()
        interactingPlans.any { plan -> plan.participants().any { saga(it.sagaFqn()).steps().size() > 1 } }
        interactingPlans.every { plan ->
            plan.forwardSchedule().size() == plan.participants().collect { instance -> saga(instance.sagaFqn()).steps().size() }.sum()
        }
        interactingPlans.every { plan ->
            plan.participants().every { instance ->
                def expectedStepIds = saga(instance.sagaFqn()).steps()*.deterministicId()
                def actualStepIds = plan.forwardSchedule()
                        .findAll { it.sagaInstanceId() == instance.deterministicId() }
                        .sort { it.scheduleOrder() }*.stepId()
                actualStepIds == expectedStepIds
            }
        }

        and:
        compressedRow.compatibleInputTupleCount() != '0'
        compressedRow.scenarioShapeCount() != '0'
        new BigInteger(compressedRow.scheduleCountPerTuple()) < new BigInteger(opiRow.scheduleCountPerTuple())
    }

    def 'dummyapp broad count-only accounting covers type-only missing input and three-saga chain'() {
        given:
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY,
                false,
                3,
                true,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING)

        when:
        def result = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), config)
        def accounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(), model.inputVariants(), config, 0)

        then:
        result.workloadPlans().isEmpty()
        accounting.inputBoundScenarioSpace().catalogWritten().total() == '0'
        accounting.inputBoundScenarioSpace().selectedByGenerator().total() == '0'
        accounting.inputBoundScenarioSpace().allInputBound().total() == '33'
        accounting.groupedSagaSets().size() == 4

        and:
        accounting.typeLevelCoverage().sagasWithoutAcceptedInputs().contains(DEPENDENCY_GRAPH_SAGA)
        accounting.typeLevelCoverage().sagasWithoutAcceptedInputs().contains(COMPENSATION_SAGA)
        accounting.typeLevelCoverage().broad().missingInputInteractionPairCount() > 0
        new BigInteger(accounting.typeLevelCoverage().broad().connectedSetCountsBySize()['3']) > BigInteger.ZERO
    }

    private static ApplicationAnalysisState buildDummyappAnalysisState() {
        def state = new ApplicationAnalysisState()
        def files = parseAllDummyappFiles()
        def indexVisitor = new CommandHandlerIndexVisitor()
        def serviceVisitor = new ServiceVisitor()
        def commandHandlerVisitor = new CommandHandlerVisitor()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        def creationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor()
        files.each { cu -> indexVisitor.visit(cu, state) }
        files.each { cu -> serviceVisitor.visit(cu, state) }
        files.each { cu -> commandHandlerVisitor.visit(cu, state) }
        files.each { cu -> workflowVisitor.visit(cu, state) }
        files.each { cu -> creationSiteVisitor.visit(cu, state) }

        def sourceIndex = new GroovySourceIndex()
        sourceIndex.parse(resolveProjectPath('applications', 'dummyapp', 'src', 'test', 'groovy'))
        new GroovyConstructorInputTraceVisitor().visit(sourceIndex, state)
        state
    }

    private List acceptedInputsFor(String sagaFqn) {
        model.inputVariants().findAll { it.sagaFqn() == sagaFqn }
    }

    private static Map<String, List> inputsBySaga(def itemInput, def cancelInput) {
        def inputs = new LinkedHashMap<String, List>()
        inputs.put(ITEM_SAGA.toString(), List.of(itemInput))
        inputs.put(CANCEL_ORDER_SAGA.toString(), List.of(cancelInput))
        inputs
    }

    private static List<String> sagaOrder() {
        List.of(ITEM_SAGA.toString(), CANCEL_ORDER_SAGA.toString())
    }

    private Map<String, List> groupedInputs(List<String> sagaFqns, ScenarioGeneratorConfig config) {
        def normalized = InputVariantNormalizer.normalize(model.inputVariants(), config)
        def grouped = new LinkedHashMap<String, List>()
        sagaFqns.each { sagaFqn -> grouped.put(sagaFqn, normalized.inputsBySaga().getOrDefault(sagaFqn, [])) }
        grouped
    }

    private static def row(def accounting, List<String> sagaFqns) {
        def key = sagaFqns.sort().join('|')
        accounting.groupedSagaSets().find { it.sagaSetKey() == key }
    }

    private static BigInteger inputProduct(def row) {
        row.inputCountsBySaga().values().inject(BigInteger.ONE) { acc, count -> acc.multiply(BigInteger.valueOf(count as long)) }
    }

    private def saga(String sagaFqn) {
        def saga = model.sagaDefinitions().find { it.sagaFqn() == sagaFqn }
        assert saga != null: "expected dummyapp saga ${sagaFqn}"
        saga
    }

    private static boolean adjacent(def graph, String left, String right) {
        graph.adjacency().getOrDefault(left, [] as Set).contains(right) ||
                graph.adjacency().getOrDefault(right, [] as Set).contains(left)
    }

    private static ScenarioGeneratorConfig strictConfig() {
        new ScenarioGeneratorConfig(false,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                true,
                3,
                100,
                100,
                100,
                false,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                1234L)
    }

    private static ScenarioGeneratorConfig broadConfig() {
        new ScenarioGeneratorConfig(false,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                true,
                3,
                100,
                100,
                100,
                true,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                1234L)
    }

    private static ScenarioGeneratorConfig accountingConfig(ScenarioGeneratorConfig.GenerationStrategy generationStrategy,
                                                            ScenarioGeneratorConfig.CatalogWriteMode writeMode,
                                                            boolean includeSingles,
                                                            int maxSagaSetSize,
                                                            boolean allowTypeOnlyFallback,
                                                            ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy) {
        new ScenarioGeneratorConfig(false,
                generationStrategy,
                writeMode,
                includeSingles,
                maxSagaSetSize,
                10_000,
                3,
                10_000,
                allowTypeOnlyFallback,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                scheduleStrategy,
                1234L)
    }

    private static ScenarioGeneratorConfig fullDummyappConfig(ScenarioGeneratorConfig.GenerationStrategy generationStrategy,
                                                              ScenarioGeneratorConfig.CatalogWriteMode writeMode,
                                                              boolean includeSingles,
                                                              int maxSagaSetSize,
                                                              boolean allowTypeOnlyFallback,
                                                              ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy) {
        new ScenarioGeneratorConfig(false,
                generationStrategy,
                writeMode,
                includeSingles,
                maxSagaSetSize,
                10_000,
                100,
                10_000,
                allowTypeOnlyFallback,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                scheduleStrategy,
                1234L)
    }
}
