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
class AnonymizeUserTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    UserDto userDto

    def setup() {
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
    }

    def "anonymizeUser: success — name and username set to ANONYMOUS"() {
        when:
        def uow = unitOfWorkService.createUnitOfWork("anonymizeUser")
        userService.anonymizeUser(userDto.aggregateId, uow)
        unitOfWorkService.commit(uow)

        then:
        def readUow = unitOfWorkService.createUnitOfWork("check")
        def result = userService.getUserById(userDto.aggregateId, readUow)
        result.name == "ANONYMOUS"
        result.username == "ANONYMOUS"
    }
}
