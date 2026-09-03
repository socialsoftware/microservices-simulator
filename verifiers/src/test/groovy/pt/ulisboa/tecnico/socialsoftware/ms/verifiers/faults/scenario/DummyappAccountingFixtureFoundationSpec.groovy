package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingCalculator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ExecutableArtifactWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.StaticAnalysisArtifactWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationEvidenceClass
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequenceDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventEmissionSite
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerIndexVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.GroovyConstructorInputTraceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.ServiceVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.VisitorTestSupport
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityCreationSiteVisitor
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityVisitor
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

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
        !steps.implicitWriteStep.forwardAnalysisComplete()
        steps.implicitWriteStep.compensationEvidence() == CompensationEvidenceClass.IMPLICIT_SAGA_ROLLBACK
        steps.conservativeUnresolvedStep.compensationEvidence() == CompensationEvidenceClass.CONSERVATIVE_UNKNOWN
        steps.mixedReadHelperStep.footprints()*.accessMode() == [AccessMode.READ]
        !steps.mixedReadHelperStep.forwardAnalysisComplete()
        steps.mixedReadHelperStep.compensationEvidence() == CompensationEvidenceClass.CONSERVATIVE_UNKNOWN
        ['constructorKeyHelperStep', 'overloadedGatewayStep', 'unrelatedSendStep', 'mismatchedCommandBindingStep'].each { stepName ->
            assert steps[stepName].footprints()*.accessMode() == [AccessMode.READ]
            assert !steps[stepName].forwardAnalysisComplete()
            assert steps[stepName].compensationEvidence() == CompensationEvidenceClass.CONSERVATIVE_UNKNOWN
        }
        ['inlineReadOnlyStep', 'readOnlyStep'].each { stepName ->
            assert steps[stepName].forwardAnalysisComplete()
            assert steps[stepName].compensationEvidence() == null
        }
    }

    def 'dummyapp real parser shapes keep materialized workloads bounded and preserve recovery checkpoints'() {
        given:
        def materializedConfig = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                true,
                1,
                false,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL)

        when:
        def materialized = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), materializedConfig)

        then: 'real accepted-input workloads remain small enough for semantic exhaustive checks'
        materialized.workloadPlans()*.faultSlots()*.size().max() == 2
        materialized.workloadPlans().every { it.faultSlots().size() <= 2 }

        when: 'the parser-real compensation fixture is paired with one bounded synthetic accepted input'
        def compensationInput = new InputVariant(null,
                COMPENSATION_SAGA,
                'com.example.dummyapp.CompensationRecoverySpec',
                'fixture',
                'saga',
                InputResolutionStatus.RESOLVED,
                SourceMode.SAGAS,
                SourceModeConfidence.TYPE_EVIDENCE,
                ['saga fixture'],
                'compensation fixture',
                'dummyapp parser shape',
                [],
                [:],
                [])
        def compensationResult = ScenarioGenerator.generate(
                [saga(COMPENSATION_SAGA)],
                [compensationInput],
                materializedConfig)
        def plan = compensationResult.workloadPlans()[0]
        def recovery = RecoveryScheduleGenerator.generate(plan, '00000000001', 20)

        then:
        plan.faultSlots().size() == 11
        plan.compensationCheckpoints().size() == 9
        recovery.uncappedScheduleCount() == BigInteger.ONE
        recovery.faultScenarios().size() == 1
        recovery.faultScenarios()[0].actions()
                .findAll { it.kind() == FaultScenarioActionKind.COMPENSATION }
                *.sourceCompensationCheckpointId() == plan.compensationCheckpoints().reverse()*.deterministicId()
    }

    def 'dummyapp eager baseline tracks the ready capped selection and helper-built input vectors'() {
        given:
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                true,
                1,
                false,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL)
        def helperInput = model.inputVariants().find {
            it.sourceClassFqn() == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName() == 'buildMutatedItemDtoViaFacade' &&
                    it.callContextMethodName() == 'caller values and helper dto mutations feed item facade recipe'
        }
        assert helperInput != null

        when:
        def workloads = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), config)
        def eager = EagerFaultScenarioGenerator.generate(workloads, new RecoveryScheduleCap(20))
        def helperWorkloads = ScenarioGenerator.generate([saga(ITEM_SAGA)], [helperInput], config)
        def helperEager = EagerFaultScenarioGenerator.generate(helperWorkloads, new RecoveryScheduleCap(20))
        def helperDtoRecipe = helperInput.inputRecipe().arguments()[1].recipe()

        then: 'the approved helper input is independently ready and contributes deterministic all-zero and single-point vectors'
        helperDtoRecipe.assignments()*.propertyName() == ['aggregateId', 'name', 'orderId']
        helperDtoRecipe.assignments()*.orderIndex() == [0, 1, 2]
        helperDtoRecipe.assignments()*.valueRecipe()*.value() == [701L, 'source-helper-name', 809L]
        helperWorkloads.workloadPlans().size() == 1
        helperWorkloads.workloadPlans()[0].faultSlots().size() == 2
        helperEager.workloadMaterializability()*.materializable() == [true]
        helperEager.computedVectors()*.assignedVector() == ['00', '10', '01']
        helperEager.faultScenarios()*.assignedVector() == ['00', '10', '01']

        and: 'the fixed cap exports eager vectors only for the statically ready subset'
        eager.workloadPlans().size() == 7
        eager.workloadMaterializability().count { it.materializable() } == 6
        eager.workloadMaterializability().count { !it.materializable() } == 1
        eager.computedVectors().size() == eager.workloadPlans().findAll { plan ->
            eager.workloadMaterializability().find { it.workloadPlanId() == plan.deterministicId() }.materializable()
        }.sum { it.faultSlots().size() + 1 }
        eager.faultScenarios().size() == eager.computedVectors().size()
        eager.workloadPlans()*.deterministicId().toSet() == workloads.workloadPlans()*.deterministicId().toSet()
        eager.faultScenarios()*.workloadPlanId().toSet() ==
                eager.workloadMaterializability().findAll { it.materializable() }*.workloadPlanId().toSet()
    }

    def 'dummyapp fixed configuration and timestamp produce byte-stable workload and fault catalogs'() {
        given:
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                true,
                1,
                false,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL)
        def firstWorkloads = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), config)
        def secondWorkloads = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), config)
        def first = EagerFaultScenarioGenerator.generate(firstWorkloads, new RecoveryScheduleCap(20))
        def second = EagerFaultScenarioGenerator.generate(secondWorkloads, new RecoveryScheduleCap(20))
        def firstDirectory = Files.createTempDirectory('dummyapp-eager-first')
        def secondDirectory = Files.createTempDirectory('dummyapp-eager-second')

        when:
        writePackage(first, firstDirectory, config)
        writePackage(second, secondDirectory, config)

        then:
        first.workloadPlans()*.deterministicId() == second.workloadPlans()*.deterministicId()
        first.faultScenarios()*.deterministicId() == second.faultScenarios()*.deterministicId()
        Files.readAllBytes(firstDirectory.resolve('workloads.jsonl')) ==
                Files.readAllBytes(secondDirectory.resolve('workloads.jsonl'))
        Files.readAllBytes(firstDirectory.resolve('fault-scenarios.jsonl')) ==
                Files.readAllBytes(secondDirectory.resolve('fault-scenarios.jsonl'))
    }

    def 'dummyapp exposes key-bearing input variants for strict-positive and unequal tuple tests'() {
        given:
        def itemOrder13 = model.inputVariants().find { it.sagaFqn() == ITEM_SAGA && it.logicalKeyBindings().orderId == '13' }
        def itemOrder23 = model.inputVariants().find { it.sagaFqn() == ITEM_SAGA && it.sourceMethodName() == 'item saga input shares order id with cancellation fixture' }
        def cancelOrder23 = model.inputVariants().find { it.sagaFqn() == CANCEL_ORDER_SAGA && it.sourceMethodName() == 'item-derived order cancellation shares symbolic order key' }

        expect:
        itemOrder13 != null
        itemOrder23 != null
        cancelOrder23 != null

        and:
        def candidates = ConflictGraphBuilder.build([saga(ITEM_SAGA), saga(CANCEL_ORDER_SAGA)], strictConfig())
                .conflictCandidates()
        InputTupleJoiner.join(sagaOrder(), inputsBySaga(itemOrder23, cancelOrder23), candidates,
                model.aggregateKeyInputEvidence(), InputTupleSelection.Mode.STRICT)
                .counts().inputTuplesEmitted == 1
        InputTupleJoiner.join(sagaOrder(), inputsBySaga(itemOrder13, cancelOrder23), candidates,
                model.aggregateKeyInputEvidence(), InputTupleSelection.Mode.STRICT)
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
        def result = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), [], [],
                model.aggregateKeyInputEvidence(), config)
        def accounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(),
                model.inputVariants(), model.aggregateKeyInputEvidence(), config, result.workloadPlans().size())

        then:
        result.workloadPlans().size() == 7
        accounting.inputBoundScenarioSpace().allInputBound().total() == '7'
        accounting.inputBoundScenarioSpace().selectedByGenerator().total() == '7'
        accounting.inputBoundScenarioSpace().catalogWritten().total() == '7'
        accounting.groupedSagaSets()*.sagaSetSize().unique() == [1]
    }

    def 'dummyapp strict candidate without a positive capped input tuple remains inspectable but unselected'() {
        given:
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false,
                2,
                false,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING)

        when:
        def result = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), [], [],
                model.aggregateKeyInputEvidence(), config)
        def accounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(),
                model.inputVariants(), model.aggregateKeyInputEvidence(), config, result.workloadPlans().size())
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
        !strict.selectedByConfiguredGenerator()
        strict.strictInteractionSummary().connected()
        strict.scheduleCountPerTuple() == '3'
        new BigInteger(strict.compatibleInputTupleCount()) == InputTupleJoiner.join(
                [ITEM_SAGA, CANCEL_ORDER_SAGA], groupedInputs([ITEM_SAGA, CANCEL_ORDER_SAGA], config),
                ConflictGraphBuilder.build([saga(ITEM_SAGA), saga(CANCEL_ORDER_SAGA)], strictConfig()).conflictCandidates(),
                model.aggregateKeyInputEvidence(), InputTupleSelection.Mode.STRICT).tuples().size()
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
        def result = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), [], [],
                model.aggregateKeyInputEvidence(), config)
        def accounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(),
                model.inputVariants(), model.aggregateKeyInputEvidence(), config, result.workloadPlans().size())
        def opiAccounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(),
                model.inputVariants(), model.aggregateKeyInputEvidence(), opiConfig, 0)
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

    def 'count-only compact workload totals preserve Cartesian all and segment semantics'() {
        given: 'an unequal exact-key pair which remains in the all baseline'
        def itemOrder13 = model.inputVariants().find { it.sagaFqn() == ITEM_SAGA && it.logicalKeyBindings().orderId == '13' }
        def cancelOrder23 = model.inputVariants().find { it.sagaFqn() == CANCEL_ORDER_SAGA && it.sourceMethodName() == 'item-derived order cancellation shares symbolic order key' }
        def pairModel = new ScenarioModelAdapterResult(
                [saga(ITEM_SAGA), saga(CANCEL_ORDER_SAGA)],
                [itemOrder13, cancelOrder23], [], [], [:], [], model.dispatchesBySaga(), model.aggregateKeyInputEvidence())
        def contradictionConfig = fullDummyappConfig(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, false, 2, false,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL)
        def contradictionReport = new ScenarioSpaceAccountingCalculator().calculate(
                'dummyapp', pairModel.sagaDefinitions(), pairModel.inputVariants(),
                pairModel.aggregateKeyInputEvidence(), contradictionConfig, 0)
        def contradictionDirectory = Files.createTempDirectory('current-accounting-contradiction-')

        when:
        new StaticAnalysisArtifactWriter().write(pairModel, 'dummyapp', contradictionConfig, contradictionDirectory,
                '2026-09-01T00:00:00Z')
        def contradictionAccounting = new ObjectMapper().readTree(contradictionDirectory.resolve('accounting.json').toFile())

        then:
        InputTupleJoiner.join([ITEM_SAGA, CANCEL_ORDER_SAGA], [
                (ITEM_SAGA): [itemOrder13], (CANCEL_ORDER_SAGA): [cancelOrder23]
        ]).tuples().size() == 1
        contradictionReport.groupedSagaSets().find { it.sagaSetSize() == 2 }.compatibleInputTupleCount() == '1'
        contradictionAccounting.path('workloads').path('all').path('inputBoundTotal').bigIntegerValue() == BigInteger.ONE
        contradictionAccounting.path('workloads').path('selected').path('inputBoundTotal').bigIntegerValue() == BigInteger.ONE

        and: 'SEGMENT_COMPRESSED totals and row counts come from the same report as the writer'
        def segmentConfig = fullDummyappConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, false, 2, false,
                ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED)
        def segmentReport = new ScenarioSpaceAccountingCalculator().calculate(
                'dummyapp', model.sagaDefinitions(), model.inputVariants(), model.aggregateKeyInputEvidence(), segmentConfig, 0)
        def segmentDirectory = Files.createTempDirectory('current-accounting-segment-')
        new StaticAnalysisArtifactWriter().write(model, 'dummyapp', segmentConfig, segmentDirectory,
                '2026-09-01T00:00:00Z')
        def segmentAccounting = new ObjectMapper().readTree(segmentDirectory.resolve('accounting.json').toFile())
        segmentAccounting.path('workloads').path('all').path('inputBoundTotal').bigIntegerValue() ==
                new BigInteger(segmentReport.inputBoundScenarioSpace().allInputBound().total())
        segmentAccounting.path('workloads').path('selected').path('inputBoundTotal').bigIntegerValue() ==
                new BigInteger(segmentReport.inputBoundScenarioSpace().selectedByGenerator().total())
        segmentAccounting.path('workloads').path('all').path('total').intValue() == segmentReport.groupedSagaSets().size()
        segmentReport.groupedSagaSets().find { it.sagaSetSize() == 2 }.scheduleCountPerTuple() !=
                new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(), model.inputVariants(),
                        model.aggregateKeyInputEvidence(),
                        fullDummyappConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, false, 2, false,
                                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING), 0)
                        .groupedSagaSets().find { it.sagaSetSize() == 2 }.scheduleCountPerTuple()
    }

    def 'input facts omit aggregate-key evidence when only unrelated logical bindings exist'() {
        given:
        def input = model.inputVariants().find { it.sagaFqn() == ITEM_SAGA && it.logicalKeyBindings().orderId == '13' }
        def noEvidenceModel = new ScenarioModelAdapterResult(
                [saga(ITEM_SAGA)], [input], [], [], [:], [], model.dispatchesBySaga(), [])
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, true, 1, false,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL)
        def directory = Files.createTempDirectory('current-input-evidence-')

        when:
        new StaticAnalysisArtifactWriter().write(noEvidenceModel, 'dummyapp', config, directory,
                '2026-09-01T00:00:00Z')
        def inputFacts = new ScenarioCatalogPackageReader().readCurrentStatic(
                directory.resolve('scenario-catalog-manifest.json')).inputFacts()

        then:
        inputFacts.size() == 1
        !inputFacts.first().has('aggregateKeyEvidence')
    }

    def 'nested duplicate blocker reasons retain every top-level argument owner and count one affected input'() {
        given:
        def sagaFqn = 'example.BlockedSaga'
        def syntheticSaga = new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition(
                sagaFqn, [new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition(
                'blocked-step', "${sagaFqn}::step", 'step', 0, [], [], [])], [])
        def unresolved = pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode
                .builder('unresolved').executorReady(false).blockers(['UNRESOLVED_VALUE']).build()
        def nested = pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode
                .builder('property_access').executorReady(true).propertyName('id').receiver(unresolved).build()
        def arguments = [0, 1].collect { index ->
            new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument(
                    index, 'example.Dto', InputResolutionStatus.PARTIAL, true, [], "argument${index}", nested)
        }
        def recipe = new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe(
                pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe.SCHEMA_VERSION,
                null, false, ['UNMATERIALIZABLE_RECEIVER'], arguments)
        def input = new InputVariant(null, sagaFqn, 'example.BlockedSpec', 'case', 'binding',
                InputResolutionStatus.PARTIAL, SourceMode.SAGAS, SourceModeConfidence.TYPE_EVIDENCE,
                ['fixture'], 'new BlockedSaga()', 'fixture', [], [], [:], [], recipe)
        def blockedModel = new ScenarioModelAdapterResult([syntheticSaga], [input], [], [], [:], [], [:], [])
        def config = new ScenarioGeneratorConfig(false,
                ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY,
                true, 1, 10, 10, 10, false,
                ScenarioGeneratorConfig.InputPolicy.ALLOW_PARTIAL,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 1234L)
        def directory = Files.createTempDirectory('current-blocker-ownership-')

        when:
        new StaticAnalysisArtifactWriter().write(blockedModel, 'example', config, directory,
                '2026-09-01T00:00:00Z')
        def contents = new ScenarioCatalogPackageReader().readCurrentStatic(
                directory.resolve('scenario-catalog-manifest.json'))
        def fact = contents.inputFacts().first()

        then:
        fact.path('blockers')*.path('argument')*.asInt() == [0, 1]
        fact.path('blockers')*.path('reason')*.asText() ==
                ['unmaterializableReceiver', 'unmaterializableReceiver']
        fact.path('blockers')*.path('sourceExpression')*.asText() == ['argument0', 'argument1']
        contents.accounting().path('inputs').path('materializability')
                .path('affectedInputsByReason').path('unmaterializableReceiver').asInt() == 1
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
        def result = ScenarioGenerator.generate(model.sagaDefinitions(), model.inputVariants(), [], [],
                model.aggregateKeyInputEvidence(), config)
        def accounting = new ScenarioSpaceAccountingCalculator().calculate('dummyapp', model.sagaDefinitions(),
                model.inputVariants(), model.aggregateKeyInputEvidence(), config, 0)

        then:
        result.workloadPlans().isEmpty()
        accounting.inputBoundScenarioSpace().catalogWritten().total() == '0'
        accounting.inputBoundScenarioSpace().selectedByGenerator().total() == '0'
        accounting.inputBoundScenarioSpace().allInputBound().total() == '150'
        accounting.groupedSagaSets().size() == 4

        and:
        accounting.typeLevelCoverage().sagasWithoutAcceptedInputs().contains(DEPENDENCY_GRAPH_SAGA)
        accounting.typeLevelCoverage().sagasWithoutAcceptedInputs().contains(COMPENSATION_SAGA)
        accounting.typeLevelCoverage().broad().missingInputInteractionPairCount() > 0
        new BigInteger(accounting.typeLevelCoverage().broad().connectedSetCountsBySize()['3']) > BigInteger.ZERO
    }

    def 'dummyapp count-only writer emits the shared current static shape and central reader accepts it'() {
        given:
        def config = new ScenarioGeneratorConfig(false,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY,
                true, 3, 10_000, 100, 10_000, true,
                ScenarioGeneratorConfig.InputPolicy.ALLOW_UNRESOLVED,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 1234L)
        def directory = Files.createTempDirectory('dummyapp-current-static-')
        def mapper = new ObjectMapper()
        def fixture = mapper.readTree(resolveProjectPath('issues', '2026-08-30-current-only-verifier-artifacts',
                'examples', 'current-package.fixture.json').toFile())
        def expected = fixture.path('artifacts').path('accounting').path('count-only')
        def expectedManifest = fixture.path('manifests').path('count-only')
        def expectedSaga = fixture.path('artifacts').path('sagas').first()
        def expectedStep = expectedSaga.path('steps').first()
        def expectedInteraction = fixture.path('artifacts').path('interactions').first()
        def expectedMaterializableInput = fixture.path('artifacts').path('inputs').find { it.path('id').asText() == 'input-1' }
        def expectedBlockedInput = fixture.path('artifacts').path('inputs').find { it.path('id').asText() == 'input-2' }
        def expectedRejectedInput = fixture.path('artifacts').path('inputs').find { it.path('id').asText() == 'input-3' }

        when:
        def manifest = new StaticAnalysisArtifactWriter().write(model, 'dummyapp', config, directory,
                '2026-09-01T00:00:00Z')
        def contents = new ScenarioCatalogPackageReader().readCurrentStatic(directory.resolve('scenario-catalog-manifest.json'))
        def emittedManifest = mapper.readTree(directory.resolve('scenario-catalog-manifest.json').toFile())
        def accounting = contents.accounting()

        then:
        manifest.formatVersion() == 1
        manifest.files().keySet() == ['accounting', 'sagas', 'inputs', 'interactions'] as Set
        manifest.files().values()*.path as Set == ['accounting.json', 'sagas.jsonl', 'inputs.jsonl', 'interactions.jsonl'] as Set
        manifest.files().values().every { it.sha256() ==~ /[0-9a-f]{64}/ }
        emittedManifest.fieldNames().toList() == expectedManifest.fieldNames().toList()
        emittedManifest.path('files').fieldNames().toList() == expectedManifest.path('files').fieldNames().toList()
        expectedManifest.path('files').fields().every { entry ->
            emittedManifest.path('files').path(entry.key).fieldNames().toList() == entry.value.fieldNames().toList() &&
                    emittedManifest.path('files').path(entry.key).path('path').asText() == entry.value.path('path').asText()
        }
        Files.list(directory).collect { it.fileName.toString() }.sort() ==
                ['accounting.json', 'inputs.jsonl', 'interactions.jsonl', 'sagas.jsonl', 'scenario-catalog-manifest.json']
        assert accounting.fieldNames().toList() == expected.fieldNames().toList()
        assert accounting.path('configuration').fieldNames().toList() == expected.path('configuration').fieldNames().toList()
        assert accounting.path('sagas').fieldNames().toList() == expected.path('sagas').fieldNames().toList()
        assert accounting.path('inputs').fieldNames().toList() == expected.path('inputs').fieldNames().toList()
        assert accounting.path('interactions').fieldNames().toList() == expected.path('interactions').fieldNames().toList()
        assert accounting.path('events').fieldNames().toList() == expected.path('events').fieldNames().toList()
        assert accounting.path('workloads').fieldNames().toList() == expected.path('workloads').fieldNames().toList()
        accounting.path('configuration').path('catalogWriteMode').asText() == 'count-only'
        !accounting.has('schemaVersion')
        !accounting.toString().contains('workloadPlans')
        !accounting.toString().contains('faultVectors')
        accounting.path('workloads').path('written').path('total').canConvertToInt()
        contents.sagaFacts().first().fieldNames().toList() == expectedSaga.fieldNames().toList()
        contents.sagaFacts().first().path('steps').first().fieldNames().toList() == expectedStep.fieldNames().toList()
        contents.interactionFacts().first().fieldNames().toList() == expectedInteraction.fieldNames().toList()
        def emittedKeyedInteraction = contents.interactionFacts().find { interaction ->
            interaction.path('accesses').any { it.has('keyEvidence') }
        }
        emittedKeyedInteraction.path('accesses').find { it.has('keyEvidence') }.fieldNames().toList() ==
                expectedInteraction.path('accesses').first().fieldNames().toList()
        contents.inputFacts().find { it.path('accepted').asBoolean() && it.path('materializable').asBoolean() && it.has('aggregateKeyEvidence') }
                .fieldNames().toList() == expectedMaterializableInput.fieldNames().toList()
        contents.inputFacts().find { !it.path('accepted').asBoolean() }
                .fieldNames().toList() == expectedRejectedInput.fieldNames().toList()
        def emittedBlocked = contents.inputFacts().find { it.path('accepted').asBoolean() && !it.path('materializable').asBoolean() &&
                it.has('aggregateKeyEvidence') }
        emittedBlocked.fieldNames().toList().containsAll(
                expectedBlockedInput.fieldNames().findAll { !(it in ['usedBy', 'aggregateKeyEvidence']) })
        emittedBlocked.has('aggregateKeyEvidence')
        expectedBlockedInput.has('aggregateKeyEvidence')
        contents.sagaFacts().every { it.has('fqn') && it.has('dependencies') && it.has('steps') }
        contents.sagaFacts().every { !it.has('warnings') }
        contents.sagaFacts().every { saga -> saga.path('steps').every { it.has('id') && it.has('commandAccesses') && it.has('compensation') && it.has('eventRoutes') } }
        contents.sagaFacts().every { saga -> saga.path('steps').findAll { it.path('compensation').path('kind').asText() == 'explicit' }.every { !it.path('compensation').has('step') } }
        contents.sagaFacts().any { saga -> saga.path('steps').any { step -> step.path('commandAccesses').any { it.has('command') } } }
        contents.inputFacts().any { it.path('accepted').asBoolean() }
        contents.inputFacts().any { !it.path('accepted').asBoolean() && it.has('notAcceptedReason') }
        contents.inputFacts().any { it.path('accepted').asBoolean() && it.path('materializable').asBoolean() }
        contents.inputFacts().any { it.path('accepted').asBoolean() && !it.path('materializable').asBoolean() }
        contents.interactionFacts().every { it.path('accesses').size() == 2 && it.has('evidence') }
        contents.interactionFacts().any { it.path('evidence').asText() in ['symbolic', 'typeOnly'] }
        def runtimeNodes = contents.inputFacts().collectMany { fact ->
            fact.path('arguments').collectMany { argument -> recipeNodes(argument.path('value')) }
        }.findAll { it.path('kind').asText() == 'runtime' }
        assert !runtimeNodes.isEmpty()
        runtimeNodes.findAll { it.path('type').asText().endsWith('SagaUnitOfWorkService') || it.path('type').asText().endsWith('CommandGateway') }
                .every { it.path('scope').asText() == 'execution' }
        runtimeNodes.findAll { it.path('type').asText().endsWith('SagaUnitOfWork') &&
                !it.path('type').asText().endsWith('SagaUnitOfWorkService') }.every { it.path('scope').asText() == 'participant' }
        contents.inputFacts().findAll { it.has('aggregateKeyEvidence') }.every {
            it.path('aggregateKeyEvidence').path('kind').asText() == 'sameSource'
        }
        contents.inputFacts().findAll { it.path('accepted').asBoolean() && !it.path('materializable').asBoolean() }
                .collectMany { it.path('blockers').toList() }.every { blocker ->
                    !blocker.has('argument') || blocker.path('argument').isIntegralNumber() && blocker.path('argument').asInt() >= 0
                }
    }

    def 'the shared M0 fixture round trips exact current package bytes through the central reader'() {
        given:
        def mapper = new ObjectMapper()
        def fixture = mapper.readTree(resolveProjectPath('issues', '2026-08-30-current-only-verifier-artifacts',
                'examples', 'current-package.fixture.json').toFile())
        def manifestNode = fixture.path('manifests').path('count-only')
        def artifacts = fixture.path('artifacts')
        def directory = Files.createTempDirectory('current-fixture-exact-')
        writeFixtureArtifact(directory.resolve('accounting.json'), artifacts.path('accounting').path('count-only'), false, mapper)
        writeFixtureArtifact(directory.resolve('sagas.jsonl'), artifacts.path('sagas'), true, mapper)
        writeFixtureArtifact(directory.resolve('inputs.jsonl'), artifacts.path('inputs'), true, mapper)
        writeFixtureArtifact(directory.resolve('interactions.jsonl'), artifacts.path('interactions'), true, mapper)
        mapper.writeValue(directory.resolve('scenario-catalog-manifest.json').toFile(), manifestNode)

        when:
        def contents = new ScenarioCatalogPackageReader().readCurrentStatic(directory.resolve('scenario-catalog-manifest.json'))

        then:
        contents.manifest().formatVersion() == manifestNode.path('formatVersion').asInt()
        contents.manifest().files().collectEntries { key, value -> [(key): [value.path(), value.sha256()]] } ==
                manifestNode.path('files').fields().collectEntries { [(it.key): [it.value.path('path').asText(), it.value.path('sha256').asText()]] }
        contents.accounting() == artifacts.path('accounting').path('count-only')
        contents.sagaFacts() == artifacts.path('sagas').toList()
        contents.inputFacts() == artifacts.path('inputs').toList()
        contents.interactionFacts() == artifacts.path('interactions').toList()
    }

    def 'routes use full trigger keys deduplicate semantic chains and keep stable local ids'() {
        given:
        def sagaFqn = 'example.RepeatedNameSaga'
        def firstStep = new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition(
                'step-a', "${sagaFqn}::first", 'sameName', 0, [], [], [])
        def triggerStep = new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition(
                'step-b', "${sagaFqn}::second", 'sameName', 1, [], [], [])
        def repeatedSaga = new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition(
                sagaFqn, [firstStep, triggerStep], [])
        def expectedRoute = new ObjectMapper().readTree(resolveProjectPath('issues', '2026-08-30-current-only-verifier-artifacts',
                'examples', 'current-package.fixture.json').toFile()).path('artifacts').path('sagas').first()
                .path('steps').first().path('eventRoutes').first()
        def site = new EventEmissionSite('dummy-emission', 'dummy.Service', 'emit()', 0, 'dummy.Event', ['fixture'])
        def routeA = new EventConsequenceDefinition(sagaFqn, triggerStep.stepKey(), site,
                'dummy.Handling', 'handleA', 'dummy.HandlerA', 'dummy.Processing', 'processA',
                'dummy.Facade', 'invokeA', sagaFqn, EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER, [])
        def duplicateRouteA = new EventConsequenceDefinition(sagaFqn, triggerStep.stepKey(), site,
                'dummy.Handling', 'handleA', 'dummy.HandlerA', 'dummy.Processing', 'processA',
                'dummy.Facade', 'invokeA', sagaFqn, EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER, ['duplicate source row'])
        def routeB = new EventConsequenceDefinition(sagaFqn, triggerStep.stepKey(), site,
                'dummy.Handling', 'handleB', 'dummy.HandlerB', 'dummy.Processing', 'processB',
                'dummy.Facade', 'invokeB', sagaFqn, EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER, [])
        def eventModel = new ScenarioModelAdapterResult([repeatedSaga], [],
                [routeB, duplicateRouteA, routeA], [], [:], [], [:], [])
        def config = new ScenarioGeneratorConfig(false,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY,
                true, 3, 10_000, 100, 10_000, true,
                ScenarioGeneratorConfig.InputPolicy.ALLOW_UNRESOLVED,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 1234L)
        def directory = Files.createTempDirectory('current-routes-')

        when:
        new StaticAnalysisArtifactWriter().write(eventModel, 'dummyapp', config, directory, '2026-09-01T00:00:00Z')
        def manifestPath = directory.resolve('scenario-catalog-manifest.json')
        def accepted = new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)
        def emittedSaga = accepted.sagaFacts().find { it.path('fqn').asText() == sagaFqn }
        def firstRoutes = emittedSaga.path('steps').find { it.path('id').asText() == 'sameName#0' }.path('eventRoutes')
        def emitted = emittedSaga.path('steps').find { it.path('id').asText() == 'sameName#1' }.path('eventRoutes')

        then:
        firstRoutes.isEmpty()
        emitted.size() == 2
        emitted*.path('id')*.asText() == ['sameName#1/event#0', 'sameName#1/event#0-route#1']
        emitted.first().fieldNames().toList() == expectedRoute.fieldNames().toList()

        when:
        def mapper = new ObjectMapper()
        def sagaPath = directory.resolve('sagas.jsonl')
        def lines = Files.readAllLines(sagaPath)
        def first = mapper.readTree(lines.find { it.contains(sagaFqn) })
        def routeArray = first.path('steps').find { it.path('id').asText() == 'sameName#1' }.path('eventRoutes')
        routeArray[1].put('id', routeArray[0].path('id').asText())
        def replaced = lines.collect { it.contains(sagaFqn) ? mapper.writeValueAsString(first) : it }
        Files.writeString(sagaPath, replaced.join('\n') + '\n')
        def manifest = mapper.readTree(manifestPath.toFile())
        refreshHash(manifest, manifestPath, 'sagas', sagaPath)
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def duplicateRoute = thrown(IllegalArgumentException)
        duplicateRoute.message.contains('duplicate event route id')
    }

    def 'dummyapp current reader rejects checksum mismatch and escaping artifact paths'() {
        given:
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, true, 2, true,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL)
        def directory = Files.createTempDirectory('dummyapp-current-reader-')
        new StaticAnalysisArtifactWriter().write(model, 'dummyapp', config, directory, '2026-09-01T00:00:00Z')
        def manifestPath = directory.resolve('scenario-catalog-manifest.json')
        def mapper = new ObjectMapper()

        when: 'a linked artifact changes without a manifest hash update'
        Files.writeString(directory.resolve('accounting.json'), '{}')
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def hashFailure = thrown(IllegalArgumentException)
        hashFailure.message.contains('hash mismatch')

        when: 'a manifest path escapes its package directory'
        def escapedManifest = mapper.readTree(manifestPath.toFile())
        escapedManifest.withObject('/files/accounting').put('path', '../accounting.json')
        mapper.writeValue(manifestPath.toFile(), escapedManifest)
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def pathFailure = thrown(IllegalArgumentException)
        pathFailure.message.contains('escaping path')
    }

    def 'dummyapp current reader rejects duplicate ids and dangling Saga step references'() {
        given:
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, true, 3, true,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING)
        def directory = Files.createTempDirectory('dummyapp-current-reader-refs-')
        new StaticAnalysisArtifactWriter().write(model, 'dummyapp', config, directory, '2026-09-01T00:00:00Z')
        def reader = new ScenarioCatalogPackageReader()
        def mapper = new ObjectMapper()
        def manifestPath = directory.resolve('scenario-catalog-manifest.json')
        def manifest = mapper.readTree(manifestPath.toFile())
        def sagaPath = directory.resolve('sagas.jsonl')
        def originalSagas = Files.readAllLines(sagaPath)

        when: 'two Saga records have one id'
        def duplicate = mapper.readTree(originalSagas[0])
        Files.writeString(sagaPath, ([mapper.writeValueAsString(duplicate), mapper.writeValueAsString(duplicate)] + originalSagas.drop(2)).join('\n') + '\n')
        refreshHash(manifest, manifestPath, 'sagas', sagaPath)
        reader.readCurrentStatic(manifestPath)

        then:
        def duplicateFailure = thrown(IllegalArgumentException)
        duplicateFailure.message.contains('duplicate Saga id')

        when: 'an interaction points to a missing local Saga step'
        Files.write(sagaPath, originalSagas)
        refreshHash(manifest, manifestPath, 'sagas', sagaPath)
        def interactionPath = directory.resolve('interactions.jsonl')
        def interactions = Files.readAllLines(interactionPath)
        assert !interactions.isEmpty()
        def brokenInteraction = mapper.readTree(interactions[0])
        brokenInteraction.path('accesses').get(0).put('step', 'missingStep#0')
        Files.writeString(interactionPath, ([mapper.writeValueAsString(brokenInteraction)] + interactions.drop(1)).join('\n') + '\n')
        refreshHash(manifest, manifestPath, 'interactions', interactionPath)
        reader.readCurrentStatic(manifestPath)

        then:
        def danglingFailure = thrown(IllegalArgumentException)
        danglingFailure.message.contains('references missing Saga/step')
    }

    def 'dummyapp current reader rejects missing required input fields'() {
        given:
        def config = accountingConfig(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, true, 2, true,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL)
        def directory = Files.createTempDirectory('dummyapp-current-reader-required-')
        new StaticAnalysisArtifactWriter().write(model, 'dummyapp', config, directory, '2026-09-01T00:00:00Z')
        def manifestPath = directory.resolve('scenario-catalog-manifest.json')
        def inputPath = directory.resolve('inputs.jsonl')
        def mapper = new ObjectMapper()
        def input = mapper.readTree(Files.readAllLines(inputPath)[0])
        input.remove('accepted')
        Files.writeString(inputPath, mapper.writeValueAsString(input) + '\n')
        def manifest = mapper.readTree(manifestPath.toFile())
        refreshHash(manifest, manifestPath, 'inputs', inputPath)

        when:
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('missing required field accepted')
    }

    def 'dummyapp current reader validates rejected reasons and blocked-input conditions semantically'() {
        given:
        def config = new ScenarioGeneratorConfig(false,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY,
                true, 3, 10_000, 100, 10_000, true,
                ScenarioGeneratorConfig.InputPolicy.ALLOW_UNRESOLVED,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 1234L)
        def directory = Files.createTempDirectory('dummyapp-current-reader-conditional-')
        new StaticAnalysisArtifactWriter().write(model, 'dummyapp', config, directory, '2026-09-01T00:00:00Z')
        def manifestPath = directory.resolve('scenario-catalog-manifest.json')
        def inputPath = directory.resolve('inputs.jsonl')
        def mapper = new ObjectMapper()
        def original = Files.readAllLines(inputPath)
        def rejectedIndex = original.findIndexOf { mapper.readTree(it).path('accepted').asBoolean() == false }
        def blockedIndex = original.findIndexOf { def n = mapper.readTree(it); n.path('accepted').asBoolean() && !n.path('materializable').asBoolean() }
        def materializableIndex = original.findIndexOf { def n = mapper.readTree(it); n.path('accepted').asBoolean() && n.path('materializable').asBoolean() }
        assert rejectedIndex >= 0
        assert blockedIndex >= 0
        assert materializableIndex >= 0

        when: 'an accepted input carries a rejection reason'
        def accepted = mapper.readTree(original[materializableIndex]); accepted.put('notAcceptedReason', 'inputPolicyRejected')
        rewriteInput(inputPath, original, materializableIndex, accepted, manifestPath, mapper)
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def acceptedReason = thrown(IllegalArgumentException)
        acceptedReason.message.contains('must not have notAcceptedReason')

        when: 'a materializable input carries blockers'
        def ready = mapper.readTree(original[materializableIndex])
        ready.putArray('blockers').addObject().put('reason', 'impossible')
        rewriteInput(inputPath, original, materializableIndex, ready, manifestPath, mapper)
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def readyBlocker = thrown(IllegalArgumentException)
        readyBlocker.message.contains('must not have blockers')

        when: 'a rejected input has a null reason'
        def rejected = mapper.readTree(original[rejectedIndex]); rejected.putNull('notAcceptedReason')
        rewriteInput(inputPath, original, rejectedIndex, rejected, manifestPath, mapper)
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def nullReason = thrown(IllegalArgumentException)
        nullReason.message.contains('notAcceptedReason')

        when: 'a rejected input has a non-text reason'
        rejected = mapper.readTree(original[rejectedIndex]); rejected.put('notAcceptedReason', 7)
        rewriteInput(inputPath, original, rejectedIndex, rejected, manifestPath, mapper)
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def wrongReason = thrown(IllegalArgumentException)
        wrongReason.message.contains('notAcceptedReason')

        when: 'a blocked input has an empty blockers array'
        def blocked = mapper.readTree(original[blockedIndex]); blocked.putArray('blockers')
        rewriteInput(inputPath, original, blockedIndex, blocked, manifestPath, mapper)
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def emptyBlockers = thrown(IllegalArgumentException)
        emptyBlockers.message.contains('non-empty blockers')

        when: 'a blocked input has a non-array blockers value'
        blocked = mapper.readTree(original[blockedIndex]); blocked.put('blockers', 'runtimeProviderUnavailable')
        rewriteInput(inputPath, original, blockedIndex, blocked, manifestPath, mapper)
        new ScenarioCatalogPackageReader().readCurrentStatic(manifestPath)

        then:
        def wrongBlockers = thrown(IllegalArgumentException)
        wrongBlockers.message.contains('blockers must be an array')
    }

    private static void refreshHash(def manifest, Path manifestPath, String role, Path artifactPath) {
        manifest.withObject("/files/${role}").put('sha256', sha256(artifactPath))
        new ObjectMapper().writeValue(manifestPath.toFile(), manifest)
    }

    private static String sha256(Path path) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        digest.digest(Files.readAllBytes(path)).encodeHex().toString()
    }

    private static void writeFixtureArtifact(Path path, def value, boolean jsonLines, ObjectMapper mapper) {
        if (jsonLines) {
            Files.writeString(path, value.collect { mapper.writeValueAsString(it) }.join('\n') + '\n')
        } else {
            mapper.writeValue(path.toFile(), value)
        }
    }

    private static List recipeNodes(def node) {
        if (node == null || node.isMissingNode() || node.isNull()) return []
        def values = []
        if (node.isObject()) {
            if (node.has('kind')) values << node
            def fields = node.fields()
            while (fields.hasNext()) {
                def entry = fields.next()
                values.addAll(recipeNodes(entry.value))
            }
        } else if (node.isArray()) {
            node.each { value -> values.addAll(recipeNodes(value)) }
        }
        values
    }

    private static void rewriteInput(Path inputPath, List<String> original, int index, def replacement,
                                     Path manifestPath, ObjectMapper mapper) {
        def lines = []
        original.eachWithIndex { line, current -> lines << (current == index ? mapper.writeValueAsString(replacement) : line) }
        Files.writeString(inputPath, lines.join('\n') + '\n')
        def manifest = mapper.readTree(manifestPath.toFile())
        refreshHash(manifest, manifestPath, 'inputs', inputPath)
    }

    private void writePackage(def eager, java.nio.file.Path directory, ScenarioGeneratorConfig config) {
        new ExecutableArtifactWriter().write(model, 'dummyapp', eager,
                directory.resolve('scenario-catalog-manifest.json'),
                directory.resolve(StaticAnalysisArtifactWriter.DEFAULT_ACCOUNTING_FILE),
                directory.resolve(StaticAnalysisArtifactWriter.DEFAULT_SAGA_FACT_FILE),
                directory.resolve(StaticAnalysisArtifactWriter.DEFAULT_INPUT_FACT_FILE),
                directory.resolve(StaticAnalysisArtifactWriter.DEFAULT_INTERACTION_FACT_FILE),
                directory.resolve(ExecutableArtifactWriter.DEFAULT_SETUP_FILE),
                directory.resolve(ExecutableArtifactWriter.DEFAULT_WORKLOAD_FILE),
                directory.resolve(ExecutableArtifactWriter.DEFAULT_FAULT_FILE),
                directory.resolve(ExecutableArtifactWriter.DEFAULT_REQUEST_FILE),
                '2026-07-20T00:00:00Z')
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
