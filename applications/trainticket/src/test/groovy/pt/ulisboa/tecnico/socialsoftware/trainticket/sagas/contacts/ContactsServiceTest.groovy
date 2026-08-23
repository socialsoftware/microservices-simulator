package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.contacts

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

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

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
