package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionStudent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.SagaExecution
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class ExecutionTest extends QuizzesFullSpockTest {

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

    def "verifyInvariants: REMOVE_NO_STUDENTS — cannot remove execution with enrolled students"() {
        given: 'an execution with one enrolled student'
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setName("Software Engineering")
        courseDto.setType("TECNICO")
        def executionCourse = new ExecutionCourse(courseDto)
        def execution = new SagaExecution(1, "SE-2024", "1st Semester 2024", executionCourse)
        def userDto = new UserDto(200, "Alice", "alice", "STUDENT", true)
        execution.addStudent(new ExecutionStudent(userDto))

        when: 'execution is marked as deleted and invariants verified'
        execution.remove()
        execution.verifyInvariants()

        then:
        thrown(QuizzesFullException)
    }

    def "verifyInvariants: STUDENT_ALREADY_ENROLLED — duplicate user in students set"() {
        given: 'an execution with the same user enrolled twice'
        def courseDto = new CourseDto()
        courseDto.setAggregateId(100)
        courseDto.setName("Software Engineering")
        courseDto.setType("TECNICO")
        def executionCourse = new ExecutionCourse(courseDto)
        def execution = new SagaExecution(1, "SE-2024", "1st Semester 2024", executionCourse)
        def userDto = new UserDto(200, "Alice", "alice", "STUDENT", true)
        execution.addStudent(new ExecutionStudent(userDto))
        execution.addStudent(new ExecutionStudent(userDto))

        when:
        execution.verifyInvariants()

        then:
        thrown(QuizzesFullException)
    }
}
