package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.SagaUser

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.USER_DELETED_STATE

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UserIntraInvariantTest extends QuizzesFullSpockTest {

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
        user.isActive() == true
        notThrown(QuizzesFullException)
    }

    def "USER_DELETED_STATE: state DELETED but user is still active violates invariant"() {
        given: "a valid user"
        def userDto = new UserDto()
        userDto.setName("Jane Doe")
        userDto.setUsername("janedoe")
        userDto.setRole("STUDENT")
        def user = new SagaUser(2, userDto)

        and: "aggregate is marked deleted while active flag is not cleared"
        user.setState(Aggregate.AggregateState.DELETED)
        // active remains true — violates USER_DELETED_STATE

        when:
        user.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == USER_DELETED_STATE
    }
}
