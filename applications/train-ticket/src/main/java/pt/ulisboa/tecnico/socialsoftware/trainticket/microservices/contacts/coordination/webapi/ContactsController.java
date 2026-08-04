package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.functionalities.ContactsFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.webapi.requestDtos.CreateContactsRequestDto;

@RestController
public class ContactsController {
    @Autowired
    private ContactsFunctionalities contactsFunctionalities;

    @PostMapping("/contactss/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactsDto createContacts(@RequestBody CreateContactsRequestDto createRequest) {
        return contactsFunctionalities.createContacts(createRequest);
    }

    @GetMapping("/contactss/{contactsAggregateId}")
    public ContactsDto getContactsById(@PathVariable Integer contactsAggregateId) {
        return contactsFunctionalities.getContactsById(contactsAggregateId);
    }

    @PutMapping("/contactss")
    public ContactsDto updateContacts(@RequestBody ContactsDto contactsDto) {
        return contactsFunctionalities.updateContacts(contactsDto);
    }

    @DeleteMapping("/contactss/{contactsAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContacts(@PathVariable Integer contactsAggregateId) {
        contactsFunctionalities.deleteContacts(contactsAggregateId);
    }

    @GetMapping("/contactss")
    public List<ContactsDto> getAllContactss() {
        return contactsFunctionalities.getAllContactss();
    }
}
