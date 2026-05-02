package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.SagaExecution

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
}
