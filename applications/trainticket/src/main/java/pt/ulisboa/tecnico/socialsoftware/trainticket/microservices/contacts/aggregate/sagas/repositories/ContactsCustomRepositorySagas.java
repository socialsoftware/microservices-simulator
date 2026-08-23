package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsRepository;

import java.util.List;

@Service
@Profile("sagas")
public class ContactsCustomRepositorySagas implements ContactsCustomRepository {
    @Autowired
    private ContactsRepository contactsRepository;

    @Override
    public List<Contacts> findAllLatestActiveByAccount(Integer userAggregateId) {
        return contactsRepository.findAllLatestActiveByAccount(userAggregateId);
    }
}
