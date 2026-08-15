package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.executor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioRuntimeContext
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.BaselineBindingRequirement
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.executor.QuizzesStaleReadPrerequisiteProvider
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class QuizzesStaleReadPrerequisiteProviderTest extends QuizzesSpockTest {
    @Autowired
    ApplicationContext applicationContext

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

        then:
        provider.providerId() == 'quizzes-stale-read-baseline'
        provider.providerVersion() == '1'
        result.bindings().keySet() == requirements*.key() as Set
        result.bindings().courseExecutionAggregateId instanceof Integer
        result.bindings().creatorUserAggregateId instanceof Integer
        result.bindings().tournamentAggregateId instanceof Integer
        result.bindings().updatedUser instanceof UserDto
        result.bindings().updatedUser.name == 'UpdatedName'
        result.evidence().requiredBindingCount == '4'
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
