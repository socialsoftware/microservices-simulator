package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.ActivateUserEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain.QuizzesFull2DomainConstants
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.Role
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.User
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(UserServiceTest.LocalBeanConfiguration)
class UserServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999

    @Autowired
    EventService eventService

    def "getUserById: reads back the persisted user through a fresh UnitOfWork"() {
        // Spec: plan.md §2 User — GetUserById(userAggregateId); fields name, username, role, active
        given:
        def userAggregateId = createUser()

        when:
        flushAndClear()
        def result = userService.getUserById(userAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == userAggregateId
        result.name == USER_NAME
        result.username == USER_USERNAME
        result.role == USER_ROLE
        !result.active
        result.version != null
    }

    def "getUserById: unknown aggregate id is not found"() {
        // Spec: plan.md §2 User — GetUserById(userAggregateId); Path A (aggregateLoadAndRegisterRead)
        when:
        userService.getUserById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getStudents: returns only the users with the STUDENT role"() {
        // Spec: plan.md §2 User — GetStudents()
        given:
        def studentAggregateId = createUser(USER_NAME, USER_USERNAME, Role.STUDENT)
        def otherStudentAggregateId = createUser("Bob Jones", "bob", Role.STUDENT)
        createUser("Carol Reed", "carol", Role.TEACHER)
        createUser("Dave Kent", "dave", Role.ADMIN)

        when:
        flushAndClear()
        def result = userService.getStudents(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        result.collect { it.aggregateId } as Set == [studentAggregateId, otherStudentAggregateId] as Set
        def student = result.find { it.aggregateId == studentAggregateId }
        student.name == USER_NAME
        student.username == USER_USERNAME
        student.role == Role.STUDENT
        def otherStudent = result.find { it.aggregateId == otherStudentAggregateId }
        otherStudent.name == "Bob Jones"
        otherStudent.username == "bob"
        otherStudent.role == Role.STUDENT
    }

    def "getStudents: returns an empty list when no student exists"() {
        // Spec: plan.md §2 User — GetStudents()
        given:
        createUser("Carol Reed", "carol", Role.TEACHER)

        when:
        flushAndClear()
        def result = userService.getStudents(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    def "getTeachers: returns only the users with the TEACHER role"() {
        // Spec: plan.md §2 User — GetTeachers()
        given:
        def teacherAggregateId = createUser("Carol Reed", "carol", Role.TEACHER)
        createUser(USER_NAME, USER_USERNAME, Role.STUDENT)
        createUser("Dave Kent", "dave", Role.ADMIN)

        when:
        flushAndClear()
        def result = userService.getTeachers(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 1
        result[0].aggregateId == teacherAggregateId
        result[0].name == "Carol Reed"
        result[0].username == "carol"
        result[0].role == Role.TEACHER
    }

    def "getTeachers: returns an empty list when no teacher exists"() {
        // Spec: plan.md §2 User — GetTeachers()
        given:
        createUser(USER_NAME, USER_USERNAME, Role.STUDENT)

        when:
        flushAndClear()
        def result = userService.getTeachers(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    def "createUser: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §2 User — CreateUser(name, username, role); account is inactive until activated
        given:
        def userDto = new UserDto()
        userDto.setName("Carol Reed")
        userDto.setUsername("carol")
        userDto.setRole(Role.TEACHER)

        when:
        def created = userService.createUser(userDto,
                unitOfWorkService.createUnitOfWork("createUser"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        created.aggregateId != null
        flushAndClear()
        def readBack = userService.getUserById(created.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.aggregateId == created.aggregateId
        readBack.name == "Carol Reed"
        readBack.username == "carol"
        readBack.role == Role.TEACHER
        !readBack.active
        readBack.version != null
    }

    def "createUser: each user gets its own aggregate id and state"() {
        // Spec: plan.md §2 User — CreateUser(name, username, role); accounts are independent
        given:
        def firstDto = new UserDto()
        firstDto.setName(USER_NAME)
        firstDto.setUsername(USER_USERNAME)
        firstDto.setRole(Role.STUDENT)
        def secondDto = new UserDto()
        secondDto.setName("Carol Reed")
        secondDto.setUsername("carol")
        secondDto.setRole(Role.TEACHER)

        when:
        def first = userService.createUser(firstDto,
                unitOfWorkService.createUnitOfWork("createUser"))
        def second = userService.createUser(secondDto,
                unitOfWorkService.createUnitOfWork("createUser"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        first.aggregateId != second.aggregateId
        flushAndClear()
        def readBackFirst = userService.getUserById(first.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBackFirst.username == USER_USERNAME
        readBackFirst.role == Role.STUDENT
        def readBackSecond = userService.getUserById(second.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBackSecond.username == "carol"
        readBackSecond.role == Role.TEACHER
    }

    def "activateUser: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §2 User — ActivateUser(userAggregateId); sets active = true
        given:
        def userAggregateId = createUser()

        when:
        userService.activateUser(userAggregateId,
                unitOfWorkService.createUnitOfWork("activateUser"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = userService.getUserById(userAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.active
        readBack.name == USER_NAME
        readBack.username == USER_USERNAME
        readBack.role == USER_ROLE
    }

    def "activateUser: unknown aggregate id is not found"() {
        // Spec: plan.md §2 User — ActivateUser(userAggregateId); Path A (aggregateLoadAndRegisterRead)
        when:
        userService.activateUser(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("activateUser"))

        then:
        thrown(SimulatorException)
    }

    def "activateUser publishes ActivateUserEvent with correct payload"() {
        // Spec: plan.md §2 User — events published: ActivateUserEvent; payload userAggregateId, active
        given:
        def userAggregateId = createUser()

        when:
        userService.activateUser(userAggregateId,
                unitOfWorkService.createUnitOfWork("activateUser"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof ActivateUserEvent }
        events.size() == 1
        def event = events[0] as ActivateUserEvent
        event.publisherAggregateId == userAggregateId
        event.userAggregateId == userAggregateId
        event.active
    }

    def "updateUserName: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §2 User — UpdateUserName(userAggregateId, name); only name changes
        given:
        def userAggregateId = createUser()

        when:
        userService.updateUserName(userAggregateId, "Alice Smithson",
                unitOfWorkService.createUnitOfWork("updateUserName"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = userService.getUserById(userAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == "Alice Smithson"
        readBack.username == USER_USERNAME
        readBack.role == USER_ROLE
        !readBack.active
    }

    def "updateUserName: unknown aggregate id is not found"() {
        // Spec: plan.md §2 User — UpdateUserName(userAggregateId, name); Path A (aggregateLoadAndRegisterRead)
        when:
        userService.updateUserName(NONEXISTENT_AGGREGATE_ID, "Alice Smithson",
                unitOfWorkService.createUnitOfWork("updateUserName"))

        then:
        thrown(SimulatorException)
    }

    def "updateUserName publishes UpdateStudentNameEvent with correct payload"() {
        // Spec: plan.md §2 User — events published: UpdateStudentNameEvent; payload studentAggregateId, updatedName
        given:
        def userAggregateId = createUser()

        when:
        userService.updateUserName(userAggregateId, "Alice Smithson",
                unitOfWorkService.createUnitOfWork("updateUserName"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof UpdateStudentNameEvent }
        events.size() == 1
        def event = events[0] as UpdateStudentNameEvent
        event.publisherAggregateId == userAggregateId
        event.studentAggregateId == userAggregateId
        event.updatedName == "Alice Smithson"
    }

    def "createUser publishes no event"() {
        // Spec: plan.md §2 User — CreateUser publishes nothing; only the four listed events exist
        given:
        def countBefore = eventService.getAllEvents().size()
        def userDto = new UserDto()
        userDto.setName("Carol Reed")
        userDto.setUsername("carol")
        userDto.setRole(Role.TEACHER)

        when:
        userService.createUser(userDto, unitOfWorkService.createUnitOfWork("createUser"))

        then:
        eventService.getAllEvents().size() == countBefore
    }

    def "anonymizeUser: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §2 User — AnonymizeUser(userAggregateId); sets both name and username to ANONYMOUS
        given:
        def userAggregateId = createUser()

        when:
        userService.anonymizeUser(userAggregateId,
                unitOfWorkService.createUnitOfWork("anonymizeUser"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = userService.getUserById(userAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == QuizzesFull2DomainConstants.ANONYMOUS
        readBack.username == QuizzesFull2DomainConstants.ANONYMOUS
        readBack.role == USER_ROLE
        !readBack.active
    }

    def "anonymizeUser: unknown aggregate id is not found"() {
        // Spec: plan.md §2 User — AnonymizeUser(userAggregateId); Path A (aggregateLoadAndRegisterRead)
        when:
        userService.anonymizeUser(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("anonymizeUser"))

        then:
        thrown(SimulatorException)
    }

    def "anonymizeUser publishes AnonymizeStudentEvent with correct payload"() {
        // Spec: plan.md §2 User — events published: AnonymizeStudentEvent; payload studentAggregateId, name, username
        given:
        def userAggregateId = createUser()

        when:
        userService.anonymizeUser(userAggregateId,
                unitOfWorkService.createUnitOfWork("anonymizeUser"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof AnonymizeStudentEvent }
        events.size() == 1
        def event = events[0] as AnonymizeStudentEvent
        event.publisherAggregateId == userAggregateId
        event.studentAggregateId == userAggregateId
        event.name == QuizzesFull2DomainConstants.ANONYMOUS
        event.username == QuizzesFull2DomainConstants.ANONYMOUS
    }

    def "deleteUser: soft-deletes the user and clears the active flag"() {
        // Spec: plan.md §2 User — DeleteUser(userAggregateId); must also set active = false so
        // the P1 USER_DELETED_STATE predicate holds
        given: 'an active user, so active = false is a change this operation makes'
        def userAggregateId = createUser()
        userService.activateUser(userAggregateId,
                unitOfWorkService.createUnitOfWork("activateUser"))

        when:
        userService.deleteUser(userAggregateId,
                unitOfWorkService.createUnitOfWork("deleteUser"))

        then: 'read back off a cleared persistence context, through the deleted-version load path'
        flushAndClear()
        def readBack = (User) unitOfWorkService.aggregateDeletedLoad(userAggregateId)
        readBack.state == Aggregate.AggregateState.DELETED
        !readBack.isActive()
        readBack.name == USER_NAME
        readBack.username == USER_USERNAME
        readBack.role == USER_ROLE
    }

    def "deleteUser: the deleted user is no longer visible to the non-deleted load path"() {
        // Spec: plan.md §2 User — DeleteUser(userAggregateId) soft-deletes the account
        given:
        def userAggregateId = createUser()
        userService.deleteUser(userAggregateId,
                unitOfWorkService.createUnitOfWork("deleteUser"))

        when:
        flushAndClear()
        userService.getUserById(userAggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "deleteUser: unknown aggregate id is not found"() {
        // Spec: plan.md §2 User — DeleteUser(userAggregateId); Path A (aggregateLoadAndRegisterRead)
        when:
        userService.deleteUser(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("deleteUser"))

        then:
        thrown(SimulatorException)
    }

    def "deleteUser publishes DeleteUserEvent with correct payload"() {
        // Spec: plan.md §2 User — events published: DeleteUserEvent; payload userAggregateId
        given:
        def userAggregateId = createUser()

        when:
        userService.deleteUser(userAggregateId,
                unitOfWorkService.createUnitOfWork("deleteUser"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof DeleteUserEvent }
        events.size() == 1
        def event = events[0] as DeleteUserEvent
        event.publisherAggregateId == userAggregateId
        event.userAggregateId == userAggregateId
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
