package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.USER_MISSING_ROLE

@DataJpaTest
@Import(BeanConfigurationSagas)
class UserTest extends QuizzesFullSpockTest {

    @Autowired
    UserService userService

    def "create a user successfully"() {
        given: "a user DTO"
        def userDto = new UserDto()
        userDto.setKey(1)
        userDto.setName(USER_NAME_1)
        userDto.setUsername(USER_USERNAME_1)
        userDto.setRole(STUDENT_ROLE)
        userDto.setActive(true)

        when: "the user is created"
        def uow = unitOfWorkService.createUnitOfWork("createUser")
        def result = userService.createUser(userDto, uow)
        unitOfWorkService.commit(uow)

        then: "the returned DTO has the correct fields"
        result.getKey() == 1
        result.getName() == USER_NAME_1
        result.getUsername() == USER_USERNAME_1
        result.getRole() == STUDENT_ROLE
        result.getActive() == false
        result.getAggregateId() != null
    }

    def "create a user with null role throws exception"() {
        given: "a user DTO with null role"
        def userDto = new UserDto()
        userDto.setKey(1)
        userDto.setName(USER_NAME_1)
        userDto.setUsername(USER_USERNAME_1)
        userDto.setRole(null)
        userDto.setActive(true)

        when: "the user is created"
        def uow = unitOfWorkService.createUnitOfWork("createUser")
        userService.createUser(userDto, uow)

        then: "an exception is thrown"
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == USER_MISSING_ROLE
    }

    def "create a user with teacher role"() {
        given: "a user DTO with teacher role"
        def userDto = new UserDto()
        userDto.setKey(2)
        userDto.setName(USER_NAME_2)
        userDto.setUsername(USER_USERNAME_2)
        userDto.setRole(TEACHER_ROLE)
        userDto.setActive(true)

        when: "the user is created"
        def uow = unitOfWorkService.createUnitOfWork("createUser")
        def result = userService.createUser(userDto, uow)
        unitOfWorkService.commit(uow)

        then: "the returned DTO has the correct fields"
        result.getKey() == 2
        result.getName() == USER_NAME_2
        result.getUsername() == USER_USERNAME_2
        result.getRole() == TEACHER_ROLE
        result.getActive() == false
        result.getAggregateId() != null
    }
}
