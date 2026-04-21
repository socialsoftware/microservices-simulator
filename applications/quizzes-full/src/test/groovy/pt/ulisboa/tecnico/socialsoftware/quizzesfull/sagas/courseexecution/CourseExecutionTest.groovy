package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.courseexecution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.COURSE_EXECUTION_MISSING_ACRONYM
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.COURSE_EXECUTION_MISSING_ACADEMIC_TERM

@DataJpaTest
@Import(BeanConfigurationSagas)
class CourseExecutionTest extends QuizzesFullSpockTest {

    @Autowired
    CourseExecutionService courseExecutionService

    def validDto() {
        def dto = new CourseExecutionDto()
        dto.setCourseAggregateId(1)
        dto.setName(COURSE_NAME)
        dto.setType(COURSE_TYPE)
        dto.setAcronym(COURSE_EXECUTION_ACRONYM)
        dto.setAcademicTerm(COURSE_EXECUTION_ACADEMIC_TERM)
        dto.setEndDate("2025-07-01T00:00:00")
        return dto
    }

    def "create a course execution successfully"() {
        given: "a valid course execution DTO"
        def dto = validDto()

        when: "the course execution is created"
        def uow = unitOfWorkService.createUnitOfWork("createCourseExecution")
        def result = courseExecutionService.createCourseExecution(dto, uow)
        unitOfWorkService.commit(uow)

        then: "the returned DTO has the correct fields"
        result.getAggregateId() != null
        result.getAcronym() == COURSE_EXECUTION_ACRONYM
        result.getAcademicTerm() == COURSE_EXECUTION_ACADEMIC_TERM
        result.getName() == COURSE_NAME
    }

    def "create a course execution with blank acronym throws exception"() {
        given: "a DTO with blank acronym"
        def dto = validDto()
        dto.setAcronym("")

        when: "the course execution is created"
        def uow = unitOfWorkService.createUnitOfWork("createCourseExecution")
        courseExecutionService.createCourseExecution(dto, uow)

        then: "an exception is thrown"
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == COURSE_EXECUTION_MISSING_ACRONYM
    }

    def "create a course execution with null academic term throws exception"() {
        given: "a DTO with null academic term"
        def dto = validDto()
        dto.setAcademicTerm(null)

        when: "the course execution is created"
        def uow = unitOfWorkService.createUnitOfWork("createCourseExecution")
        courseExecutionService.createCourseExecution(dto, uow)

        then: "an exception is thrown"
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == COURSE_EXECUTION_MISSING_ACADEMIC_TERM
    }
}
