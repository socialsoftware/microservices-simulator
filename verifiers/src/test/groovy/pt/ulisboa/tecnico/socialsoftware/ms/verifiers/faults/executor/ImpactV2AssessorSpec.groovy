package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence
import spock.lang.Specification
import spock.lang.Unroll

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

    def 'a creation removed during recovery retains its evidence but adds no residual point'() {
        given:
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def created = snapshot(order, 1, 'ACTIVE', [status: 'open'])
        def removed = snapshot(order, 2, 'DELETED', [status: 'open'])
        def writes = [write(1, created, sagaWriter('p1', 'create', 'FORWARD')),
                      write(2, removed, sagaWriter('p1', 'recover', 'RECOVERY'))]
        def execution = execution('COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [], [removed], writes)

        then:
        report.assessmentStatus() == 'COMPLETE'
        report.completeScore() == 0
        category(report, 'FAILED_OPERATION_RESIDUAL').candidateCount() == 1
        category(report, 'FAILED_OPERATION_RESIDUAL').findings().empty
        report.finalState() == [removed]
        report.committedWrites() == writes

        when: 'another active object still depends on the compensated creation'
        def item = id('com.example.dummyapp.item.aggregate.Item', 3)
        def dependency = new ImpactEvidence.Dependency(item, order, 2, 1L, 'OrderChanged', 'OrderSubscription')
        def dependant = snapshot(item, 1, 'ACTIVE', [name: 'item'], [dependency])
        def referenced = assess(execution, [], [removed, dependant], writes)

        then:
        referenced.assessmentStatus() == 'COMPLETE'
        referenced.completeScore() == 1
        category(referenced, 'FAILED_OPERATION_RESIDUAL').findings().empty
        category(referenced, 'DELETED_DEPENDENCY').findings()*.affectedObject() == [item]
    }

    def 'the creation-remnant exclusion preserves other residual effects'() {
        given:
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def before = snapshot(order, 1, 'ACTIVE', [count: 1])
        def after = snapshot(order, 2, finalLifecycle, [count: 0])
        def execution = execution('COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, preexisting ? [before] : [], [after], [
                write(1, before, sagaWriter('p1', 'create-or-update', 'FORWARD')),
                write(2, after, sagaWriter('p1', 'last-write', lastPhase))])

        then:
        report.assessmentStatus() == 'COMPLETE'
        report.completeScore() == 1
        category(report, 'FAILED_OPERATION_RESIDUAL').findings()*.affectedObject() == [order]

        where:
        preexisting | finalLifecycle | lastPhase
        false       | 'ACTIVE'       | 'FORWARD'
        false       | 'INACTIVE'     | 'RECOVERY'
        false       | 'DELETED'      | 'FORWARD'
        true        | 'ACTIVE'       | 'RECOVERY'
        true        | 'DELETED'      | 'RECOVERY'
    }

    def 'a forward deletion is not reclassified as a compensated creation by a later recovery write'() {
        given:
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def created = snapshot(order, 1, 'ACTIVE', [status: 'open'])
        def removed = snapshot(order, 2, 'DELETED', [status: 'open'])
        def recovered = snapshot(order, 3, 'DELETED', [status: 'open'])
        def execution = execution('COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [], [recovered], [
                write(1, created, sagaWriter('p1', 'create', 'FORWARD')),
                write(2, removed, sagaWriter('p1', 'remove', 'FORWARD')),
                write(3, recovered, sagaWriter('p1', 'recover', 'RECOVERY'))])

        then:
        report.assessmentStatus() == 'COMPLETE'
        report.completeScore() == 1
    }

    def 'a deleted creation does not turn incomplete or ambiguous recovery into a complete zero'() {
        given:
        def order = id('com.example.dummyapp.order.aggregate.Order', 2)
        def created = snapshot(order, 1, 'ACTIVE', [status: 'open'])
        def removed = snapshot(order, 2, 'DELETED', [status: 'open'])
        def execution = execution('COMPENSATED', 'EXACT', [failedAction('p1')],
                recovered ? [compensatedParticipant('p1')] : [],
                recovered ? [compensatedLifecycle('p1')] : [])
        def gap = new ImpactEvidence.CoverageGap('SNAPSHOT', 'baseline',
                'PERSISTENT_ATTRIBUTE_UNSUPPORTED', 'baseline not fully observed')

        when:
        def report = assess(execution, [], [removed], [
                write(1, created, sagaWriter('p1', 'create', 'FORWARD')),
                write(2, removed, competing ? eventWriter(9) : sagaWriter('p1', 'recover', 'RECOVERY'))
        ], [], missingBaseline ? [gap] : [])

        then:
        report.assessmentStatus() == 'PARTIAL'
        report.completeScore() == null
        category(report, 'FAILED_OPERATION_RESIDUAL').findings().empty
        !category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons().empty

        where:
        recovered | competing | missingBaseline
        false     | false     | false
        true      | true      | false
        true      | false     | true
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

    def 'empty event attempts neither create delivery candidates nor hide an independent delivered event finding'() {
        given:
        def item = id('com.example.dummyapp.item.aggregate.Item', 1)
        def unchanged = snapshot(item, 1, 'ACTIVE', [name: 'same'])

        when: 'the completed schedule contains only a confirmed empty route attempt'
        def emptyOnly = assess(execution('SUCCESS', 'EXACT', [noEligibleEventAction('p1', 8)]),
                [], [], [], [])

        then:
        emptyOnly.assessmentStatus() == 'COMPLETE'
        emptyOnly.completeScore() == 0
        category(emptyOnly, 'UNRESOLVED_DELIVERED_EVENT').with {
            candidateCount() == 0
            positiveObjectCount() == 0
            unknownReasons().empty
        }

        when: 'another route in the same schedule has exact unresolved-delivery evidence'
        def mixed = assess(execution('SUCCESS', 'EXACT', [
                noEligibleEventAction('p1', 8), successfulEventAction('p1', 9)
        ]), [], [], [], [delivery(9, unchanged, unchanged, unchanged, true)])

        then:
        mixed.assessmentStatus() == 'COMPLETE'
        mixed.completeScore() == 1
        category(mixed, 'UNRESOLVED_DELIVERED_EVENT').with {
            candidateCount() == 1
            positiveObjectCount() == 1
            findings()*.eventId() == [9]
            unknownReasons().empty
        }

        when: 'independent deletion and failed-recovery effects coexist with the empty route'
        def source = id('com.example.dummyapp.item.aggregate.Item', 2)
        def target = id('com.example.dummyapp.order.aggregate.Order', 3)
        def dependency = new ImpactEvidence.Dependency(source, target, 3, 1L,
                'OrderChanged', 'OrderSubscription')
        def sourceState = snapshot(source, 1, 'ACTIVE', [name: 'source'], [dependency])
        def targetBefore = snapshot(target, 1, 'ACTIVE', [status: 'open'])
        def targetDeleted = snapshot(target, 2, 'DELETED', [status: 'open'])
        def independent = assess(execution('COMPENSATED', 'EXACT', [
                failedAction('p1'), noEligibleEventAction('p1', 8)
        ], [compensatedParticipant('p1')], [compensatedLifecycle('p1')]),
                [sourceState, targetBefore], [sourceState, targetDeleted],
                [write(1, targetDeleted, sagaWriter('p1', 'recover-order', 'RECOVERY'))], [])

        then:
        independent.assessmentStatus() == 'COMPLETE'
        independent.completeScore() == 2
        category(independent, 'DELETED_DEPENDENCY').positiveObjectCount() == 1
        category(independent, 'FAILED_OPERATION_RESIDUAL').positiveObjectCount() == 1
        category(independent, 'UNRESOLVED_DELIVERED_EVENT').with {
            candidateCount() == 0
            positiveObjectCount() == 0
            unknownReasons().empty
        }
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

    def 'exclusive observed fields distinguish residual data from successful participant changes'() {
        given:
        def fixture = exclusiveFieldsFixture(restored)

        when:
        def report = assess(fixture.execution, [fixture.baseline], [fixture.finalState], fixture.writes)
        def residual = category(report, 'FAILED_OPERATION_RESIDUAL')

        then:
        report.residualAssessmentPolicy() == 'exclusive-keyed-list-fields-v4'
        report.assessmentStatus() == 'COMPLETE'
        report.completeScore() == (restored ? 0 : 1)
        residual.unknownReasons().empty
        residual.findings()*.affectedFields() == (restored ? [] : [['/applicationData/labels']])
        residual.findings()*.versions() == (restored ? [] : [[1L, 2L, 3L, 4L, 5L]])
        residual.findings()*.actionIds() == (restored ? [] : [['other-after', 'other-before', 'recover', 'update']])

        when: 'input observation order differs, but sequence numbers retain the execution order'
        def reordered = assess(fixture.execution, [fixture.baseline], [fixture.finalState], fixture.writes.reverse())

        then:
        MAPPER.writeValueAsString(reordered.categoryResults()) == MAPPER.writeValueAsString(report.categoryResults())

        where:
        restored << [false, true]
    }

    def 'exclusive field assessment refuses unsupported attribution and interference'() {
        given:
        def f = exclusiveFieldsFixture(false)
        def last = f.finalState
        def lastWriter = f.writes.last().writer()
        def metadata = last.frameworkMetadata()
        switch (caseName) {
            case 'same field':
                last = successor(f.writes[2].aggregate(), 5, [labels: ['another'], participants: ['x', 'y'], count: 10])
                break
            case 'unknown writer': lastWriter = ImpactEvidence.Writer.unknown('unknown', 'step'); break
            case 'event writer': lastWriter = eventWriter(9); break
            case 'wrong attempt':
                lastWriter = new ImpactEvidence.Writer('SAGA', 'other-attempt', 'workload', 'p2', 'other-after',
                        'FORWARD', 'FixtureSaga', 'other-after', null)
                break
            case 'uncommitted writer':
                f.execution = execution('COMPENSATED', 'EXACT', [failedAction('p1')],
                        [compensatedParticipant('p1')], [compensatedLifecycle('p1')])
                break
            case 'broken predecessor':
                metadata = new ImpactEvidence.FrameworkMetadata(null, null, last.identity(), 2L, null, null)
                break
            case 'missing predecessor': metadata = null; break
            case 'wrong predecessor identity':
                metadata = new ImpactEvidence.FrameworkMetadata(null, null, id('Other', 99), 4L, null, null)
                break
        }
        if (caseName in ['broken predecessor', 'missing predecessor', 'wrong predecessor identity']) {
            last = new ImpactEvidence.AggregateSnapshot(last.identity(), last.version(), last.lifecycleState(),
                    last.runtimeType(), metadata, last.applicationData(), [])
        }
        f.writes[3] = write(caseName == 'duplicate sequence' ? 3 : 4, last, lastWriter)

        when:
        def report = assess(f.execution, caseName == 'absent baseline' ? [] : [f.baseline], [last], f.writes)

        then:
        report.assessmentStatus() == 'PARTIAL'
        report.completeScore() == null
        category(report, 'FAILED_OPERATION_RESIDUAL').findings().empty
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() == [reason]

        where:
        caseName                    | reason
        'same field'                | 'COMPETING_FIELD_CHANGE'
        'unknown writer'            | 'COMPETING_OR_UNKNOWN_WRITER'
        'event writer'              | 'COMPETING_OR_UNKNOWN_WRITER'
        'wrong attempt'             | 'COMPETING_OR_UNKNOWN_WRITER'
        'uncommitted writer'        | 'COMPETING_OR_UNKNOWN_WRITER'
        'broken predecessor'        | 'RESIDUAL_VERSION_CHAIN_INCOMPLETE'
        'missing predecessor'       | 'RESIDUAL_VERSION_CHAIN_INCOMPLETE'
        'wrong predecessor identity'| 'RESIDUAL_VERSION_CHAIN_INCOMPLETE'
        'duplicate sequence'        | 'RESIDUAL_VERSION_CHAIN_INCOMPLETE'
        'absent baseline'           | 'RESIDUAL_BASELINE_UNAVAILABLE'
    }

    def 'residual deletion can be attributed after another Saga changes unrelated data'() {
        given:
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(identity, 1, 'ACTIVE', [items: ['old']])
        def updated = successor(baseline, 2, [items: ['new']])
        def deleted = successor(updated, 3, [items: ['new']], 'DELETED')
        def execution = execution('PARTIAL_COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [deleted], [
                write(1, updated, sagaWriter('p2', 'update-items', 'FORWARD')),
                write(2, deleted, sagaWriter('p1', 'remove', 'FORWARD'))])

        then:
        report.completeScore() == 1
        category(report, 'FAILED_OPERATION_RESIDUAL').findings()*.affectedFields() == [['/lifecycleState']]
    }

    def 'another Saga lifecycle change is assessed by the same exclusive field rule'() {
        given:
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(identity, 1, 'ACTIVE', [label: 'before'])
        def failed = successor(baseline, 2, [label: 'failed'])
        def recovered = successor(failed, 3, [label: restored ? 'before' : 'residual'])
        def deleted = successor(recovered, 4, recovered.applicationData(), 'DELETED')
        def execution = execution('PARTIAL_COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [deleted], [
                write(1, failed, sagaWriter('p1', 'update', 'FORWARD')),
                write(2, recovered, sagaWriter('p1', 'recover', 'RECOVERY')),
                write(3, deleted, sagaWriter('p2', 'delete', 'FORWARD'))])

        then:
        report.assessmentStatus() == 'COMPLETE'
        report.completeScore() == (restored ? 0 : 1)
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons().empty
        category(report, 'FAILED_OPERATION_RESIDUAL').findings()*.affectedFields() ==
                (restored ? [] : [['/applicationData/label']])

        where:
        restored << [false, true]
    }

    def 'a lifecycle field changed by both the failed and committed Sagas remains unknown'() {
        given:
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(identity, 1, 'ACTIVE', [label: 'same'])
        def failed = successor(baseline, 2, baseline.applicationData(), 'DELETED')
        def recovered = successor(failed, 3, baseline.applicationData(), 'ACTIVE')
        def other = successor(recovered, 4, baseline.applicationData(), 'DELETED')
        def execution = execution('PARTIAL_COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [other], [
                write(1, failed, sagaWriter('p1', 'remove', 'FORWARD')),
                write(2, recovered, sagaWriter('p1', 'restore', 'RECOVERY')),
                write(3, other, sagaWriter('p2', 'other-remove', 'FORWARD'))])

        then:
        report.assessmentStatus() == 'PARTIAL'
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() == ['COMPETING_FIELD_CHANGE']
    }

    def 'an exactly proven successful event write participates in exclusive field attribution'() {
        given:
        def f = eventResidualFixture(restored)

        when:
        def report = assess(f.execution, [f.baseline], [f.finalState], f.writes, f.deliveries)

        then:
        report.assessmentStatus() == 'COMPLETE'
        report.completeScore() == (restored ? 0 : 1)
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons().empty
        category(report, 'FAILED_OPERATION_RESIDUAL').findings()*.affectedFields() ==
                (restored ? [] : [['/applicationData/failedField']])

        where:
        restored << [false, true]
    }

    def 'event writer provenance rejects #caseName'() {
        given:
        def f = eventResidualFixture(false)
        mutation(f)

        when:
        def report = assess(f.execution, [f.baseline], [f.finalState], f.writes, f.deliveries)

        then:
        report.assessmentStatus() == 'PARTIAL'
        category(report, 'FAILED_OPERATION_RESIDUAL').findings().empty
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() ==
                ['COMPETING_OR_UNKNOWN_WRITER']

        where:
        caseName        | mutation
        'wrong identity'| { fixture -> fixture.deliveries = [delivery(9, fixture.recovered,
                snapshot(id('Other', 1), 4, 'ACTIVE', fixture.finalState.applicationData()),
                fixture.finalState, false)] }
        'wrong version' | { fixture -> fixture.deliveries = [delivery(9, fixture.recovered,
                snapshot(fixture.finalState.identity(), 99, 'ACTIVE', fixture.finalState.applicationData()),
                fixture.finalState, false)] }
        'wrong state'   | { fixture -> fixture.deliveries = [delivery(9, fixture.recovered,
                snapshot(fixture.finalState.identity(), 4, 'ACTIVE',
                        [failedField: 'residual', eventField: 'wrong']), fixture.finalState, false)] }
        'wrong attempt' | { fixture -> fixture.writes[-1] = write(3, fixture.finalState,
                eventWriter(9, 'other-attempt')) }
        'duplicates'    | { fixture -> fixture.deliveries = [fixture.deliveries[0], fixture.deliveries[0]] }
        'failed action' | { fixture -> fixture.execution = execution('PARTIAL_COMPENSATED', 'EXACT', [
                failedAction('p1'), action('event-action', 'EVENT_CONSEQUENCE', 'p2', 'FAILED', 9)
        ], [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')]) }
    }

    def 'an event writer overlapping a failed Saga field remains unknown'() {
        given:
        def f = eventResidualFixture(false)
        def overlap = successor(f.recovered, 4, [failedField: 'event-change', eventField: 'after'])
        f.finalState = overlap
        f.writes[-1] = write(3, overlap, eventWriter(9))
        f.deliveries = [delivery(9, f.recovered, overlap, overlap, false)]

        when:
        def report = assess(f.execution, [f.baseline], [f.finalState], f.writes, f.deliveries)

        then:
        report.assessmentStatus() == 'PARTIAL'
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() == ['COMPETING_FIELD_CHANGE']
    }

    def 'different entries of one collection do not establish exclusive fields'() {
        given:
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(identity, 1, 'ACTIVE', [items: [a: 1, b: 1]])
        def failed = successor(baseline, 2, [items: [a: 2, b: 1]])
        def other = successor(failed, 3, [items: [a: 2, b: 2]])
        def execution = execution('PARTIAL_COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [other], [
                write(1, failed, sagaWriter('p1', 'update-a', 'FORWARD')),
                write(2, other, sagaWriter('p2', 'update-b', 'FORWARD'))])

        then:
        report.completeScore() == null
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() == ['COMPETING_FIELD_CHANGE']
    }

    @Unroll
    def 'stable keyed list proves a residual despite an event #placement recovery'() {
        given:
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(identity, 1, 'ACTIVE', [topics: topicRows(1, 'old', false)])
        def forward = successor(baseline, 2, [topics: topicRows(1, 'old', true)])
        def eventState
        def recovery
        def writes
        if (placement == 'before') {
            eventState = successor(forward, 3, [topics: topicRows(1, 'renamed', true)])
            recovery = successor(eventState, 4, [topics: topicRows(null, 'old', false)])
            writes = [write(1, forward, sagaWriter('p1', 'update', 'FORWARD')),
                      write(2, eventState, eventWriter(9)),
                      write(3, recovery, sagaWriter('p1', 'recover', 'RECOVERY'))]
        } else {
            recovery = successor(forward, 3, [topics: topicRows(null, 'old', false)])
            eventState = successor(recovery, 4, [topics: topicRows(null, 'renamed', false)])
            writes = [write(1, forward, sagaWriter('p1', 'update', 'FORWARD')),
                      write(2, recovery, sagaWriter('p1', 'recover', 'RECOVERY')),
                      write(3, eventState, eventWriter(9))]
        }
        def finalState = placement == 'before' ? recovery : eventState
        def execution = execution('PARTIAL_COMPENSATED', 'EXACT',
                [failedAction('p1'), successfulEventAction('p2', 9, 2)],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [finalState], writes,
                [delivery(9, placement == 'before' ? forward : recovery, eventState, finalState, false)])

        then:
        report.completeScore() == 1
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons().empty
        category(report, 'FAILED_OPERATION_RESIDUAL').findings()*.affectedFields() == [[
                '/applicationData/topics/topicAggregateId=5/topicCourseAggregateId',
                '/applicationData/topics/topicAggregateId=6/topicCourseAggregateId']]

        where:
        placement << ['before', 'after']
    }

    def 'keyed list without an exclusive final field remains unknown'() {
        given:
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(identity, 1, 'ACTIVE', [topics: topicRows(1, 'old', false)])
        def failed = successor(baseline, 2, [topics: topicRows(1, 'failed-name', false)])
        def other = successor(failed, 3, [topics: topicRows(1, 'other-name', false)])
        def execution = execution('PARTIAL_COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [other], [
                write(1, failed, sagaWriter('p1', 'update', 'FORWARD')),
                write(2, other, sagaWriter('p2', 'other', 'FORWARD'))])

        then:
        report.completeScore() == null
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() == ['COMPETING_FIELD_CHANGE']
    }

    def 'keyed list attribution is generic across collection and item field names'() {
        given:
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(identity, 1, 'ACTIVE',
                [entries: [[entryAggregateId: 7, ownerAggregateId: 3, label: 'old']]])
        def failed = successor(baseline, 2,
                [entries: [[entryAggregateId: 7, ownerAggregateId: null, label: 'old']]])
        def other = successor(failed, 3,
                [entries: [[entryAggregateId: 7, ownerAggregateId: null, label: 'new']]])
        def execution = execution('PARTIAL_COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [other], [
                write(1, failed, sagaWriter('p1', 'unlink', 'FORWARD')),
                write(2, other, sagaWriter('p2', 'rename', 'FORWARD'))])

        then:
        report.completeScore() == 1
        category(report, 'FAILED_OPERATION_RESIDUAL').findings()*.affectedFields() == [[
                '/applicationData/entries/entryAggregateId=7/ownerAggregateId']]
    }

    def 'duplicate collection identity does not support keyed residual attribution'() {
        given:
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def original = [[topicAggregateId: 5, topicCourseAggregateId: 1],
                        [topicAggregateId: 5, topicCourseAggregateId: 1]]
        def changed = [[topicAggregateId: 5, topicCourseAggregateId: null], original[1]]
        def other = [changed[0], [topicAggregateId: 5, topicCourseAggregateId: 2]]
        def baseline = snapshot(identity, 1, 'ACTIVE', [topics: original])
        def failed = successor(baseline, 2, [topics: changed])
        def finalState = successor(failed, 3, [topics: other])
        def execution = execution('PARTIAL_COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [finalState], [
                write(1, failed, sagaWriter('p1', 'update', 'FORWARD')),
                write(2, finalState, sagaWriter('p2', 'other', 'FORWARD'))])

        then:
        report.completeScore() == null
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason() == ['COMPETING_FIELD_CHANGE']
    }

    def 'exclusive field rule preserves incomplete recovery evidence and final state checks'() {
        given:
        def f = exclusiveFieldsFixture(false)
        def gaps = []
        def baseline = f.baseline
        def finalState = f.finalState
        if (condition == 'recovery') f.execution = execution('PARTIAL_COMPENSATED', 'EXACT',
                [failedAction('p1')], [committedParticipant('p2')], [])
        if (condition == 'two failed writers') f.execution = execution('COMPENSATED', 'EXACT',
                [failedAction('p1'), failedAction('p2')], [compensatedParticipant('p1'), compensatedParticipant('p2')],
                [compensatedLifecycle('p1'), compensatedLifecycle('p2')])
        if (condition == 'snapshot gap') gaps = [new ImpactEvidence.CoverageGap('SNAPSHOT', 'baseline',
                'PERSISTENT_ATTRIBUTE_UNSUPPORTED', 'unobserved value')]
        if (condition == 'final revision') finalState = successor(finalState, 6, finalState.applicationData())

        when:
        def report = assess(f.execution, [baseline], [finalState], f.writes, [], gaps)

        then:
        report.completeScore() == null
        category(report, 'FAILED_OPERATION_RESIDUAL').findings().empty
        category(report, 'FAILED_OPERATION_RESIDUAL').unknownReasons()*.reason().contains(reason)

        where:
        condition            | reason
        'recovery'           | 'FAILED_SAGA_RECOVERY_INCOMPLETE'
        'two failed writers' | 'FAILED_WRITER_IDENTITY_UNAVAILABLE'
        'snapshot gap'       | 'PERSISTENT_ATTRIBUTE_UNSUPPORTED'
        'final revision'     | 'FINAL_STATE_NOT_EXPLAINED_BY_TRACKED_WRITE'
    }

    def 'policy and field evidence round trip without relabelling retained legacy reports'() {
        given:
        def f = exclusiveFieldsFixture(false)
        def report = assess(f.execution, [f.baseline], [f.finalState], f.writes)

        when:
        def json = MAPPER.writeValueAsString(report)
        def reread = MAPPER.readValue(json, ImpactV2EvidenceReport)
        def legacy = MAPPER.readTree(json)
        legacy.remove('residualAssessmentPolicy')

        then:
        reread == report
        MAPPER.treeToValue(legacy, ImpactV2EvidenceReport).residualAssessmentPolicy() == 'whole-object-single-writer-v1'
    }

    def 'multiple residual fields still count one object and distinguish null from absence'() {
        given:
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(identity, 1, 'ACTIVE', [count: 1, participants: []])
        def changed = successor(baseline, 2, [count: 2, participants: [], 'a/b~c': null])
        def other = successor(changed, 3, [count: 2, participants: ['x'], 'a/b~c': null])
        def execution = execution('PARTIAL_COMPENSATED', 'EXACT', [failedAction('p1')],
                [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')])

        when:
        def report = assess(execution, [baseline], [other], [
                write(1, changed, sagaWriter('p1', 'change', 'FORWARD')),
                write(2, other, sagaWriter('p2', 'participants', 'FORWARD'))])

        then:
        report.completeScore() == 1
        category(report, 'FAILED_OPERATION_RESIDUAL').positiveObjectCount() == 1
        category(report, 'FAILED_OPERATION_RESIDUAL').findings()*.affectedFields() ==
                [['/applicationData/a~1b~0c', '/applicationData/count']]
    }

    private static Map exclusiveFieldsFixture(boolean restored) {
        def identity = id('com.example.dummyapp.order.aggregate.Order', 2)
        def baseline = snapshot(identity, 1, 'ACTIVE', [labels: ['old'], participants: [], count: 10])
        def other = successor(baseline, 2, [labels: ['old'], participants: ['x'], count: 10])
        def failed = successor(other, 3, [labels: ['new'], participants: ['x'], count: 20])
        def recovered = successor(failed, 4, [labels: restored ? ['old'] : [null], participants: ['x'], count: 10])
        def finalState = successor(recovered, 5, [labels: recovered.applicationData().labels, participants: ['x', 'y'], count: 10])
        [baseline: baseline, finalState: finalState,
         execution: execution('PARTIAL_COMPENSATED', 'EXACT', [failedAction('p1')],
                 [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')]),
         writes: [write(1, other, sagaWriter('p2', 'other-before', 'FORWARD')),
                  write(2, failed, sagaWriter('p1', 'update', 'FORWARD')),
                  write(3, recovered, sagaWriter('p1', 'recover', 'RECOVERY')),
                  write(4, finalState, sagaWriter('p2', 'other-after', 'FORWARD'))]]
    }

    private static Map eventResidualFixture(boolean restored) {
        def identity = id('com.example.dummyapp.item.aggregate.Item', 1)
        def baseline = snapshot(identity, 1, 'ACTIVE', [failedField: 'before', eventField: 'before'])
        def failed = successor(baseline, 2, [failedField: 'forward', eventField: 'before'])
        def recovered = successor(failed, 3,
                [failedField: restored ? 'before' : 'residual', eventField: 'before'])
        def finalState = successor(recovered, 4,
                [failedField: recovered.applicationData().failedField, eventField: 'after'])
        [baseline: baseline, recovered: recovered, finalState: finalState,
         execution: execution('PARTIAL_COMPENSATED', 'EXACT', [
                 failedAction('p1'), successfulEventAction('p2', 9)
         ], [compensatedParticipant('p1'), committedParticipant('p2')], [compensatedLifecycle('p1')]),
         writes: [write(1, failed, sagaWriter('p1', 'update', 'FORWARD')),
                  write(2, recovered, sagaWriter('p1', 'recover', 'RECOVERY')),
                  write(3, finalState, eventWriter(9))],
         deliveries: [delivery(9, recovered, finalState, finalState, false)]]
    }

    private static List<Map<String, Object>> topicRows(Integer courseId, String firstName,
                                                        boolean third) {
        def rows = [[topicAggregateId: 5, topicCourseAggregateId: courseId,
                     topicName: firstName, topicVersion: firstName == 'renamed' ? 2 : 1],
                    [topicAggregateId: 6, topicCourseAggregateId: courseId,
                     topicName: 'second', topicVersion: 1]]
        if (third) rows << [topicAggregateId: 7, topicCourseAggregateId: 1,
                            topicName: 'third', topicVersion: 1]
        rows
    }

    private static ImpactEvidence.AggregateSnapshot successor(ImpactEvidence.AggregateSnapshot previous,
                                                               long version, Map data, String lifecycle = 'ACTIVE') {
        new ImpactEvidence.AggregateSnapshot(previous.identity(), version, lifecycle, previous.runtimeType(),
                new ImpactEvidence.FrameworkMetadata(null, null, previous.identity(), previous.version(), null, null), data, [])
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

    private static ImpactEvidence.Writer eventWriter(int eventId, String attempt = 'attempt') {
        new ImpactEvidence.Writer('EVENT_CONSUMER', attempt, 'workload', 'p2', 'event-action', 'EVENT',
                'FixtureHandling', 'handle', eventId)
    }

    private static ImpactEvidence.EventDelivery delivery(int eventId,
                                                         ImpactEvidence.AggregateSnapshot before,
                                                         ImpactEvidence.AggregateSnapshot after,
                                                         ImpactEvidence.AggregateSnapshot finalState,
                                                         Boolean finalEligibility,
                                                         ImpactEvidence.Writer writer = eventWriter(eventId)) {
        new ImpactEvidence.EventDelivery(10, eventId, 'OrderChanged', 2, 1L, before, after,
                true, true, finalState, finalEligibility, writer)
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

    private static ScenarioExecutionReport.ActionOutcome noEligibleEventAction(String saga, int eventId) {
        action('empty-event-action', 'EVENT_CONSEQUENCE', saga, 'NO_ELIGIBLE_SUBSCRIBER', eventId, 0)
    }

    private static ScenarioExecutionReport.ActionOutcome action(String actionId, String kind, String saga,
                                                                 String status, Integer eventId,
                                                                 int subscriberId = 1) {
        def eventEvidence = eventId == null ? null : new ScenarioExecutionReport.EventRuntimeEvidence(
                eventId, 'OrderChanged', 2, 1L, true,
                status == 'NO_ELIGIBLE_SUBSCRIBER' ? null : subscriberId,
                'FixtureHandling', 'handle', 'FixtureHandler')
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
