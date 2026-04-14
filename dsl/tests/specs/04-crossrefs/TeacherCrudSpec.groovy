package pt.ulisboa.tecnico.socialsoftware.crossrefs

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.webapi.requestDtos.CreateTeacherRequestDto
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service.TeacherService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class TeacherCrudSpec extends Specification {

    @Autowired TeacherService teacherService
    @Autowired UnitOfWorkService unitOfWorkService

    def "createTeacher + getTeacherById round-trip"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("xref-create")
            def req = new CreateTeacherRequestDto()
            req.name = "Alan Turing"
            req.email = "alan@example.com"
            req.department = "CS"

        when:
            def created = teacherService.createTeacher(req, uow)
            unitOfWorkService.commit(uow)

        then:
            def uowR = unitOfWorkService.createUnitOfWork("xref-read")
            def reloaded = teacherService.getTeacherById(created.aggregateId, uowR)
            reloaded.name == "Alan Turing"
            reloaded.department == "CS"
    }
}
