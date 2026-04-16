package pt.ulisboa.tecnico.socialsoftware.quizzesfull.course

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.service.CourseService

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
}
