package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.executor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioRuntimeContext
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.BaselineBindingRequirement
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.executor.QuizzesPersistedSagaStateObserver
import pt.ulisboa.tecnico.socialsoftware.quizzes.executor.QuizzesStaleReadPrerequisiteProvider
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

import javax.sql.DataSource

@DataJpaTest
class QuizzesStaleReadPrerequisiteProviderTest extends QuizzesSpockTest {
    @Autowired
    ApplicationContext applicationContext

    @Autowired
    DataSource dataSource

    def 'provider creates the stale-read baseline and returns all typed bindings'() {
        given:
        def provider = new QuizzesStaleReadPrerequisiteProvider()
        def runtime = [bean: { Class<?> type -> applicationContext.getBean(type) }] as ScenarioRuntimeContext
        def requirements = [
                new BaselineBindingRequirement('courseExecutionAggregateId', Integer.name),
                new BaselineBindingRequirement('creatorUserAggregateId', Integer.name),
                new BaselineBindingRequirement('tournamentAggregateId', Integer.name),
                new BaselineBindingRequirement('updatedUser', UserDto.name)
        ]

        when:
        def result = provider.prepare(runtime, requirements)
        def repeated = provider.prepare(runtime, requirements.take(3))
        def tournament = applicationContext.getBean(TournamentFunctionalities)
                .findTournament(result.bindings().tournamentAggregateId as Integer)
        def execution = applicationContext.getBean(ExecutionFunctionalities)
                .getCourseExecutionByAggregateId(result.bindings().courseExecutionAggregateId as Integer)
        def persistedSagaState = new QuizzesPersistedSagaStateObserver(dataSource)
                .observeTournament(result.bindings().tournamentAggregateId as Integer)

        then:
        provider.providerId() == 'quizzes-stale-read-baseline'
        provider.providerVersion() == '1'
        result.bindings().keySet() == requirements*.key() as Set
        result.bindings().courseExecutionAggregateId instanceof Integer
        result.bindings().creatorUserAggregateId instanceof Integer
        result.bindings().tournamentAggregateId instanceof Integer
        result.bindings().updatedUser instanceof UserDto
        result.bindings().updatedUser.name == 'UpdatedName'
        result.evidence().baselineInstance == 'created'
        repeated.bindings() == result.bindings()
        repeated.evidence().baselineInstance == 'reused'
        repeated.evidence().requiredBindingCount == '3'
        result.evidence().tournamentAggregateId == result.bindings().tournamentAggregateId.toString()
        result.evidence().courseExecutionAggregateId == result.bindings().courseExecutionAggregateId.toString()
        result.evidence().participantUserAggregateId == result.bindings().creatorUserAggregateId.toString()
        result.evidence().referencedQuizAggregateId == tournament.quiz.aggregateId.toString()
        tournament.participants.empty
        tournament.quiz != null
        tournament.state == 'ACTIVE'
        persistedSagaState.aggregateId() == tournament.aggregateId
        persistedSagaState.rawValue() == 'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState:NOT_IN_SAGA'
        persistedSagaState.valueStatus() == 'VALUE'
        persistedSagaState.decodeStatus() == 'DECODED'
        persistedSagaState.decodedStateClass() == 'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState'
        persistedSagaState.decodedStateName() == 'NOT_IN_SAGA'
        persistedSagaState.decoder() == 'pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter'
        persistedSagaState.source() == 'RAW_DATABASE_COLUMN'
        persistedSagaState.storageTable() == 'saga_tournament'
        persistedSagaState.storageColumn() == 'saga_state'
        persistedSagaState.rowSelection() == 'LATEST_AGGREGATE_VERSION'
        execution.students*.aggregateId.contains(result.bindings().creatorUserAggregateId)
        result.evidence().requiredBindingCount == '4'
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
