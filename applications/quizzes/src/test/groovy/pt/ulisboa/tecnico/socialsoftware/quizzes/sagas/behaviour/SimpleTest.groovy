package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.user.CreateUserFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.user.*

@DataJpaTest
class SimpleTest extends QuizzesSpockTest {
    public static final String UPDATED_NAME = "UpdatedName"

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private UserService userService
    
    private UserDto userDto

    def unitOfWork2

    def setup() {
        given: 'user'
        userDto = new UserDto()
        userDto.setName("USER_NAME_2")
        userDto.setUsername("USER_USERNAME_2")
        userDto.setRole("STUDENT")

        and: 'a unit of work for adding participant'
        def functionalityName2 = CreateUserFunctionalitySagas.class.getSimpleName()
        unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

    }
    def 'add participant' () {

        when: 'execute createuser functionality'
        def CreateUserFunctionality = new CreateUserFunctionalitySagas(userService, unitOfWorkService, userDto, unitOfWork2);
        CreateUserFunctionality.executeWorkflow(unitOfWork2);
        
        then: 'no exceptions occur (just to verify it runs)'
        notThrown(Exception);
    }

   @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}