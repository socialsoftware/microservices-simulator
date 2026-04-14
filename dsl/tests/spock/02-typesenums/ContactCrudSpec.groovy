package pt.ulisboa.tecnico.socialsoftware.typesenums

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.webapi.requestDtos.CreateContactRequestDto
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.service.ContactService
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.enums.ContactCategory
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ContactCrudSpec extends Specification {

    @Autowired ContactService contactService
    @Autowired UnitOfWorkService unitOfWorkService

    def "createContact persists every typed field"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("contact-create")
            def req = new CreateContactRequestDto()
            req.firstName = "Ada"
            req.lastName = "Lovelace"
            req.email = "ada@example.com"
            req.category = ContactCategory.WORK
            req.createdAt = LocalDateTime.now()
            req.favorite = true
            req.callCount = 3

        when:
            def created = contactService.createContact(req, uow)
            unitOfWorkService.commit(uow)

        then:
            def uowR = unitOfWorkService.createUnitOfWork("contact-read")
            def reloaded = contactService.getContactById(created.aggregateId, uowR)
            reloaded.firstName == "Ada"
            reloaded.email == "ada@example.com"
            reloaded.callCount == 3
    }
}
