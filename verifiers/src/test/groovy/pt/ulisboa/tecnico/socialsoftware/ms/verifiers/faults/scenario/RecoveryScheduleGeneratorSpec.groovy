package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification

class RecoveryScheduleGeneratorSpec extends Specification {

    def 'a pre-body fault after two eligible steps enables reverse recovery without reordering residual forwards'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('a', 'a2', true),
                step('a', 'a3'),
                step('b', 'b1'),
                step('b', 'b2')
        ])

        when:
        def result = RecoveryScheduleGenerator.generate(workload, '00100', 20)

        then:
        result.uncappedScheduleCount() == new BigInteger('6')
        result.writtenScheduleCount() == 6
        result.faultScenarios()*.deterministicId().toSet().size() == 6
        result.faultScenarios().every { scenario ->
            labels(workload, scenario).findAll { it.startsWith('C:') } == ['C:a2', 'C:a1']
            labels(workload, scenario).findAll { it.startsWith('F:') } == ['F:a1', 'F:a2', 'F:a3', 'F:b1', 'F:b2']
        }
    }

    def 'failed-owner slots are omitted while later live-participant assignments still realize'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('a', 'a2', true),
                step('b', 'b1')
        ])

        when:
        def skipped = RecoveryScheduleGenerator.generate(workload, '101', 20)
        def masked = RecoveryScheduleGenerator.generate(workload, '111', 20)

        then:
        labels(workload, skipped.faultScenarios()[0]) == ['F:a1', 'F:b1']
        skipped.faultSlotDiagnostics()*.state() == [
                FaultSlotGenerationState.REALIZED,
                FaultSlotGenerationState.SKIPPED_AFTER_PARTICIPANT_FAILURE,
                FaultSlotGenerationState.REALIZED
        ]

        and:
        labels(workload, masked.faultScenarios()[0]) == ['F:a1', 'F:b1']
        masked.faultSlotDiagnostics()*.state() == [
                FaultSlotGenerationState.REALIZED,
                FaultSlotGenerationState.MASKED,
                FaultSlotGenerationState.REALIZED
        ]
    }

    def 'multiple participant queues are enabled only by their realized faults'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('a', 'a2', true),
                step('a', 'a3'),
                step('b', 'b1', true),
                step('b', 'b2'),
                step('c', 'c1')
        ])

        when:
        def result = RecoveryScheduleGenerator.generate(workload, '001010', 1000)
        def priorityPrefix = RecoveryScheduleGenerator.generate(workload, '001010', 2)

        then:
        result.faultScenarios().size() == result.uncappedScheduleCount().intValueExact()
        result.faultScenarios().every { scenario ->
            def schedule = labels(workload, scenario)
            schedule.findAll { it.startsWith('F:') } == ['F:a1', 'F:a2', 'F:a3', 'F:b1', 'F:b2', 'F:c1']
            schedule.findAll { it in ['C:a2', 'C:a1'] } == ['C:a2', 'C:a1']
            schedule.count('C:b1') == 1
            schedule.indexOf('F:a3') < schedule.indexOf('C:a2')
            schedule.indexOf('F:b2') < schedule.indexOf('C:b1')
        }
        result.faultSlotDiagnostics().findAll { it.state() == FaultSlotGenerationState.REALIZED }*.slotIndex() == [2, 4]

        and: 'latest recovery drains simultaneously enabled queues by participant id while preserving each reverse queue'
        labels(workload, priorityPrefix.faultScenarios()[1]).findAll { it.startsWith('C:') } == ['C:a2', 'C:a1', 'C:b1']
    }

    def 'all-zero vectors produce one forward-only scenario through the same generator'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('b', 'b1', true),
                step('a', 'a2')
        ])

        when:
        def result = RecoveryScheduleGenerator.generate(workload, '000')

        then:
        result.recoveryScheduleCap() == 20
        result.uncappedScheduleCount() == BigInteger.ONE
        result.writtenScheduleCount() == 1
        labels(workload, result.faultScenarios()[0]) == ['F:a1', 'F:b1', 'F:a2']
        result.faultScenarios()[0].actions()*.kind().unique() == [FaultScenarioActionKind.FORWARD]
        result.faultSlotDiagnostics()*.state().unique() == [FaultSlotGenerationState.NOT_ASSIGNED]
    }

    def 'every retained schedule preserves the valid residual WorkloadPlan forward subsequence'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('b', 'b1', true),
                step('a', 'a2'),
                step('b', 'b2'),
                step('c', 'c1')
        ])
        def vectors = ['00000', '00100', '01000', '00110', '10101']

        expect:
        vectors.every { vector ->
            def expected = expectedResidualSlotIds(workload, vector)
            def result = RecoveryScheduleGenerator.generate(workload, vector, 200)
            result.faultScenarios().every { scenario ->
                scenario.actions()
                        .findAll { it.kind() == FaultScenarioActionKind.FORWARD }
                        *.sourceFaultSlotId() == expected
            }
        }
    }

    def 'caps one through four retain the coverage-first policy prefix'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('a', 'a2', true),
                step('a', 'a3'),
                step('b', 'b1'),
                step('b', 'b2')
        ])
        def priority = [
                ['F:a1', 'F:a2', 'F:a3', 'C:a2', 'C:a1', 'F:b1', 'F:b2'],
                ['F:a1', 'F:a2', 'F:a3', 'F:b1', 'F:b2', 'C:a2', 'C:a1'],
                ['F:a1', 'F:a2', 'F:a3', 'C:a2', 'F:b1', 'C:a1', 'F:b2'],
                ['F:a1', 'F:a2', 'F:a3', 'F:b1', 'C:a2', 'F:b2', 'C:a1']
        ]

        expect:
        (1..4).every { cap ->
            def result = RecoveryScheduleGenerator.generate(workload, '00100', cap)
            result.uncappedScheduleCount() == new BigInteger('6') &&
                    result.writtenScheduleCount() == cap &&
                    result.faultScenarios().collect { labels(workload, it) } == priority.take(cap)
        }
    }

    def 'duplicate representatives do not consume cap capacity'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('a', 'a2'),
                step('b', 'b1'),
                step('b', 'b2'),
                step('b', 'b3'),
                step('b', 'b4')
        ])

        when:
        def result = RecoveryScheduleGenerator.generate(workload, '010000', 4)

        then:
        result.uncappedScheduleCount() == new BigInteger('5')
        result.faultScenarios().collect { labels(workload, it) } == [
                ['F:a1', 'F:a2', 'C:a1', 'F:b1', 'F:b2', 'F:b3', 'F:b4'],
                ['F:a1', 'F:a2', 'F:b1', 'F:b2', 'F:b3', 'F:b4', 'C:a1'],
                ['F:a1', 'F:a2', 'F:b1', 'C:a1', 'F:b2', 'F:b3', 'F:b4'],
                ['F:a1', 'F:a2', 'F:b1', 'F:b2', 'F:b3', 'C:a1', 'F:b4']
        ]
        result.faultScenarios()*.deterministicId().toSet().size() == 4
        result.metrics().representativeCandidatesConstructed() == 6
    }

    def 'multiple final-boundary representatives exclude faulted boundaries and use earliest handling for suffix faults'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('a', 'a2', true),
                step('a', 'a3'),
                step('b', 'b0'),
                step('b', 'b1'),
                step('c', 'c1', true),
                step('c', 'c2'),
                step('d', 'd1')
        ])

        when:
        def result = RecoveryScheduleGenerator.generate(workload, '00100010', 7)
        def schedules = result.faultScenarios().collect { labels(workload, it) }

        then: 'the first final boundary is b1; a3 and c2 are assigned and are not boundary candidates'
        schedules[4] == ['F:a1', 'F:a2', 'F:a3', 'F:b0', 'C:a2', 'C:a1', 'F:b1', 'F:c1', 'F:c2', 'C:c1', 'F:d1']
        schedules[5] == ['F:a1', 'F:a2', 'F:a3', 'F:b0', 'F:b1', 'C:a2', 'C:a1', 'F:c1', 'F:c2', 'C:c1', 'F:d1']
        schedules[6] == ['F:a1', 'F:a2', 'F:a3', 'F:b0', 'F:b1', 'F:c1', 'F:c2', 'C:a2', 'C:a1', 'C:c1', 'F:d1']
        result.metrics().representativeCandidatesConstructed() == 8

        and: 'the later c2 fault enables c1 recovery, which earliest suffix handling drains before d1'
        schedules[4].indexOf('F:c2') < schedules[4].indexOf('C:c1')
        schedules[4].indexOf('C:c1') < schedules[4].indexOf('F:d1')
        schedules[5].indexOf('F:c2') < schedules[5].indexOf('C:c1')
        schedules[5].indexOf('C:c1') < schedules[5].indexOf('F:d1')
    }

    def 'remaining cap capacity is filled by lazy lexicographic action-identity order'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('a', 'a2', true),
                step('a', 'a3'),
                step('b', 'b1'),
                step('b', 'b2'),
                step('b', 'b3'),
                step('b', 'b4')
        ])

        when:
        def exhaustive = RecoveryScheduleGenerator.generate(workload, '0010000', 20)
        def capped = RecoveryScheduleGenerator.generate(workload, '0010000', 6)
        def representativeIds = capped.faultScenarios().take(5)*.deterministicId()
        def firstLexicographicGap = exhaustive.faultScenarios().find { !representativeIds.contains(it.deterministicId()) }

        then:
        exhaustive.uncappedScheduleCount() == new BigInteger('15')
        exhaustive.writtenScheduleCount() == 15
        capped.writtenScheduleCount() == 6
        capped.faultScenarios()[5].deterministicId() == firstLexicographicGap.deterministicId()
        capped.metrics().materializedLeavesVisited() < exhaustive.uncappedScheduleCount().longValueExact()
    }

    def 'action and FaultScenario identities close over every replay semantic field'() {
        given:
        def baselineAction = action(FaultScenarioActionKind.FORWARD, 'a', 'slot-1', null, 'occurrence-1')
        def secondAction = action(FaultScenarioActionKind.COMPENSATION, 'a', null, 'checkpoint-1', 'occurrence-0')
        def baselineScenario = scenario('workload-1', '10', [baselineAction, secondAction])

        expect:
        [
                action(FaultScenarioActionKind.COMPENSATION, 'a', 'slot-1', null, 'occurrence-1'),
                action(FaultScenarioActionKind.FORWARD, 'b', 'slot-1', null, 'occurrence-1'),
                action(FaultScenarioActionKind.FORWARD, 'a', 'slot-2', null, 'occurrence-1'),
                action(FaultScenarioActionKind.FORWARD, 'a', 'slot-1', 'checkpoint-1', 'occurrence-1'),
                action(FaultScenarioActionKind.FORWARD, 'a', 'slot-1', null, 'occurrence-2')
        ].every { it.deterministicId() != baselineAction.deterministicId() }

        and:
        ScenarioIdGenerator.faultScenarioId(scenario('workload-2', '10', [baselineAction, secondAction])) != ScenarioIdGenerator.faultScenarioId(baselineScenario)
        ScenarioIdGenerator.faultScenarioId(scenario('workload-1', '01', [baselineAction, secondAction])) != ScenarioIdGenerator.faultScenarioId(baselineScenario)
        ScenarioIdGenerator.faultScenarioId(scenario('workload-1', '10', [secondAction, baselineAction])) != ScenarioIdGenerator.faultScenarioId(baselineScenario)
        ScenarioIdGenerator.faultScenarioId(scenario('workload-1', '10', [
                action(FaultScenarioActionKind.FORWARD, 'a', 'slot-1', null, 'occurrence-2'), secondAction
        ])) != ScenarioIdGenerator.faultScenarioId(baselineScenario)

        and: 'scenario identity recomputes action semantics rather than trusting a stale supplied action id'
        def staleIdMutation = new FaultScenarioAction(baselineAction.deterministicId(), FaultScenarioActionKind.FORWARD,
                'a', 'slot-1', null, 'occurrence-2')
        ScenarioIdGenerator.faultScenarioId(scenario('workload-1', '10', [staleIdMutation, secondAction])) != ScenarioIdGenerator.faultScenarioId(baselineScenario)
    }

    def 'repeated step definitions retain distinct occurrence and action identities'() {
        given:
        def workload = workload([
                step('a', 'repeat', true),
                step('a', 'repeat', true),
                step('a', 'fault')
        ])

        when:
        def result = RecoveryScheduleGenerator.generate(workload, '001', 20)
        def scenario = result.faultScenarios()[0]
        def repeatedForwards = scenario.actions().findAll { it.kind() == FaultScenarioActionKind.FORWARD }.take(2)

        then:
        repeatedForwards*.occurrenceId() == ['forward-0', 'forward-1']
        repeatedForwards*.deterministicId().toSet().size() == 2
        scenario.actions().findAll { it.kind() == FaultScenarioActionKind.COMPENSATION }*.occurrenceId() == ['forward-1', 'forward-0']
    }

    def 'generation rejects malformed vectors plans and recovery caps'() {
        given:
        def workload = workload([step('a', 'a1'), step('a', 'a2')])

        when:
        RecoveryScheduleGenerator.generate(workload, '0', 20)
        then:
        thrown(IllegalArgumentException)

        when:
        RecoveryScheduleGenerator.generate(workload, '0x', 20)
        then:
        thrown(IllegalArgumentException)

        when:
        RecoveryScheduleGenerator.generate(malformedSlotWorkload(workload), '00', 20)
        then:
        def invalidPlan = thrown(IllegalArgumentException)
        invalidPlan.message.contains('MALFORMED_FAULT_SLOT')

        when:
        RecoveryScheduleGenerator.generate(workload, '00', 0)
        then:
        thrown(IllegalArgumentException)

        when:
        RecoveryScheduleGenerator.generate(workload, '00', -1)
        then:
        thrown(IllegalArgumentException)

        expect:
        RecoveryScheduleCap.parse(null).value() == 20
        RecoveryScheduleCap.parse('7').value() == 7

        when:
        RecoveryScheduleCap.parse('abc')
        then:
        thrown(IllegalArgumentException)

        when:
        RecoveryScheduleCap.parse(' ')
        then:
        thrown(IllegalArgumentException)
    }

    def 'exact high-cardinality counting is memoized and capped materialization does not visit every leaf'() {
        given:
        def definitions = (1..30).collect { step('a', "a${it}".toString(), true) }
        definitions << step('a', 'fault')
        definitions.addAll((1..30).collect { step('b', "b${it}".toString()) })
        def workload = workload(definitions)
        def vector = ('0' * 30) + '1' + ('0' * 30)
        def oracle = binomial(60, 30)

        when:
        def result = RecoveryScheduleGenerator.generate(workload, vector)

        then:
        oracle == new BigInteger('118264581564861424')
        result.uncappedScheduleCount() == oracle
        result.writtenScheduleCount() == 20
        result.faultScenarios().size() == 20
        result.metrics().countingStatesVisited() == 992
        result.metrics().materializedLeavesVisited() < 100
        BigInteger.valueOf(result.metrics().materializedLeavesVisited()) < result.uncappedScheduleCount()
    }

    def 'identical inputs produce stable scenario ordering ids counts and diagnostics'() {
        given:
        def workload = workload([
                step('a', 'a1', true),
                step('a', 'a2', true),
                step('a', 'a3'),
                step('b', 'b1'),
                step('b', 'b2'),
                step('b', 'b3')
        ])

        when:
        def first = RecoveryScheduleGenerator.generate(workload, '001000', 7)
        def second = RecoveryScheduleGenerator.generate(workload, '001000', 7)

        then:
        first == second
        first.faultScenarios()*.deterministicId() == second.faultScenarios()*.deterministicId()
    }

    private static WorkloadPlan workload(List<Map> definitions) {
        def ownerIds = definitions*.owner.unique()
        def participants = ownerIds.collect { owner -> new SagaInstance(owner, "example.${owner.toUpperCase()}Saga".toString(), "input-${owner}".toString(), []) }
        def inputs = ownerIds.collect { owner ->
            new InputVariant("input-${owner}".toString(), "example.${owner.toUpperCase()}Saga".toString(),
                    'example.RecoverySpec', 'fixture', owner as String, InputResolutionStatus.RESOLVED,
                    'source', 'provenance', [], [:], [])
        }
        def schedule = []
        def slots = []
        def checkpoints = []
        definitions.eachWithIndex { definition, index ->
            def stepId = "example.${definition.owner.toUpperCase()}Saga::${definition.name}".toString()
            def occurrenceId = "forward-${index}".toString()
            def scheduled = new ScheduledStep(occurrenceId, definition.owner as String, stepId, index,
                    definition.name as String, [])
            schedule << scheduled
            slots << new ForwardFaultSlot("slot-${index}".toString(), index, occurrenceId,
                    definition.owner as String, stepId, definition.name as String, occurrenceId)
            if (definition.checkpoint) {
                checkpoints << new CompensationCheckpoint("checkpoint-${index}".toString(), checkpoints.size(),
                        definition.owner as String, occurrenceId, stepId, definition.name as String, occurrenceId,
                        CompensationEvidenceClass.EXPLICIT_COMPENSATION, [], [], [])
            }
        }
        withCurrentId(new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, null,
                participants.size() == 1 ? ScenarioKind.SINGLE_SAGA : ScenarioKind.MULTI_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, participants, inputs, schedule, [], slots, checkpoints, []))
    }

    private static WorkloadPlan malformedSlotWorkload(WorkloadPlan original) {
        def slots = original.faultSlots().withIndex().collect { slot, index ->
            index == 0
                    ? new ForwardFaultSlot(slot.deterministicId(), slot.slotIndex(), slot.scheduledStepId(),
                    slot.sagaInstanceId(), slot.stepId(), slot.runtimeStepName(), 'wrong-occurrence')
                    : slot
        }
        withCurrentId(new WorkloadPlan(original.schemaVersion(), null, original.kind(), original.executionShape(),
                original.participants(), original.acceptedInputs(), original.forwardSchedule(), original.conflictEvidence(),
                slots, original.compensationCheckpoints(), original.warnings()))
    }

    private static WorkloadPlan withCurrentId(WorkloadPlan plan) {
        new WorkloadPlan(plan.schemaVersion(), ScenarioIdGenerator.workloadPlanId(plan), plan.kind(), plan.executionShape(),
                plan.participants(), plan.acceptedInputs(), plan.forwardSchedule(), plan.conflictEvidence(), plan.faultSlots(),
                plan.compensationCheckpoints(), plan.warnings())
    }

    private static Map step(String owner, String name, boolean checkpoint = false) {
        [owner: owner, name: name, checkpoint: checkpoint]
    }

    private static List<String> labels(WorkloadPlan workload, FaultScenario scenario) {
        def slotNames = workload.faultSlots().collectEntries { [(it.deterministicId()): it.runtimeStepName()] }
        def checkpointNames = workload.compensationCheckpoints().collectEntries { [(it.deterministicId()): it.runtimeStepName()] }
        scenario.actions().collect { action ->
            action.kind() == FaultScenarioActionKind.FORWARD
                    ? "F:${slotNames[action.sourceFaultSlotId()]}".toString()
                    : "C:${checkpointNames[action.sourceCompensationCheckpointId()]}".toString()
        }
    }

    private static List<String> expectedResidualSlotIds(WorkloadPlan workload, String vector) {
        def failed = [] as Set
        def result = []
        workload.faultSlots().eachWithIndex { slot, index ->
            if (!failed.contains(slot.sagaInstanceId())) {
                result << slot.deterministicId()
                if (vector.charAt(index) == '1' as char) {
                    failed << slot.sagaInstanceId()
                }
            }
        }
        result
    }

    private static FaultScenarioAction action(FaultScenarioActionKind kind,
                                              String owner,
                                              String sourceSlot,
                                              String sourceCheckpoint,
                                              String occurrence) {
        def id = ScenarioIdGenerator.faultScenarioActionId(kind, owner, sourceSlot, sourceCheckpoint, occurrence)
        new FaultScenarioAction(id, kind, owner, sourceSlot, sourceCheckpoint, occurrence)
    }

    private static FaultScenario scenario(String workloadId, String vector, List<FaultScenarioAction> actions) {
        new FaultScenario(FaultScenario.SCHEMA_VERSION, null, workloadId, vector, actions)
    }

    private static BigInteger binomial(int n, int k) {
        def result = BigInteger.ONE
        (1..k).each { index ->
            result = result.multiply(BigInteger.valueOf(n - k + index)).divide(BigInteger.valueOf(index))
        }
        result
    }
}
