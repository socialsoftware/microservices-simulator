package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.contacts

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class ContactsServiceTest extends TrainticketSpockTest {

    def "getContactsById: reads back the persisted contact through a fresh UnitOfWork"() {
        // Spec: plan.md §5 Contacts - GetContactsById
        given:
        def contactsAggregateId = createContacts()

        when:
        def result = contactsService.getContactsById(contactsAggregateId,
                unitOfWorkService.createUnitOfWork("getContactsById"))

        then:
        result.aggregateId == contactsAggregateId
        result.userAggregateId == CONTACTS_USER_AGGREGATE_ID
        result.name == CONTACTS_NAME
        result.documentType == CONTACTS_DOCUMENT_TYPE
        result.documentNumber == CONTACTS_DOCUMENT_NUMBER
        result.phoneNumber == CONTACTS_PHONE_NUMBER
        result.isActive()
    }

    def "getContactsById: unknown aggregate id"() {
        // Spec: plan.md §5 Contacts - GetContactsById; Path A (aggregateLoadAndRegisterRead)
        when:
        contactsService.getContactsById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getContactsById"))

        then:
        thrown(SimulatorException)
    }

    def "getContactsByAccount: returns only the contacts owned by the account"() {
        // Spec: plan.md §5 Contacts - GetContactsByAccount
        given: 'two contacts on one account and one on another'
        def firstAggregateId = createContacts(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME)
        def secondAggregateId = createContacts(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME_TWO)
        createContacts(CONTACTS_USER_AGGREGATE_ID_TWO, CONTACTS_NAME)

        when:
        def result = contactsService.getContactsByAccount(CONTACTS_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getContactsByAccount"))

        then:
        result.collect { it.aggregateId }.toSet() == [firstAggregateId, secondAggregateId].toSet()
        result.every { it.userAggregateId == CONTACTS_USER_AGGREGATE_ID }
        result.collect { it.name }.toSet() == [CONTACTS_NAME, CONTACTS_NAME_TWO].toSet()
        result.find { it.aggregateId == firstAggregateId }.documentNumber == CONTACTS_DOCUMENT_NUMBER
        result.find { it.aggregateId == firstAggregateId }.phoneNumber == CONTACTS_PHONE_NUMBER
    }

    def "getContactsByAccount: account with no contacts yields an empty list"() {
        // Spec: plan.md §5 Contacts - GetContactsByAccount; collection read has no not-found path
        given:
        createContacts(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME)

        when:
        def result = contactsService.getContactsByAccount(CONTACTS_USER_AGGREGATE_ID_TWO,
                unitOfWorkService.createUnitOfWork("getContactsByAccount"))

        then:
        result.isEmpty()
    }

    def "createContacts: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §5 Contacts - CreateContacts
        given:
        def contactsDto = new ContactsDto(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME,
                CONTACTS_DOCUMENT_TYPE, CONTACTS_DOCUMENT_NUMBER, CONTACTS_PHONE_NUMBER)

        when:
        def result = contactsService.createContacts(contactsDto,
                unitOfWorkService.createUnitOfWork("createContacts"))

        then: 'read back through a second, fresh UnitOfWork'
        result.aggregateId != null
        def readBack = contactsService.getContactsById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.userAggregateId == CONTACTS_USER_AGGREGATE_ID
        readBack.name == CONTACTS_NAME
        readBack.documentType == CONTACTS_DOCUMENT_TYPE
        readBack.documentNumber == CONTACTS_DOCUMENT_NUMBER
        readBack.phoneNumber == CONTACTS_PHONE_NUMBER
        readBack.isActive()
    }

    def "createContacts: the account reference is stored without validating the account exists"() {
        // Spec: plan.md §5 Contacts - CreateContacts is a single-aggregate write; no ACCOUNT_EXISTS rule
        given:
        def contactsDto = new ContactsDto(NONEXISTENT_AGGREGATE_ID, CONTACTS_NAME,
                CONTACTS_DOCUMENT_TYPE, CONTACTS_DOCUMENT_NUMBER, CONTACTS_PHONE_NUMBER)

        when:
        def result = contactsService.createContacts(contactsDto,
                unitOfWorkService.createUnitOfWork("createContacts"))

        then:
        def readBack = contactsService.getContactsById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.userAggregateId == NONEXISTENT_AGGREGATE_ID
    }

    def "updateContacts: new name, document and phone number persisted, account reference untouched"() {
        // Spec: plan.md §5 Contacts - UpdateContacts; userAggregateId is immutable (domain model §2)
        given:
        def contactsAggregateId = createContacts()

        when:
        contactsService.updateContacts(contactsAggregateId,
                new ContactsDto(CONTACTS_USER_AGGREGATE_ID_TWO, CONTACTS_NAME_TWO,
                        CONTACTS_DOCUMENT_TYPE_TWO, CONTACTS_DOCUMENT_NUMBER_TWO, CONTACTS_PHONE_NUMBER_TWO),
                unitOfWorkService.createUnitOfWork("updateContacts"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = contactsService.getContactsById(contactsAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == CONTACTS_NAME_TWO
        readBack.documentType == CONTACTS_DOCUMENT_TYPE_TWO
        readBack.documentNumber == CONTACTS_DOCUMENT_NUMBER_TWO
        readBack.phoneNumber == CONTACTS_PHONE_NUMBER_TWO
        readBack.userAggregateId == CONTACTS_USER_AGGREGATE_ID
    }

    def "updateContacts: unknown aggregate id"() {
        // Spec: plan.md §5 Contacts - UpdateContacts; Path A (aggregateLoadAndRegisterRead)
        when:
        contactsService.updateContacts(NONEXISTENT_AGGREGATE_ID,
                new ContactsDto(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME_TWO,
                        CONTACTS_DOCUMENT_TYPE_TWO, CONTACTS_DOCUMENT_NUMBER_TWO, CONTACTS_PHONE_NUMBER_TWO),
                unitOfWorkService.createUnitOfWork("updateContacts"))

        then:
        thrown(SimulatorException)
    }

    def "deleteContacts: soft-deleted contact is no longer loadable"() {
        // Spec: plan.md §5 Contacts - DeleteContacts (soft delete)
        given:
        def contactsAggregateId = createContacts()

        when:
        contactsService.deleteContacts(contactsAggregateId,
                unitOfWorkService.createUnitOfWork("deleteContacts"))

        and: 'read back through a second, fresh UnitOfWork'
        contactsService.getContactsById(contactsAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "deleteContacts: the deleted contact stops being listed for its account"() {
        // Spec: plan.md §5 Contacts - DeleteContacts (soft delete)
        given:
        def deletedAggregateId = createContacts(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME)
        def survivingAggregateId = createContacts(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME_TWO)

        when:
        contactsService.deleteContacts(deletedAggregateId,
                unitOfWorkService.createUnitOfWork("deleteContacts"))

        then:
        def remaining = contactsService.getContactsByAccount(CONTACTS_USER_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))
        remaining.collect { it.aggregateId } == [survivingAggregateId]
    }

    def "deleteContacts: unknown aggregate id"() {
        // Spec: plan.md §5 Contacts - DeleteContacts; Path A (aggregateLoadAndRegisterRead)
        when:
        contactsService.deleteContacts(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("deleteContacts"))

        then:
        thrown(SimulatorException)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
