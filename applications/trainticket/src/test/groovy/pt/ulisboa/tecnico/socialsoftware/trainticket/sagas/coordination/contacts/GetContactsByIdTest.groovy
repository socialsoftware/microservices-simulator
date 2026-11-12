package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.contacts

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetContactsByIdTest extends TrainticketSpockTest {

    def "getContactsById: success"() {
        // Spec: plan.md §5 Contacts - GetContactsById
        given: 'a contact record exists'
        def contactsAggregateId = createContacts()

        when:
        def result = contactsFunctionalities.getContactsById(contactsAggregateId)

        then: 'the saga returns a coherent contacts DTO'
        result.aggregateId == contactsAggregateId
        result.userAggregateId == CONTACTS_USER_AGGREGATE_ID
        result.name == CONTACTS_NAME
        result.documentType == CONTACTS_DOCUMENT_TYPE
        result.documentNumber == CONTACTS_DOCUMENT_NUMBER
        result.phoneNumber == CONTACTS_PHONE_NUMBER
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
