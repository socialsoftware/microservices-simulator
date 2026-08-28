package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.executor

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutionReport
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

    def 'no-provider proof accepts only the exact NOT_REQUIRED sentinel'() {
        expect:
        QuizzesRemoveAddBenchmarkRunner.isExactNoProviderPrerequisiteSetup(
                prerequisite('NOT_REQUIRED', null, null, 0L, 0L, true, [], [:], null, null))
    }

    @Unroll
    def 'no-provider proof rejects #caseName'() {
        expect:
        !QuizzesRemoveAddBenchmarkRunner.isExactNoProviderPrerequisiteSetup(setup)

        where:
        caseName                                | setup
        'null evidence'                         | null
        'provider-looking zero-binding success' | prerequisite('SUCCEEDED', 'remove-add-provider', '1', 0L, 0L, true, [], [:], null, null)
        'failed status'                         | prerequisite('FAILED', null, null, 0L, 0L, true, [], [:], null, null)
        'provider identity'                     | prerequisite('NOT_REQUIRED', 'provider', null, 0L, 0L, true, [], [:], null, null)
        'provider version'                      | prerequisite('NOT_REQUIRED', null, '1', 0L, 0L, true, [], [:], null, null)
        'duration'                              | prerequisite('NOT_REQUIRED', null, null, 1L, 0L, true, [], [:], null, null)
        'cleared events'                        | prerequisite('NOT_REQUIRED', null, null, 0L, 1L, true, [], [:], null, null)
        'nonempty baseline'                     | prerequisite('NOT_REQUIRED', null, null, 0L, 0L, false, [], [:], null, null)
        'binding evidence'                      | prerequisite('NOT_REQUIRED', null, null, 0L, 0L, true,
                [new ScenarioExecutionReport.BaselineBinding('key', String.name, String.name, 'RESOLVED')], [:], null, null)
        'provider evidence'                     | prerequisite('NOT_REQUIRED', null, null, 0L, 0L, true, [], [key: 'value'], null, null)
        'failure reason'                        | prerequisite('NOT_REQUIRED', null, null, 0L, 0L, true, [], [:], 'FAILED', null)
        'failure message'                       | prerequisite('NOT_REQUIRED', null, null, 0L, 0L, true, [], [:], null, 'failure')
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

    def 'automatic source setup proves action 12 supplied both Saga Tournament arguments'() {
        given:
        def retained = 'attempt:workload:setup-action-12'
        def actions = (1..12).collect { number ->
            new ScenarioExecutionReport.SetupActionOutcome(
                    "setup-action-${number}", number - 1, "method-${number}", 'SUCCEEDED',
                    number == 12 ? 'TournamentDto' : 'void',
                    number == 12 ? 'TournamentDto' : null,
                    number == 12 ? retained : null,
                    number == 12 ? '10' : null)
        }
        def bindings = ['remove-input', 'add-input'].collect { inputId ->
            new ScenarioExecutionReport.SetupParticipantBindingOutcome(
                    inputId, 1, 'setup-action-12', 'aggregateId', 'RESOLVED',
                    Integer.name, retained, '10')
        }
        def setup = new ScenarioExecutionReport.SourceSetup(
                'SUCCEEDED', 1L, 2L, true, actions, bindings, null, null)

        expect:
        QuizzesRemoveAddBenchmarkRunner.exactSourceSetupTournamentId(
                setup, ['remove-input', 'add-input'] as Set) == 10
    }

    def 'automatic source setup rejects distinct Tournament result reuse evidence'() {
        given:
        def actions = (1..12).collect { number ->
            new ScenarioExecutionReport.SetupActionOutcome(
                    "setup-action-${number}", number - 1, "method-${number}", 'SUCCEEDED',
                    number == 12 ? 'TournamentDto' : 'void',
                    number == 12 ? 'TournamentDto' : null,
                    number == 12 ? 'retained-12' : null,
                    number == 12 ? '10' : null)
        }
        def bindings = [
                new ScenarioExecutionReport.SetupParticipantBindingOutcome(
                        'remove-input', 1, 'setup-action-12', 'aggregateId', 'RESOLVED',
                        Integer.name, 'retained-12', '10'),
                new ScenarioExecutionReport.SetupParticipantBindingOutcome(
                        'add-input', 1, 'setup-action-12', 'aggregateId', 'RESOLVED',
                        Integer.name, 'different-result', '10')
        ]
        def setup = new ScenarioExecutionReport.SourceSetup(
                'SUCCEEDED', 1L, 2L, true, actions, bindings, null, null)

        when:
        QuizzesRemoveAddBenchmarkRunner.exactSourceSetupTournamentId(
                setup, ['remove-input', 'add-input'] as Set)

        then:
        thrown(IllegalArgumentException)
    }

    def 'attempt artifact uses only the approved benchmark labels and rule'() {
        expect:
        QuizzesRemoveAddBenchmarkAttempt.SCHEMA_VERSION ==
                'microservices-simulator.quizzes-remove-add-benchmark-attempt.v3'
        QuizzesRemoveAddBenchmarkAttempt.BENCHMARK_ID == 'quizzes-remove-tournament-add-participant'
        QuizzesRemoveAddBenchmarkAttempt.OBSERVATION_RULE.contains('active Tournament')
        QuizzesRemoveAddBenchmarkAttempt.OBSERVATION_RULE.contains('deleted Quiz')
    }

    private static ScenarioExecutionReport.PrerequisiteSetup prerequisite(
            String status,
            String providerId,
            String providerVersion,
            long durationNanos,
            long pendingEventsCleared,
            boolean emptyPendingEventBaseline,
            List<ScenarioExecutionReport.BaselineBinding> bindings,
            Map<String, String> evidence,
            String failureReason,
            String failureMessage) {
        new ScenarioExecutionReport.PrerequisiteSetup(
                providerId, providerVersion, status, durationNanos, pendingEventsCleared,
                emptyPendingEventBaseline, bindings, evidence, failureReason, failureMessage)
    }

    private static QuizzesPersistedSagaStateObserver.PersistedSagaState decodedState(
            int aggregateId, Long version, String rawValue) {
        QuizzesPersistedSagaStateObserver.decodePersistedValue(aggregateId, version, rawValue)
    }

    private static String typed(Class<? extends Enum<?>> stateClass, String stateName) {
        stateClass.name + ':' + stateName
    }
}
