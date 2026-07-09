package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UserServiceTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    EventService eventService

    def "createUser: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §2 User — CreateUser postconditions
        when:
        def dto = userService.createUser(new UserDto(null, USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE, false),
                unitOfWorkService.createUnitOfWork("createUser"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = userService.getUserById(dto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == USER_NAME_1
        readBack.username == USER_USERNAME_1
        readBack.role == STUDENT_ROLE
        readBack.active == true
    }

    def "getUserById: not found throws SimulatorException"() {
        // Spec: plan.md §2 User — GetUserById not-found path (Path A: aggregateLoadAndRegisterRead)
        when:
        userService.getUserById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getUserById"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "updateUserName: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §2 User — UpdateUserName postconditions
        given:
        def existing = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)

        when:
        userService.updateUserName(existing.aggregateId, USER_NAME_2,
                unitOfWorkService.createUnitOfWork("updateUserName"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = userService.getUserById(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == USER_NAME_2
    }

    def "anonymizeUser: name and username set to ANONYMOUS, persisted and readable"() {
        // Spec: plan.md §2 User — AnonymizeUser postconditions
        given:
        def existing = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)

        when:
        userService.anonymizeUser(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("anonymizeUser"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = userService.getUserById(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == "ANONYMOUS"
        readBack.username == "ANONYMOUS"
    }

    def "deleteUser: removes user, not found via fresh UnitOfWork"() {
        // Spec: plan.md §2 User — DeleteUser postconditions (soft-delete)
        given:
        def existing = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)

        when:
        userService.deleteUser(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("deleteUser"))

        and: 'verify user is no longer retrievable'
        userService.getUserById(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "deleteUser publishes DeleteUserEvent with correct payload"() {
        // Spec: plan.md §2 User — events published by DeleteUser
        given:
        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)

        when:
        userService.deleteUser(user.aggregateId, unitOfWorkService.createUnitOfWork("deleteUser"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof DeleteUserEvent }
        events.size() == 1
        def event = events[0] as DeleteUserEvent
        event.publisherAggregateId == user.aggregateId
    }

    def "updateUserName publishes UpdateStudentNameEvent with correct payload"() {
        // Spec: plan.md §2 User — events published by UpdateUserName
        given:
        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)

        when:
        userService.updateUserName(user.aggregateId, USER_NAME_2,
                unitOfWorkService.createUnitOfWork("updateUserName"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof UpdateStudentNameEvent }
        events.size() == 1
        def event = events[0] as UpdateStudentNameEvent
        event.publisherAggregateId == user.aggregateId
        event.studentAggregateId == user.aggregateId
        event.updatedName == USER_NAME_2
    }

    def "anonymizeUser publishes AnonymizeStudentEvent with correct payload"() {
        // Spec: plan.md §2 User — events published by AnonymizeUser
        given:
        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)

        when:
        userService.anonymizeUser(user.aggregateId, unitOfWorkService.createUnitOfWork("anonymizeUser"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof AnonymizeStudentEvent }
        events.size() == 1
        def event = events[0] as AnonymizeStudentEvent
        event.publisherAggregateId == user.aggregateId
        event.studentAggregateId == user.aggregateId
        event.name == "ANONYMOUS"
        event.username == "ANONYMOUS"
    }

    def "createUser does not publish any event"() {
        // Negative case: CreateUser has no events-published entry in plan.md §2 User
        given:
        def countBefore = eventService.getAllEvents().size()

        when:
        userService.createUser(new UserDto(null, USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE, false),
                unitOfWorkService.createUnitOfWork("createUser"))

        then:
        eventService.getAllEvents().size() == countBefore
    }
}
