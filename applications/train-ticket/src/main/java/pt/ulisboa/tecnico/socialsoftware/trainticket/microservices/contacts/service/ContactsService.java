package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsUserDto;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.DocumentType;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.ContactsDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.ContactsUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.webapi.requestDtos.CreateContactsRequestDto;


@Service
@Transactional
public class ContactsService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private ContactsRepository contactsRepository;

    @Autowired
    private ContactsFactory contactsFactory;

    public ContactsService() {}

    public ContactsDto createContacts(CreateContactsRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            ContactsDto contactsDto = new ContactsDto();
            contactsDto.setName(createRequest.getName());
            contactsDto.setDocumentType(createRequest.getDocumentType() != null ? createRequest.getDocumentType().name() : null);
            contactsDto.setDocumentNumber(createRequest.getDocumentNumber());
            contactsDto.setPhoneNumber(createRequest.getPhoneNumber());
            if (createRequest.getUser() != null) {
                ContactsUserDto userDto = new ContactsUserDto();
                userDto.setAggregateId(createRequest.getUser().getAggregateId());
                userDto.setVersion(createRequest.getUser().getVersion());
                userDto.setState(createRequest.getUser().getState() != null ? createRequest.getUser().getState().name() : null);
                contactsDto.setUser(userDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Contacts contacts = contactsFactory.createContacts(aggregateId, contactsDto);
            unitOfWorkService.registerChanged(contacts, unitOfWork);
            return contactsFactory.createContactsDto(contacts);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error creating contacts: " + e.getMessage());
        }
    }

    public ContactsDto getContactsById(Integer id, UnitOfWork unitOfWork) {
        try {
            Contacts contacts = (Contacts) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return contactsFactory.createContactsDto(contacts);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving contacts: " + e.getMessage());
        }
    }

    public List<ContactsDto> getAllContactss(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = contactsRepository.findAll().stream()
                .map(Contacts::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Contacts) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(contactsFactory::createContactsDto)
                .collect(Collectors.toList());
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving contacts: " + e.getMessage());
        }
    }

    public ContactsDto updateContacts(ContactsDto contactsDto, UnitOfWork unitOfWork) {
        try {
            Integer id = contactsDto.getAggregateId();
            Contacts oldContacts = (Contacts) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Contacts newContacts = contactsFactory.createContactsFromExisting(oldContacts);
            if (contactsDto.getName() != null) {
                newContacts.setName(contactsDto.getName());
            }
            if (contactsDto.getDocumentType() != null) {
                newContacts.setDocumentType(DocumentType.valueOf(contactsDto.getDocumentType()));
            }
            if (contactsDto.getDocumentNumber() != null) {
                newContacts.setDocumentNumber(contactsDto.getDocumentNumber());
            }
            if (contactsDto.getPhoneNumber() != null) {
                newContacts.setPhoneNumber(contactsDto.getPhoneNumber());
            }

            unitOfWorkService.registerChanged(newContacts, unitOfWork);            ContactsUpdatedEvent event = new ContactsUpdatedEvent(newContacts.getAggregateId(), newContacts.getName(), newContacts.getDocumentNumber(), newContacts.getPhoneNumber());
            event.setPublisherAggregateVersion(newContacts.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return contactsFactory.createContactsDto(newContacts);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating contacts: " + e.getMessage());
        }
    }

    public void deleteContacts(Integer id, UnitOfWork unitOfWork) {
        try {
            Contacts oldContacts = (Contacts) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Contacts newContacts = contactsFactory.createContactsFromExisting(oldContacts);
            newContacts.remove();
            unitOfWorkService.registerChanged(newContacts, unitOfWork);            unitOfWorkService.registerEvent(new ContactsDeletedEvent(newContacts.getAggregateId()), unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error deleting contacts: " + e.getMessage());
        }
    }








}