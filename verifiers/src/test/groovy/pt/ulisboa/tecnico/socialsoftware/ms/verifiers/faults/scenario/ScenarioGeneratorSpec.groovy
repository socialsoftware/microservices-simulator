package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig.InputPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig.ScheduleStrategy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationEvidenceClass
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequenceDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventEmissionSite
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputOwner
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadGenerationResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScheduleEnumerator.SagaScheduleInput
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeRejectionReason
import spock.lang.Specification

class ScenarioGeneratorSpec extends Specification {

    def 'single saga scenarios are emitted before multi saga scenarios'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'order-1'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'order-1'))
        def inputs = [
                input('input-a', 'com.example.A', 'order-1'),
                input('input-b', 'com.example.B', 'order-1')
        ]

        when:
        def first = ScenarioGenerator.generate([sagaA, sagaB], inputs, config(maxSagaSetSize: 2))
        def second = ScenarioGenerator.generate([sagaB, sagaA], [inputs[1], inputs[0]], config(maxSagaSetSize: 2))

        then:
        first.workloadPlans()*.kind() == [ScenarioKind.SINGLE_SAGA, ScenarioKind.SINGLE_SAGA, ScenarioKind.MULTI_SAGA]
        first.workloadPlans()*.deterministicId() == second.workloadPlans()*.deterministicId()
    }

    def 'source mode does not participate in input variant deterministic identity'() {
        given:
        def saga = saga('com.example.A', step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'order-1'))
        def sagaInput = inputWithSourceMode('com.example.A', SourceMode.SAGAS)
        def unknownInput = inputWithSourceMode('com.example.A', SourceMode.UNKNOWN)

        when:
        def result = ScenarioGenerator.generate([saga], [sagaInput, unknownInput], config(maxSagaSetSize: 1))

        then:
        result.workloadPlans().size() == 1
        result.counts().inputVariantsDeduplicated == 1
    }

    def 'recipe fingerprints and logical keys participate in input identity while owners remain metadata'() {
        given:
        def saga = saga('com.example.A', step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'order-1'))
        def ownerOne = new InputOwner('com.example.OrderSpec', 'feature one')
        def ownerTwo = new InputOwner('com.example.OrderSpec', 'feature two')
        def recipeOne = recipeForLiteral('1')
        def recipeTwo = recipeForLiteral('2')
        def sameRecipeOwnerOne = inputWithRecipe('com.example.A', recipeOne, [orderId: 'order-1'], [ownerOne])
        def sameRecipeOwnerTwo = inputWithRecipe('com.example.A', recipeOne, [orderId: 'order-1'], [ownerTwo])
        def differentRecipe = inputWithRecipe('com.example.A', recipeTwo, [orderId: 'order-1'], [ownerOne])
        def differentLogicalKey = inputWithRecipe('com.example.A', recipeOne, [orderId: 'order-2'], [ownerOne])

        when:
        def result = ScenarioGenerator.generate([saga], [sameRecipeOwnerOne, sameRecipeOwnerTwo, differentRecipe, differentLogicalKey],
                config(maxInputVariantsPerSaga: 10))
        def accepted = result.workloadPlans()*.acceptedInputs().flatten()
        def mergedSameRecipe = accepted.find { it.inputRecipe().recipeFingerprint() == recipeOne.recipeFingerprint() && it.logicalKeyBindings().orderId == 'order-1' }

        then:
        accepted.size() == 3
        accepted*.deterministicId().toSet().size() == 3
        result.counts().inputVariantsDeduplicated == 1
        mergedSameRecipe.owners().toSet() == [ownerOne, ownerTwo] as Set
    }

    def 'source-mode policy accepts sagas rejects tcc and mixed and accepts unknown with warning'() {
        given:
        def saga = saga('com.example.A', step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'order-1'))
        def sagasInput = inputWithSourceModeAndSource('com.example.A', SourceMode.SAGAS, 'sagas-source')
        def tccInput = inputWithSourceModeAndSource('com.example.A', SourceMode.TCC, 'tcc-source')
        def mixedInput = inputWithSourceModeAndSource('com.example.A', SourceMode.MIXED, 'mixed-source')
        def unknownInput = inputWithSourceModeAndSource('com.example.A', SourceMode.UNKNOWN, 'unknown-source')

        when:
        def result = ScenarioGenerator.generate([saga], [sagasInput, tccInput, mixedInput, unknownInput], config(maxSagaSetSize: 1))

        then:
        result.workloadPlans().size() == 2
        result.workloadPlans()*.acceptedInputs().flatten()*.stableSourceText().toSet() == ['sagas-source', 'unknown-source'] as Set
        result.rejectedInputVariants()*.inputVariant()*.sourceMode() == [SourceMode.TCC, SourceMode.MIXED]
        result.rejectedInputVariants()*.rejectionReason() == [
                SourceModeRejectionReason.SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG,
                SourceModeRejectionReason.SOURCE_MODE_MIXED_REJECTED_FOR_SAGA_CATALOG
        ]
        result.counts().inputVariantsRejectedBySourceMode == 2
        result.warnings().any { it.contains('Source mode could not be proven') }
    }

    def 'deduplicated known source mode does not inherit unknown warning'() {
        given:
        def saga = saga('com.example.A', step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'order-1'))
        def unknownInput = inputWithSourceMode('com.example.A', SourceMode.UNKNOWN)
        def sagasInput = inputWithSourceMode('com.example.A', SourceMode.SAGAS)

        when:
        def result = ScenarioGenerator.generate([saga], [unknownInput, sagasInput], config(maxSagaSetSize: 1))

        then:
        result.workloadPlans().size() == 1
        def accepted = result.workloadPlans()[0].acceptedInputs()[0]
        accepted.sourceMode() == SourceMode.SAGAS
        !accepted.warnings().any { it.contains('Source mode could not be proven') }
        !result.warnings().any { it.contains('Source mode could not be proven') }
    }

    def 'source-mode rejection is collected before deterministic-id deduplication'() {
        given:
        def saga = saga('com.example.A', step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'order-1'))
        def sagasInput = inputWithSourceMode('com.example.A', SourceMode.SAGAS)
        def tccInput = inputWithSourceMode('com.example.A', SourceMode.TCC)

        when:
        def result = ScenarioGenerator.generate([saga], [sagasInput, tccInput], config(maxSagaSetSize: 1))

        then:
        result.workloadPlans().size() == 1
        result.rejectedInputVariants().size() == 1
        result.rejectedInputVariants()[0].rejectionReason() == SourceModeRejectionReason.SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG
        result.counts().inputVariantsDeduplicated == 0
    }

    def 'read read shared aggregate does not create multi saga conflict'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.READ, 'shared'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared'))

        when:
        def result = ScenarioGenerator.generate([sagaA, sagaB], [
                input('input-a', 'com.example.A', 'shared'),
                input('input-b', 'com.example.B', 'shared')
        ], config(maxSagaSetSize: 2))

        then:
        result.workloadPlans()*.kind().every { it == ScenarioKind.SINGLE_SAGA }
        result.workloadPlans().size() == 2
    }

    def 'brute force write plans emits all input-bound saga sets including unrelated pairs'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'order-a'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.WRITE, 'order-b'))
        def inputs = [
                input('input-a', 'com.example.A', 'order-a'),
                input('input-b', 'com.example.B', 'order-a')
        ]

        when:
        def result = ScenarioGenerator.generate([sagaA, sagaB], inputs, config(
                generationStrategy: ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                includeSingles: false,
                maxSagaSetSize: 2))

        then:
        result.workloadPlans().size() == 1
        result.workloadPlans()[0].kind() == ScenarioKind.MULTI_SAGA
        result.workloadPlans()[0].participants()*.sagaFqn().toSet() == ['com.example.A', 'com.example.B'] as Set
        result.workloadPlans()[0].conflictEvidence().isEmpty()
    }

    def 'interaction pruned write plans emits only selected graph-connected plans'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared'))
        def sagaC = saga('com.example.C',
                step('com.example.C', 'step-1', 0, AccessMode.WRITE, 'unrelated'))
        def inputs = [
                input('input-a', 'com.example.A', 'shared'),
                input('input-b', 'com.example.B', 'shared'),
                input('input-c', 'com.example.C', 'unrelated')
        ]

        when:
        def result = ScenarioGenerator.generate([sagaA, sagaB, sagaC], inputs, config(
                generationStrategy: ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                includeSingles: false,
                maxSagaSetSize: 2))

        then:
        result.workloadPlans().size() == 1
        result.workloadPlans()[0].participants()*.sagaFqn().toSet() == ['com.example.A', 'com.example.B'] as Set
        result.workloadPlans()[0].conflictEvidence().size() == 1
    }

    def 'broad pruning retains an event-only pair without attributing the consumer key to the producer input'() {
        given:
        def producer = saga('dummyapp.Producer',
                aggregateStep('dummyapp.Producer', 'emit', 0, 'Producer', AccessMode.WRITE,
                        'producerId', FootprintConfidence.SYMBOLIC))
        def consumer = saga('dummyapp.EventConsumer',
                aggregateStep('dummyapp.EventConsumer', 'update', 0, 'Order', AccessMode.WRITE,
                        'orderId', FootprintConfidence.SYMBOLIC))
        def reader = saga('dummyapp.Reader',
                aggregateStep('dummyapp.Reader', 'read', 0, 'Order', AccessMode.READ,
                        'orderId', FootprintConfidence.SYMBOLIC))
        def unrelated = saga('dummyapp.Unrelated',
                aggregateStep('dummyapp.Unrelated', 'read', 0, 'Other', AccessMode.READ,
                        'otherId', FootprintConfidence.SYMBOLIC))
        def route = eventRoute(producer, consumer)
        def inputs = [
                input('producer-input', producer.sagaFqn(), [producerId: '10']),
                input('reader-input', reader.sagaFqn(), [orderId: '20']),
                input('unrelated-input', unrelated.sagaFqn(), [otherId: '30'])
        ]

        when:
        def broad = ScenarioGenerator.generate([producer, consumer, reader, unrelated], inputs, [route], config(
                includeSingles: false, maxSagaSetSize: 2, allowTypeOnlyFallback: true))
        def strict = ScenarioGenerator.generate([producer, consumer, reader, unrelated], inputs, [route], config(
                includeSingles: false, maxSagaSetSize: 2, allowTypeOnlyFallback: false))

        then:
        broad.workloadPlans().any { it.participants()*.sagaFqn().toSet() ==
                [producer.sagaFqn(), reader.sagaFqn()] as Set }
        broad.workloadPlans().findAll { it.participants()*.sagaFqn().contains(unrelated.sagaFqn()) }.isEmpty()
        broad.workloadPlans().findAll { it.participants()*.sagaFqn().toSet() ==
                [producer.sagaFqn(), reader.sagaFqn()] as Set }.any { it.eventConsequences() }
        broad.workloadPlans().find { it.kind() == ScenarioKind.MULTI_SAGA }.conflictEvidence().every {
            it.warnings().any { warning -> warning.contains('receiver identity is not attributed') }
        }
        strict.workloadPlans().isEmpty()
    }

    def 'event selection accepts proven exact identity but rejects contradictory identity and selected downstream routes'() {
        given:
        def producer = saga('dummyapp.Producer',
                aggregateStep('dummyapp.Producer', 'emit', 0, 'Producer', AccessMode.WRITE, 'producer'))
        def consumer = saga('dummyapp.EventConsumer',
                aggregateStep('dummyapp.EventConsumer', 'update', 0, 'Order', AccessMode.WRITE, consumerKey))
        def reader = saga('dummyapp.Reader',
                aggregateStep('dummyapp.Reader', 'read', 0, 'Order', AccessMode.READ, readerKey))
        def route = eventRoute(producer, consumer)
        def inputs = [input('producer-input', producer.sagaFqn(), [:]), input('reader-input', reader.sagaFqn(), [:]),
                      input('consumer-input', consumer.sagaFqn(), [:])]

        when:
        def generated = ScenarioGenerator.generate([producer, consumer, reader], inputs, [route], config(
                includeSingles: false, maxSagaSetSize: 2, allowTypeOnlyFallback: fallback))
        def producerReader = generated.workloadPlans().findAll { it.participants()*.sagaFqn().toSet() ==
                [producer.sagaFqn(), reader.sagaFqn()] as Set }
        def producerConsumer = generated.workloadPlans().findAll { it.participants()*.sagaFqn().toSet() ==
                [producer.sagaFqn(), consumer.sagaFqn()] as Set }

        then:
        !producerReader.isEmpty() == retained
        producerConsumer.isEmpty()

        where:
        consumerKey | readerKey | fallback || retained
        'same'      | 'same'    | false    || true
        'left'      | 'right'   | true     || false
    }

    def 'indirect surfaces do not change base identities when forward evidence already retains the pair'() {
        given:
        def producer = saga('dummyapp.Producer',
                aggregateStep('dummyapp.Producer', 'emit', 0, 'Order', AccessMode.WRITE, 'shared'))
        def consumer = saga('dummyapp.EventConsumer',
                aggregateStep('dummyapp.EventConsumer', 'update', 0, 'Order', AccessMode.WRITE, 'shared'))
        def reader = saga('dummyapp.Reader',
                aggregateStep('dummyapp.Reader', 'read', 0, 'Order', AccessMode.READ, 'shared'))
        def inputs = [input('producer-input', producer.sagaFqn(), [:]),
                      input('reader-input', reader.sagaFqn(), [:])]
        def directConfig = config(includeSingles: false, maxSagaSetSize: 2,
                scheduleStrategy: ScheduleStrategy.SEGMENT_COMPRESSED)

        when:
        def withoutRoute = ScenarioGenerator.generate([producer, consumer, reader], inputs, [], directConfig)
        def withRoute = ScenarioGenerator.generate([producer, consumer, reader], inputs,
                [eventRoute(producer, consumer)], directConfig)

        then:
        withRoute.workloadPlans().findAll { !it.eventConsequences() }*.deterministicId() ==
                withoutRoute.workloadPlans()*.deterministicId()
        withRoute.workloadPlans().findAll { !it.eventConsequences() }.every { plan ->
            plan.conflictEvidence().every { !it.warnings().any { warning -> warning.contains('mediated selection edge') } }
        }
    }

    def 'recovery-only access retains its pair and supplies segment-compression anchors'() {
        given:
        def recovering = saga('dummyapp.Recovering',
                aggregateStep('dummyapp.Recovering', 'prepare', 0, 'Producer', AccessMode.READ, 'producer'),
                recoveryStep('dummyapp.Recovering', 'checkpoint', 1, 'Producer', 'Order', 'shared'))
        def reader = saga('dummyapp.Reader',
                aggregateStep('dummyapp.Reader', 'read', 0, 'Order', AccessMode.READ, 'shared'))
        def inputs = [input('reader-input', reader.sagaFqn(), [:]),
                      input('recovering-input', recovering.sagaFqn(), [:])]

        when:
        def generated = ScenarioGenerator.generate([recovering, reader], inputs, config(
                includeSingles: false, maxSagaSetSize: 2,
                scheduleStrategy: ScheduleStrategy.SEGMENT_COMPRESSED))

        then:
        generated.workloadPlans().size() == 2
        generated.workloadPlans().every { plan ->
            plan.conflictEvidence().any { it.warnings().contains('recovery-mediated selection edge') }
        }
        generated.workloadPlans().collect { it.forwardSchedule()*.stepId() }.toSet() == [
                ['dummyapp.Recovering::prepare', 'dummyapp.Recovering::checkpoint', 'dummyapp.Reader::read'],
                ['dummyapp.Reader::read', 'dummyapp.Recovering::prepare', 'dummyapp.Recovering::checkpoint']
        ] as Set
    }

    def 'all recovery conflict steps anchor compression while workload evidence stays minimal'() {
        given:
        def recovering = saga('dummyapp.Recovering',
                recoveryStep('dummyapp.Recovering', 'first', 0, 'Producer', 'Order', 'shared'),
                recoveryStep('dummyapp.Recovering', 'second', 1, 'Producer', 'Order', 'shared'))
        def reader = saga('dummyapp.Reader',
                aggregateStep('dummyapp.Reader', 'first', 0, 'Order', AccessMode.READ, 'shared'),
                aggregateStep('dummyapp.Reader', 'second', 1, 'Order', AccessMode.READ, 'shared'))
        def inputs = [input('reader-input', reader.sagaFqn(), [:]),
                      input('recovering-input', recovering.sagaFqn(), [:])]
        def generationConfig = config(includeSingles: false, maxSagaSetSize: 2,
                scheduleStrategy: ScheduleStrategy.SEGMENT_COMPRESSED)
        def graph = ConflictGraphBuilder.buildSelectionGraph([recovering, reader], [], generationConfig)

        when:
        def evidence = InputTupleSelection.selectedCandidates(
                [recovering.sagaFqn(), reader.sagaFqn()], inputs, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK)
        def anchors = InputTupleSelection.selectedAnchorCandidates(
                [recovering.sagaFqn(), reader.sagaFqn()], inputs, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK)
        def generated = ScenarioGenerator.generate([recovering, reader], inputs, generationConfig)

        then:
        evidence.size() == 1
        anchors.size() == 4
        generated.workloadPlans().size() == 6
        generated.workloadPlans().every { it.conflictEvidence().size() == 1 }
    }

    def 'all event-mediated participant steps anchor compression'() {
        given:
        def producer = saga('dummyapp.Producer',
                aggregateStep('dummyapp.Producer', 'emit', 0, 'Producer', AccessMode.WRITE, 'producer'))
        def consumer = saga('dummyapp.EventConsumer',
                aggregateStep('dummyapp.EventConsumer', 'first', 0, 'Order', AccessMode.WRITE, 'shared'),
                aggregateStep('dummyapp.EventConsumer', 'second', 1, 'Order', AccessMode.WRITE, 'shared'))
        def reader = saga('dummyapp.Reader',
                aggregateStep('dummyapp.Reader', 'first', 0, 'Order', AccessMode.READ, 'shared'),
                aggregateStep('dummyapp.Reader', 'second', 1, 'Order', AccessMode.READ, 'shared'))
        def inputs = [input('producer-input', producer.sagaFqn(), [:]),
                      input('reader-input', reader.sagaFqn(), [:])]
        def generationConfig = config(includeSingles: false, maxSagaSetSize: 2,
                scheduleStrategy: ScheduleStrategy.SEGMENT_COMPRESSED)
        def graph = ConflictGraphBuilder.buildSelectionGraph(
                [producer, consumer, reader], [eventRoute(producer, consumer)], generationConfig)

        when:
        def evidence = InputTupleSelection.selectedCandidates(
                [producer.sagaFqn(), reader.sagaFqn()], inputs, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK)
        def anchors = InputTupleSelection.selectedAnchorCandidates(
                [producer.sagaFqn(), reader.sagaFqn()], inputs, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK)
        def generated = ScenarioGenerator.generate([producer, consumer, reader], inputs,
                [eventRoute(producer, consumer)], generationConfig)

        then:
        evidence.size() == 1
        anchors.size() == 2
        generated.workloadPlans().findAll { !it.eventConsequences() }.size() == 3
        generated.workloadPlans().findAll { !it.eventConsequences() }.collect {
            it.forwardSchedule()*.stepId()
        }.toSet().size() == 3
    }

    def 'indirect anchors remain active when direct evidence already connects the pair'() {
        given:
        def mixed = saga('dummyapp.Mixed',
                aggregateStep('dummyapp.Mixed', 'direct', 0, 'Order', AccessMode.WRITE, 'order'),
                recoveryStep('dummyapp.Mixed', 'recovering', 1, 'Producer', 'Product', 'product'))
        def reader = saga('dummyapp.Reader',
                aggregateStep('dummyapp.Reader', 'direct', 0, 'Order', AccessMode.READ, 'order'),
                aggregateStep('dummyapp.Reader', 'recoveryTarget', 1, 'Product', AccessMode.READ, 'product'))
        def inputs = [input('mixed-input', mixed.sagaFqn(), [:]),
                      input('reader-input', reader.sagaFqn(), [:])]
        def generationConfig = config(includeSingles: false, maxSagaSetSize: 2,
                scheduleStrategy: ScheduleStrategy.SEGMENT_COMPRESSED)
        def graph = ConflictGraphBuilder.buildSelectionGraph([mixed, reader], [], generationConfig)

        when:
        def evidence = InputTupleSelection.selectedCandidates(
                [mixed.sagaFqn(), reader.sagaFqn()], inputs, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK)
        def anchors = InputTupleSelection.selectedAnchorCandidates(
                [mixed.sagaFqn(), reader.sagaFqn()], inputs, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK)
        def generated = ScenarioGenerator.generate([mixed, reader], inputs, generationConfig)

        then:
        evidence*.origin().unique() == [ConflictGraphBuilder.ConflictOrigin.FORWARD]
        anchors*.origin().toSet() == [ConflictGraphBuilder.ConflictOrigin.FORWARD,
                                     ConflictGraphBuilder.ConflictOrigin.RECOVERY] as Set
        generated.workloadPlans().size() == 6
    }

    def 'count only mode does not materialize scenario plans but keeps rejected inputs'() {
        given:
        def saga = saga('com.example.A', step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'order-1'))
        def sagasInput = inputWithSourceModeAndSource('com.example.A', SourceMode.SAGAS, 'sagas-source')
        def tccInput = inputWithSourceModeAndSource('com.example.A', SourceMode.TCC, 'tcc-source')

        when:
        def result = ScenarioGenerator.generate([saga], [sagasInput, tccInput], config(
                catalogWriteMode: ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY,
                maxSagaSetSize: 1))

        then:
        result.workloadPlans().isEmpty()
        result.rejectedInputVariants().size() == 1
        result.rejectedInputVariants()[0].inputVariant().sourceMode() == SourceMode.TCC
        result.counts().workloadsEmitted == 0
    }

    def 'write read same exact key creates conflict evidence'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared'))

        when:
        def result = ScenarioGenerator.generate([sagaA, sagaB], [
                input('input-a', 'com.example.A', 'shared'),
                input('input-b', 'com.example.B', 'shared')
        ], config(maxSagaSetSize: 2))

        then:
        def multi = result.workloadPlans().find { it.kind() == ScenarioKind.MULTI_SAGA }
        multi != null
        multi.conflictEvidence().size() == 1
        multi.conflictEvidence()*.kind().every { it in [ConflictKind.WRITE_READ, ConflictKind.READ_WRITE] }
    }

    def 'static exact aggregate equality is not contradicted by unrelated input bindings'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared'))

        when:
        def result = ScenarioGenerator.generate([sagaA, sagaB], [
                input('input-a', 'com.example.A', 'order-1'),
                input('input-b', 'com.example.B', 'order-2')
        ], config(maxSagaSetSize: 2))

        then:
        result.workloadPlans()*.kind().count { it == ScenarioKind.MULTI_SAGA } == 1
        result.workloadPlans().size() == 3
    }

    def 'exact command keys remain positive when inputs carry other unequal logical keys'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared'))

        when:
        def result = ScenarioGenerator.generate([sagaA, sagaB], [
                input('input-a', 'com.example.A', [orderId: 'A', tenant: 'same']),
                input('input-b', 'com.example.B', [orderId: 'B', tenant: 'same'])
        ], config(maxSagaSetSize: 2))

        then:
        result.workloadPlans()*.kind().count { it == ScenarioKind.MULTI_SAGA } == 1
        result.workloadPlans().size() == 3
    }

    def 'type only fallback is opt in'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, null, FootprintConfidence.TYPE_ONLY))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, null, FootprintConfidence.TYPE_ONLY))
        def inputs = [
                input('input-a', 'com.example.A', 'shared'),
                input('input-b', 'com.example.B', 'shared')
        ]

        when:
        def disabled = ScenarioGenerator.generate([sagaA, sagaB], inputs, config(maxSagaSetSize: 2, allowTypeOnlyFallback: false))
        def enabled = ScenarioGenerator.generate([sagaA, sagaB], inputs, config(maxSagaSetSize: 2, allowTypeOnlyFallback: true))

        then:
        disabled.workloadPlans()*.kind().every { it == ScenarioKind.SINGLE_SAGA }
        enabled.workloadPlans().any { it.kind() == ScenarioKind.MULTI_SAGA }
        enabled.workloadPlans().find { it.kind() == ScenarioKind.MULTI_SAGA }.conflictEvidence()*.kind().every { it == ConflictKind.TYPE_ONLY }
        enabled.warnings().any { it.toLowerCase().contains('type-only') }
    }

    def 'disconnected saga sets are pruned'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared-a'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared-a'))
        def sagaC = saga('com.example.C',
                step('com.example.C', 'step-1', 0, AccessMode.WRITE, 'shared-c'))

        when:
        def result = ScenarioGenerator.generate([sagaA, sagaB, sagaC], [
                input('input-a', 'com.example.A', 'shared-a'),
                input('input-b', 'com.example.B', 'shared-a'),
                input('input-c', 'com.example.C', 'shared-c')
        ], config(maxSagaSetSize: 3))

        then:
        !result.workloadPlans().any { it.participants().size() == 3 }
        result.workloadPlans().any { it.participants().size() == 2 }
    }

    def 'connected chain is allowed when max saga set size permits'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared-a'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared-a'),
                step('com.example.B', 'step-2', 1, AccessMode.WRITE, 'shared-b'))
        def sagaC = saga('com.example.C',
                step('com.example.C', 'step-1', 0, AccessMode.READ, 'shared-b'))

        when:
        def result = ScenarioGenerator.generate([sagaA, sagaB, sagaC], [
                input('input-a', 'com.example.A', 'shared'),
                input('input-b', 'com.example.B', 'shared'),
                input('input-c', 'com.example.C', 'shared')
        ], config(includeSingles: false, maxSagaSetSize: 3))

        then:
        result.workloadPlans().any { it.kind() == ScenarioKind.MULTI_SAGA && it.participants().size() == 3 }
    }

    def 'caps are deterministic and reported'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared-a'),
                step('com.example.A', 'step-2', 1, AccessMode.READ, 'shared-b'),
                step('com.example.A', 'step-3', 2, AccessMode.WRITE, 'shared-c'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared-a'),
                step('com.example.B', 'step-2', 1, AccessMode.WRITE, 'shared-b'),
                step('com.example.B', 'step-3', 2, AccessMode.READ, 'shared-c'))
        def inputs = [
                input('input-a', 'com.example.A', 'shared'),
                input('input-b', 'com.example.B', 'shared')
        ]

        when:
        def first = ScenarioGenerator.generate([sagaA, sagaB], inputs, config(
                includeSingles: false,
                maxSagaSetSize: 2,
                maxCatalogScenarios: 2,
                maxSchedulesPerInputTuple: 20,
                scheduleStrategy: ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING))
        def second = ScenarioGenerator.generate([sagaA, sagaB], inputs, config(
                includeSingles: false,
                maxSagaSetSize: 2,
                maxCatalogScenarios: 2,
                maxSchedulesPerInputTuple: 20,
                scheduleStrategy: ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING))

        then:
        first.workloadPlans().size() == 2
        first.workloadPlans()*.deterministicId() == second.workloadPlans()*.deterministicId()
        first.counts().get('workloadsCapped') > 0
        first.warnings().any { it.contains('maxCatalogScenarios') }
    }

    def 'event expansion reports reaching the cap between base workloads'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'emit', 0, AccessMode.WRITE, 'a'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'emit', 0, AccessMode.WRITE, 'b'))
        def inputs = [
                input('input-a', 'com.example.A', 'a'),
                input('input-b', 'com.example.B', 'b')
        ]
        def definitions = ['com.example.A', 'com.example.B'].collect { sagaFqn ->
            def eventType = "${sagaFqn}Event"
            def site = new EventEmissionSite(
                    ScenarioIdGenerator.eventEmissionSiteId("${sagaFqn}Service", 'emit()', 0, eventType),
                    "${sagaFqn}Service", 'emit()', 0, eventType, ['direct'])
            new EventConsequenceDefinition(
                    sagaFqn, "${sagaFqn}::emit", site,
                    "${sagaFqn}Handling", 'handle', "${sagaFqn}Handler",
                    "${sagaFqn}Processing", 'process', "${sagaFqn}Facade", 'invoke',
                    'com.example.Downstream', EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER, [])
        }
        def cappedConfig = config(maxSagaSetSize: 1, maxCatalogScenarios: 2)

        when:
        def first = ScenarioGenerator.generate([sagaA, sagaB], inputs, definitions, cappedConfig)
        def permuted = ScenarioGenerator.generate([sagaB, sagaA], inputs.reverse(), definitions.reverse(), cappedConfig)

        then:
        first.workloadPlans().size() == 2
        first.workloadPlans()*.deterministicId() == permuted.workloadPlans()*.deterministicId()
        first.counts().workloadsCapped == 1
        first.counts().eventConsequenceExpansionCapEncounters == 1
        first.counts().eventConsequenceBaseWorkloadsOmittedAtCap == 1
        first.warnings().any {
            it == 'reached maxCatalogScenarios=2 between base workloads during event-consequence expansion; ' +
                    '1 remaining base workloads and their placements were not emitted'
        }
    }

    def 'scenario ids are stable after input order permutation'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared'))
        def inputs = [
                input('input-a', 'com.example.A', 'shared'),
                input('input-b', 'com.example.B', 'shared')
        ]

        when:
        def first = ScenarioGenerator.generate([sagaA, sagaB], inputs, config(maxSagaSetSize: 2))
        def second = ScenarioGenerator.generate([sagaB, sagaA], [inputs[1], inputs[0]], config(maxSagaSetSize: 2))

        then:
        first.workloadPlans()*.deterministicId() == second.workloadPlans()*.deterministicId()
        first.workloadPlans()*.kind() == second.workloadPlans()*.kind()
    }

    def 'segment compressed scenario schedules and ids are stable across repeated and permuted generation'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'internal', 0, AccessMode.READ, 'a-only'),
                step('com.example.A', 'conflict', 1, AccessMode.WRITE, 'shared'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'internal', 0, AccessMode.READ, 'b-only'),
                step('com.example.B', 'conflict', 1, AccessMode.READ, 'shared'))
        def inputs = [
                input('input-a', 'com.example.A', 'shared'),
                input('input-b', 'com.example.B', 'shared')
        ]
        def segmentConfig = config(
                generationStrategy: ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                includeSingles: false,
                maxSagaSetSize: 2,
                maxSchedulesPerInputTuple: 20,
                scheduleStrategy: ScheduleStrategy.SEGMENT_COMPRESSED)

        when:
        def first = ScenarioGenerator.generate([sagaA, sagaB], inputs, segmentConfig)
        def repeated = ScenarioGenerator.generate([sagaA, sagaB], inputs, segmentConfig)
        def permuted = ScenarioGenerator.generate([sagaB, sagaA], [inputs[1], inputs[0]], segmentConfig)
        def differentSeed = ScenarioGenerator.generate([sagaA, sagaB], inputs, config(
                generationStrategy: ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                includeSingles: false,
                maxSagaSetSize: 2,
                maxSchedulesPerInputTuple: 20,
                scheduleStrategy: ScheduleStrategy.SEGMENT_COMPRESSED,
                deterministicSeed: 9999L))

        then:
        scenarioIdentitySnapshot(first) == scenarioIdentitySnapshot(repeated)
        scenarioIdentitySnapshot(first) == scenarioIdentitySnapshot(permuted)
        scenarioIdentitySnapshot(first) == scenarioIdentitySnapshot(differentSeed)
        first.workloadPlans().size() == 2
    }

    def 'expanded schedules preserve intra saga order'() {
        given:
        def sagaA = saga('com.example.A',
                step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared-a'),
                step('com.example.A', 'step-2', 1, AccessMode.READ, 'shared-b'))
        def sagaB = saga('com.example.B',
                step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared-a'),
                step('com.example.B', 'step-2', 1, AccessMode.WRITE, 'shared-b'))

        when:
        def result = ScenarioGenerator.generate([sagaA, sagaB], [
                input('input-a', 'com.example.A', 'shared'),
                input('input-b', 'com.example.B', 'shared')
        ], config(
                includeSingles: false,
                maxSagaSetSize: 2,
                maxSchedulesPerInputTuple: 20,
                scheduleStrategy: ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING))

        then:
        result.workloadPlans().size() > 1
        result.workloadPlans().every { plan ->
            def stepsByInstance = plan.forwardSchedule().groupBy { it.sagaInstanceId() }
            stepsByInstance.every { sagaInstanceId, steps ->
                def sagaFqn = plan.participants().find { it.deterministicId() == sagaInstanceId }.sagaFqn()
                def expectedStepIds = expectedStepIdsBySaga[sagaFqn]
                def actualStepIds = steps.sort { it.scheduleOrder() }*.stepId()
                actualStepIds == expectedStepIds
            }
        }
    }

    def 'schedule cap is reported after truncating interleavings'() {
        given:
        def sagaInputs = [
                new SagaScheduleInput('instance-a', 'com.example.A', [
                        step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared-a'),
                        step('com.example.A', 'step-2', 1, AccessMode.READ, 'shared-b')
                ]),
                new SagaScheduleInput('instance-b', 'com.example.B', [
                        step('com.example.B', 'step-1', 0, AccessMode.READ, 'shared-a'),
                        step('com.example.B', 'step-2', 1, AccessMode.WRITE, 'shared-b')
                ])
        ]

        when:
        def result = ScheduleEnumerator.enumerate(sagaInputs, ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING, 2, 1234L)

        then:
        result.schedules().size() == 2
        result.counts().get('schedulesCapped') == 1
        result.warnings().any { it.toLowerCase().contains('schedule cap') }
    }

    def 'segment compressed schedules interleave conflict anchors instead of internal steps'() {
        given:
        def sagaInputs = [
                new SagaScheduleInput('instance-a', 'com.example.A', [
                        step('com.example.A', 'internal', 0, AccessMode.READ, 'a-only'),
                        step('com.example.A', 'conflict', 1, AccessMode.WRITE, 'shared')
                ]),
                new SagaScheduleInput('instance-b', 'com.example.B', [
                        step('com.example.B', 'internal', 0, AccessMode.READ, 'b-only'),
                        step('com.example.B', 'conflict', 1, AccessMode.READ, 'shared')
                ])
        ]

        when:
        def result = ScheduleEnumerator.enumerate(sagaInputs, ScheduleStrategy.SEGMENT_COMPRESSED, 20, 1234L)

        then:
        result.schedules()*.collect { it.stepId() } == [
                ['com.example.A::internal', 'com.example.A::conflict', 'com.example.B::internal', 'com.example.B::conflict'],
                ['com.example.B::internal', 'com.example.B::conflict', 'com.example.A::internal', 'com.example.A::conflict']
        ]
        result.counts().get('schedulesEmitted') == 2
        result.warnings().isEmpty()
    }

    def 'segment compressed schedules do not fall back to serial above the old step cutoff'() {
        given:
        def sagaInputs = [
                new SagaScheduleInput('instance-a', 'com.example.A', (0..5).collect {
                    step('com.example.A', "internal-${it}".toString(), it, AccessMode.READ, "a-${it}".toString())
                } + [step('com.example.A', 'conflict', 6, AccessMode.WRITE, 'shared')]),
                new SagaScheduleInput('instance-b', 'com.example.B', (0..4).collect {
                    step('com.example.B', "internal-${it}".toString(), it, AccessMode.READ, "b-${it}".toString())
                } + [step('com.example.B', 'conflict', 5, AccessMode.READ, 'shared')])
        ]

        when:
        def result = ScheduleEnumerator.enumerate(sagaInputs, ScheduleStrategy.SEGMENT_COMPRESSED, 20, 1234L)

        then:
        result.schedules().size() == 2
        !result.warnings().any { it.contains('SERIAL') }
    }

    def 'segment compressed emits one canonical schedule when no anchors exist'() {
        given:
        def sagaInputs = [
                new SagaScheduleInput('instance-a', 'com.example.A', [
                        step('com.example.A', 'step-1', 0, AccessMode.READ, 'a-only'),
                        step('com.example.A', 'step-2', 1, AccessMode.READ, 'a-only-2')
                ]),
                new SagaScheduleInput('instance-b', 'com.example.B', [
                        step('com.example.B', 'step-1', 0, AccessMode.READ, 'b-only'),
                        step('com.example.B', 'step-2', 1, AccessMode.READ, 'b-only-2')
                ])
        ]

        when:
        def result = ScheduleEnumerator.enumerate(sagaInputs, ScheduleStrategy.SEGMENT_COMPRESSED, 20, 1234L)

        then:
        result.schedules()*.collect { it.stepId() } == [[
                'com.example.A::step-1', 'com.example.A::step-2',
                'com.example.B::step-1', 'com.example.B::step-2'
        ]]
        result.counts().get('schedulesEmitted') == 1
    }

    def 'segment compressed matches order preserving interleaving when every step is an anchor'() {
        given:
        def sagaInputs = [
                new SagaScheduleInput('instance-a', 'com.example.A', [
                        step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'shared'),
                        step('com.example.A', 'step-2', 1, AccessMode.READ, 'shared')
                ]),
                new SagaScheduleInput('instance-b', 'com.example.B', [
                        step('com.example.B', 'step-1', 0, AccessMode.WRITE, 'shared')
                ])
        ]

        when:
        def interleaving = ScheduleEnumerator.enumerate(sagaInputs, ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING, 20, 1234L)
        def compressed = ScheduleEnumerator.enumerate(sagaInputs, ScheduleStrategy.SEGMENT_COMPRESSED, 20, 1234L)

        then:
        compressed.schedules()*.collect { it.stepId() } == interleaving.schedules()*.collect { it.stepId() }
        compressed.schedules().size() == 3
    }

    def 'segment compressed emits internal steps with following anchors and tails after anchor segments'() {
        given:
        def sagaInputs = [
                new SagaScheduleInput('instance-a', 'com.example.A', [
                        step('com.example.A', 'before-anchor-1', 0, AccessMode.READ, 'a-only-1'),
                        step('com.example.A', 'anchor-1', 1, AccessMode.WRITE, 'shared-1'),
                        step('com.example.A', 'before-anchor-2', 2, AccessMode.READ, 'a-only-2'),
                        step('com.example.A', 'anchor-2', 3, AccessMode.WRITE, 'shared-2'),
                        step('com.example.A', 'tail', 4, AccessMode.READ, 'a-tail')
                ]),
                new SagaScheduleInput('instance-b', 'com.example.B', [
                        step('com.example.B', 'anchor-1', 0, AccessMode.READ, 'shared-1'),
                        step('com.example.B', 'anchor-2', 1, AccessMode.READ, 'shared-2')
                ])
        ]

        when:
        def result = ScheduleEnumerator.enumerate(sagaInputs, ScheduleStrategy.SEGMENT_COMPRESSED, 20, 1234L)

        then:
        result.schedules().first()*.stepId() == [
                'com.example.A::before-anchor-1', 'com.example.A::anchor-1',
                'com.example.A::before-anchor-2', 'com.example.A::anchor-2',
                'com.example.B::anchor-1', 'com.example.B::anchor-2',
                'com.example.A::tail'
        ]
        result.schedules().every { it.last().stepId() == 'com.example.A::tail' }
        result.schedules().every { schedule ->
            schedule.findIndexOf { it.stepId() == 'com.example.A::before-anchor-2' } <
                    schedule.findIndexOf { it.stepId() == 'com.example.A::anchor-2' }
        }
    }

    def 'segment compressed appends zero-anchor saga steps to the canonical tail'() {
        given:
        def sagaInputs = [
                new SagaScheduleInput('instance-a', 'com.example.A', [
                        step('com.example.A', 'before-anchor', 0, AccessMode.READ, 'a-only'),
                        step('com.example.A', 'anchor', 1, AccessMode.WRITE, 'shared')
                ]),
                new SagaScheduleInput('instance-b', 'com.example.B', [
                        step('com.example.B', 'tail-1', 0, AccessMode.READ, 'b-only-1'),
                        step('com.example.B', 'tail-2', 1, AccessMode.READ, 'b-only-2')
                ]),
                new SagaScheduleInput('instance-c', 'com.example.C', [
                        step('com.example.C', 'anchor', 0, AccessMode.READ, 'shared')
                ])
        ]

        when:
        def result = ScheduleEnumerator.enumerate(sagaInputs, ScheduleStrategy.SEGMENT_COMPRESSED, 20, 1234L)

        then:
        result.schedules()*.collect { it.stepId() } == [
                ['com.example.A::before-anchor', 'com.example.A::anchor', 'com.example.C::anchor', 'com.example.B::tail-1', 'com.example.B::tail-2'],
                ['com.example.C::anchor', 'com.example.A::before-anchor', 'com.example.A::anchor', 'com.example.B::tail-1', 'com.example.B::tail-2']
        ]
    }

    private static Map<String, List<String>> getExpectedStepIdsBySaga() {
        [
                'com.example.A': ['com.example.A::step-1', 'com.example.A::step-2'],
                'com.example.B': ['com.example.B::step-1', 'com.example.B::step-2']
        ]
    }

    private static List<Map<String, Object>> scenarioIdentitySnapshot(WorkloadGenerationResult result) {
        result.workloadPlans().collect { plan ->
            [
                    scenarioId      : plan.deterministicId(),
                    scheduleStepIds  : plan.forwardSchedule()*.stepId(),
                    scheduledStepIds : plan.forwardSchedule()*.deterministicId()
            ]
        }
    }

    private static ScenarioGeneratorConfig config(Map<String, ?> overrides = [:]) {
        new ScenarioGeneratorConfig(
                overrides.get('exportEnabled', false) as boolean,
                overrides.get('generationStrategy', ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED) as ScenarioGeneratorConfig.GenerationStrategy,
                overrides.get('catalogWriteMode', ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS) as ScenarioGeneratorConfig.CatalogWriteMode,
                overrides.get('includeSingles', true) as boolean,
                overrides.get('maxSagaSetSize', 2) as int,
                overrides.get('maxCatalogScenarios', 100) as int,
                overrides.get('maxInputVariantsPerSaga', 3) as int,
                overrides.get('maxSchedulesPerInputTuple', 20) as int,
                overrides.get('allowTypeOnlyFallback', false) as boolean,
                overrides.get('inputPolicy', InputPolicy.RESOLVED_OR_REPLAYABLE) as InputPolicy,
                overrides.get('scheduleStrategy', ScheduleStrategy.SERIAL) as ScheduleStrategy,
                overrides.get('deterministicSeed', 1234L) as long
        )
    }

    private static SagaDefinition saga(String sagaFqn, StepDefinition... steps) {
        new SagaDefinition(sagaFqn, steps.toList(), [])
    }

    private static StepDefinition step(String sagaFqn,
                                       String stepName,
                                       int orderIndex,
                                       AccessMode accessMode,
                                       String keyText,
                                       FootprintConfidence confidence = FootprintConfidence.EXACT) {
        new StepDefinition(
                "${sagaFqn}::${stepName}",
                "${sagaFqn}::${stepName}",
                stepName,
                orderIndex,
                [],
                [new StepFootprint(
                        new AggregateKey('com.example.Order', 'Order', keyText, confidence),
                        accessMode,
                        [])],
                [])
    }

    private static StepDefinition aggregateStep(String sagaFqn,
                                                String stepName,
                                                int orderIndex,
                                                String aggregateName,
                                                AccessMode accessMode,
                                                String keyText,
                                                FootprintConfidence confidence = FootprintConfidence.EXACT) {
        new StepDefinition(
                "${sagaFqn}::${stepName}", "${sagaFqn}::${stepName}", stepName, orderIndex, [],
                [new StepFootprint(new AggregateKey("dummyapp.${aggregateName}", aggregateName,
                        keyText, confidence), accessMode, [])], [])
    }

    private static StepDefinition recoveryStep(String sagaFqn,
                                               String stepName,
                                               int orderIndex,
                                               String forwardAggregate,
                                               String recoveryAggregate,
                                               String recoveryKey) {
        new StepDefinition(
                "${sagaFqn}::${stepName}", "${sagaFqn}::${stepName}", stepName, orderIndex, [],
                [new StepFootprint(new AggregateKey("dummyapp.${forwardAggregate}", forwardAggregate,
                        'producer', FootprintConfidence.EXACT), AccessMode.READ, [])],
                [new StepFootprint(new AggregateKey("dummyapp.${recoveryAggregate}", recoveryAggregate,
                        recoveryKey, FootprintConfidence.EXACT), AccessMode.WRITE, [])],
                true, true, true, CompensationEvidenceClass.EXPLICIT_COMPENSATION,
                [], [])
    }

    private static EventConsequenceDefinition eventRoute(SagaDefinition producer, SagaDefinition consumer) {
        def trigger = producer.steps().first()
        def eventType = 'dummyapp.Event'
        def site = new EventEmissionSite(
                ScenarioIdGenerator.eventEmissionSiteId('dummyapp.Service', 'emit()', 0, eventType),
                'dummyapp.Service', 'emit()', 0, eventType, [])
        new EventConsequenceDefinition(producer.sagaFqn(), trigger.stepKey(), site,
                'dummyapp.EventHandling', 'handle', 'dummyapp.EventHandler', 'dummyapp.EventProcessing',
                'process', 'dummyapp.Facade', 'invoke', consumer.sagaFqn(),
                EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER, [])
    }

    private static InputVariant input(String deterministicId, String sagaFqn, String keyValue) {
        input(deterministicId, sagaFqn, [orderId: keyValue])
    }

    private static InputVariant inputWithSourceMode(String sagaFqn, SourceMode sourceMode) {
        inputWithSourceModeAndSource(sagaFqn, sourceMode, 'same-source')
    }

    private static InputVariant inputWithSourceModeAndSource(String sagaFqn, SourceMode sourceMode, String sourceText) {
        new InputVariant(
                null,
                sagaFqn,
                'com.example.TestInput',
                'build',
                'sagaField',
                InputResolutionStatus.RESOLVED,
                sourceMode,
                SourceModeConfidence.TYPE_EVIDENCE,
                ['mode evidence'],
                sourceText,
                'same-provenance',
                ['arg'],
                [orderId: 'order-1'],
                [])
    }

    private static InputVariant inputWithRecipe(String sagaFqn, InputRecipe recipe, Map<String, String> logicalKeyBindings, List<InputOwner> owners) {
        new InputVariant(
                null,
                sagaFqn,
                'com.example.TestInput',
                'build',
                'sagaField',
                InputResolutionStatus.RESOLVED,
                SourceMode.SAGAS,
                SourceModeConfidence.TYPE_EVIDENCE,
                ['mode evidence'],
                'same-source',
                'same-provenance',
                owners,
                ['arg[0]: literal'],
                logicalKeyBindings,
                [],
                recipe)
    }

    private static InputRecipe recipeForLiteral(String text) {
        def node = InputRecipeNode.builder('literal')
                .sourceText(text)
                .executorReady(true)
                .literalKind('integer')
                .value(Long.valueOf(text))
                .expectedTypeFqn('java.lang.Integer')
                .build()
        def argument = new InputRecipeArgument(0,
                'java.lang.Integer',
                InputResolutionStatus.RESOLVED,
                true,
                [],
                'literal',
                node)
        new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], [argument])
    }

    private static InputVariant input(String deterministicId, String sagaFqn, Map<String, String> logicalKeyBindings) {
        new InputVariant(
                deterministicId,
                sagaFqn,
                'com.example.TestInput',
                'build',
                'sagaField',
                InputResolutionStatus.RESOLVED,
                "source-${deterministicId}",
                "provenance-${deterministicId}",
                ['arg'],
                logicalKeyBindings,
                [])
    }
}
