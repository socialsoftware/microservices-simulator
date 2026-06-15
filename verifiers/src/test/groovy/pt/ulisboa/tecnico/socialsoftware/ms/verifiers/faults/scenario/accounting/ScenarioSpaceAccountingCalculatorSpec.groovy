package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputTupleJoiner
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScheduleEnumerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import spock.lang.Specification

class ScenarioSpaceAccountingCalculatorSpec extends Specification {

    def 'compressed brute force counts all input-bound rows and top contributors deterministically'() {
        given:
        def report = calculate(
                [saga('saga.A', 2), saga('saga.B', 1), saga('saga.C', 1)],
                [input('saga.A', 'a1', [orderId: '1']), input('saga.A', 'a2', [orderId: '2']),
                 input('saga.B', 'b1', [orderId: '1']), input('saga.B', 'b2', [orderId: '3']),
                 input('saga.C', 'c1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        true,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000),
                7)

        expect:
        report.inputBoundScenarioSpace().allInputBound().total() == '10'
        report.inputBoundScenarioSpace().allInputBound().bySagaSetSize() == ['1': '5', '2': '5']
        report.inputBoundScenarioSpace().selectedByGenerator().total() == '10'
        report.inputBoundScenarioSpace().catalogWritten().total() == '7'

        and:
        report.groupedSagaSets()*.sagaSetKey() == [
                'saga.A', 'saga.B', 'saga.C',
                'saga.A|saga.B', 'saga.A|saga.C', 'saga.B|saga.C']
        row(report, 'saga.A|saga.B').compatibleInputTupleCount() == '1'
        row(report, 'saga.A|saga.B').scenarioShapeCount() == '1'
        row(report, 'saga.A|saga.B').strictInteractionSummary().evidenceKindCounts().isEmpty()
        row(report, 'saga.A|saga.B').selectedByConfiguredGenerator()

        and:
        report.topContributors()*.sagaSetKey().take(2) == ['saga.A', 'saga.A|saga.C']
        report.topContributors()*.representedScenarioShapeCount().take(2) == ['2', '2']
    }

    def 'order preserving schedule formula matches materialized small enumerations'() {
        given:
        def report = calculate(
                [saga('saga.A', 2), saga('saga.B', 1)],
                [input('saga.A', 'a1', [orderId: '1']), input('saga.B', 'b1', [orderId: '1'])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        100,
                        ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                        100000),
                0)
        def materialized = ScheduleEnumerator.enumerate([
                new ScheduleEnumerator.SagaScheduleInput('a', 'saga.A', saga('saga.A', 2).steps()),
                new ScheduleEnumerator.SagaScheduleInput('b', 'saga.B', saga('saga.B', 1).steps())
        ], ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING, 100, 1234L)

        expect:
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == materialized.schedules().size().toString()
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '3'
        row(report, 'saga.A|saga.B').scenarioShapeCount() == '3'
    }

    def 'input and schedule bounds shape all input-bound and selected brute force counts'() {
        given:
        def report = calculate(
                [saga('saga.A', 2), saga('saga.B', 1)],
                [input('saga.A', 'a1', [orderId: '1']), input('saga.A', 'a2', [orderId: '1']), input('saga.B', 'b1', [orderId: '1'])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        1,
                        2,
                        ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                        100000),
                0)

        expect:
        row(report, 'saga.A|saga.B').inputCountsBySaga() == ['saga.A': 1, 'saga.B': 1]
        row(report, 'saga.A|saga.B').compatibleInputTupleCount() == '1'
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '2'
        report.inputBoundScenarioSpace().allInputBound().total() == '2'
        report.inputBoundScenarioSpace().selectedByGenerator().total() == '2'
    }

    def 'include singles toggle excludes set-size one rows consistently'() {
        given:
        def report = calculate(
                [saga('saga.A', 1), saga('saga.B', 1)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000),
                0)

        expect:
        report.groupedSagaSets()*.sagaSetSize().unique() == [2]
        report.inputBoundScenarioSpace().allInputBound().bySagaSetSize() == ['2': '1']
    }

    def 'lower grouped row threshold fails fast'() {
        when:
        calculate(
                [saga('saga.A', 1), saga('saga.B', 1), saga('saga.C', 1)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:]), input('saga.C', 'c1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        true,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        5),
                0)

        then:
        def error = thrown(IllegalStateException)
        error.message.contains('maxGroupedSagaSetRows=5')
    }

    def 'default grouped row threshold is at least one hundred thousand'() {
        expect:
        new ScenarioGeneratorConfig().maxGroupedSagaSetRows() >= 100000
    }

    def 'counts serialize as decimal strings'() {
        given:
        def report = calculate(
                [saga('saga.A', 1), saga('saga.B', 1)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        Integer.MAX_VALUE,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000),
                0)

        when:
        def json = new ObjectMapper().writeValueAsString(report)

        then:
        json.contains('"total":"1"')
        json.contains('"scenarioShapeCount":"1"')
        !json.contains('"scenarioShapeCount":1')
    }

    def 'compatible input tuple counts use arbitrary precision beyond integer range'() {
        given:
        def manyA = (1..50_000).collect { input('saga.A', "a${it}".toString(), [:]) }
        def manyB = (1..50_000).collect { input('saga.B', "b${it}".toString(), [:]) }

        when:
        def report = calculate(
                [saga('saga.A', 1), saga('saga.B', 1)],
                manyA + manyB,
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        50_000,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000),
                0)

        then:
        row(report, 'saga.A|saga.B').compatibleInputTupleCount() == '2500000000'
        row(report, 'saga.A|saga.B').scenarioShapeCount() == '2500000000'
        report.inputBoundScenarioSpace().allInputBound().total() == '2500000000'
    }

    def 'compatible input tuple counting matches explicit enumeration for exact contradictions and missing evidence'() {
        given:
        def sagaSet = ['saga.A', 'saga.B']
        def inputsBySaga = [
                'saga.A': [
                        input('saga.A', 'a-order-1-item-1', [orderId: '1', itemId: '1']),
                        input('saga.A', 'a-order-1-item-2', [orderId: '1', itemId: '2']),
                        input('saga.A', 'a-order-2-item-1', [orderId: '2', itemId: '1']),
                        input('saga.A', 'a-unknown', [:])],
                'saga.B': [
                        input('saga.B', 'b-order-1-item-1', [orderId: '1', itemId: '1']),
                        input('saga.B', 'b-order-1-item-2', [orderId: '1', itemId: '2']),
                        input('saga.B', 'b-order-2-item-2', [orderId: '2', itemId: '2']),
                        input('saga.B', 'b-order-2-missing-item', [orderId: '2']),
                        input('saga.B', 'b-unknown', [:])]
        ]
        def explicit = InputTupleJoiner.join(sagaSet, inputsBySaga)

        expect:
        new ScenarioSpaceAccountingCalculator().countCompatibleInputTuples(sagaSet, inputsBySaga) == explicit.tuples().size()
        new ScenarioSpaceAccountingCalculator().countCompatibleInputTuples(sagaSet, inputsBySaga).toString() == '11'
    }

    def 'compatible input tuple counting uses simple multiplication when exact bindings are absent'() {
        given:
        def sagaSet = ['saga.A', 'saga.B', 'saga.C']
        def inputsBySaga = [
                'saga.A': (1..3).collect { input('saga.A', "a${it}".toString(), [:]) },
                'saga.B': (1..4).collect { input('saga.B', "b${it}".toString(), [:]) },
                'saga.C': (1..2).collect { input('saga.C', "c${it}".toString(), [:]) }
        ]

        expect:
        new ScenarioSpaceAccountingCalculator().countCompatibleInputTuples(sagaSet, inputsBySaga).toString() == '24'
    }

    def 'compatible input tuple count feeds grouped rows and all input-bound totals'() {
        when:
        def report = calculate(
                [saga('saga.A', 2), saga('saga.B', 1)],
                [input('saga.A', 'a-order-1-item-1', [orderId: '1', itemId: '1']),
                 input('saga.A', 'a-order-1-item-2', [orderId: '1', itemId: '2']),
                 input('saga.A', 'a-order-2-item-1', [orderId: '2', itemId: '1']),
                 input('saga.A', 'a-unknown', [:]),
                 input('saga.B', 'b-order-1-item-1', [orderId: '1', itemId: '1']),
                 input('saga.B', 'b-order-1-item-2', [orderId: '1', itemId: '2']),
                 input('saga.B', 'b-order-2-item-2', [orderId: '2', itemId: '2']),
                 input('saga.B', 'b-order-2-missing-item', [orderId: '2']),
                 input('saga.B', 'b-unknown', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                        100000),
                0)

        then:
        row(report, 'saga.A|saga.B').compatibleInputTupleCount() == '11'
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '3'
        row(report, 'saga.A|saga.B').scenarioShapeCount() == '33'
        report.inputBoundScenarioSpace().allInputBound().total() == '33'
    }

    def 'global write cap affects catalog written but not input-bound or selected counts'() {
        when:
        def report = calculate(
                [saga('saga.A', 1), saga('saga.B', 1)],
                [input('saga.A', 'a1', [:]), input('saga.A', 'a2', [:]), input('saga.B', 'b1', [:]), input('saga.B', 'b2', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000,
                        false,
                        1),
                1)

        then:
        row(report, 'saga.A|saga.B').scenarioShapeCount() == '4'
        report.inputBoundScenarioSpace().allInputBound().total() == '4'
        report.inputBoundScenarioSpace().selectedByGenerator().total() == '4'
        report.inputBoundScenarioSpace().catalogWritten().total() == '1'
        report.runConfig().maxScenarios() == 1
    }

    def 'segment compressed accounting matches small tuple interleaving behavior'() {
        when:
        def report = calculate(
                [saga('saga.A', 2), saga('saga.B', 2)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                        100000),
                0)
        def materialized = ScheduleEnumerator.enumerate([
                new ScheduleEnumerator.SagaScheduleInput('a', 'saga.A', saga('saga.A', 2).steps()),
                new ScheduleEnumerator.SagaScheduleInput('b', 'saga.B', saga('saga.B', 2).steps())
        ], ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED, 20, 1234L)

        then:
        report.runConfig().scheduleStrategy() == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED
        report.runConfig().effectiveSegmentBehavior().contains('totalSteps<=12')
        report.runConfig().effectiveSegmentBehavior().contains('SERIAL fallback')
        report.runConfig().effectiveSegmentBehavior().contains('not thesis-style segment compression')
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == materialized.schedules().size().toString()
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '6'
        row(report, 'saga.A|saga.B').scenarioShapeCount() == '6'
    }

    def 'segment compressed accounting matches large tuple serial fallback behavior'() {
        when:
        def report = calculate(
                [saga('saga.A', 7), saga('saga.B', 6)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        1000,
                        ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                        100000),
                0)
        def materialized = ScheduleEnumerator.enumerate([
                new ScheduleEnumerator.SagaScheduleInput('a', 'saga.A', saga('saga.A', 7).steps()),
                new ScheduleEnumerator.SagaScheduleInput('b', 'saga.B', saga('saga.B', 6).steps())
        ], ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED, 1000, 1234L)

        then:
        materialized.warnings().contains('segment-compressed schedule strategy fell back to SERIAL for large tuples')
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == materialized.schedules().size().toString()
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '1'
        row(report, 'saga.A|saga.B').scenarioShapeCount() == '1'
    }

    def 'executor readiness summarizes accepted input recipes without scenario-level admissibility'() {
        given:
        def readyRecipe = recipe(true, [])
        def blockedRecipe = recipe(false, ['UNRESOLVED_VALUE', 'MISSING_TARGET_TYPE'])

        when:
        def report = calculate(
                [saga('saga.A', 1), saga('saga.B', 1)],
                [input('saga.A', 'a-ready', [:], readyRecipe),
                 input('saga.A', 'a-blocked', [:], blockedRecipe),
                 input('saga.B', 'b-no-recipe', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        true,
                        1,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000),
                0)
        def json = new ObjectMapper().writeValueAsString(report)

        then:
        report.executorReadiness().acceptedInputVariantCount() == 3
        report.executorReadiness().executorReadyInputVariantCount() == 1
        report.executorReadiness().blockedInputVariantCount() == 2
        report.executorReadiness().blockerReasonCounts() == [MISSING_TARGET_TYPE: 1, UNRESOLVED_VALUE: 1]

        and:
        json.contains('"executorReadiness"')
        !json.contains('scenarioExecutorReadiness')
        !json.contains('scenarioAdmissibility')
    }

    def 'executor readiness uses normalized accepted inputs before known saga filtering'() {
        given:
        def readyRecipe = recipe(true, [])
        def blockedRecipe = recipe(false, ['UNKNOWN_SAGA_RECIPE_BLOCKER'])
        def rejectedRecipe = recipe(false, ['REJECTED_SOURCE_MODE_BLOCKER'])

        when:
        def report = calculate(
                [saga('saga.A', 1)],
                [input('saga.A', 'a-ready', [:], readyRecipe),
                 input('saga.Unknown', 'unknown-blocked', [:], blockedRecipe),
                 input('saga.A', 'a-tcc-rejected', [:], rejectedRecipe, SourceMode.TCC)],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        true,
                        1,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000),
                0)

        then:
        report.executorReadiness().acceptedInputVariantCount() == 2
        report.executorReadiness().executorReadyInputVariantCount() == 1
        report.executorReadiness().blockedInputVariantCount() == 1
        report.executorReadiness().blockerReasonCounts() == [UNKNOWN_SAGA_RECIPE_BLOCKER: 1]

        and:
        report.groupedSagaSets()*.sagaSetKey() == ['saga.A']
    }

    def 'type level coverage reports strict and broad missing input diagnostics'() {
        when:
        def report = calculate(
                [footprintSaga('saga.A', 'Order', 'order-1', FootprintConfidence.EXACT),
                 footprintSaga('saga.B', 'Order', 'order-1', FootprintConfidence.EXACT),
                 footprintSaga('saga.C', 'Order', null, FootprintConfidence.TYPE_ONLY),
                 footprintSaga('saga.D', 'Product', 'product-1', FootprintConfidence.EXACT)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:]), input('saga.D', 'd1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000),
                0)

        then:
        report.typeLevelCoverage().discoveredSagaFqns() == ['saga.A', 'saga.B', 'saga.C', 'saga.D']
        report.typeLevelCoverage().discoveredSagaCount() == 4
        report.typeLevelCoverage().sagasWithAcceptedInputs() == ['saga.A', 'saga.B', 'saga.D']
        report.typeLevelCoverage().sagasWithoutAcceptedInputs() == ['saga.C']

        and:
        report.typeLevelCoverage().strict().interactionPairCount() == 1
        report.typeLevelCoverage().strict().inputCoveredInteractionPairCount() == 1
        report.typeLevelCoverage().strict().missingInputInteractionPairCount() == 0
        report.typeLevelCoverage().broad().interactionPairCount() == 3
        report.typeLevelCoverage().broad().inputCoveredInteractionPairCount() == 1
        report.typeLevelCoverage().broad().missingInputInteractionPairCount() == 2
        report.typeLevelCoverage().broad().connectedSetCountsBySize()['2'] == '3'

        and:
        row(report, 'saga.A|saga.B').selectedByConfiguredGenerator()
        !row(report, 'saga.A|saga.D').selectedByConfiguredGenerator()
        row(report, 'saga.A|saga.B').strictInteractionSummary().directPairCount() == 1
        row(report, 'saga.A|saga.B').strictInteractionSummary().evidenceKindCounts()['WRITE_WRITE'] == 1
    }

    def 'interaction pruned selection can use broad graph while pruning unrelated aggregates'() {
        when:
        def report = calculate(
                [footprintSaga('saga.A', 'Order', 'order-1', FootprintConfidence.EXACT),
                 footprintSaga('saga.B', 'Order', 'order-1', FootprintConfidence.EXACT),
                 footprintSaga('saga.C', 'Order', null, FootprintConfidence.TYPE_ONLY),
                 footprintSaga('saga.D', 'Product', 'product-1', FootprintConfidence.EXACT)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:]), input('saga.C', 'c1', [:]), input('saga.D', 'd1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000,
                        true),
                0)

        then:
        row(report, 'saga.A|saga.B').selectedByConfiguredGenerator()
        row(report, 'saga.A|saga.C').selectedByConfiguredGenerator()
        row(report, 'saga.B|saga.C').selectedByConfiguredGenerator()
        !row(report, 'saga.A|saga.D').selectedByConfiguredGenerator()
        !row(report, 'saga.C|saga.D').selectedByConfiguredGenerator()

        and:
        row(report, 'saga.A|saga.C').strictInteractionSummary().connected() == false
        row(report, 'saga.A|saga.C').broadInteractionSummary().connected() == true
        row(report, 'saga.A|saga.C').broadInteractionSummary().evidenceKindCounts()['TYPE_ONLY'] == 1
        report.inputBoundScenarioSpace().selectedByGenerator().total() == '3'
        report.inputBoundScenarioSpace().allInputBound().total() == '6'
    }

    def 'row direct pair count deduplicates multiple evidence candidates for the same saga pair'() {
        when:
        def report = calculate(
                [footprintSaga('saga.A', [
                        footprint('Order', 'order-1', FootprintConfidence.EXACT),
                        footprint('Order', 'order-1', FootprintConfidence.EXACT)]),
                 footprintSaga('saga.B', [footprint('Order', 'order-1', FootprintConfidence.EXACT)])],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                        100000),
                0)

        then:
        row(report, 'saga.A|saga.B').strictInteractionSummary().directPairCount() == 1
        row(report, 'saga.A|saga.B').strictInteractionSummary().evidenceKindCounts()['WRITE_WRITE'] == 2
    }

    private static ScenarioSpaceAccountingReport calculate(List<SagaDefinition> sagas,
                                                           List<InputVariant> inputs,
                                                           ScenarioGeneratorConfig config,
                                                           int catalogWritten) {
        new ScenarioSpaceAccountingCalculator().calculate('dummyapp', sagas, inputs, config, catalogWritten)
    }

    private static ScenarioSpaceAccountingReport.GroupedSagaSetRow row(ScenarioSpaceAccountingReport report, String key) {
        report.groupedSagaSets().find { it.sagaSetKey() == key }
    }

    private static SagaDefinition saga(String fqn, int stepCount) {
        new SagaDefinition(fqn, (0..<stepCount).collect { step(fqn, it) }, [])
    }

    private static StepDefinition step(String sagaFqn, int index) {
        new StepDefinition("${sagaFqn}.step.${index}".toString(), "step${index}".toString(), "step${index}".toString(), index, [], [], [])
    }

    private static SagaDefinition footprintSaga(String fqn, String aggregateName, String keyText, FootprintConfidence confidence) {
        footprintSaga(fqn, [footprint(aggregateName, keyText, confidence)])
    }

    private static SagaDefinition footprintSaga(String fqn, List<StepFootprint> footprints) {
        new SagaDefinition(fqn, footprints.withIndex().collect { footprint, index ->
            new StepDefinition("${fqn}.step.${index}".toString(), "step${index}".toString(), "step${index}".toString(), index, [], [footprint], [])
        }, [])
    }

    private static StepFootprint footprint(String aggregateName, String keyText, FootprintConfidence confidence) {
        new StepFootprint(new AggregateKey(null, aggregateName, keyText, confidence), AccessMode.WRITE, [])
    }

    private static InputVariant input(String sagaFqn, String id, Map<String, String> keys) {
        new InputVariant(id, sagaFqn, 'Spec', id, 'saga', InputResolutionStatus.RESOLVED, 'new Saga()', 'saga', [], keys, [])
    }

    private static InputVariant input(String sagaFqn, String id, Map<String, String> keys, InputRecipe recipe) {
        new InputVariant(id, sagaFqn, 'Spec', id, 'saga', InputResolutionStatus.RESOLVED, 'new Saga()', 'saga', [], keys, [], recipe)
    }

    private static InputVariant input(String sagaFqn, String id, Map<String, String> keys, InputRecipe recipe, SourceMode sourceMode) {
        new InputVariant(id,
                sagaFqn,
                'Spec',
                id,
                'saga',
                InputResolutionStatus.RESOLVED,
                sourceMode,
                SourceModeConfidence.TYPE_EVIDENCE,
                ['source-mode evidence'],
                'new Saga()',
                'saga',
                [],
                [],
                keys,
                [],
                recipe)
    }

    private static InputRecipe recipe(boolean executorReady, List<String> blockers) {
        new InputRecipe(InputRecipe.SCHEMA_VERSION, null, executorReady, blockers, [])
    }

    private static ScenarioGeneratorConfig config(ScenarioGeneratorConfig.GenerationStrategy generationStrategy,
                                                  boolean includeSingles,
                                                  int maxSagaSetSize,
                                                  int maxInputVariantsPerSaga,
                                                  int maxSchedulesPerInputTuple,
                                                  ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy,
                                                  int maxGroupedRows) {
        config(generationStrategy, includeSingles, maxSagaSetSize, maxInputVariantsPerSaga, maxSchedulesPerInputTuple,
                scheduleStrategy, maxGroupedRows, false)
    }

    private static ScenarioGeneratorConfig config(ScenarioGeneratorConfig.GenerationStrategy generationStrategy,
                                                   boolean includeSingles,
                                                   int maxSagaSetSize,
                                                   int maxInputVariantsPerSaga,
                                                   int maxSchedulesPerInputTuple,
                                                   ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy,
                                                   int maxGroupedRows,
                                                   boolean allowTypeOnlyFallback) {
        config(generationStrategy, includeSingles, maxSagaSetSize, maxInputVariantsPerSaga, maxSchedulesPerInputTuple,
                scheduleStrategy, maxGroupedRows, allowTypeOnlyFallback, 100)
    }

    private static ScenarioGeneratorConfig config(ScenarioGeneratorConfig.GenerationStrategy generationStrategy,
                                                   boolean includeSingles,
                                                   int maxSagaSetSize,
                                                   int maxInputVariantsPerSaga,
                                                   int maxSchedulesPerInputTuple,
                                                   ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy,
                                                   int maxGroupedRows,
                                                   boolean allowTypeOnlyFallback,
                                                   int maxScenarios) {
        new ScenarioGeneratorConfig(false,
                generationStrategy,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_PLANS,
                includeSingles,
                maxSagaSetSize,
                maxScenarios,
                maxInputVariantsPerSaga,
                maxSchedulesPerInputTuple,
                allowTypeOnlyFallback,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                scheduleStrategy,
                1234L,
                maxGroupedRows)
    }
}
