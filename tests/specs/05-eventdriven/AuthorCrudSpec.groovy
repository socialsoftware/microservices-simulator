package pt.ulisboa.tecnico.socialsoftware.eventdriven

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.webapi.requestDtos.CreateAuthorRequestDto
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.service.AuthorService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class AuthorCrudSpec extends Specification {

    @Autowired AuthorService authorService
    @Autowired UnitOfWorkService unitOfWorkService

    def "createAuthor + getAuthorById round-trip"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("event-create")
            def req = new CreateAuthorRequestDto()
            req.name = "Edsger Dijkstra"
            req.bio = "structured programming"

        when:
            def created = authorService.createAuthor(req, uow)
            unitOfWorkService.commit(uow)

        then:
            def uowR = unitOfWorkService.createUnitOfWork("event-read")
            def reloaded = authorService.getAuthorById(created.aggregateId, uowR)
            reloaded.name == "Edsger Dijkstra"
            reloaded.bio == "structured programming"
    }
}
