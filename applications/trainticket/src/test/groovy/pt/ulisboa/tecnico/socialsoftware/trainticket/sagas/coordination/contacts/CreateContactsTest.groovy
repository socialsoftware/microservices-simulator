package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.contacts

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateContactsTest extends TrainticketSpockTest {

    def "createContacts: success"() {
        // Spec: plan.md §5 Contacts - CreateContacts
        given: 'a contacts request'
        def contactsDto = new ContactsDto(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME,
                CONTACTS_DOCUMENT_TYPE, CONTACTS_DOCUMENT_NUMBER, CONTACTS_PHONE_NUMBER)

        when:
        def result = contactsFunctionalities.createContacts(contactsDto)

        then: 'the saga returns a coherent contacts DTO'
        result.aggregateId != null
        result.userAggregateId == CONTACTS_USER_AGGREGATE_ID
        result.name == CONTACTS_NAME
        result.documentType == CONTACTS_DOCUMENT_TYPE
        result.documentNumber == CONTACTS_DOCUMENT_NUMBER
        result.phoneNumber == CONTACTS_PHONE_NUMBER

        and: 'the saga left no lock behind'
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
