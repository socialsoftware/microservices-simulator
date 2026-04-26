package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.SagaUser

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UserTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create user"() {
        given:
        def userDto = new UserDto()
        userDto.setName("John Doe")
        userDto.setUsername("johndoe")
        userDto.setRole("STUDENT")

        when:
        def user = new SagaUser(1, userDto)

        then:
        user.name == "John Doe"
        user.username == "johndoe"
        user.role.toString() == "STUDENT"
        user.isActive() == false
    }
}
