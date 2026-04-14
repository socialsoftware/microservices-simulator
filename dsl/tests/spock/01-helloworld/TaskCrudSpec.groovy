package pt.ulisboa.tecnico.socialsoftware.helloworld

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.webapi.requestDtos.CreateTaskRequestDto
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.service.TaskService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class TaskCrudSpec extends Specification {

    @Autowired TaskService taskService
    @Autowired UnitOfWorkService unitOfWorkService

    def "createTask + getTaskById round-trip"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("hello-create")
            def req = new CreateTaskRequestDto()
            req.title = "write thesis"
            req.description = "ASAP"
            req.done = false

        when:
            def created = taskService.createTask(req, uow)
            unitOfWorkService.commit(uow)

        then:
            def uowR = unitOfWorkService.createUnitOfWork("hello-read")
            def reloaded = taskService.getTaskById(created.aggregateId, uowR)
            reloaded.title == "write thesis"
            reloaded.done == false
    }
}
