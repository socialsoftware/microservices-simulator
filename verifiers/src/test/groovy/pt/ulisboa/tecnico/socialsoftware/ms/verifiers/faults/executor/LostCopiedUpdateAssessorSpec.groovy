package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence
import spock.lang.Specification
import spock.lang.Unroll

class LostCopiedUpdateAssessorSpec extends Specification {
    private static final def ASSESSOR = new LostCopiedUpdateAssessor()
    private static final def CARD = identity('CardAggregate', 1)

    @Unroll
    def 'groups a proved #phase copied overwrite into one finding'() {
        given:
        def trace = positiveTrace(phase)
        trace.events.find { it.kind == 'CONSTRUCTOR_COPY' }.data.values.color = 'red'
        trace.events.find { it.kind == 'REGISTERED_COPY' }.data.values.color = 'red'
        trace.events.find { it.kind == 'RESPONSE' }.data.values.'$read'.values.shade = 'red'
        trace.events.find { it.kind == 'COMMAND_INPUT' }.data.values.'$.input'.values.shade = 'red'
        trace.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 4 }
                .data.aggregate.applicationData.cards[0].color = 'blue'
        trace.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 6 }
                .data.aggregate.applicationData.cards[0].color = 'red'

        when:
        def report = ASSESSOR.assess(trace, [snapshot(CARD, 1, cards('old', 'red'))])

        then:
        report.coverageGaps().empty
        report.findings().size() == 1
        with(report.findings()[0]) {
            aggregate() == CARD
            beforeVersion() == 2
            afterVersion() == 3
            it.phase() == phase
            writer().sagaInstanceId() == 'A'
            fields()*.fieldPath == ['$.cards[reference=5].color', '$.cards[reference=5].heading']
            fields().every { it.readOrder() == 0 && it.inputOrder() == 1 && it.constructorOrder() == 3 }
            fields().every { it.foreignWriteOrder() == 4 && it.overwriteOrder() == 6 }
            fields().every { it.foreignWriter().sagaInstanceId() == 'B' }
        }
        report.findings()[0].findingId() == ASSESSOR.assess(trace,
                [snapshot(CARD, 1, cards('old', 'red'))]).findings()[0].findingId()

        where:
        phase << ['FORWARD', 'RECOVERY']
    }

    def 'counts distinct overwriting commits and not copied fields'() {
        given:
        def trace = positiveTrace('FORWARD')
        trace.events.addAll(secondOverwriteEvents())

        when:
        def report = ASSESSOR.assess(trace, [snapshot(CARD, 1, cards('old', 'red'))])

        then:
        report.findings().size() == 2
        report.findings()*.afterVersion == [3L, 5L]
        report.findings()*.fields*.size() == [1, 1]
        report.findings()*.findingId.toSet().size() == 2
    }

    def 'repeated registration evidence does not duplicate a copied field'() {
        given:
        def trace = positiveTrace('FORWARD')
        trace.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 6 }.order = 8
        trace.events.add(event(7, 'REGISTERED_COPY', new LinkedHashMap<>(
                trace.events.find { it.kind == 'REGISTERED_COPY' }.data)))

        when:
        def report = ASSESSOR.assess(trace, [snapshot(CARD, 1, cards('old', 'red'))])

        then:
        report.findings().size() == 1
        report.findings()[0].fields()*.fieldPath == ['$.cards[reference=5].heading']
    }

    def 'one constructed destination aliased at two application-data paths yields one finding with both fields'() {
        given:
        def trace = positiveTrace('FORWARD')
        def registration = trace.events.find { it.kind == 'REGISTERED_COPY' }
        registration.data.path = '$.primary'
        trace.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 4 }
                .data.aggregate.applicationData = aliasData('new')
        trace.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 6 }.with {
            order = 7
            data.aggregate.applicationData = aliasData('old')
        }
        def aliasRegistration = new LinkedHashMap<>(registration.data)
        aliasRegistration.path = '$.secondary'
        trace.events.add(event(6, 'REGISTERED_COPY', aliasRegistration))

        when:
        def report = ASSESSOR.assess(trace, [snapshot(CARD, 1, aliasData('old'))])

        then:
        report.coverageGaps().empty
        report.findings().size() == 1
        report.findings()[0].fields()*.fieldPath == ['$.primary.heading', '$.secondary.heading']
        report.findings()[0].fields()*.overwrittenValue*.textValue() == ['new', 'new']
    }

    def 'fresh and unrelated field histories are evaluated negatives'() {
        given:
        def fresh = positiveTrace('FORWARD')
        fresh.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 4 }.order = 0
        fresh.events.find { it.kind == 'RESPONSE' }.order = 1
        fresh.events.find { it.kind == 'COMMAND_INPUT' }.with {
            order = 2; data.values.'$.input'.readOrder = 1
        }
        fresh.events.find { it.kind == 'TRANSPORT_LINK' }.with { order = 3; data.call = 2; data.readOrder = 1 }
        fresh.events.find { it.kind == 'CONSTRUCTOR_COPY' }.with {
            order = 4; data.readOrder = 1; data.call = 2; data.transportLinkOrders = [3]
        }
        fresh.events.find { it.kind == 'REGISTERED_COPY' }.with { order = 5; data.copyOrder = 4 }

        def unrelated = positiveTrace('FORWARD')
        unrelated.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 4 }
                .data.aggregate.applicationData.cards[0].heading = 'old'

        expect:
        ASSESSOR.assess(fresh, [snapshot(CARD, 1, cards('old', 'red'))]).findings().empty
        ASSESSOR.assess(unrelated, [snapshot(CARD, 1, cards('old', 'red'))]).findings().empty
    }

    def 'uses the immediate committed predecessor'() {
        given:
        def trace = positiveTrace('FORWARD')
        trace.events.find { it.kind == 'REGISTERED_COPY' }.order = 6
        trace.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 6 }.order = 7
        trace.events.add(event(5, 'COMMITTED_WRITE', [aggregate: aggregate(CARD, 25, cards('own-new', 'red')),
                writer: writer('A', 'FORWARD')]))

        expect:
        ASSESSOR.assess(trace, [snapshot(CARD, 1, cards('old', 'red'))]).findings().empty
    }

    def 'requires the copied value to be present in the committed after snapshot'() {
        given:
        def trace = positiveTrace('FORWARD')
        trace.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 6 }
                .data.aggregate.applicationData.cards[0].heading = 'computed-old'

        expect:
        ASSESSOR.assess(trace, [snapshot(CARD, 1, cards('old', 'red'))]).findings().empty
    }

    def 'missing origin evidence and aggregate identity metadata are explicit gaps'() {
        given:
        def missingOrigin = positiveTrace('FORWARD')
        missingOrigin.events.removeAll { it.kind == 'RESPONSE' }
        def missingIdentity = positiveTrace('FORWARD')
        missingIdentity.events.find { it.kind == 'REGISTERED_COPY' }.data.aggregateIdentity = [aggregateId: 1]

        expect:
        ASSESSOR.assess(missingOrigin, [snapshot(CARD, 1, cards('old', 'red'))])
                .coverageGaps()*.reason().contains('RESPONSE_INPUT_COPY_CHAIN_UNAVAILABLE')
        ASSESSOR.assess(missingIdentity, [snapshot(CARD, 1, cards('old', 'red'))])
                .coverageGaps()*.reason().contains('AGGREGATE_IDENTITY_UNAVAILABLE')
    }

    @Unroll
    def 'contradictory or disconnected #broken evidence prevents attribution'() {
        given:
        def trace = positiveTrace('FORWARD')
        mutation(trace)

        when:
        def report = ASSESSOR.assess(trace, [snapshot(CARD, 1, cards('old', 'red'))])

        then:
        report.findings().empty
        report.coverageGaps()*.reason().contains('DISCONNECTED_RESPONSE_INPUT_COPY_CHAIN')

        where:
        broken              | mutation
        'response value'    | { it.events.find { e -> e.kind == 'RESPONSE' }.data.values.'$read'.values.caption = 'other' }
        'input value'       | { it.events.find { e -> e.kind == 'COMMAND_INPUT' }.data.values.'$.input'.values.caption = 'other' }
        'transport pairing' | { it.events.removeAll { e -> e.kind == 'TRANSPORT_LINK' } }
    }

    def 'multiple command aliases with the same observed origin remain attributable'() {
        given:
        def trace = positiveTrace('FORWARD')
        trace.events.find { it.kind == 'COMMAND_INPUT' }.data.values.'$.selected' = sourceItem('old', 0)
        trace.events.add(event(25, 'TRANSPORT_LINK', [call: 1, readOrder: 0,
                path: '$.selected', cloned: false]))

        expect:
        ASSESSOR.assess(trace, [snapshot(CARD, 1, cards('old', 'red'))]).findings().size() == 1
    }

    def 'a registered object whose transaction rolls back has no committed finding or gap'() {
        given:
        def trace = positiveTrace('FORWARD')
        trace.events.removeAll { it.kind == 'COMMITTED_WRITE' && it.order == 6 }

        when:
        def report = ASSESSOR.assess(trace, [snapshot(CARD, 1, cards('old', 'red'))])

        then:
        report.findings().empty
        report.coverageGaps().empty
    }

    def 'a newly created keyed cell is outside copied-overwrite classification'() {
        expect:
        ASSESSOR.assess(positiveTrace('FORWARD'), [snapshot(CARD, 1, [cards: []])]).findings().empty
    }

    def 'joins full aggregate identity when integer ids collide across types'() {
        given:
        def trace = positiveTrace('FORWARD')
        trace.events.find { it.kind == 'REGISTERED_COPY' }.order = 6
        trace.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 6 }.order = 7
        trace.events.add(event(5, 'COMMITTED_WRITE', [
                aggregate: aggregate(identity('OtherAggregate', 1), 2, cards('other', 'red')),
                writer: writer('X', 'EVENT')]))

        when:
        def report = ASSESSOR.assess(trace, [
                snapshot(CARD, 1, cards('old', 'red')),
                snapshot(identity('OtherAggregate', 1), 1, cards('other-old', 'red'))])

        then:
        report.findings().size() == 1
        report.findings()[0].aggregate().aggregateType() == 'CardAggregate'
        report.findings()[0].fields()[0].overwrittenValue().textValue() == 'new'
    }

    def 'compares structurally equal numbers across Java number representations'() {
        given:
        def trace = positiveTrace('FORWARD')
        trace.events.find { it.kind == 'CONSTRUCTOR_COPY' }.data.values.heading = 1L
        trace.events.find { it.kind == 'REGISTERED_COPY' }.data.values.heading = 1.0G
        trace.events.find { it.kind == 'RESPONSE' }.data.values.'$read'.values.caption = 1
        trace.events.find { it.kind == 'COMMAND_INPUT' }.data.values.'$.input'.values.caption = 1.000G
        trace.events.find { it.kind == 'COMMITTED_WRITE' && it.order == 6 }
                .data.aggregate.applicationData.cards[0].heading = 1.00G

        expect:
        ASSESSOR.assess(trace, [snapshot(CARD, 1, cards(1, 'red'))]).findings().size() == 1
    }

    private static Map<String, Object> positiveTrace(String phase) {
        def ownWriter = writer('A', phase)
        def readWriter = writer('A', 'FORWARD')
        [contracts: [[sourceType: 'fixture.CardInput', targetType: 'fixture.StoredCard',
                      fields: [aggregateId: 'reference', version: 'copiedVersion',
                               caption: 'heading', shade: 'color']]], gaps: [], events: [
                event(0, 'RESPONSE', [writer: readWriter, values: ['$read': sourceItem('old', null)]]),
                event(1, 'COMMAND_INPUT', [writer: ownWriter, values: ['$.input': sourceItem('old', 0)]]),
                event(2, 'TRANSPORT_LINK', [call: 1, readOrder: 0, path: '$.input', cloned: false]),
                event(3, 'CONSTRUCTOR_COPY', [call: 1, readOrder: 0, readWriter: readWriter,
                        readPath: '$read', writer: ownWriter,
                        transportLinkOrders: [2],
                        sourceType: 'fixture.CardInput', targetType: 'fixture.StoredCard',
                        values: [reference: 5, copiedVersion: 1, heading: 'old']]),
                event(4, 'COMMITTED_WRITE', [aggregate: aggregate(CARD, 2, cards('new', 'red')),
                        writer: writer('B', 'EVENT')]),
                event(5, 'REGISTERED_COPY', [copyOrder: 3, aggregateIdentity: identityMap(CARD), version: 3,
                        path: '$.cards[reference=5]', values: [reference: 5L, copiedVersion: 1L, heading: 'old'],
                        writer: ownWriter]),
                event(6, 'COMMITTED_WRITE', [aggregate: aggregate(CARD, 3, cards('old', 'red')),
                        writer: ownWriter])]]
    }

    private static List<Map<String, Object>> secondOverwriteEvents() {
        def ownWriter = writer('A', 'RECOVERY')
        [event(7, 'RESPONSE', [writer: ownWriter, values: ['$read': sourceItem('old', null)]]),
         event(8, 'COMMAND_INPUT', [writer: ownWriter, values: ['$.input': sourceItem('old', 7)]]),
         event(9, 'TRANSPORT_LINK', [call: 8, readOrder: 7, path: '$.input', cloned: true]),
         event(10, 'CONSTRUCTOR_COPY', [call: 8, readOrder: 7, readWriter: ownWriter, readPath: '$read',
                 writer: ownWriter, transportLinkOrders: [9],
                 sourceType: 'fixture.CardInput', targetType: 'fixture.StoredCard',
                 values: [reference: 5, copiedVersion: 1, heading: 'old']]),
         event(11, 'COMMITTED_WRITE', [aggregate: aggregate(CARD, 4, cards('newer', 'red')),
                 writer: writer('C', 'EVENT')]),
         event(12, 'REGISTERED_COPY', [copyOrder: 10, aggregateIdentity: identityMap(CARD), version: 5,
                 path: '$.cards[reference=5]', values: [reference: 5, copiedVersion: 1, heading: 'old'], writer: ownWriter]),
         event(13, 'COMMITTED_WRITE', [aggregate: aggregate(CARD, 5, cards('old', 'red')),
                 writer: ownWriter])]
    }

    private static Map<String, Object> event(long order, String kind, Map<String, Object> data) {
        [order: order, kind: kind, data: data]
    }

    private static Map<String, Object> writer(String saga, String phase) {
        [kind: 'SAGA', executionAttemptId: 'attempt-1', workloadPlanId: 'workload-1',
         sagaInstanceId: saga, actionId: saga + ':' + phase, phase: phase,
         functionalityName: 'fixture.Functionality', stepName: 'step', eventId: null]
    }

    private static Map<String, Object> sourceItem(Object caption, Long readOrder) {
        [type: 'fixture.CardInput', values: [aggregateId: 5, version: 1, caption: caption],
         readOrder: readOrder]
    }

    private static ImpactEvidence.AggregateIdentity identity(String type, int id) {
        new ImpactEvidence.AggregateIdentity(type, id)
    }

    private static Map<String, Object> identityMap(ImpactEvidence.AggregateIdentity identity) {
        [aggregateType: identity.aggregateType(), aggregateId: identity.aggregateId()]
    }

    private static Map<String, Object> cards(Object heading, Object color) {
        [cards: [[reference: 5, copiedVersion: 1, heading: heading, color: color]]]
    }

    private static Map<String, Object> aliasData(Object heading) {
        [primary: [reference: 5, copiedVersion: 1, heading: heading],
         secondary: [reference: 5, copiedVersion: 1, heading: heading]]
    }

    private static Map<String, Object> aggregate(ImpactEvidence.AggregateIdentity identity, long version,
                                                 Map<String, Object> applicationData) {
        [identity: identityMap(identity), version: version, lifecycleState: 'ACTIVE',
         runtimeType: 'fixture.Aggregate', applicationData: applicationData, dependencies: []]
    }

    private static ImpactEvidence.AggregateSnapshot snapshot(ImpactEvidence.AggregateIdentity identity,
                                                             long version, Map<String, Object> applicationData) {
        new ImpactEvidence.AggregateSnapshot(identity, version, 'ACTIVE', 'fixture.Aggregate',
                applicationData, [])
    }
}
