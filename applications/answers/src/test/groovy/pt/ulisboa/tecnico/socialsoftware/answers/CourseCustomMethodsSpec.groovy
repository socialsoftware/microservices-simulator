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
class CourseCustomMethodsSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UnitOfWorkService unitOfWorkService

    private def seedCourse(String name) {
        def uow = unitOfWorkService.createUnitOfWork("ccm-seed-$name")
        def req = new CreateCourseRequestDto()
        req.name = name
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "findCourseByName returns matching course"() {
        given:
            def uniqueName = "FindMe-${System.nanoTime()}"
            def course = seedCourse(uniqueName)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ccm-find")
            def results = courseService.findCourseByName(uniqueName, uow)
        then:
            results.any { it.aggregateId == course.aggregateId }
    }

    def "findCourseByName returns empty for non-existing name"() {
        when:
            def uow = unitOfWorkService.createUnitOfWork("ccm-find-none")
            def results = courseService.findCourseByName("NonExistent-${System.nanoTime()}", uow)
        then:
            results.isEmpty()
    }

    def "updateCourse changes name"() {
        given:
            def course = seedCourse("Original-${System.nanoTime()}")
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("ccm-upd-get")
            def existing = courseService.getCourseById(course.aggregateId, uowGet)
            existing.name = "Updated-${System.nanoTime()}"
            def uowPut = unitOfWorkService.createUnitOfWork("ccm-upd-put")
            courseService.updateCourse(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("ccm-upd-read")
            def reloaded = courseService.getCourseById(course.aggregateId, uowR)
            reloaded.name == existing.name
    }
}
