package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class CourseCrudSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UnitOfWorkService unitOfWorkService

    def "createCourse + getCourseById round-trip"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("ans-create")
            def req = new CreateCourseRequestDto()
            req.name = "Algorithms"
            req.type = CourseType.TECNICO
            req.creationDate = LocalDateTime.now()

        when:
            def created = courseService.createCourse(req, uow)
            unitOfWorkService.commit(uow)

        then:
            def uowR = unitOfWorkService.createUnitOfWork("ans-read")
            def reloaded = courseService.getCourseById(created.aggregateId, uowR)
            reloaded.name == "Algorithms"
    }
}
