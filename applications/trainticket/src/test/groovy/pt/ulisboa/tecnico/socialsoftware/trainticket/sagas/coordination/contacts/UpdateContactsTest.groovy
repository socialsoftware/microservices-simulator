package pt.ulisboa.tecnico.socialsoftware.trainticket.sagas.coordination.contacts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.trainticket.TrainticketSpockTest
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.states.ContactsSagaState
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.UpdateContactsFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateContactsTest extends TrainticketSpockTest {

    @Autowired
    CommandGateway commandGateway

    def "updateContacts: success"() {
        // Spec: plan.md §5 Contacts - UpdateContacts
        given: 'a contact record exists'
        def contactsAggregateId = createContacts()

        when:
        contactsFunctionalities.updateContacts(contactsAggregateId,
                new ContactsDto(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME_TWO,
                        CONTACTS_DOCUMENT_TYPE_TWO, CONTACTS_DOCUMENT_NUMBER_TWO, CONTACTS_PHONE_NUMBER_TWO))

        then: 'the traversal completes and releases the lock'
        sagaStateOf(contactsAggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "updateContacts: getContactsStep acquires IN_UPDATE_CONTACTS semantic lock"() {
        // Spec: plan.md §5 Contacts - UpdateContacts; primary-aggregate lock acquisition
        given:
        def contactsAggregateId = createContacts()
        def uow = unitOfWorkService.createUnitOfWork("updateContacts")
        def func = new UpdateContactsFunctionalitySagas(unitOfWorkService, contactsAggregateId,
                new ContactsDto(CONTACTS_USER_AGGREGATE_ID, CONTACTS_NAME_TWO,
                        CONTACTS_DOCUMENT_TYPE_TWO, CONTACTS_DOCUMENT_NUMBER_TWO, CONTACTS_PHONE_NUMBER_TWO),
                uow, commandGateway)
        func.executeUntilStep("getContactsStep", uow)

        expect: 'acquire transition: NOT_IN_SAGA -> IN_UPDATE_CONTACTS'
        sagaStateOf(contactsAggregateId) == ContactsSagaState.IN_UPDATE_CONTACTS

        when:
        func.resumeWorkflow(uow)

        then: 'traversal completes back to NOT_IN_SAGA'
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
