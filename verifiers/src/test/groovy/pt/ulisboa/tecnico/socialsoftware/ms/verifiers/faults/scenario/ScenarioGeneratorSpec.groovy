package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig.InputPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig.ScheduleStrategy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputOwner
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioGenerationResult
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
        first.scenarioPlans()*.kind() == [ScenarioKind.SINGLE_SAGA, ScenarioKind.SINGLE_SAGA, ScenarioKind.MULTI_SAGA]
        first.scenarioPlans()*.deterministicId() == second.scenarioPlans()*.deterministicId()
    }

    def 'source mode does not participate in input variant deterministic identity'() {
        given:
        def saga = saga('com.example.A', step('com.example.A', 'step-1', 0, AccessMode.WRITE, 'order-1'))
        def sagaInput = inputWithSourceMode('com.example.A', SourceMode.SAGAS)
        def unknownInput = inputWithSourceMode('com.example.A', SourceMode.UNKNOWN)

        when:
        def result = ScenarioGenerator.generate([saga], [sagaInput, unknownInput], config(maxSagaSetSize: 1))

        then:
        result.scenarioPlans().size() == 1
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
        def accepted = result.scenarioPlans()*.inputs().flatten()
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
        result.scenarioPlans().size() == 2
        result.scenarioPlans()*.inputs().flatten()*.stableSourceText().toSet() == ['sagas-source', 'unknown-source'] as Set
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
        result.scenarioPlans().size() == 1
        def accepted = result.scenarioPlans()[0].inputs()[0]
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
        result.scenarioPlans().size() == 1
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
        result.scenarioPlans()*.kind().every { it == ScenarioKind.SINGLE_SAGA }
        result.scenarioPlans().size() == 2
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
        result.scenarioPlans().size() == 1
        result.scenarioPlans()[0].kind() == ScenarioKind.MULTI_SAGA
        result.scenarioPlans()[0].sagaInstances()*.sagaFqn().toSet() == ['com.example.A', 'com.example.B'] as Set
        result.scenarioPlans()[0].conflictEvidence().isEmpty()
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
        result.scenarioPlans().size() == 1
        result.scenarioPlans()[0].sagaInstances()*.sagaFqn().toSet() == ['com.example.A', 'com.example.B'] as Set
        result.scenarioPlans()[0].conflictEvidence().size() == 1
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
        result.scenarioPlans().isEmpty()
        result.rejectedInputVariants().size() == 1
        result.rejectedInputVariants()[0].inputVariant().sourceMode() == SourceMode.TCC
        result.counts().scenariosEmitted == 0
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
        def multi = result.scenarioPlans().find { it.kind() == ScenarioKind.MULTI_SAGA }
        multi != null
        multi.conflictEvidence().size() == 1
        multi.conflictEvidence()*.kind().every { it in [ConflictKind.WRITE_READ, ConflictKind.READ_WRITE] }
    }

    def 'same aggregate different exact keys are not joined'() {
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
        result.scenarioPlans()*.kind().count { it == ScenarioKind.MULTI_SAGA } == 0
        result.scenarioPlans().size() == 2
    }

    def 'mixed known keys with different exact values do not join'() {
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
        result.scenarioPlans()*.kind().every { it == ScenarioKind.SINGLE_SAGA }
        result.scenarioPlans().size() == 2
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
        disabled.scenarioPlans()*.kind().every { it == ScenarioKind.SINGLE_SAGA }
        enabled.scenarioPlans().any { it.kind() == ScenarioKind.MULTI_SAGA }
        enabled.scenarioPlans().find { it.kind() == ScenarioKind.MULTI_SAGA }.conflictEvidence()*.kind().every { it == ConflictKind.TYPE_ONLY }
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
        !result.scenarioPlans().any { it.sagaInstances().size() == 3 }
        result.scenarioPlans().any { it.sagaInstances().size() == 2 }
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
        result.scenarioPlans().any { it.kind() == ScenarioKind.MULTI_SAGA && it.sagaInstances().size() == 3 }
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
        first.scenarioPlans().size() == 2
        first.scenarioPlans()*.deterministicId() == second.scenarioPlans()*.deterministicId()
        first.counts().get('scenariosCapped') > 0
        first.warnings().any { it.contains('maxCatalogScenarios') }
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
        first.scenarioPlans()*.deterministicId() == second.scenarioPlans()*.deterministicId()
        first.scenarioPlans()*.kind() == second.scenarioPlans()*.kind()
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
        first.scenarioPlans().size() == 2
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
        result.scenarioPlans().size() > 1
        result.scenarioPlans().every { plan ->
            def stepsByInstance = plan.expandedSchedule().groupBy { it.sagaInstanceId() }
            stepsByInstance.every { sagaInstanceId, steps ->
                def sagaFqn = plan.sagaInstances().find { it.deterministicId() == sagaInstanceId }.sagaFqn()
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

    private static List<Map<String, Object>> scenarioIdentitySnapshot(ScenarioGenerationResult result) {
        result.scenarioPlans().collect { plan ->
            [
                    scenarioId      : plan.deterministicId(),
                    scheduleStepIds  : plan.expandedSchedule()*.stepId(),
                    scheduledStepIds : plan.expandedSchedule()*.deterministicId()
            ]
        }
    }

    private static ScenarioGeneratorConfig config(Map<String, ?> overrides = [:]) {
        new ScenarioGeneratorConfig(
                overrides.get('exportEnabled', false) as boolean,
                overrides.get('generationStrategy', ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED) as ScenarioGeneratorConfig.GenerationStrategy,
                overrides.get('catalogWriteMode', ScenarioGeneratorConfig.CatalogWriteMode.WRITE_PLANS) as ScenarioGeneratorConfig.CatalogWriteMode,
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
