package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContactsService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private ContactsFactory contactsFactory;

    private final ContactsRepository contactsRepository;
    private final ContactsCustomRepository contactsCustomRepository;
    private final UnitOfWorkService unitOfWorkService;

    public ContactsService(UnitOfWorkService unitOfWorkService,
                           ContactsRepository contactsRepository,
                           ContactsCustomRepository contactsCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.contactsRepository = contactsRepository;
        this.contactsCustomRepository = contactsCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ContactsDto getContactsById(Integer contactsAggregateId, UnitOfWork unitOfWork) {
        return contactsFactory.createContactsDto(
                (Contacts) unitOfWorkService.aggregateLoadAndRegisterRead(contactsAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<ContactsDto> getContactsByAccount(Integer userAggregateId, UnitOfWork unitOfWork) {
        List<ContactsDto> contactsDtos = new ArrayList<>();
        for (Contacts contacts : contactsCustomRepository.findAllLatestActiveByAccount(userAggregateId)) {
            contactsDtos.add(contactsFactory.createContactsDto(
                    (Contacts) unitOfWorkService.aggregateLoadAndRegisterRead(
                            contacts.getAggregateId(), unitOfWork)));
        }
        return contactsDtos;
    }
}
