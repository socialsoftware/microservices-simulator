package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserverHolder
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserver
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.PersistentStateObserver
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence
import spock.lang.Specification
import spock.lang.Unroll

import static pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.SagaReadExposureReport.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class SagaReadExposureSpec extends Specification {
    static final String TYPE = 'com.example.dummyapp.item.aggregate.Item'
    static final def ID = new ImpactEvidence.AggregateIdentity(TYPE, 7)
    static final def CONTRACT = new ReadResponseEvidence.Contract('dummyapp.item.outer-revision', '1',
            'com.example.dummyapp.diagnostics.ReadResponseFixture$Inspect',
            'com.example.dummyapp.diagnostics.ReadResponseFixture$Revision', TYPE, TYPE)
    static final def JSON = new ObjectMapper()

    def cleanup() {
        System.clearProperty(ImpactV2EvidenceCollector.ENABLED_PROPERTY)
        System.clearProperty(SagaReadExposureCollector.ENABLED_PROPERTY)
    }

    def 'dummyapp creation delivered to a reader without writes is observed with metadata-only self-contained proof'() {
        when:
        def result = assess(fixture())
        def json = JSON.writeValueAsString(result)

        then:
        result.collectionCoverage() == 'COMPLETE_WITHIN_SCOPE'
        result.executionValidity() == 'COMPLETE'
        result.observedExposureCount() == 1
        result.findings()[0].readerSagaId() == 'B'
        result.findings()[0].createdVersion() == 10L
        result.findings()[0].deletedVersion() == 11L
        result.findings()[0].sourceScheduledStepId() == 'source-create'
        result.findings()[0].checkpointId() == 'checkpoint-create'
        result.committedWrites()*.order() == [1L, 3L]
        result.calls()*.order() == [2L]
        result.committedWrites()*.writer()*.sagaInstanceId().unique() == ['A']
        result.actions().find { it.id() == 'action-read' }.commitOutcome() == 'SUCCEEDED'
        result.baselineAbsenceCovered()
        result.artifacts()[0].status() == 'UNAVAILABLE'
        !json.contains('secret payload')
        !json.contains('applicationData')
        !json.contains('exceptionMessage')
        !json.contains('impactScore')
        !json.contains('completeScore')
    }

    @Unroll
    def 'dummyapp proof matrix: #caseName gives #verdict because #reason'() {
        when:
        def result = assess(caseFixture(caseName))

        then:
        result.assessments()[0].verdict() == verdict
        result.assessments()[0].reason() == reason
        result.observedExposureCount() == 0
        (verdict == 'UNKNOWN') == (result.collectionCoverage() == 'PARTIAL')

        where:
        caseName                 | verdict        | reason
        'successful creator'     | 'NOT_OBSERVED' | 'NO_SUBSEQUENT_COMPENSATING_DELETION'
        'different returned'     | 'NOT_OBSERVED' | 'REVISION_PREEXISTS_MEASUREMENT'
        'ordinary later change'  | 'NOT_OBSERVED' | 'NO_SUBSEQUENT_COMPENSATING_DELETION'
        'delete another object'  | 'NOT_OBSERVED' | 'NO_SUBSEQUENT_COMPENSATING_DELETION'
        'release lock only'      | 'NOT_OBSERVED' | 'NO_SUBSEQUENT_COMPENSATING_DELETION'
        'failed call'            | 'NOT_OBSERVED' | 'FAILED_CALL_WITHOUT_DELIVERY'
        'already deleted'        | 'NOT_OBSERVED' | 'DELETION_PRECEDES_DELIVERY'
        'creator completed'      | 'NOT_OBSERVED' | 'PRODUCER_COMPLETED_BEFORE_DELIVERY'
        'uncovered baseline'     | 'UNKNOWN'      | 'BASELINE_ABSENCE_UNCOVERED'
        'local rollback'         | 'UNKNOWN'      | 'RETURNED_REVISION_UNATTRIBUTED'
        'failed reload'          | 'UNKNOWN'      | 'WRITE_COVERAGE_INCOMPLETE'
        'wrong predecessor'      | 'UNKNOWN'      | 'DIRECT_DELETION_PREDECESSOR_UNPROVEN'
        'missing metadata'       | 'UNKNOWN'      | 'CREATION_PREDECESSOR_UNAVAILABLE'
        'competing revision'     | 'UNKNOWN'      | 'AMBIGUOUS_REVISION_PRODUCER'
        'competing writer'       | 'UNKNOWN'      | 'INTERMEDIATE_REVISION_OUT_OF_SCOPE'
        'wrong checkpoint'       | 'UNKNOWN'      | 'RECOVERY_CHECKPOINT_UNPROVEN'
        'wrong occurrence'       | 'UNKNOWN'      | 'COMPENSATION_PRODUCING_OCCURRENCE_MISMATCH'
        'restored update'        | 'UNKNOWN'      | 'NON_CREATION_EFFECT_OUT_OF_SCOPE'
        'type collision'         | 'UNKNOWN'      | 'PERSISTENT_RUNTIME_TYPE_COLLISION'
        'unknown author'         | 'UNKNOWN'      | 'PRODUCER_ACTION_UNPROVEN'
        'old attempt reader'     | 'UNKNOWN'      | 'READER_ACTION_UNPROVEN'
        'missing reader action'  | 'UNKNOWN'      | 'READER_ACTION_UNPROVEN'
        'failed explicit'        | 'UNKNOWN'      | 'EXPLICIT_COMPENSATION_SUCCESS_UNPROVEN'
        'ambiguous explicit'     | 'UNKNOWN'      | 'EXPLICIT_COMPENSATION_SUCCESS_UNPROVEN'
        'implicit only'          | 'UNKNOWN'      | 'EXPLICIT_COMPENSATION_SUCCESS_UNPROVEN'
        'invalid failed call'    | 'UNKNOWN'      | 'MISSING_READER_ATTRIBUTION'
    }

    def 'a proven exposure survives reader recovery later recreation and a failed implicit rollback'() {
        given:
        def data = fixture()
        data.execution = change(data.execution, [terminalStatus: 'COMPENSATION_FAILED', scheduleConformance: 'INCOMPLETE',
                participants: data.execution.participants().collect { change(it, [finalState: 'COMPENSATED']) },
                actualActions: data.execution.actualActions().collect { action -> action.kind() == 'COMPENSATION'
                        ? change(action, [status: 'COMPENSATION_FAILED', recoverySubOutcomes: [
                            recovery('EXPLICIT_COMPENSATION', 'SUCCEEDED'), recovery('IMPLICIT_SAGA_ROLLBACK', 'FAILED')]]) : action }])
        data.events << [write: snapshot(12L, 'ACTIVE', 11L), writer: writer('A', 'action-recover', 'RECOVERY', 'write')]

        when:
        def result = assess(data)

        then:
        result.observedExposureCount() == 1
        result.findings()[0].verdict() == 'OBSERVED'
        result.executionValidity() == 'INCOMPLETE'
        result.collectionCoverage() == 'PARTIAL'
        result.gaps()*.reason().contains('INCOMPLETE_EXECUTION_PREFIX')
    }

    def 'successful creator followed by a proven ordinary deletion is an evaluated negative while unknown authors remain gaps'() {
        given:
        def data = caseFixture('successful creator')
        def ordinaryDelete = change(data.execution.actualActions()[0], [actionId: 'action-delete', sagaInstanceId: 'C',
                sourceScheduledStepId: 'source-delete', sourceStepId: 'remove#0', runtimeStepName: 'remove',
                runtimeOccurrenceId: 'source-delete', actualPosition: 3, plannedPosition: 3, commitOutcome: 'SUCCEEDED'])
        data.execution = change(data.execution, [actualActions: data.execution.actualActions() + [ordinaryDelete],
                participants: data.execution.participants() + [participant('C')]])
        data.sources = new SourceContract(data.sources.occurrences() + [new Occurrence('source-delete', 'C', 'remove#0', 'remove', 3)], data.sources.checkpoints())
        data.events << [write: snapshot(11L, 'DELETED', 10L), writer: writer('C', 'action-delete', 'FORWARD', 'remove')]

        expect:
        assess(data).collectionCoverage() == 'COMPLETE_WITHIN_SCOPE'
        assess(data).observedExposureCount() == 0
        assess(data).assessments()[0].verdict() == 'NOT_OBSERVED'
        assess(data).assessments()[0].reason() == 'ORDINARY_DELETION_AFTER_PRODUCER_SUCCESS'

        when:
        data.events[-1].writer = change(data.events[-1].writer, [actionId: 'unproven-delete'])

        then:
        assess(data).collectionCoverage() == 'PARTIAL'
        assess(data).assessments()[0].reason() == 'DELETION_COMPENSATION_AUTHOR_UNPROVEN'

        when:
        data.events[-1].writer = null

        then:
        assess(data).collectionCoverage() == 'PARTIAL'
        assess(data).assessments()[0].reason() == 'DELETION_COMPENSATION_AUTHOR_UNPROVEN'
    }

    def 'an interrupted prefix without proven deletion is unknown rather than a complete zero'() {
        given:
        def data = fixture()
        data.events.removeLast()
        data.execution = change(data.execution, [terminalStatus: 'UNEXPECTED_EXECUTION_FAILURE', scheduleConformance: 'INCOMPLETE'])

        expect:
        assess(data).assessments()[0].reason() == 'COMPENSATION_HORIZON_INCOMPLETE'
        assess(data).collectionCoverage() == 'PARTIAL'
    }

    def 'deduplicate repeated deliveries per reader but preserve distinct readers and stable attempt-local ids'() {
        given:
        def data = fixture()
        data.events.add(2, [read: observation()])
        def cAction = change(data.execution.actualActions()[1], [actionId: 'action-read-c', sagaInstanceId: 'C',
                sourceScheduledStepId: 'source-read-c', runtimeOccurrenceId: 'source-read-c', actualPosition: 2])
        data.sources = new SourceContract(data.sources.occurrences().collect { it.order() >= 2 ? change(it, [order: it.order() + 1]) : it }
                + [new Occurrence('source-read-c', 'C', 'inspect#0', 'inspect', 2)], data.sources.checkpoints())
        data.execution = change(data.execution, [actualActions: data.execution.actualActions().collect {
            it.actualPosition() >= 2 ? change(it, [actualPosition: it.actualPosition() + 1, plannedPosition: it.plannedPosition() + 1]) : it } + [cAction],
                participants: data.execution.participants() + [participant('C')]])
        data.events.add(3, [read: change(observation(), [reader: writer('C', 'action-read-c', 'FORWARD', 'inspect')])])

        when:
        def first = assess(data)
        def second = assess(data)

        then:
        first.observedExposureCount() == 2
        first.findings()*.deliveryIds()*.size() == [2, 1]
        first.findings()*.readerSagaId() == ['B', 'C']
        JSON.writeValueAsString(first) == JSON.writeValueAsString(second)
    }

    def 'a new attempt never accepts delayed prior-attempt writer or reader facts'() {
        given:
        def data = fixture()
        def diagnostic = new SagaReadExposureCollector('attempt-next', 'workload', 'scenario', data.sources)
        diagnostic.begin(new ImpactEvidence.SnapshotBatch([], []), [CONTRACT])
        data.events.each { collect(diagnostic, it) }
        def execution = change(data.execution, [executionAttemptId: 'attempt-next'])

        expect:
        diagnostic.report(execution, []).assessments()[0].reason() == 'READER_ACTION_UNPROVEN'
        diagnostic.report(execution, []).observedExposureCount() == 0
    }

    def 'runtime recovery requires its explicit exact checkpoint and rejects repeated source names'() {
        given:
        def data = fixture()
        data.execution = change(data.execution, [actualActions: data.execution.actualActions().collect { action ->
            action.kind() == 'COMPENSATION' ? change(action, [plannedPosition: null]) : action }])

        expect:
        assess(data).observedExposureCount() == 1

        when:
        def duplicate = change(data.execution.actualActions()[0], [actionId: 'action-create-again', sourceScheduledStepId: 'source-create-again',
                sourceStepId: 'write#1', runtimeOccurrenceId: 'source-create-again', actualPosition: 1, plannedPosition: 1])
        data.execution = change(data.execution, [actualActions: data.execution.actualActions().collect {
            it.actualPosition() >= 1 ? change(it, [actualPosition: it.actualPosition() + 1,
                    plannedPosition: it.plannedPosition() == null ? null : it.plannedPosition() + 1]) : it } + [duplicate]])
        data.sources = new SourceContract(data.sources.occurrences().collect { it.order() >= 1 ? change(it, [order: it.order() + 1]) : it }
                + [new Occurrence('source-create-again', 'A', 'write#1', 'write', 1)], data.sources.checkpoints())

        then:
        assess(data).assessments()[0].reason() == 'AMBIGUOUS_RUNTIME_RECOVERY_SOURCE'
    }

    def 'unmapped and excluded calls are scope facts while invalid and failed-invalid calls are gaps without deliveries'() {
        given:
        def data = fixture()
        data.events = [
                [read: change(observation(), [outcome: ReadResponseEvidence.Outcome.EXCLUDED, reason: 'OBSERVER'])],
                [read: change(observation(), [outcome: ReadResponseEvidence.Outcome.DELIVERED_UNMAPPED, contract: null])],
                [read: change(observation(), [outcome: ReadResponseEvidence.Outcome.FAILED_INVALID, reason: 'MISSING_READER_ATTRIBUTION'])],
                [read: change(observation(), [outcome: ReadResponseEvidence.Outcome.DELIVERED_INVALID, reason: 'MISSING_RETURNED_REVISION'])]]

        when:
        def result = assess(data)

        then:
        result.assessments()*.verdict() == ['EXCLUDED', 'EXCLUDED', 'UNKNOWN', 'UNKNOWN']
        result.findings().empty
        result.collectionCoverage() == 'PARTIAL'
        result.gaps()*.reason() == ['MISSING_READER_ATTRIBUTION', 'MISSING_RETURNED_REVISION']
    }

    def 'no measured actions or usable adapter scope produces unavailable null rather than a zero'() {
        given:
        def data = fixture()
        def diagnostic = new SagaReadExposureCollector('attempt', 'workload', 'scenario', data.sources)

        expect:
        diagnostic.report(data.execution, []).observedExposureCount() == null
        diagnostic.report(data.execution, []).collectionReason() == 'MEASUREMENT_NOT_STARTED'

        when:
        diagnostic.begin(new ImpactEvidence.SnapshotBatch([], []), [])

        then:
        diagnostic.report(data.execution, []).observedExposureCount() == null
        diagnostic.report(data.execution, []).collectionReason() == 'NO_USABLE_ADAPTER_SCOPE'

        when:
        data.execution = change(data.execution, [actualActions: []])

        then:
        assess(data).observedExposureCount() == null
        assess(data).collectionReason() == 'NO_MEASURED_ACTIONS'
    }

    def 'uninvoked duplicate adapter scope is unavailable and mixed ambiguous scope preserves explicit gaps'() {
        given:
        def data = fixture()
        def onlyAmbiguous = new SagaReadExposureCollector('attempt', 'workload', 'scenario', data.sources)
        onlyAmbiguous.begin(new ImpactEvidence.SnapshotBatch([], []), [CONTRACT, CONTRACT])
        def distinct = change(CONTRACT, [id: 'other', commandType: 'other.Command', responseType: 'other.Result'])
        def mixed = new SagaReadExposureCollector('attempt', 'workload', 'scenario', data.sources)
        mixed.begin(new ImpactEvidence.SnapshotBatch([], []), [CONTRACT, CONTRACT, distinct])

        expect:
        onlyAmbiguous.report(data.execution, []).collectionCoverage() == 'UNAVAILABLE'
        onlyAmbiguous.report(data.execution, []).observedExposureCount() == null
        mixed.report(data.execution, []).collectionCoverage() == 'PARTIAL'
        mixed.report(data.execution, []).observedExposureCount() == 0
        mixed.report(data.execution, []).gaps()*.reason() == ['AMBIGUOUS_DECLARED_CONTRACT']
    }

    def 'composition leaves ImpactV2 write-event sequence and gaps unchanged and never repeats snapshot queries'() {
        given:
        def data = fixture()
        def state = Mock(PersistentStateObserver)
        def runtime = Stub(ScenarioRuntimeContext) {
            bean(PersistentStateObserver) >> state
            beans(_) >> []
        }
        def diag = new SagaReadExposureCollector('attempt', 'workload', 'scenario', data.sources)
        def composed = new ImpactV2EvidenceCollector('attempt', 'workload', 'scenario', diag)

        when:
        composed.start(runtime)
        composed.committedWrite(snapshot(10L, 'ACTIVE', null), writer('A', 'action-create', 'FORWARD', 'write'))
        composed.readResponse(observation())
        composed.eventDelivery(new ImpactEvidence.EventDelivery(99, 1, 'event', 7, 10L, null, null, false, false, null, null,
                writer('A', 'action-create', 'FORWARD', 'write')))
        composed.committedWrite(snapshot(11L, 'DELETED', 10L), writer('A', 'action-recover', 'RECOVERY', 'write'))
        diag.readFailure(new ImpactEvidence.CoverageGap('READ', null, 'READ_CALLBACK_FAILED', 'secret payload'))
        composed.finish()
        def impact = composed.report(data.execution)
        def diagnostic = diag.report(data.execution, [])

        then:
        2 * state.snapshotAll() >> new ImpactEvidence.SnapshotBatch([], [])
        1 * state.observeAtHorizon(_) >> { ImpactEvidence.EventDelivery delivery -> new PersistentStateObserver.HorizonObservation(delivery, []) }
        0 * state._
        impact.committedWrites()*.sequence() == [1L, 3L]
        impact.eventDeliveries()*.sequence() == [2L]
        impact.coverageGaps().empty
        diagnostic.committedWrites()*.order() == [1L, 3L]
        diagnostic.calls()*.order() == [2L]
        diagnostic.gaps()*.reason().contains('READ_CALLBACK_FAILED')
    }

    def 'write collection disabled remains disabled even with an opted-in diagnostic'() {
        given:
        System.setProperty(ImpactV2EvidenceCollector.ENABLED_PROPERTY, 'false')
        def data = fixture()
        def diagnostic = new SagaReadExposureCollector('attempt', 'workload', 'scenario', data.sources)
        def composed = new ImpactV2EvidenceCollector('attempt', 'workload', 'scenario', diagnostic)
        def runtime = Mock(ScenarioRuntimeContext)

        when:
        composed.start(runtime)

        then:
        !composed.started()
        !composed.isReadObservationEnabled()
        diagnostic.report(data.execution, []).observedExposureCount() == null
        diagnostic.report(data.execution, []).collectionReason() == 'WRITE_COLLECTION_DISABLED'
        0 * runtime._
    }

    def 'retained read callback failure reaches only the diagnostic and is drained exactly once'() {
        given:
        def data = fixture()
        def diagnostic = new SagaReadExposureCollector('attempt', 'workload', 'scenario', data.sources)
        diagnostic.begin(new ImpactEvidence.SnapshotBatch([], []), [CONTRACT])
        def composed = new ImpactV2EvidenceCollector('attempt', 'workload', 'scenario', diagnostic)
        def broken = Stub(ImpactEvidenceObserver) {
            readResponse(_) >> { throw new IllegalStateException('secret payload') }
        }
        def scope = ImpactEvidenceObserverHolder.install(broken)

        when:
        try { ImpactEvidenceObserverHolder.readResponse(broken, observation()) }
        finally { scope.close() }
        composed.recordObserverFailures(scope)
        composed.recordObserverFailures(scope)

        then:
        composed.report(data.execution).coverageGaps().empty
        diagnostic.report(data.execution, []).gaps()*.reason() == ['READ_OBSERVER_CALLBACK_FAILED']
        diagnostic.report(data.execution, []).collectionCoverage() == 'PARTIAL'
        !JSON.writeValueAsString(diagnostic.report(data.execution, [])).contains('secret payload')
    }

    def 'adapter discovery failure cannot change existing write collection coverage or application execution'() {
        given:
        def data = fixture()
        def diagnostic = new SagaReadExposureCollector('attempt', 'workload', 'scenario', data.sources)
        def composed = new ImpactV2EvidenceCollector('attempt', 'workload', 'scenario', diagnostic)
        def state = Stub(PersistentStateObserver) { snapshotAll() >> new ImpactEvidence.SnapshotBatch([], []) }
        def runtime = Stub(ScenarioRuntimeContext) {
            bean(PersistentStateObserver) >> state
            beans(_) >> { throw new IllegalStateException('adapter discovery failure') }
        }

        when:
        composed.start(runtime)
        composed.finish()

        then:
        composed.started()
        composed.report(data.execution).collectionStatus() == 'OBSERVED'
        composed.report(data.execution).coverageGaps().empty
        diagnostic.report(data.execution, []).collectionCoverage() == 'UNAVAILABLE'
        diagnostic.report(data.execution, []).gaps()*.reason() == ['DIAGNOSTIC_COLLECTION_FAILED']
    }

    def 'an already proven prefix survives a later write collection gap but complete absence is unavailable'() {
        given:
        def data = fixture()
        data.events << [gap: new ImpactEvidence.CoverageGap('WRITE', null, 'WRITE_RELOAD_FAILED', '')]

        expect:
        assess(data).observedExposureCount() == 1
        assess(data).collectionCoverage() == 'PARTIAL'
    }

    private static def assess(Map data) {
        def diagnostic = new SagaReadExposureCollector('attempt', 'workload', 'scenario', data.sources)
        diagnostic.begin(new ImpactEvidence.SnapshotBatch(data.baseline, data.baselineGaps), [CONTRACT])
        data.events.each { collect(diagnostic, it) }
        diagnostic.report(data.execution, [new ArtifactReference('PACKAGE_MANIFEST', null, null, 'UNAVAILABLE', 'CONTROLLED_HARNESS')])
    }

    private static void collect(SagaReadExposureCollector diagnostic, Map event) {
        if (event.containsKey('write')) diagnostic.committedWrite(event.write, event.writer)
        if (event.containsKey('read')) diagnostic.readResponse(event.read)
        if (event.containsKey('gap')) diagnostic.coverageGap(event.gap)
    }

    private static Map caseFixture(String name) {
        def data = fixture()
        switch (name) {
            case 'successful creator':
                data.events.removeLast()
                data.execution = change(data.execution, [terminalStatus: 'SUCCESS',
                        actualActions: data.execution.actualActions().findAll { it.kind() != 'COMPENSATION' }.collect {
                            it.actionId() == 'action-fault' ? change(it, [status: 'COMPLETED', bodyOutcome: 'SUCCEEDED', commitOutcome: 'SUCCEEDED']) : it },
                        participants: data.execution.participants().collect { change(it, [finalState: 'COMMITTED']) }]); break
            case 'release lock only': data.events.removeLast(); break
            case 'ordinary later change': data.events[-1].write = snapshot(11L, 'ACTIVE', 10L); break
            case 'delete another object': data.events[-1].write = change(data.events[-1].write, [identity: new ImpactEvidence.AggregateIdentity(TYPE, 8)]); break
            case 'failed call': data.events[1].read = change(observation(), [outcome: ReadResponseEvidence.Outcome.FAILED]); break
            case 'already deleted':
                data.events = [data.events[0], data.events[2], data.events[1]]
                data.execution = change(data.execution, [actualActions: data.execution.actualActions().collect {
                    int position = ['action-create': 0, 'action-fault': 1, 'action-recover': 2, 'action-read': 3][it.actionId()]
                    change(it, [actualPosition: position, plannedPosition: position]) }]); break
            case 'creator completed': data.execution = change(data.execution, [actualActions: data.execution.actualActions().collect { it.actionId() == 'action-create' ? change(it, [commitOutcome: 'SUCCEEDED']) : it }]); break
            case 'different returned':
                def original = change(snapshot(8L, 'ACTIVE', null), [identity: new ImpactEvidence.AggregateIdentity(TYPE, 9)])
                data.baseline = [original]
                data.events[1].read = change(observation(), [identity: original.identity(), version: 8L]); break
            case 'uncovered baseline': data.baselineGaps = [new ImpactEvidence.CoverageGap('SNAPSHOT', null, 'MISSING_AGGREGATE_IDENTITY', '')]; break
            case 'local rollback': data.events.removeFirst(); break
            case 'failed reload': data.events.add(0, [gap: new ImpactEvidence.CoverageGap('WRITE', null, 'WRITE_RELOAD_FAILED', 'secret payload')]); break
            case 'wrong predecessor': data.events[-1].write = snapshot(11L, 'DELETED', 9L); break
            case 'missing metadata': data.events[0].write = change(data.events[0].write, [frameworkMetadata: null]); break
            case 'competing revision': data.events.add(1, data.events[0]); break
            case 'competing writer': data.events.add(2, [write: snapshot(12L, 'ACTIVE', 10L), writer: writer('B', 'action-read', 'FORWARD', 'inspect')]); break
            case 'wrong checkpoint': data.execution = changeRecovery(data.execution, [sourceCompensationCheckpointId: 'other']); break
            case 'wrong occurrence': data.execution = changeRecovery(data.execution, [runtimeOccurrenceId: 'other']); break
            case 'restored update': data.events[0].write = snapshot(10L, 'ACTIVE', 9L); data.baseline = [snapshot(9L, 'ACTIVE', null)]; break
            case 'type collision': data.events[0].write = change(data.events[0].write, [runtimeType: 'com.example.dummyapp.order.aggregate.Order']); break
            case 'unknown author': data.events[0].writer = null; break
            case 'old attempt reader': data.events[1].read = change(observation(), [reader: change(observation().reader(), [executionAttemptId: 'prior'])]); break
            case 'missing reader action': data.events[1].read = change(observation(), [reader: change(observation().reader(), [actionId: 'missing'])]); break
            case 'failed explicit': data.execution = changeRecovery(data.execution, [recoverySubOutcomes: [recovery('EXPLICIT_COMPENSATION', 'FAILED')]]); break
            case 'ambiguous explicit': data.execution = changeRecovery(data.execution, [recoverySubOutcomes: [recovery('EXPLICIT_COMPENSATION', 'SUCCEEDED'), recovery('EXPLICIT_COMPENSATION', 'FAILED')]]); break
            case 'implicit only': data.execution = changeRecovery(data.execution, [recoverySubOutcomes: [recovery('IMPLICIT_SAGA_ROLLBACK', 'SUCCEEDED')]]); break
            case 'invalid failed call': data.events[1].read = change(observation(), [outcome: ReadResponseEvidence.Outcome.FAILED_INVALID, reason: 'MISSING_READER_ATTRIBUTION']); break
            default: throw new IllegalArgumentException(name)
        }
        data
    }

    private static def changeRecovery(def execution, Map updates) {
        change(execution, [actualActions: execution.actualActions().collect { it.kind() == 'COMPENSATION' ? change(it, updates) : it }])
    }

    private static Map fixture() {
        def source = new SourceContract([new Occurrence('source-create', 'A', 'write#0', 'write', 0),
                new Occurrence('source-read', 'B', 'inspect#0', 'inspect', 1),
                new Occurrence('source-fault', 'A', 'after#0', 'after', 2)],
                [new Checkpoint('checkpoint-create', 'A', 'source-create', 'write#0', 'write', 'compensate-create', 'EXPLICIT_COMPENSATION')])
        def create = record(ScenarioExecutionReport.ActionOutcome, [actionId: 'action-create', kind: 'FORWARD', sagaInstanceId: 'A',
                sourceScheduledStepId: 'source-create', sourceStepId: 'write#0', runtimeStepName: 'write', runtimeOccurrenceId: 'source-create',
                plannedPosition: 0, actualPosition: 0, status: 'COMPLETED', bodyOutcome: 'SUCCEEDED', commitOutcome: 'NOT_RUN'])
        def read = record(ScenarioExecutionReport.ActionOutcome, [actionId: 'action-read', kind: 'FORWARD', sagaInstanceId: 'B',
                sourceScheduledStepId: 'source-read', sourceStepId: 'inspect#0', runtimeStepName: 'inspect', runtimeOccurrenceId: 'source-read',
                plannedPosition: 1, actualPosition: 1, status: 'COMPLETED', bodyOutcome: 'SUCCEEDED', commitOutcome: 'SUCCEEDED'])
        def fault = record(ScenarioExecutionReport.ActionOutcome, [actionId: 'action-fault', kind: 'FORWARD', sagaInstanceId: 'A',
                sourceScheduledStepId: 'source-fault', sourceStepId: 'after#0', runtimeStepName: 'after', runtimeOccurrenceId: 'source-fault',
                plannedPosition: 2, actualPosition: 2, status: 'ASSIGNED_FAULT', bodyOutcome: 'NOT_RUN', commitOutcome: 'NOT_RUN'])
        def recover = record(ScenarioExecutionReport.ActionOutcome, [actionId: 'action-recover', kind: 'COMPENSATION', sagaInstanceId: 'A',
                sourceScheduledStepId: 'source-create', sourceStepId: 'write#0', runtimeStepName: 'write', runtimeOccurrenceId: 'compensate-create',
                sourceCompensationCheckpointId: 'checkpoint-create', compensationEvidenceClass: 'EXPLICIT_COMPENSATION',
                plannedPosition: 3, actualPosition: 3, status: 'COMPENSATED', bodyOutcome: 'NOT_APPLICABLE', commitOutcome: 'NOT_APPLICABLE',
                recoverySubOutcomes: [recovery('EXPLICIT_COMPENSATION', 'SUCCEEDED')], exceptionMessage: 'secret payload'])
        def execution = record(ScenarioExecutionReport, [executionAttemptId: 'attempt', workloadPlanId: 'workload', faultScenarioId: 'scenario',
                terminalStatus: 'PARTIAL_COMPENSATED', scheduleConformance: 'EXACT', actualActions: [create, read, fault, recover],
                participants: [participant('A'), participant('B')]])
        [sources: source, execution: execution, baseline: [], baselineGaps: [], events: [
                [write: snapshot(10L, 'ACTIVE', null), writer: writer('A', 'action-create', 'FORWARD', 'write')],
                [read: observation()], [write: snapshot(11L, 'DELETED', 10L), writer: writer('A', 'action-recover', 'RECOVERY', 'write')]]]
    }

    private static def snapshot(Long version, String state, Long predecessor) {
        new ImpactEvidence.AggregateSnapshot(ID, version, state, TYPE,
                new ImpactEvidence.FrameworkMetadata(null, null, predecessor == null ? null : ID, predecessor, null, null),
                [value: 'secret payload'], [])
    }
    private static def writer(String saga, String action, String phase, String step) {
        new ImpactEvidence.Writer('SAGA', 'attempt', 'workload', saga, action, phase, 'com.example.dummyapp.' + saga, step, null)
    }
    private static def observation() {
        new ReadResponseEvidence.Observation(writer('B', 'action-read', 'FORWARD', 'inspect'), 'SagaCommand', CONTRACT.commandType(),
                CONTRACT.responseType(), true, ReadResponseEvidence.Outcome.DELIVERED, CONTRACT, ID, 10L, null)
    }
    private static def participant(String id) {
        record(ScenarioExecutionReport.Participant, [sagaInstanceId: id, sagaFqn: 'com.example.dummyapp.' + id, finalState: id == 'A' ? 'COMPENSATED' : 'COMMITTED'])
    }
    private static def recovery(String kind, String status) { new ScenarioExecutionReport.RecoverySubOutcome(kind, status) }
    private static def change(def value, Map updates) {
        record(value.class, value.class.recordComponents.collectEntries { [(it.name): it.accessor.invoke(value)] } + updates)
    }
    private static def record(Class type, Map values) {
        def components = type.recordComponents
        Object[] args = components.collect { values.containsKey(it.name) ? values[it.name] : it.type == int ? 0 : it.type == boolean ? false : null } as Object[]
        type.getDeclaredConstructor(components*.type as Class[]).newInstance(args)
    }
}
