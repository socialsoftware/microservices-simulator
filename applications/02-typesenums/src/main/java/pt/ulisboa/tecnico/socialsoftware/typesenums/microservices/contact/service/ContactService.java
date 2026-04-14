package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;

import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.enums.ContactCategory;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.typesenums.events.ContactDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.typesenums.events.ContactUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.typesenums.events.*;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.exception.TypesenumsException;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.webapi.requestDtos.CreateContactRequestDto;


@Service
@Transactional(noRollbackFor = TypesenumsException.class)
public class ContactService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ContactFactory contactFactory;

    @Autowired
    private ContactServiceExtension extension;

    public ContactService() {}

    public ContactDto createContact(CreateContactRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            ContactDto contactDto = new ContactDto();
            contactDto.setFirstName(createRequest.getFirstName());
            contactDto.setLastName(createRequest.getLastName());
            contactDto.setEmail(createRequest.getEmail());
            contactDto.setCategory(createRequest.getCategory() != null ? createRequest.getCategory().name() : null);
            contactDto.setCreatedAt(createRequest.getCreatedAt());
            contactDto.setFavorite(createRequest.getFavorite());
            contactDto.setCallCount(createRequest.getCallCount());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Contact contact = contactFactory.createContact(aggregateId, contactDto);
            unitOfWorkService.registerChanged(contact, unitOfWork);
            return contactFactory.createContactDto(contact);
        } catch (TypesenumsException e) {
            throw e;
        } catch (Exception e) {
            throw new TypesenumsException("Error creating contact: " + e.getMessage());
        }
    }

    public ContactDto getContactById(Integer id, UnitOfWork unitOfWork) {
        try {
            Contact contact = (Contact) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return contactFactory.createContactDto(contact);
        } catch (TypesenumsException e) {
            throw e;
        } catch (Exception e) {
            throw new TypesenumsException("Error retrieving contact: " + e.getMessage());
        }
    }

    public List<ContactDto> getAllContacts(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = contactRepository.findAll().stream()
                .map(Contact::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Contact) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(contactFactory::createContactDto)
                .collect(Collectors.toList());
        } catch (TypesenumsException e) {
            throw e;
        } catch (Exception e) {
            throw new TypesenumsException("Error retrieving contact: " + e.getMessage());
        }
    }

    public ContactDto updateContact(ContactDto contactDto, UnitOfWork unitOfWork) {
        try {
            Integer id = contactDto.getAggregateId();
            Contact oldContact = (Contact) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Contact newContact = contactFactory.createContactFromExisting(oldContact);
            if (contactDto.getFirstName() != null) {
                newContact.setFirstName(contactDto.getFirstName());
            }
            if (contactDto.getLastName() != null) {
                newContact.setLastName(contactDto.getLastName());
            }
            if (contactDto.getEmail() != null) {
                newContact.setEmail(contactDto.getEmail());
            }
            if (contactDto.getCategory() != null) {
                newContact.setCategory(ContactCategory.valueOf(contactDto.getCategory()));
            }
            if (contactDto.getCreatedAt() != null) {
                newContact.setCreatedAt(contactDto.getCreatedAt());
            }
            newContact.setFavorite(contactDto.getFavorite());
            if (contactDto.getCallCount() != null) {
                newContact.setCallCount(contactDto.getCallCount());
            }

            unitOfWorkService.registerChanged(newContact, unitOfWork);            ContactUpdatedEvent event = new ContactUpdatedEvent(newContact.getAggregateId(), newContact.getFirstName(), newContact.getLastName(), newContact.getEmail(), newContact.getCreatedAt(), newContact.getFavorite(), newContact.getCallCount());
            event.setPublisherAggregateVersion(newContact.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return contactFactory.createContactDto(newContact);
        } catch (TypesenumsException e) {
            throw e;
        } catch (Exception e) {
            throw new TypesenumsException("Error updating contact: " + e.getMessage());
        }
    }

    public void deleteContact(Integer id, UnitOfWork unitOfWork) {
        try {
            Contact oldContact = (Contact) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Contact newContact = contactFactory.createContactFromExisting(oldContact);
            newContact.remove();
            unitOfWorkService.registerChanged(newContact, unitOfWork);            unitOfWorkService.registerEvent(new ContactDeletedEvent(newContact.getAggregateId()), unitOfWork);
        } catch (TypesenumsException e) {
            throw e;
        } catch (Exception e) {
            throw new TypesenumsException("Error deleting contact: " + e.getMessage());
        }
    }








}