package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
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
class UserEventPublicationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    EventService eventService

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
