package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingCalculator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.StaticAnalysisArtifactWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupAction
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPlan
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SourceSetupPlanBinding
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceValueReference
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceAggregateKeyInputEvidence
import spock.lang.Specification

import java.nio.file.Files

class InputTupleSelectionSpec extends Specification {

    def 'direct candidates require equal exact keys or two resolved symbolic keys'() {
        given:
        def exactEqual = graph(saga('A', footprint('Order', '7', FootprintConfidence.EXACT)),
                saga('B', footprint('Order', '7', FootprintConfidence.EXACT)), false)
        def exactUnequal = graph(saga('A', footprint('Order', '7', FootprintConfidence.EXACT)),
                saga('B', footprint('Order', '8', FootprintConfidence.EXACT)), true)
        def symbolic = graph(saga('A', footprint('Order', 'leftOrderId', FootprintConfidence.SYMBOLIC)),
                saga('B', footprint('Order', 'rightOrderId', FootprintConfidence.SYMBOLIC)), false)
        def mixedUnknownStrict = graph(saga('A', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)),
                saga('B', footprint('Order', null, FootprintConfidence.TYPE_ONLY)), false)
        def mixedUnknownBroad = graph(saga('A', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)),
                saga('B', footprint('Order', null, FootprintConfidence.TYPE_ONLY)), true)
        def readRead = graph(
                saga('A', footprint('Order', 'leftOrderId', FootprintConfidence.SYMBOLIC, AccessMode.READ)),
                saga('B', footprint('Order', 'rightOrderId', FootprintConfidence.SYMBOLIC, AccessMode.READ)), false)

        expect:
        exactEqual.conflictCandidates().size() == 1
        exactUnequal.conflictCandidates().isEmpty()
        symbolic.conflictCandidates().size() == 1
        symbolic.conflictCandidates().first().kind().name() == 'SYMBOLIC'
        mixedUnknownStrict.conflictCandidates().isEmpty()
        mixedUnknownBroad.conflictCandidates().size() == 1
        mixedUnknownBroad.conflictCandidates().first().kind().name() == 'TYPE_ONLY'
        readRead.conflictCandidates().isEmpty()
    }

    def 'exact numeric candidates and input bindings compare by value rather than signed spelling'() {
        given:
        def equalNumeric = graph(saga('A', footprint('Order', '+42', FootprintConfidence.EXACT)),
                saga('B', footprint('Order', '42', FootprintConfidence.EXACT)), false)
        def unequalNumeric = graph(saga('A', footprint('Order', '+42', FootprintConfidence.EXACT)),
                saga('B', footprint('Order', '43', FootprintConfidence.EXACT)), false)
        def quotedString = graph(saga('A', footprint('Order', '+42', FootprintConfidence.EXACT)),
                saga('B', footprint('Order', '"42"', FootprintConfidence.EXACT)), false)
        def mixed = graph(saga('A', footprint('Order', '+42', FootprintConfidence.EXACT)),
                saga('B', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)), false)

        expect:
        equalNumeric.conflictCandidates().size() == 1
        unequalNumeric.conflictCandidates().isEmpty()
        quotedString.conflictCandidates().isEmpty()
        selected([input('A', 'a', [:]), input('B', 'b', [orderId: '42'])], mixed,
                InputTupleSelection.Mode.STRICT)
        !selected([input('A', 'a', [:]), input('B', 'b', [orderId: '43'])], mixed,
                InputTupleSelection.Mode.STRICT)
    }

    def 'strict needs positive exact evidence while fallback admits missing and rejects unequal'() {
        given:
        def sagas = [saga('A', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)),
                     saga('B', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC))]
        def strictGraph = ConflictGraphBuilder.build(sagas, config(false))
        def broadGraph = ConflictGraphBuilder.build(sagas, config(true))
        def equal = [input('A', 'a1', [orderId: '1']), input('B', 'b1', [orderId: '1'])]
        def unequal = [input('A', 'a2', [orderId: '1']), input('B', 'b2', [orderId: '2'])]
        def missing = [input('A', 'a3', [:]), input('B', 'b3', [:])]

        expect:
        selected(equal, strictGraph, InputTupleSelection.Mode.STRICT)
        !selected(unequal, strictGraph, InputTupleSelection.Mode.STRICT)
        !selected(missing, strictGraph, InputTupleSelection.Mode.STRICT)
        !selected(unequal, broadGraph, InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK)
        selected(missing, broadGraph, InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK)
    }

    def 'canonical same-source evidence is strict-positive but unrelated origins are not'() {
        given:
        def sagas = [saga('A', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)),
                     saga('B', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC))]
        def graph = ConflictGraphBuilder.build(sagas, config(false))
        def inputs = [input('A', 'a', [:]), input('B', 'b', [:])]
        def shared = new GroovySourceValueReference('Spec:10:4', 'createOrder', ['aggregateId'])
        def different = new GroovySourceValueReference('Spec:11:4', 'createOrder', ['aggregateId'])
        def sharedEvidence = [source(inputs[0], shared), source(inputs[1], shared)]

        expect:
        selected(inputs, graph, InputTupleSelection.Mode.STRICT,
                sharedEvidence)
        !selected(inputs, graph, InputTupleSelection.Mode.STRICT,
                [source(inputs[0], shared), source(inputs[1], different)])

        and: 'catalog generation consumes the same canonical evidence'
        ScenarioGenerator.generate(sagas, inputs, [], [], sharedEvidence, config(false))
                .workloadPlans().size() == 1
    }

    def 'same-source evidence stays scoped to the exact input occurrence and key path'() {
        given:
        def sagas = [saga('A', keyedFootprint('Order', 'orderId', 0, ['aggregateId'])),
                     saga('B', keyedFootprint('Order', 'orderId', 0, ['aggregateId']))]
        def graph = ConflictGraphBuilder.build(sagas, config(false))
        def intended = inputFromMethod('A', 'a-intended', 'same feature')
        def otherCall = inputFromMethod('A', 'a-other-call', 'same feature')
        def right = inputFromMethod('B', 'b', 'same feature')
        def shared = new GroovySourceValueReference('Spec:30:4', 'createOrder', ['aggregateId'])
        def other = new GroovySourceValueReference('Spec:31:4', 'createOrder', ['aggregateId'])
        def evidence = [source(intended, 0, ['aggregateId'], shared),
                        source(otherCall, 0, ['aggregateId'], other),
                        source(right, 0, ['aggregateId'], shared)]

        expect: 'a second direct call in the same method does not inherit the first call occurrence'
        selected([intended, right], graph, InputTupleSelection.Mode.STRICT, evidence)
        !selected([otherCall, right], graph, InputTupleSelection.Mode.STRICT, evidence)

        and: 'another constructor path for the same aggregate cannot satisfy this candidate key'
        !selected([intended, right], graph, InputTupleSelection.Mode.STRICT,
                [source(intended, 0, ['aggregateId'], other),
                 source(intended, 1, ['alternateId'], shared),
                 source(right, 0, ['aggregateId'], shared)])
    }

    def 'size three strict selection requires a positively connected tuple graph'() {
        given:
        def sagas = [saga('A', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)),
                     saga('B', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC),
                             footprint('Product', 'productId', FootprintConfidence.SYMBOLIC)),
                     saga('C', footprint('Product', 'productId', FootprintConfidence.SYMBOLIC))]
        def graph = ConflictGraphBuilder.build(sagas, config(false))
        def connected = [input('A', 'a', [orderId: '1']),
                         input('B', 'b', [orderId: '1', productId: '9']),
                         input('C', 'c', [productId: '9'])]
        def broken = [connected[0], connected[1], input('C', 'c2', [productId: '10'])]

        expect:
        selected(connected, graph, InputTupleSelection.Mode.STRICT)
        !selected(broken, graph, InputTupleSelection.Mode.STRICT)
    }

    def 'early strict pruning keeps a triple connected through an alternate path'() {
        given:
        def sagas = [saga('A', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)),
                     saga('B', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC),
                             footprint('Product', 'productId', FootprintConfidence.SYMBOLIC)),
                     saga('C', footprint('Product', 'productId', FootprintConfidence.SYMBOLIC))]
        def graph = ConflictGraphBuilder.build(sagas, config(false))
        def values = [input('A', 'a', [orderId: '1']),
                      input('B', 'b', [orderId: '1', productId: '9']),
                      input('C', 'c', [productId: '9'])]
        def bySaga = values.groupBy { it.sagaFqn() }

        when:
        def joined = InputTupleJoiner.join(['A', 'B', 'C'], bySaga, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.STRICT)

        then:
        InputTupleSelection.potentiallySelected(['A', 'B', 'C'], [values[0]],
                [[values[0]], bySaga.B, bySaga.C],
                graph.conflictCandidates(), [], InputTupleSelection.Mode.STRICT)
        joined.tuples().size() == 1
        joined.tuples()[0].inputs() == values
    }

    def 'all stays Cartesian and strict grouped accounting equals catalog enumeration'() {
        given:
        def sagas = [saga('A', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)),
                     saga('B', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC))]
        def inputs = [input('A', 'a1', [orderId: '1']), input('A', 'a2', [orderId: '2']),
                      input('B', 'b1', [orderId: '1']), input('B', 'b2', [orderId: '3'])]
        def strictConfig = config(false)
        def graph = ConflictGraphBuilder.build(sagas, strictConfig)
        def bySaga = inputs.groupBy { it.sagaFqn() }

        when:
        def all = InputTupleJoiner.join(['A', 'B'], bySaga)
        def strict = InputTupleJoiner.join(['A', 'B'], bySaga, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.STRICT)
        def generated = ScenarioGenerator.generate(sagas, inputs, [], [], [], strictConfig)
        def accounting = new ScenarioSpaceAccountingCalculator().calculate(
                'dummyapp', sagas, inputs, [], strictConfig, generated.workloadPlans().size())

        then:
        all.tuples().size() == 4
        strict.tuples().size() == 1
        generated.workloadPlans().size() == 1
        accounting.groupedSagaSets().first().compatibleInputTupleCount() == '1'
        accounting.inputBoundScenarioSpace().allInputBound().total() == '4'
        accounting.inputBoundScenarioSpace().selectedByGenerator().total() == '1'
        accounting.inputBoundScenarioSpace().catalogWritten().total() == '1'
    }

    def 'generator enumeration and grouped accounting share strict fallback and all tuple counts'() {
        given:
        def sagas = [saga('A', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)),
                     saga('B', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC))]
        def inputs = [input('A', 'a1', [orderId: '1']), input('A', 'a-missing', [:]),
                      input('B', 'b1', [orderId: '1']), input('B', 'b2', [orderId: '2'])]
        def configs = [config(false), config(true), bruteForceConfig()]

        expect:
        configs.collect { scenarioConfig ->
            def generated = ScenarioGenerator.generate(sagas, inputs, [], [], [], scenarioConfig)
            def accounting = new ScenarioSpaceAccountingCalculator().calculate(
                    'dummyapp', sagas, inputs, [], scenarioConfig, generated.workloadPlans().size())
            [generated.workloadPlans().size(),
             accounting.groupedSagaSets().first().compatibleInputTupleCount() as int,
             accounting.inputBoundScenarioSpace().allInputBound().total() as int]
        } == [[1, 1, 4], [3, 3, 4], [4, 4, 4]]
    }

    def 'count-only groups equivalent evidence instead of enumerating a billion tuples'() {
        given:
        def sagas = ['A', 'B', 'C'].collect {
            saga(it, footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC))
        }
        def graph = ConflictGraphBuilder.build(sagas, config(false))
        def bySaga = sagas.collectEntries { definition ->
            [(definition.sagaFqn()): (0..<1000).collect { index ->
                input(definition.sagaFqn(), "${definition.sagaFqn()}-${index}".toString(),
                        [orderId: Integer.toString(index % 2)])
            }]
        }

        expect:
        InputTupleSelection.count(['A', 'B', 'C'], bySaga, [], [],
                InputTupleSelection.Mode.ALL).toString() == '1000000000'
        InputTupleSelection.count(['A', 'B', 'C'], bySaga, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.STRICT).toString() == '250000000'
    }

    def 'count-only remains bounded with many distinct evidence profiles'() {
        given:
        def sagas = ['A', 'B', 'C'].collect {
            saga(it, footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC))
        }
        def graph = ConflictGraphBuilder.build(sagas, config(false))
        def bySaga = sagas.collectEntries { definition ->
            [(definition.sagaFqn()): (0..<40).collect { index ->
                input(definition.sagaFqn(), "${definition.sagaFqn()}-${index}".toString(),
                        [orderId: Integer.toString(index)])
            }]
        }

        expect:
        InputTupleSelection.count(['A', 'B', 'C'], bySaga, graph.conflictCandidates(), [],
                InputTupleSelection.Mode.STRICT).toString() == '40'
    }

    def 'count-only artifact reports type-level symbolic connectivity separately from positive input coverage'() {
        given:
        def sagas = [saga('A', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC)),
                     saga('B', footprint('Order', 'orderId', FootprintConfidence.SYMBOLIC))]
        def inputs = [input('A', 'a', [:]), input('B', 'b', [:])]
        def shared = new GroovySourceValueReference('Spec:20:4', 'createOrder', ['aggregateId'])
        def model = new ScenarioModelAdapterResult(sagas, inputs, [], [], [:], [], [:],
                [source(inputs[0], shared), source(inputs[1], shared)])
        def countOnly = new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY,
                false, 2, 100, 10, 1, false,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 41L)
        def directory = Files.createTempDirectory('m1-positive-accounting-')

        when:
        new StaticAnalysisArtifactWriter().write(model, 'dummyapp', countOnly, directory,
                '2026-09-02T00:00:00Z')
        def accounting = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                directory.resolve('accounting.json').toFile())

        then:
        accounting.path('interactions').path('direct').path('byEvidence').path('symbolic').asInt() == 1
        accounting.path('interactions').path('sagaSets').path('strict')
                .path('connectedBySize').path('2').asInt() == 1
        accounting.path('interactions').path('sagaSets').path('strict')
                .path('withAcceptedInputsBySize').path('2').asInt() == 1
        accounting.path('workloads').path('all').path('inputBoundTotal').asInt() == 1
        accounting.path('workloads').path('selected').path('inputBoundTotal').asInt() == 1
    }

    def 'count-only writer reports exact setup categories with schedule multiplicity'() {
        given:
        def sagas = [
                saga('A', footprint('Order', 'shared', FootprintConfidence.EXACT),
                        footprint('Order', 'shared', FootprintConfidence.EXACT)),
                saga('B', footprint('Order', 'shared', FootprintConfidence.EXACT)),
                saga('C', footprint('Order', 'shared', FootprintConfidence.EXACT))
        ]
        def sourceInput = readyInput('A', 'source')
        def noSetupInput = readyInput('B', 'no-setup')
        def blockedInput = blockedInput('C', 'blocked')
        def setup = new SetupPlan(SetupPlan.SCHEMA_VERSION, [
                new SetupAction('setup-1', 0, 'Spec:1', 'demo.Fixture#create():java.lang.String', [],
                        String.name, false, [])
        ], [], [])
        def model = new ScenarioModelAdapterResult(
                sagas,
                [sourceInput, noSetupInput, blockedInput],
                [],
                [new SourceSetupPlanBinding([sourceInput.deterministicId()], setup)],
                [:], [], [:], [])
        def config = new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY,
                true, 2, 100, 10, 10, false,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING, 41L)
        def directory = Files.createTempDirectory('setup-accounting-shape-')

        when:
        new StaticAnalysisArtifactWriter().write(model, 'dummyapp', config, directory,
                '2026-09-02T00:00:00Z')
        def accounting = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                directory.resolve('accounting.json').toFile())
        def setupMetrics = accounting.path('workloads').path('setup')

        then:
        setupMetrics.path('withSourceSetup').path('total').bigIntegerValue() == 1
        setupMetrics.path('withoutSetup').path('total').bigIntegerValue() == 4
        setupMetrics.path('blocked').path('total').bigIntegerValue() == 6
        setupMetrics.path('withSourceSetup').path('bySagaSetSize').path('1').bigIntegerValue() == 1
        setupMetrics.path('withoutSetup').path('bySagaSetSize').path('1').bigIntegerValue() == 1
        setupMetrics.path('withoutSetup').path('bySagaSetSize').path('2').bigIntegerValue() == 3
        setupMetrics.path('blocked').path('bySagaSetSize').path('1').bigIntegerValue() == 1
        setupMetrics.path('blocked').path('bySagaSetSize').path('2').bigIntegerValue() == 5
        (setupMetrics.path('withSourceSetup').path('total').bigIntegerValue()
                + setupMetrics.path('withoutSetup').path('total').bigIntegerValue()
                + setupMetrics.path('blocked').path('total').bigIntegerValue())
                == accounting.path('workloads').path('selected').path('inputBoundTotal').bigIntegerValue()
    }

    private static boolean selected(List<InputVariant> inputs,
                                    ConflictGraphBuilder.Result graph,
                                    InputTupleSelection.Mode mode,
                                    List<SourceAggregateKeyInputEvidence> evidence = []) {
        InputTupleSelection.selected(inputs*.sagaFqn(), inputs, graph.conflictCandidates(), evidence, mode)
    }

    private static ConflictGraphBuilder.Result graph(SagaDefinition left, SagaDefinition right, boolean fallback) {
        ConflictGraphBuilder.build([left, right], config(fallback))
    }

    private static ScenarioGeneratorConfig config(boolean fallback) {
        new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false, 3, 100, 10, 1, fallback,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 41L)
    }

    private static ScenarioGeneratorConfig bruteForceConfig() {
        new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false, 3, 100, 10, 1, false,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 41L)
    }

    private static SagaDefinition saga(String fqn, StepFootprint... footprints) {
        new SagaDefinition(fqn, footprints.toList().withIndex().collect { footprint, index ->
            new StepDefinition("${fqn}::step${index}".toString(), "${fqn}::step${index}".toString(),
                    "step${index}".toString(), index, [], [footprint], [])
        }, [])
    }

    private static StepFootprint footprint(String aggregate, String key, FootprintConfidence confidence) {
        footprint(aggregate, key, confidence, AccessMode.WRITE)
    }

    private static StepFootprint footprint(String aggregate, String key, FootprintConfidence confidence,
                                           AccessMode mode) {
        new StepFootprint(new AggregateKey(null, aggregate, key, confidence, 0, []), mode, [])
    }

    private static StepFootprint keyedFootprint(String aggregate, String key, int argumentIndex,
                                                List<String> propertyPath) {
        new StepFootprint(new AggregateKey(null, aggregate, key, FootprintConfidence.SYMBOLIC,
                argumentIndex, propertyPath), AccessMode.WRITE, [])
    }

    private static InputVariant input(String saga, String id, Map<String, String> bindings) {
        InputVariantNormalizer.normalizeForArtifact(new InputVariant(id, saga, 'Spec', id, 'saga',
                InputResolutionStatus.RESOLVED, 'new Saga()', 'source', [], bindings, []))
    }

    private static InputVariant inputFromMethod(String saga, String id, String sourceMethod) {
        InputVariantNormalizer.normalizeForArtifact(new InputVariant(id, saga, 'Spec', sourceMethod, null,
                InputResolutionStatus.RESOLVED, "new Saga(${id})".toString(),
                "source ${id}".toString(), [], [:], []))
    }

    private static InputVariant readyInput(String saga, String id) {
        def argument = new InputRecipeArgument(0, String.name, InputResolutionStatus.RESOLVED,
                true, [], 'fixture', InputRecipeNode.builder('literal')
                .executorReady(true).literalKind('string').value(id).build())
        inputWithRecipe(saga, id, new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], [argument]))
    }

    private static InputVariant blockedInput(String saga, String id) {
        def argument = new InputRecipeArgument(0, String.name, InputResolutionStatus.UNRESOLVED,
                false, ['UNRESOLVED_VALUE'], 'fixture', InputRecipeNode.builder('unresolved')
                .executorReady(false).blockers(['UNRESOLVED_VALUE']).build())
        inputWithRecipe(saga, id, new InputRecipe(InputRecipe.SCHEMA_VERSION, null, false,
                ['UNRESOLVED_VALUE'], [argument]))
    }

    private static InputVariant inputWithRecipe(String saga, String id, InputRecipe recipe) {
        InputVariantNormalizer.normalizeForArtifact(new InputVariant(id, saga, 'Spec', id, 'saga',
                InputResolutionStatus.RESOLVED, 'new Saga()', 'source', [], [:], [], recipe))
    }

    private static SourceAggregateKeyInputEvidence source(InputVariant input,
                                                          GroovySourceValueReference reference) {
        source(input, 0, [], reference)
    }

    private static SourceAggregateKeyInputEvidence source(InputVariant input,
                                                          int argumentIndex,
                                                          List<String> propertyPath,
                                                          GroovySourceValueReference reference) {
        new SourceAggregateKeyInputEvidence(input.sagaFqn(), input.sourceClassFqn(), input.sourceMethodName(),
                input.callContextMethodName(), input.sourceBindingName(), argumentIndex, 'Order', reference,
                propertyPath, input.deterministicId())
    }
}
