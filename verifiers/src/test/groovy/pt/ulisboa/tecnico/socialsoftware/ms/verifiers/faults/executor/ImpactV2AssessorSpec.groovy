package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence
import spock.lang.Specification

class ImpactV2AssessorSpec extends Specification {
    private static final ObjectMapper MAPPER = new ObjectMapper()

    def 'three complete checks retain reasons and count the union of affected dummyapp identities'() {
        given:
        def item = id('com.example.dummyapp.item.aggregate.Item', 1)
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def dependency = new ImpactEvidence.Dependency(item, order, 2, 1L, 'OrderChanged', 'OrderSubscription')
        def itemBaseline = snapshot(item, 1, 'ACTIVE', [name: 'item'], [dependency])
        def itemFinal = snapshot(item, 2, 'ACTIVE', [name: 'item'], [dependency])
        def orderBaseline = snapshot(order, 1, 'ACTIVE', [status: 'open'])
        def orderDeleted = snapshot(order, 2, 'DELETED', [status: 'open'])
        def eventDelivery = delivery(9, itemBaseline, itemFinal, itemFinal, true)
        def execution = execution('COMPENSATED', 'EXACT', [
                failedAction('p1'), successfulEventAction('p2', 9)
        ], [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [itemBaseline, orderBaseline], [itemFinal, orderDeleted],
                [write(1, orderDeleted, sagaWriter('p1', 'recover-order', 'RECOVERY'))], [eventDelivery])

        then:
        report.assessmentStatus() == 'COMPLETE'
        report.completeScore() == 2
        report.observedAffectedObjectCount() == 2
        report.categoryResults()*.category() == [
                'DELETED_DEPENDENCY', 'FAILED_OPERATION_RESIDUAL', 'UNRESOLVED_DELIVERED_EVENT']
        report.categoryResults()*.coverageStatus().unique() == ['COMPLETE']
        report.categoryResults()*.positiveObjectCount() == [1, 1, 1]
        report.categoryResults()*.unknownReasons().every { it.empty }
        report.categoryResults()[0].findings()[0].affectedObject() == item
        report.categoryResults()[0].findings()[0].relatedObject() == order
        report.categoryResults()[1].findings()[0].affectedObject() == order
        report.categoryResults()[2].findings()[0].affectedObject() == item
        report.categoryResults()[2].findings()[0].eventId() == 9
        report.baseline() == [itemBaseline, orderBaseline]
        report.finalState() == [itemFinal, orderDeleted]
    }

    def 'deleted dependency accepts a target created active and then deleted during measurement'() {
        given:
        def item = id('com.example.dummyapp.item.aggregate.Item', 1)
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def dependency = new ImpactEvidence.Dependency(item, order, 2, 1L, 'OrderChanged', 'OrderSubscription')
        def itemFinal = snapshot(item, 1, 'ACTIVE', [name: 'item'], [dependency])
        def orderActive = snapshot(order, 1, 'ACTIVE', [status: 'open'])
        def orderDeleted = snapshot(order, 2, 'DELETED', [status: 'open'])

        when:
        def report = assess(execution(), [], [itemFinal, orderDeleted], [
                write(1, orderActive, sagaWriter('p1', 'create-order', 'FORWARD')),
                write(2, orderDeleted, sagaWriter('p1', 'delete-order', 'FORWARD'))
        ])

        then:
        category(report, 'DELETED_DEPENDENCY').positiveObjectCount() == 1
        category(report, 'DELETED_DEPENDENCY').coverageStatus() == 'COMPLETE'
    }

    def 'preexisting deletion restored residual and converged delivery produce an explicit complete zero'() {
        given:
        def item = id('com.example.dummyapp.item.aggregate.Item', 1)
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def dependency = new ImpactEvidence.Dependency(item, order, 2, 1L, 'OrderChanged', 'OrderSubscription')
        def itemState = snapshot(item, 1, 'ACTIVE', [name: 'item'], [dependency])
        def deleted = snapshot(order, 1, 'DELETED', [status: 'closed'])
        def restoredWrite = snapshot(order, 2, 'DELETED', [status: 'closed'])
        def execution = execution('COMPENSATED', 'EXACT', [failedAction('p1'), successfulEventAction('p2', 9)],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [itemState, deleted], [itemState, restoredWrite],
                [write(1, restoredWrite, sagaWriter('p1', 'restore-order', 'RECOVERY'))],
                [delivery(9, itemState, itemState, itemState, false)])

        then:
        report.assessmentStatus() == 'COMPLETE'
        report.completeScore() == 0
        report.observedAffectedObjectCount() == 0
        report.categoryResults()*.positiveObjectCount() == [0, 0, 0]
        report.categoryResults()*.coverageStatus().unique() == ['COMPLETE']
    }

    def 'failed residual is unknown when a later state is unobserved or another writer touches the object'() {
        given:
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(order, 1, 'ACTIVE', [status: 'open'])
        def failedWrite = snapshot(order, 2, 'ACTIVE', [status: 'failed-value'])
        def finalState = snapshot(order, 4, 'ACTIVE', [status: 'later-value'])
        def execution = execution('COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1')], [compensatedLifecycle('p1')])

        when: 'the tracked latest write does not explain the final projection'
        def unobserved = assess(execution, [baseline], [finalState], [
                write(1, failedWrite, sagaWriter('p1', 'failed-write', 'FORWARD'))
        ])

        then:
        unobserved.assessmentStatus() == 'PARTIAL'
        unobserved.completeScore() == null
        unobserved.observedAffectedObjectCount() == 0
        category(unobserved, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() ==
                ['FINAL_STATE_NOT_EXPLAINED_BY_TRACKED_WRITE']

        when: 'an event consumer is an additional writer of the same object'
        def competing = assess(execution, [baseline], [finalState], [
                write(1, failedWrite, sagaWriter('p1', 'failed-write', 'FORWARD')),
                write(2, finalState, eventWriter(9))
        ])

        then:
        competing.assessmentStatus() == 'PARTIAL'
        category(competing, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() ==
                ['COMPETING_OR_UNKNOWN_WRITER']
        category(competing, 'FAILED_OPERATION_RESIDUAL').findings().empty

        when: 'a final physical absence has no attributed delete fact in the Saga local observer'
        def absent = assess(execution, [baseline], [], [
                write(1, failedWrite, sagaWriter('p1', 'failed-write', 'FORWARD'))
        ])

        then:
        absent.assessmentStatus() == 'PARTIAL'
        absent.completeScore() == null
        category(absent, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() ==
                ['FINAL_STATE_NOT_EXPLAINED_BY_TRACKED_WRITE']
        category(absent, 'FAILED_OPERATION_RESIDUAL').findings().empty
    }

    def 'missing writer phase is retained as an unknown residual instead of failing assessment'() {
        given:
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(order, 1, 'ACTIVE', [status: 'open'])
        def finalState = snapshot(order, 2, 'ACTIVE', [status: 'failed-value'])
        def malformedWriter = new ImpactEvidence.Writer('SAGA', 'attempt', 'workload', 'p1',
                'failed-write', null, 'FixtureSaga', 'write', null)
        def execution = execution('COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [finalState], [write(1, finalState, malformedWriter)])

        then:
        report.assessmentStatus() == 'PARTIAL'
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() ==
                ['COMPETING_OR_UNKNOWN_WRITER']
    }

    def 'event check requires exact successful delivery typed receiver and surviving final eligibility'() {
        given:
        def item = id('com.example.dummyapp.item.aggregate.Item', 1)
        def otherType = id('com.example.dummyapp.order.aggregate.Order', 1)
        def before = snapshot(item, 1, 'ACTIVE', [name: 'same'])
        def afterWrongType = snapshot(otherType, 2, 'ACTIVE', [name: 'same'])
        def execution = execution('SUCCESS', 'EXACT', [successfulEventAction('p1', 9)],
                [committedParticipant('p1')], [])

        when:
        def mismatched = assess(execution, [], [], [], [delivery(9, before, afterWrongType, afterWrongType, true)])

        then:
        mismatched.assessmentStatus() == 'PARTIAL'
        category(mismatched, 'UNRESOLVED_DELIVERED_EVENT').unknownReasons()*.reason() ==
                ['RECEIVER_IDENTITY_UNAVAILABLE']

        when: 'the same receiver is deleted at the horizon'
        def deletedFinal = snapshot(item, 3, 'DELETED', [name: 'same'])
        def cleared = assess(execution, [], [], [], [delivery(9, before, before, deletedFinal, true)])

        then:
        cleared.assessmentStatus() == 'COMPLETE'
        category(cleared, 'UNRESOLVED_DELIVERED_EVENT').positiveObjectCount() == 0
    }

    def 'partial projection keeps positive lower bound and null complete score'() {
        given:
        def item = id('com.example.dummyapp.item.aggregate.Item', 1)
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def dependency = new ImpactEvidence.Dependency(item, order, 2, 1L, 'OrderChanged', 'OrderSubscription')
        def itemState = snapshot(item, 1, 'ACTIVE', [name: 'item'], [dependency])
        def orderBaseline = snapshot(order, 1, 'ACTIVE', [status: 'open'])
        def orderDeleted = snapshot(order, 2, 'DELETED', [status: 'open'])
        def gap = new ImpactEvidence.CoverageGap('FINAL', 'OtherAggregate[3].payload',
                'PERSISTENT_ATTRIBUTE_UNSUPPORTED', 'unsupported fixture mapping')

        when:
        def report = assess(execution(), [itemState, orderBaseline], [itemState, orderDeleted],
                [write(1, orderDeleted, sagaWriter('p1', 'delete-order', 'FORWARD'))], [], [gap])

        then:
        report.assessmentStatus() == 'PARTIAL'
        report.completeScore() == null
        report.observedAffectedObjectCount() == 1
        category(report, 'DELETED_DEPENDENCY').positiveObjectCount() == 1
        category(report, 'DELETED_DEPENDENCY').coverageStatus() == 'PARTIAL'
        report.coverageGaps() == [gap]
    }

    def 'receiver projection gap suppresses event finding while retaining unrelated deleted dependency'() {
        given:
        def item = id('com.example.dummyapp.item.aggregate.Item', 7)
        def order = id('com.example.dummyapp.order.aggregate.Order', 8)
        def dependency = new ImpactEvidence.Dependency(item, order, 8, 1L, 'OrderChanged', 'OrderSubscription')
        def itemState = snapshot(item, 1, 'ACTIVE', [name: 'same'], [dependency])
        def orderBaseline = snapshot(order, 1, 'ACTIVE', [status: 'open'])
        def orderDeleted = snapshot(order, 2, 'DELETED', [status: 'open'])
        def execution = execution('SUCCESS', 'EXACT', [successfulEventAction('p1', 42, 7)],
                [committedParticipant('p1')], [])
        def gap = new ImpactEvidence.CoverageGap('AFTER_EVENT', '7',
                'PERSISTENT_ATTRIBUTE_UNSUPPORTED', 'receiver projection is partial')

        when:
        def report = assess(execution, [itemState, orderBaseline], [itemState, orderDeleted], [
                write(1, orderDeleted, sagaWriter('p1', 'delete-order', 'FORWARD'))
        ], [delivery(42, itemState, itemState, itemState, true)], [gap])

        then:
        report.assessmentStatus() == 'PARTIAL'
        report.completeScore() == null
        report.observedAffectedObjectCount() == 1
        category(report, 'DELETED_DEPENDENCY').positiveObjectCount() == 1
        category(report, 'UNRESOLVED_DELIVERED_EVENT').positiveObjectCount() == 0
        category(report, 'UNRESOLVED_DELIVERED_EVENT').unknownReasons()*.reason() ==
                ['PERSISTENT_ATTRIBUTE_UNSUPPORTED']
    }

    def 'invalid and unavailable attempts serialize required nullable score fields'() {
        when:
        def invalid = assess(execution('UNEXPECTED_EXECUTION_FAILURE', 'INCOMPLETE'), [], [], [])
        def unavailable = new ImpactV2Assessor().assess(execution(), 'attempt', 'workload', 'scenario',
                'UNAVAILABLE', 'COLLECTION_DISABLED', [], [], [], [], [])
        def invalidJson = MAPPER.readTree(MAPPER.writeValueAsBytes(invalid))
        def unavailableJson = MAPPER.readTree(MAPPER.writeValueAsBytes(unavailable))

        then:
        invalid.assessmentStatus() == 'INVALID'
        invalid.completeScore() == null
        invalid.observedAffectedObjectCount() == null
        invalidJson.has('completeScore') && invalidJson.path('completeScore').isNull()
        invalidJson.has('observedAffectedObjectCount') && invalidJson.path('observedAffectedObjectCount').isNull()
        unavailable.assessmentStatus() == 'UNAVAILABLE'
        unavailable.completeScore() == null
        unavailable.observedAffectedObjectCount() == null
        unavailableJson.path('completeScore').isNull()
        unavailableJson.path('observedAffectedObjectCount').isNull()
        unavailable.packageManifestPath() == '/package/manifest.json'
    }

    def 'collector contains assessment failure and preserves raw facts'() {
        given:
        def failure = new IllegalStateException('broken assessment fixture')
        def assessor = Mock(ImpactV2Assessor) {
            assess(_, _, _, _, _, _, _, _, _, _, _) >> { throw failure }
        }
        def collector = new ImpactV2EvidenceCollector('attempt', 'workload', 'scenario', assessor)
        def identity = id('com.example.dummyapp.item.aggregate.Item', 1)
        def state = snapshot(identity, 1, 'ACTIVE', [name: 'item'])
        collector.committedWrite(state, sagaWriter('p1', 'write-item', 'FORWARD'))

        when:
        def report = collector.report(execution())

        then:
        report.assessmentStatus() == 'UNAVAILABLE'
        report.assessmentReason() == 'ASSESSMENT_FAILED'
        report.completeScore() == null
        report.observedAffectedObjectCount() == null
        report.committedWrites()*.aggregate() == [state]
        report.coverageGaps()*.reason() == ['ASSESSMENT_FAILED']
        report.categoryResults()*.coverageStatus().unique() == ['UNAVAILABLE']
    }

    private static ImpactV2EvidenceReport assess(ScenarioExecutionReport execution,
                                                  List<ImpactEvidence.AggregateSnapshot> baseline,
                                                  List<ImpactEvidence.AggregateSnapshot> finalState,
                                                  List<ImpactEvidence.CommittedWrite> writes,
                                                  List<ImpactEvidence.EventDelivery> deliveries = [],
                                                  List<ImpactEvidence.CoverageGap> gaps = []) {
        new ImpactV2Assessor().assess(execution, 'attempt', 'workload', 'scenario', 'OBSERVED', null,
                baseline, finalState, writes, deliveries, gaps)
    }

    private static ImpactV2EvidenceReport.CategoryResult category(ImpactV2EvidenceReport report, String name) {
        report.categoryResults().find { it.category() == name }
    }

    private static ImpactEvidence.AggregateIdentity id(String type, int value) {
        new ImpactEvidence.AggregateIdentity(type, value)
    }

    private static ImpactEvidence.AggregateSnapshot snapshot(ImpactEvidence.AggregateIdentity identity,
                                                              long version,
                                                              String lifecycle,
                                                              Map<String, Object> data,
                                                              List<ImpactEvidence.Dependency> dependencies = []) {
        new ImpactEvidence.AggregateSnapshot(identity, version, lifecycle, identity.aggregateType(), data, dependencies)
    }

    private static ImpactEvidence.CommittedWrite write(long sequence,
                                                        ImpactEvidence.AggregateSnapshot snapshot,
                                                        ImpactEvidence.Writer writer) {
        new ImpactEvidence.CommittedWrite(sequence, snapshot, writer)
    }

    private static ImpactEvidence.Writer sagaWriter(String saga, String action, String phase) {
        new ImpactEvidence.Writer('SAGA', 'attempt', 'workload', saga, action, phase,
                'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas', action, null)
    }

    private static ImpactEvidence.Writer eventWriter(int eventId) {
        new ImpactEvidence.Writer('EVENT_CONSUMER', 'attempt', 'workload', 'p2', 'event-action', 'EVENT',
                'com.example.dummyapp.item.aggregate.ItemService', 'handle', eventId)
    }

    private static ImpactEvidence.EventDelivery delivery(int eventId,
                                                         ImpactEvidence.AggregateSnapshot before,
                                                         ImpactEvidence.AggregateSnapshot after,
                                                         ImpactEvidence.AggregateSnapshot finalState,
                                                         Boolean finalEligibility) {
        new ImpactEvidence.EventDelivery(10, eventId, 'OrderChanged', 2, 1L, before, after,
                true, true, finalState, finalEligibility, eventWriter(eventId))
    }

    private static ScenarioExecutionReport execution(String status = 'SUCCESS',
                                                     String conformance = 'EXACT',
                                                     List<ScenarioExecutionReport.ActionOutcome> actions = [],
                                                     List<ScenarioExecutionReport.Participant> participants = [],
                                                     List<ScenarioExecutionReport.LifecycleEvent> lifecycle = []) {
        new ScenarioExecutionReport(null, 'attempt', status, '/package/manifest.json', 'workload', 'scenario',
                'SINGLE_SAGA', '0', 'IN_MEMORY_FAULT_VECTOR', conformance, null, null, null, null, null,
                null, null, null, [], [], actions, lifecycle, participants, [])
    }

    private static ScenarioExecutionReport.ActionOutcome failedAction(String saga) {
        action('failed-action', 'FORWARD', saga, 'ASSIGNED_FAULT', null)
    }

    private static ScenarioExecutionReport.ActionOutcome successfulEventAction(String saga, int eventId,
                                                                                int subscriberId = 1) {
        action('event-action', 'EVENT_CONSEQUENCE', saga, 'COMPLETED', eventId, subscriberId)
    }

    private static ScenarioExecutionReport.ActionOutcome action(String actionId, String kind, String saga,
                                                                 String status, Integer eventId,
                                                                 int subscriberId = 1) {
        def eventEvidence = eventId == null ? null : new ScenarioExecutionReport.EventRuntimeEvidence(
                eventId, 'OrderChanged', 2, 1L, true, subscriberId, 'FixtureHandling', 'handle', 'FixtureHandler')
        new ScenarioExecutionReport.ActionOutcome(actionId, kind, saga, null, null,
                eventId == null ? null : 'event-consequence', null, null, null, null, actionId,
                0, 0, status, status == 'COMPLETED' ? 'SUCCEEDED' : 'NOT_RUN',
                'NOT_APPLICABLE', null, eventEvidence, [], null, null)
    }

    private static ScenarioExecutionReport.Participant compensatedParticipant(String saga) {
        new ScenarioExecutionReport.Participant(saga, 'FixtureSaga', 'input', 'MATERIALIZED', 'STARTUP_READY',
                'COMPENSATED', [], [])
    }

    private static ScenarioExecutionReport.Participant committedParticipant(String saga) {
        new ScenarioExecutionReport.Participant(saga, 'FixtureSaga', 'input', 'MATERIALIZED', 'STARTUP_READY',
                'COMMITTED', [], [])
    }

    private static ScenarioExecutionReport.LifecycleEvent compensatedLifecycle(String saga) {
        new ScenarioExecutionReport.LifecycleEvent(1, saga, 'COMPENSATED', 'recover', 'SUCCEEDED', null, null)
    }
}
