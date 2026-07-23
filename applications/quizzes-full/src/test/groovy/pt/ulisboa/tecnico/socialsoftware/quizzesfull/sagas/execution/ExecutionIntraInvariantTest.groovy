package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionStudent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.SagaExecution
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.REMOVE_NO_STUDENTS
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.STUDENT_ALREADY_ENROLLED

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class ExecutionIntraInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create execution"() {
        given:
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setName("Software Engineering")
        courseDto.setType("TECNICO")
        def executionCourse = new ExecutionCourse(courseDto)

        when:
        def execution = new SagaExecution(1, "SE-2024", "1st Semester 2024", executionCourse)

        then:
        execution.acronym == "SE-2024"
        execution.academicTerm == "1st Semester 2024"
        execution.executionCourse.courseAggregateId == 100
        execution.executionCourse.courseName == "Software Engineering"
        execution.executionCourse.courseType == "TECNICO"
        execution.students.isEmpty()
    }

    // ─── REMOVE_NO_STUDENTS ───────────────────────────────────────────────────

    def "REMOVE_NO_STUDENTS: deleted execution with no students passes invariant"() {
        given:
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setName("Software Engineering")
        courseDto.setType("TECNICO")
        def execution = new SagaExecution(1, "SE-2024", "1st Semester 2024", new ExecutionCourse(courseDto))
        execution.setState(Aggregate.AggregateState.DELETED)

        when:
        execution.verifyInvariants()

        then:
        noExceptionThrown()
    }

    def "REMOVE_NO_STUDENTS: deleted execution with enrolled student violates invariant"() {
        given:
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setName("Software Engineering")
        courseDto.setType("TECNICO")
        def execution = new SagaExecution(1, "SE-2024", "1st Semester 2024", new ExecutionCourse(courseDto))
        def userDto = new UserDto()
        userDto.setAggregateId(200)
        userDto.setName(USER_NAME_1)
        userDto.setUsername(USER_USERNAME_1)
        userDto.setActive(true)
        execution.addStudent(new ExecutionStudent(userDto))
        execution.setState(Aggregate.AggregateState.DELETED)

        when:
        execution.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == REMOVE_NO_STUDENTS
    }

    // ─── STUDENT_ALREADY_ENROLLED ─────────────────────────────────────────────

    def "STUDENT_ALREADY_ENROLLED: distinct enrolled students pass invariant"() {
        given:
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setName("Software Engineering")
        courseDto.setType("TECNICO")
        def execution = new SagaExecution(1, "SE-2024", "1st Semester 2024", new ExecutionCourse(courseDto))
        def userDto1 = new UserDto()
        userDto1.setAggregateId(200)
        userDto1.setName(USER_NAME_1)
        userDto1.setUsername(USER_USERNAME_1)
        userDto1.setActive(true)
        def userDto2 = new UserDto()
        userDto2.setAggregateId(201)
        userDto2.setName(USER_NAME_2)
        userDto2.setUsername("janedoe")
        userDto2.setActive(true)
        execution.addStudent(new ExecutionStudent(userDto1))
        execution.addStudent(new ExecutionStudent(userDto2))

        when:
        execution.verifyInvariants()

        then:
        noExceptionThrown()
    }

    def "STUDENT_ALREADY_ENROLLED: duplicate userId in students collection violates invariant"() {
        given:
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setName("Software Engineering")
        courseDto.setType("TECNICO")
        def execution = new SagaExecution(1, "SE-2024", "1st Semester 2024", new ExecutionCourse(courseDto))
        def userDto = new UserDto()
        userDto.setAggregateId(200)
        userDto.setName(USER_NAME_1)
        userDto.setUsername(USER_USERNAME_1)
        userDto.setActive(true)
        // add two distinct ExecutionStudent objects with the same userAggregateId
        def student1 = new ExecutionStudent(userDto)
        def student2 = new ExecutionStudent(userDto)
        execution.getStudents().add(student1)
        execution.getStudents().add(student2)

        when:
        execution.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == STUDENT_ALREADY_ENROLLED
    }
}
