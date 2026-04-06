package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.functionalities.ContactFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.webapi.requestDtos.CreateContactRequestDto;

@RestController
public class ContactController {
    @Autowired
    private ContactFunctionalities contactFunctionalities;

    @PostMapping("/contacts/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactDto createContact(@RequestBody CreateContactRequestDto createRequest) {
        return contactFunctionalities.createContact(createRequest);
    }

    @GetMapping("/contacts/{contactAggregateId}")
    public ContactDto getContactById(@PathVariable Integer contactAggregateId) {
        return contactFunctionalities.getContactById(contactAggregateId);
    }

    @PutMapping("/contacts")
    public ContactDto updateContact(@RequestBody ContactDto contactDto) {
        return contactFunctionalities.updateContact(contactDto);
    }

    @DeleteMapping("/contacts/{contactAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContact(@PathVariable Integer contactAggregateId) {
        contactFunctionalities.deleteContact(contactAggregateId);
    }

    @GetMapping("/contacts")
    public List<ContactDto> getAllContacts() {
        return contactFunctionalities.getAllContacts();
    }
}
