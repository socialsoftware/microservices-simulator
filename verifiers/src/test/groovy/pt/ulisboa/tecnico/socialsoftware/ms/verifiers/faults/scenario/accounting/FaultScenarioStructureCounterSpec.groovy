package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification
import spock.lang.Unroll

class FaultScenarioStructureCounterSpec extends Specification {
    @Unroll
    def 'count matches complete generation for #lengths / #strategy / #deliveries deliveries'() {
        given:
        def sagas = lengths.withIndex().collect { len, i -> saga("dummyapp.S${i}", len) }
        def events = deliveries ? (0..<deliveries).collect { route(sagas[0], it) } : []
        def cfg = config(strategy, Math.max(1, deliveries))
        def anchors = ConflictGraphBuilder.buildSelectionGraph(sagas, events, cfg).conflictCandidates()
        def plans = ScenarioGenerator.generate(sagas, sagas.collect { input(it.sagaFqn()) }, events, cfg)
                .workloadPlans().findAll { it.participants().size() == sagas.size() }
        when:
        def counted = new FaultScenarioStructureCounter(2000000).count(sagas, anchors, events, strategy, Math.max(1, deliveries))
        BigInteger total = 0
        plans.each { plan -> vectors(plan).each { vector ->
            def full = RecoveryScheduleGenerator.generate(plan, vector, 100000)
            assert full.writtenScheduleCount() == full.uncappedScheduleCount()
            assert RecoveryScheduleGenerator.count(plan, vector) == full.uncappedScheduleCount()
            assert full.faultScenarios()*.deterministicId().toSet().size() == full.writtenScheduleCount()
            total += full.writtenScheduleCount()
        } }
        then:
        plans
        counted.workloads() == plans.size()
        counted.faultScenarios() == total
        where:
        lengths      | strategy                                                              | deliveries
        [2, 1]       | ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING | 0
        [3, 2]       | ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING | 0
        [2, 1, 1]    | ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING | 1
        [2, 1, 1, 1] | ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING | 0
        [2, 1]       | ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING | 3
        [3, 2]       | ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED            | 2
        [2, 1, 1, 1] | ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED            | 1
        [3, 2]       | ScenarioGeneratorConfig.ScheduleStrategy.SERIAL                        | 1
    }
    def 'compressed unrelated tails and producer exclusion match generation'() {
        given:
        def sagas = [saga('dummyapp.S0', 3, false), saga('dummyapp.S1', 2, false), saga('dummyapp.Consumer0', 1, false)]
        def events = [route(sagas[0], 0)]
        def strategy = ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED
        def plans = ScenarioGenerator.generate(sagas, sagas.collect { input(it.sagaFqn()) }, events, config(strategy, 3))
                .workloadPlans().findAll { it.participants().size() == 3 }
        when:
        def counts = new FaultScenarioStructureCounter(100000).count(sagas, [], events, strategy, 3)
        then:
        plans.size() == 1
        counts.workloads() == 1
        counts.faultScenarios() == vectors(plans[0]).sum { RecoveryScheduleGenerator.count(plans[0], it) }
    }
    def 'different emission sites stay separate and duplicate routes do not inflate counts'() {
        given:
        def sagas = [saga('dummyapp.S0', 3), saga('dummyapp.S1', 1)]
        def first = route(sagas[0], 0)
        def site = new EventEmissionSite(ScenarioIdGenerator.eventEmissionSiteId('dummyapp.Service', 'emit()', 1, 'dummyapp.Event'),
                'dummyapp.Service', 'emit()', 1, 'dummyapp.Event', [])
        def second = new EventConsequenceDefinition(first.triggerSagaFqn(), first.triggerStepKey(), site,
                first.eventHandlingClassFqn(), first.eventHandlingMethodName(), first.eventHandlerClassFqn(),
                first.eventProcessingClassFqn(), first.eventProcessingMethodName(), first.facadeClassFqn(),
                first.facadeMethodName(), first.downstreamSagaFqn(), first.deliveryPolicy(), [])
        def events = [first, first, second]
        def cfg = config(ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING, 3)
        def plans = ScenarioGenerator.generate(sagas, sagas.collect { input(it.sagaFqn()) }, events, cfg)
                .workloadPlans().findAll { it.participants().size() == 2 }
        when:
        def counter = new FaultScenarioStructureCounter(100000)
        def counts = counter.count(sagas, [], events, cfg.scheduleStrategy(), 3)
        then:
        plans.every { it.eventConsequences().size() <= 1 }
        counts.workloads() == plans.size()
        counts.faultScenarios() == plans.sum { p -> vectors(p).sum { RecoveryScheduleGenerator.count(p, it) } }
        counts == counter.count(sagas.reverse(), [], events.reverse(), cfg.scheduleStrategy(), 3)
    }

    def 'input dependent pruning counts only equal keys and ignores catalogue write caps'() {
        given:
        def sagas = ['dummyapp.A','dummyapp.B'].collect { name ->
            new SagaDefinition(name, [new StepDefinition(name+'::s', name+'::s', 's', 0, [],
                    [new StepFootprint(new AggregateKey('dummyapp.Item','Item','id',FootprintConfidence.SYMBOLIC),AccessMode.WRITE,[])], [])], [])
        }
        def inputs = sagas.collectMany { saga -> ['1','2'].collect { value ->
            new InputVariant(saga.sagaFqn()+value, saga.sagaFqn(), 'dummyapp.Test','test','saga',InputResolutionStatus.RESOLVED,
                    'source'+value,'provenance'+value,[],[id:value],[])
        } }
        def model = new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult(sagas, inputs, [:], [])
        def cfg = new ScenarioGeneratorConfig(false, ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, false,2,0,100,0,false,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,1L)
        def rows = []
        when:
        def result = new FaultScenarioCountService(100000,100).count(model,cfg,{ rows.add(it) })
        then:
        rows.size() == 1
        rows[0].inputCombinations() == 2
        rows[0].workloads() == 4
        rows[0].faultScenarios() == 16
        result.totals()[0].status() == 'COMPLETE'
    }

    def 'state limit refuses a total instead of returning a truncated number'() {
        when:
        new FaultScenarioStructureCounter(1).count([saga('dummyapp.S0', 3)], [], [], ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 1)
        then:
        thrown(IllegalStateException)
    }
    static ScenarioGeneratorConfig config(strategy, deliveries) {
        new ScenarioGeneratorConfig(true, ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS, false, 4, 100000, 100,
                100000, true, ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE, strategy, 1234L, 100000, deliveries)
    }
    static SagaDefinition saga(String name, int length, boolean interacting = true) {
        new SagaDefinition(name, (0..<length).collect { i ->
            new StepDefinition(name + '::s' + i, name + '::s' + i, 's' + i, i, [],
                    interacting && i == 0 ? [new StepFootprint(new AggregateKey('dummyapp.Item', 'Item', 'id', FootprintConfidence.EXACT), AccessMode.WRITE, [])] : [], [],
                    i % 2 == 0, true, true, i % 2 == 0 ? CompensationEvidenceClass.EXPLICIT_COMPENSATION : null, [], [])
        }, [])
    }
    static InputVariant input(String saga) {
        new InputVariant('input-' + saga, saga, 'dummyapp.Test', 'build', 'saga', InputResolutionStatus.RESOLVED,
                'source-' + saga, 'provenance-' + saga, [], [:], [])
    }
    static EventConsequenceDefinition route(SagaDefinition producer, int i) {
        def site = new EventEmissionSite(ScenarioIdGenerator.eventEmissionSiteId('dummyapp.Service', 'emit()', 0, 'dummyapp.Event'), 'dummyapp.Service', 'emit()', 0, 'dummyapp.Event', [])
        new EventConsequenceDefinition(producer.sagaFqn(), producer.steps()[producer.steps().size() > 2 ? 1 : 0].stepKey(), site,
                'dummyapp.Handling', 'handle' + i, 'dummyapp.Handler' + i, 'dummyapp.Processing' + i,
                'process', 'dummyapp.Facade', 'invoke', 'dummyapp.Consumer' + i, null, [])
    }
    static List<String> vectors(WorkloadPlan plan) {
        def choices = plan.participants().collect { p -> [-1] + plan.faultSlots().findAll { it.sagaInstanceId() == p.deterministicId() }*.slotIndex() }
        List<List<Integer>> product = [[]]
        choices.each { options -> product = product.collectMany { prefix -> options.collect { prefix + [it] } } }
        product.collect { selected -> (0..<plan.faultSlots().size()).collect { selected.contains(it) ? '1' : '0' }.join() }
    }
}
