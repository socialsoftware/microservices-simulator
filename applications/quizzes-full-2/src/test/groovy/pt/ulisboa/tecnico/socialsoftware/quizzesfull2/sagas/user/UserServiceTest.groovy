package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.user

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.Role

@DataJpaTest
@Transactional
@Import(UserServiceTest.LocalBeanConfiguration)
class UserServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999

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

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
