package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.executor

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzes.executor.QuizzesPersistedSagaStateObserver
import pt.ulisboa.tecnico.socialsoftware.quizzes.executor.QuizzesRemoveAddBenchmarkAttempt
import pt.ulisboa.tecnico.socialsoftware.quizzes.executor.QuizzesRemoveAddBenchmarkRunner
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState
import spock.lang.Specification
import spock.lang.Unroll

class QuizzesRemoveAddBenchmarkRunnerSpec extends Specification {
    @Unroll
    def 'classification keeps execution validity separate from the one final-state rule'() {
        expect:
        QuizzesRemoveAddBenchmarkRunner.classify(impactEvaluable, observationCompleted, brokenReference) == expected

        where:
        impactEvaluable | observationCompleted | brokenReference || expected
        true            | true                 | true            || 'HARMFUL_FOR_RULE'
        true            | true                 | false           || 'NO_BROKEN_REFERENCE'
        false           | true                 | false           || 'NOT_EVALUATED'
        true            | false                | false           || 'NOT_EVALUATED'
        false           | false                | true            || 'NOT_EVALUATED'
    }

    @Unroll
    def 'broken-reference predicate requires an active Tournament pointing to the observed deleted Quiz'() {
        expect:
        QuizzesRemoveAddBenchmarkRunner.isBrokenReference(
                tournamentState, tournamentQuizId, observedQuizId, quizState) == expected

        where:
        tournamentState                  | tournamentQuizId | observedQuizId | quizState                        || expected
        Aggregate.AggregateState.ACTIVE  | 7                | 7              | Aggregate.AggregateState.DELETED || true
        Aggregate.AggregateState.ACTIVE  | 7                | 8              | Aggregate.AggregateState.DELETED || false
        Aggregate.AggregateState.ACTIVE  | 7                | 7              | Aggregate.AggregateState.ACTIVE  || false
        Aggregate.AggregateState.DELETED | 7                | 7              | Aggregate.AggregateState.DELETED || false
    }

    @Unroll
    def 'outside-active-Saga proof requires the exact converter-decoded generic state'() {
        given:
        def evidence = QuizzesPersistedSagaStateObserver.decodePersistedValue(9, 18L, rawValue)

        expect:
        QuizzesRemoveAddBenchmarkRunner.isOutsideActiveSaga(evidence) == expected

        where:
        rawValue                                                                                   || expected
        typed(GenericSagaState, 'NOT_IN_SAGA')                                                     || true
        typed(GenericSagaState, 'IN_SAGA')                                                         || false
        typed(CourseExecutionSagaState, 'READ_COURSE')                                             || false
        'java.time.DayOfWeek:MONDAY'                                                               || false
        typed(GenericSagaState, 'MISSING')                                                         || false
        'NOT_IN_SAGA'                                                                              || false
        null                                                                                       || false
    }

    def 'raw persisted Saga-state observer accepts and decodes one complete latest-version row'() {
        given:
        def observed = decodedState(9, 17L, typed(GenericSagaState, 'NOT_IN_SAGA'))

        expect:
        QuizzesPersistedSagaStateObserver.requireExactTournamentState(9, [observed]).is(observed)
        observed.rawValue() == typed(GenericSagaState, 'NOT_IN_SAGA')
        observed.decodeStatus() == 'DECODED'
        observed.decodedStateClass() == GenericSagaState.name
        observed.decodedStateName() == 'NOT_IN_SAGA'
        observed.decoder() == 'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter'
    }

    def 'raw persisted Saga-state observer preserves SQL null for fail-closed evaluation'() {
        when:
        def observed = QuizzesPersistedSagaStateObserver.requireExactTournamentState(
                9, [decodedState(9, 18L, null)])

        then:
        observed.rawValue() == null
        observed.valueStatus() == 'SQL_NULL'
        observed.decodeStatus() == 'NOT_DECODED'
        !QuizzesRemoveAddBenchmarkRunner.isOutsideActiveSaga(observed)
    }

    @Unroll
    def 'malformed or wrong enum raw values retain diagnostics and fail closed'(String rawValue) {
        when:
        def observed = QuizzesPersistedSagaStateObserver.requireExactTournamentState(
                9, [decodedState(9, 18L, rawValue)])

        then:
        observed.rawValue() == rawValue
        observed.valueStatus() == 'VALUE'
        observed.decodeStatus() == 'DECODE_FAILED'
        observed.decodeFailure()
        !QuizzesRemoveAddBenchmarkRunner.isOutsideActiveSaga(observed)

        where:
        rawValue << ['NOT_IN_SAGA', 'java.time.DayOfWeek:MONDAY', typed(GenericSagaState, 'MISSING')]
    }

    @Unroll
    def 'raw persisted Saga-state observer rejects missing or structurally mismatched evidence'() {
        when:
        QuizzesPersistedSagaStateObserver.requireExactTournamentState(9, rows)

        then:
        thrown(IllegalStateException)

        where:
        rows << [
                [],
                [decodedState(9, 17L, typed(GenericSagaState, 'NOT_IN_SAGA')),
                 decodedState(9, 17L, typed(GenericSagaState, 'NOT_IN_SAGA'))],
                [decodedState(10, 17L, typed(GenericSagaState, 'NOT_IN_SAGA'))],
                [decodedState(9, null, typed(GenericSagaState, 'NOT_IN_SAGA'))],
                [new QuizzesPersistedSagaStateObserver.PersistedSagaState(
                        9, 17L, typed(GenericSagaState, 'NOT_IN_SAGA'), 'VALUE', 'DECODED',
                        GenericSagaState.name, 'NOT_IN_SAGA', null,
                        QuizzesPersistedSagaStateObserver.DECODER, 'ENTITY_CONVERTER',
                        QuizzesPersistedSagaStateObserver.TABLE,
                        QuizzesPersistedSagaStateObserver.COLUMN,
                        QuizzesPersistedSagaStateObserver.SELECTION)],
                [new QuizzesPersistedSagaStateObserver.PersistedSagaState(
                        9, 17L, typed(GenericSagaState, 'NOT_IN_SAGA'), 'VALUE', 'DECODED',
                        GenericSagaState.name, 'NOT_IN_SAGA', null,
                        'wrong.Decoder', QuizzesPersistedSagaStateObserver.SOURCE,
                        QuizzesPersistedSagaStateObserver.TABLE,
                        QuizzesPersistedSagaStateObserver.COLUMN,
                        QuizzesPersistedSagaStateObserver.SELECTION)]
        ]
    }

    def 'attempt artifact uses only the approved benchmark labels and rule'() {
        expect:
        QuizzesRemoveAddBenchmarkAttempt.SCHEMA_VERSION ==
                'microservices-simulator.quizzes-remove-add-benchmark-attempt.v3'
        QuizzesRemoveAddBenchmarkAttempt.BENCHMARK_ID == 'quizzes-remove-tournament-add-participant'
        QuizzesRemoveAddBenchmarkAttempt.OBSERVATION_RULE.contains('active Tournament')
        QuizzesRemoveAddBenchmarkAttempt.OBSERVATION_RULE.contains('deleted Quiz')
    }

    private static QuizzesPersistedSagaStateObserver.PersistedSagaState decodedState(
            int aggregateId, Long version, String rawValue) {
        QuizzesPersistedSagaStateObserver.decodePersistedValue(aggregateId, version, rawValue)
    }

    private static String typed(Class<? extends Enum<?>> stateClass, String stateName) {
        stateClass.name + ':' + stateName
    }
}
