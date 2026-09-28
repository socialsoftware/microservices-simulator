package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateObservation
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateSession
import spock.lang.Specification
import spock.lang.Unroll

class CopiedUpdateSessionSpec extends Specification {
    GroovyClassLoader loader
    Class inputType
    Class storedType
    Class envelopeType
    List<Map<String, Object>> contracts

    def setup() {
        loader = new GroovyClassLoader(getClass().classLoader)
        def fixture = new File('../applications/dummyapp/src/main/java/com/example/dummyapp/inferredcopy')
        inputType = loader.parseClass(new File(fixture, 'CardInput.java'))
        storedType = loader.parseClass(new File(fixture, 'StoredCard.java'))
        envelopeType = loader.parseClass('''package com.example.dummyapp.inferredcopy
            class Envelope { Set cards; Object selected }
        ''')
        contracts = [[sourceType: inputType.name, targetType: storedType.name,
            sourceKey: 'aggregateId', targetKey: 'reference', fields: [aggregateId: 'reference', caption: 'heading']]]
    }

    def cleanup() { loader.close() }

    def input(int id, String text) { inputType.getConstructor(Integer, String).newInstance(id, text) }
    def envelope(Object... values) { envelopeType.newInstance(cards: new LinkedHashSet(values.toList())) }
    def copy(CopiedUpdateSession session, Object value) {
        def target = storedType.getConstructor(inputType).newInstance(value)
        session.copied(target, [value] as Object[])
    }

    @Unroll
    def 'origin is retained across #mode without equating unrelated instances'() {
        given:
        def session = new CopiedUpdateSession('attempt', contracts)
        def first = input(1, 'old')
        def second = input(2, 'other')
        session.begin(new Object())
        session.end(envelope(first, second), null)
        if (mode == 'untracked') first = input(1, 'old')
        if (mode == 'changed') first.caption = 'changed'
        def outbound = envelope(first, second)
        if (mode == 'alias') outbound.selected = first
        session.begin(outbound)
        def inbound = outbound
        if (mode in ['serialized', 'reordered', 'alias']) {
            def x = input(1, 'old'), y = input(2, 'other')
            inbound = mode == 'reordered' ? envelope(y, x) : envelope(x, y)
            if (mode == 'alias') inbound.selected = input(1, 'old')
        }
        session.inbound(inbound)
        inbound.cards.each { copy(session, it) }
        session.end(null, null)

        when:
        def trace = session.finish()
        def oldCopies = trace.events.findAll { it.kind == 'CONSTRUCTOR_COPY' && it.data.key == 1 && it.data.values.heading == 'old' }

        then:
        oldCopies.size() == expected
        trace.gaps.empty == complete
        !trace.events.any { it.kind == 'REGISTERED_COPY' } // A constructor alone is not persistence.

        where:
        mode         | expected | complete
        'same'       | 1        | true
        'serialized' | 1        | true
        'reordered'  | 1        | true
        'alias'      | 1        | true
        'untracked'  | 0        | false
        'changed'    | 0        | false
    }

    def 'failed input observation still balances a nested call and preserves the outer origin'() {
        given:
        def session = new CopiedUpdateSession('attempt', contracts)
        session.begin(new Object())
        session.begin(envelope(input(1, 'a'), input(1, 'b')))
        session.end(null, new IllegalArgumentException('application rejection'))
        session.end(envelope(input(2, 'ok')), null)
        when:
        def trace = session.finish()
        then:
        trace.gaps.any { it.contains('Duplicate collection identity') }
        trace.gapContexts.any { it.hook == 'BEGIN' &&
            it.reason == 'IllegalStateException: Duplicate collection identity' &&
            it.location.startsWith('$.cards[') &&
            it.sourceType == envelopeType.name && it.commandDepth == 1 }
        !trace.gaps.contains('UNCLOSED_COMMAND_SCOPE')
        trace.events.count { it.kind == 'RESPONSE' } == 1
    }

    def 'transport mismatch does not admit cloned inputs'() {
        given:
        def session = new CopiedUpdateSession('attempt', contracts)
        def value = input(1, 'old')
        session.begin(new Object()); session.end(envelope(value), null)
        session.begin(envelope(value))
        def wrong = input(1, 'wrong')
        session.inbound(envelope(wrong)); copy(session, wrong); session.end(null, null)
        when:
        def trace = session.finish()
        then:
        !trace.gaps.empty
        !trace.events.any { it.kind == 'CONSTRUCTOR_COPY' }
    }

    def 'attempt closure releases origins and rejects unsupported concurrent hooks'() {
        given:
        def session = new CopiedUpdateSession('attempt', contracts)
        def thread = Thread.start { session.begin(new Object()) }
        thread.join()
        when:
        def trace = session.finish()
        session.begin(new Object())
        then:
        trace.gaps.contains('UNSUPPORTED_CONCURRENT_THREAD')
        trace.gapContexts.any { it.hook == 'BEGIN' && it.crossThread }
        session.finish().events.empty
    }

    def 'unscoped copy without a response retains an origin gap'() {
        given:
        def session = new CopiedUpdateSession('attempt', contracts)
        def source = input(1, 'old')
        when:
        copy(session, source)
        def trace = session.finish()
        then:
        trace.gaps == ['MISSING_INPUT_ORIGIN:' + inputType.name]
        trace.gapContexts.size() == 1
        trace.gapContexts[0].hook == 'CONSTRUCTOR_COPY'
        trace.gapContexts[0].sourceType == inputType.name
        trace.gapContexts[0].targetType == storedType.name
        trace.gapContexts[0].order == 0
    }

    def 'saga construction retains its response origin outside a command'() {
        given:
        def session = new CopiedUpdateSession('attempt', contracts)
        def source = input(1, 'old')
        session.begin(new Object())
        session.end(envelope(source), null)
        when:
        copy(session, source)
        def trace = session.finish()
        then:
        trace.gaps.empty
        trace.events.find { it.kind == 'CONSTRUCTOR_COPY' }.data.readOrder == 1
        trace.events.find { it.kind == 'CONSTRUCTOR_COPY' }.data.sagaConstruction
    }

    def 'returning an intentionally changed in-command copy preserves existing coverage'() {
        given:
        def session = new CopiedUpdateSession('attempt', contracts)
        def source = input(1, 'old')
        session.begin(new Object()); session.end(envelope(source), null)
        def command = envelope(source)
        session.begin(command); session.inbound(command)
        def target = storedType.getConstructor(inputType).newInstance(source)
        session.copied(target, [source] as Object[])
        target.heading = 'intentional replacement'
        session.end(envelope(target), null)
        when:
        def trace = session.finish()
        then:
        trace.gaps.empty
    }

    def 'changed target transport cannot acquire constructor provenance'() {
        given:
        def session = new CopiedUpdateSession('attempt', contracts)
        def source = input(1, 'old')
        session.begin(new Object()); session.end(envelope(source), null)
        def target = storedType.getConstructor(inputType).newInstance(source)
        session.copied(target, [source] as Object[])
        session.begin(envelope(target))
        def changed = storedType.getConstructor(inputType).newInstance(input(1, 'other'))
        session.inbound(envelope(changed))
        session.end(null, null)
        when:
        def trace = session.finish()
        then:
        trace.gaps.any { it.contains('Transport projection mismatch') }
        !trace.events.any { it.kind == 'COPY_TRANSPORT_LINK' }
    }

    @Unroll
    def 'dummyapp observer and assessor prove saga copy through #mode'() {
        given:
        def aggregateType = loader.parseClass('''package com.example.dummyapp.inferredcopy
            class Holder extends pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate {
                Object card
                void verifyInvariants() {}
                Set getEventSubscriptions() { [] as Set }
            }
        ''')
        def session = new CopiedUpdateSession('attempt', contracts)
        def owner = new pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence.Writer(
            'SAGA', 'attempt', 'workload', 'A', 'action', 'FORWARD', 'fixture', 'step', null)
        def foreign = new pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence.Writer(
            'SAGA', 'attempt', 'workload', 'B', 'other', 'FORWARD', 'fixture', 'step', null)
        def identity = new pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence.AggregateIdentity('Holder', 7)
        def snapshot = { version, text ->
            new pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence.AggregateSnapshot(
                identity, version as Long, 'ACTIVE', aggregateType.name,
                [card: [reference: 1, heading: text]], [])
        }
        def scope = pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactWriterContext.enter(owner)
        def source = input(1, 'old')
        session.begin(new Object()); session.end(envelope(source), null)
        def stored = storedType.getConstructor(inputType).newInstance(source)
        session.copied(stored, [source] as Object[])
        def outbound = envelope(stored)
        session.begin(outbound)
        def received = stored
        if (mode == 'serialized') {
            // Decode the actual wire projection into a distinct object, as local transport does.
            def mapper = new com.fasterxml.jackson.databind.ObjectMapper()
            def wire = mapper.writeValueAsString([reference: stored.reference, heading: stored.heading])
            def data = mapper.readValue(wire, Map)
            received = storedType.getConstructor(inputType).newInstance(input(data.reference, data.heading))
        }
        session.inbound(envelope(received))
        session.committed(snapshot(2, 'new'), foreign)
        def aggregate = aggregateType.newInstance(card: received, aggregateId: 7, aggregateType: 'Holder', version: 3L)
        if (mode != 'not-persisted') session.registered(aggregate)
        if (mode != 'rollback') session.committed(snapshot(3, 'old'), owner)
        session.end(null, null)
        scope.close()
        when:
        def trace = session.finish()
        def report = new LostCopiedUpdateAssessor().assess(trace, [snapshot(1, 'old')])
        then:
        trace.gaps.empty
        report.coverageGaps().empty
        report.findings().size() == expected
        where:
        mode            | expected
        'same'          | 1
        'serialized'    | 1
        'rollback'      | 0
        'not-persisted' | 0
    }

    @Unroll
    def 'saga constructed copy crosses #mode transport with explicit identity evidence'() {
        given:
        def session = new CopiedUpdateSession('attempt', contracts)
        def source = input(1, 'old')
        session.begin(new Object()); session.end(envelope(source), null)
        def stored = storedType.getConstructor(inputType).newInstance(source)
        session.copied(stored, [source] as Object[])
        if (mode == 'changed') stored.heading = 'changed'
        def outbound = envelope(stored)
        session.begin(outbound)
        def received = mode == 'serialized' ? envelope(storedType.getConstructor(inputType).newInstance(source)) : outbound
        session.inbound(received)
        session.end(null, null)
        when:
        def trace = session.finish()
        then:
        trace.gaps.empty == (mode != 'changed')
        trace.events.count { it.kind == 'COPY_TRANSPORT_LINK' } == 1
        trace.events.find { it.kind == 'COPY_TRANSPORT_LINK' }.data.cloned == (mode == 'serialized')
        !trace.events.any { it.kind == 'REGISTERED_COPY' }
        where:
        mode << ['same', 'serialized', 'changed']
    }

    def 'observer installation is exclusive and missing agent cannot produce complete zero'() {
        given:
        def session = CopiedUpdateObservation.start('attempt', 'missing')
        when:
        CopiedUpdateObservation.start('second', 'missing')
        then:
        thrown(IllegalStateException)
        cleanup:
        def trace = CopiedUpdateObservation.finish(session)
        assert trace.gaps.contains('CONSTRUCTOR_INSTRUMENTATION_UNAVAILABLE')
        assert trace.gaps.contains('COPY_CONTRACT_MANIFEST_MISMATCH')
    }
}
