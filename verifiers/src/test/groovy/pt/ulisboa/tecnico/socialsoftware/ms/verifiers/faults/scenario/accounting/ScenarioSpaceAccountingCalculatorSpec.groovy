package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorMaterializationPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputTupleJoiner
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleCap
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScheduleEnumerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationEvidenceClass
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadGenerationResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan
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
        report.runConfig().maxCatalogScenarios() == 1
    }

    def 'segment compressed accounting counts conflict-anchor interleavings instead of internal step interleavings'() {
        when:
        def report = calculate(
                [segmentSaga('saga.A', 1, AccessMode.WRITE), segmentSaga('saga.B', 1, AccessMode.READ)],
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
                new ScheduleEnumerator.SagaScheduleInput('a', 'saga.A', segmentSaga('saga.A', 1, AccessMode.WRITE).steps()),
                new ScheduleEnumerator.SagaScheduleInput('b', 'saga.B', segmentSaga('saga.B', 1, AccessMode.READ).steps())
        ], ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED, 20, 1234L)

        then:
        report.runConfig().scheduleStrategy() == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED
        report.runConfig().effectiveSegmentBehavior().contains('conflict-anchor segment compression')
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == materialized.schedules().size().toString()
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '2'
        row(report, 'saga.A|saga.B').scenarioShapeCount() == '2'
    }

    def 'segment compressed accounting does not fall back to serial above the old step cutoff'() {
        when:
        def report = calculate(
                [segmentSaga('saga.A', 6, AccessMode.WRITE), segmentSaga('saga.B', 5, AccessMode.READ)],
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
                new ScheduleEnumerator.SagaScheduleInput('a', 'saga.A', segmentSaga('saga.A', 6, AccessMode.WRITE).steps()),
                new ScheduleEnumerator.SagaScheduleInput('b', 'saga.B', segmentSaga('saga.B', 5, AccessMode.READ).steps())
        ], ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED, 1000, 1234L)

        then:
        materialized.schedules().size() == 2
        materialized.warnings().isEmpty()
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == materialized.schedules().size().toString()
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '2'
        row(report, 'saga.A|saga.B').scenarioShapeCount() == '2'
    }

    def 'segment compressed accounting ignores external conflict anchors outside the grouped saga set'() {
        when:
        def report = calculate(
                [segmentSaga('saga.A', 1, AccessMode.WRITE), saga('saga.B', 2), segmentSaga('saga.C', 1, AccessMode.READ)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:]), input('saga.C', 'c1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                        100000),
                0)

        then:
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '1'
        row(report, 'saga.A|saga.C').scheduleCountPerTuple() == '2'
    }

    def 'segment compressed anchors follow strict and broad configured conflict lenses'() {
        when:
        def strict = calculate(
                [segmentSaga('saga.A', 1, AccessMode.WRITE, FootprintConfidence.TYPE_ONLY, null),
                 segmentSaga('saga.B', 1, AccessMode.READ, FootprintConfidence.TYPE_ONLY, null)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                        100000,
                        false),
                0)
        def broad = calculate(
                [segmentSaga('saga.A', 1, AccessMode.WRITE, FootprintConfidence.TYPE_ONLY, null),
                 segmentSaga('saga.B', 1, AccessMode.READ, FootprintConfidence.TYPE_ONLY, null)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                        100000,
                        true),
                0)

        then:
        row(strict, 'saga.A|saga.B').scheduleCountPerTuple() == '1'
        row(strict, 'saga.A|saga.B').broadInteractionSummary().evidenceKindCounts()['TYPE_ONLY'] == 1
        row(broad, 'saga.A|saga.B').scheduleCountPerTuple() == '2'
        row(broad, 'saga.A|saga.B').broadInteractionSummary().evidenceKindCounts()['TYPE_ONLY'] == 1
    }

    def 'segment compressed does not create anchors for read read footprint pairs'() {
        when:
        def report = calculate(
                [segmentSaga('saga.A', 1, AccessMode.READ), segmentSaga('saga.B', 1, AccessMode.READ)],
                [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])],
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                        100000),
                0)

        then:
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '1'
        row(report, 'saga.A|saga.B').strictInteractionSummary().evidenceKindCounts().isEmpty()
    }

    def 'schedule strategy does not change interaction pruned saga set selection'() {
        given:
        def sagas = [
                segmentSaga('saga.A', 1, AccessMode.WRITE),
                segmentSaga('saga.B', 1, AccessMode.READ),
                footprintSaga('saga.C', 'Product', 'product-1', FootprintConfidence.EXACT)
        ]
        def inputs = [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:]), input('saga.C', 'c1', [:])]

        when:
        def interleaving = calculate(sagas, inputs,
                config(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                        100000),
                0)
        def compressed = calculate(sagas, inputs,
                config(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                        100000),
                0)

        then:
        selectedRows(compressed) == selectedRows(interleaving)
        selectedRows(compressed) == ['saga.A|saga.B']
        row(interleaving, 'saga.A|saga.B').scheduleCountPerTuple() == '6'
        row(compressed, 'saga.A|saga.B').scheduleCountPerTuple() == '2'
    }

    def 'segment compressed brute force selection remains input-bound instead of interaction-pruned'() {
        given:
        def sagas = [segmentSaga('saga.A', 1, AccessMode.WRITE), saga('saga.B', 2), segmentSaga('saga.C', 1, AccessMode.READ)]
        def inputs = [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:]), input('saga.C', 'c1', [:])]

        when:
        def report = calculate(sagas, inputs,
                config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                        false,
                        2,
                        10,
                        20,
                        ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                        100000),
                0)

        then:
        selectedRows(report) == ['saga.A|saga.B', 'saga.A|saga.C', 'saga.B|saga.C']
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '1'
        row(report, 'saga.A|saga.C').scheduleCountPerTuple() == '2'
        row(report, 'saga.B|saga.C').scheduleCountPerTuple() == '1'
    }

    def 'segment compressed accounting selected total matches fully materialized generator output'() {
        given:
        def sagas = [segmentSaga('saga.A', 1, AccessMode.WRITE), segmentSaga('saga.B', 1, AccessMode.READ)]
        def inputs = [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])]
        def segmentConfig = config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                false,
                2,
                10,
                20,
                ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                100000)

        when:
        def generated = ScenarioGenerator.generate(sagas, inputs, segmentConfig)
        def report = calculate(sagas, inputs, segmentConfig, generated.workloadPlans().size())

        then:
        generated.workloadPlans().size().toString() == report.inputBoundScenarioSpace().selectedByGenerator().total()
        generated.workloadPlans().size() == 2
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '2'
    }

    def 'segment compressed accounting and materialized generation apply schedule caps consistently'() {
        given:
        def sagas = [multiAnchorSaga('saga.A', AccessMode.WRITE), multiAnchorSaga('saga.B', AccessMode.READ)]
        def inputs = [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])]
        def cappedConfig = config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                false,
                2,
                10,
                3,
                ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                100000)

        when:
        def generated = ScenarioGenerator.generate(sagas, inputs, cappedConfig)
        def report = calculate(sagas, inputs, cappedConfig, generated.workloadPlans().size())

        then:
        generated.workloadPlans().size() == 3
        generated.counts().schedulesCapped == 1
        generated.warnings().any { it.contains('maxSchedulesPerInputTuple=3') }
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '3'
        report.inputBoundScenarioSpace().selectedByGenerator().total() == '3'
    }

    def 'segment compressed zero schedule cap disables accounting and materialized schedules'() {
        given:
        def sagas = [segmentSaga('saga.A', 1, AccessMode.WRITE), segmentSaga('saga.B', 1, AccessMode.READ)]
        def inputs = [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:])]
        def zeroCapConfig = config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                false,
                2,
                10,
                0,
                ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                100000)

        when:
        def generated = ScenarioGenerator.generate(sagas, inputs, zeroCapConfig)
        def report = calculate(sagas, inputs, zeroCapConfig, generated.workloadPlans().size())

        then:
        generated.workloadPlans().isEmpty()
        generated.counts().schedulesEmitted == 0
        generated.warnings().contains('schedule cap disabled all schedules')
        row(report, 'saga.A|saga.B').scheduleCountPerTuple() == '0'
        report.inputBoundScenarioSpace().selectedByGenerator().total() == '0'
    }

    def 'segment compressed accounting ordering totals and behavior text are stable and non-placeholder'() {
        given:
        def sagas = [segmentSaga('saga.A', 1, AccessMode.WRITE), saga('saga.B', 2), segmentSaga('saga.C', 1, AccessMode.READ)]
        def inputs = [input('saga.A', 'a1', [:]), input('saga.B', 'b1', [:]), input('saga.C', 'c1', [:])]
        def segmentConfig = config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                false,
                2,
                10,
                20,
                ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,
                100000)

        when:
        def first = calculate(sagas, inputs, segmentConfig, 0)
        def second = calculate(sagas.reverse(), inputs.reverse(), segmentConfig, 0)

        then:
        first.groupedSagaSets()*.sagaSetKey() == second.groupedSagaSets()*.sagaSetKey()
        first.groupedSagaSets()*.scheduleCountPerTuple() == second.groupedSagaSets()*.scheduleCountPerTuple()
        first.inputBoundScenarioSpace().selectedByGenerator().total() == second.inputBoundScenarioSpace().selectedByGenerator().total()
        first.runConfig().effectiveSegmentBehavior().contains('conflict-anchor segment compression')
        !first.runConfig().effectiveSegmentBehavior().contains('not thesis-style')
        !first.runConfig().effectiveSegmentBehavior().contains('SERIAL fallback')
        !first.runConfig().effectiveSegmentBehavior().contains('totalSteps<=12')
    }

    def 'executor readiness summarizes executor materializability without scenario-level admissibility'() {
        given:
        def runtimeOwnedRecipe = recipe(false, ['UNRESOLVED_VARIABLE'], [
                arg(0, ScenarioExecutorMaterializationPolicy.SAGA_UNIT_OF_WORK, false, ['UNRESOLVED_VARIABLE'], unresolved())
        ])
        def blockedRecipe = recipe(false, ['UNRESOLVED_VALUE'], [
                arg(0, Integer.name, false, ['UNRESOLVED_VALUE'], unresolved())
        ])

        when:
        def report = calculate(
                [saga('saga.A', 1), saga('saga.B', 1)],
                [input('saga.A', 'a-runtime-owned', [:], runtimeOwnedRecipe),
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
        report.executorReadiness().executorMaterializableInputVariantCount() == 1
        report.executorReadiness().executorReadyInputVariantCount() == 1
        report.executorReadiness().staticRecipeReadyInputVariantCount() == 0
        report.executorReadiness().blockedInputVariantCount() == 2
        report.executorReadiness().blockerReasonCounts() == [MISSING_INPUT_RECIPE: 1, UNRESOLVED_VALUE: 1]
        report.executorReadiness().runtimeOwnedResolutionCounts() == [(ScenarioExecutorMaterializationPolicy.SAGA_UNIT_OF_WORK): 1]

        and:
        json.contains('"executorReadiness"')
        json.contains('"executorMaterializableInputVariantCount":1')
        json.contains('"staticRecipeReadyInputVariantCount":0')
        !json.contains('scenarioExecutorReadiness')
        !json.contains('scenarioAdmissibility')
    }

    def 'executor readiness uses normalized accepted inputs before known saga filtering'() {
        given:
        def readyRecipe = recipe(true, [])
        def blockedRecipe = recipe(false, ['UNKNOWN_SAGA_RECIPE_BLOCKER'], [
                arg(0, Integer.name, false, ['UNKNOWN_SAGA_RECIPE_BLOCKER'], unresolved())
        ])
        def rejectedRecipe = recipe(false, ['REJECTED_SOURCE_MODE_BLOCKER'], [
                arg(0, Integer.name, false, ['REJECTED_SOURCE_MODE_BLOCKER'], unresolved())
        ])

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
        report.executorReadiness().executorMaterializableInputVariantCount() == 1
        report.executorReadiness().executorReadyInputVariantCount() == 1
        report.executorReadiness().staticRecipeReadyInputVariantCount() == 1
        report.executorReadiness().blockedInputVariantCount() == 1
        report.executorReadiness().blockerReasonCounts() == [UNKNOWN_SAGA_RECIPE_BLOCKER: 1]

        and:
        report.groupedSagaSets()*.sagaSetKey() == ['saga.A']
    }

    def 'eager baseline emits all-zero and every single-point vector only for input-ready structurally admissible workloads'() {
        given:
        def generatorConfig = config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                true,
                1,
                10,
                20,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                100000)
        def eligible = ScenarioGenerator.generate(
                [saga('saga.Ready', 2)],
                [input('saga.Ready', 'ready', [:], recipe(true, []))],
                generatorConfig).workloadPlans().first()
        def blocked = ScenarioGenerator.generate(
                [saga('saga.Blocked', 1)],
                [input('saga.Blocked', 'blocked', [:], recipe(false, ['UNRESOLVED_VALUE'], [
                        arg(0, Integer.name, false, ['UNRESOLVED_VALUE'], unresolved())
                ]))],
                generatorConfig).workloadPlans().first()
        def malformed = new WorkloadPlan(
                eligible.schemaVersion(),
                'malformed-workload',
                eligible.kind(),
                eligible.executionShape(),
                eligible.participants(),
                eligible.acceptedInputs(),
                eligible.forwardSchedule(),
                eligible.conflictEvidence(),
                eligible.faultSlots().dropRight(1),
                eligible.compensationCheckpoints(),
                eligible.warnings())
        def workloads = new WorkloadGenerationResult(
                WorkloadPlan.SCHEMA_VERSION,
                generatorConfig,
                [eligible, blocked, malformed],
                [],
                [:],
                [])

        when:
        def eager = EagerFaultScenarioGenerator.generate(workloads, new RecoveryScheduleCap(2))
        def readinessById = eager.workloadMaterializability().collectEntries { [(it.workloadPlanId()): it] }

        then:
        eager.workloadPlans()*.deterministicId() == [eligible, blocked, malformed]*.deterministicId().sort()
        readinessById[eligible.deterministicId()].materializable()
        !readinessById[blocked.deterministicId()].materializable()
        readinessById[blocked.deterministicId()].diagnostics().any { it.contains('UNRESOLVED_VALUE') }
        !readinessById[malformed.deterministicId()].materializable()
        readinessById[malformed.deterministicId()].diagnostics().any { it.contains('FAULT_SPACE_SHAPE_MISMATCH') }

        and:
        eager.computedVectors()*.assignedVector() == ['00', '10', '01']
        eager.computedVectors()*.writtenScheduleCount() == [1, 1, 1]
        eager.faultScenarios().size() == 3
        eager.faultScenarios().find { it.assignedVector() == '00' }.actions()*.kind().unique() == [FaultScenarioActionKind.FORWARD]
        eager.faultScenarios().every { it.workloadPlanId() == eligible.deterministicId() }
        eager.recoveryScheduleCap() == 2
    }

    def 'eager recovery cap applies independently to each vector'() {
        given:
        def checkpointStep = new StepDefinition(
                'saga.A::a1', 'a1', 'a1', 0, [], [], [],
                true, true, true, CompensationEvidenceClass.EXPLICIT_COMPENSATION, [], [])
        def sagaA = new SagaDefinition('saga.A', [checkpointStep,
                                                   new StepDefinition('saga.A::a2', 'a2', 'a2', 1, ['a1'], [], [])], [])
        def sagaB = new SagaDefinition('saga.B', [
                new StepDefinition('saga.B::b1', 'b1', 'b1', 0, [], [], []),
                new StepDefinition('saga.B::b2', 'b2', 'b2', 1, ['b1'], [], [])], [])
        def generatorConfig = config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                false,
                2,
                10,
                20,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                100000)
        def workloads = ScenarioGenerator.generate(
                [sagaA, sagaB],
                [input('saga.A', 'a', [:], recipe(true, [])), input('saga.B', 'b', [:], recipe(true, []))],
                generatorConfig)
        def plan = workloads.workloadPlans().find { it.forwardSchedule()*.runtimeStepName() == ['a1', 'a2', 'b1', 'b2'] }

        when:
        def eager = EagerFaultScenarioGenerator.generate(
                new WorkloadGenerationResult(WorkloadPlan.SCHEMA_VERSION, generatorConfig, [plan], [], [:], []),
                new RecoveryScheduleCap(2))
        def recoveryByVector = eager.computedVectors().collectEntries { [(it.assignedVector()): it] }

        then:
        recoveryByVector['0000'].uncappedScheduleCount() == BigInteger.ONE
        recoveryByVector['0000'].writtenScheduleCount() == 1
        recoveryByVector['0100'].uncappedScheduleCount() == BigInteger.valueOf(3)
        recoveryByVector['0100'].writtenScheduleCount() == 2
        eager.computedVectors().every { it.writtenScheduleCount() <= 2 }
        eager.faultScenarios().count { it.assignedVector() == '0100' } == 2
    }

    def 'catalog accounting uses exact decimal vector and computed recovery layers without an all-vector recovery claim'() {
        given:
        def slotCount = 70
        def generatorConfig = config(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                true,
                1,
                10,
                20,
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                100000)
        def saga = saga('saga.Large', slotCount)
        def input = input('saga.Large', 'large', [:], recipe(true, []))
        def workloads = ScenarioGenerator.generate([saga], [input], generatorConfig)
        def eager = EagerFaultScenarioGenerator.generate(workloads, new RecoveryScheduleCap(1))
        def base = calculate([saga], [input], generatorConfig, workloads.workloadPlans().size())

        when:
        def report = base.withCatalogPackage(eager)
        def vectorSpace = report.workloadCatalogSpace().perWorkloadVectorSpace().first()

        then:
        vectorSpace.faultSlotCount() == slotCount.toString()
        vectorSpace.possibleBinaryVectors() == BigInteger.TWO.pow(slotCount).toString()
        vectorSpace.eagerVectorCount() == (slotCount + 1).toString()
        vectorSpace.executorMaterializable()
        report.workloadCatalogSpace().materializableWorkloadPlans() == '1'
        report.workloadCatalogSpace().nonMaterializableWorkloadPlans() == '0'

        and:
        report.faultScenarioCatalogSpace().computedEagerVectorCount() == (slotCount + 1).toString()
        report.faultScenarioCatalogSpace().exactComputedVectorUncappedScheduleSum() == (slotCount + 1).toString()
        report.faultScenarioCatalogSpace().exactComputedVectorWrittenScheduleSum() == (slotCount + 1).toString()
        report.faultScenarioCatalogSpace().perComputedVectorRecoverySpace().size() == slotCount + 1
        report.faultScenarioCatalogSpace().allVectorRecoveryTotalStatus() == 'NOT_COMPUTED'
        !new ObjectMapper().writeValueAsString(report).contains('exactAllVectorRecovery')
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

    private static List<String> selectedRows(ScenarioSpaceAccountingReport report) {
        report.groupedSagaSets().findAll { it.selectedByConfiguredGenerator() }*.sagaSetKey()
    }

    private static SagaDefinition saga(String fqn, int stepCount) {
        new SagaDefinition(fqn, (0..<stepCount).collect { step(fqn, it) }, [])
    }

    private static StepDefinition step(String sagaFqn, int index) {
        new StepDefinition("${sagaFqn}::step${index}".toString(), "step${index}".toString(), "step${index}".toString(), index, [], [], [])
    }

    private static SagaDefinition segmentSaga(String fqn, int internalSteps, AccessMode anchorMode) {
        segmentSaga(fqn, internalSteps, anchorMode, FootprintConfidence.EXACT, 'shared')
    }

    private static SagaDefinition segmentSaga(String fqn, int internalSteps, AccessMode anchorMode, FootprintConfidence confidence, String keyText) {
        def steps = (0..<internalSteps).collect { index ->
            new StepDefinition("${fqn}.internal.${index}".toString(), "internal${index}".toString(), "internal${index}".toString(), index, [], [], [])
        }
        steps.add(new StepDefinition("${fqn}.conflict".toString(), 'conflict', 'conflict', internalSteps, [],
                [new StepFootprint(new AggregateKey(null, 'Order', keyText, confidence), anchorMode, [])], []))
        new SagaDefinition(fqn, steps, [])
    }

    private static SagaDefinition multiAnchorSaga(String fqn, AccessMode anchorMode) {
        new SagaDefinition(fqn, [
                new StepDefinition("${fqn}.conflict.1".toString(), 'conflict1', 'conflict1', 0, [],
                        [new StepFootprint(new AggregateKey(null, 'Order', 'shared-1', FootprintConfidence.EXACT), anchorMode, [])], []),
                new StepDefinition("${fqn}.conflict.2".toString(), 'conflict2', 'conflict2', 1, [],
                        [new StepFootprint(new AggregateKey(null, 'Order', 'shared-2', FootprintConfidence.EXACT), anchorMode, [])], [])
        ], [])
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
        recipe(executorReady, blockers, [])
    }

    private static InputRecipe recipe(boolean executorReady, List<String> blockers, List<InputRecipeArgument> arguments) {
        new InputRecipe(InputRecipe.SCHEMA_VERSION, null, executorReady, blockers, arguments)
    }

    private static InputRecipeArgument arg(int index, String type, boolean executorReady, List<String> blockers, InputRecipeNode node) {
        new InputRecipeArgument(index, type, InputResolutionStatus.UNRESOLVED, executorReady, blockers, 'spec', node)
    }

    private static InputRecipeNode unresolved() {
        InputRecipeNode.builder('unresolved').executorReady(false).blockers(['UNRESOLVED_VALUE']).build()
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
                                                    int maxCatalogScenarios) {
        new ScenarioGeneratorConfig(false,
                generationStrategy,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                includeSingles,
                maxSagaSetSize,
                maxCatalogScenarios,
                maxInputVariantsPerSaga,
                maxSchedulesPerInputTuple,
                allowTypeOnlyFallback,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                scheduleStrategy,
                1234L,
                maxGroupedRows)
    }
}
