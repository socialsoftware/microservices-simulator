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
        session.finish().events.empty
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
