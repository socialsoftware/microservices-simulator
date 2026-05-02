package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateUserTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "createUser: success"() {
        when:
        def result = userFunctionalities.createUser(new UserDto(null, USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE, false))

        then:
        result.aggregateId != null
        result.name == USER_NAME_1
        result.username == USER_USERNAME_1
        result.role == STUDENT_ROLE
        result.active == true
    }
}
