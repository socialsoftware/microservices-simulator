package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.contacts

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateContactsCompensationTest extends TrainticketSpockTest {

    def contactsAggregateId

    def setup() {
        loadBehaviorScripts()
        contactsAggregateId = createContacts()
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateContacts: fault on updateContactsStep compensates the lock acquired by getContactsStep"() {
        // Spec: plan.md §5 Contacts - UpdateContacts; compensate transition
        when:
        contactsFunctionalities.updateContacts(contactsAggregateId,
                new ContactsDto(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME_TWO,
                        CONTACTS_DOCUMENT_TYPE_TWO, CONTACTS_DOCUMENT_NUMBER_TWO, CONTACTS_PHONE_NUMBER_TWO))

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(contactsAggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: read-back shows the pre-saga state'
        def reread = contactsFunctionalities.getContactsById(contactsAggregateId)
        reread.name == CONTACTS_NAME
        reread.documentType == CONTACTS_DOCUMENT_TYPE
        reread.documentNumber == CONTACTS_DOCUMENT_NUMBER
        reread.phoneNumber == CONTACTS_PHONE_NUMBER
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
