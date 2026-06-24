package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.course

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.COURSE_MISSING_NAME
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.COURSE_MISSING_TYPE

@DataJpaTest
@Import(BeanConfigurationSagas)
class CourseTest extends QuizzesFullSpockTest {

    @Autowired
    CourseService courseService

    def "create a course successfully"() {
        given: "a course DTO"
        def courseDto = new CourseDto()
        courseDto.setName(COURSE_NAME)
        courseDto.setType(COURSE_TYPE)

        when: "the course is created"
        def uow = unitOfWorkService.createUnitOfWork("createCourse")
        def result = courseService.createCourse(courseDto, uow)
        unitOfWorkService.commit(uow)

        then: "the returned DTO has the correct fields"
        result.getName() == COURSE_NAME
        result.getType() == COURSE_TYPE
        result.getAggregateId() != null
    }

    def "create a course with blank name throws exception"() {
        given: "a course DTO with blank name"
        def courseDto = new CourseDto()
        courseDto.setName("")
        courseDto.setType(COURSE_TYPE)

        when: "the course is created"
        def uow = unitOfWorkService.createUnitOfWork("createCourse")
        courseService.createCourse(courseDto, uow)

        then: "an exception is thrown"
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == COURSE_MISSING_NAME
    }

    def "create a course with null type throws exception"() {
        given: "a course DTO with null type"
        def courseDto = new CourseDto()
        courseDto.setName(COURSE_NAME)
        courseDto.setType(null)

        when: "the course is created"
        def uow = unitOfWorkService.createUnitOfWork("createCourse")
        courseService.createCourse(courseDto, uow)

        then: "an exception is thrown"
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == COURSE_MISSING_TYPE
    }
}
